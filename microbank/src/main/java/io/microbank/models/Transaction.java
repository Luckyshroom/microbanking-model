package io.microbank.models;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class Transaction {

    private double txAmount;
    private String txFrom;
    private String txTo;
    private boolean txStatus;

    public Transaction() {
    }

    public Transaction(JsonObject obj) {
        TransactionConverter.fromJson(obj, this);
    }

    public Transaction(double txAmount, String txFrom, String txTo) {
        this.txAmount = txAmount;
        this.txFrom = txFrom;
        this.txTo = txTo;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        TransactionConverter.toJson(this, json);
        return json;
    }

    public double getTxAmount() {
        return txAmount;
    }
    public String getTxFrom() {
        return txFrom;
    }
    public String getTxTo() {
        return txTo;
    }
    public boolean isTxStatus() {
        return txStatus;
    }

    public void setTxAmount(double txAmount) {
        this.txAmount = txAmount;
    }
    public void setTxFrom(String txFrom) {
        this.txFrom = txFrom;
    }
    public void setTxTo(String txTo) {
        this.txTo = txTo;
    }
    public void setTxStatus(boolean txStatus) {
        this.txStatus = txStatus;
    }
}
