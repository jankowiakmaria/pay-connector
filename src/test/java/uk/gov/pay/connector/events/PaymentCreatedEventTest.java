package uk.gov.pay.connector.events;

import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.connector.charge.model.ServicePaymentReference;
import uk.gov.pay.connector.charge.model.domain.ChargeEntity;
import uk.gov.pay.connector.model.domain.ChargeEntityFixture;

import java.time.ZonedDateTime;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class PaymentCreatedEventTest {

    private final String paymentId = "jweojfewjoifewj";
    private final String time = "2018-03-12T16:25:01.123456Z";

    ChargeEntity chargeEntity = ChargeEntityFixture.aValidChargeEntity()
            .withCreatedDate(ZonedDateTime.parse(time))
            .withExternalId(paymentId)
            .withDescription("new passport")
            .withReference(ServicePaymentReference.of("myref"))
            .withReturnUrl("http://example.com")
            .withAmount(100L)
            .build();
    
    private final PaymentCreatedEvent paymentCreatedEvent = PaymentCreatedEvent.from(chargeEntity);
    private String actual;


    @Before
    public void setup() throws Exception {
        actual = paymentCreatedEvent.toJsonString();
    }
    
    @Test
    public void serializesTimeWithMicrosecondPrecision() throws Exception {
        assertThat(actual, hasJsonPath("$.event_date", equalTo(time)));
    }

    @Test
    public void serializesPaymentCreatedEventType() {
        assertThat(actual, hasJsonPath("$.event_type", equalTo("PaymentCreated")));
    }

    @Test
    public void serializesPaymentResourceType() throws Exception {
        final String actual = paymentCreatedEvent.toJsonString();

        assertThat(actual, hasJsonPath("$.resource_type", equalTo("payment")));
    }

    @Test
    public void serializesPayloadFieldstoJsonString() throws Exception{
        final String actual = paymentCreatedEvent.toJsonString();

        assertThat(actual, hasJsonPath("$.event_data.amount", equalTo(100)));
        assertThat(actual, hasJsonPath("$.event_data.description", equalTo("new passport")));
        assertThat(actual, hasJsonPath("$.event_data.reference", equalTo("myref")));
        assertThat(actual, hasJsonPath("$.event_data.return_url", equalTo("http://example.com")));
        assertThat(actual, hasJsonPath("$.event_data.gateway_account_id", equalTo(chargeEntity.getGatewayAccount().getId().intValue())));
        assertThat(actual, hasJsonPath("$.event_data.payment_provider", equalTo(chargeEntity.getGatewayAccount().getGatewayName())));
    }
}
