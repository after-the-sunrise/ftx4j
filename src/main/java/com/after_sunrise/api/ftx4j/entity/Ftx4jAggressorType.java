package com.after_sunrise.api.ftx4j.entity;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    final String id;

    Ftx4jAggressorType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static final Map<String, Ftx4jAggressorType> MAP = Collections.unmodifiableMap(Stream.of(
            Ftx4jAggressorType.values()).collect(Collectors.toMap(Ftx4jAggressorType::getId, Function.identity())));

    public static final int FIELD = 1057; // FIX.4.4 EP21

}
