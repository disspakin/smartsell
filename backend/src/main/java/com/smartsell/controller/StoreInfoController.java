package com.smartsell.controller;

import com.smartsell.dto.StoreInfoDTO;
import com.smartsell.service.StoreInfoService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/store")
@CrossOrigin(origins = "*")
public class StoreInfoController {

    private final StoreInfoService storeInfoService;

    public StoreInfoController(StoreInfoService storeInfoService) {
        this.storeInfoService = storeInfoService;
    }

    // GET /api/store  -> footer: ที่ตั้ง เบอร์ เวลาเปิด และป้าย "เปิดอยู่" / "ปิดแล้ว"
    @GetMapping
    public StoreInfoDTO get() {
        return storeInfoService.getStoreInfo(storeInfoService.now());
    }
}
