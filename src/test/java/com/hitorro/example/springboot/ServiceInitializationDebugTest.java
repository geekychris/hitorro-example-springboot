/*
 * Copyright (c) 2006-2025 Chris Collins
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.hitorro.example.springboot;

import com.hitorro.jsontypesystem.JVS;
import com.hitorro.jsontypesystem.propreaders.JVSProperties;
import com.hitorro.spring.autoconfigure.jvs.JsonTypeSystemManager;
import com.hitorro.spring.autoconfigure.service.ServiceContextManager;
import com.hitorro.util.startupframework.ServiceWrapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Minimal debug test to verify what's actually being initialized in Spring Boot context.
 * This test strips away all complexity to show exactly what happens during startup.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Service Initialization Debug Test")
public class ServiceInitializationDebugTest {
    
    @Autowired(required = false)
    private ServiceContextManager serviceContextManager;
    
    @Autowired(required = false)
    private JsonTypeSystemManager jtsManager;
    
    @Test
    @DisplayName("DEBUG: Show exactly what's initialized")
    public void debugShowWhatIsInitialized() {
        System.out.println("\n");
        System.out.println("=".repeat(80));
        System.out.println("SERVICE INITIALIZATION DEBUG REPORT");
        System.out.println("=".repeat(80));
        System.out.println();
        
        // 1. Check ServiceContextManager
        System.out.println("1. ServiceContextManager Status:");
        System.out.println("-".repeat(40));
        if (serviceContextManager != null) {
            System.out.println("✓ ServiceContextManager bean EXISTS");
            System.out.println("  - Initialized: " + serviceContextManager.isInitialized());
            System.out.println("  - Service count: " + serviceContextManager.getAllServices().size());
            System.out.println();
            System.out.println("  Loaded services:");
            for (ServiceWrapper sw : serviceContextManager.getAllServices()) {
                System.out.println("    - " + sw.getShortName() + ": " + sw.getDescription());
            }
        } else {
            System.out.println("✗ ServiceContextManager bean is NULL");
            System.out.println("  This means:");
            System.out.println("  - hitorro.services.enabled is false, OR");
            System.out.println("  - HitorroServiceAutoConfiguration didn't run, OR");
            System.out.println("  - ServiceContextManager bean creation failed");
        }
        System.out.println();
        
        // 2. Check JVSProperties (the critical one!)
        System.out.println("2. JVSProperties Status:");
        System.out.println("-".repeat(40));
        JVS props = JVSProperties.getProperties();
        if (props != null) {
            System.out.println("✓ JVSProperties singleton is INITIALIZED");
            System.out.println("  - Properties object: " + props.getClass().getName());
            
            // Try to access some common properties
            try {
                String servertype = props.getString("servertype", null);
                System.out.println("  - servertype: " + (servertype != null ? servertype : "(not set)"));
            } catch (Exception e) {
                System.out.println("  - servertype: (error accessing: " + e.getMessage() + ")");
            }
            
            // Check if it's empty by trying to get the underlying node
            try {
                System.out.println("  - Properties object exists: true");
            } catch (Exception e) {
                System.out.println("  - Properties object: (unknown)");
            }
        } else {
            System.out.println("✗ JVSProperties singleton is NULL");
            System.out.println("  This means:");
            System.out.println("  - ServiceContextManager.initializeHitorroProperties() was NOT called, OR");
            System.out.println("  - ServiceContextManager.afterPropertiesSet() was NOT called, OR");
            System.out.println("  - Phase 0 initialization failed silently");
        }
        System.out.println();
        
        // 3. Check JsonTypeSystemManager
        System.out.println("3. JsonTypeSystemManager Status:");
        System.out.println("-".repeat(40));
        if (jtsManager != null) {
            System.out.println("✓ JsonTypeSystemManager bean EXISTS");
            System.out.println("  - Can access JTS: " + (jtsManager.getJsonTypeSystem() != null));
            
            // Check for type definitions
            try {
                boolean hasArticle = jtsManager.hasType("article");
                boolean hasUserProfile = jtsManager.hasType("user_profile");
                System.out.println("  - Type 'article' loaded: " + hasArticle);
                System.out.println("  - Type 'user_profile' loaded: " + hasUserProfile);
            } catch (Exception e) {
                System.out.println("  - Error checking types: " + e.getMessage());
            }
        } else {
            System.out.println("✗ JsonTypeSystemManager bean is NULL");
            System.out.println("  This means:");
            System.out.println("  - hitorro.jvs.enabled is false, OR");
            System.out.println("  - JVSAutoConfiguration didn't run");
        }
        System.out.println();
        
        // 4. Check System Properties
        System.out.println("4. System Properties (Environment):");
        System.out.println("-".repeat(40));
        String htBin = System.getProperty("HT_BIN");
        String htHome = System.getProperty("HT_HOME");
        System.out.println("  - HT_BIN: " + (htBin != null ? htBin : "(not set)"));
        System.out.println("  - HT_HOME: " + (htHome != null ? htHome : "(not set)"));
        System.out.println();
        
        // 5. Summary
        System.out.println("=".repeat(80));
        System.out.println("SUMMARY:");
        System.out.println("-".repeat(40));
        boolean fullyInitialized = serviceContextManager != null 
            && serviceContextManager.isInitialized() 
            && props != null;
        
        if (fullyInitialized) {
            System.out.println("✓ FULL STACK IS INITIALIZED");
            System.out.println("  - ServiceContextManager: OK");
            System.out.println("  - JVSProperties: OK");
            System.out.println("  - Services loaded: " + serviceContextManager.getAllServices().size());
        } else {
            System.out.println("✗ FULL STACK IS NOT INITIALIZED");
            if (serviceContextManager == null) {
                System.out.println("  PROBLEM: ServiceContextManager bean missing");
            } else if (!serviceContextManager.isInitialized()) {
                System.out.println("  PROBLEM: ServiceContextManager not initialized");
            }
            if (props == null) {
                System.out.println("  PROBLEM: JVSProperties not initialized");
            }
        }
        System.out.println("=".repeat(80));
        System.out.println();
        
        // Assert for test framework
        assertThat(serviceContextManager)
            .as("ServiceContextManager should be autowired")
            .isNotNull();
    }
    
    @Test
    @DisplayName("DEBUG: Check initialization order")
    public void debugInitializationOrder() {
        System.out.println("\n");
        System.out.println("INITIALIZATION ORDER CHECK:");
        System.out.println("-".repeat(40));
        
        // The order SHOULD be:
        // 1. ServiceContextManager bean created
        // 2. ServiceContextManager.afterPropertiesSet() called
        //    a. Phase 0: initializeHitorroProperties() -> Sets JVSProperties
        //    b. Phase 1: loadServicesFromConfiguration()
        //    c. Phase 2: validateConfigKeys()
        //    d. Phase 3: serviceContext.init()
        // 3. Other beans created (JTS, DMS, etc.)
        
        System.out.println("Expected order:");
        System.out.println("  1. ServiceContextManager.afterPropertiesSet()");
        System.out.println("     -> Phase 0: JVSProperties.setDefaultProperties()");
        System.out.println("  2. JsonTypeSystemManager created");
        System.out.println("  3. Tests run");
        System.out.println();
        
        JVS props = JVSProperties.getProperties();
        
        System.out.println("Actual state:");
        System.out.println("  - ServiceContextManager present: " + (serviceContextManager != null));
        System.out.println("  - JVSProperties initialized: " + (props != null));
        
        if (serviceContextManager != null && props == null) {
            System.out.println();
            System.out.println("⚠ WARNING: ServiceContextManager exists but JVSProperties is null!");
            System.out.println("  This suggests Phase 0 didn't run or failed silently.");
            System.out.println("  Check application startup logs for:");
            System.out.println("  - 'Phase 0: Initializing JVSProperties'");
            System.out.println("  - Any errors during property initialization");
        }
        
        System.out.println();
    }
    
    @Test
    @DisplayName("Verify JVSProperties can be accessed")
    public void verifyJVSPropertiesAccessible() {
        JVS props = JVSProperties.getProperties();
        
        if (props == null) {
            System.out.println("\n❌ TEST FAILED: JVSProperties is NULL");
            System.out.println("This confirms that ServiceContextManager is NOT initializing JVSProperties");
        } else {
            System.out.println("\n✓ TEST PASSED: JVSProperties is initialized");
        }
        
        assertThat(props)
            .as("JVSProperties should be initialized by ServiceContextManager Phase 0")
            .isNotNull();
    }
}
