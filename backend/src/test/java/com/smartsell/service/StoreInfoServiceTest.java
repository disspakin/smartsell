package com.smartsell.service;

import com.smartsell.dto.StoreInfoDTO;
import com.smartsell.entity.StoreHours;
import com.smartsell.entity.StoreInfo;
import com.smartsell.repository.StoreHoursRepository;
import com.smartsell.repository.StoreInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StoreInfoService")
class StoreInfoServiceTest {

    @Mock
    private StoreInfoRepository infoRepository;

    @Mock
    private StoreHoursRepository hoursRepository;

    private StoreInfoService service;

    private static StoreInfo info(String key, String label, String value) {
        StoreInfo i = new StoreInfo();
        i.setInfoKey(key);
        i.setLabel(label);
        i.setInfoValue(value);
        return i;
    }

    private static StoreHours hours(int day, String open, String close) {
        StoreHours h = new StoreHours();
        h.setDayOfWeek((short) day);
        if (open == null) {
            h.setClosed(true);
        } else {
            h.setOpenTime(LocalTime.parse(open));
            h.setCloseTime(LocalTime.parse(close));
        }
        return h;
    }

    /** 2026-10-12 เป็นวันจันทร์ — เวลาไทย */
    private static ZonedDateTime bangkok(int dayOfMonth, String time) {
        return ZonedDateTime.of(2026, 10, dayOfMonth, LocalTime.parse(time).getHour(), LocalTime.parse(time).getMinute(),
                0, 0, StoreInfoService.STORE_ZONE);
    }

    @BeforeEach
    void setUp() {
        service = new StoreInfoService(infoRepository, hoursRepository);
        when(infoRepository.findAllByOrderByIdAsc()).thenReturn(List.of(
                info("location", "ที่ตั้งร้าน", "ตึก 15 ชั้น 1 ใกล้หอประชุม"),
                info("phone", "เบอร์โทร", "02-697-6331"),
                // หัวข้อใหม่ที่โค้ดไม่รู้จัก ต้องออกมาด้วย
                info("line", "LINE", "@utccshop")));
        List<StoreHours> week = new ArrayList<>();
        for (int d = 1; d <= 6; d++) week.add(hours(d, "08:30", "17:00"));
        week.add(hours(7, null, null));
        when(hoursRepository.findAllByOrderByDayOfWeekAsc()).thenReturn(week);
    }

    @Test
    @DisplayName("context มีข้อมูลร้านทุกแถว เวลาเปิด 7 วัน และเวลาไทยตอนนี้")
    void chatContextListsEverything() {
        String ctx = service.buildChatContext(bangkok(12, "10:00"));

        assertThat(ctx)
                .contains("ที่ตั้งร้าน: ตึก 15 ชั้น 1 ใกล้หอประชุม")
                .contains("เบอร์โทร: 02-697-6331")
                .contains("LINE: @utccshop")
                .contains("เวลาเปิด: จันทร์ 08:30-17:00, อังคาร 08:30-17:00")
                .contains("เสาร์ 08:30-17:00, อาทิตย์ ปิด")
                .contains("วันเวลาตอนนี้ (เวลาประเทศไทย): วันจันทร์ 2026-10-12 10:00 น.")
                .contains("สถานะร้านตอนนี้: เปิดอยู่ (ปิด 17:00 น.)");
    }

    @Test
    @DisplayName("ใช้เวลาไทยเสมอ — เวลา UTC 03:00 วันจันทร์ = 10:00 ไทย ร้านเปิด")
    void convertsToBangkokTime() {
        ZonedDateTime utc = ZonedDateTime.of(2026, 10, 12, 3, 0, 0, 0, ZoneId.of("UTC"));
        // ผู้เรียกส่งเวลาไทยมาเสมอผ่าน now() — เช็กว่า now() เป็นโซนไทย
        assertThat(service.now().getZone()).isEqualTo(StoreInfoService.STORE_ZONE);
        assertThat(service.getStoreInfo(utc.withZoneSameInstant(StoreInfoService.STORE_ZONE)).openNow()).isTrue();
    }

    @Test
    @DisplayName("เปิดตั้งแต่ 08:30 ตรง ปิดตอน 17:00 ตรง")
    void openCloseBoundaries() {
        assertThat(service.getStoreInfo(bangkok(12, "08:29")).openNow()).isFalse();
        assertThat(service.getStoreInfo(bangkok(12, "08:30")).openNow()).isTrue();
        assertThat(service.getStoreInfo(bangkok(12, "16:59")).openNow()).isTrue();
        assertThat(service.getStoreInfo(bangkok(12, "17:00")).openNow()).isFalse();
    }

    @Test
    @DisplayName("ก่อนเปิด บอกว่าจะเปิดวันนี้ / หลังปิด บอกว่าเปิดพรุ่งนี้")
    void closedStatusTellsNextOpening() {
        assertThat(service.getStoreInfo(bangkok(12, "07:00")).statusText()).isEqualTo("ปิดอยู่ จะเปิดวันนี้ 08:30 น.");
        assertThat(service.getStoreInfo(bangkok(12, "18:20")).statusText())
                .isEqualTo("ปิดแล้ว จะเปิดอีกครั้งพรุ่งนี้ (วันอังคาร) 08:30 น.");
    }

    @Test
    @DisplayName("วันอาทิตย์ปิดทั้งวัน และเสาร์เย็นข้ามอาทิตย์ไปเปิดวันจันทร์")
    void sundayClosed() {
        StoreInfoDTO sunday = service.getStoreInfo(bangkok(18, "12:00"));
        assertThat(sunday.openNow()).isFalse();
        assertThat(sunday.statusText()).isEqualTo("ปิดแล้ว จะเปิดอีกครั้งพรุ่งนี้ (วันจันทร์) 08:30 น.");
        assertThat(sunday.hours().get(6).closed()).isTrue();
        assertThat(sunday.hours().get(6).openTime()).isNull();

        assertThat(service.getStoreInfo(bangkok(17, "18:00")).statusText())
                .isEqualTo("ปิดแล้ว จะเปิดอีกครั้งวันจันทร์ 08:30 น.");
    }
}
