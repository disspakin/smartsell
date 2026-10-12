package com.smartsell.controller;

import com.smartsell.dto.ProductDTO;
import com.smartsell.dto.SizeChartDTO;
import com.smartsell.entity.Product;
import com.smartsell.entity.SizeChart;
import com.smartsell.repository.CustomerInteractionRepository;
import com.smartsell.repository.ProductRepository;
import com.smartsell.repository.SizeChartRowRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*") // tighten this to your frontend origin before deploying
public class ProductController {

    private final ProductRepository productRepository;
    private final SizeChartRowRepository sizeChartRowRepository;
    private final CustomerInteractionRepository interactionRepository;

    public ProductController(ProductRepository productRepository, SizeChartRowRepository sizeChartRowRepository,
                             CustomerInteractionRepository interactionRepository) {
        this.productRepository = productRepository;
        this.sizeChartRowRepository = sizeChartRowRepository;
        this.interactionRepository = interactionRepository;
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

    // GET /api/products/popular?limit=6  -> "สินค้าแนะนำสำหรับคุณ" หน้าแรก
    // เรียงตามคนเปิดดูมากสุด แล้วตามคนกดถูกใจ (ดู CustomerInteractionRepository.findProductIdsByPopularity)
    @GetMapping("/popular")
    public List<ProductDTO> popular(@RequestParam(defaultValue = "6") int limit) {
        int size = Math.max(1, Math.min(limit, 24));
        List<Long> ids = interactionRepository.findProductIdsByPopularity(PageRequest.of(0, size));
        // findAllById ไม่รับประกันลำดับ — เรียงกลับตาม ids
        Map<Long, Product> byId = productRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        return ids.stream().map(byId::get).map(this::toDTO).collect(Collectors.toList());
    }

    // GET /api/products/{id}  -> Product Detail page
    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> get(@PathVariable Long id) {
        return productRepository.findById(id)
                .map(p -> ResponseEntity.ok(toDTO(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/products/{id}/size-chart  -> "ตารางขนาด" modal on Product Detail
    // 404 ทั้งตอนไม่มีสินค้านี้ และตอนสินค้ายังไม่มีตารางไซส์ (size_chart_id เป็น NULL)
    @GetMapping("/{id}/size-chart")
    public ResponseEntity<SizeChartDTO> sizeChart(@PathVariable Long id) {
        return productRepository.findById(id)
                .map(Product::getSizeChart)
                .map(chart -> ResponseEntity.ok(toSizeChartDTO(chart)))
                .orElse(ResponseEntity.notFound().build());
    }

    private SizeChartDTO toSizeChartDTO(SizeChart chart) {
        List<SizeChartDTO.Row> rows = sizeChartRowRepository.findBySizeChartIdOrderBySortOrder(chart.getId()).stream()
                .map(r -> new SizeChartDTO.Row(r.getSize(), r.getChestIn(), r.getLengthIn()))
                .collect(Collectors.toList());
        return new SizeChartDTO(chart.getName(), "inch", chart.isEstimated(), rows);
    }

    private ProductDTO toDTO(Product p) {
        List<ProductDTO.VariantDTO> variants = p.getVariants().stream()
                .map(ProductDTO.VariantDTO::from)
                .collect(Collectors.toList());
        return new ProductDTO(p.getId(), p.getName(), p.getCategory(), p.getPrice(), p.getDescription(), p.getImageUrl(), variants);
    }
}
