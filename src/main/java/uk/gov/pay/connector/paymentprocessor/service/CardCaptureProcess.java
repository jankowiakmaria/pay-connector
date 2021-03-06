package uk.gov.pay.connector.paymentprocessor.service;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Stopwatch;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import uk.gov.pay.connector.app.CaptureProcessConfig;
import uk.gov.pay.connector.app.ConnectorConfiguration;
import uk.gov.pay.connector.charge.dao.ChargeDao;
import uk.gov.pay.connector.charge.model.domain.ChargeEntity;
import uk.gov.pay.connector.common.exception.ConflictRuntimeException;
import uk.gov.pay.connector.gateway.CaptureResponse;
import uk.gov.pay.connector.util.RandomIdGenerator;

import javax.inject.Inject;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

public class CardCaptureProcess {

    private static final String HEADER_REQUEST_ID = "X-Request-Id";

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ChargeDao chargeDao;
    private final CardCaptureService captureService;
    private final MetricRegistry metricRegistry;
    private final CaptureProcessConfig captureConfig;
    private Queue<ChargeEntity> captureQueue;
    private volatile int totalChargesCaptured;
    private volatile String runCaptureId;
    private volatile int readyCaptureQueueSize;
    private volatile int waitingCaptureQueueSize;
    private volatile int chargesToCaptureSize;

    @Inject
    public CardCaptureProcess(Environment environment, ChargeDao chargeDao, CardCaptureService cardCaptureService,
                              ConnectorConfiguration connectorConfiguration, Queue<ChargeEntity> captureQueue) {
        this.chargeDao = chargeDao;
        this.captureService = cardCaptureService;
        this.captureConfig = connectorConfiguration.getCaptureProcessConfig();
        this.captureQueue = captureQueue;
        metricRegistry = environment.metrics();
        metricRegistry.gauge("gateway-operations.capture-process.queue-size.ready_capture_queue_size", () -> () -> readyCaptureQueueSize);
        metricRegistry.gauge("gateway-operations.capture-process.queue-size.waiting_capture_queue_size", () -> () -> waitingCaptureQueueSize);
    }

    public void runCapture(int threadNumber) {
        MDC.put(HEADER_REQUEST_ID, format("runCapture-%s, thread %d", runCaptureId, threadNumber));

        Stopwatch responseTimeStopwatch = Stopwatch.createStarted();
        int captured = 0, skipped = 0, error = 0, failedCapture = 0, total = 0;
        try {
            ChargeEntity charge;
            while ((charge = getChargeToCapture()) != null) {
                total = incrementTotalChargesCaptured();
                if (shouldRetry(charge)) {
                    try {
                        logger.info("Capturing [{} of {}] [chargeId={}]", total, chargesToCaptureSize, charge.getExternalId());
                        CaptureResponse gatewayResponse = captureService.doCapture(charge.getExternalId());
                        if (gatewayResponse.isSuccessful()) {
                            captured++;
                        } else {
                            logger.info("Failed to capture [chargeId={}] due to: {}", charge.getExternalId(),
                                    gatewayResponse.getError().get().getMessage());
                            failedCapture++;
                        }
                    } catch (ConflictRuntimeException e) {
                        logger.info("Another process has already attempted to capture [chargeId={}]. Skipping.", charge.getExternalId());
                        skipped++;
                    } catch (Exception e) {
                        logger.info("Exception [{}] when capturing charge [chargeId={}]. Skipping.", e.getMessage(), charge.getExternalId());
                        skipped++;
                    }
                } else {
                    captureService.markChargeAsCaptureError(charge.getExternalId());
                    error++;
                }
            }
        } catch (Exception e) {
            logger.error("Exception [{}] when running capture at charge [{} of {}]", e.getMessage(), total, chargesToCaptureSize, e);
        } finally {
            responseTimeStopwatch.stop();
            metricRegistry.histogram("gateway-operations.capture-process.running_time").update(responseTimeStopwatch.elapsed(TimeUnit.MILLISECONDS));
            if (readyCaptureQueueSize > 0) {
                logger.info("Capture complete [captured={}] [skipped={}] [capture_error={}] [failed_capture={}] [total={}]",
                        captured, skipped, error, failedCapture, readyCaptureQueueSize);
            }
        }
        MDC.remove(HEADER_REQUEST_ID);
    }

    private synchronized int incrementTotalChargesCaptured() {
        return ++totalChargesCaptured;
    }

    public synchronized void loadCaptureQueue() {
        if (captureQueue.isEmpty()) {
            runCaptureId = RandomIdGenerator.newId();
            MDC.put(HEADER_REQUEST_ID, format("runCapture-%s", runCaptureId));

            waitingCaptureQueueSize = chargeDao.countChargesAwaitingCaptureRetry(captureConfig.getRetryFailuresEveryAsJavaDuration());

            List<ChargeEntity> chargesToCapture = chargeDao.findChargesForCapture(captureConfig.getBatchSize(),
                    captureConfig.getRetryFailuresEveryAsJavaDuration());

            chargesToCaptureSize = chargesToCapture.size();

            if (chargesToCaptureSize < captureConfig.getBatchSize()) {
                readyCaptureQueueSize = chargesToCaptureSize;
            } else {
                readyCaptureQueueSize = chargeDao.countChargesForImmediateCapture(captureConfig.getRetryFailuresEveryAsJavaDuration());
            }

            if (chargesToCaptureSize > 0) {
                logger.info("Capturing: {} of {} charges", chargesToCaptureSize, (waitingCaptureQueueSize + readyCaptureQueueSize));
            }

            captureQueue.addAll(chargesToCapture);
            MDC.remove(HEADER_REQUEST_ID);
        }
    }

    private synchronized ChargeEntity getChargeToCapture() {
        return captureQueue.poll();
    }

    private boolean shouldRetry(ChargeEntity charge) {
        return chargeDao.countCaptureRetriesForCharge(charge.getId()) < captureConfig.getMaximumRetries();
    }

    int getReadyCaptureQueueSize() {
        return readyCaptureQueueSize;
    }

    int getWaitingCaptureQueueSize() {
        return waitingCaptureQueueSize;
    }
}
