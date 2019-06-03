package uk.gov.pay.connector.app.managed;

import com.codahale.metrics.MetricRegistry;
import io.dropwizard.setup.Environment;
import uk.gov.pay.connector.app.CaptureProcessConfig;
import uk.gov.pay.connector.app.ConnectorConfiguration;
import uk.gov.pay.connector.charge.dao.ChargeDao;

import javax.inject.Inject;

public class ChargesAwaitingCaptureMetric {

    private final MetricRegistry metricRegistry;
    private final CaptureProcessConfig captureConfig;
    private ChargeDao chargeDao;

    private int numberOfChargesAwaitingCapture;

    @Inject
    public ChargesAwaitingCaptureMetric(Environment environment, ConnectorConfiguration connectorConfiguration, ChargeDao chargeDao) {
        this.chargeDao = chargeDao;
        this.captureConfig = connectorConfiguration.getCaptureProcessConfig();

        metricRegistry = environment.metrics();
        metricRegistry.gauge(
                "gateway-operations.capture-process.queue-size.ready_capture_queue_size",
                () -> () -> numberOfChargesAwaitingCapture
        );
    }

    public void measure() {
        numberOfChargesAwaitingCapture = chargeDao.countChargesForImmediateCapture(captureConfig.getRetryFailuresEveryAsJavaDuration());
    }
}
//todo: test?
