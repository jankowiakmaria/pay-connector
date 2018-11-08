package uk.gov.pay.connector.gateway;

import uk.gov.pay.connector.gateway.model.request.CaptureGatewayRequest;
import uk.gov.pay.connector.gateway.model.response.BaseCaptureResponse;
import uk.gov.pay.connector.gateway.model.response.GatewayResponse;

public interface CaptureHandler<T extends BaseCaptureResponse> {

    GatewayResponse<T> capture(CaptureGatewayRequest request);

}