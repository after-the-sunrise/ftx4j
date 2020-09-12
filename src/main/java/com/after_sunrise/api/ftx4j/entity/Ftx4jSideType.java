package com.after_sunrise.api.ftx4j.entity;

import quickfix.field.Side;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Buy / Sell
 *
 * @author takanori.takase
 * @version 0.0.0
 */
public enum Ftx4jSideType {

    BUY(Side.BUY), SELL(Side.SELL);

    final char id;

    Ftx4jSideType(char id) {
        this.id = id;
    }

    public char getId() {
        return id;
    }

    public static final Map<Character, Ftx4jSideType> MAP = Collections.unmodifiableMap(Stream.of(
            Ftx4jSideType.values()).collect(Collectors.toMap(Ftx4jSideType::getId, Function.identity())));

}
