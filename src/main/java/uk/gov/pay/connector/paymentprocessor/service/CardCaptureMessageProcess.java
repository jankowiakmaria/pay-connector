package uk.gov.pay.connector.paymentprocessor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.connector.gateway.CaptureResponse;
import uk.gov.pay.connector.queue.CaptureQueue;
import uk.gov.pay.connector.queue.QueueException;
import uk.gov.pay.connector.queue.sqs.ChargeCaptureMessage;

import javax.inject.Inject;
import java.util.List;

// @TODO(sfount) replace `CardCaptureProcess` when feature flag is switched
public class CardCaptureMessageProcess {
  
    private static final Logger LOGGER = LoggerFactory.getLogger(CardCaptureMessageProcess.class);
    private final CaptureQueue captureQueue;
    private CardCaptureService cardCaptureService;

    @Inject
    public CardCaptureMessageProcess(CaptureQueue captureQueue, CardCaptureService cardCaptureService) { 
        this.captureQueue = captureQueue;
        this.cardCaptureService = cardCaptureService;
    }
    
    public void handleCaptureMessages() throws QueueException { 
        List<ChargeCaptureMessage> captureMessages = captureQueue.retrieveChargesForCapture();    
        for (ChargeCaptureMessage message: captureMessages) {
            try {
                LOGGER.info("Charge capture message received - {}", message.getChargeId());
                runCapture(message);
            } catch (Exception e) {
                LOGGER.warn("Error capturing charge from SQS message [{}]", e.getMessage());
            }
        }
    }
    
    private void runCapture(ChargeCaptureMessage captureMessage) throws QueueException {
        String externalChargeId = captureMessage.getChargeId();

        CaptureResponse gatewayResponse = cardCaptureService.doCapture(externalChargeId);

        if (gatewayResponse.isSuccessful()) {
            captureQueue.markMessageAsProcessed(captureMessage);
        } else {
            LOGGER.info(
                    "Failed to capture [externalChargeId={}] due to: {}",
                    externalChargeId,
                    gatewayResponse.getError().get().getMessage()
            );
        }
    }
    
}
