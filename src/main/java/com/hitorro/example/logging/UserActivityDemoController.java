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
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Demo REST controller showcasing formatted structured logging for user activities.
 * 
 * <p>This controller provides example endpoints that demonstrate how to
 * use the {@link UserActivityLogLogger} to log various user activities.</p>
 * 
 * <p>Available endpoints:</p>
 * <ul>
 *   <li>POST /api/demo/login - Log user login with full details</li>
 *   <li>POST /api/demo/logout - Log user logout</li>
 *   <li>POST /api/demo/api-access - Log API access with request/response details</li>
 *   <li>POST /api/demo/custom - Log custom event with flexible fields</li>
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
     * Demo endpoint for logging user login with full details.
     * 
     * <p>Example request:</p>
     * <pre>
     * POST /api/demo/login
     * Content-Type: application/json
     * 
     * {
     *   "userId": "user123",
     *   "username": "john.doe",
     *   "sessionId": "sess-abc-123"
     * }
     * </pre>
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody Map<String, String> credentials,
            HttpServletRequest request) {
        
        String userId = credentials.get("userId");
        String username = credentials.get("username");
        String sessionId = credentials.get("sessionId");
        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        
        // Log the login event with timestamp
        activityLogger.log(
            userId,
            username,
            "LOGIN",
            LocalDate.now(),
            Instant.now(),
            ipAddress,
            userAgent,
            "/login",  // endpoint
            "POST",  // httpMethod
            200,    // statusCode
            null,   // responseTimeMs
            sessionId, null  // metadata
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Login event logged to Kafka topic: user-events");
        response.put("userId", userId);
        response.put("username", username);
        response.put("timestamp", Instant.now().toString());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Demo endpoint for logging user logout.
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
    public ResponseEntity<Map<String, Object>> logout(@RequestBody Map<String, String> data) {
        
        String userId = data.get("userId");
        String username = data.get("username");
        String sessionId = data.get("sessionId");
        String ipAddress = data.getOrDefault("ipAddress", getClientIpInfo());
        
        // Log the logout event
        activityLogger.log(
            userId,
            username,
            "LOGOUT",
            LocalDate.now(),
            Instant.now(),
            ipAddress,
            null,  // userAgent
            "/logout",  // endpoint
            "POST",  // httpMethod
            200,    // statusCode
            null,   // responseTimeMs
            sessionId, null  // metadata
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Logout event logged to Kafka topic: user-events");
        response.put("userId", userId);
        response.put("username", username);
        response.put("timestamp", Instant.now().toString());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Demo endpoint that logs API access with full request/response details.
     * 
     * <p>Accessed via GET: /api/demo/data?userId=user123</p>
     */
    @GetMapping("/api-access")
    public ResponseEntity<Map<String, Object>> getApiAccess(
            @RequestParam(required = false, defaultValue = "anonymous") String userId,
            HttpServletRequest request) {
        
        long startTime = System.currentTimeMillis();
        
        // Simulate some processing
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("timestamp", Instant.now().toString());
        responseBody.put("data", "Sample data from API");
        responseBody.put("userId", userId);
        responseBody.put("message", "API access logged to Kafka topic: user-events");
        
        long responseTime = System.currentTimeMillis() - startTime;
        
        // Log the API access with full details
        activityLogger.log(
            userId,
            null,  // username
            "API_ACCESS",
            LocalDate.now(),
            Instant.now(),
            getClientIp(request),
            request.getHeader("User-Agent"),
            request.getRequestURI(),  // endpoint
            request.getMethod(),   // httpMethod
            200,                    // statusCode
            responseTime,              // responseTimeMs
            request.getSession(false).getId(),  // sessionId
            null    // metadata
        );
        
        responseBody.put("responseTimeMs", responseTime);
        responseBody.put("endpoint", request.getRequestURI());
        responseBody.put("httpMethod", request.getMethod());
        responseBody.put("ipAddress", getClientIp(request));
        
        return ResponseEntity.ok(responseBody);
    }
    
    /**
     * Generic endpoint to log custom user activity events.
     * 
     * <p>Example request:</p>
     * <pre>
     * POST /api/demo/custom
     * Content-Type: application/json
     * 
     * {
     *   "userId": "user123",
     *   "username": "john.doe",
     *   "eventType": "PAGE_VIEW",
     *   "endpoint": "/products/laptop",
     *   "httpMethod": "GET",
     *   "statusCode": 200,
     *   "responseTimeMs": 125,
     *   "sessionId": "sess-abc-123",
     *   "ipAddress": "127.0.0.1",
     *   "userAgent": "Mozilla/5.0...",
     *   "metadata": {
     *     "page": "/products",
     *     "referrer": "/search"
     *   }
     * }
     * </pre>
     */
    @PostMapping("/custom")
    public ResponseEntity<Map<String, Object>> logCustomEvent(@RequestBody Map<String, Object> data) {
        
        String userId = (String) data.get("userId");
        String username = (String) data.get("username");
        String eventType = (String) data.getOrDefault("eventType", "CUSTOM_EVENT");
        String endpoint = (String) data.get("endpoint");
        String httpMethod = (String) data.getOrDefault("httpMethod", "GET");
        Integer statusCode = data.get("statusCode") != null ? 
            Integer.valueOf(data.get("statusCode").toString()) : 200;
        Long responseTimeMs = data.get("responseTimeMs") != null ? 
            Long.valueOf(data.get("responseTimeMs").toString()) : null;
        String sessionId = (String) data.get("sessionId");
        String ipAddress = (String) data.get("ipAddress");
        String userAgent = (String) data.get("userAgent");
        
        @SuppressWarnings("unchecked")
        Map<String, String> metadata = data.get("metadata") != null ? 
            (Map<String, String>) data.get("metadata") : null;
        
        try {
            // Parse timestamp if provided
            Instant timestamp = data.get("timestamp") != null ? 
                Instant.parse(data.get("timestamp").toString()) : Instant.now();
            LocalDate eventDate = data.get("eventDate") != null ? 
                LocalDate.parse(data.get("eventDate").toString()) : LocalDate.now();
            
            // Log the custom event
            activityLogger.log(
                userId,
                username,
                eventType,
                eventDate,
                timestamp,
                ipAddress,
                userAgent,
                endpoint,
                httpMethod,
                statusCode,
                responseTimeMs,
                sessionId,
                metadata
            );
        } catch (Exception e) {
            // Fallback to current time if timestamp parsing fails
            activityLogger.log(
                userId,
                username,
                eventType,
                LocalDate.now(),
                Instant.now(),
                ipAddress,
                userAgent,
                endpoint,
                httpMethod,
                statusCode,
                responseTimeMs,
                sessionId,
                metadata
            );
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Custom event logged to Kafka topic: user-events");
        response.put("eventType", eventType);
        response.put("timestamp", Instant.now().toString());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Endpoint to describe available fields and their expected types.
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("description", "User Activity Structured Logging Demo");
        info.put("kafkaTopic", "user-events");
        info.put("tableDestination", "analytics_logs.user_activity_log");
        
        Map<String, Object> fields = new HashMap<>();
        fields.put("user_id", "String (required) - Unique identifier for the user");
        fields.put("username", "String (optional) - Username/Display name");
        fields.put("event_type", "String (required) - Type of user activity event");
        fields.put("event_date", "LocalDate (required) - Event date for partitioning (YYYY-MM-DD)");
        fields.put("timestamp", "Instant (required) - Event timestamp in ISO-8601 format");
        fields.put("ip_address", "String (optional) - IP address of the client");
        fields.put("user_agent", "String (optional) - Browser user agent string");
        fields.put("endpoint", "String (optional) - API endpoint or page URL");
        fields.put("http_method", "String (optional) - HTTP method (GET, POST, DELETE, etc.)");
        fields.put("status_code", "Integer (optional) - HTTP status code (e.g., 200, 404, 500)");
        fields.put("response_time_ms", "Long (optional) - Response time in milliseconds");
        fields.put("session_id", "String (optional) - Session identifier");
        fields.put("metadata", "Map<String, String> (optional) - Additional metadata as key-value pairs");
        info.put("fields", fields);
        
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("POST /api/demo/login", "Log user login event");
        endpoints.put("POST /api/demo/logout", "Log user logout event");
        endpoints.put("GET /api/demo/api-access", "Log API access automatically");
        endpoints.put("POST /api/demo/custom", "Log custom event with all fields");
        info.put("endpoints", endpoints);
        
        Map<String, Object> exampleLogin = new HashMap<>();
        exampleLogin.put("userId", "user123");
        exampleLogin.put("username", "john.doe");
        exampleLogin.put("sessionId", "sess-abc-123");
        info.put("exampleLogin", exampleLogin);
        
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
        // Handle multiple IPs in X-Forwarded-For
        if (ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
    
    /**
     * Helper method to get client IP from a map (for custom events)
     */
    private String getClientIpInfo() {
        return "127.0.0.1"; // Default for custom events
    }
}