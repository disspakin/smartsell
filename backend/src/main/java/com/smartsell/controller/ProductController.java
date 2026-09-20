package com.smartsell.controller;

import com.smartsell.dto.ProductDTO;
import com.smartsell.entity.Product;
import com.smartsell.repository.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*") // tighten this to your frontend origin before deploying
public class ProductController {

    private final ProductRepository productRepository;
    private final com.smartsell.service.ProductScoringService productScoringService;

    public ProductController(ProductRepository productRepository, com.smartsell.service.ProductScoringService productScoringService) {
        this.productRepository = productRepository;
        this.productScoringService = productScoringService;
    }

    // GET /api/products                -> product grid on Home / catalog
    // GET /api/products?category=Polo  -> filtered catalog
    @GetMapping
    public List<ProductDTO> list(@RequestParam(required = false) String category) {
        List<Product> products = (category == null)
                ? productRepository.findAll()
                : productRepository.findByCategory(category);
        return products.stream().map(this::toDTO).collect(Collectors.toList());
    }

    // GET /api/products/{id}  -> Product Detail page
    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> get(@PathVariable Long id) {
        return productRepository.findById(id)
                .map(p -> ResponseEntity.ok(toDTO(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    // used by the recommendation engine on the Personal Shopping Assistant page
    @GetMapping("/recommend")
    public List<ProductDTO> recommend(@RequestParam(required = false) String season,
                                       @RequestParam(required = false) String occasion) {
        return productScoringService.recommendByPersonalColor(season, occasion, 10);
    }

    private ProductDTO toDTO(Product p) {
        List<ProductDTO.VariantDTO> variants = p.getVariants().stream()
                .map(v -> new ProductDTO.VariantDTO(v.getId(), v.getColor(), v.getColorHex(), v.getSize(), v.getImageUrl()))
                .collect(Collectors.toList());
        return new ProductDTO(p.getId(), p.getName(), p.getCategory(), p.getPrice(), p.getDescription(), p.getImageUrl(), variants);
    }
}
