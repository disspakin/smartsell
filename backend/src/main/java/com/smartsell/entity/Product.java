package com.smartsell.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 50)
    private String category;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariant> variants = new ArrayList<>();

    // NULL ได้ — สินค้าที่ยังไม่มีตารางไซส์ หน้าเว็บจะซ่อนลิงก์ "ตารางขนาด"
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "size_chart_id")
    private SizeChart sizeChart;

    // MALE / FEMALE / UNISEX / KIDS (ดู TargetGroup) — default ใน DB ทำให้ ddl-auto เพิ่มคอลัมน์ให้ตารางที่มีข้อมูลอยู่แล้วได้
    @Column(name = "target_group", nullable = false, length = 10,
            columnDefinition = "varchar(10) not null default 'UNISEX'")
    private String targetGroup = TargetGroup.UNISEX;



    // ----- getters / setters -----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public List<ProductVariant> getVariants() { return variants; }
    public void setVariants(List<ProductVariant> variants) { this.variants = variants; }

    public SizeChart getSizeChart() { return sizeChart; }
    public void setSizeChart(SizeChart sizeChart) { this.sizeChart = sizeChart; }

    public String getTargetGroup() { return targetGroup; }
    public void setTargetGroup(String targetGroup) { this.targetGroup = targetGroup; }

}
