package uk.gov.pay.connector.queue.sqs;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.AmazonSQSException;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;
import uk.gov.pay.connector.queue.QueueException;
import uk.gov.pay.connector.queue.QueueMessage;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SqsQueueServiceTest {

    private static final String QUEUE_URL = "http://queue-url";
    private static final String MESSAGE = "{chargeId: 123}";

    @Mock
    private AmazonSQS mockSqsClient;
    @Mock
    private Appender<ILoggingEvent> mockAppender;
    private ArgumentCaptor<LoggingEvent> loggingEventArgumentCaptor = ArgumentCaptor.forClass(LoggingEvent.class);

    private SqsQueueService sqsQueueService;

    @Before
    public void setUp() {
        sqsQueueService = new SqsQueueService(mockSqsClient);

        Logger root = (Logger) LoggerFactory.getLogger(SqsQueueService.class);
        root.setLevel(Level.INFO);
        root.addAppender(mockAppender);
    }

    @Test
    public void shouldSendMessageToQueueSuccessfully() throws QueueException {
        SendMessageResult sendMessageResult = new SendMessageResult();
        sendMessageResult.setMessageId("test-message-id");
        when(mockSqsClient.sendMessage(QUEUE_URL, MESSAGE)).thenReturn(sendMessageResult);

        QueueMessage message = sqsQueueService.sendMessage(QUEUE_URL, MESSAGE);
        assertEquals("test-message-id", message.getMessageId());

        verify(mockAppender, times(1)).doAppend(loggingEventArgumentCaptor.capture());
        List<LoggingEvent> logEvents = loggingEventArgumentCaptor.getAllValues();

        assertThat(logEvents.stream().anyMatch(e -> e.getFormattedMessage().contains("Message sent to SQS queue - {MessageId: test-message-id")), is(true));
    }

    @Test(expected = QueueException.class)
    public void shouldThrowExceptionIfMessageIsNotSentToQueue() throws QueueException {
        when(mockSqsClient.sendMessage(QUEUE_URL, MESSAGE)).thenThrow(AmazonSQSException.class);

        sqsQueueService.sendMessage(QUEUE_URL, MESSAGE);
    }

    @Test
    public void shouldReceiveMessagesFromQueueSuccessfully() throws QueueException {
        ReceiveMessageResult receiveMessageResult = new ReceiveMessageResult();
        Message message = new Message();
        message.setMessageId("test-message-id");
        message.setReceiptHandle("test-receipt-handle");
        message.setBody("test-message-body");
        receiveMessageResult.getMessages().add(message);

        when(mockSqsClient.receiveMessage(QUEUE_URL)).thenReturn(receiveMessageResult);

        List<QueueMessage> queueMessages = sqsQueueService.receiveMessages(QUEUE_URL);
        Assert.assertThat(queueMessages.size(), is(1));
        Assert.assertThat(queueMessages.get(0).getMessageId(), is("test-message-id"));
        Assert.assertThat(queueMessages.get(0).getReceiptHandle(), is("test-receipt-handle"));
        Assert.assertThat(queueMessages.get(0).getMessageBody(), is("test-message-body"));
    }

    @Test
    public void shouldReturnEmptyListWhenReceiveDoesNotReturnAnyMessages() throws QueueException {
        ReceiveMessageResult receiveMessageResult = new ReceiveMessageResult();
        when(mockSqsClient.receiveMessage(QUEUE_URL)).thenReturn(receiveMessageResult);

        List<QueueMessage> queueMessages = sqsQueueService.receiveMessages(QUEUE_URL);
        assertTrue(queueMessages.isEmpty());
    }

    @Test(expected = QueueException.class)
    public void shouldThrowExceptionIfMessageCannotBeReceivedFromQueue() throws QueueException {
        when(mockSqsClient.receiveMessage(anyString())).thenThrow(AmazonSQSException.class);

        sqsQueueService.receiveMessages(QUEUE_URL);
    }
}
