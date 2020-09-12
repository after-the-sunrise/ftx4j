package com.after_sunrise.api.ftx4j;

import quickfix.Session;
import quickfix.SessionID;

import java.util.Objects;

/**
 * @author takanori.takase
 * @version 0.0.0
 */
public final class Ftx4jSession {

    private final SessionID id;

    private final Session session;

    Ftx4jSession(SessionID id, Session session) {

        this.id = Objects.requireNonNull(id, "SessionID is required.");

        this.session = Objects.requireNonNull(session, "Session is required.");

    }

    SessionID getId() {
        return id;
    }

    Session getSession() {
        return session;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Ftx4jSession && id.equals(((Ftx4jSession) o).id);
    }

    @Override
    public String toString() {
        return id.toString();
    }

}
