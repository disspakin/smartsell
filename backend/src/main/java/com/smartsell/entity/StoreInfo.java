package com.smartsell.entity;

import jakarta.persistence.*;

/**
 * ข้อมูลร้านแบบ key/label/value เช่น location / ที่ตั้งร้าน / ตึก 15 ...
 * หัวข้อเพิ่มได้ใน DB โดยไม่ต้องแก้โค้ด — chatbot และ footer ใช้ทุกแถว ไม่อ้าง info_key ตรงๆ
 */
@Entity
@Table(name = "store_info")
public class StoreInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "info_key", nullable = false)
    private String infoKey;

    @Column(nullable = false)
    private String label;

    @Column(name = "info_value", nullable = false, columnDefinition = "TEXT")
    private String infoValue;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getInfoKey() { return infoKey; }
    public void setInfoKey(String infoKey) { this.infoKey = infoKey; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getInfoValue() { return infoValue; }
    public void setInfoValue(String infoValue) { this.infoValue = infoValue; }
}
