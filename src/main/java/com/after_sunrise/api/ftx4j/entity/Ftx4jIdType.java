package com.after_sunrise.api.ftx4j.entity;

/**
 * OrderID / ClOrdID
 *
 * @author takanori.takase
 * @version 0.0.0
 */
public enum Ftx4jIdType {

    /**
     * Server-assigned order ID. (OrderID)
     */
    SYSTEM,

    /**
     * Client-selected order ID. (ClOrdID)
     */
    CLIENT;

}
