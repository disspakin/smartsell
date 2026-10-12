package com.smartsell.entity;

import jakarta.persistence.*;
import java.time.LocalTime;

/**
 * เวลาเปิด-ปิดร้านรายวัน day_of_week: 1 = จันทร์ ... 7 = อาทิตย์ (ตรงกับ DayOfWeek.getValue())
 * วันที่ปิด: closed = true และ openTime/closeTime เป็น NULL
 */
@Entity
@Table(name = "store_hours")
public class StoreHours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "day_of_week", nullable = false)
    private Short dayOfWeek;

    @Column(name = "open_time")
    private LocalTime openTime;

    @Column(name = "close_time")
    private LocalTime closeTime;

    @Column(name = "is_closed", nullable = false)
    private boolean closed;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Short getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(Short dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public LocalTime getOpenTime() { return openTime; }
    public void setOpenTime(LocalTime openTime) { this.openTime = openTime; }

    public LocalTime getCloseTime() { return closeTime; }
    public void setCloseTime(LocalTime closeTime) { this.closeTime = closeTime; }

    public boolean isClosed() { return closed; }
    public void setClosed(boolean closed) { this.closed = closed; }
}
