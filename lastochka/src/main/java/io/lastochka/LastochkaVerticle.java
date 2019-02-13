package io.lastochka;

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

import java.util.HashMap;
import java.util.Map;

public class LastochkaVerticle extends MicrobankVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(LastochkaVerticle.class);
    private static final int DEFAULT_PORT = 8001;

    private static final String SERVICE_NAME = "lastochka";
    private static final String PARTNER_SERVICE_NAME = "mimoza";

    private static final Account CORR_ACCOUNT = new Account();
    private static final Map<Holder, Account> ACCOUNT_MAP = new HashMap<>();
    private static final Map<Integer, Transaction> TX_MAP = new HashMap<>();
    static {
        CORR_ACCOUNT.setAmount(1605400.90).setNumber("30101810400000000225");
        ACCOUNT_MAP.put(
                new Holder("Григорий", "Прик"),
                new Account(2200.18, "40817810088810000312"));
        ACCOUNT_MAP.put(
                new Holder("Антон", "Ляшевич"),
                new Account(3370.00, "40817810088810001865"));
        ACCOUNT_MAP.put(
                new Holder("Семён", "Цинаев"),
                new Account(700.20, "40817810088818885132"));
        ACCOUNT_MAP.put(
                new Holder("Виктория", "Баум"),
                new Account(5000.00, "40817810088810745519"));
        ACCOUNT_MAP.put(
                new Holder("Фёдор", "Ломаченко"),
                new Account(2100.30, "40817810088810765515"));
    }

    @Override
    @SuppressWarnings("Duplicates")
    public void start(Future<Void> future) throws Exception {
        super.start();

        int port = config().getInteger("http.port", DEFAULT_PORT);
        String host = config().getString("http.address", "localhost");

        Router router = Router.router(vertx);

        router.route("/*").handler(BodyHandler.create());
        router.route("/*").handler(StaticHandler.create());

        router.get("/api/lastochka/accounts").handler(context ->
                getAccountList(context, ACCOUNT_MAP));
        router.get("/api/lastochka/transactions").handler(context ->
                getTxList(context, TX_MAP));
        router.get("/api/lastochka/info").handler(context ->
                getInfo(context, CORR_ACCOUNT, "Ласточка"));
        router.post("/api/lastochka/ops/deposit").handler(context ->
                depositRequest(context, CORR_ACCOUNT, ACCOUNT_MAP, TX_MAP));
        router.post("/api/lastochka/ops/withdraw").handler(context ->
                withdrawRequest(context, CORR_ACCOUNT, ACCOUNT_MAP, TX_MAP));
        router.post("/api/lastochka/transaction").handler(context ->
                createTransaction(context, PARTNER_SERVICE_NAME, CORR_ACCOUNT, ACCOUNT_MAP, TX_MAP));

        publishHttpEndpoint(port, host, SERVICE_NAME).subscribe(CompletableHelper.toObserver(future));

        vertx.createHttpServer()
                .requestHandler(router::accept)
                .rxListen(port, host)
                .ignoreElement()
                .subscribe(() -> LOGGER.info("Service <" + SERVICE_NAME + "> start at port: " + port),
                        throwable -> LOGGER.info(throwable.getMessage()));
    }
}
