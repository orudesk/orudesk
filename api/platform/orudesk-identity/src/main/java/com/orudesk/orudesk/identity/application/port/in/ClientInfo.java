package com.orudesk.orudesk.identity.application.port.in;

import org.jspecify.annotations.Nullable;

/**
 * Where a login comes from (Day 66): the browser's User-Agent and the client's IP address, so a user's list of
 * sessions can say "Firefox on Windows, from 203.0.113.7". Shown to that user only; never used to decide anything.
 */
public record ClientInfo(String device, String ipAddress) {

    private static final ClientInfo UNKNOWN = new ClientInfo("Unknown device", "");

    public static ClientInfo unknown() {
        return UNKNOWN;
    }

    public static ClientInfo of(@Nullable String userAgent, @Nullable String ipAddress) {
        return new ClientInfo(deviceName(userAgent), ipAddress == null ? "" : ipAddress.strip());
    }

    /**
     * "Firefox on Windows" from a User-Agent string. Deliberately rough: user agents lie and change, and a person
     * only needs enough to recognise their own devices. The order matters: Edge and Opera also say "Chrome", and
     * Chrome also says "Safari".
     */
    static String deviceName(@Nullable String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return UNKNOWN.device();
        }
        String browser = userAgent.contains("Edg/") ? "Edge"
                : userAgent.contains("OPR/") ? "Opera"
                : userAgent.contains("Firefox/") ? "Firefox"
                : userAgent.contains("Chrome/") ? "Chrome"
                : userAgent.contains("Safari/") ? "Safari"
                : null;
        String system = userAgent.contains("Android") ? "Android"
                : userAgent.contains("iPhone") || userAgent.contains("iPad") ? "iOS"
                : userAgent.contains("Windows") ? "Windows"
                : userAgent.contains("Mac OS X") ? "macOS"
                : userAgent.contains("Linux") ? "Linux"
                : null;
        if (browser == null && system == null) {
            String first = userAgent.strip().split("[ /]", 2)[0];   // "okhttp/4.12" → "okhttp", "curl/8.5" → "curl"
            return first.length() > 40 ? first.substring(0, 40) : first;
        }
        if (browser == null) {
            return system;
        }
        return system == null ? browser : browser + " on " + system;
    }
}
