package uk.gov.pay.connector.it.resources;

import org.junit.Rule;
import org.junit.Test;
import uk.gov.pay.connector.it.dao.DatabaseFixtures;
import uk.gov.pay.connector.rules.DropwizardAppWithPostgresRule;
import uk.gov.pay.connector.util.RestAssuredClient;

import static org.hamcrest.core.Is.is;

public class CardTypesResourceITest {

    @Rule
    public DropwizardAppWithPostgresRule app = new DropwizardAppWithPostgresRule();

    private String accountId = "66757943593456";
    private RestAssuredClient connectorApi = new RestAssuredClient(app, accountId);

    private DatabaseFixtures.TestCardType mastercardCreditCardTypeTestRecord;

    @Test
    public void shouldGetNoCardTypesWhenNoCardTypesExist() throws Exception {
        connectorApi
                .getCardTypes()
                .body("card_types.size()", is(0));
    }

    @Test
    public void shouldGetAllCardTypesWhenCardTypesExist() throws Exception {
        this.mastercardCreditCardTypeTestRecord = DatabaseFixtures
                .withDatabaseTestHelper(app.getDatabaseTestHelper())
                .aMastercardCreditCardType()
                .insert();

        connectorApi
                .getCardTypes()
                .body("card_types.size()", is(1))
                .body("card_types[0].id", is(this.mastercardCreditCardTypeTestRecord.getId().toString()))
                .body("card_types[0].brand", is(this.mastercardCreditCardTypeTestRecord.getBrand()))
                .body("card_types[0].label", is(this.mastercardCreditCardTypeTestRecord.getLabel()))
                .body("card_types[0].type", is(this.mastercardCreditCardTypeTestRecord.getType().toString()));
    }
}
