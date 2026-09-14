package com.smartsell.service;

import com.smartsell.dto.PersonalColorDTO;
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

    public PersonalColorService(PersonalColorResultRepository resultRepository,
                                 SeasonPaletteRepository paletteRepository) {
        this.resultRepository = resultRepository;
        this.paletteRepository = paletteRepository;
    }

    // ================= MANUAL PATH (fully working, no AI needed) =================
    public PersonalColorDTO.Result saveManualChoice(PersonalColorDTO.ManualRequest req) {
        PersonalColorResult entity = new PersonalColorResult();
        entity.setSessionToken(req.sessionToken());
        entity.setInputMethod("MANUAL");
        entity.setSeason(req.season());
        entity.setReasoning("เลือกโทนสีด้วยตัวเอง");
        entity = resultRepository.save(entity);

        return toResult(entity);
    }

    // ================= PHOTO PATH (stub — TODO: wire up Gemini Vision here) =================
    public PersonalColorDTO.Result analyzeFromPhoto(PersonalColorDTO.PhotoRequest req) {
        // TODO: call Gemini Vision API here with req.imageBase64(), parse season/confidence/reasoning
        // from the response, same idea as ChatService. Left as a stub for now so the endpoint
        // doesn't break the frontend before the API key is wired up — it just asks the
        // customer to pick manually instead.
        PersonalColorResult entity = new PersonalColorResult();
        entity.setSessionToken(req.sessionToken());
        entity.setInputMethod("PHOTO");
        entity.setSeason("Winter"); // placeholder until Gemini is connected
        entity.setConfidence(BigDecimal.ZERO);
        entity.setReasoning("ยังไม่ได้เชื่อมต่อ AI วิเคราะห์รูป — นี่คือผลลัพธ์ตัวอย่าง ลองเลือกโทนสีเองด้านล่างแทนได้เลย");
        entity = resultRepository.save(entity);

        return toResult(entity);
    }

    private PersonalColorDTO.Result toResult(PersonalColorResult entity) {
        List<PersonalColorDTO.PaletteColor> palette = paletteRepository
                .findBySeasonOrderBySortOrder(entity.getSeason())
                .stream()
                .map(p -> new PersonalColorDTO.PaletteColor(p.getColorName(), p.getColorHex()))
                .collect(Collectors.toList());

        return new PersonalColorDTO.Result(
                entity.getId(), entity.getSeason(), entity.getConfidence(), entity.getReasoning(), palette
        );
    }
}
