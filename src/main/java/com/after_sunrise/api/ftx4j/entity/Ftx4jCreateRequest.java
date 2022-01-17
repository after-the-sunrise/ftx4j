package com.after_sunrise.api.ftx4j.entity;

import com.after_sunrise.api.ftx4j.Ftx4jSession;
import org.immutables.value.Value;

import java.math.BigDecimal;

/**
 * New Order Single (D)
 *
 * @author takanori.takase
 * @version 0.0.0
 */
@Value.Immutable(singleton = true)
public abstract class Ftx4jCreateRequest {

    @Nullable
    public abstract Ftx4jSession getSession();

    @Nullable
    public abstract String getSymbol();

    @Nullable
    public abstract Ftx4jSideType getSide();

    @Nullable
    public abstract BigDecimal getPrice();

    @Nullable
    public abstract BigDecimal getSize();

    @Nullable
    public abstract Ftx4jTimeInForceType getTimeInForce();

    @Nullable
    public abstract Ftx4jExecInstType getExecInst();

    @Nullable
    public abstract String getClientId();

}
