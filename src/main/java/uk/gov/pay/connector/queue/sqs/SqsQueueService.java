package uk.gov.pay.connector.queue.sqs;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.AmazonSQSException;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.connector.queue.QueueException;
import uk.gov.pay.connector.queue.QueueMessage;

import javax.inject.Inject;
import java.util.List;

public class SqsQueueService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private AmazonSQS sqsClient;

    @Inject
    public SqsQueueService(AmazonSQS sqsClient) {
        this.sqsClient = sqsClient;
    }

    public QueueMessage sendMessage(String queueUrl, String messageBody) throws QueueException {
        try {
            SendMessageResult sendMessageResult = sqsClient.sendMessage(queueUrl, messageBody);

            logger.info("Message sent to SQS queue - {}", sendMessageResult);
            return QueueMessage.of(sendMessageResult, messageBody);
        } catch (AmazonSQSException | UnsupportedOperationException e) {
            logger.error("Failed sending message to SQS queue - {}", e.getMessage());
            throw new QueueException(e.getMessage());
        }
    }

    public List<QueueMessage> receiveMessages(String queueUrl) throws QueueException {
        try {
            ReceiveMessageResult receiveMessageResult = sqsClient.receiveMessage(queueUrl);

            return QueueMessage.of(receiveMessageResult);
        } catch (AmazonSQSException | UnsupportedOperationException e) {
            logger.error("Failed to receive messages from SQS queue - {}", e.getMessage());
            throw new QueueException(e.getMessage());
        }
    }

}
