package com.smartsell.service;

import com.smartsell.dto.ChatDTO;
import com.smartsell.entity.ChatMessage;
import com.smartsell.entity.CustomerSession;
import com.smartsell.repository.ChatMessageRepository;
import com.smartsell.repository.CustomerInteractionRepository;
import com.smartsell.repository.CustomerSessionRepository;
import com.smartsell.repository.ProductRepository;
import com.smartsell.repository.SizeChartRowRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ChatService")
class ChatServiceTest {

    @Test
    @DisplayName("XXL, XXXL, XXXXL แปลงเป็น 2XL, 3XL, 4XL ให้ตรงกับไซส์ใน DB")
    void normalizeSizeMapsXxlToNumbered() {
        assertThat(ChatService.normalizeSize("XXL")).isEqualTo("2XL");
        assertThat(ChatService.normalizeSize("xxxl")).isEqualTo("3XL");
        assertThat(ChatService.normalizeSize("XXXXL")).isEqualTo("4XL");
    }

    @Test
    @DisplayName("ไซส์ที่ Gemini คืนมาซ้ำติดกัน เก็บแค่ชุดแรก")
    void normalizeSizeCollapsesRepeats() {
        assertThat(ChatService.normalizeSize("SSSS")).isEqualTo("SS");
        assertThat(ChatService.normalizeSize("XXXLXXXL")).isEqualTo("3XL");
        assertThat(ChatService.normalizeSize("2XL2XL")).isEqualTo("2XL");
        assertThat(ChatService.normalizeSize("XLXL")).isEqualTo("XL");
    }

    @Test
    @DisplayName("ตอบครบแต่ไม่บอกสี ถือว่าข้าม และแนะนำสินค้าให้")
    void recommendsWhenColorNeverAnswered() {
        CustomerSessionRepository sessions = mock(CustomerSessionRepository.class);
        GeminiClientService gemini = mock(GeminiClientService.class);
        ProductScoringService scoring = mock(ProductScoringService.class);
        CustomerSession session = new CustomerSession();
        session.setId(1L);
        session.setOccasion("ทั่วไป");
        session.setBudgetMin(java.math.BigDecimal.ZERO);
        session.setSize("L");
        when(sessions.findById(1L)).thenReturn(java.util.Optional.of(session));
        when(sessions.save(any(CustomerSession.class))).thenAnswer(call -> call.getArgument(0));
        GeminiClientService.AssistantResponse reply = new GeminiClientService.AssistantResponse();
        reply.setReply("ครบแล้วค่ะ");
        reply.setGender("ANY");
        when(gemini.generateAssistantReply(any(), anyString(), any())).thenReturn(reply);
        ChatService service = new ChatService(sessions, mock(ChatMessageRepository.class), scoring, gemini,
                mock(ProductRepository.class), mock(CustomerInteractionRepository.class),
                mock(StoreInfoService.class), mock(SizeChartRowRepository.class));

        ChatDTO.ChatResponse response = service.handleShoppingAssistantChat(new ChatDTO.ChatRequest(1L, "ไม่ระบุเพศ"));

        assertThat(response.preferences().personalColor()).isEqualTo("Unknown");
        verify(scoring).recommendByPersonalColor("Unknown", "ทั่วไป", null, null, "L", "ANY", 3);
    }

    @Test
    @DisplayName("ดึงประวัติก่อนบันทึกข้อความใหม่ ไม่ให้ Gemini เห็นข้อความเดิมซ้ำ")
    void readsHistoryBeforeSavingNewMessage() {
        CustomerSessionRepository sessions = mock(CustomerSessionRepository.class);
        ChatMessageRepository messages = mock(ChatMessageRepository.class);
        GeminiClientService gemini = mock(GeminiClientService.class);
        ProductRepository products = mock(ProductRepository.class);
        when(sessions.save(any(CustomerSession.class))).thenAnswer(call -> {
            CustomerSession session = call.getArgument(0);
            session.setId(1L);
            return session;
        });
        when(messages.findBySessionIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());
        when(products.findAllWithVariants()).thenReturn(List.of());
        when(gemini.generateSupportReply(any(), anyString(), anyString(), any())).thenReturn("ตอบ");
        ChatService service = new ChatService(sessions, messages, mock(ProductScoringService.class), gemini, products,
                mock(CustomerInteractionRepository.class), mock(StoreInfoService.class), mock(SizeChartRowRepository.class));

        service.handleGeneralSupportChat(new ChatDTO.ChatRequest(null, "สวัสดี"));

        InOrder order = inOrder(messages);
        order.verify(messages).findBySessionIdOrderByCreatedAtAsc(1L);
        order.verify(messages, times(2)).save(any(ChatMessage.class)); // ข้อความลูกค้า + คำตอบ AI
    }

    @Test
    @DisplayName("ไซส์อื่นแค่ตัดช่องว่างและทำเป็นตัวพิมพ์ใหญ่")
    void normalizeSizeKeepsOtherSizes() {
        assertThat(ChatService.normalizeSize(" xl ")).isEqualTo("XL");
        assertThat(ChatService.normalizeSize("2xl")).isEqualTo("2XL");
        assertThat(ChatService.normalizeSize("SS")).isEqualTo("SS");
        assertThat(ChatService.normalizeSize("  ")).isNull();
        assertThat(ChatService.normalizeSize(null)).isNull();
    }
}
