package com.smartattendance.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS_PER_IP = 30;
    private static final int MAX_ATTEMPTS_PER_ACCOUNT = 10;

    private static final Duration WINDOW =
            Duration.ofMinutes(15);

    private static final int MAX_TRACKED_KEYS = 10000;

    private final Map<String, AttemptWindow> attempts =
            new LinkedHashMap<>(128, 0.75f, true);

    /**
     * Apply limits using the remote IP and normalized email.
     */
    public synchronized void checkAndRecord(
            String email,
            String remoteAddress
    ) {

        Instant now = Instant.now();

        String ip = remoteAddress == null
                ? "unknown"
                : remoteAddress;

        String normalizedEmail = email == null
                ? "unknown"
                : email.trim().toLowerCase(Locale.ROOT);

        String ipKey = "IP:" + ip;
        String accountKey = "ACCOUNT:" + normalizedEmail;

        checkLimit(
                ipKey,
                MAX_ATTEMPTS_PER_IP,
                now
        );

        checkLimit(
                accountKey,
                MAX_ATTEMPTS_PER_ACCOUNT,
                now
        );

        record(ipKey, now);
        record(accountKey, now);
    }

    private void checkLimit(
            String key,
            int maximum,
            Instant now
    ) {

        AttemptWindow window = attempts.get(key);

        if (window == null
                || !window.startedAt.plus(WINDOW).isAfter(now)) {
            return;
        }

        if (window.count >= maximum) {

            long retryAfter = Math.max(
                    1,
                    Duration.between(now,
                            window.startedAt.plus(WINDOW))
                            .toSeconds()
            );

            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many login attempts. Please try again in "
                            + retryAfter + " seconds."
            );
        }
    }

    private void record(String key, Instant now) {

        AttemptWindow window = attempts.get(key);

        if (window == null
                || !window.startedAt.plus(WINDOW).isAfter(now)) {

            attempts.put(key, new AttemptWindow(now, 1));

        } else {

            window.count++;
        }

        // Prevent unbounded memory growth.
        while (attempts.size() > MAX_TRACKED_KEYS) {
            String oldestKey =
                    attempts.keySet().iterator().next();

            attempts.remove(oldestKey);
        }
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
