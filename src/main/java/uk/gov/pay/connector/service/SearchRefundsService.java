package uk.gov.pay.connector.service;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.Pair;
import uk.gov.pay.connector.dao.SearchParams;
import uk.gov.pay.connector.dao.RefundDao;
import uk.gov.pay.connector.model.SearchRefundsResponse;
import uk.gov.pay.connector.model.domain.RefundEntity;
import uk.gov.pay.connector.resources.PaginationResponseBuilder;
import uk.gov.pay.connector.util.DateTimeUtils;
import uk.gov.pay.connector.util.ResponseUtil;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static javax.ws.rs.HttpMethod.GET;
import static uk.gov.pay.connector.model.SearchRefundsResponse.anAllRefundsResponseBuilder;
import static uk.gov.pay.connector.util.ResponseUtil.notFoundResponse;

public class SearchRefundsService {

    private static final Long MAX_DISPLAY_SIZE = 500L;
    private RefundDao refundDao;

    @Inject
    public SearchRefundsService(RefundDao refundDao) {
        this.refundDao = refundDao;
    }

    public Response getAllRefunds(UriInfo uriInfo, Long accountId, Long pageNumber, Long displaySize) {
        List<Pair<String, Long>> queryParams = ImmutableList.of(
                Pair.of("page", pageNumber),
                Pair.of("display_size", displaySize));

        return validateQueryParams(queryParams)
                .map(ResponseUtil::badRequestResponse)
                .orElseGet(() -> {
                    SearchParams searchParams = new SearchParams()
                            .withGatewayAccountId(accountId)
                            .withDisplaySize(calculateDisplaySize(displaySize))
                            .withPage(pageNumber != null ? pageNumber : 1);
                    return search(searchParams, uriInfo);
                });
    }

    private Long calculateDisplaySize(Long displaySize) {
        return displaySize == null ? MAX_DISPLAY_SIZE :
                (displaySize > MAX_DISPLAY_SIZE) ? MAX_DISPLAY_SIZE : displaySize;
    }

    private Response search(SearchParams searchParams, UriInfo uriInfo) {
        Long totalCount = refundDao.getTotalFor(searchParams);
        Long size = searchParams.getDisplaySize();
        if (totalCount > 0 && size > 0) {
            long lastPage = (totalCount + size - 1) / size;
            if (searchParams.getPage() > lastPage || searchParams.getPage() < 1) {
                return notFoundResponse("the requested page not found");
            }
        }

        List<RefundEntity> refunds = refundDao.findAllBy(searchParams);
        
        List<SearchRefundsResponse> refundResponses =
                refunds.stream()
                        .map(refund -> buildResponse(uriInfo, refund))
                        .collect(Collectors.toList());

        return new PaginationResponseBuilder(searchParams, uriInfo)
                .withResponses(refundResponses)
                .withTotalCount(totalCount)
                .buildResponse();
    }
    
    private SearchRefundsResponse buildResponse(UriInfo uriInfo, RefundEntity refundEntity){
        return populateResponseBuilderWith(anAllRefundsResponseBuilder(), uriInfo, refundEntity).build();
    }

    private SearchRefundsResponse.SearchRefundsResponseBuilder populateResponseBuilderWith(
            SearchRefundsResponse.SearchRefundsResponseBuilder responseBuilder, 
            UriInfo uriInfo, RefundEntity refundEntity) {
        Long accountId = refundEntity.getChargeEntity().getGatewayAccount().getId();
        return responseBuilder
                .withRefundId(refundEntity.getExternalId())
                .withCreatedDate(DateTimeUtils.toUTCDateTimeString(refundEntity.getCreatedDate()))
                .withStatus(String.valueOf(refundEntity.getStatus()))
                .withChargeId(refundEntity.getChargeEntity().getExternalId())
                .withAmountSubmitted(refundEntity.getChargeEntity().getAmount())
                .withLink("self", GET, selfUriFor(uriInfo, accountId))
                .withLink("payment_url", GET, paymentLinkFor(uriInfo, refundEntity.getChargeEntity().getExternalId()));
    }

    private URI paymentLinkFor(UriInfo uriInfo, String externalId) {
        String targetPath = "/v1/payments/{externalId}"
                .replace("{externalId}",externalId);
        return UriBuilder.fromUri(uriInfo.getBaseUri())
                .path(targetPath)
                .build();
    }

    private URI selfUriFor(UriInfo uriInfo, Long accountId) {
        String targetPath = "/v1/refunds/account/{accountId}"
                .replace("{accountId}", String.valueOf(accountId));
        return UriBuilder.fromUri(uriInfo.getBaseUri())
                .path(targetPath)
                .build();
    }

    private static Optional<List> validateQueryParams(List<Pair<String, Long>> nonNegativePairMap) {
        Map<String, String> invalidQueryParams = new HashMap<>();

        nonNegativePairMap.forEach(param -> {
            if (param.getRight() != null && param.getRight() < 1) {
                invalidQueryParams.put(param.getLeft(), "query param '%s' should be a non zero positive integer");
            }
        });

        if (!invalidQueryParams.isEmpty()) {
            List<String> invalidResponse = newArrayList();
            invalidResponse.addAll(invalidQueryParams.keySet()
                    .stream()
                    .map(param -> String.format(invalidQueryParams.get(param), param))
                    .collect(Collectors.toList()));
            return Optional.of(invalidResponse);
        }
        return Optional.empty();
    }
}
