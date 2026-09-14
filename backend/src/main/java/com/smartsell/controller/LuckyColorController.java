package com.smartsell.controller;

import com.smartsell.dto.LuckyColorDTO;
import com.smartsell.service.LuckyColorService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lucky-color")
@CrossOrigin(origins = "*")
public class LuckyColorController {

    private final LuckyColorService service;

    public LuckyColorController(LuckyColorService service) {
        this.service = service;
    }

    // POST /api/lucky-color
    // body: { "sessionToken": "...", "birthWeekday": "จันทร์", "luckyGoal": "การงาน" }
    @PostMapping
    public LuckyColorDTO.Result compute(@RequestBody LuckyColorDTO.Request request) {
        return service.compute(request);
    }
}
