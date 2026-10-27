package com.orudesk.orudesk.shared.adapter.in.web;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientConnectionException;

/**
 * Is this error, somewhere in its causes, a lost connection to a backing service? Drivers and pools report it
 * in many shapes: a socket error, a JDBC "connection" exception type, or a plain SQLException whose SQLState
 * says so. Class 08 is the SQL standard's "connection exception" (HikariCP's "Connection is closed" is 08003);
 * 57P01-57P03 are PostgreSQL's "the server is shutting down / crashed / not accepting connections yet".
 */
final class ConnectionFailures {

    private ConnectionFailures() {
    }

    static boolean isConnectionFailure(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause() == cause ? null : cause.getCause()) {
            if (cause instanceof IOException
                    || cause instanceof SQLTransientConnectionException
                    || cause instanceof SQLNonTransientConnectionException
                    || cause instanceof SQLRecoverableException) {
                return true;
            }
            if (cause instanceof SQLException sql && sql.getSQLState() != null
                    && (sql.getSQLState().startsWith("08") || sql.getSQLState().startsWith("57P0"))) {
                return true;
            }
        }
        return false;
    }
}
