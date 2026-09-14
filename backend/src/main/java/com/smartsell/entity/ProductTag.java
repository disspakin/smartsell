package com.smartsell.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "product_tag")
public class ProductTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "tag_type", nullable = false, length = 30)
    private String tagType; // SEASON | OCCASION | LUCKY_COLOR

    @Column(name = "tag_value", nullable = false, length = 50)
    private String tagValue; // e.g. "Winter", "Formal", "Navy"

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public String getTagType() { return tagType; }
    public void setTagType(String tagType) { this.tagType = tagType; }

    public String getTagValue() { return tagValue; }
    public void setTagValue(String tagValue) { this.tagValue = tagValue; }
}
