package uk.gov.pay.connector.gateway.stripe.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.connector.app.StripeGatewayConfig;
import uk.gov.pay.connector.gateway.CaptureHandler;
import uk.gov.pay.connector.gateway.CaptureResponse;
import uk.gov.pay.connector.gateway.GatewayClient;
import uk.gov.pay.connector.gateway.GatewayException;
import uk.gov.pay.connector.gateway.GatewayException.GatewayErrorException;
import uk.gov.pay.connector.gateway.model.GatewayError;
import uk.gov.pay.connector.gateway.model.request.CaptureGatewayRequest;
import uk.gov.pay.connector.gateway.stripe.json.StripeCharge;
import uk.gov.pay.connector.gateway.stripe.json.StripeErrorResponse;
import uk.gov.pay.connector.gateway.stripe.json.StripeTransferResponse;
import uk.gov.pay.connector.gateway.stripe.request.StripeCaptureRequest;
import uk.gov.pay.connector.gateway.stripe.request.StripeTransferOutRequest;
import uk.gov.pay.connector.gateway.stripe.response.StripeCaptureResponse;
import uk.gov.pay.connector.util.JsonObjectMapper;

import java.util.Optional;

import static javax.ws.rs.core.Response.Status.Family.CLIENT_ERROR;
import static javax.ws.rs.core.Response.Status.Family.SERVER_ERROR;
import static uk.gov.pay.connector.gateway.CaptureResponse.ChargeState.COMPLETE;
import static uk.gov.pay.connector.gateway.CaptureResponse.fromBaseCaptureResponse;
import static uk.gov.pay.connector.gateway.model.GatewayError.gatewayConnectionError;

public class StripeCaptureHandler implements CaptureHandler {

    private static final Logger logger = LoggerFactory.getLogger(StripeCaptureHandler.class);

    private final GatewayClient client;
    private final StripeGatewayConfig stripeGatewayConfig;
    private final JsonObjectMapper jsonObjectMapper;

    public StripeCaptureHandler(GatewayClient client,
                                StripeGatewayConfig stripeGatewayConfig,
                                JsonObjectMapper jsonObjectMapper) {
        this.client = client;
        this.stripeGatewayConfig = stripeGatewayConfig;
        this.jsonObjectMapper = jsonObjectMapper;
    }

    @Override
    public CaptureResponse capture(CaptureGatewayRequest request) {
        String transactionId = request.getTransactionId();

        try {
            StripeCharge stripeCaptureResponse = captureWithPlatform(request);
            Optional<Long> maybeFee = Optional.empty();

            if (stripeCaptureResponse.isPlatformCharge()) {
                maybeFee = calculateProcessingFee(request.getAmount(), stripeCaptureResponse);
                Long netTransferAmount = maybeFee
                        .map(fee -> calculateNetTransferAmount(request.getAmount(), fee))
                        .orElse(request.getAmount());
                transferToConnectAccount(request, netTransferAmount);
            }

            return new CaptureResponse(
                    stripeCaptureResponse.getId(),
                    COMPLETE,
                    maybeFee.orElse(null)
            );
        } catch (GatewayErrorException e) {

            if (e.getFamily() == CLIENT_ERROR) {
                StripeErrorResponse stripeErrorResponse = jsonObjectMapper.getObject(e.getResponseFromGateway(), StripeErrorResponse.class);
                String errorCode = stripeErrorResponse.getError().getCode();
                String errorMessage = stripeErrorResponse.getError().getMessage();
                logger.error("Capture failed for transaction id {}. Failure code from Stripe: {}, failure message from Stripe: {}. External Charge id: {}. Response code from Stripe: {}",
                        transactionId, errorCode, errorMessage, request.getExternalId(), e.getStatus());

                return fromBaseCaptureResponse(new StripeCaptureResponse(transactionId, errorCode, errorMessage), null);
            }

            if (e.getFamily() == SERVER_ERROR) {
                logger.error("Capture failed for transaction id {}. Reason: {}. Status code from Stripe: {}. Charge External Id: {}",
                        transactionId, e.getMessage(), e.getStatus(), request.getExternalId());
                GatewayError gatewayError = gatewayConnectionError("An internal server error occurred when capturing charge_external_id: " + request.getExternalId());
                return CaptureResponse.fromGatewayError(gatewayError);
            }

            logger.info("Unrecognised response status during capture. charge_external_id={}, status={}, response={}",
                    request.getExternalId(), e.getStatus(), e.getResponseFromGateway());
            throw new RuntimeException("Unrecognised response status during capture.");

        } catch (GatewayException e) {
            return CaptureResponse.fromGatewayError(e.toGatewayError());
        }
    }

    private StripeCharge captureWithPlatform(CaptureGatewayRequest request) throws GatewayException.GenericGatewayException, GatewayErrorException, GatewayException.GatewayConnectionTimeoutException {
        String captureResponse = client.postRequestFor(StripeCaptureRequest.of(request, stripeGatewayConfig)).getEntity();
        StripeCharge stripeCaptureResponse = jsonObjectMapper.getObject(captureResponse, StripeCharge.class);
        logger.info("Captured charge id {} with platform account - stripe capture id {}",
                request.getExternalId(),
                stripeCaptureResponse.getId()
        );

        return stripeCaptureResponse;
    }

    private void transferToConnectAccount(CaptureGatewayRequest request, Long netTransferAmount) throws GatewayException.GenericGatewayException, GatewayErrorException, GatewayException.GatewayConnectionTimeoutException {
        String transferResponse = client.postRequestFor(StripeTransferOutRequest.of(netTransferAmount.toString(), request, stripeGatewayConfig)).getEntity();
        StripeTransferResponse stripeTransferResponse = jsonObjectMapper.getObject(transferResponse, StripeTransferResponse.class);
        logger.info("In capturing charge id {}, transferred net amount {} - transfer id {} -  to Stripe Connect account id {} in transfer group {}",
                request.getExternalId(),
                stripeTransferResponse.getAmount(),
                stripeTransferResponse.getId(),
                stripeTransferResponse.getDestinationStripeAccountId(),
                stripeTransferResponse.getStripeTransferGroup()
        );
    }

    private Optional<Long> calculateProcessingFee(Long grossChargeAmount, StripeCharge stripeCaptureResponse) {
        if (stripeGatewayConfig.isCollectFee()) {
            Double platformFee = Math.ceil((stripeGatewayConfig.getFeePercentage()/100) * grossChargeAmount);
            return stripeCaptureResponse.getFee().map(stripeFee -> stripeFee + platformFee.longValue());
        }
        
        return Optional.empty();
    }
    
    private Long calculateNetTransferAmount(Long captureAmount, Long fee) {
        return captureAmount - fee;
    }
}
