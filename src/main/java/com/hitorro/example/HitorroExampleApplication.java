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
package com.hitorro.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

/**
 * Example Spring Boot application demonstrating Hitorro integration.
 * 
 * This application showcases:
 * - Automatic Hitorro service initialization
 * - Command execution via REST endpoints
 * - Property integration
 * - Multiple CLI access modes
 * - DMS (Document Management System) integration
 */
@SpringBootApplication
@EntityScan(basePackages = {
    "com.hitorro.base.objects",      // Hitorro base entities (NamedLongEntry, etc.)
    "com.hitorro.basedms",           // Hitorro DMS entities
    "com.hitorro.example",           // Application entities (if any)
    "com.hitorro.example.entities"   // Custom DMS entities (ProductReview, etc.)
})
public class HitorroExampleApplication {

    public static void main(String[] args) {
        // CRITICAL: Configure HT_BIN and HT_HOME BEFORE Spring Boot starts
        // These are required for Hitorro's property system and type definitions
        configureHitorroSystemProperties();
        
        SpringApplication.run(HitorroExampleApplication.class, args);
    }
    
    /**
     * Configure required Hitorro system properties before Spring initialization.
     * Must be called before SpringApplication.run().
     */
    private static void configureHitorroSystemProperties() {
        if (System.getProperty("HT_BIN") == null) {
            String htBin = System.getenv("HT_BIN");
            if (htBin == null) {
                // Default to project directory if not set
                htBin = "/Users/chris/hitorro";
                System.err.println("WARNING: HT_BIN not configured. Using default: " + htBin);
                System.err.println("Set via: -DHT_BIN=/path or export HT_BIN=/path");
            }
            System.setProperty("HT_BIN", htBin);
            System.out.println("HT_BIN configured: " + htBin);
        } else {
            System.out.println("HT_BIN already set: " + System.getProperty("HT_BIN"));
        }
        
        if (System.getProperty("HT_HOME") == null) {
            String htHome = System.getenv("HT_HOME");
            if (htHome == null) {
                // Default to home directory if not set
                htHome = "/Users/chris/hthome";
                System.err.println("WARNING: HT_HOME not configured. Using default: " + htHome);
                System.err.println("Set via: -DHT_HOME=/path or export HT_HOME=/path");
            }
            System.setProperty("HT_HOME", htHome);
            System.out.println("HT_HOME configured: " + htHome);
        } else {
            System.out.println("HT_HOME already set: " + System.getProperty("HT_HOME"));
        }
    }
}
