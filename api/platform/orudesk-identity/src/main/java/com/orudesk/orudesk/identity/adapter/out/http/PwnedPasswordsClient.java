package com.orudesk.orudesk.identity.adapter.out.http;

import com.orudesk.orudesk.identity.application.port.out.BreachedPasswords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Have I Been Pwned's Pwned Passwords range API (Day 69), with k-anonymity: only the first five characters of the
 * password's SHA-1 leave the server, and the answer lists every breached hash starting with them (hundreds), so the
 * service never learns which password was checked. "Add-Padding" pads the answer with fake entries (count 0), so
 * even its size says nothing.
 *
 * Off with orudesk.security.breached-passwords.enabled=false (tests, servers without internet access). A slow or
 * failing service never blocks a sign-up: the check fails open after a short timeout.
 */
@Component
class PwnedPasswordsClient implements BreachedPasswords {

    private static final Logger log = LoggerFactory.getLogger(PwnedPasswordsClient.class);

    private final boolean enabled;
    private final RestClient http;

    PwnedPasswordsClient(@Value("${orudesk.security.breached-passwords.enabled:true}") boolean enabled,
                         @Value("${orudesk.security.breached-passwords.url:https://api.pwnedpasswords.com/range/}")
                         String url,
                         @Value("${orudesk.security.breached-passwords.timeout:2s}") Duration timeout) {
        this.enabled = enabled;
        SimpleClientHttpRequestFactory requests = new SimpleClientHttpRequestFactory();
        requests.setConnectTimeout(timeout);
        requests.setReadTimeout(timeout);
        this.http = RestClient.builder().baseUrl(url).requestFactory(requests).build();
    }

    @Override
    public boolean isBreached(String rawPassword) {
        if (!enabled) {
            return false;
        }
        String hash = sha1(rawPassword);
        String prefix = hash.substring(0, 5);
        String suffix = hash.substring(5);
        try {
            String body = http.get().uri("{prefix}", prefix)
                    .header("Add-Padding", "true")
                    .header("User-Agent", "Orudesk")
                    .retrieve()
                    .body(String.class);
            return body != null && body.lines().anyMatch(line -> isHit(line, suffix));
        } catch (RuntimeException e) {
            log.warn("Could not check the password against breaches; allowing it: {}", e.toString());
            return false;
        }
    }

    /** A line is "SUFFIX:COUNT"; padding lines have count 0. */
    private static boolean isHit(String line, String suffix) {
        int colon = line.indexOf(':');
        return colon > 0 && line.substring(0, colon).equalsIgnoreCase(suffix)
                && !line.substring(colon + 1).trim().equals("0");
    }

    private static String sha1(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().withUpperCase().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 is always available in Java", e);
        }
    }
}
