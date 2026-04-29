package com.backend.comment.wall.service;

import com.backend.comment.wall.exception.RateLimitExceededException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class RateLimiterService {

    private static final int MAX_REQUESTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final Map<String, Deque<Instant>> requestsByIp = new ConcurrentHashMap<>();

    public void validateRequest(String ipAddress) {
        Instant now = Instant.now();
        Deque<Instant> timestamps = requestsByIp.computeIfAbsent(ipAddress, key -> new ConcurrentLinkedDeque<>());

        synchronized (timestamps) {
            Instant threshold = now.minus(WINDOW);
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(threshold)) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= MAX_REQUESTS) {
                throw new RateLimitExceededException("Rate limit exceeded. Maximum 5 requests per minute.");
            }

            timestamps.addLast(now);
        }
    }
}
