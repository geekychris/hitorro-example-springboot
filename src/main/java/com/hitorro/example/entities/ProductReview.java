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
package com.hitorro.example.entities;

import com.hitorro.base.objects.Document;
import com.hitorro.base.objects.VersionableObject;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.typesystem.HTObjectInputStream;
import com.hitorro.util.typesystem.HTObjectOutputStream;

import jakarta.persistence.*;

import java.io.IOException;

/**
 * ProductReview entity - extends the DMS Document class to add product review specific fields.
 * 
 * <p>This class demonstrates how to extend the Hitorro Document Management System with
 * custom document types. It includes all necessary JPA and Hitorro type system annotations
 * for proper persistence and integration with the DMS.
 * 
 * <p><b>Key Features:</b>
 * <ul>
 *   <li>Extends Document to inherit versioning, content management, and categorization</li>
 *   <li>Adds product-specific fields: rating, reviewer name, pros/cons, verification status</li>
 *   <li>Implements Hitorro serialization for cross-platform compatibility</li>
 *   <li>Supports schema versioning for future upgrades</li>
 * </ul>
 * 
 * @see Document
 * @see VersionableObject
 */
@Entity
@Table(name = "product_review")
@PrimaryKeyJoinColumn(name = "system_id")
@com.hitorro.util.typesystem.annotation.TypeClassMetaInfo(
    shortTypeName = "ProductReview",
    isView = false,
    isPersisted = true,
    schemaVersion = ProductReview.SerializationVersion,
    softLinkField = "productName"
)
@com.hitorro.util.typesystem.annotation.UiTypeProperties(
    name = "Product Review"
)
public class ProductReview extends Document {
    
    public static final int SerializationVersion = 1;
    
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;
    
    @Column(name = "rating", nullable = false)
    private Integer rating;
    
    @Column(name = "reviewer_name", length = 255)
    private String reviewerName;
    
    @Lob
    @Column(name = "pros")
    private String pros;
    
    @Lob
    @Column(name = "cons")
    private String cons;
    
    @Column(name = "verified", nullable = false)
    private Boolean verified = false;
    
    /**
     * Default constructor required by JPA.
     */
    public ProductReview() {
        super();
        this.verified = false;
    }
    
    /**
     * Copy constructor for versioning support.
     * Called when creating new versions of the document.
     */
    @Override
    public void copy(VersionableObject orig) {
        super.copy(orig);
        if (orig instanceof ProductReview) {
            ProductReview other = (ProductReview) orig;
            this.productName = other.productName;
            this.rating = other.rating;
            this.reviewerName = other.reviewerName;
            this.pros = other.pros;
            this.cons = other.cons;
            this.verified = other.verified;
        }
    }
    
    // ========================================================================
    // Getters and Setters with UI Annotations
    // ========================================================================
    
    @com.hitorro.util.typesystem.annotation.UiProperties(
        displayName = "Product Name",
        displayType = com.hitorro.util.typesystem.annotation.UiProperties.TextFieldDisplay,
        order = 30
    )
    @com.hitorro.util.typesystem.annotation.FullTextAttributeMetaInfo(
        displayName = "productName",
        isFullTextIndexable = true,
        luceneIndexingFilters = "STANDARD,CASE,PORTERSTEM",
        luceneFieldName = "productName",
        stringLiteral = false,
        allField = true
    )
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    @com.hitorro.util.typesystem.annotation.UiProperties(
        displayName = "Rating",
        displayType = com.hitorro.util.typesystem.annotation.UiProperties.TextFieldDisplay,
        order = 40
    )
    public Integer getRating() {
        return rating;
    }
    
    public void setRating(Integer rating) {
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        this.rating = rating;
    }
    
    @com.hitorro.util.typesystem.annotation.UiProperties(
        displayName = "Reviewer Name",
        displayType = com.hitorro.util.typesystem.annotation.UiProperties.TextFieldDisplay,
        order = 50
    )
    public String getReviewerName() {
        return reviewerName;
    }
    
    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }
    
    @com.hitorro.util.typesystem.annotation.UiProperties(
        displayName = "Pros",
        displayType = com.hitorro.util.typesystem.annotation.UiProperties.TextAreaDisplay,
        order = 60
    )
    @com.hitorro.util.typesystem.annotation.FullTextAttributeMetaInfo(
        displayName = "pros",
        isFullTextIndexable = true,
        luceneIndexingFilters = "STANDARD,CASE,PORTERSTEM",
        luceneFieldName = "pros",
        stringLiteral = false,
        allField = true
    )
    public String getPros() {
        return pros;
    }
    
    public void setPros(String pros) {
        this.pros = pros;
    }
    
    @com.hitorro.util.typesystem.annotation.UiProperties(
        displayName = "Cons",
        displayType = com.hitorro.util.typesystem.annotation.UiProperties.TextAreaDisplay,
        order = 70
    )
    @com.hitorro.util.typesystem.annotation.FullTextAttributeMetaInfo(
        displayName = "cons",
        isFullTextIndexable = true,
        luceneIndexingFilters = "STANDARD,CASE,PORTERSTEM",
        luceneFieldName = "cons",
        stringLiteral = false,
        allField = true
    )
    public String getCons() {
        return cons;
    }
    
    public void setCons(String cons) {
        this.cons = cons;
    }
    
    @com.hitorro.util.typesystem.annotation.UiProperties(
        displayName = "Verified Purchase",
        displayType = com.hitorro.util.typesystem.annotation.UiProperties.TextFieldDisplay,
        order = 80
    )
    public Boolean getVerified() {
        return verified;
    }
    
    public void setVerified(Boolean verified) {
        this.verified = verified != null ? verified : false;
    }
    
    // ========================================================================
    // Hitorro Serialization Support
    // ========================================================================
    
    /**
     * Serialize this object to the Hitorro object output stream.
     * This enables cross-platform serialization and storage.
     */
    @Override
    public void serialize(HTObjectOutputStream os) throws IOException, StoreException {
        os.writeInt(getSerializationVersion());
        super.serialize(os);
        
        // Version 1 fields
        os.writeString(productName);
        os.writeInt(rating != null ? rating : 0);
        os.writeString(reviewerName);
        os.writeString(pros);
        os.writeString(cons);
        os.writeBoolean(verified != null ? verified : false);
    }
    
    /**
     * Deserialize this object from the Hitorro object input stream.
     * Supports multiple schema versions for backward compatibility.
     */
    @Override
    public void deserialize(HTObjectInputStream os) 
            throws IOException, ClassNotFoundException, StoreException {
        int version = os.readInt();
        super.deserialize(os);
        
        switch (version) {
            case 1:
                productName = os.readString();
                rating = os.readInt();
                reviewerName = os.readString();
                pros = os.readString();
                cons = os.readString();
                verified = os.readBoolean();
                break;
            default:
                throw new IOException("Unknown ProductReview version: " + version);
        }
    }
    
    /**
     * Handle schema upgrades when the serialization version changes.
     * This method is called during system upgrades to migrate existing data.
     * 
     * @param currentSchemaVersion The current schema version in the database
     * @return true if upgrade was performed, false otherwise
     */
    @Override
    public boolean upgradeAllInstances(long currentSchemaVersion) {
        switch ((int) currentSchemaVersion) {
            case 1:
                // Future upgrade logic would go here
                // Example: upgrade 1->2
                return true;
            default:
                return false;
        }
    }
    
    @Override
    public String toString() {
        return "ProductReview{" +
                "id=" + getId() +
                ", productName='" + productName + '\'' +
                ", rating=" + rating +
                ", reviewerName='" + reviewerName + '\'' +
                ", verified=" + verified +
                ", title='" + getTitle() + '\'' +
                '}';
    }
}
