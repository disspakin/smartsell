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

        if (session != null && product != null) {
            CustomerInteraction interaction = new CustomerInteraction(
                    session, product, req.eventType(),
                    session.getPersonalColor(), session.getOccasion(),
                    session.getLuckyGoal(), session.getSize(), session.getBudgetMax()
            );
            interactionRepository.save(interaction);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
