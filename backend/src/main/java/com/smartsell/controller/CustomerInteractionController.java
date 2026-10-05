package com.smartsell.controller;

import com.smartsell.entity.CustomerInteraction;
import com.smartsell.entity.CustomerSession;
import com.smartsell.entity.Product;
import com.smartsell.repository.CustomerInteractionRepository;
import com.smartsell.repository.CustomerSessionRepository;
import com.smartsell.repository.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/interactions")
@CrossOrigin(origins = "*")
public class CustomerInteractionController {

    private final CustomerInteractionRepository interactionRepository;
    private final CustomerSessionRepository sessionRepository;
    private final ProductRepository productRepository;

    public CustomerInteractionController(CustomerInteractionRepository interactionRepository,
                                         CustomerSessionRepository sessionRepository,
                                         ProductRepository productRepository) {
        this.interactionRepository = interactionRepository;
        this.sessionRepository = sessionRepository;
        this.productRepository = productRepository;
    }

    public record InteractionRequest(Long sessionId, Long productId, String eventType) {}

    @PostMapping
    public ResponseEntity<?> recordInteraction(@RequestBody InteractionRequest req) {
        if (req.sessionId() == null || req.productId() == null || req.eventType() == null) {
            return ResponseEntity.badRequest().body("Missing required fields");
        }

        CustomerSession session = sessionRepository.findById(req.sessionId()).orElse(null);
        Product product = productRepository.findById(req.productId()).orElse(null);

        if (product != null) {
            // กดถูกใจซ้ำใน session เดิมไม่นับเพิ่ม
            if (session != null && "INTERESTED_CLICK".equals(req.eventType())
                    && interactionRepository.existsBySessionIdAndProductIdAndEventType(
                            session.getId(), product.getId(), "INTERESTED_CLICK")) {
                return ResponseEntity.ok().build();
            }

            CustomerInteraction interaction;
            if (session != null) {
                interaction = new CustomerInteraction(
                        session, product, req.eventType(),
                        session.getPersonalColor(), session.getOccasion(),
                        session.getLuckyGoal(), session.getSize(), session.getBudgetMax()
                );
            } else {
                interaction = new CustomerInteraction(
                        null, product, req.eventType(),
                        null, null, null, null, null
                );
            }
            interactionRepository.save(interaction);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    public record RatingRequest(Long sessionId, String eventType, Integer rating) {}

    @PostMapping("/rating")
    public ResponseEntity<?> recordRating(@RequestBody RatingRequest req) {
        if (req.rating() == null || req.rating() < 1 || req.rating() > 5) {
            return ResponseEntity.badRequest().body("Rating must be between 1 and 5");
        }
        CustomerSession session = (req.sessionId() != null)
                ? sessionRepository.findById(req.sessionId()).orElse(null)
                : null;
        CustomerInteraction interaction = new CustomerInteraction();
        interaction.setSession(session);
        interaction.setEventType(req.eventType() != null ? req.eventType() : "AI_RATING");
        interaction.setRating(req.rating());
        if (session != null) {
            interaction.setPersonalColor(session.getPersonalColor());
            interaction.setOccasion(session.getOccasion());
            interaction.setLuckyColor(session.getLuckyGoal());
            interaction.setSize(session.getSize());
            interaction.setBudget(session.getBudgetMax());
        }
        interactionRepository.save(interaction);
        return ResponseEntity.ok().build();
    }
}
