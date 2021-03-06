package uk.gov.pay.connector.refund.service;

import uk.gov.pay.connector.charge.dao.SearchParams;
import uk.gov.pay.connector.common.service.search.AbstractSearchStrategy;
import uk.gov.pay.connector.common.service.search.BuildResponseStrategy;
import uk.gov.pay.connector.common.service.search.SearchStrategy;
import uk.gov.pay.connector.refund.dao.RefundDao;
import uk.gov.pay.connector.refund.model.SearchRefundsResponse;
import uk.gov.pay.connector.refund.model.domain.RefundEntity;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

import static java.lang.String.format;
import static javax.ws.rs.HttpMethod.GET;
import static uk.gov.pay.connector.refund.model.SearchRefundsResponse.anAllRefundsResponseBuilder;

public class RefundSearchStrategy extends AbstractSearchStrategy<RefundEntity, SearchRefundsResponse> implements SearchStrategy, BuildResponseStrategy<RefundEntity, SearchRefundsResponse> {

    private RefundDao refundDao;

    public RefundSearchStrategy(RefundDao refundDao) {
        this.refundDao = refundDao;
    }

    @Override
    public long getTotalFor(SearchParams params) {
        return refundDao.getTotalFor(params);
    }

    @Override
    public List<RefundEntity> findAllBy(SearchParams params) {
        return refundDao.findAllBy(params);
    }

    @Override
    public SearchRefundsResponse buildResponse(UriInfo uriInfo, RefundEntity refundEntity) {
        return populateResponseBuilderWith(anAllRefundsResponseBuilder(), uriInfo, refundEntity).build();
    }

    private SearchRefundsResponse.SearchRefundsResponseBuilder populateResponseBuilderWith(
            SearchRefundsResponse.SearchRefundsResponseBuilder responseBuilder,
            UriInfo uriInfo, RefundEntity refundEntity) {
        String accountId = String.valueOf(refundEntity.getChargeEntity().getGatewayAccount().getId());
        String externalChargeId = refundEntity.getChargeEntity().getExternalId();
        String externalRefundId = refundEntity.getExternalId();
        String gatewayTransactionId = refundEntity.getGatewayTransactionId();

        return responseBuilder
                .withRefundId(externalRefundId)
                .withCreatedDate(refundEntity.getCreatedDate())
                .withStatus(refundEntity.getStatus().toExternal().getStatus())
                .withChargeId(externalChargeId)
                .withGatewayTransactionId(gatewayTransactionId)
                .withAmountSubmitted(refundEntity.getAmount())
                .withLink("self", GET, selfUriFor(uriInfo, accountId, externalChargeId, externalRefundId))
                .withLink("payment_url", GET, paymentLinkFor(uriInfo, accountId, externalChargeId));
    }

    private URI selfUriFor(UriInfo uriInfo, String accountId, String externalChargeId, String externalRefundId) {
        String targetPath = format("/v1/api/accounts/%s/charges/%s/refunds/%s", accountId, externalChargeId, externalRefundId);
        return UriBuilder.fromUri(uriInfo.getBaseUri())
                .path(targetPath)
                .build();
    }

    private URI paymentLinkFor(UriInfo uriInfo, String accountId, String externalChargeId) {
        String targetPath = format("/v1/api/accounts/%s/charges/%s", accountId, externalChargeId);
        return UriBuilder.fromUri(uriInfo.getBaseUri())
                .path(targetPath)
                .build();
    }
}
