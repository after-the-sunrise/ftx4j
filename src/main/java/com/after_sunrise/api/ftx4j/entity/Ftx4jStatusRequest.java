package com.after_sunrise.api.ftx4j.entity;

import com.after_sunrise.api.ftx4j.Ftx4jSession;
import org.immutables.value.Value;

/**
 * Order Status Request (H)
 *
 * @author takanori.takase
 * @version 0.0.0
 */
@Value.Immutable(singleton = true)
public abstract class Ftx4jStatusRequest {

    @Nullable
    public abstract Ftx4jSession getSession();

    @Nullable
    public abstract Ftx4jIdType getIdType();

    @Nullable
    public abstract String getIdValue();

}
