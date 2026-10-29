package com.orudesk.orudesk.shared;

/**
 * Remembers which pieces of work were already done, so an event handled twice does its work once (Day 54).
 * Call it inside the listener's transaction: if the work then fails, the transaction rolls back and the key is
 * forgotten, so the retry is allowed.
 */
public interface ProcessedEvents {

    /**
     * True the first time this key is seen, false ever after. Two concurrent callers with the same key: the
     * second waits for the first transaction and gets false if it committed.
     *
     * @param key what the work is about, for example "welcome-email:{user id}"; at most 200 characters
     */
    boolean firstTime(String key);
}
