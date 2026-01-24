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

import com.hitorro.basedms.session.DMSSession;
import com.hitorro.example.entities.ProductReview;
import com.hitorro.spring.autoconfigure.dms.DMSSessionFactory;
import com.hitorro.util.typesystem.HTSerializable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration tests for ProductReview entity in the Hitorro DMS.
 * 
 * <p>This test class demonstrates how to test custom DMS entities and validates
 * that the ProductReview entity properly integrates with the DMS infrastructure.
 * 
 * <p><b>Tests Cover:</b>
 * <ul>
 *   <li>CRUD operations (Create, Retrieve, Update, Delete)</li>
 *   <li>Product-specific field validation</li>
 *   <li>Versioning support</li>
 *   <li>Query capabilities</li>
 *   <li>Transaction management</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
class ProductReviewDMSTest {

    @Autowired
    private DMSSessionFactory dmsSessionFactory;

    @Test
    void dmsSessionFactoryIsAvailable() {
        assertThat(dmsSessionFactory).isNotNull();
        assertThat(dmsSessionFactory.getNativeFactory()).isNotNull();
    }

    @Test
    void canCreateAndPersistProductReview() throws Exception {
        DMSSession session = dmsSessionFactory.createSession();
        assertThat(session).isNotNull();

        try {
            // Create a new ProductReview
            ProductReview review = new ProductReview();
            review.setTitle("Excellent Laptop for Development");
            review.setProductName("ThinkPad X1 Carbon");
            review.setRating(5);
            review.setReviewerName("John Developer");
            review.setPros("Lightweight, excellent keyboard, great battery life");
            review.setCons("Expensive, limited ports");
            review.setVerified(true);
            review.setNote("Purchased for software development work");

            // Persist the review
            session.persist(review);
            session.commit();

            // Verify the review was persisted
            assertThat(review.getId()).isNotNull();
            assertThat(review.getGuid()).isNotNull();
            
            System.out.println("Created ProductReview with ID: " + review.getId());
            System.out.println("  Product: " + review.getProductName());
            System.out.println("  Rating: " + review.getRating() + "/5");
            System.out.println("  Reviewer: " + review.getReviewerName());
            System.out.println("  Verified: " + review.getVerified());

        } finally {
            session.close();
        }
    }

    @Test
    void canRetrieveProductReviewById() throws Exception {
        DMSSession session = dmsSessionFactory.createSession();
        Long reviewId = null;

        try {
            // Create and save a review
            ProductReview review = new ProductReview();
            review.setTitle("Great Headphones");
            review.setProductName("Sony WH-1000XM5");
            review.setRating(4);
            review.setReviewerName("Audio Enthusiast");
            review.setPros("Excellent noise cancellation, comfortable");
            review.setCons("Price could be lower");
            review.setVerified(true);
            
            session.persist(review);
            session.commit();
            reviewId = review.getId();

            // Retrieve by ID
            ProductReview retrieved = (ProductReview) session.getSingleObjectById(
                ProductReview.class, 
                reviewId
            );
            
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getId()).isEqualTo(reviewId);
            assertThat(retrieved.getProductName()).isEqualTo("Sony WH-1000XM5");
            assertThat(retrieved.getRating()).isEqualTo(4);
            assertThat(retrieved.getReviewerName()).isEqualTo("Audio Enthusiast");
            assertThat(retrieved.getVerified()).isTrue();
            assertThat(retrieved.getPros()).contains("noise cancellation");
            assertThat(retrieved.getCons()).contains("Price");
            
            System.out.println("Successfully retrieved ProductReview by ID: " + reviewId);
            System.out.println("  Title: " + retrieved.getTitle());

        } finally {
            session.close();
        }
    }

    @Test
    void canUpdateProductReview() throws Exception {
        DMSSession session = dmsSessionFactory.createSession();

        try {
            // Create a review
            ProductReview review = new ProductReview();
            String uniqueProduct = "Product-" + System.currentTimeMillis();
            review.setTitle("Initial Review");
            review.setProductName(uniqueProduct);
            review.setRating(3);
            review.setReviewerName("Test Reviewer");
            review.setPros("Initial pros");
            review.setCons("Initial cons");
            review.setVerified(false);
            
            session.persist(review);
            session.commit();
            Long reviewId = review.getId();

            // Retrieve and update
            ProductReview toUpdate = (ProductReview) session.getSingleObjectById(
                ProductReview.class, 
                reviewId
            );
            assertThat(toUpdate).isNotNull();
            
            toUpdate.setTitle("Updated Review After More Use");
            toUpdate.setRating(4);
            toUpdate.setPros("Updated pros - even better than expected");
            toUpdate.setCons("Updated cons - minor issues resolved");
            toUpdate.setVerified(true);
            
            session.update(toUpdate);
            session.commit();

            // Verify the update
            ProductReview updated = (ProductReview) session.getSingleObjectById(
                ProductReview.class, 
                reviewId
            );
            
            assertThat(updated).isNotNull();
            assertThat(updated.getTitle()).isEqualTo("Updated Review After More Use");
            assertThat(updated.getRating()).isEqualTo(4);
            assertThat(updated.getPros()).contains("even better");
            assertThat(updated.getCons()).contains("resolved");
            assertThat(updated.getVerified()).isTrue();
            assertThat(updated.getProductName()).isEqualTo(uniqueProduct); // Unchanged
            
            System.out.println("Successfully updated ProductReview: " + reviewId);
            System.out.println("  Rating changed: 3 -> 4");
            System.out.println("  Verified changed: false -> true");

        } finally {
            session.close();
        }
    }

    @Test
    void canDeleteProductReview() throws Exception {
        DMSSession session = dmsSessionFactory.createSession();

        try {
            // Create a review
            ProductReview review = new ProductReview();
            review.setTitle("Review to Delete");
            review.setProductName("Temporary Product");
            review.setRating(3);
            review.setReviewerName("Temp Reviewer");
            review.setPros("Test");
            review.setCons("Test");
            review.setVerified(false);
            
            session.persist(review);
            session.commit();
            Long reviewId = review.getId();

            // Delete the review
            ProductReview toDelete = (ProductReview) session.getSingleObjectById(
                ProductReview.class, 
                reviewId
            );
            assertThat(toDelete).isNotNull();
            
            session.delete(toDelete);
            session.commit();

            // Verify deletion
            ProductReview deleted = (ProductReview) session.getSingleObjectById(
                ProductReview.class, 
                reviewId
            );
            assertThat(deleted).isNull();
            
            System.out.println("Successfully deleted ProductReview: " + reviewId);

        } finally {
            session.close();
        }
    }

    @Test
    void ratingValidationWorks() {
        ProductReview review = new ProductReview();
        review.setProductName("Test Product");
        
        // Valid ratings should work
        review.setRating(1);
        assertThat(review.getRating()).isEqualTo(1);
        
        review.setRating(5);
        assertThat(review.getRating()).isEqualTo(5);
        
        // Invalid ratings should throw exception
        assertThrows(IllegalArgumentException.class, () -> {
            review.setRating(0);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            review.setRating(6);
        });
        
        System.out.println("Rating validation working correctly (1-5 range enforced)");
    }

    @Test
    void canCreateVersionOfProductReview() throws Exception {
        DMSSession session = dmsSessionFactory.createSession();

        try {
            // Create original review
            ProductReview original = new ProductReview();
            original.setTitle("Version Test Review");
            original.setProductName("Versioned Product");
            original.setRating(3);
            original.setReviewerName("Version Tester");
            original.setPros("Original pros");
            original.setCons("Original cons");
            original.setVerified(false);
            
            session.persist(original);
            session.commit();
            Long originalId = original.getId();
            String originalGuid = original.getGuid();

            // Create a new version
            ProductReview newVersion = (ProductReview) original.createMajorVersion();
            assertThat(newVersion).isNotNull();
            assertThat(newVersion).isNotSameAs(original);
            
            // Verify version fields were copied
            assertThat(newVersion.getProductName()).isEqualTo("Versioned Product");
            assertThat(newVersion.getRating()).isEqualTo(3);
            assertThat(newVersion.getReviewerName()).isEqualTo("Version Tester");
            
            // Update the new version
            newVersion.setRating(4);
            newVersion.setPros("Updated pros in new version");
            newVersion.setTitle("Updated Version Test Review");
            
            session.persist(newVersion);
            session.commit();
            Long newVersionId = newVersion.getId();

            // Verify both versions exist
            assertThat(newVersionId).isNotEqualTo(originalId);
            
            ProductReview retrievedOriginal = (ProductReview) session.getSingleObjectById(
                ProductReview.class, 
                originalId
            );
            ProductReview retrievedNew = (ProductReview) session.getSingleObjectById(
                ProductReview.class, 
                newVersionId
            );
            
            assertThat(retrievedOriginal).isNotNull();
            assertThat(retrievedNew).isNotNull();
            assertThat(retrievedOriginal.getRating()).isEqualTo(3);
            assertThat(retrievedNew.getRating()).isEqualTo(4);
            
            System.out.println("Successfully created version of ProductReview:");
            System.out.println("  Original ID: " + originalId + ", Rating: 3");
            System.out.println("  New Version ID: " + newVersionId + ", Rating: 4");
            System.out.println("  Version Label: " + newVersion.getVersionLabel());

        } finally {
            session.close();
        }
    }

    @Test
    void canQueryMultipleProductReviews() throws Exception {
        DMSSession session = dmsSessionFactory.createSession();

        try {
            // Create multiple reviews
            String prefix = "query-test-" + System.currentTimeMillis();
            for (int i = 0; i < 3; i++) {
                ProductReview review = new ProductReview();
                review.setTitle(prefix + " Review " + i);
                review.setProductName(prefix + "-product-" + i);
                review.setRating(i + 3); // 3, 4, 5
                review.setReviewerName("Reviewer " + i);
                review.setPros("Pros for product " + i);
                review.setCons("Cons for product " + i);
                review.setVerified(i % 2 == 0);
                session.persist(review);
            }
            session.commit();

            // Query all reviews with the prefix
            List<ProductReview> results = new ArrayList<>();
            String hql = "from " + ProductReview.class.getName() + 
                        " where productName like '" + prefix + "%'";
            session.getObjects(hql, results);

            assertThat(results).hasSize(3);
            
            // Verify all reviews were found
            for (int i = 0; i < 3; i++) {
                ProductReview review = results.get(i);
                assertThat(review.getProductName()).startsWith(prefix);
                assertThat(review.getRating()).isGreaterThanOrEqualTo(3);
                assertThat(review.getRating()).isLessThanOrEqualTo(5);
            }

            System.out.println("Successfully queried " + results.size() + " ProductReviews");
            System.out.println("  Ratings: " + results.stream()
                .map(r -> r.getRating().toString())
                .reduce((a, b) -> a + ", " + b)
                .orElse(""));

        } finally {
            session.close();
        }
    }

    @Test
    void rollbackWorksForProductReview() throws Exception {
        DMSSession session = dmsSessionFactory.createSession();

        try {
            // Create a review and rollback
            ProductReview review = new ProductReview();
            String uniqueProduct = "rollback-test-" + System.currentTimeMillis();
            review.setTitle("Should Be Rolled Back");
            review.setProductName(uniqueProduct);
            review.setRating(3);
            review.setReviewerName("Rollback Tester");
            review.setPros("Test");
            review.setCons("Test");
            review.setVerified(false);
            
            session.persist(review);
            
            // Rollback instead of commit
            session.rollbackAndClose();

            // Create new session to verify the review was not saved
            DMSSession verifySession = dmsSessionFactory.createSession();
            try {
                List<ProductReview> results = new ArrayList<>();
                String hql = "from " + ProductReview.class.getName() + 
                            " where productName = '" + uniqueProduct + "'";
                verifySession.getObjects(hql, results);
                
                assertThat(results).isEmpty();
                
                System.out.println("Rollback test passed - ProductReview was not persisted");
            } finally {
                verifySession.close();
            }

        } catch (Exception e) {
            session.close();
            throw e;
        }
    }

    @Test
    void productReviewInheritsDocumentFeatures() throws Exception {
        DMSSession session = dmsSessionFactory.createSession();

        try {
            // Create a review
            ProductReview review = new ProductReview();
            review.setTitle("Feature Inheritance Test");
            review.setProductName("Test Product");
            review.setRating(4);
            review.setReviewerName("Feature Tester");
            review.setPros("Good");
            review.setCons("Not bad");
            review.setVerified(true);
            
            // Set Document-inherited fields
            review.setNote("This is a test note from Document");
            
            session.persist(review);
            session.commit();
            Long reviewId = review.getId();

            // Retrieve and verify Document fields work
            ProductReview retrieved = (ProductReview) session.getSingleObjectById(
                ProductReview.class, 
                reviewId
            );
            
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getNote()).isEqualTo("This is a test note from Document");
            assertThat(retrieved.getCreator()).isNotNull(); // DMS sets creator automatically
            assertThat(retrieved.getGuid()).isNotNull();
            assertThat(retrieved.getCreationDate()).isNotNull();
            
            System.out.println("ProductReview successfully inherits Document features:");
            System.out.println("  GUID: " + retrieved.getGuid());
            System.out.println("  Creator: " + retrieved.getCreator());
            System.out.println("  Created: " + retrieved.getCreationDate());

        } finally {
            session.close();
        }
    }
}
