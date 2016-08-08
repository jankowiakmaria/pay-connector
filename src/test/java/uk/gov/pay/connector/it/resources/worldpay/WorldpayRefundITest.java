package uk.gov.pay.connector.it.resources.worldpay;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.ValidatableResponse;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.connector.it.base.CardResourceITestBase;
import uk.gov.pay.connector.it.dao.DatabaseFixtures;
import uk.gov.pay.connector.model.domain.RefundStatus;
import uk.gov.pay.connector.util.DatabaseTestHelper;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.SECONDS;
import static javax.ws.rs.core.Response.Status.*;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static uk.gov.pay.connector.matcher.ZoneDateTimeAsStringWithinMatcher.isWithin;
import static uk.gov.pay.connector.model.domain.ChargeStatus.CAPTURED;
import static uk.gov.pay.connector.model.domain.ChargeStatus.ENTERING_CARD_DETAILS;
import static uk.gov.pay.connector.resources.ApiPaths.REFUNDS_API_PATH;
import static uk.gov.pay.connector.resources.ApiPaths.REFUND_API_PATH;

public class WorldpayRefundITest extends CardResourceITestBase {

    private DatabaseFixtures.TestAccount defaultTestAccount;
    private DatabaseFixtures.TestCharge defaultTestCharge;
    private DatabaseTestHelper databaseTestHelper;

    public WorldpayRefundITest() {
        super("worldpay");
    }

    @Before
    public void setUp() throws Exception {
        databaseTestHelper = app.getDatabaseTestHelper();
        defaultTestAccount = DatabaseFixtures
                .withDatabaseTestHelper(databaseTestHelper)
                .aTestAccount()
                .withAccountId(Long.valueOf(accountId));

        defaultTestCharge = DatabaseFixtures
                .withDatabaseTestHelper(databaseTestHelper)
                .aTestCharge()
                .withAmount(100L)
                .withTestAccount(defaultTestAccount)
                .withChargeStatus(CAPTURED)
                .insert();
    }

    @Test
    public void shouldBeAbleToRequestARefund_partialAmount() {
        Long refundAmount = 50L;

        ValidatableResponse validatableResponse = postRefundFor(defaultTestCharge.getExternalChargeId(), refundAmount);
        String refundId = assertRefundResponseWith(refundAmount, validatableResponse, ACCEPTED.getStatusCode());

        List<Map<String, Object>> refundsFoundByChargeId = databaseTestHelper.getRefundsByChargeId(defaultTestCharge.getChargeId());
        assertThat(refundsFoundByChargeId.size(), is(1));
        assertThat(refundsFoundByChargeId, hasItems(aRefundMatching(refundId, defaultTestCharge.getChargeId(), refundAmount, "REFUND SUBMITTED")));
    }

    @Test
    public void shouldBeAbleToRequestARefund_fullAmount() {

        Long refundAmount = defaultTestCharge.getAmount();

        ValidatableResponse validatableResponse = postRefundFor(defaultTestCharge.getExternalChargeId(), refundAmount);
        String refundId = assertRefundResponseWith(refundAmount, validatableResponse, ACCEPTED.getStatusCode());

        List<Map<String, Object>> refundsFoundByChargeId = databaseTestHelper.getRefundsByChargeId(defaultTestCharge.getChargeId());
        assertThat(refundsFoundByChargeId.size(), is(1));
        assertThat(refundsFoundByChargeId, hasItems(aRefundMatching(refundId, defaultTestCharge.getChargeId(), refundAmount, "REFUND SUBMITTED")));
    }

    @Test
    public void shouldBeAbleToRequestARefund_multiplePartialAmounts() {

        Long firstRefundAmount = 80L;
        Long secondRefundAmount = 20L;
        Long chargeId = defaultTestCharge.getChargeId();
        String externalChargeId = defaultTestCharge.getExternalChargeId();

        ValidatableResponse firstValidatableResponse = postRefundFor(externalChargeId, firstRefundAmount);
        String firstRefundId = assertRefundResponseWith(firstRefundAmount, firstValidatableResponse, ACCEPTED.getStatusCode());

        ValidatableResponse secondValidatableResponse = postRefundFor(externalChargeId, secondRefundAmount);
        String secondRefundId = assertRefundResponseWith(secondRefundAmount, secondValidatableResponse, ACCEPTED.getStatusCode());

        List<Map<String, Object>> refundsFoundByChargeId = databaseTestHelper.getRefundsByChargeId(chargeId);
        assertThat(refundsFoundByChargeId.size(), is(2));

        assertThat(refundsFoundByChargeId, hasItems(
                aRefundMatching(firstRefundId, chargeId, firstRefundAmount, "REFUND SUBMITTED"),
                aRefundMatching(secondRefundId, chargeId, secondRefundAmount, "REFUND SUBMITTED")));
    }

    @Test
    public void shouldFailRequestingARefund_whenChargeStatusMakesItNotRefundable() {

        DatabaseFixtures.TestCharge testCharge = DatabaseFixtures
                .withDatabaseTestHelper(databaseTestHelper)
                .aTestCharge()
                .withAmount(100L)
                .withTestAccount(defaultTestAccount)
                .withChargeStatus(ENTERING_CARD_DETAILS)
                .insert();

        Long refundAmount = 20L;

        postRefundFor(testCharge.getExternalChargeId(), refundAmount)
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("reason", is("pending"))
                .body("message", is(format("Charge with id [%s] not available for refund.", testCharge.getExternalChargeId())));

        List<Map<String, Object>> refundsFoundByChargeId = databaseTestHelper.getRefundsByChargeId(defaultTestCharge.getChargeId());
        assertThat(refundsFoundByChargeId.size(), is(0));
    }

    @Test
    public void shouldFailRequestingARefund_whenAmountIsBiggerThanChargeAmount() {
        Long refundAmount = defaultTestCharge.getAmount() + 20;

        postRefundFor(defaultTestCharge.getExternalChargeId(), refundAmount)
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("reason", is("full"))
                .body("message", is(format("Charge with id [%s] not available for refund.", defaultTestCharge.getExternalChargeId())));

        List<Map<String, Object>> refundsFoundByChargeId = databaseTestHelper.getRefundsByChargeId(defaultTestCharge.getChargeId());
        assertThat(refundsFoundByChargeId.size(), is(0));
    }

    @Test
    public void shouldFailRequestingARefund_whenAPartialRefundMakesTotalRefundedAmountBiggerThanChargeAmount() {
        Long firstRefundAmount = 80L;
        Long secondRefundAmount = 30L; // 10 more than available

        ValidatableResponse validatableResponse = postRefundFor(defaultTestCharge.getExternalChargeId(), firstRefundAmount);
        String firstRefundId = assertRefundResponseWith(firstRefundAmount, validatableResponse, ACCEPTED.getStatusCode());

        List<Map<String, Object>> refundsFoundByChargeId = databaseTestHelper.getRefundsByChargeId(defaultTestCharge.getChargeId());
        assertThat(refundsFoundByChargeId.size(), is(1));
        assertThat(refundsFoundByChargeId, hasItems(aRefundMatching(firstRefundId, defaultTestCharge.getChargeId(), firstRefundAmount, "REFUND SUBMITTED")));

        postRefundFor(defaultTestCharge.getExternalChargeId(), secondRefundAmount)
                .statusCode(400)
                .body("reason", is("full"))
                .body("message", is(format("Charge with id [%s] not available for refund.", defaultTestCharge.getExternalChargeId())));

        List<Map<String, Object>> refundsFoundByChargeId1 = databaseTestHelper.getRefundsByChargeId(defaultTestCharge.getChargeId());
        assertThat(refundsFoundByChargeId1.size(), is(1));
        assertThat(refundsFoundByChargeId1, hasItems(aRefundMatching(firstRefundId, defaultTestCharge.getChargeId(), firstRefundAmount, "REFUND SUBMITTED")));
    }

    @Test
    public void shouldBeAbleRetrieveARefund() {
        DatabaseFixtures.TestRefund testRefund = DatabaseFixtures
                .withDatabaseTestHelper(databaseTestHelper)
                .aTestRefund()
                .withTestCharge(defaultTestCharge)
                .withType(RefundStatus.REFUND_SUBMITTED)
                .insert();

        ValidatableResponse validatableResponse = getRefundFor(
                defaultTestAccount.getAccountId(), defaultTestCharge.getExternalChargeId(), testRefund.getExternalRefundId());
        String refundId = assertRefundResponseWith(testRefund.getAmount(), validatableResponse, OK.getStatusCode());


        List<Map<String, Object>> refundsFoundByChargeId = databaseTestHelper.getRefundsByChargeId(defaultTestCharge.getChargeId());
        assertThat(refundsFoundByChargeId.size(), is(1));
        assertThat(refundsFoundByChargeId, hasItems(aRefundMatching(refundId, defaultTestCharge.getChargeId(), testRefund.getAmount(), "REFUND SUBMITTED")));
    }

    @Test
    public void shouldBeAbleToRetrieveAllRefundsForACharge() {

        DatabaseFixtures.TestRefund testRefund1 = DatabaseFixtures
                .withDatabaseTestHelper(databaseTestHelper)
                .aTestRefund()
                .withAmount(10L)
                .withCreatedDate(ZonedDateTime.of(2016, 8, 1, 0, 0, 0, 0, ZoneId.of("UTC")))
                .withTestCharge(defaultTestCharge)
                .withType(RefundStatus.REFUND_SUBMITTED)
                .insert();

        DatabaseFixtures.TestRefund testRefund2 = DatabaseFixtures
                .withDatabaseTestHelper(databaseTestHelper)
                .aTestRefund()
                .withAmount(20L)
                .withCreatedDate(ZonedDateTime.of(2016, 8, 2, 0, 0, 0, 0, ZoneId.of("UTC")))
                .withTestCharge(defaultTestCharge)
                .withType(RefundStatus.REFUND_SUBMITTED)
                .insert();

        String paymentUrl = format("http://localhost:%s/v1/api/accounts/%s/charges/%s",
                app.getLocalPort(), defaultTestAccount.getAccountId(), defaultTestCharge.getExternalChargeId());

        ValidatableResponse validatableResponse = getRefundsFor(defaultTestAccount.getAccountId(),
                defaultTestCharge.getExternalChargeId())
                .statusCode(OK.getStatusCode());

        String body = validatableResponse.extract().body().asString();

        System.out.println("body = " + body);

        validatableResponse
                .body("payment_id", is(defaultTestCharge.getExternalChargeId()))
                .body("_links.self.href", is(paymentUrl + "/refunds"))
                .body("_links.payment.href", is(paymentUrl))
                .body("_embedded.refunds", hasSize(2))
                .body("_embedded.refunds[0].refund_id", is(testRefund1.getExternalRefundId()))
                .body("_embedded.refunds[0].amount", is(10))
                .body("_embedded.refunds[0].status", is("submitted"))
                .body("_embedded.refunds[0].created_date", is("2016-08-01T00:00:00Z"))
                .body("_embedded.refunds[0]._links.self.href", is(paymentUrl + "/refunds/" + testRefund1.getExternalRefundId()))
                .body("_embedded.refunds[0]._links.payment.href", is(paymentUrl))
                .body("_embedded.refunds[1].refund_id", is(testRefund2.getExternalRefundId()))
                .body("_embedded.refunds[1].amount", is(20))
                .body("_embedded.refunds[1].status", is("submitted"))
                .body("_embedded.refunds[1].created_date", is("2016-08-02T00:00:00Z"))
                .body("_embedded.refunds[1]._links.self.href", is(paymentUrl + "/refunds/" + testRefund2.getExternalRefundId()))
                .body("_embedded.refunds[1]._links.payment.href", is(paymentUrl));
    }

    @Test
    public void shouldFailRetrieveARefund_whenNonExistentAccountId() {
        DatabaseFixtures.TestRefund testRefund = DatabaseFixtures
                .withDatabaseTestHelper(databaseTestHelper)
                .aTestRefund()
                .withTestCharge(defaultTestCharge)
                .withType(RefundStatus.REFUND_SUBMITTED)
                .insert();

        Long nonExistentAccountId = 999L;

        ValidatableResponse validatableResponse = getRefundFor(
                nonExistentAccountId, defaultTestCharge.getExternalChargeId(), testRefund.getExternalRefundId());

        validatableResponse.statusCode(NOT_FOUND.getStatusCode())
                .body("message", is(format("Charge with id [%s] not found.", defaultTestCharge.getExternalChargeId())));
    }

    @Test
    public void shouldFailRetrieveARefund_whenNonExistentChargeId() {
        DatabaseFixtures.TestRefund testRefund = DatabaseFixtures
                .withDatabaseTestHelper(databaseTestHelper)
                .aTestRefund()
                .withTestCharge(defaultTestCharge)
                .withType(RefundStatus.REFUND_SUBMITTED)
                .insert();

        String nonExistentChargeId = "999";

        ValidatableResponse validatableResponse = getRefundFor(
                defaultTestAccount.getAccountId(), nonExistentChargeId, testRefund.getExternalRefundId());

        validatableResponse.statusCode(NOT_FOUND.getStatusCode())
                .body("message", is(format("Charge with id [%s] not found.", nonExistentChargeId)));
    }

    @Test
    public void shouldFailRetrieveARefund_whenNonExistentRefundId() {
        DatabaseFixtures
                .withDatabaseTestHelper(databaseTestHelper)
                .aTestRefund()
                .withTestCharge(defaultTestCharge)
                .withType(RefundStatus.REFUND_SUBMITTED)
                .insert();

        String nonExistentRefundId = "999";

        ValidatableResponse validatableResponse = getRefundFor(
                defaultTestAccount.getAccountId(), defaultTestCharge.getExternalChargeId(), nonExistentRefundId);

        validatableResponse.statusCode(NOT_FOUND.getStatusCode())
                .body("message", is(format("Refund with id [%s] not found.", nonExistentRefundId)));
    }

    private ValidatableResponse postRefundFor(String chargeId, Long refundAmount) {
        worldpay.mockRefundResponse();
        return givenSetup()
                .body("{\"amount\": " + refundAmount + "}")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .post(REFUNDS_API_PATH
                        .replace("{accountId}", accountId)
                        .replace("{chargeId}", chargeId))
                .then();
    }

    private ValidatableResponse getRefundsFor(Long accountId, String chargeId) {
        worldpay.mockRefundResponse();
        return givenSetup()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get(REFUNDS_API_PATH
                        .replace("{accountId}", accountId.toString())
                        .replace("{chargeId}", chargeId))
                .then();
    }

    private ValidatableResponse getRefundFor(Long accountId, String chargeId, String refundId) {
        worldpay.mockRefundResponse();
        return givenSetup()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get(REFUND_API_PATH
                        .replace("{accountId}", accountId.toString())
                        .replace("{chargeId}", chargeId)
                        .replace("{refundId}", refundId))
                .then();
    }

    private String assertRefundResponseWith(Long refundAmount, ValidatableResponse validatableResponse, int expectedStatusCode) {
        ValidatableResponse response = validatableResponse
                .statusCode(expectedStatusCode)
                .body("refund_id", is(notNullValue()))
                .body("amount", is(refundAmount.intValue()))
                .body("status", is("submitted"))
                .body("created_date", matchesPattern("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}Z"))
                .body("created_date", isWithin(10, SECONDS));

        String paymentUrl = format("http://localhost:%s/v1/api/accounts/%s/charges/%s",
                app.getLocalPort(), defaultTestAccount.getAccountId(), defaultTestCharge.getExternalChargeId());

        String refundId = response.extract().path("refund_id");
        response.body("_links.self.href", is(paymentUrl + "/refunds/" + refundId))
                .body("_links.payment.href", is(paymentUrl));

        return refundId;
    }

    private static Matcher<Map<String, Object>> aRefundMatching(String externalId, long chargeId, long amount, String status) {
        return new TypeSafeMatcher<Map<String, Object>>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("{amount=").appendValue(amount).appendText(", ");
                description.appendText("charge_id=").appendValue(chargeId).appendText(", ");
                description.appendText("external_id=").appendValue(externalId).appendText(", ");
                description.appendText("status=").appendValue(status).appendText("}");
            }

            @Override
            protected boolean matchesSafely(Map<String, Object> record) {
                return record.get("external_id").equals(externalId) &&
                        record.get("amount").equals(amount) &&
                        record.get("status").equals(status) &&
                        record.get("charge_id").equals(chargeId);
            }
        };
    }
}