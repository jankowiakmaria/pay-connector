package uk.gov.pay.connector.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.pay.connector.common.model.domain.Address;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static uk.gov.pay.connector.gateway.model.PayersCardType.CREDIT_OR_DEBIT;

@ValidAuthCardDetails
public class AuthCardDetails implements AuthorisationDetails {

    private String cardNo;
    private String cardHolder;
    private String cvc;
    private String endDate;
    private Address address;
    private String cardBrand;
    private String userAgentHeader;
    private String acceptHeader;
    private PayersCardType payersCardType;
    private PayersCardPrepaidStatus payersCardPrepaidStatus;
    private Boolean corporateCard;

    public static AuthCardDetails anAuthCardDetails() {
        return new AuthCardDetails();
    }

    @JsonProperty("card_number")
    public void setCardNo(String cardNo) {
        this.cardNo = cardNo;
    }

    @JsonProperty("card_brand")
    public void setCardBrand(String cardBrand) {
        this.cardBrand = cardBrand;
    }

    @JsonProperty("cardholder_name")
    public void setCardHolder(String cardHolder) {
        this.cardHolder = cardHolder;
    }

    @JsonProperty("cvc")
    public void setCvc(String cvc) {
        this.cvc = cvc;
    }

    @JsonProperty("expiry_date")
    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    @JsonProperty("address")
    public void setAddress(Address address) {
        this.address = address;
    }

    @JsonProperty("user_agent_header")
    public void setUserAgentHeader(String userAgentHeader) {
        this.userAgentHeader = userAgentHeader;
    }

    @JsonProperty("accept_header")
    public void setAcceptHeader(String acceptHeader) {
        this.acceptHeader = acceptHeader;
    }

    @JsonProperty("corporate_card")
    public void setCorporateCard(Boolean corporateCard) {
        this.corporateCard = corporateCard;
    }

    @JsonProperty("card_type")
    public void setPayersCardType(PayersCardType payersCardType) {
        this.payersCardType = payersCardType;
    }

    @JsonProperty("prepaid")
    public void setPayersCardPrepaidStatus(PayersCardPrepaidStatus payersCardPrepaidStatus) {
        this.payersCardPrepaidStatus = payersCardPrepaidStatus;
    }

    public String getCardNo() {
        return cardNo;
    }

    public String getCardHolder() {
        return cardHolder;
    }

    public String getCvc() {
        return cvc;
    }

    public String getEndDate() {
        return endDate;
    }

    public String expiryMonth() {
        YearMonth yearMonth = YearMonth.parse(endDate, DateTimeFormatter.ofPattern("MM/yy"));
        return String.valueOf(yearMonth.getMonthValue());
    }

    public String expiryYear() {
        YearMonth yearMonth = YearMonth.parse(endDate, DateTimeFormatter.ofPattern("MM/yy"));
        return String.valueOf(yearMonth.getYear()).substring(2, 4);
    }

    public Optional<Address> getAddress() {
        return Optional.ofNullable(address);
    }

    public String getCardBrand() {
        return cardBrand;
    }

    public String getUserAgentHeader() {
        return userAgentHeader;
    }

    public String getAcceptHeader() {
        return acceptHeader;
    }

    public boolean isCorporateCard() {
        return corporateCard == null ? false : corporateCard;
    }

    public PayersCardType getPayersCardType() {
        return payersCardType == null ? CREDIT_OR_DEBIT : payersCardType;
    }

    public PayersCardPrepaidStatus getPayersCardPrepaidStatus() {
        return payersCardPrepaidStatus == null ? PayersCardPrepaidStatus.UNKNOWN : payersCardPrepaidStatus;
    }
}
