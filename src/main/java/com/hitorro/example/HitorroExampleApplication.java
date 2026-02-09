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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

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
     * 
     * Resolution order (highest to lowest priority):
     * 1. System property (-DHT_BIN=path, -DHT_HOME=path)
     * 2. Environment variable (HT_BIN, HT_HOME)
     * 3. Default: parent directory of the application location
     */
    private static void configureHitorroSystemProperties() {
        Path defaultHitorroPath = getDefaultHitorroPath();
        configureHtBin(defaultHitorroPath);
        configureHtHome(defaultHitorroPath);
    }
    
    /**
     * Determine the default Hitorro directory path based on application location.
     * 
     * For development (classes in target/classes): uses parent of project directory
     * For JAR deployment: uses parent of JAR directory
     * Fallback: uses parent of current working directory
     */
    private static Path getDefaultHitorroPath() {
        try {
            // Get the location of this class
            URL classLocation = HitorroExampleApplication.class.getProtectionDomain()
                    .getCodeSource().getLocation();
            Path classPath = Paths.get(classLocation.toURI());
            
            // classPath is either:
            // - target/classes (development)
            // - path/to/app.jar (packaged)
            Path appParentDir;
            if (classPath.toString().endsWith(".jar")) {
                // Running from JAR: go up two levels (jar -> jar-dir -> parent)
                appParentDir = classPath.getParent().getParent();
            } else {
                // Running from classes (development): go up from target/classes to project's parent
                // target/classes -> target -> project -> project-parent
                appParentDir = classPath.getParent().getParent().getParent();
            }
            
            System.out.println("[HITORRO] Default Hitorro directory: " + appParentDir);
            return appParentDir;
            
        } catch (URISyntaxException | NullPointerException | SecurityException e) {
            // Fallback to parent of current working directory
            System.err.println("[HITORRO] WARNING: Could not determine application location: " + e.getMessage());
            Path fallback = Paths.get(System.getProperty("user.dir")).getParent();
            System.err.println("[HITORRO]   Falling back to: " + fallback);
            return fallback;
        }
    }
    
    private static void configureHtBin(Path defaultPath) {
        if (System.getProperty("HT_BIN") != null) {
            System.out.println("[HITORRO] HT_BIN set via system property: " + System.getProperty("HT_BIN"));
            return;
        }
        
        String htBin = System.getenv("HT_BIN");
        if (htBin != null) {
            System.setProperty("HT_BIN", htBin);
            System.out.println("[HITORRO] HT_BIN set via environment: " + htBin);
            return;
        }
        
        // Use default path relative to application location
        String resolvedPath = toCanonicalPath(defaultPath);
        System.setProperty("HT_BIN", resolvedPath);
        System.err.println("[HITORRO] WARNING: HT_BIN not configured, using default: " + resolvedPath);
        System.err.println("[HITORRO]   Override via: -DHT_BIN=/path or export HT_BIN=/path");
        
        // Validate the default path has expected structure
        File configDir = new File(resolvedPath, "config");
        if (!configDir.exists() || !configDir.isDirectory()) {
            System.err.println("[HITORRO] WARNING: Expected 'config' directory not found at: " + configDir.getAbsolutePath());
            System.err.println("[HITORRO]   The application may fail to start. Please configure HT_BIN correctly.");
        }
    }
    
    private static void configureHtHome(Path defaultPath) {
        if (System.getProperty("HT_HOME") != null) {
            System.out.println("[HITORRO] HT_HOME set via system property: " + System.getProperty("HT_HOME"));
            return;
        }
        
        String htHome = System.getenv("HT_HOME");
        if (htHome != null) {
            System.setProperty("HT_HOME", htHome);
            System.out.println("[HITORRO] HT_HOME set via environment: " + htHome);
            return;
        }
        
        // Use default path relative to application location
        String resolvedPath = toCanonicalPath(defaultPath);
        System.setProperty("HT_HOME", resolvedPath);
        System.err.println("[HITORRO] WARNING: HT_HOME not configured, using default: " + resolvedPath);
        System.err.println("[HITORRO]   Override via: -DHT_HOME=/path or export HT_HOME=/path");
        
        // Create the directory if it doesn't exist
        File htHomeDir = new File(resolvedPath);
        if (!htHomeDir.exists()) {
            if (htHomeDir.mkdirs()) {
                System.out.println("[HITORRO] Created HT_HOME directory: " + resolvedPath);
            } else {
                System.err.println("[HITORRO] WARNING: Failed to create HT_HOME directory: " + resolvedPath);
            }
        }
    }
    
    /**
     * Convert a Path to canonical string form.
     */
    private static String toCanonicalPath(Path path) {
        try {
            return path.toFile().getCanonicalPath();
        } catch (IOException e) {
            return path.toAbsolutePath().toString();
        }
    }
}
