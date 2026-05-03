package com.openfinova.banking.customer.testsupport;

import com.openfinova.banking.customer.api.entity.CustomerStatus;
import com.openfinova.banking.customer.api.entity.CustomerType;
import com.openfinova.banking.customer.entity.Customer;

public final class CustomerTestFixtures {

    private CustomerTestFixtures() {
    }

    public static Customer individual(String customerNumber) {
        Customer c = new Customer(CustomerType.INDIVIDUAL, "Jane", "Doe");
        c.setCustomerNumber(customerNumber);
        c.setStatus(CustomerStatus.PROSPECT);
        return c;
    }

    public static Customer business(String customerNumber) {
        Customer c = new Customer(CustomerType.BUSINESS, "Acme Ltd");
        c.setCustomerNumber(customerNumber);
        c.setStatus(CustomerStatus.PROSPECT);
        return c;
    }
}
