package com.smartsell.service;

import com.smartsell.dto.LuckyColorDTO;
import com.smartsell.entity.CustomerInteraction;
import com.smartsell.exception.InvalidRequestException;
import com.smartsell.repository.CustomerInteractionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LuckyColorService {

    static final String EVENT_TYPE = "LUCKY_COLOR_SEARCH";

    /** token ยาวกว่านี้ถือว่าไม่ใช่ token จากหน้าเว็บเรา (หน้าเว็บสุ่มยาวไม่ถึง 20 ตัว) */
    private static final int MAX_TOKEN_LENGTH = 100;

    private final LuckyColorScoringService luckyColorScoringService;
    private final ColorRuleEngine colorRuleEngine;
    private final CustomerInteractionRepository interactionRepository;

    public LuckyColorService(LuckyColorScoringService luckyColorScoringService,
                             ColorRuleEngine colorRuleEngine,
                             CustomerInteractionRepository interactionRepository) {
        this.luckyColorScoringService = luckyColorScoringService;
        this.colorRuleEngine = colorRuleEngine;
        this.interactionRepository = interactionRepository;
    }

    // สีมงคลประจำวันเกิดอยู่ที่ ColorRuleEngine ที่เดียว ทั้งสีที่แสดงและสีที่ใช้คัดสินค้า

    private static final Map<String, String> GOAL_TEXT = Map.of(
            "การเรียน",   "ช่วยเสริมสมาธิและความจำในการเรียน",
            "การงาน",     "ช่วยเสริมความน่าเชื่อถือและความมั่นคงในหน้าที่การงาน",
            "การเงิน",    "ช่วยเสริมโชคลาภด้านการเงิน",
            "ความรัก",    "ช่วยเสริมเสน่ห์และความรัก",
            "โชคลาภ",    "ช่วยเสริมดวงโชคลาภโดยรวม",
            "ความมั่นใจ", "ช่วยเสริมความมั่นใจในตัวเอง"
    );

    @Transactional
    public LuckyColorDTO.Result compute(LuckyColorDTO.Request req) {
        String day = trimToNull(req.birthWeekday());
        String goal = trimToNull(req.luckyGoal());

        // ค่าผิดต้องบอกตรงๆ — เดิมวันที่ไม่รู้จักได้สีฟ้า/ขาวกลับไปเงียบๆ เหมือนเป็นคำตอบจริง
        if (day == null) {
            throw new InvalidRequestException("กรุณาเลือกวันเกิด");
        }
        if (!colorRuleEngine.isKnownDay(day)) {
            throw new InvalidRequestException("ไม่รู้จักวันเกิด \"" + day + "\" กรุณาเลือกวันจันทร์ถึงอาทิตย์");
        }
        if (goal != null && !colorRuleEngine.isKnownGoal(goal)) {
            throw new InvalidRequestException("ไม่รู้จักเป้าหมาย \"" + goal + "\"");
        }

        List<ColorRuleEngine.ColorDisplay> colors = colorRuleEngine.getLuckyColorDisplayByDay(day);
        String colorName = colors.stream().map(ColorRuleEngine.ColorDisplay::thaiName).collect(Collectors.joining(" / "));
        List<String> hexes = colors.stream().map(ColorRuleEngine.ColorDisplay::hex).toList();

        String goalText = goal != null ? GOAL_TEXT.get(goal) : "ช่วยเสริมดวงโดยรวม";
        String reasoning = "เกิดวัน" + day + " สีมงคลที่เหมาะ ได้แก่ สี" + colorName + " — " + goalText;

        List<LuckyColorDTO.LuckyProduct> products = luckyColorScoringService.recommend(day, goal, 3);

        if (goal != null) {
            recordSearch(trimToNull(req.sessionToken()), goal);
        }

        return new LuckyColorDTO.Result(colorName, hexes, reasoning, products);
    }

    /**
     * บันทึกว่ามีคนสนใจเสริมดวงด้านนี้ ให้การ์ด "Lucky Color Intentions" บน Dashboard
     *
     * นับครั้งเดียวต่อ token ต่อเป้าหมาย — กดดูผลซ้ำหรือเปลี่ยนวันเกิดไปมาจะไม่นับเพิ่ม
     * คำขอที่ไม่มี token (เช่นยิง API ตรง) ยังนับทุกครั้ง เพราะไม่มีอะไรให้แยกว่าเป็นคนเดิม
     */
    private void recordSearch(String token, String goal) {
        String clientToken = token != null && token.length() <= MAX_TOKEN_LENGTH ? token : null;
        if (clientToken != null
                && interactionRepository.existsByClientTokenAndEventTypeAndLuckyColor(clientToken, EVENT_TYPE, goal)) {
            return;
        }

        CustomerInteraction interaction = new CustomerInteraction();
        interaction.setEventType(EVENT_TYPE);
        interaction.setLuckyColor(goal);
        interaction.setClientToken(clientToken);
        interactionRepository.save(interaction);
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
