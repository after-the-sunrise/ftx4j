package com.after_sunrise.api.ftx4j.entity;

import org.immutables.value.Value;

/**
 * Execution Report (8), ExecType=A (Pending New)
 *
 * @author takanori.takase
 * @version 0.0.0
 */
@Value.Immutable(singleton = true)
public abstract class Ftx4jCreateResponse {

    @Nullable
    public abstract Ftx4jOrder getOrder();

}
