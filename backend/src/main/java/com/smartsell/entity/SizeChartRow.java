package com.smartsell.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * หนึ่งแถวในตารางไซส์ = หนึ่งไซส์ หน่วยเป็นนิ้ว
 * ลำดับที่แสดงใช้ sort_order เพราะเรียงตามตัวอักษรจะได้ 2XL, 3XL, ... ก่อน S
 */
@Entity
@Table(name = "size_chart_row")
public class SizeChartRow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "size_chart_id", nullable = false)
    private SizeChart sizeChart;

    @Column(nullable = false, length = 10)
    private String size;

    @Column(name = "chest_in", nullable = false, precision = 4, scale = 1)
    private BigDecimal chestIn;

    @Column(name = "length_in", nullable = false, precision = 4, scale = 1)
    private BigDecimal lengthIn;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SizeChart getSizeChart() { return sizeChart; }
    public void setSizeChart(SizeChart sizeChart) { this.sizeChart = sizeChart; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public BigDecimal getChestIn() { return chestIn; }
    public void setChestIn(BigDecimal chestIn) { this.chestIn = chestIn; }

    public BigDecimal getLengthIn() { return lengthIn; }
    public void setLengthIn(BigDecimal lengthIn) { this.lengthIn = lengthIn; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
