package io.microbank.models;

public class Account {

    private Double amount;
    private String number;

    public Account() {
    }

    public Account(Double amount, String number) {
        this.amount = amount;
        this.number = number;
    }

    public Double getAmount() {
        return amount;
    }
    public String getNumber() {
        return number;
    }

    public Account setAmount(Double amount) {
        this.amount = amount;
        return this;
    }
    public Account setNumber(String number) {
        this.number = number;
        return this;
    }
}
