package uk.gov.pay.connector.model;

import org.slf4j.Logger;

import javax.ws.rs.core.Response;

import static java.lang.String.format;
import static uk.gov.pay.connector.model.ErrorResponse.unexpectedStatusCodeFromGateway;
import static uk.gov.pay.connector.model.GatewayResponse.ResponseStatus.FAILED;
import static uk.gov.pay.connector.model.GatewayResponse.ResponseStatus.SUCCEDED;

public class InquiryResponse extends GatewayResponse {
    private ErrorResponse error;
    private String transactionId;
    private String newStatus;

    public InquiryResponse(ResponseStatus responseStatus, ErrorResponse error, String transactionId, String newStatus) {
        this.responseStatus = responseStatus;
        this.error = error;
        this.transactionId = transactionId;
        this.newStatus = newStatus;
    }

    @Override
    public ErrorResponse getError() {
        return error;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public static InquiryResponse inquiryFailureResponse(ErrorResponse errorResponse) {
        return new InquiryResponse(FAILED, errorResponse, null, null);
    }

    public static InquiryResponse inquiryStatusUpdate(String transactionId, String newStatus) {
        return new InquiryResponse(SUCCEDED, null, transactionId, newStatus);
    }

    public static InquiryResponse errorInquiryResponse(Logger logger, Response response) {
        logger.error(format("Error code received from gateway: %s.", response.getStatus()));
        return new InquiryResponse(FAILED, unexpectedStatusCodeFromGateway("Error processing status inquiry"), null, null);
    }
}
