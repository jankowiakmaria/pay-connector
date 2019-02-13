package uk.gov.pay.connector.it.dao;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.connector.gatewayaccount.dao.GatewayAccountStripeSetupDao;
import uk.gov.pay.connector.gatewayaccount.model.GatewayAccountStripeSetupTaskEntity;

import java.util.List;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.pay.connector.gatewayaccount.model.GatewayAccountStripeSetupTask.BANK_ACCOUNT_DETAILS;
import static uk.gov.pay.connector.gatewayaccount.model.GatewayAccountStripeSetupTask.ORGANISATION_VAT_NUMBER_COMPANY_NUMBER;
import static uk.gov.pay.connector.gatewayaccount.model.GatewayAccountStripeSetupTask.RESPONSIBLE_PERSON;

public class GatewayAccountStripeSetupDaoITest extends DaoITestBase {
    private GatewayAccountStripeSetupDao gatewayAccountStripeSetupDao;

    @Before
    public void setUp() {
        gatewayAccountStripeSetupDao = env.getInstance(GatewayAccountStripeSetupDao.class);
    }

    @After
    public void truncate() {
        databaseTestHelper.truncateAllData();
    }

    @Test
    public void shouldFindTasksByGatewayAccountId() {
        long gatewayAccountId = 42;
        databaseTestHelper.addGatewayAccount(gatewayAccountId, "stripe");

        long anotherGatewayAccountId = 1;
        databaseTestHelper.addGatewayAccount(anotherGatewayAccountId, "stripe");

        databaseTestHelper.addGatewayAccountsStripeSetupTask(gatewayAccountId, BANK_ACCOUNT_DETAILS);
        databaseTestHelper.addGatewayAccountsStripeSetupTask(gatewayAccountId, RESPONSIBLE_PERSON);
        databaseTestHelper.addGatewayAccountsStripeSetupTask(anotherGatewayAccountId, BANK_ACCOUNT_DETAILS);
        databaseTestHelper.addGatewayAccountsStripeSetupTask(anotherGatewayAccountId, ORGANISATION_VAT_NUMBER_COMPANY_NUMBER);

        List<GatewayAccountStripeSetupTaskEntity> tasks = gatewayAccountStripeSetupDao.findByGatewayAccountId(gatewayAccountId);
        assertThat(tasks, hasSize(2));
        assertThat(tasks, contains(
                allOf(
                        hasProperty("gatewayAccount", hasProperty("id", is(gatewayAccountId))),
                        hasProperty("task", is(BANK_ACCOUNT_DETAILS))),
                allOf(
                        hasProperty("gatewayAccount", hasProperty("id", is(gatewayAccountId))),
                        hasProperty("task", is(RESPONSIBLE_PERSON)))
        ));
    }
}