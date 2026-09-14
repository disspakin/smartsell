package com.smartsell.controller;

import com.smartsell.dto.PersonalColorDTO;
import com.smartsell.service.PersonalColorService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/personal-color")
@CrossOrigin(origins = "*")
public class PersonalColorController {

    private final PersonalColorService service;

    public PersonalColorController(PersonalColorService service) {
        this.service = service;
    }

    // POST /api/personal-color/manual  — body: { "sessionToken": "...", "season": "Winter" }
    @PostMapping("/manual")
    public PersonalColorDTO.Result manual(@RequestBody PersonalColorDTO.ManualRequest request) {
        return service.saveManualChoice(request);
    }

    // POST /api/personal-color/photo  — body: { "sessionToken": "...", "imageBase64": "..." }
    // stub until Gemini Vision is wired up in PersonalColorService — see TODO there
    @PostMapping("/photo")
    public PersonalColorDTO.Result photo(@RequestBody PersonalColorDTO.PhotoRequest request) {
        return service.analyzeFromPhoto(request);
    }
}
