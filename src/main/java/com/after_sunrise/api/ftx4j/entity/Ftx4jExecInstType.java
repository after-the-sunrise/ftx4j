package com.after_sunrise.api.ftx4j.entity;

import quickfix.field.ExecInst;

/**
 * ExecInst (18)
 *
 * @author takanori.takase
 * @version 0.0.0
 */
public enum Ftx4jExecInstType {

    /**
     * Reduce only.
     */
    REDUCE_ONLY(ExecInst.DO_NOT_INCREASE_DNI),

    /**
     * Post only.
     */
    POST_ONLY(ExecInst.PARTICIPATE_DONT_INITIATE);

    final String id;

    Ftx4jExecInstType(char id) {
        this.id = String.valueOf(id);
    }

    public String getId() {
        return id;
    }

}
