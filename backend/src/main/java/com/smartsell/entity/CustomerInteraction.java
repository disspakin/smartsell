package com.smartsell.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_interaction")
public class CustomerInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private CustomerSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType; // VIEW | INTERESTED_CLICK | AI_RECOMMENDED

    @Column(name = "personal_color", length = 20)
    private String personalColor; // Spring | Summer | Autumn | Winter

    @Column(length = 30)
    private String occasion;

    @Column(name = "lucky_color", length = 30)
    private String luckyColor;

    @Column(length = 10)
    private String size;

    @Column(precision = 10, scale = 2)
    private BigDecimal budget;

    @Column
    private Integer rating; // 1 to 5 stars AI feedback

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public CustomerInteraction() {}

    public CustomerInteraction(CustomerSession session, Product product, String eventType, String personalColor, String occasion, String luckyColor, String size, BigDecimal budget) {
        this.session = session;
        this.product = product;
        this.eventType = eventType;
        this.personalColor = personalColor;
        this.occasion = occasion;
        this.luckyColor = luckyColor;
        this.size = size;
        this.budget = budget;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CustomerSession getSession() { return session; }
    public void setSession(CustomerSession session) { this.session = session; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getPersonalColor() { return personalColor; }
    public void setPersonalColor(String personalColor) { this.personalColor = personalColor; }

    public String getOccasion() { return occasion; }
    public void setOccasion(String occasion) { this.occasion = occasion; }

    public String getLuckyColor() { return luckyColor; }
    public void setLuckyColor(String luckyColor) { this.luckyColor = luckyColor; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
