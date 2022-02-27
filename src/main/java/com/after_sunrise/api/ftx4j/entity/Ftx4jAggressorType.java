package com.after_sunrise.api.ftx4j.entity;

/**
 * Only present if this message was the result of a fill.
 *
 * @author takanori.takase
 * @version 0.0.0
 */
public enum Ftx4jAggressorType {

    /**
     * Taker fill.
     */
    TAKER("Y"),

    /**
     * Maker fill.
     */
    MAKER("N");

    final String value;

    Ftx4jAggressorType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Nullable
    public static Ftx4jAggressorType getByValue(String value) {
        if (value.equals("Y")) return TAKER;
        else if (value.equals("N")) return MAKER;
        return null;
    }

    public static final int FIELD = 1057; // FIX.4.4 EP21

}
