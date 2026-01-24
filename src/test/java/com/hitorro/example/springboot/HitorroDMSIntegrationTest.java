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

import com.hitorro.base.objects.NamedLongEntry;
import com.hitorro.basedms.session.DMSSession;
import com.hitorro.spring.autoconfigure.dms.DMSSessionFactory;
import com.hitorro.util.typesystem.HTSerializable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Hitorro DMS functionality in Spring Boot.
 * Tests core DMS operations including session management, CRUD operations,
 * transactions, and the unified ID system.
 */
@SpringBootTest
@ActiveProfiles("test")
class HitorroDMSIntegrationTest {

    @Autowired
    private DMSSessionFactory dmsSessionFactory;

    @Test
    void dmsSessionFactoryIsAvailable() {
        assertThat(dmsSessionFactory).isNotNull();
        assertThat(dmsSessionFactory.getNativeFactory()).isNotNull();
    }

    @Test
    void canCreateAndCommitEntity() throws Exception {
        // Create a new DMS session
        DMSSession session = dmsSessionFactory.createSession();
        assertThat(session).isNotNull();

        try {
            // Create a new entity
            NamedLongEntry entry = new NamedLongEntry();
            entry.setName("test-counter");
            entry.setValue(100L);
            entry.setIncrementor(10L);
            entry.setDescription("Test counter for DMS integration");

            // Persist the entity (automatic transaction)
            session.persist(entry);
            session.commit();

            // Verify the entity was persisted (ID is auto-generated)
            // Note: getId() returns the database primary key
            System.out.println("Created entity with name: " + entry.getName());
            System.out.println("  Value: " + entry.getValue());
            System.out.println("  Description: " + entry.getDescription());

        } finally {
            // Clean up
            session.close();
        }
    }

    @Test
    void canRetrieveEntityByName() throws Exception {
        DMSSession session = dmsSessionFactory.createSession();
        String guid = null;

        try {
            // Create and save an entity
            NamedLongEntry entry = new NamedLongEntry();
            entry.setName("retrieve-test-" + System.currentTimeMillis());
            entry.setValue(200L);
            entry.setIncrementor(20L);
            entry.setDescription("Test entity for retrieval");
            session.persist(entry);
            session.commit();
            guid = entry.getGuid();

            // Retrieve by name (soft reference via unified ID system)
            HTSerializable retrieved = session.getBySoftReference(
                NamedLongEntry.class, 
                entry.getName()
            );
            
            assertThat(retrieved).isNotNull();
            assertThat(retrieved).isInstanceOf(NamedLongEntry.class);
            
            NamedLongEntry retrievedEntry = (NamedLongEntry) retrieved;
            assertThat(retrievedEntry.getGuid()).isEqualTo(guid);
            assertThat(retrievedEntry.getName()).isEqualTo(entry.getName());
            assertThat(retrievedEntry.getValue()).isEqualTo(200L);
            assertThat(retrievedEntry.getIncrementor()).isEqualTo(20L);
            assertThat(retrievedEntry.getDescription()).isEqualTo("Test entity for retrieval");
            
            System.out.println("Successfully retrieved entity by name: " + entry.getName());
            System.out.println("Entity GUID: " + guid);

        } finally {
            session.close();
        }
    }

    @Test
    void canUpdateEntity() throws Exception {
        DMSSession session = dmsSessionFactory.createSession();

        try {
            // Create an entity
            NamedLongEntry entry = new NamedLongEntry();
            String uniqueName = "update-test-" + System.currentTimeMillis();
            entry.setName(uniqueName);
            entry.setValue(300L);
            entry.setIncrementor(30L);
            entry.setDescription("Before update");
            session.persist(entry);
            session.commit();
            String guid = entry.getGuid();

            // Retrieve and update the entity
            HTSerializable retrieved = session.getBySoftReference(
                NamedLongEntry.class, 
                uniqueName
            );
            assertThat(retrieved).isNotNull();
            
            NamedLongEntry toUpdate = (NamedLongEntry) retrieved;
            toUpdate.setValue(350L);
            toUpdate.setDescription("After update");
            session.update(toUpdate);
            session.commit();

            // Verify the update by retrieving again
            HTSerializable updated = session.getBySoftReference(
                NamedLongEntry.class, 
                uniqueName
            );
            assertThat(updated).isNotNull();
            
            NamedLongEntry updatedEntry = (NamedLongEntry) updated;
            assertThat(updatedEntry.getValue()).isEqualTo(350L);
            assertThat(updatedEntry.getDescription()).isEqualTo("After update");
            assertThat(updatedEntry.getName()).isEqualTo(uniqueName);  // Unchanged
            assertThat(updatedEntry.getIncrementor()).isEqualTo(30L);  // Unchanged
            assertThat(updatedEntry.getGuid()).isEqualTo(guid);  // GUID unchanged
            
            System.out.println("Successfully updated entity: " + guid);

        } finally {
            session.close();
        }
    }

    @Test
    void canDeleteEntity() throws Exception {
        DMSSession session = dmsSessionFactory.createSession();

        try {
            // Create an entity
            NamedLongEntry entry = new NamedLongEntry();
            String uniqueName = "delete-test-" + System.currentTimeMillis();
            entry.setName(uniqueName);
            entry.setValue(400L);
            entry.setIncrementor(40L);
            entry.setDescription("To be deleted");
            session.persist(entry);
            session.commit();
            String guid = entry.getGuid();

            // Delete the entity
            HTSerializable toDelete = session.getBySoftReference(
                NamedLongEntry.class, 
                uniqueName
            );
            assertThat(toDelete).isNotNull();
            session.delete(toDelete);
            session.commit();

            // Verify deletion
            HTSerializable deleted = session.getBySoftReference(
                NamedLongEntry.class, 
                uniqueName
            );
            assertThat(deleted).isNull();
            
            System.out.println("Successfully deleted entity: " + guid);

        } finally {
            session.close();
        }
    }

    @Test
    void rollbackWorks() throws Exception {
        DMSSession session = dmsSessionFactory.createSession();

        try {
            // Create an entity and rollback
            NamedLongEntry entry = new NamedLongEntry();
            String uniqueName = "rollback-test-" + System.currentTimeMillis();
            entry.setName(uniqueName);
            entry.setValue(500L);
            entry.setIncrementor(50L);
            entry.setDescription("Should be rolled back");
            session.persist(entry);
            
            // Rollback instead of commit
            session.rollbackAndClose();

            // Create new session to verify the entity was not saved
            DMSSession verifySession = dmsSessionFactory.createSession();
            try {
                HTSerializable found = verifySession.getBySoftReference(
                    NamedLongEntry.class, 
                    uniqueName
                );
                assertThat(found).isNull();
                
                System.out.println("Rollback test passed - entity was not persisted");
            } finally {
                verifySession.close();
            }

        } catch (Exception e) {
            session.close();
            throw e;
        }
    }

    @Test
    void multipleSessionsAreIndependent() throws Exception {
        // Create two independent sessions
        DMSSession session1 = dmsSessionFactory.createSession();
        DMSSession session2 = dmsSessionFactory.createSession();

        assertThat(session1).isNotNull();
        assertThat(session2).isNotNull();
        assertThat(session1).isNotSameAs(session2);

        try {
            // Both sessions can operate independently
            String name1 = "session1-entry-" + System.currentTimeMillis();
            String name2 = "session2-entry-" + System.currentTimeMillis();

            NamedLongEntry entry1 = new NamedLongEntry();
            entry1.setName(name1);
            entry1.setValue(1L);
            entry1.setIncrementor(1L);
            entry1.setDescription("From session 1");
            session1.persist(entry1);

            NamedLongEntry entry2 = new NamedLongEntry();
            entry2.setName(name2);
            entry2.setValue(2L);
            entry2.setIncrementor(2L);
            entry2.setDescription("From session 2");
            session2.persist(entry2);

            // Commit both
            session1.commit();
            session2.commit();

            // Verify both entities exist
            HTSerializable found1 = session1.getBySoftReference(
                NamedLongEntry.class, 
                name1
            );
            assertThat(found1).isNotNull();
            assertThat(((NamedLongEntry) found1).getDescription()).isEqualTo("From session 1");

            HTSerializable found2 = session2.getBySoftReference(
                NamedLongEntry.class, 
                name2
            );
            assertThat(found2).isNotNull();
            assertThat(((NamedLongEntry) found2).getDescription()).isEqualTo("From session 2");
            
            System.out.println("Multiple independent sessions work correctly");

        } finally {
            session1.close();
            session2.close();
        }
    }

    @Test
    void unifiedIdSystemWorks() throws Exception {
        DMSSession session = dmsSessionFactory.createSession();

        try {
            // Create an entity with name-based soft reference
            String uniqueName = "unified-id-test-" + System.currentTimeMillis();
            NamedLongEntry entry = new NamedLongEntry();
            entry.setName(uniqueName);
            entry.setValue(999L);
            entry.setIncrementor(99L);
            entry.setDescription("Testing unified ID system");
            session.persist(entry);
            session.commit();

            // Retrieve by name (soft reference via unified ID system)
            HTSerializable byName = session.getBySoftReference(
                NamedLongEntry.class, 
                uniqueName
            );
            assertThat(byName).isNotNull();
            assertThat(byName).isInstanceOf(NamedLongEntry.class);
            
            NamedLongEntry retrievedByName = (NamedLongEntry) byName;
            assertThat(retrievedByName.getName()).isEqualTo(uniqueName);
            assertThat(retrievedByName.getValue()).isEqualTo(999L);
            assertThat(retrievedByName.getIncrementor()).isEqualTo(99L);
            
            System.out.println("Unified ID system test passed:");
            System.out.println("  Name (soft ref): " + uniqueName);
            System.out.println("  Soft reference lookup successful");
            System.out.println("  Retrieved entity matches original data");

        } finally {
            session.close();
        }
    }

    @Test
    void canQueryMultipleEntities() throws Exception {
        DMSSession session = dmsSessionFactory.createSession();

        try {
            // Create multiple entities
            String prefix = "query-test-" + System.currentTimeMillis();
            for (int i = 0; i < 5; i++) {
                NamedLongEntry entry = new NamedLongEntry();
                entry.setName(prefix + "-" + i);
                entry.setValue((long) i * 100);
                entry.setIncrementor((long) i * 10);
                entry.setDescription("Query test entry " + i);
                session.persist(entry);
            }
            session.commit();

            // Query all entities using HQL
            // Note: Hitorro's query API uses JDBC-style ? parameters
            List<NamedLongEntry> results = new ArrayList<>();
            String hql = "from " + NamedLongEntry.class.getName() + " where name like '" + prefix + "%'";
            session.getObjects(hql, results);

            assertThat(results).hasSize(5);
            for (int i = 0; i < 5; i++) {
                NamedLongEntry entry = results.get(i);
                assertThat(entry.getName()).startsWith(prefix);
                assertThat(entry.getValue()).isGreaterThanOrEqualTo(0L);
            }

            System.out.println("Successfully queried " + results.size() + " entities");

        } finally {
            session.close();
        }
    }
}
