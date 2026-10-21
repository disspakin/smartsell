package com.smartsell.service;

import com.smartsell.dto.PersonalColorDTO;
import com.smartsell.entity.PersonalColorResult;
import com.smartsell.repository.PersonalColorResultRepository;
import com.smartsell.repository.SeasonPaletteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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


    // ================= QUIZ PATH =================
    // question order must match the frontend quiz: true = tone question, false = depth question
    private static final boolean[] TONE_QUESTION = {true, true, false, true, false};

    public PersonalColorDTO.Result saveQuizResult(PersonalColorDTO.QuizRequest req) {
        String season = computeSeason(req.answers());

        PersonalColorResult entity = new PersonalColorResult();
        entity.setSessionToken(req.sessionToken());
        entity.setInputMethod("QUIZ");
        entity.setSeason(season);
        entity.setReasoning("คำนวณจากแบบทดสอบโทนสี");
        entity = resultRepository.save(entity);

        return toResult(entity);
    }

    String computeSeason(List<String> answers) {
        if (answers == null || answers.size() != TONE_QUESTION.length) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "answers must contain " + TONE_QUESTION.length + " items");
        }
        int toneTotal = 0, warm = 0, depthTotal = 0, light = 0;
        for (int i = 0; i < answers.size(); i++) {
            String a = answers.get(i);
            if (TONE_QUESTION[i]) {
                if (!"warm".equals(a) && !"cool".equals(a)) throw badAnswer(i);
                toneTotal++;
                if ("warm".equals(a)) warm++;
            } else {
                if (!"light".equals(a) && !"deep".equals(a)) throw badAnswer(i);
                depthTotal++;
                if ("light".equals(a)) light++;
            }
        }
        boolean isWarm = warm > toneTotal / 2.0;
        boolean isLight = light > depthTotal / 2.0;
        if (isWarm) return isLight ? "Spring" : "Autumn";
        return isLight ? "Summer" : "Winter";
    }

    private ResponseStatusException badAnswer(int index) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid answer at index " + index);
    }

    // ── private helpers ──────────────────────────────────────────────────

    private PersonalColorDTO.Result toResult(PersonalColorResult entity) {
        List<PersonalColorDTO.PaletteColor> palette = paletteRepository
                .findBySeasonOrderBySortOrder(entity.getSeason())
                .stream()
                .map(p -> new PersonalColorDTO.PaletteColor(p.getColorName(), p.getColorHex()))
                .collect(Collectors.toList());

        return new PersonalColorDTO.Result(
                entity.getId(), entity.getSeason(), entity.getConfidence(),
                entity.getReasoning(), palette
        );
    }
}
