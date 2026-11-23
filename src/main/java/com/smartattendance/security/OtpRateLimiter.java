package com.smartattendance.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class OtpRateLimiter {

    private static final int MAX_VERIFY_ATTEMPTS = 20;
    private static final int MAX_RESEND_ATTEMPTS = 10;

    private static final Duration WINDOW =
            Duration.ofMinutes(15);

    private static final int MAX_TRACKED_KEYS = 10000;

    private final Map<String, AttemptWindow> attempts =
            new LinkedHashMap<>(128, 0.75f, true);

    public synchronized void checkVerification(
            String remoteAddress
    ) {
        checkAndRecord(
                "OTP_VERIFY:" + normalizeIp(remoteAddress),
                MAX_VERIFY_ATTEMPTS
        );
    }

    public synchronized void checkResend(
            String remoteAddress
    ) {
        checkAndRecord(
                "OTP_RESEND:" + normalizeIp(remoteAddress),
                MAX_RESEND_ATTEMPTS
        );
    }

    private void checkAndRecord(
            String key,
            int maximum
    ) {

        Instant now = Instant.now();

        AttemptWindow window = attempts.get(key);

        if (window != null
                && window.startedAt.plus(WINDOW).isAfter(now)
                && window.count >= maximum) {

            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many OTP requests. Please try again later."
            );
        }

        if (window == null
                || !window.startedAt.plus(WINDOW).isAfter(now)) {

            attempts.put(key, new AttemptWindow(now, 1));

        } else {

            window.count++;
        }

        while (attempts.size() > MAX_TRACKED_KEYS) {
            String oldestKey =
                    attempts.keySet().iterator().next();

            attempts.remove(oldestKey);
        }
    }

    private String normalizeIp(String ip) {
        return ip == null || ip.isBlank()
                ? "unknown"
                : ip;
    }

    private static class AttemptWindow {

        private final Instant startedAt;
        private int count;

        private AttemptWindow(
                Instant startedAt,
                int count
        ) {
            this.startedAt = startedAt;
            this.count = count;
        }
    }
}
