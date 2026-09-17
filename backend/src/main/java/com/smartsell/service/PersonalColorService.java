package com.smartsell.service;

import com.smartsell.dto.PersonalColorDTO;
import com.smartsell.dto.ProductDTO;
import com.smartsell.entity.PersonalColorResult;
import com.smartsell.repository.PersonalColorResultRepository;
import com.smartsell.repository.SeasonPaletteRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PersonalColorService {

    private final PersonalColorResultRepository resultRepository;
    private final SeasonPaletteRepository paletteRepository;
    private final ProductScoringService productScoringService;

    public PersonalColorService(PersonalColorResultRepository resultRepository,
                                 SeasonPaletteRepository paletteRepository,
                                 ProductScoringService productScoringService) {
        this.resultRepository = resultRepository;
        this.paletteRepository = paletteRepository;
        this.productScoringService = productScoringService;
    }

    // ================= MANUAL PATH =================
    public PersonalColorDTO.Result saveManualChoice(PersonalColorDTO.ManualRequest req) {
        PersonalColorResult entity = new PersonalColorResult();
        entity.setSessionToken(req.sessionToken());
        entity.setInputMethod("MANUAL");
        entity.setSeason(req.season());
        entity.setReasoning("เลือกโทนสีด้วยตัวเอง");
        entity = resultRepository.save(entity);

        return toResult(entity);
    }


    // ── private helpers ──────────────────────────────────────────────────

    private PersonalColorDTO.Result toResult(PersonalColorResult entity) {
        List<PersonalColorDTO.PaletteColor> palette = paletteRepository
                .findBySeasonOrderBySortOrder(entity.getSeason())
                .stream()
                .map(p -> new PersonalColorDTO.PaletteColor(p.getColorName(), p.getColorHex()))
                .collect(Collectors.toList());

        // Rulebase scoring — top 3 products, no occasion filter at this stage
        List<ProductDTO> products = productScoringService
                .recommendByPersonalColor(entity.getSeason(), null, 3);

        return new PersonalColorDTO.Result(
                entity.getId(), entity.getSeason(), entity.getConfidence(),
                entity.getReasoning(), palette, products
        );
    }
}
