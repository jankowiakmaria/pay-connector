package uk.gov.pay.connector.gatewayaccount.service;

import com.google.inject.persist.Transactional;
import uk.gov.pay.connector.gatewayaccount.dao.GatewayAccountDao;
import uk.gov.pay.connector.gatewayaccount.model.EmailCollectionMode;
import uk.gov.pay.connector.gatewayaccount.model.GatewayAccount;
import uk.gov.pay.connector.gatewayaccount.model.GatewayAccountEntity;
import uk.gov.pay.connector.gatewayaccount.model.PatchRequest;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import static uk.gov.pay.connector.gatewayaccount.resource.GatewayAccountRequestValidator.FIELD_ALLOW_WEB_PAYMENTS;
import static uk.gov.pay.connector.gatewayaccount.resource.GatewayAccountRequestValidator.FIELD_EMAIL_COLLECTION_MODE;
import static uk.gov.pay.connector.gatewayaccount.resource.GatewayAccountRequestValidator.FIELD_NOTIFY_SETTINGS;

public class GatewayAccountUpdater {

    private final GatewayAccountDao gatewayAccountDao;

    private final Map<String, BiConsumer<PatchRequest, GatewayAccountEntity>> attributeUpdater =
            new HashMap<String, BiConsumer<PatchRequest, GatewayAccountEntity>>() {{
                put(FIELD_ALLOW_WEB_PAYMENTS,
                        (gatewayAccountRequest, gatewayAccountEntity) -> gatewayAccountEntity.setAllowWebPayments(Boolean.valueOf(gatewayAccountRequest.valueAsString())));
                put(FIELD_NOTIFY_SETTINGS, 
                        (gatewayAccountRequest, gatewayAccountEntity) -> gatewayAccountEntity.setNotifySettings(gatewayAccountRequest.valueAsObject()));
                put(FIELD_EMAIL_COLLECTION_MODE,
                        (gatewayAccountRequest, gatewayAccountEntity) -> gatewayAccountEntity.setEmailCollectionMode(EmailCollectionMode.fromString(gatewayAccountRequest.valueAsString())));
            }};

    @Inject
    public GatewayAccountUpdater(GatewayAccountDao gatewayAccountDao) {
        this.gatewayAccountDao = gatewayAccountDao;
    }

    @Transactional
    public Optional<GatewayAccount> doPatch(Long gatewayAccountId, PatchRequest gatewayAccountRequest) {
        return gatewayAccountDao.findById(gatewayAccountId)
                .flatMap(gatewayAccountEntity -> {
                    attributeUpdater.get(gatewayAccountRequest.getPath())
                            .accept(gatewayAccountRequest, gatewayAccountEntity);
                    gatewayAccountDao.merge(gatewayAccountEntity);
                    return Optional.of(GatewayAccount.valueOf(gatewayAccountEntity));
                });
    }
}