package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.out.TotpStore;
import com.orudesk.orudesk.identity.domain.UserId;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** A fake of the two-factor tables for tests. */
class InMemoryTotpStore implements TotpStore {

    private final Map<UserId, StoredTotp> totps = new HashMap<>();
    private final Map<UserId, Set<String>> unusedCodes = new HashMap<>();

    @Override
    public Optional<StoredTotp> find(UserId userId) {
        return Optional.ofNullable(totps.get(userId));
    }

    @Override
    public void savePending(UserId userId, String sealedSecret, Instant now) {
        totps.put(userId, new StoredTotp(sealedSecret, null, 0));
    }

    @Override
    public void confirm(UserId userId, long step, Instant now) {
        totps.computeIfPresent(userId, (id, t) -> new StoredTotp(t.sealedSecret(), now, step));
    }

    @Override
    public boolean useStep(UserId userId, long step) {
        StoredTotp t = totps.get(userId);
        if (t == null || t.lastUsedStep() >= step) {
            return false;
        }
        totps.put(userId, new StoredTotp(t.sealedSecret(), t.confirmedAt(), step));
        return true;
    }

    @Override
    public void delete(UserId userId) {
        totps.remove(userId);
        unusedCodes.remove(userId);
    }

    @Override
    public void replaceRecoveryCodes(UserId userId, List<String> hashes) {
        unusedCodes.put(userId, new HashSet<>(hashes));
    }

    @Override
    public boolean useRecoveryCode(UserId userId, String hash, Instant now) {
        return unusedCodes.getOrDefault(userId, new HashSet<>()).remove(hash);
    }

    @Override
    public int recoveryCodesLeft(UserId userId) {
        return unusedCodes.getOrDefault(userId, Set.of()).size();
    }
}
