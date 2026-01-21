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
package com.hitorro.example.services;

import com.hitorro.base.objects.BaseDMSService;
import com.hitorro.example.entities.ProductReview;
import com.hitorro.util.startupframework.phases.ServiceDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service to register custom DMS entities for the example application.
 * 
 * <p>This service extends the base DMS functionality by registering custom
 * document types like ProductReview with Hitorro's type system and Hibernate.
 * 
 * <p><b>Usage:</b> Add to hitorro.services.load configuration:
 * <pre>
 * hitorro:
 *   services:
 *     load:
 *       - com.hitorro.base.objects.BaseDMSService
 *       - com.hitorro.example.services.ExampleDMSService
 * </pre>
 * 
 * <p>The {@code typeManagedClasses} array ensures that ProductReview and any
 * other custom entities are registered with:
 * <ul>
 *   <li>Hitorro's TypeManager for soft references and GUID lookups</li>
 *   <li>Hibernate's SessionFactory for persistence</li>
 * </ul>
 */
@ServiceDefinition(
    dependentService = {BaseDMSService.class},
    shortName = "exampledms",
    description = "Example application DMS extensions - registers custom document types",
    debugCommands = {},
    typeManagedClasses = {
        ProductReview.class
        // Add other custom entities here as needed
    },
    uiDirectories = {}
)
public class ExampleDMSService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExampleDMSService.class);
    
    /**
     * Initialize the service.
     * Called during Hitorro service initialization phase.
     * 
     * @param dbInit Whether database initialization should be performed
     * @param upgrading Whether this is an upgrade scenario
     * @param currentVersion Current schema version
     * @param targetVersion Target schema version
     * @return null on success, error message on failure
     */
    public String init(boolean dbInit, final boolean upgrading, final long currentVersion, final long targetVersion) {
        logger.info("Initializing ExampleDMSService");
        logger.info("  - Registering custom DMS entities: ProductReview");
        logger.info("  - dbInit: {}, upgrading: {}", dbInit, upgrading);
        
        // The typeManagedClasses annotation handles entity registration automatically
        // No manual registration needed here
        
        logger.info("✓ ExampleDMSService initialized successfully");
        return null;
    }
    
    /**
     * Run the service.
     * Called after all services are initialized.
     * 
     * @return null on success, error message on failure
     */
    public String run() {
        logger.debug("ExampleDMSService running");
        return null;
    }
    
    /**
     * De-initialize the service.
     * Called during shutdown.
     * 
     * @return null on success, error message on failure
     */
    public String deInit() {
        logger.info("De-initializing ExampleDMSService");
        return null;
    }
    
    /**
     * Start the service.
     * Called after initialization.
     * 
     * @param dbInit Whether database initialization should be performed
     * @return null on success, error message on failure
     */
    public String start(boolean dbInit) {
        logger.debug("ExampleDMSService starting (dbInit: {})", dbInit);
        return null;
    }
}
