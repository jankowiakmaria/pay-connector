package uk.gov.pay.connector.gateway.stripe.payload;

import com.google.common.collect.ImmutableList;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import uk.gov.pay.connector.gateway.templates.PayloadDefinition;

import static org.apache.commons.lang3.StringUtils.isEmpty;
 
abstract class StripePayloadDefinition implements PayloadDefinition {

    static ParameterBuilder newParameterBuilder() {
        return new ParameterBuilder();
    }

    public static class ParameterBuilder {

        private ImmutableList.Builder<NameValuePair> parameters = new ImmutableList.Builder<>();

        public ParameterBuilder add(String name, String value) {
            if (!isEmpty(value)) {
                parameters.add(new BasicNameValuePair(name, value));
            }
            return this;
        }

        public ImmutableList<NameValuePair> build() {
            return parameters.build();
        }
    }
}
