package com.after_sunrise.api.ftx4j.entity;

import org.immutables.value.Value;

/**
 * Execution Report (8), ExecType=I (OrderStatus)
 *
 * @author takanori.takase
 * @version 0.0.0
 */
@Value.Immutable(singleton = true)
public abstract class Ftx4jStatusResponse {

    @Nullable
    public abstract Ftx4jOrder getOrder();

}
