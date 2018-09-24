package uk.gov.pay.connector.service.search;

import uk.gov.pay.connector.dao.SearchParams;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

public interface SearchStrategy {

    Response search(SearchParams searchParams, UriInfo uriInfo);

}
