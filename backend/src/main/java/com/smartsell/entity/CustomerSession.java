package com.smartsell.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_session")
public class CustomerSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user; // nullable — anonymous shoppers allowed

    @Column(name = "personal_color", length = 20)
    private String personalColor; // Spring / Summer / Autumn / Winter

    @Column(length = 30)
    private String occasion;

    @Column(name = "budget_min", precision = 10, scale = 2)
    private BigDecimal budgetMin;

    @Column(name = "budget_max", precision = 10, scale = 2)
    private BigDecimal budgetMax;

    @Column(length = 10)
    private String size;

    @Column(name = "birth_weekday", length = 15)
    private String birthWeekday;

    @Column(name = "lucky_goal", length = 30)
    private String luckyGoal;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }

    public String getPersonalColor() { return personalColor; }
    public void setPersonalColor(String personalColor) { this.personalColor = personalColor; }

    public String getOccasion() { return occasion; }
    public void setOccasion(String occasion) { this.occasion = occasion; }

    public BigDecimal getBudgetMin() { return budgetMin; }
    public void setBudgetMin(BigDecimal budgetMin) { this.budgetMin = budgetMin; }

    public BigDecimal getBudgetMax() { return budgetMax; }
    public void setBudgetMax(BigDecimal budgetMax) { this.budgetMax = budgetMax; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public String getBirthWeekday() { return birthWeekday; }
    public void setBirthWeekday(String birthWeekday) { this.birthWeekday = birthWeekday; }

    public String getLuckyGoal() { return luckyGoal; }
    public void setLuckyGoal(String luckyGoal) { this.luckyGoal = luckyGoal; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
