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

import com.hitorro.base.objects.Document;
import com.hitorro.base.objects.Folder;
import com.hitorro.basedms.session.DMSSession;
import com.hitorro.spring.autoconfigure.dms.DMSSessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Folder hierarchy functionality.
 * Tests that folders can contain subfolders and documents, and that
 * folders can be linked to multiple parents (many-to-many relationship).
 */
@SpringBootTest
@ActiveProfiles("test")
public class FolderHierarchyIntegrationTest {

    @Autowired
    private DMSSessionFactory sessionFactory;

    private DMSSession session;

    @BeforeEach
    public void setUp() {
        session = sessionFactory.createSession();
    }

    @Test
    public void testCreateFolderHierarchy() throws Exception {
        // Create root folder
        Folder root = new Folder();
        root.setName("root");
        root.setDescription("Root folder");
        root.setIsRootLevel(true);
        session.persist(root);

        // Create subfolder
        Folder subfolder = new Folder();
        subfolder.setName("subfolder");
        subfolder.setDescription("Subfolder under root");
        subfolder.setIsRootLevel(false);
        subfolder.addContainer(root); // Link to parent
        session.persist(subfolder);

        // Create sub-subfolder
        Folder subsubfolder = new Folder();
        subsubfolder.setName("subsubfolder");
        subsubfolder.setDescription("Subfolder under subfolder");
        subsubfolder.setIsRootLevel(false);
        subsubfolder.addContainer(subfolder); // Link to parent
        session.persist(subsubfolder);

        session.commit();

        // Verify hierarchy
        assertNotNull(root.getId());
        assertNotNull(subfolder.getId());
        assertNotNull(subsubfolder.getId());

        // Verify relationships
        Set<com.hitorro.base.objects.Container> subfolderContainers = subfolder.getContainers();
        assertEquals(1, subfolderContainers.size());
        assertTrue(subfolderContainers.contains(root));

        Set<com.hitorro.base.objects.Container> subsubfolderContainers = subsubfolder.getContainers();
        assertEquals(1, subsubfolderContainers.size());
        assertTrue(subsubfolderContainers.contains(subfolder));

        System.out.println("✓ Successfully created 3-level folder hierarchy");
    }

    @Test
    public void testMultipleParentFolders() throws Exception {
        // Create two parent folders
        Folder parent1 = new Folder();
        parent1.setName("parent1");
        parent1.setDescription("First parent");
        parent1.setIsRootLevel(true);
        session.persist(parent1);

        Folder parent2 = new Folder();
        parent2.setName("parent2");
        parent2.setDescription("Second parent");
        parent2.setIsRootLevel(true);
        session.persist(parent2);

        // Create a child folder linked to BOTH parents
        Folder child = new Folder();
        child.setName("shared_child");
        child.setDescription("Folder with multiple parents");
        child.setIsRootLevel(false);
        child.addContainer(parent1);
        child.addContainer(parent2);
        session.persist(child);

        session.commit();

        // Verify the child has two parents
        Set<com.hitorro.base.objects.Container> childContainers = child.getContainers();
        assertEquals(2, childContainers.size());
        assertTrue(childContainers.contains(parent1));
        assertTrue(childContainers.contains(parent2));

        System.out.println("✓ Successfully created folder with multiple parents (many-to-many relationship)");
    }

    @Test
    public void testDocumentsInFolders() throws Exception {
        // Create folder
        Folder folder = new Folder();
        folder.setName("docs_folder");
        folder.setDescription("Folder for documents");
        folder.setIsRootLevel(true);
        session.persist(folder);

        // Create documents in folder
        Document doc1 = new Document();
        doc1.setTitle("document1.txt");
        doc1.setNote("First document");
        doc1.addContainer(folder);
        session.persist(doc1);

        Document doc2 = new Document();
        doc2.setTitle("document2.txt");
        doc2.setNote("Second document");
        doc2.addContainer(folder);
        session.persist(doc2);

        session.commit();

        // Query documents in folder
        folder.setSession(session);
        List<com.hitorro.base.objects.VersionableObject> folderContents = folder.getList();

        assertNotNull(folderContents);
        assertEquals(2, folderContents.size());
        
        // Verify both documents are in the list
        boolean foundDoc1 = false;
        boolean foundDoc2 = false;
        for (com.hitorro.base.objects.VersionableObject obj : folderContents) {
            if (obj instanceof Document) {
                Document doc = (Document) obj;
                if (doc.getTitle().equals("document1.txt")) foundDoc1 = true;
                if (doc.getTitle().equals("document2.txt")) foundDoc2 = true;
            }
        }
        
        assertTrue(foundDoc1, "document1.txt should be in folder");
        assertTrue(foundDoc2, "document2.txt should be in folder");

        System.out.println("✓ Successfully stored and retrieved 2 documents from folder");
    }

    @Test
    public void testDocumentInMultipleFolders() throws Exception {
        // Create two folders
        Folder folder1 = new Folder();
        folder1.setName("folder1");
        folder1.setDescription("First folder");
        folder1.setIsRootLevel(true);
        session.persist(folder1);

        Folder folder2 = new Folder();
        folder2.setName("folder2");
        folder2.setDescription("Second folder");
        folder2.setIsRootLevel(true);
        session.persist(folder2);

        // Create a document that belongs to BOTH folders
        Document sharedDoc = new Document();
        sharedDoc.setTitle("shared_document.txt");
        sharedDoc.setNote("Document in multiple folders");
        sharedDoc.addContainer(folder1);
        sharedDoc.addContainer(folder2);
        session.persist(sharedDoc);

        session.commit();

        // Verify document appears in both folders
        folder1.setSession(session);
        folder2.setSession(session);

        List<com.hitorro.base.objects.VersionableObject> folder1Contents = folder1.getList();
        List<com.hitorro.base.objects.VersionableObject> folder2Contents = folder2.getList();

        assertEquals(1, folder1Contents.size());
        assertEquals(1, folder2Contents.size());

        // Verify it's the same document
        Set<com.hitorro.base.objects.Container> docContainers = sharedDoc.getContainers();
        assertEquals(2, docContainers.size());
        assertTrue(docContainers.contains(folder1));
        assertTrue(docContainers.contains(folder2));

        System.out.println("✓ Successfully created document in multiple folders");
    }

    @Test
    public void testComplexHierarchy() throws Exception {
        // Create complex hierarchy:
        //   /root
        //     /projects
        //       /project_a
        //         doc_a1.txt
        //         doc_a2.txt
        //       /project_b
        //         doc_b1.txt
        //     /shared (also links to project_a)
        //       doc_shared.txt

        Folder root = new Folder();
        root.setName("root");
        root.setIsRootLevel(true);
        session.persist(root);

        Folder projects = new Folder();
        projects.setName("projects");
        projects.addContainer(root);
        session.persist(projects);

        Folder projectA = new Folder();
        projectA.setName("project_a");
        projectA.addContainer(projects);
        session.persist(projectA);

        Folder projectB = new Folder();
        projectB.setName("project_b");
        projectB.addContainer(projects);
        session.persist(projectB);

        Folder shared = new Folder();
        shared.setName("shared");
        shared.addContainer(root);
        shared.addContainer(projectA); // Multiple parents!
        session.persist(shared);

        // Add documents
        Document docA1 = new Document();
        docA1.setTitle("doc_a1.txt");
        docA1.addContainer(projectA);
        session.persist(docA1);

        Document docA2 = new Document();
        docA2.setTitle("doc_a2.txt");
        docA2.addContainer(projectA);
        session.persist(docA2);

        Document docB1 = new Document();
        docB1.setTitle("doc_b1.txt");
        docB1.addContainer(projectB);
        session.persist(docB1);

        Document docShared = new Document();
        docShared.setTitle("doc_shared.txt");
        docShared.addContainer(shared);
        docShared.addContainer(projectA); // In multiple folders!
        session.persist(docShared);

        session.commit();

        // Verify structure
        projects.setSession(session);
        projectA.setSession(session);
        projectB.setSession(session);
        shared.setSession(session);

        // Project A should have 3 documents (2 direct + 1 shared)
        List<com.hitorro.base.objects.VersionableObject> projectAContents = projectA.getList();
        long docCount = projectAContents.stream().filter(obj -> obj instanceof Document).count();
        assertEquals(3, docCount, "project_a should have 3 documents");

        // Project B should have 1 document
        List<com.hitorro.base.objects.VersionableObject> projectBContents = projectB.getList();
        docCount = projectBContents.stream().filter(obj -> obj instanceof Document).count();
        assertEquals(1, docCount, "project_b should have 1 document");

        // Shared folder should have 1 document
        List<com.hitorro.base.objects.VersionableObject> sharedContents = shared.getList();
        docCount = sharedContents.stream().filter(obj -> obj instanceof Document).count();
        assertEquals(1, docCount, "shared folder should have 1 document");

        // Verify shared folder has 2 parents
        assertEquals(2, shared.getContainers().size());

        System.out.println("✓ Successfully created and verified complex multi-parent hierarchy");
        System.out.println("  /root");
        System.out.println("    /projects");
        System.out.println("      /project_a (3 docs)");
        System.out.println("      /project_b (1 doc)");
        System.out.println("    /shared (1 doc, linked to both root and project_a)");
    }
}
