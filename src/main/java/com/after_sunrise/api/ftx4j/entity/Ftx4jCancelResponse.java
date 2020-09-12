package com.after_sunrise.api.ftx4j.entity;

import org.immutables.value.Value;

/**
 * ExecutionReport (8), ExecType=6 (Pending Cancel)
 *
 * @author takanori.takase
 * @version 0.0.0
 */
@Value.Immutable(singleton = true)
public abstract class Ftx4jCancelResponse {

    @Nullable
    public abstract Ftx4jOrder getOrder();

}
