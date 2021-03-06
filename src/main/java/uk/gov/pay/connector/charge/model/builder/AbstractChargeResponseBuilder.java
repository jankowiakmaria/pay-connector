package uk.gov.pay.connector.charge.model.builder;

import com.google.common.collect.ImmutableMap;
import uk.gov.pay.commons.model.SupportedLanguage;
import uk.gov.pay.commons.model.charge.ExternalMetadata;
import uk.gov.pay.connector.charge.model.ChargeResponse;
import uk.gov.pay.connector.charge.model.ServicePaymentReference;
import uk.gov.pay.connector.charge.model.domain.PersistedCard;
import uk.gov.pay.connector.common.model.api.ExternalTransactionState;
import uk.gov.pay.connector.wallets.WalletType;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractChargeResponseBuilder<T extends AbstractChargeResponseBuilder<T, R>, R> {
    protected String chargeId;
    protected Long amount;
    protected ExternalTransactionState state;
    protected String cardBrand;
    protected String gatewayTransactionId;
    protected String returnUrl;
    protected String description;
    protected ZonedDateTime createdDate;
    protected List<Map<String, Object>> links = new ArrayList<>();
    protected ServicePaymentReference reference;
    protected String providerName;
    protected String email;
    protected ChargeResponse.RefundSummary refundSummary;
    protected ChargeResponse.SettlementSummary settlementSummary;
    protected PersistedCard cardDetails;
    protected ChargeResponse.Auth3dsData auth3dsData;
    protected SupportedLanguage language;
    protected boolean delayedCapture;
    protected Long corporateCardSurcharge;
    protected Long fee;
    protected Long totalAmount;
    protected Long netAmount;
    protected WalletType walletType;
    protected ExternalMetadata externalMetadata;

    protected abstract T thisObject();

    public T withChargeId(String chargeId) {
        this.chargeId = chargeId;
        return thisObject();
    }

    public T withAmount(Long amount) {
        this.amount = amount;
        return thisObject();
    }

    public T withState(ExternalTransactionState state) {
        this.state = state;
        return thisObject();
    }

    public T withGatewayTransactionId(String gatewayTransactionId) {
        this.gatewayTransactionId = gatewayTransactionId;
        return thisObject();
    }

    public T withReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
        return thisObject();
    }

    public T withEmail(String email) {
        this.email = email;
        return thisObject();
    }

    public T withDescription(String description) {
        this.description = description;
        return thisObject();
    }

    public T withReference(ServicePaymentReference reference) {
        this.reference = reference;
        return thisObject();
    }

    public T withCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
        return thisObject();
    }

    public T withLink(String rel, String method, URI href) {
        links.add(ImmutableMap.of(
                "rel", rel,
                "method", method,
                "href", href
        ));
        return thisObject();
    }

    public T withLink(String rel, String method, URI href, String type, Map<String, Object> params) {
        links.add(ImmutableMap.of(
                "rel", rel,
                "method", method,
                "href", href,
                "type", type,
                "params", params
        ));

        return thisObject();
    }

    public T withProviderName(String providerName) {
        this.providerName = providerName;
        return thisObject();
    }

    public T withRefunds(ChargeResponse.RefundSummary refundSummary) {
        this.refundSummary = refundSummary;
        return thisObject();
    }

    public T withSettlement(ChargeResponse.SettlementSummary settlementSummary) {
        this.settlementSummary = settlementSummary;
        return thisObject();
    }

    public T withCardDetails(PersistedCard cardDetails) {
        this.cardDetails = cardDetails;
        return thisObject();
    }

    public T withAuth3dsData(ChargeResponse.Auth3dsData auth3dsData) {
        this.auth3dsData = auth3dsData;
        return thisObject();
    }

    public T withLanguage(SupportedLanguage language) {
        this.language = language;
        return thisObject();
    }
    
    public T withDelayedCapture(boolean delayedCapture) {
        this.delayedCapture = delayedCapture;
        return thisObject();
    }
    
    public T withCorporateCardSurcharge(Long corporateCardSurcharge) {
        this.corporateCardSurcharge = corporateCardSurcharge;
        return thisObject();
    }
    
    public T withFee(Long fee) {
        this.fee = fee;
        return thisObject();
    }
    
    public T withTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
        return thisObject();
    }
    
    public T withNetAmount(Long netAmount) {
        this.netAmount = netAmount;
        return thisObject();
    }
    
    public T withWalletType(WalletType walletType) {
        this.walletType = walletType;
        return thisObject();
    }
    
    public T withExternalMetadata(ExternalMetadata externalMetadata) {
        this.externalMetadata = externalMetadata;
        return thisObject();
    }

    public String getChargeId() {
        return chargeId;
    }

    public Long getAmount() {
        return amount;
    }

    public ExternalTransactionState getState() {
        return state;
    }

    public String getCardBrand() {
        return cardBrand;
    }

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public String getDescription() {
        return description;
    }

    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    public List<Map<String, Object>> getLinks() {
        return links;
    }

    public ServicePaymentReference getReference() {
        return reference;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getEmail() {
        return email;
    }

    public ChargeResponse.RefundSummary getRefundSummary() {
        return refundSummary;
    }

    public ChargeResponse.SettlementSummary getSettlementSummary() {
        return settlementSummary;
    }

    public PersistedCard getCardDetails() {
        return cardDetails;
    }

    public ChargeResponse.Auth3dsData getAuth3dsData() {
        return auth3dsData;
    }

    public SupportedLanguage getLanguage() {
        return language;
    }

    public boolean isDelayedCapture() {
        return delayedCapture;
    }

    public Long getCorporateCardSurcharge() {
        return corporateCardSurcharge;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }
    
    public Long getNetAmount() {
        return netAmount;
    }

    public WalletType getWalletType() {
        return walletType;
    }

    public abstract R build();

    public Long getFee() {
        return fee;
    }

    public ExternalMetadata getExternalMetadata() {
        return externalMetadata;
    }
}
