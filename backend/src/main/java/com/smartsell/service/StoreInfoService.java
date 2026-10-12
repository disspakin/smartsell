package com.smartsell.service;

import com.smartsell.dto.StoreInfoDTO;
import com.smartsell.entity.StoreHours;
import com.smartsell.entity.StoreInfo;
import com.smartsell.repository.StoreHoursRepository;
import com.smartsell.repository.StoreInfoRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ข้อมูลร้าน (store_info) + เวลาเปิด-ปิด (store_hours) สำหรับ chatbot และ footer หน้าเว็บ
 * เวลา "ตอนนี้" คิดตามเวลาประเทศไทยเสมอ ไม่ใช้ timezone ของเครื่อง server
 */
@Service
public class StoreInfoService {

    public static final ZoneId STORE_ZONE = ZoneId.of("Asia/Bangkok");

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final StoreInfoRepository infoRepository;
    private final StoreHoursRepository hoursRepository;

    public StoreInfoService(StoreInfoRepository infoRepository, StoreHoursRepository hoursRepository) {
        this.infoRepository = infoRepository;
        this.hoursRepository = hoursRepository;
    }

    public ZonedDateTime now() {
        return ZonedDateTime.now(STORE_ZONE);
    }

    /** 1 = จันทร์ ... 7 = อาทิตย์ */
    static String thaiDayName(int isoDay) {
        return switch (DayOfWeek.of(isoDay)) {
            case MONDAY -> "จันทร์";
            case TUESDAY -> "อังคาร";
            case WEDNESDAY -> "พุธ";
            case THURSDAY -> "พฤหัสบดี";
            case FRIDAY -> "ศุกร์";
            case SATURDAY -> "เสาร์";
            case SUNDAY -> "อาทิตย์";
        };
    }

    /** ข้อมูลสำหรับ footer: ทุกแถวของ store_info, เวลาเปิดครบ 7 วัน และสถานะเปิด/ปิด ณ ตอนนี้ */
    public StoreInfoDTO getStoreInfo(ZonedDateTime now) {
        List<StoreInfoDTO.Item> info = infoRepository.findAllByOrderByIdAsc().stream()
                .map(i -> new StoreInfoDTO.Item(i.getInfoKey(), i.getLabel(), i.getInfoValue()))
                .toList();
        Map<Integer, StoreHours> byDay = hoursByDay();
        List<StoreInfoDTO.Day> hours = new java.util.ArrayList<>();
        for (int d = 1; d <= 7; d++) {
            StoreHours h = byDay.get(d);
            boolean closed = isClosedDay(h);
            hours.add(new StoreInfoDTO.Day(d, thaiDayName(d),
                    closed ? null : h.getOpenTime().format(HH_MM),
                    closed ? null : h.getCloseTime().format(HH_MM),
                    closed));
        }
        Status status = status(now, byDay);
        return new StoreInfoDTO(info, hours, status.open(), status.text());
    }

    /**
     * ข้อความใส่ใน system prompt ของ chatbot — ดึงใหม่ทุกครั้งที่ตอบ ร้านแก้ข้อมูลใน DB แล้วมีผลทันที
     * สถานะเปิด/ปิดคำนวณให้ที่นี่ ไม่ให้ AI คิดเวลาเอง
     */
    public String buildChatContext(ZonedDateTime now) {
        StringBuilder sb = new StringBuilder("ข้อมูลร้าน:\n");
        for (StoreInfo i : infoRepository.findAllByOrderByIdAsc()) {
            sb.append(i.getLabel()).append(": ").append(i.getInfoValue()).append("\n");
        }

        Map<Integer, StoreHours> byDay = hoursByDay();
        sb.append("เวลาเปิด: ");
        for (int d = 1; d <= 7; d++) {
            if (d > 1) sb.append(", ");
            sb.append(thaiDayName(d)).append(" ").append(formatHours(byDay.get(d)));
        }
        sb.append("\n");

        sb.append("วันเวลาตอนนี้ (เวลาประเทศไทย): วัน").append(thaiDayName(now.getDayOfWeek().getValue()))
                .append(" ").append(now.format(DATE)).append(" ").append(now.format(HH_MM)).append(" น.\n");
        sb.append("สถานะร้านตอนนี้: ").append(status(now, byDay).text()).append("\n");
        return sb.toString();
    }

    private Map<Integer, StoreHours> hoursByDay() {
        return hoursRepository.findAllByOrderByDayOfWeekAsc().stream()
                .collect(Collectors.toMap(h -> h.getDayOfWeek().intValue(), Function.identity(), (a, b) -> a));
    }

    /** วันที่ไม่มีแถว หรือเวลาไม่ครบ ถือว่าปิด — ไม่บอกลูกค้าว่าเปิดถ้าไม่แน่ใจ */
    private static boolean isClosedDay(StoreHours h) {
        return h == null || h.isClosed() || h.getOpenTime() == null || h.getCloseTime() == null;
    }

    private static String formatHours(StoreHours h) {
        if (h == null) return "ไม่มีข้อมูล";
        if (isClosedDay(h)) return "ปิด";
        return h.getOpenTime().format(HH_MM) + "-" + h.getCloseTime().format(HH_MM);
    }

    record Status(boolean open, String text) {}

    /** เปิดอยู่ถ้า open <= ตอนนี้ < close ของวันนี้ ถ้าปิดอยู่บอกด้วยว่าจะเปิดอีกทีเมื่อไหร่ */
    Status status(ZonedDateTime now, Map<Integer, StoreHours> byDay) {
        int today = now.getDayOfWeek().getValue();
        LocalTime time = now.toLocalTime();
        StoreHours h = byDay.get(today);

        if (!isClosedDay(h) && !time.isBefore(h.getOpenTime()) && time.isBefore(h.getCloseTime())) {
            return new Status(true, "เปิดอยู่ (ปิด " + h.getCloseTime().format(HH_MM) + " น.)");
        }
        if (!isClosedDay(h) && time.isBefore(h.getOpenTime())) {
            return new Status(false, "ปิดอยู่ จะเปิดวันนี้ " + h.getOpenTime().format(HH_MM) + " น.");
        }
        for (int ahead = 1; ahead <= 7; ahead++) {
            int day = (today - 1 + ahead) % 7 + 1;
            StoreHours next = byDay.get(day);
            if (!isClosedDay(next)) {
                String when = ahead == 1 ? "พรุ่งนี้ (วัน" + thaiDayName(day) + ")" : "วัน" + thaiDayName(day);
                return new Status(false, "ปิดแล้ว จะเปิดอีกครั้ง" + when + " " + next.getOpenTime().format(HH_MM) + " น.");
            }
        }
        return new Status(false, "ปิดแล้ว");
    }
}
