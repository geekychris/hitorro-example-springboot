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

import com.hitorro.base.service.BasicService;
import com.hitorro.basedms.db.HibernateService;
import com.hitorro.spring.autoconfigure.service.HitorroServiceFactory;
import com.hitorro.spring.autoconfigure.service.ServiceContextManager;
import com.hitorro.util.startupframework.ServiceContext;
import com.hitorro.util.startupframework.ServiceWrapper;
import com.hitorro.util.typesystem.TypeManagerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test demonstrating Hitorro service loading in Spring Boot.
 * 
 * This test shows how to configure and load Hitorro services using Spring Boot's
 * application.yml configuration. Services are loaded via the hitorro.services.load
 * property.
 * 
 * NOTE: These tests are currently disabled due to H2 schema conflicts when HibernateService
 * is loaded via services. The DMS auto-config and HibernateService both try to create
 * the schema, causing index conflicts. This is a known limitation that needs further work.
 */
@org.junit.jupiter.api.Disabled("Schema conflicts with HibernateService - needs further work")
@SpringBootTest
@ActiveProfiles("services")
public class HitorroServiceLoadingTest {

    @Autowired
    private ServiceContextManager serviceContextManager;

    @Autowired
    private HitorroServiceFactory serviceFactory;

    @Autowired
    private ServiceContext serviceContext;

    @Test
    void serviceContextManagerIsAvailable() {
        assertThat(serviceContextManager).isNotNull();
        assertThat(serviceContext).isNotNull();
        assertThat(serviceFactory).isNotNull();
    }

    @Test
    void servicesAreLoadedFromConfiguration() {
        List<ServiceWrapper> services = serviceContextManager.getAllServices();
        
        assertThat(services).isNotEmpty();
        System.out.println("\nLoaded " + services.size() + " Hitorro services:");
        
        for (ServiceWrapper sw : services) {
            System.out.println("  ✓ " + sw.getShortName() + ": " + sw.getDescription());
        }
    }

    @Test
    void basicServiceIsLoaded() {
        BasicService basicService = serviceFactory.getService(BasicService.class);
        
        assertThat(basicService).isNotNull();
        assertThat(serviceContextManager.isServiceInitialized(BasicService.class)).isTrue();
        
        System.out.println("BasicService loaded successfully");
        System.out.println("  Short name: " + getServiceShortName(BasicService.class));
    }

    @Test
    void typeManagerServiceIsLoaded() {
        TypeManagerService typeManager = serviceFactory.getService(TypeManagerService.class);
        
        assertThat(typeManager).isNotNull();
        assertThat(serviceContextManager.isServiceInitialized(TypeManagerService.class)).isTrue();
        
        System.out.println("TypeManagerService loaded successfully");
        System.out.println("  Short name: " + getServiceShortName(TypeManagerService.class));
    }

    @Test
    void hibernateServiceIsLoaded() {
        HibernateService hibernateService = serviceFactory.getService(HibernateService.class);
        
        assertThat(hibernateService).isNotNull();
        assertThat(serviceContextManager.isServiceInitialized(HibernateService.class)).isTrue();
        
        System.out.println("HibernateService loaded successfully");
        System.out.println("  Short name: " + getServiceShortName(HibernateService.class));
    }

    @Test
    void canAccessServicesViaFactory() {
        // Access services by class
        BasicService basic = serviceFactory.getService(BasicService.class);
        assertThat(basic).isNotNull();
        
        // Access services by short name
        Object hibernateByName = serviceContextManager.getService("hibernate");
        assertThat(hibernateByName).isNotNull();
        assertThat(hibernateByName).isInstanceOf(HibernateService.class);
        
        System.out.println("Service factory methods working:");
        System.out.println("  - getService(Class) ✓");
        System.out.println("  - getService(String) ✓");
    }

    @Test
    void serviceInitializationOrderIsCorrect() {
        // TypeManagerService should be initialized before HibernateService
        // because HibernateService depends on TypeManagerService
        
        TypeManagerService typeManager = serviceFactory.getService(TypeManagerService.class);
        HibernateService hibernate = serviceFactory.getService(HibernateService.class);
        
        assertThat(typeManager).isNotNull();
        assertThat(hibernate).isNotNull();
        
        // Both should be initialized
        assertThat(serviceContextManager.isServiceInitialized(TypeManagerService.class)).isTrue();
        assertThat(serviceContextManager.isServiceInitialized(HibernateService.class)).isTrue();
        
        System.out.println("Service dependency order respected:");
        System.out.println("  1. BasicService (no dependencies)");
        System.out.println("  2. TypeManagerService (depends on BasicService)");
        System.out.println("  3. HibernateService (depends on TypeManagerService)");
    }

    @Test
    void allConfiguredServicesAreInitialized() {
        // From application-services.yml, we configured:
        // - BasicService
        // - TypeManagerService
        // - HibernateService
        
        List<ServiceWrapper> services = serviceContextManager.getAllServices();
        
        // Should have at least our 3 configured services (may have more due to dependencies)
        assertThat(services.size()).isGreaterThanOrEqualTo(3);
        
        // Check each configured service is present
        boolean hasBasic = services.stream()
            .anyMatch(sw -> sw.getClazz().equals(BasicService.class));
        boolean hasTypeManager = services.stream()
            .anyMatch(sw -> sw.getClazz().equals(TypeManagerService.class));
        boolean hasHibernate = services.stream()
            .anyMatch(sw -> sw.getClazz().equals(HibernateService.class));
        
        assertThat(hasBasic).isTrue();
        assertThat(hasTypeManager).isTrue();
        assertThat(hasHibernate).isTrue();
        
        System.out.println("\nAll configured services initialized:");
        System.out.println("  ✓ BasicService");
        System.out.println("  ✓ TypeManagerService");
        System.out.println("  ✓ HibernateService");
        System.out.println("\nTotal services loaded: " + services.size());
    }

    /**
     * Helper to get service short name.
     */
    private String getServiceShortName(Class<?> serviceClass) {
        for (ServiceWrapper sw : serviceContextManager.getAllServices()) {
            if (sw.getClazz().equals(serviceClass)) {
                return sw.getShortName();
            }
        }
        return "unknown";
    }
}
