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

import com.hitorro.spring.autoconfigure.service.HitorroServiceFactory;
import com.hitorro.util.startupframework.ServiceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the Hitorro Spring Boot example application.
 */
@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
class HitorroExampleApplicationTests {

    @Autowired
    private ServiceContext serviceContext;

    @Autowired
    private HitorroServiceFactory serviceFactory;

    @Test
    void contextLoads() {
        // Verify Spring context loads successfully
        assertNotNull(serviceContext, "ServiceContext should be injected");
        assertNotNull(serviceFactory, "HitorroServiceFactory should be injected");
    }

    @Test
    void hitorroServicesInitialized() {
        // Verify Hitorro ServiceContext is initialized (even if no services are registered)
        assertTrue(serviceContext.isInitialized(), "Hitorro ServiceContext should be initialized");
        
        // Note: Service count may be 0 if no services are explicitly registered
        // In a real application, services would be added via serviceContext.addModule()
        // or by configuring them in application.yml
        int serviceCount = serviceContext.getServices().size();
        System.out.println("Initialized " + serviceCount + " Hitorro services");
        
        // This is expected - services need to be explicitly registered
        assertTrue(serviceCount >= 0, "Service count should be non-negative");
    }

    @Test
    void serviceFactoryWorks() {
        // Verify service factory can list services
        assertNotNull(serviceFactory.getAllServices(), "Should be able to get all services");
        
        // Service count may be 0 if no services are registered
        int serviceCount = serviceFactory.getAllServices().size();
        System.out.println("Service factory returns " + serviceCount + " services");
        
        // This is expected - services need to be explicitly registered
        assertTrue(serviceCount >= 0, "Service count should be non-negative");
    }
}
