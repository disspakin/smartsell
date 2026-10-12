package com.smartsell.entity;

import jakarta.persistence.*;

/**
 * ตารางไซส์หนึ่งแบบ เช่น "T-Shirt คอกลม" — สินค้าหลายตัวใช้ตารางเดียวกันได้ (product.size_chart_id)
 * แถวของแต่ละไซส์อยู่ใน SizeChartRow
 */
@Entity
@Table(name = "size_chart")
public class SizeChart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    // ตัวเลขในตารางยังเป็นค่าประมาณ ร้านยังไม่ยืนยัน — modal แสดงข้อความเตือนใต้ตาราง
    @Column(name = "is_estimated", nullable = false)
    private boolean estimated;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isEstimated() { return estimated; }
    public void setEstimated(boolean estimated) { this.estimated = estimated; }
}
