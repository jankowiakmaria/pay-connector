package uk.gov.pay.connector.gateway.epdq;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.connector.gateway.model.request.Auth3dsResponseGatewayRequest;
import uk.gov.pay.connector.gateway.model.response.BaseAuthoriseResponse;
import uk.gov.pay.connector.gateway.model.response.Gateway3DSAuthorisationResponse;
import uk.gov.pay.connector.gateway.model.response.GatewayResponse;

import static java.lang.String.format;
import static junit.framework.TestCase.assertFalse;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.pay.connector.gateway.model.response.BaseAuthoriseResponse.AuthoriseStatus.AUTHORISED;
import static uk.gov.pay.connector.gateway.model.response.BaseAuthoriseResponse.AuthoriseStatus.ERROR;
import static uk.gov.pay.connector.gateway.model.response.BaseAuthoriseResponse.AuthoriseStatus.REJECTED;
import static uk.gov.pay.connector.gateway.model.response.BaseAuthoriseResponse.AuthoriseStatus.REQUIRES_3DS;

@RunWith(MockitoJUnitRunner.class)
public class EpdqPaymentProvider3dsTest extends BaseEpdqPaymentProviderTest {

    @Test
    public void shouldRequire3dsAuthoriseRequest() throws Exception {
        mockPaymentProviderResponse(200, successAuth3dResponse());
        GatewayResponse<BaseAuthoriseResponse> response = provider.authorise(buildTestAuthorisationRequest());
        verifyPaymentProviderRequest(successAuthRequest());
        assertTrue(response.isSuccessful());
        assertThat(response.getBaseResponse().get().authoriseStatus(), is(REQUIRES_3DS));
    }

    @Test
    public void shouldAuthorise3dsResponseIfMatchesWithEpdqStatus() {
        mockPaymentProviderResponse(200, successAuthorisedQueryResponse());
        Gateway3DSAuthorisationResponse response = provider.authorise3dsResponse(buildTestAuthorisation3dsVerifyRequest("AUTHORISED"));
        verifyPaymentProviderRequest(successAuthQueryRequest());
        assertTrue(response.isSuccessful());
    }

    @Test
    public void shouldRejectOnRejectedEpdqStatusResponse_evenIfFrontendStatusIsSuccess() {
        mockPaymentProviderResponse(200, declinedAuthorisedQueryResponse());
        Auth3dsResponseGatewayRequest request = buildTestAuthorisation3dsVerifyRequest("AUTHORISED");
        Gateway3DSAuthorisationResponse response = provider.authorise3dsResponse(request);
        verifyPaymentProviderRequest(successAuthQueryRequest());
        assertFalse(response.isSuccessful());
        assertThat(response.isDeclined(), is(true));
        verify(mockMetricRegistry, times(1)).counter(format("epdq.authorise-3ds.result.mismatch.account.%s.frontendstatus.%s.gatewaystatus.%s", request.getGatewayAccount().getGatewayName(), "AUTHORISED", REJECTED.name()));
        verify(mockMetricRegistry.counter(""), times(1)).inc();
    }

    @Test
    public void shouldErrorOnErrorEpdqStatusResponse_evenIfFrontendStatusIsSuccess() {
        mockPaymentProviderResponse(200, errorAuthorisedQueryResponse());
        Auth3dsResponseGatewayRequest request = buildTestAuthorisation3dsVerifyRequest("AUTHORISED");
        Gateway3DSAuthorisationResponse response = provider.authorise3dsResponse(request);
        verifyPaymentProviderRequest(successAuthQueryRequest());
        assertTrue(response.isException());
        verify(mockMetricRegistry, times(1)).counter(format("epdq.authorise-3ds.result.mismatch.account.%s.frontendstatus.%s.gatewaystatus.%s", request.getGatewayAccount().getGatewayName(), "AUTHORISED", "ERROR"));
        verify(mockMetricRegistry.counter(""), times(1)).inc();
    }

    @Test
    public void shouldErrorAuthorisationWhenFrontendStatusIsError_evenIfEpdqStatusIsSuccess() {
        mockPaymentProviderResponse(200, successAuthResponse());
        Auth3dsResponseGatewayRequest request = buildTestAuthorisation3dsVerifyRequest("ERROR");
        Gateway3DSAuthorisationResponse response = provider.authorise3dsResponse(request);
        verifyPaymentProviderRequest(successAuthQueryRequest());
        assertTrue(response.isException());
        verify(mockMetricRegistry, times(1)).counter(format("epdq.authorise-3ds.result.mismatch.account.%s.frontendstatus.%s.gatewaystatus.%s", request.getGatewayAccount().getGatewayName(), "ERROR", AUTHORISED.name()));
        verify(mockMetricRegistry.counter(""), times(1)).inc();
    }

    @Test
    public void shouldRejectAuthorisationWhenFrontendStatusIsDeclined_evenIfEpdqStatusIsSuccess() {
        mockPaymentProviderResponse(200, successAuthResponse());
        Auth3dsResponseGatewayRequest request = buildTestAuthorisation3dsVerifyRequest("DECLINED");
        Gateway3DSAuthorisationResponse response = provider.authorise3dsResponse(request);
        verifyPaymentProviderRequest(successAuthQueryRequest());
        assertFalse(response.isSuccessful());
        verify(mockMetricRegistry, times(1)).counter(format("epdq.authorise-3ds.result.mismatch.account.%s.frontendstatus.%s.gatewaystatus.%s", request.getGatewayAccount().getGatewayName(), "DECLINED", AUTHORISED.name()));
        verify(mockMetricRegistry.counter(""), times(1)).inc();
    }

    @Test
    public void shouldRejectAuthorisationWhenFrontendStatusIsDeclined_evenIfEpdqStatusIsError() {
        mockPaymentProviderResponse(200, errorAuthResponse());
        Auth3dsResponseGatewayRequest request = buildTestAuthorisation3dsVerifyRequest("DECLINED");
        Gateway3DSAuthorisationResponse response = provider.authorise3dsResponse(request);
        verifyPaymentProviderRequest(successAuthQueryRequest());
        assertFalse(response.isSuccessful());
        verify(mockMetricRegistry, times(1)).counter(format("epdq.authorise-3ds.result.mismatch.account.%s.frontendstatus.%s.gatewaystatus.%s", request.getGatewayAccount().getGatewayName(), "DECLINED", ERROR.name()));
        verify(mockMetricRegistry.counter(""), times(1)).inc();
    }
}
