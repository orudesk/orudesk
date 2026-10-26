package com.orudesk.orudesk.shared.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.CannotCreateTransactionException;

import java.net.SocketException;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;

import static com.orudesk.orudesk.shared.adapter.in.web.ConnectionFailures.isConnectionFailure;
import static org.assertj.core.api.Assertions.assertThat;

class ConnectionFailuresTest {

    @Test
    void theDatabaseWentAwayInsideATransaction() {
        // What PostgreSQL stopping mid-request looked like (Day 36): HikariCP's closed connection, SQLState 08003
        var error = new JpaSystemException(new RuntimeException("Unable to rollback against JDBC Connection",
                new SQLException("Connection is closed", "08003")));

        assertThat(isConnectionFailure(error)).isTrue();
    }

    @Test
    void thePoolCouldNotGetAConnection() {
        var error = new CannotCreateTransactionException("Could not open JPA EntityManager",
                new SQLTransientConnectionException("HikariPool-1 - Connection is not available"));

        assertThat(isConnectionFailure(error)).isTrue();
    }

    @Test
    void postgresIsShuttingDownOrASocketBroke() {
        assertThat(isConnectionFailure(new SQLException("terminating connection", "57P01"))).isTrue();
        assertThat(isConnectionFailure(new RuntimeException(new SocketException("Connection reset")))).isTrue();
    }

    @Test
    void aRealBugIsNotAnOutage() {
        assertThat(isConnectionFailure(new SQLException("syntax error at or near", "42601"))).isFalse();
        assertThat(isConnectionFailure(new IllegalStateException("oops"))).isFalse();
    }
}
