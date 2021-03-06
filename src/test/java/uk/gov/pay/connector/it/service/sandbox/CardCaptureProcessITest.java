package uk.gov.pay.connector.it.service.sandbox;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.connector.it.dao.DatabaseFixtures;
import uk.gov.pay.connector.it.service.CardCaptureProcessBaseITest;
import uk.gov.pay.connector.paymentprocessor.service.CardCaptureProcess;
import uk.gov.pay.connector.rules.WorldpayMockClient;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.pay.connector.charge.model.domain.ChargeStatus.CAPTURED;
import static uk.gov.pay.connector.charge.model.domain.ChargeStatus.CAPTURE_APPROVED;
import static uk.gov.pay.connector.charge.model.domain.ChargeStatus.ENTERING_CARD_DETAILS;

@RunWith(MockitoJUnitRunner.class)
public class CardCaptureProcessITest extends CardCaptureProcessBaseITest {

    private static final String PAYMENT_PROVIDER = "sandbox";

    @Test
    public void shouldCapture() {
        DatabaseFixtures.TestCharge testCharge = createTestCharge(PAYMENT_PROVIDER, CAPTURE_APPROVED);

        new WorldpayMockClient().mockCaptureSuccess();
        app.getInstanceFromGuiceContainer(CardCaptureProcess.class).loadCaptureQueue();
        app.getInstanceFromGuiceContainer(CardCaptureProcess.class).runCapture(1);

        assertThat(app.getDatabaseTestHelper().getChargeStatus(testCharge.getChargeId()), is(CAPTURED.getValue()));
    }

    @Test
    public void shouldNotCaptureWhenChargeIsNotInCorrectState() {
        DatabaseFixtures.TestCharge testCharge = createTestCharge(PAYMENT_PROVIDER, ENTERING_CARD_DETAILS);

        new WorldpayMockClient().mockCaptureSuccess();
        app.getInstanceFromGuiceContainer(CardCaptureProcess.class).loadCaptureQueue();
        app.getInstanceFromGuiceContainer(CardCaptureProcess.class).runCapture(1);

        assertThat(app.getDatabaseTestHelper().getChargeStatus(testCharge.getChargeId()), is(ENTERING_CARD_DETAILS.getValue()));
    }
}
