package com.after_sunrise.api.ftx4j;

import com.after_sunrise.api.ftx4j.entity.Ftx4jCancelRequest;
import com.after_sunrise.api.ftx4j.entity.Ftx4jCancelResponse;
import com.after_sunrise.api.ftx4j.entity.Ftx4jCreateRequest;
import com.after_sunrise.api.ftx4j.entity.Ftx4jCreateResponse;
import com.after_sunrise.api.ftx4j.entity.Ftx4jStatusRequest;
import com.after_sunrise.api.ftx4j.entity.Ftx4jStatusResponse;
import quickfix.ConfigError;

import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * Java wrapper library for <a href="https://ftx.com/#a=ftx4j">FTX</a> FIX client API.
 *
 * @author takanori.takase
 * @version 0.0.0
 */
public interface Ftx4jApi extends AutoCloseable {

    static Ftx4jApi create(String apiKey, String secret, Ftx4jListener listener) {

        return create(apiKey, secret, null, listener);

    }

    static Ftx4jApi create(String apiKey, String secret, String account, Ftx4jListener listener) {

        Properties properties = new Properties(System.getProperties());

        Optional.ofNullable(apiKey).ifPresent(v -> properties.setProperty(Ftx4jConfig.AUTH_APIKEY.getId(), v));

        Optional.ofNullable(secret).ifPresent(v -> properties.setProperty(Ftx4jConfig.AUTH_SECRET.getId(), v));

        Optional.ofNullable(account).ifPresent(v -> properties.setProperty(Ftx4jConfig.AUTH_ACCOUNT.getId(), v));

        return create(properties, listener);

    }

    static Ftx4jApi create(Properties properties, Ftx4jListener listener) {

        try {

            Properties p = Objects.requireNonNullElseGet(properties, System::getProperties);

            Ftx4jListener l = Objects.requireNonNullElseGet(listener, () -> new Ftx4jListener() {
            });

            Ftx4jApiImpl impl = new Ftx4jApiImpl(p, l);

            impl.start();

            return impl;

        } catch (ConfigError e) {

            throw new IllegalArgumentException(e);

        }

    }

    /**
     * Send test request.
     */
    CompletableFuture<?> ping(Ftx4jSession session);

    /**
     * Create an order. Completes exceptionally on either:
     *
     * <ul>
     *     <li>ExecutionReport with ExecType=8 (Rejected)</li>
     *     <li>Reject (3)</li>
     * </ul>
     */
    CompletableFuture<Ftx4jCreateResponse> createOrder(Ftx4jCreateRequest request);

    /**
     * Cancel an order. Completes exceptionally on either:
     *
     * <ul>
     *     <li>OrderCancelReject (9)</li>
     *     <li>Reject (3)</li>
     * </ul>
     */
    CompletableFuture<Ftx4jCancelResponse> cancelOrder(Ftx4jCancelRequest request);

    /**
     * Query an order. Completes exceptionally on:
     *
     * <ul>
     *     <li>Reject (3)</li>
     * </ul>
     */
    CompletableFuture<Ftx4jStatusResponse> queryOrder(Ftx4jStatusRequest request);

}
