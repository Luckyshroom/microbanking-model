package io.microbank;

import io.microbank.models.Account;
import io.microbank.models.Holder;
import io.microbank.models.Transaction;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.Future;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.CompletableHelper;
import io.vertx.reactivex.SingleHelper;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.http.HttpClient;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.servicediscovery.ServiceDiscovery;
import io.vertx.reactivex.servicediscovery.types.HttpEndpoint;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;

import java.util.*;

public abstract class MicrobankVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MicrobankVerticle.class);

    private ServiceDiscovery serviceDiscovery;
    private Set<Record> registeredRecords = new ConcurrentHashSet<>();

    @Override
    public void start() throws Exception {
        serviceDiscovery = ServiceDiscovery
                .create(vertx, new ServiceDiscoveryOptions().setBackendConfiguration(config()));
    }

    protected void createTransaction(RoutingContext context, String serviceName, Account corrAccount,
                                     Map<Holder, Account> accountMap, Map<Integer, Transaction> txMap) {
        Transaction tx = new Transaction(context.getBodyAsJson());
        Map<String, Holder> holderMap = new HashMap<>();
        accountMap.forEach((holder, account) -> {
            if (tx.getTxFrom().equals(account.getNumber())) {
                holderMap.put("txFrom", holder);
            }
            if (tx.getTxTo().equals(account.getNumber())) {
                holderMap.put("txTo", holder);
            }
        });
        if (holderMap.size() == 0) {
            LOGGER.info("Delegate processing...");
            if (!context.getBodyAsJson().getBoolean("isDelegated")) {
                dispatchRequest(context, "/transaction", serviceName, tx, txMap)
                        .subscribe(SingleHelper.toObserver(ar -> {
                            if (ar.succeeded()) {
                                if (ar.result()) {
                                    LOGGER.info("Delegate processing is finished!");
                                }
                            } else {
                                LOGGER.info("Service not found!");
                                notFound(context);
                            }
                        }));
            } else {
                LOGGER.info("Prevent loop!");
                tx.setTxStatus(false);
                txMap.put(txMap.size() + 1, tx);
                txFail(context, tx);
            }
        } else if (holderMap.size() == 2) {
            LOGGER.info("Inside processing...");
            Account txFrom = accountMap.get(holderMap.get("txFrom"));
            Account txTo = accountMap.get(holderMap.get("txTo"));
            if (txFrom.getAmount() < tx.getTxAmount()) {
                tx.setTxStatus(false);
                txMap.put(txMap.size() + 1, tx);
                txFail(context, tx);
            } else {
                txFrom.setAmount(txFrom.getAmount() - tx.getTxAmount());
                txTo.setAmount(txTo.getAmount() + tx.getTxAmount());
                tx.setTxStatus(true);
                txMap.put(txMap.size() + 1, tx);
                txSuccess(context, tx);
            }
        } else if (holderMap.size() == 1 && holderMap.containsKey("txFrom")) {
            LOGGER.info("Dispatch deposit processing...");
            Account txFrom = accountMap.get(holderMap.get("txFrom"));
            if (txFrom.getAmount() < tx.getTxAmount()) {
                tx.setTxStatus(false);
                txMap.put(txMap.size() + 1, tx);
                txFail(context, tx);
            } else {
                dispatchRequest(context, "/ops/deposit", serviceName, tx, txMap)
                        .subscribe(SingleHelper.toObserver(ar -> {
                            if (ar.succeeded()) {
                                if (ar.result()) {
                                    txFrom.setAmount(txFrom.getAmount() - tx.getTxAmount());
                                    corrAccount.setAmount(corrAccount.getAmount() - tx.getTxAmount());
                                }
                            } else {
                                notFound(context);
                            }
                        }));
            }
        } else if (holderMap.size() == 1 && holderMap.containsKey("txTo")) {
            LOGGER.info("Dispatch withdraw processing...");
            Account txTo = accountMap.get(holderMap.get("txTo"));
            dispatchRequest(context, "/ops/withdraw", serviceName, tx, txMap)
                    .subscribe(SingleHelper.toObserver(ar -> {
                        if (ar.succeeded()) {
                            if (ar.result()) {
                                txTo.setAmount(txTo.getAmount() + tx.getTxAmount());
                                corrAccount.setAmount(corrAccount.getAmount() + tx.getTxAmount());
                            }
                        } else {
                            notFound(context);
                        }
                    }));
        }
    }

    protected void depositRequest(RoutingContext context, Account corrAccount, Map<Holder, Account> accountMap,
                                  Map<Integer, Transaction> txMap) {
        LOGGER.info("Deposit operation processing...");
        Transaction tx = new Transaction(context.getBodyAsJson());
        accountMap.forEach((holder, account) -> {
            if (tx.getTxTo().equals(account.getNumber())) {
                account.setAmount(account.getAmount() + tx.getTxAmount());
                corrAccount.setAmount(corrAccount.getAmount() + tx.getTxAmount());
            }
        });
        tx.setTxStatus(true);
        txMap.put(txMap.size() + 1, tx);
        txSuccess(context, tx);
    }

    protected void withdrawRequest(RoutingContext context, Account corrAccount, Map<Holder, Account> accountMap,
                                   Map<Integer, Transaction> txMap) {
        LOGGER.info("Withdraw operation processing...");
        Transaction tx = new Transaction(context.getBodyAsJson());
        accountMap.forEach((holder, account) -> {
            if (tx.getTxFrom().equals(account.getNumber())) {
                account.setAmount(account.getAmount() - tx.getTxAmount());
                corrAccount.setAmount(corrAccount.getAmount() - tx.getTxAmount());
            }
        });
        tx.setTxStatus(true);
        txMap.put(txMap.size() + 1, tx);
        txSuccess(context, tx);
    }

    protected Single<Boolean> dispatchRequest(RoutingContext context, String url, String serviceName, Transaction tx,
                                              Map<Integer, Transaction> transactionMap) {
        return getEndpoint(serviceName)
                .flatMapSingle(ar -> WebClient.create(vertx)
                        .postAbs(ar.getLocation().getString("endpoint") + url)
                        .rxSendJsonObject(context.getBodyAsJson().put("isDelegated", true)))
                .map(response -> {
                    if (response.statusCode() == 200) {
                        tx.setTxStatus(true);
                        transactionMap.put(transactionMap.size() + 1, tx);
                        txSuccess(context, tx);
                        return true;
                    } else {
                        tx.setTxStatus(false);
                        transactionMap.put(transactionMap.size() + 1, tx);
                        txFail(context, tx);
                        return false;
                    }
                });
    }

    protected void getAccountList(RoutingContext context, Map<Holder, Account> accountMap) {
        LOGGER.info("Fetching account list...");
        List<JsonObject> accountList = new ArrayList<>();
        accountMap.forEach((holder, account) -> {
            JsonObject obj = new JsonObject();
            obj.put("holder", holder.toString());
            obj.put("amount", account.getAmount());
            obj.put("number", account.getNumber());
            accountList.add(obj);
        });
        context.response().setStatusCode(200)
                .putHeader("content-type", "application/json")
                .end(accountList.toString());
    }

    protected void getTxList(RoutingContext context, Map<Integer, Transaction> txMap) {
        LOGGER.info("Fetching transaction list...");
        List<JsonObject> txList = new ArrayList<>();
        txMap.forEach((key, tx) -> txList.add(tx.toJson()));
        context.response().setStatusCode(200)
                .putHeader("content-type", "application/json")
                .end(txList.toString());
    }

    protected void getInfo(RoutingContext context, Account correspondentAccount, String bankName) {
        LOGGER.info("Fetching info...");
        JsonObject info = new JsonObject();
        info.put("bankName", bankName);
        info.put("bankAmount", correspondentAccount.getAmount());
        info.put("bankNumber", correspondentAccount.getNumber());
        context.response().setStatusCode(200)
                .putHeader("content-type", "application/json")
                .end(info.toString());
    }

    /**
     * Get all REST endpoints from the service discovery infrastructure.
     *
     * @return async result
     */
    protected Maybe<Record> getEndpoint(String serviceName) {
        return serviceDiscovery.rxGetRecord(new JsonObject().put("name", serviceName));
    }

    protected Single<HttpClient> getHttpClient(String serviceName) {
        return HttpEndpoint.rxGetClient(serviceDiscovery, new JsonObject().put("name", serviceName));
    }

    protected Completable publish(Record record) {
        return serviceDiscovery.rxPublish(record)
                .doOnSuccess(ar -> {
                    registeredRecords.add(record);
                    LOGGER.info("Service <" + ar.getName() + "> published");
                }).ignoreElement();
    }

    protected Completable publishHttpEndpoint(int port, String host, String serviceName) {
        Record record = HttpEndpoint.createRecord(serviceName, host, port, "/api/" + serviceName,
                new JsonObject().put("api.name", serviceName));
        return publish(record);
    }

    protected void badRequest(RoutingContext context, Throwable ex) {
        context.response().setStatusCode(400)
                .putHeader("content-type", "application/json")
                .end(new JsonObject().put("error", ex.getMessage()).encodePrettily());
    }

    protected void notFound(RoutingContext context) {
        context.response().setStatusCode(404)
                .putHeader("content-type", "application/json")
                .end(new JsonObject().put("message", "not_found").encodePrettily());
    }

    protected void internalError(RoutingContext context, Throwable ex) {
        context.response().setStatusCode(500)
                .putHeader("content-type", "application/json")
                .end(new JsonObject().put("error", ex.getMessage()).encodePrettily());
    }

    protected void txFail(RoutingContext context, Transaction transaction) {
        context.response().setStatusCode(500)
                .putHeader("content-type", "application/json")
                .end(transaction.toJson().toString());
    }

    protected void txSuccess(RoutingContext context, Transaction transaction) {
        context.response().setStatusCode(200)
                .putHeader("content-type", "application/json")
                .end(transaction.toJson().toString());
    }

    @Override
    public void stop(Future<Void> future) throws Exception {
        Observable.fromIterable(registeredRecords)
                .flatMapCompletable(record -> serviceDiscovery.rxUnpublish(record.getRegistration()))
                .subscribe(CompletableHelper.toObserver(future));
    }
}
