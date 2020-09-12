package com.after_sunrise.api.ftx4j;

import com.after_sunrise.api.ftx4j.entity.Ftx4jExecution;
import com.after_sunrise.api.ftx4j.entity.Ftx4jOrder;

/**
 * @author takanori.takase
 * @version 0.0.0
 */
public interface Ftx4jListener {

    /**
     * Callback for successful logon.
     */
    default void onConnect(Ftx4jSession session) {
    }

    /**
     * Callback for successful logoff, or unexpected disconnect/failure.
     */
    default void onDisconnect(Ftx4jSession session) {
    }

    /**
     * Callback for Execution Report (8)
     */
    default void onOrder(Ftx4jSession session, Ftx4jOrder order) {
    }

    /**
     * Callback for Execution Report (8) with partial-filled (ExecType=1) or fully filled (ExecType=3).
     */
    default void onExecution(Ftx4jSession session, Ftx4jExecution execution) {
    }

}
