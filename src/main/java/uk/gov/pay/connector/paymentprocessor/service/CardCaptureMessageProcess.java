package uk.gov.pay.connector.paymentprocessor.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import uk.gov.pay.connector.gateway.CaptureResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.connector.queue.CaptureQueue;
import uk.gov.pay.connector.queue.QueueException;
import uk.gov.pay.connector.queue.QueueMessage;

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
        List<QueueMessage> captureMessages = captureQueue.receiveCaptureMessages();     
        for (QueueMessage message: captureMessages) {
            try {
                runCapture(message);
                
                // @TODO(sfount) model charge message as class, include charge ID (extracted) and message receipt handle
                captureQueue.markMessageAsProcessed(message);
            } catch (Exception e) {
                LOGGER.warn("Error capturing charge from SQS message [{}]", e);
            }
        }
    }
    
    private void runCapture(QueueMessage captureMessage) throws QueueException {  
        LOGGER.info("SQS message received [{}] - {}", captureMessage.getMessageId(), captureMessage.getMessageBody());

        String externalChargeId = getExternalChargeIdFromMessage(captureMessage);

        CaptureResponse gatewayResponse = cardCaptureService.doCapture(externalChargeId);

        if (gatewayResponse.isSuccessful()) {
            captureQueue.markMessageAsProcessed(captureMessage);
        } else {
            LOGGER.info(
                    "Failed to capture [messageBody={}] due to: {}",
                    captureMessage.getMessageBody(),
                    gatewayResponse.getError().get().getMessage()
            );
        }
    }

    private String getExternalChargeIdFromMessage(QueueMessage message) {
        JsonObject captureObject = new Gson().fromJson(message.getMessageBody(), JsonObject.class);
        return captureObject.get("chargeId").getAsString();
    }
    
}
