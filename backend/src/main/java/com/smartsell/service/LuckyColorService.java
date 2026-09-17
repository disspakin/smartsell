package com.smartsell.service;

import com.smartsell.dto.LuckyColorDTO;
import com.smartsell.dto.ProductDTO;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class LuckyColorService {

    private final LuckyColorScoringService luckyColorScoringService;

    public LuckyColorService(LuckyColorScoringService luckyColorScoringService) {
        this.luckyColorScoringService = luckyColorScoringService;
    }

    /**
     * DAY_COLOR — สีมงคลประจำวันเกิดแบบไทย
     * index 0 = ชื่อสีรวม (display text)
     * index 1..N = hex ของแต่ละสี (ครบตามจำนวนสีของวันนั้น)
     *
     * ข้อมูลอิงจาก rulebase สีมงคลที่นิยมใช้เรื่องการแต่งกายตามวันเกิด
     */
    private static final Map<String, String[]> DAY_COLOR = Map.of(
        "อาทิตย์", new String[]{
            "ส้ม / แดง / ชมพู / เขียว / ขาว",
            "#e07a3d", "#c94f4f", "#e88fa8", "#6fa87f", "#FFFFFF"
        },
        "จันทร์", new String[]{
            "เขียว / ดำ / ขาว / ม่วง",
            "#6fa87f", "#1a1a1a", "#FFFFFF", "#8f6fa8"
        },
        "อังคาร", new String[]{
            "ดำ / เหลือง / ชมพู / ม่วง / แดง / แสด",
            "#1a1a1a", "#f4d35e", "#e88fa8", "#8f6fa8", "#c94f4f", "#e05a20"
        },
        "พุธ", new String[]{
            "เขียว / เหลืองอ่อน / เหลืองแก่",
            "#6fa87f", "#f0e68c", "#c9a800"
        },
        "พฤหัส", new String[]{
            "ส้ม / เหลือง / ฟ้า / น้ำเงิน / เขียว / แดง",
            "#e08a3d", "#f4d35e", "#6fa8c9", "#1E3A8A", "#6fa87f", "#c94f4f"
        },
        "ศุกร์", new String[]{
            "ฟ้า / น้ำเงิน / ขาว / เหลือง / ชมพู / แสด",
            "#6fa8c9", "#1E3A8A", "#FFFFFF", "#f4d35e", "#e88fa8", "#e05a20"
        },
        "เสาร์", new String[]{
            "แดง / เหลือง / ฟ้า / น้ำเงิน / ชมพู / น้ำตาล",
            "#c94f4f", "#f4d35e", "#6fa8c9", "#1E3A8A", "#e88fa8", "#a0693a"
        }
    );

    private static final Map<String, String> GOAL_TEXT = Map.of(
            "การเรียน",   "ช่วยเสริมสมาธิและความจำในการเรียน",
            "การงาน",     "ช่วยเสริมความน่าเชื่อถือและความมั่นคงในหน้าที่การงาน",
            "การเงิน",    "ช่วยเสริมโชคลาภด้านการเงิน",
            "ความรัก",    "ช่วยเสริมเสน่ห์และความรัก",
            "โชคลาภ",    "ช่วยเสริมดวงโชคลาภโดยรวม",
            "ความมั่นใจ", "ช่วยเสริมความมั่นใจในตัวเอง"
    );

    public LuckyColorDTO.Result compute(LuckyColorDTO.Request req) {
        String[] colorInfo = DAY_COLOR.getOrDefault(req.birthWeekday(),
                new String[]{"ฟ้า / ขาว", "#6fa8c9", "#FFFFFF"});

        String colorName = colorInfo[0];
        // hex list = everything from index 1 onward
        List<String> hexes = Arrays.asList(colorInfo).subList(1, colorInfo.length);

        String goalText = GOAL_TEXT.getOrDefault(req.luckyGoal(), "ช่วยเสริมดวงโดยรวม");
        String reasoning = "เกิดวัน" + req.birthWeekday()
                + " สีมงคลที่เหมาะ ได้แก่ สี" + colorName
                + " — " + goalText;

        List<ProductDTO> products = luckyColorScoringService
                .recommend(req.birthWeekday(), req.luckyGoal(), 3);

        return new LuckyColorDTO.Result(colorName, hexes, reasoning, products);
    }
}
