package com.smartsell.service;

import com.smartsell.dto.LuckyColorDTO;
import com.smartsell.dto.ProductDTO;
import com.smartsell.entity.Product;
import com.smartsell.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LuckyColorService {

    private final ProductRepository productRepository;

    public LuckyColorService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // ตามความเชื่อสีมงคลประจำวันแบบไทยดั้งเดิม
    private static final Map<String, String[]> DAY_COLOR = Map.of(
            "จันทร์",  new String[]{"เหลือง", "#e8c547"},
            "อังคาร",  new String[]{"ชมพู",   "#e88fa8"},
            "พุธ",     new String[]{"เขียว",  "#6fa87f"},
            "พฤหัส",  new String[]{"ส้ม",    "#e08a3d"},
            "ศุกร์",   new String[]{"ฟ้า",    "#6fa8c9"},
            "เสาร์",   new String[]{"ม่วง",   "#8f6fa8"},
            "อาทิตย์", new String[]{"แดง",    "#c94f4f"}
    );

    private static final Map<String, String> GOAL_TEXT = Map.of(
            "การเรียน", "ช่วยเสริมสมาธิและความจำในการเรียน",
            "การงาน",   "ช่วยเสริมความน่าเชื่อถือและความมั่นคงในหน้าที่การงาน",
            "การเงิน",  "ช่วยเสริมโชคลาภด้านการเงิน",
            "ความรัก",  "ช่วยเสริมเสน่ห์และความรัก",
            "โชคลาภ",  "ช่วยเสริมดวงโชคลาภโดยรวม",
            "ความมั่นใจ", "ช่วยเสริมความมั่นใจในตัวเอง"
    );

    public LuckyColorDTO.Result compute(LuckyColorDTO.Request req) {
        String[] colorInfo = DAY_COLOR.getOrDefault(req.birthWeekday(), new String[]{"ฟ้า", "#6fa8c9"});
        String color = colorInfo[0];
        String hex = colorInfo[1];
        String goalText = GOAL_TEXT.getOrDefault(req.luckyGoal(), "ช่วยเสริมดวงโดยรวม");

        String reasoning = "เกิดวัน" + req.birthWeekday() + " สีมงคลคือสี" + color
                + " — " + goalText;

        List<Product> matched = productRepository.findByLuckyColor(color);
        List<ProductDTO> productDTOs = matched.stream().map(this::toDTO).collect(Collectors.toList());

        return new LuckyColorDTO.Result(color, hex, reasoning, productDTOs);
    }

    private ProductDTO toDTO(Product p) {
        List<ProductDTO.VariantDTO> variants = p.getVariants().stream()
                .map(v -> new ProductDTO.VariantDTO(v.getId(), v.getColor(), v.getColorHex(), v.getSize(), v.getImageUrl()))
                .collect(Collectors.toList());
        return new ProductDTO(p.getId(), p.getName(), p.getCategory(), p.getPrice(), p.getDescription(), p.getImageUrl(), variants);
    }
}
