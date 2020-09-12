package com.after_sunrise.api.ftx4j.entity;

import quickfix.field.TimeInForce;

/**
 * GTC / IOC
 *
 * @author takanori.takase
 * @version 0.0.0
 */
public enum Ftx4jTimeInForceType {

    /**
     * GTC
     */
    GOOD_TIL_CANCEL(TimeInForce.GOOD_TILL_CANCEL),

    /**
     * IOC
     */
    IMMEDIATE_OR_CANCEL(TimeInForce.IMMEDIATE_OR_CANCEL);

    final char id;

    Ftx4jTimeInForceType(char id) {
        this.id = id;
    }

    public char getId() {
        return id;
    }

}
