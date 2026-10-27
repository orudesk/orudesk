package com.orudesk.orudesk.identity.application.port.out;

import com.orudesk.orudesk.identity.domain.User;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface SearchUsersPort {

    /**
     * Users whose email contains {@code text}, sorted by email, starting after {@code afterEmail}.
     *
     * @param limit at most this many
     */
    List<User> search(@Nullable String text, @Nullable String afterEmail, int limit);
}
