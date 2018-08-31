package io.mimoza;

import io.microbank.MicrobankVerticle;
import io.microbank.models.Account;
import io.microbank.models.Holder;
import io.microbank.models.Transaction;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.CompletableHelper;
import io.vertx.reactivex.SingleHelper;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.reactivex.ext.web.handler.StaticHandler;

import java.util.*;

public class MimozaVerticle extends MicrobankVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MimozaVerticle.class);
    private static final int DEFAULT_PORT = 8002;

    private static final String SERVICE_NAME = "mimoza";
    private static final String PARTNER_SERVICE_NAME = "lastochka";

    private static final Account CORR_ACCOUNT = new Account();
    private static final Map<Holder, Account> ACCOUNT_MAP = new HashMap<>();
    private static final Map<Integer, Transaction> TX_MAP = new HashMap<>();
    static {
        CORR_ACCOUNT.setAmount(2306600.56).setNumber("30101810400000000225");
        ACCOUNT_MAP.put(
                new Holder("Василий", "Бородино"),
                new Account(1200.08, "40817810099910004312"));
        ACCOUNT_MAP.put(
                new Holder("Светлана", "Цыганько"),
                new Account(6350.00, "40817810099910002865"));
        ACCOUNT_MAP.put(
                new Holder("Виталий", "Гаас"),
                new Account(500.50, "40817810099918885532"));
        ACCOUNT_MAP.put(
                new Holder("Роберт", "Трап"),
                new Account(320.00, "40817810099910745599"));
        ACCOUNT_MAP.put(
                new Holder("Игорь", "Фильтергауп"),
                new Account(2700.70, "40817810099910765555"));
    }

    @Override
    public void start(Future<Void> future) throws Exception {
        super.start();

        int port = config().getInteger("http.port", DEFAULT_PORT);
        String host = config().getString("http.address", "localhost");

        Router router = Router.router(vertx);

        router.route("/*").handler(BodyHandler.create());
        router.route("/*").handler(StaticHandler.create());

        router.get("/api/mimoza/accounts").handler(context ->
                getAccountList(context, ACCOUNT_MAP));
        router.get("/api/mimoza/transactions").handler(context ->
                getTxList(context, TX_MAP));
        router.get("/api/mimoza/info").handler(context ->
                getInfo(context, CORR_ACCOUNT, "Мимоза"));
        router.post("/api/mimoza/ops/deposit").handler(context ->
                depositRequest(context, CORR_ACCOUNT, ACCOUNT_MAP, TX_MAP));
        router.post("/api/mimoza/ops/withdraw").handler(context ->
                withdrawRequest(context, CORR_ACCOUNT, ACCOUNT_MAP, TX_MAP));
        router.post("/api/mimoza/transaction").handler(context ->
                createTransaction(context, PARTNER_SERVICE_NAME, CORR_ACCOUNT, ACCOUNT_MAP, TX_MAP));

        publishHttpEndpoint(port, host, SERVICE_NAME).subscribe(CompletableHelper.toObserver(future));

        vertx.createHttpServer()
                .requestHandler(router::accept)
                .rxListen(port, host)
                .subscribe(SingleHelper.toObserver(ar -> {
                    if (ar.succeeded()) {
                        LOGGER.info("Service " + SERVICE_NAME + " start at port: " + port);
                    } else {
                        LOGGER.info(ar.cause().getMessage());
                    }
                }));
    }
}
