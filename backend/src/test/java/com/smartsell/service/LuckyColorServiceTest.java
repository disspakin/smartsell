package com.smartsell.service;

import com.smartsell.dto.LuckyColorDTO;
import com.smartsell.entity.CustomerInteraction;
import com.smartsell.exception.InvalidRequestException;
import com.smartsell.repository.CustomerInteractionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LuckyColorService")
class LuckyColorServiceTest {

    @Mock
    private LuckyColorScoringService scoringService;

    @Mock
    private CustomerInteractionRepository interactionRepository;

    private LuckyColorService service;

    @BeforeEach
    void setUp() {
        service = new LuckyColorService(scoringService, new ColorRuleEngine(), interactionRepository);
        lenient().when(scoringService.recommend(anyString(), any(), any(), anyInt())).thenReturn(List.of());
    }

    private LuckyColorDTO.Request request(String token, String day, String goal) {
        return new LuckyColorDTO.Request(token, day, goal, null);
    }

    @Test
    @DisplayName("ชื่อสีและ hex ที่ส่งกลับมาจากกฎชุดเดียวกับที่ใช้คัดสินค้า")
    void resultUsesSingleColorSource() {
        // อังคาร + การงาน ไม่มีสีซ้ำกัน: สีการงาน 3 สีแรก แล้วช่องสุดท้ายจองให้สีวันเกิดอันดับแรก
        LuckyColorDTO.Result result = service.compute(request("t1", "อังคาร", "การงาน"));

        assertThat(result.color()).isEqualTo("ฟ้า / น้ำเงิน / เทา / ดำ");
        assertThat(result.colorHexes()).hasSize(4);
        assertThat(result.reasoning()).startsWith("เกิดวันอังคาร เสริมด้านการงาน สีมงคลที่เหมาะ ได้แก่ สีฟ้า / น้ำเงิน / เทา / ดำ");
    }

    @Test
    @DisplayName("ไม่ส่งวันเกิด / วันที่ไม่มีจริง / เป้าหมายที่ไม่มีจริง ต้องได้ InvalidRequestException และไม่บันทึกสถิติ")
    void rejectsInvalidInput() {
        assertThatThrownBy(() -> service.compute(request("t1", null, "การงาน")))
                .isInstanceOf(InvalidRequestException.class).hasMessageContaining("เลือกวันเกิด");
        assertThatThrownBy(() -> service.compute(request("t1", "  ", null)))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.compute(request("t1", "วันหยุด", "การงาน")))
                .isInstanceOf(InvalidRequestException.class).hasMessageContaining("วันหยุด");
        assertThatThrownBy(() -> service.compute(request("t1", "จันทร์", "อะไรก็ได้")))
                .isInstanceOf(InvalidRequestException.class).hasMessageContaining("อะไรก็ได้");

        verify(interactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("ส่งเพศต่อไปให้ตัวคัดสินค้า และเพศที่ไม่รู้จักต้องได้ InvalidRequestException")
    void passesGenderToScoring() {
        service.compute(new LuckyColorDTO.Request("t1", "จันทร์", null, "female"));
        verify(scoringService).recommend("จันทร์", null, "FEMALE", LuckyColorService.MAX_PRODUCTS);

        service.compute(new LuckyColorDTO.Request("t1", "จันทร์", null, null));
        verify(scoringService).recommend("จันทร์", null, null, LuckyColorService.MAX_PRODUCTS);

        assertThatThrownBy(() -> service.compute(new LuckyColorDTO.Request("t1", "จันทร์", null, "UNISEX")))
                .isInstanceOf(InvalidRequestException.class).hasMessageContaining("UNISEX");
    }

    @Test
    @DisplayName("ไม่เลือกเป้าหมายก็ได้ผลลัพธ์ และไม่บันทึกสถิติ")
    void goalIsOptional() {
        LuckyColorDTO.Result result = service.compute(request("t1", "จันทร์", null));

        assertThat(result.reasoning()).endsWith("ช่วยเสริมดวงโดยรวม");
        verify(interactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("token เดิม + เป้าหมายเดิม บันทึกแค่ครั้งแรก พร้อมเก็บ token ไว้")
    void recordsOncePerTokenAndGoal() {
        when(interactionRepository.existsByClientTokenAndEventTypeAndLuckyColor("anon-abc", "LUCKY_COLOR_SEARCH", "ความรัก"))
                .thenReturn(false, true);

        service.compute(request("anon-abc", "จันทร์", "ความรัก"));
        service.compute(request("anon-abc", "ศุกร์", "ความรัก")); // เปลี่ยนวันเกิดแต่เป้าหมายเดิม

        ArgumentCaptor<CustomerInteraction> saved = ArgumentCaptor.forClass(CustomerInteraction.class);
        verify(interactionRepository, times(1)).save(saved.capture());
        assertThat(saved.getValue().getClientToken()).isEqualTo("anon-abc");
        assertThat(saved.getValue().getLuckyColor()).isEqualTo("ความรัก");
    }

    @Test
    @DisplayName("ไม่มี token นับทุกครั้ง เพราะไม่มีอะไรให้แยกว่าเป็นคนเดิม")
    void recordsEveryTimeWithoutToken() {
        service.compute(request(null, "จันทร์", "ความรัก"));
        service.compute(request(null, "จันทร์", "ความรัก"));

        verify(interactionRepository, times(2)).save(any());
        verify(interactionRepository, never()).existsByClientTokenAndEventTypeAndLuckyColor(any(), any(), any());
    }
}
