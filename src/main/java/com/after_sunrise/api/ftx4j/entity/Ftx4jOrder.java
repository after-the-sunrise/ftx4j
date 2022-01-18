package com.after_sunrise.api.ftx4j.entity;

import org.immutables.value.Value;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Execution Report (8)
 *
 * @author takanori.takase
 * @version 0.0.0
 */
@Value.Immutable(singleton = true)
public abstract class Ftx4jOrder {

    @Nullable
    public abstract String getOrderId();

    @Nullable
    public abstract String getClientId();

    @Nullable
    public abstract Instant getTimestamp();

    @Nullable
    public abstract String getSymbol();

    @Nullable
    public abstract Ftx4jSideType getSide();

    @Nullable
    public abstract BigDecimal getOrderPrice();

    @Nullable
    public abstract BigDecimal getOrderQuantity();

    @Nullable
    public abstract BigDecimal getFilledQuantity();

    @Nullable
    public abstract BigDecimal getPendingQuantity();

    @Nullable
    public abstract BigDecimal getAverageFillPrice();

}
