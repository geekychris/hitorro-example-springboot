/*
 * Copyright (c) 2026 Hitorro Project
 * Licensed under the MIT License
 */

package com.hitorro.example.logging;

import com.hitorro.spring.autoconfigure.logging.StructuredLogger;
import com.hitorro.spring.autoconfigure.logging.StructuredLoggerProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * Structured logger for user activity events.
 * 
 * <p>This logger publishes user activity events to Kafka topic "user-events".
 * Events are defined by the schema in log-configs/user-activity-log.json.</p>
 * 
 * <p>Example usage:</p>
 * <pre>
 * {@literal @}Autowired
 * private UserActivityLogger activityLogger;
 * 
 * // Log a login event
 * activityLogger.logLogin("user123", "john.doe", "192.168.1.1", "Mozilla/5.0...");
 * 
 * // Log an API access
 * activityLogger.logApiAccess("user123", "/api/documents", "GET", 200, 45L);
 * </pre>
 */
@Component
@ConditionalOnProperty(prefix = "hitorro.structured-logging", name = "enabled", havingValue = "true")
public class UserActivityLogger extends StructuredLogger {
    
    public UserActivityLogger(StructuredLoggerProperties properties) {
        super(properties, "user-events");
    }
    
    /**
     * Logs a user login event.
     * 
     * @param userId the user's unique identifier
     * @param username the username
     * @param ipAddress the user's IP address
     * @param userAgent the browser user agent
     */
    public void logLogin(String userId, String username, String ipAddress, String userAgent) {
        Map<String, Object> event = createBaseEvent(userId, username, "LOGIN");
        event.put("ip_address", ipAddress);
        event.put("user_agent", userAgent);
        publish(event);
    }
    
    /**
     * Logs a user logout event.
     * 
     * @param userId the user's unique identifier
     * @param username the username
     * @param sessionId the session identifier
     */
    public void logLogout(String userId, String username, String sessionId) {
        Map<String, Object> event = createBaseEvent(userId, username, "LOGOUT");
        event.put("session_id", sessionId);
        publish(event);
    }
    
    /**
     * Logs an API access event.
     * 
     * @param userId the user's unique identifier
     * @param endpoint the API endpoint accessed
     * @param httpMethod the HTTP method used
     * @param statusCode the HTTP status code returned
     * @param responseTimeMs the response time in milliseconds
     */
    public void logApiAccess(String userId, String endpoint, String httpMethod, 
                             int statusCode, Long responseTimeMs) {
        Map<String, Object> event = createBaseEvent(userId, null, "API_ACCESS");
        event.put("endpoint", endpoint);
        event.put("http_method", httpMethod);
        event.put("status_code", statusCode);
        event.put("response_time_ms", responseTimeMs);
        publish(event);
    }
    
    /**
     * Logs an API access event with full details.
     * 
     * @param userId the user's unique identifier
     * @param username the username
     * @param endpoint the API endpoint accessed
     * @param httpMethod the HTTP method used
     * @param statusCode the HTTP status code returned
     * @param responseTimeMs the response time in milliseconds
     * @param ipAddress the user's IP address
     * @param sessionId the session identifier
     */
    public void logApiAccessDetailed(String userId, String username, String endpoint, 
                                     String httpMethod, int statusCode, Long responseTimeMs,
                                     String ipAddress, String sessionId) {
        Map<String, Object> event = createBaseEvent(userId, username, "API_ACCESS");
        event.put("endpoint", endpoint);
        event.put("http_method", httpMethod);
        event.put("status_code", statusCode);
        event.put("response_time_ms", responseTimeMs);
        event.put("ip_address", ipAddress);
        event.put("session_id", sessionId);
        publish(event);
    }
    
    /**
     * Logs a generic user activity event with custom metadata.
     * 
     * @param userId the user's unique identifier
     * @param username the username
     * @param eventType the type of event
     * @param metadata additional metadata as key-value pairs
     */
    public void logCustomEvent(String userId, String username, String eventType, 
                               Map<String, String> metadata) {
        Map<String, Object> event = createBaseEvent(userId, username, eventType);
        if (metadata != null && !metadata.isEmpty()) {
            event.put("metadata", metadata);
        }
        publish(event);
    }
    
    /**
     * Creates a base event map with common fields.
     * 
     * @param userId the user's unique identifier
     * @param username the username (optional)
     * @param eventType the type of event
     * @return a map containing base event fields
     */
    private Map<String, Object> createBaseEvent(String userId, String username, String eventType) {
        Map<String, Object> event = new HashMap<>();
        Instant now = Instant.now();
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        
        event.put("user_id", userId);
        if (username != null) {
            event.put("username", username);
        }
        event.put("event_type", eventType);
        event.put("event_date", today.toString());
        event.put("timestamp", now.toString());
        
        return event;
    }
}
