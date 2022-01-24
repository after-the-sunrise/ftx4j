package com.after_sunrise.api.ftx4j;

import com.after_sunrise.api.ftx4j.entity.Ftx4jAggressorType;
import com.after_sunrise.api.ftx4j.entity.Ftx4jCancelRequest;
import com.after_sunrise.api.ftx4j.entity.Ftx4jCancelResponse;
import com.after_sunrise.api.ftx4j.entity.Ftx4jCreateRequest;
import com.after_sunrise.api.ftx4j.entity.Ftx4jCreateResponse;
import com.after_sunrise.api.ftx4j.entity.Ftx4jExecution;
import com.after_sunrise.api.ftx4j.entity.Ftx4jIdType;
import com.after_sunrise.api.ftx4j.entity.Ftx4jOrder;
import com.after_sunrise.api.ftx4j.entity.Ftx4jSideType;
import com.after_sunrise.api.ftx4j.entity.Ftx4jStatusRequest;
import com.after_sunrise.api.ftx4j.entity.Ftx4jStatusResponse;
import com.after_sunrise.api.ftx4j.entity.ImmutableFtx4jCancelResponse;
import com.after_sunrise.api.ftx4j.entity.ImmutableFtx4jCreateResponse;
import com.after_sunrise.api.ftx4j.entity.ImmutableFtx4jExecution;
import com.after_sunrise.api.ftx4j.entity.ImmutableFtx4jOrder;
import com.after_sunrise.api.ftx4j.entity.ImmutableFtx4jStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.Application;
import quickfix.ConfigError;
import quickfix.DefaultMessageFactory;
import quickfix.FieldNotFound;
import quickfix.IncorrectTagValue;
import quickfix.Initiator;
import quickfix.Message;
import quickfix.MessageCracker;
import quickfix.NoopStoreFactory;
import quickfix.SLF4JLogFactory;
import quickfix.Session;
import quickfix.SessionFactory;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.SocketInitiator;
import quickfix.UnsupportedMessageType;
import quickfix.field.Account;
import quickfix.field.AvgPx;
import quickfix.field.ClOrdID;
import quickfix.field.Commission;
import quickfix.field.CumQty;
import quickfix.field.CxlRejReason;
import quickfix.field.EncryptMethod;
import quickfix.field.ExecInst;
import quickfix.field.ExecType;
import quickfix.field.HandlInst;
import quickfix.field.HeartBtInt;
import quickfix.field.LastPx;
import quickfix.field.LastQty;
import quickfix.field.LeavesQty;
import quickfix.field.MsgSeqNum;
import quickfix.field.MsgType;
import quickfix.field.OrdStatus;
import quickfix.field.OrdType;
import quickfix.field.OrderID;
import quickfix.field.OrderQty;
import quickfix.field.OrigClOrdID;
import quickfix.field.Price;
import quickfix.field.RawData;
import quickfix.field.RefTagID;
import quickfix.field.SenderCompID;
import quickfix.field.SendingTime;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.TargetCompID;
import quickfix.field.TestReqID;
import quickfix.field.Text;
import quickfix.field.TimeInForce;
import quickfix.field.TradeID;
import quickfix.fix42.Heartbeat;
import quickfix.fix42.Logon;
import quickfix.fix42.NewOrderSingle;
import quickfix.fix42.OrderCancelRequest;
import quickfix.fix42.OrderStatusRequest;
import quickfix.fix42.Reject;
import quickfix.fix42.TestRequest;
import quickfix.mina.ssl.SSLSupport;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.ZoneOffset.UTC;

/**
 * @author takanori.takase
 * @version 0.0.0
 */
public class Ftx4jApiImpl implements Ftx4jApi, Application, ThreadFactory {

    private static class Context {

        static final Context EMPTY = new Context(null);

        final Ftx4jSession session;

        final Map<CompletableFuture<Object>, TestRequest> tests;

        final Map<CompletableFuture<Ftx4jCreateResponse>, NewOrderSingle> creates;

        final Map<CompletableFuture<Ftx4jCancelResponse>, OrderCancelRequest> cancels;

        final Map<CompletableFuture<Ftx4jStatusResponse>, OrderStatusRequest> queries;

        final Collection<Map<? extends CompletableFuture<?>, ?>> requests;

        private Context(Ftx4jSession session) {
            this.session = session;
            this.tests = Collections.synchronizedMap(new WeakHashMap<>());
            this.creates = Collections.synchronizedMap(new WeakHashMap<>());
            this.cancels = Collections.synchronizedMap(new WeakHashMap<>());
            this.queries = Collections.synchronizedMap(new WeakHashMap<>());
            this.requests = Arrays.asList(tests, creates, cancels, queries);
        }

    }

    private static final AtomicInteger COUNT = new AtomicInteger();

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss").withZone(UTC);

    private static final char SOH = '\001';

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final MessageCracker cracker;

    private final Properties properties;

    private final Ftx4jListener listener;

    private final ExecutorService executor;

    private final Initiator initiator;

    private final Map<SessionID, Context> contexts;

    Ftx4jApiImpl(Properties properties, Ftx4jListener listener) throws ConfigError {
        this.cracker = new MessageCracker(this);
        this.contexts = Collections.synchronizedMap(new WeakHashMap<>());
        this.properties = Objects.requireNonNull(properties, "Properties is required.");
        this.listener = Objects.requireNonNull(listener, "Listener is required.");
        this.executor = Executors.newSingleThreadExecutor(this);
        this.initiator = buildInitiator(properties);
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        thread.setName(String.format("%s_%03d", getClass().getSimpleName(), COUNT.incrementAndGet()));
        thread.setUncaughtExceptionHandler((t, e) -> logger.error("Uncaught exception : {}, ", t, e));
        return thread;
    }

    Initiator buildInitiator(Properties p) throws ConfigError {

        Map<String, String> configs = new LinkedHashMap<>();

        configs.put(SessionFactory.SETTING_CONNECTION_TYPE, SessionFactory.INITIATOR_CONNECTION_TYPE);
        configs.put(Session.SETTING_NON_STOP_SESSION, Ftx4jConfig.FIX_SESSION_NON_STOP.getProperty(p));
        configs.put(Session.SETTING_RESET_ON_DISCONNECT, Ftx4jConfig.FIX_RESET_ON_DISCONNECT.getProperty(p));
        configs.put(Session.SETTING_VALIDATE_INCOMING_MESSAGE, Ftx4jConfig.FIX_VALIDATE_INCOMING.getProperty(p));
        configs.put(Session.SETTING_HEARTBTINT, Ftx4jConfig.FIX_HEARTBEAT_SECONDS.getProperty(p));
        configs.put(Initiator.SETTING_RECONNECT_INTERVAL, Ftx4jConfig.FIX_RECONNECT_SECONDS.getProperty(p));

        configs.put(SessionSettings.BEGINSTRING, Ftx4jConfig.FIX_VERSION.getProperty(p));
        configs.put(SessionSettings.TARGETCOMPID, Ftx4jConfig.FIX_TARGET_COMPID.getProperty(p));
        configs.put(SessionSettings.SENDERCOMPID, Ftx4jConfig.AUTH_APIKEY.getProperty(p));

        Optional.ofNullable(Ftx4jConfig.FIX_URI.getProperty(p)).map(URI::create).ifPresent(uri -> {
            configs.put(SSLSupport.SETTING_USE_SSL, Ftx4jConfig.FIX_SECURED.getProperty(p));
            configs.put(Initiator.SETTING_SOCKET_CONNECT_PROTOCOL, uri.getScheme());
            configs.put(Initiator.SETTING_SOCKET_CONNECT_HOST, uri.getHost());
            configs.put(Initiator.SETTING_SOCKET_CONNECT_PORT, String.valueOf(uri.getPort()));
        });

        Optional.ofNullable(Ftx4jConfig.FIX_PROXY_ADDRESS.getProperty(p)).map(URI::create).ifPresent(uri -> {
            configs.put(Initiator.SETTING_PROXY_TYPE, uri.getScheme());
            configs.put(Initiator.SETTING_PROXY_HOST, uri.getHost());
            configs.put(Initiator.SETTING_PROXY_PORT, String.valueOf(uri.getPort()));
            configs.put(Initiator.SETTING_PROXY_USER, Ftx4jConfig.FIX_PROXY_USER.getProperty(p));
            configs.put(Initiator.SETTING_PROXY_PASSWORD, Ftx4jConfig.FIX_PROXY_PASS.getProperty(p));
        });

        Optional.ofNullable(Ftx4jConfig.FIX_LOCAL_ADDRESS.getProperty(p)).map(URI::create).ifPresent(uri -> {
            configs.put(Initiator.SETTING_SOCKET_LOCAL_HOST, uri.getHost());
            configs.put(Initiator.SETTING_SOCKET_LOCAL_PORT, String.valueOf(uri.getPort()));
        });

        StringBuilder sb = new StringBuilder("[session]").append(System.lineSeparator());

        configs.forEach((k, v) -> Optional.ofNullable(v).filter(Predicate.not(String::isEmpty)).ifPresent(x -> {

            logger.debug("FIX session config : {}={}", k, v);

            sb.append(k).append('=').append(v).append(System.lineSeparator());

        }));

        SessionSettings session = new SessionSettings(new ByteArrayInputStream(sb.toString().getBytes(UTF_8)));

        return SocketInitiator.newBuilder()
                .withSettings(session)
                .withApplication(this)
                .withLogFactory(new SLF4JLogFactory(session))
                .withMessageFactory(new DefaultMessageFactory())
                .withMessageStoreFactory(new NoopStoreFactory())
                .build();

    }

    void start() throws ConfigError {

        synchronized (initiator) {

            if (executor.isShutdown()) {
                return;
            }

            logger.info("Staring FIX initiator.");

            initiator.start();

        }

    }

    @Override
    public void close() {

        synchronized (initiator) {

            if (executor.isShutdown()) {
                return;
            }

            logger.info("Stopping FIX initiator.");

            initiator.stop();

            executor.shutdown();

        }

    }

    @Override
    public void onCreate(SessionID sessionId) {

        logger.trace("onCreate : [{}]", sessionId);

        contexts.computeIfAbsent(sessionId, id -> new Context(new Ftx4jSession(id, Session.lookupSession(id))));

    }

    @Override
    public void onLogon(SessionID sessionId) {

        logger.debug("onLogon : [{}]", sessionId);

        Context context = contexts.getOrDefault(sessionId, Context.EMPTY);

        executor.execute(() -> listener.onConnect(context.session));

    }

    @Override
    public void onLogout(SessionID sessionId) {

        logger.debug("onLogout : [{}]", sessionId);

        Context context = contexts.getOrDefault(sessionId, Context.EMPTY);

        context.requests.forEach(futures -> futures.forEach(
                (future, x) -> future.completeExceptionally(new IOException("Session logout : " + sessionId))));

        executor.execute(() -> listener.onDisconnect(context.session));

    }

    @Override
    public void toAdmin(Message message, SessionID sessionId) {

        logger.trace("toAdmin : [{}] {}", sessionId, message);

        if (message instanceof Logon) {
            onLogon((Logon) message);
        }

    }

    @Override
    public void fromAdmin(Message message, SessionID sessionId) throws FieldNotFound {

        logger.trace("fromAdmin : [{}] {}", sessionId, message);

        if (message instanceof Reject) {
            onReject((Reject) message, sessionId);
        }

        if (message instanceof Heartbeat) {
            onHeartbeat((Heartbeat) message, sessionId);
        }

    }

    @Override
    public void toApp(Message message, SessionID sessionId) {

        logger.trace("toApp : [{}] {}", sessionId, message);

    }

    @Override
    public void fromApp(Message message, SessionID sessionId) throws FieldNotFound, IncorrectTagValue, UnsupportedMessageType {

        logger.trace("fromApp : [{}] {}", sessionId, message);

        if (MsgType.ORDER_MASS_CANCEL_REPORT.equals(message.getHeader().getString(MsgType.FIELD))) { // FIX 4.3

            onOrderMassCancelReport(message, sessionId);

            return;

        }

        cracker.crack(message, sessionId);

    }

    void onLogon(Logon message) {

        message.set(new EncryptMethod(EncryptMethod.NONE_OTHER));

        message.set(new HeartBtInt(Integer.parseInt(Ftx4jConfig.FIX_HEARTBEAT_SECONDS.getProperty(properties))));

        Optional.ofNullable(Ftx4jConfig.AUTH_SECRET.getProperty(properties))
                .map(v -> sign(message.getHeader(), v, Ftx4jConfig.AUTH_ALGORITHM.getProperty(properties)))
                .ifPresent(v -> message.set(new RawData(v)));

        Optional.ofNullable(Ftx4jConfig.AUTH_ACCOUNT
                .getProperty(properties)).ifPresent(v -> message.setString(Account.FIELD, v));

        Optional.ofNullable(Ftx4jConfig.FIX_CANCEL_ON_DISCONNECT
                .getProperty(properties)).ifPresent(v -> message.setString(8013, v));

    }

    String sign(Message.Header header, String secret, String algorithm) {

        if (header == null || secret == null || algorithm == null) {
            return null;
        }

        String signature = null;

        try {

            // Trim milliseconds.
            header.setString(SendingTime.FIELD, header.getUtcTimeStamp(SendingTime.FIELD).format(DTF));

            // join(SOH, [52:SendingTime, 35:MsgType, 34:MsgSeqNum, 49:SenderCompID, 56:TargetCompID])
            StringBuilder sb = new StringBuilder();
            sb.append(header.getString(SendingTime.FIELD));
            sb.append(SOH);
            sb.append(header.getString(MsgType.FIELD));
            sb.append(SOH);
            sb.append(header.getInt(MsgSeqNum.FIELD));
            sb.append(SOH);
            sb.append(header.getString(SenderCompID.FIELD));
            sb.append(SOH);
            sb.append(header.getString(TargetCompID.FIELD));

            // hmac()
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(secret.getBytes(UTF_8), algorithm));
            byte[] bytes = mac.doFinal(sb.toString().getBytes(UTF_8));

            sb.setLength(0); // reset for reuse.

            // hex()
            for (byte b : bytes) {
                sb.append(String.format("%02x", b & 0xff));
            }

            signature = sb.toString();

        } catch (FieldNotFound | GeneralSecurityException e) {

            logger.warn("Failed to generate signature : {}", algorithm, e);

        }

        return signature;

    }

    void onReject(Reject reject, SessionID sessionId) throws FieldNotFound {

        String refType = reject.getRefMsgType().getValue();

        int refSequence = reject.getRefSeqNum().getValue();

        Optional.ofNullable(contexts.get(sessionId)).ifPresent(context -> {

            String formatted = String.format("Reject - %s (RefMsgType=%s, RefSeqNum=%s, RefTagID=%s)",
                    reject.getOptionalString(Text.FIELD).orElse(null),
                    refType,
                    refSequence,
                    reject.getOptionalString(RefTagID.FIELD).orElse(null)
            );

            if (Logon.MSGTYPE.equals(refType)) {

                logger.warn("Logon failure : {}", formatted);

                return;

            }

            Consumer<Map<? extends CompletableFuture<?>, ? extends Message>> consumer = map -> map.forEach(
                    (f, m) -> m.getHeader().getOptionalDecimal(MsgSeqNum.FIELD).filter(
                            sequence -> sequence.intValue() == refSequence
                    ).ifPresent(
                            sequence -> f.completeExceptionally(new IllegalArgumentException(formatted))
                    )
            );

            if (NewOrderSingle.MSGTYPE.equals(refType)) {

                consumer.accept(context.creates);

                return;

            }

            if (OrderCancelRequest.MSGTYPE.equals(refType)) {

                consumer.accept(context.cancels);

                return;

            }

            if (OrderStatusRequest.MSGTYPE.equals(refType)) {

                consumer.accept(context.queries);

                return;

            }

            logger.warn("Unsupported reject message type : {}", formatted);

        });

    }

    void onHeartbeat(Heartbeat message, SessionID sessionId) {

        message.getOptionalString(TestReqID.FIELD).ifPresent(
                id -> Optional.ofNullable(contexts.get(sessionId)).ifPresent(context -> context.tests.forEach(
                        (future, m) -> m.getOptionalString(TestReqID.FIELD).filter(id::equals).ifPresent(
                                x -> future.complete(m.getHeader().getOptionalString(SendingTime.FIELD).orElse(null))
                        )
                ))
        );

    }

    @MessageCracker.Handler
    public void onExecutionReport(quickfix.fix42.ExecutionReport message, SessionID sessionId) throws FieldNotFound {

        Context context = contexts.getOrDefault(sessionId, Context.EMPTY);

        Ftx4jOrder order = convertOrder(message);

        char type = message.getExecType().getValue();

        if (type == ExecType.PENDING_NEW) { // New order accepted.

            Ftx4jCreateResponse response = ImmutableFtx4jCreateResponse.builder().order(order).build();

            context.creates.forEach((f, m) -> m.getOptionalString(ClOrdID.FIELD)
                    .filter(id -> id.equals(order.getClientId())).ifPresent(x -> f.complete(response)));

            return;

        } else if (type == ExecType.REJECTED) { // New order rejected.

            Exception exception = new IllegalArgumentException(String.format(
                    "Reject : %s (OrdRejReason=%s)",
                    message.getText().getValue(),
                    message.getOrdRejReason().getValue()
            ));

            context.creates.forEach((f, m) -> m.getOptionalString(ClOrdID.FIELD)
                    .filter(id -> id.equals(order.getClientId())).ifPresent(x -> f.completeExceptionally(exception)));

            return;

        } else if (type == ExecType.PENDING_CANCEL) { // Cancel order accepted.

            Ftx4jCancelResponse response = ImmutableFtx4jCancelResponse.builder().order(order).build();

            context.cancels.forEach((f, m) -> m.getOptionalString(OrderID.FIELD)
                    .filter(id -> id.equals(order.getOrderId())).ifPresent(x -> f.complete(response)));

            context.cancels.forEach((f, m) -> m.getOptionalString(OrigClOrdID.FIELD)
                    .filter(id -> id.equals(order.getClientId())).ifPresent(x -> f.complete(response)));

            return;

        } else if (type == ExecType.ORDER_STATUS) { // Order Status Response

            Ftx4jStatusResponse response = ImmutableFtx4jStatusResponse.builder().order(order).build();

            context.queries.forEach((f, m) -> m.getOptionalString(OrderID.FIELD)
                    .filter(id -> id.equals(order.getOrderId())).ifPresent(x -> f.complete(response)));

            context.queries.forEach((f, m) -> m.getOptionalString(ClOrdID.FIELD)
                    .filter(id -> id.equals(order.getClientId())).ifPresent(x -> f.complete(response)));

            return;

        } else if (type == ExecType.PARTIAL_FILL || type == ExecType.DONE_FOR_DAY) {

            Ftx4jExecution execution = convertExecution(message);

            executor.execute(() -> listener.onExecution(context.session, execution));

            // return; // Fall-through, since the order is also updated.

        }

        executor.execute(() -> listener.onOrder(context.session, order));

    }

    Ftx4jOrder convertOrder(quickfix.fix42.ExecutionReport message) throws FieldNotFound {

        ImmutableFtx4jOrder.Builder b = ImmutableFtx4jOrder.builder()
                .side(message.isSetSide() ? Ftx4jSideType.MAP.get(message.getSide().getValue()) : null)
                .timestamp(message.isSetTransactTime() ? message.getTransactTime().getValue().toInstant(UTC) : null);
        message.getOptionalString(ClOrdID.FIELD).ifPresent(b::clientId);
        message.getOptionalString(OrderID.FIELD).ifPresent(b::orderId);
        message.getOptionalString(Symbol.FIELD).ifPresent(b::symbol);
        message.getOptionalDecimal(Price.FIELD).ifPresent(b::orderPrice);
        message.getOptionalDecimal(AvgPx.FIELD).ifPresent(b::averageFillPrice);
        message.getOptionalDecimal(OrderQty.FIELD).ifPresent(b::orderQuantity);
        message.getOptionalDecimal(CumQty.FIELD).ifPresent(b::filledQuantity);
        message.getOptionalDecimal(LeavesQty.FIELD).ifPresent(b::pendingQuantity);
        return b.build();

    }

    Ftx4jExecution convertExecution(quickfix.fix42.ExecutionReport message) throws FieldNotFound {

        ImmutableFtx4jExecution.Builder b = ImmutableFtx4jExecution.builder()
                .side(message.isSetSide() ? Ftx4jSideType.MAP.get(message.getSide().getValue()) : null)
                .timestamp(message.isSetTransactTime() ? message.getTransactTime().getValue().toInstant(UTC) : null)
                .aggressor(message.isSetField(Ftx4jAggressorType.FIELD) ? Ftx4jAggressorType.MAP.get(message.getString(Ftx4jAggressorType.FIELD)) : null);
        message.getOptionalString(ClOrdID.FIELD).ifPresent(b::clientId);
        message.getOptionalString(OrderID.FIELD).ifPresent(b::orderId);
        message.getOptionalString(TradeID.FIELD).ifPresent(b::tradeId);
        message.getOptionalDecimal(LastPx.FIELD).ifPresent(b::price);
        message.getOptionalDecimal(LastQty.FIELD).ifPresent(b::quantity);
        message.getOptionalDecimal(Commission.FIELD).ifPresent(b::commission);
        return b.build();

    }

    @MessageCracker.Handler
    public void onOrderCancelReject(quickfix.fix42.OrderCancelReject message, SessionID sessionId) {

        Optional.ofNullable(contexts.get(sessionId)).ifPresent(context -> {

            String oid = message.getOptionalString(OrderID.FIELD).orElse(null);
            String cid = message.getOptionalString(OrigClOrdID.FIELD).orElse(null);

            Exception exception = new IllegalArgumentException(String.format(
                    "OrderCancelReject : OrderID=%s, OrigClOrdID=%s, OrdStatus=%s, CxlRejReason=%s",
                    oid,
                    cid,
                    message.getOptionalString(OrdStatus.FIELD).orElse(null),
                    message.getOptionalString(CxlRejReason.FIELD).orElse(null)
            ));

            context.cancels.forEach((f, m) -> m.getOptionalString(OrderID.FIELD)
                    .filter(v -> v.equals(oid)).ifPresent(x -> f.completeExceptionally(exception)));

            context.cancels.forEach((f, m) -> m.getOptionalString(OrigClOrdID.FIELD)
                    .filter(v -> v.equals(cid)).ifPresent(x -> f.completeExceptionally(exception)));

        });

    }

    void onOrderMassCancelReport(Message message, SessionID sessionId) {

        // TODO OrderMassCancelRequest (q) / OrderMassCancelReport (r) FIX.4.3

    }

    @Override
    public CompletableFuture<?> ping(Ftx4jSession session) {

        CompletableFuture<Object> future = new CompletableFuture<>();

        if (session == null) {

            future.completeExceptionally(new IllegalArgumentException("Session is required."));

        } else {

            Context context = contexts.get(session.getId());

            if (context == null || context.session == null || context.session.getSession() == null) {

                future.completeExceptionally(new IllegalArgumentException("Session does not exist."));

            } else {

                TestRequest message = new TestRequest();
                message.set(new TestReqID(UUID.randomUUID().toString()));

                context.tests.put(future, message);

                future.whenCompleteAsync((v, t) -> context.tests.remove(future));

                if (!context.session.getSession().send(message)) {

                    String template = "Failed to transmit message.";

                    future.completeExceptionally(new IOException(template));

                }

            }

        }

        return future;

    }

    @Override
    public CompletableFuture<Ftx4jCreateResponse> createOrder(Ftx4jCreateRequest request) {

        CompletableFuture<Ftx4jCreateResponse> future = new CompletableFuture<>();

        if (request == null || request.getSession() == null) {

            future.completeExceptionally(new IllegalArgumentException("Session is required."));

        } else {

            Context context = contexts.get(request.getSession().getId());

            if (context == null || context.session == null || context.session.getSession() == null) {

                future.completeExceptionally(new IllegalArgumentException("Session does not exist."));

            } else {

                NewOrderSingle message = new NewOrderSingle();
                message.set(new HandlInst(HandlInst.AUTOMATED_EXECUTION_ORDER_PRIVATE_NO_BROKER_INTERVENTION));
                message.set(new OrdType(OrdType.LIMIT));
                message.set(new ClOrdID(request.getClientId() != null ? request.getClientId() : UUID.randomUUID().toString()));
                Optional.ofNullable(request.getSymbol()).ifPresent(v -> message.set(new Symbol(v)));
                Optional.ofNullable(request.getSize()).ifPresent(v -> message.setString(OrderQty.FIELD, v.toPlainString()));
                Optional.ofNullable(request.getPrice()).ifPresent(v -> message.setString(Price.FIELD, v.toPlainString()));
                Optional.ofNullable(request.getSide()).ifPresent(v -> message.set(new Side(v.getId())));
                Optional.ofNullable(request.getTimeInForce()).ifPresent(v -> message.set(new TimeInForce(v.getId())));
                Optional.ofNullable(request.getExecInst()).ifPresent(v -> message.set(new ExecInst(v.getId())));

                context.creates.put(future, message);

                future.whenCompleteAsync((v, t) -> context.creates.remove(future));

                if (!context.session.getSession().send(message)) {

                    String template = "Failed to transmit message.";

                    future.completeExceptionally(new IOException(template));

                }

            }

        }

        return future;

    }

    @Override
    public CompletableFuture<Ftx4jCancelResponse> cancelOrder(Ftx4jCancelRequest request) {

        CompletableFuture<Ftx4jCancelResponse> future = new CompletableFuture<>();

        if (request == null || request.getSession() == null) {

            future.completeExceptionally(new IllegalArgumentException("Session is required."));

        } else {

            Context context = contexts.get(request.getSession().getId());

            if (context == null || context.session == null || context.session.getSession() == null) {

                future.completeExceptionally(new IllegalArgumentException("Session does not exist."));

            } else if (request.getIdType() == null || request.getIdValue() == null) {

                future.completeExceptionally(new IllegalArgumentException("Id is required."));

            } else {

                OrderCancelRequest message = new OrderCancelRequest();

                if (request.getIdType() == Ftx4jIdType.SYSTEM) {
                    message.set(new OrderID(request.getIdValue()));
                }

                if (request.getIdType() == Ftx4jIdType.CLIENT) {
                    message.set(new OrigClOrdID(request.getIdValue()));
                }

                context.cancels.put(future, message);

                future.whenCompleteAsync((v, t) -> context.cancels.remove(future));

                if (!context.session.getSession().send(message)) {

                    String template = "Failed to transmit message.";

                    future.completeExceptionally(new IOException(template));

                }

            }

        }

        return future;

    }

    @Override
    public CompletableFuture<Ftx4jStatusResponse> queryOrder(Ftx4jStatusRequest request) {

        CompletableFuture<Ftx4jStatusResponse> future = new CompletableFuture<>();

        if (request == null || request.getSession() == null) {

            future.completeExceptionally(new IllegalArgumentException("Session is required."));

        } else {

            Context context = contexts.get(request.getSession().getId());

            if (context == null || context.session == null || context.session.getSession() == null) {

                future.completeExceptionally(new IllegalArgumentException("Session does not exist."));

            } else if (request.getIdType() == null || request.getIdValue() == null) {

                future.completeExceptionally(new IllegalArgumentException("Id is required."));

            } else {

                OrderStatusRequest message = new OrderStatusRequest();

                if (request.getIdType() == Ftx4jIdType.SYSTEM) {
                    message.set(new OrderID(request.getIdValue()));
                }

                if (request.getIdType() == Ftx4jIdType.CLIENT) {
                    message.set(new ClOrdID(request.getIdValue()));
                }

                // TODO: IncludeFillInfo (20000), NoFills (1362) FIX.5.0 EP58

                context.queries.put(future, message);

                future.whenCompleteAsync((v, t) -> context.queries.remove(future));

                if (!context.session.getSession().send(message)) {

                    String template = "Failed to transmit message.";

                    future.completeExceptionally(new IOException(template));

                }

            }

        }

        return future;

    }

}
