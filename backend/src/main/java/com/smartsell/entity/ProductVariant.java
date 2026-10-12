package com.smartsell.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "product_variant")
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(length = 30)
    private String color;

    @Column(name = "color_hex", length = 7)
    private String colorHex;

    @Column(length = 10)
    private String size;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "stock_qty", nullable = false)
    private int stockQty;

    // ราคาเฉพาะไซส์นี้ (เช่น 3XL ขึ้นไปแพงกว่า) — NULL = ใช้ product.price
    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public int getStockQty() { return stockQty; }
    public void setStockQty(int stockQty) { this.stockQty = stockQty; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    /** ราคาที่ขายจริงของ variant นี้ — ที่เดียวที่ตัดสินว่าใช้ราคาไซส์หรือราคาปกติ ห้ามเขียน logic นี้ซ้ำที่อื่น */
    public BigDecimal getEffectivePrice() {
        return price != null ? price : product.getPrice();
    }
}
