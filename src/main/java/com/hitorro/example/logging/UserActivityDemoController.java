/*
 * Copyright (c) 2026 Hitorro Project
 * Licensed under the MIT License
 */

package com.hitorro.example.logging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Demo REST controller showcasing structured logging for user activities.
 *
 * <p>This controller provides example endpoints that demonstrate how to
 * use the {@link UserActivityLogLogger} to log various user activities.</p>
 *
 * <p>Available endpoints:</p>
 * <ul>
 *   <li>POST /api/demo/login - Simulate user login</li>
 *   <li>POST /api/demo/logout - Simulate user logout</li>
 *   <li>GET /api/demo/data - Simulate API access (logs automatically)</li>
 * </ul>
 *
 * <p>To enable, set: {@code hitorro.structured-logging.enabled=true}</p>
 */
@RestController
@RequestMapping("/api/demo")
@ConditionalOnProperty(prefix = "hitorro.structured-logging", name = "enabled", havingValue = "true")
public class UserActivityDemoController {

    private final UserActivityLogLogger activityLogger;

    @Autowired
    public UserActivityDemoController(UserActivityLogLogger activityLogger) {
        this.activityLogger = activityLogger;
    }

    /**
     * Demo endpoint for simulating user login.
     *
     * <p>Example request:</p>
     * <pre>
     * POST /api/demo/login
     * Content-Type: application/json
     *
     * {
     *   "userId": "user123",
     *   "username": "john.doe"
     * }
     * </pre>
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(
            @RequestBody Map<String, String> credentials,
            HttpServletRequest request) {

        String userId = credentials.get("userId");
        String username = credentials.get("username");
        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        // Log the login event
        activityLogger.logLogin(userId, username, ipAddress, userAgent);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Login event logged to Kafka topic: user-events");
        response.put("userId", userId);

        return ResponseEntity.ok(response);
    }

    /**
     * Demo endpoint for simulating user logout.
     *
     * <p>Example request:</p>
     * <pre>
     * POST /api/demo/logout
     * Content-Type: application/json
     *
     * {
     *   "userId": "user123",
     *   "username": "john.doe",
     *   "sessionId": "sess-abc-123"
     * }
     * </pre>
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestBody Map<String, String> data) {

        String userId = data.get("userId");
        String username = data.get("username");
        String sessionId = data.get("sessionId");

        // Log the logout event
        activityLogger.logLogout(userId, username, sessionId);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Logout event logged to Kafka topic: user-events");
        response.put("userId", userId);

        return ResponseEntity.ok(response);
    }

    /**
     * Demo endpoint that logs API access automatically.
     *
     * <p>Example request:</p>
     * <pre>
     * GET /api/demo/data?userId=user123
     * </pre>
     */
    @GetMapping("/data")
    public ResponseEntity<Map<String, Object>> getData(
            @RequestParam(required = false, defaultValue = "anonymous") String userId,
            HttpServletRequest request) {

        long startTime = System.currentTimeMillis();

        // Simulate some work
        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", System.currentTimeMillis());
        data.put("data", "Sample data from API");
        data.put("userId", userId);

        long responseTime = System.currentTimeMillis() - startTime;

        // Log the API access
        activityLogger.logApiAccess(
                userId,
                request.getRequestURI(),
                request.getMethod(),
                200,
                responseTime
        );

        data.put("message", "API access logged to Kafka topic: user-events");

        return ResponseEntity.ok(data);
    }

    /**
     * Info endpoint describing the structured logging demo.
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("description", "Structured Logging Demo Endpoints");
        info.put("kafkaTopic", "user-events");
        info.put("logSchema", "log-configs/user_activity_log.json");

        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("POST /api/demo/login", "Log a user login event");
        endpoints.put("POST /api/demo/logout", "Log a user logout event");
        endpoints.put("GET /api/demo/data", "Access data (logs API access event)");

        info.put("endpoints", endpoints);

        Map<String, String> exampleLogin = new HashMap<>();
        exampleLogin.put("userId", "user123");
        exampleLogin.put("username", "john.doe");
        info.put("exampleLoginBody", exampleLogin);

        return ResponseEntity.ok(info);
    }

    /**
     * Extracts the client's IP address from the request.
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}