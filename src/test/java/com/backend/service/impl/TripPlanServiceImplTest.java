package com.backend.service.impl;

import com.backend.entity.ScenicSpot;
import com.backend.entity.TripPlan;
import com.backend.entity.TripSession;
import com.backend.mapper.TripPlanLocationMapper;
import com.backend.mapper.TripPlanMapper;
import com.backend.service.ScenicSpotService;
import com.backend.service.TripSessionService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TripPlanServiceImpl 单元测试
 * 测试 generatePlanWithAI 方法（无需真实数据库/LLM）
 */
@ExtendWith(MockitoExtension.class)
class TripPlanServiceImplTest {

    @Mock
    private ChatModel chatModel;

    @Mock
    private ScenicSpotService spotService;

    @Mock
    private TripSessionService sessionService;

    @Mock
    private TripPlanLocationMapper locationMapper;

    @Mock
    private TripPlanMapper tripPlanMapper;

    @InjectMocks
    private TripPlanServiceImpl planService;

    @BeforeEach
    void setUp() {
        // 设置 baseMapper（ServiceImpl 的 protected 字段）
        ReflectionTestUtils.setField(planService, "baseMapper", tripPlanMapper);

        // Mock 会话
        TripSession session = new TripSession();
        session.setSessionId(42L);
        session.setUserId(100L);
        session.setPlanId(null);
        lenient().when(sessionService.getById(42L)).thenReturn(session);

        // Mock 景点数据
        ScenicSpot spot1 = new ScenicSpot();
        spot1.setSpotId(1L);
        spot1.setName("广州塔");
        spot1.setCity("广州");
        spot1.setAddress("广东省广州市海珠区阅江西路222号");
        spot1.setLatitude(BigDecimal.valueOf(23.10622633));
        spot1.setLongitude(BigDecimal.valueOf(113.32454511));
        spot1.setRating(BigDecimal.valueOf(4.6));
        spot1.setVisitDuration(240);
        spot1.setOpenTime("09:30-22:30");
        spot1.setTags("[\"遛娃宝藏地\", \"亲子同乐\"]");
        spot1.setDescription("打卡地标璀璨夜景");

        ScenicSpot spot2 = new ScenicSpot();
        spot2.setSpotId(2L);
        spot2.setName("陈家祠");
        spot2.setCity("广州");
        spot2.setAddress("广州市荔湾区中山七路恩龙里34号");
        spot2.setLatitude(BigDecimal.valueOf(23.12685416));
        spot2.setLongitude(BigDecimal.valueOf(113.24545284));
        spot2.setRating(BigDecimal.valueOf(4.6));
        spot2.setVisitDuration(120);
        spot2.setOpenTime("09:00-18:00");
        spot2.setTags("[\"赏花胜地\", \"历史建筑\"]");
        spot2.setDescription("建筑考究的清末宗祠");

        lenient().when(spotService.listByCity("广州")).thenReturn(List.of(spot1, spot2));

        // Mock LLM 响应
        String llmJson = "{\"weather\":\"晴\",\"itinerary\":[{\"day\":1,\"date\":\"第1天\",\"weather\":\"晴\",\"spots\":[{\"timeSlot\":\"上午\",\"name\":\"陈家祠\",\"address\":\"广州市荔湾区中山七路恩龙里34号\",\"duration\":\"2小时\",\"transport\":\"地铁约30分钟\",\"note\":\"建筑考究的清末宗祠\",\"lat\":23.1269,\"lng\":113.2455}]}]}";

        AiMessage aiMessage = AiMessage.from(llmJson);
        ChatResponse chatResponse = ChatResponse.builder().aiMessage(aiMessage).build();
        lenient().when(chatModel.chat(any(ChatRequest.class))).thenReturn(chatResponse);

        // Mock Mapper insert
        lenient().when(tripPlanMapper.insert(any(TripPlan.class))).thenReturn(1);
    }

    @Test
    void testGeneratePlanWithAI_Success() {
        String result = planService.generatePlanWithAI(
                42L, "广州", 1, "low", "自然风光,美食,历史文化", "moderate");

        assertNotNull(result);
        assertTrue(result.startsWith("已生成"),
                "预期结果以'已生成'开头，实际='" + result + "'");
        assertTrue(result.contains("广州"));
        assertTrue(result.contains("1天"));
    }

    @Test
    void testGeneratePlanWithAI_ArgumentOrderFix() {
        // 验证 buildPlanPrompt 中 %d 对应 int、%s 对应 String，不会抛异常
        String result = planService.generatePlanWithAI(
                42L, "广州", 3, "high", "历史文化,购物", "relaxed");

        assertNotNull(result);
        assertTrue(result.startsWith("已生成"),
                "预期结果以'已生成'开头，实际='" + result + "'");
    }

    @Test
    void testGeneratePlanWithAI_NoSpots() {
        when(spotService.listByCity("空城")).thenReturn(List.of());

        String result = planService.generatePlanWithAI(
                42L, "空城", 1, "low", "自然风光", "moderate");

        assertEquals("计划生成失败", result);
    }

    @Test
    void testGeneratePlanWithAI_LLMFailure() {
        when(chatModel.chat(any(ChatRequest.class)))
                .thenThrow(new RuntimeException("API timeout"));

        String result = planService.generatePlanWithAI(
                42L, "广州", 1, "low", "自然风光", "moderate");

        assertEquals("计划生成失败", result);
    }

    @Test
    void testGeneratePlanWithAI_NullSession() {
        when(sessionService.getById(99L)).thenReturn(null);

        String result = planService.generatePlanWithAI(
                99L, "广州", 1, "low", "自然风光", "moderate");

        assertNotNull(result);
        assertTrue(result.startsWith("已生成"),
                "预期结果以'已生成'开头，实际='" + result + "'");
    }

    @Test
    void testGeneratePlanWithAI_MultipleCities() {
        ScenicSpot spot3 = new ScenicSpot();
        spot3.setSpotId(3L);
        spot3.setName("世界之窗");
        spot3.setCity("深圳");
        spot3.setAddress("深圳南山区");
        spot3.setLatitude(BigDecimal.valueOf(22.5365));
        spot3.setLongitude(BigDecimal.valueOf(113.9745));
        spot3.setRating(BigDecimal.valueOf(4.5));
        spot3.setVisitDuration(360);
        spot3.setOpenTime("09:00-22:00");
        spot3.setTags("[\"主题公园\"]");
        spot3.setDescription("主题公园");

        when(spotService.listByCity("广州")).thenReturn(List.of());
        when(spotService.listByCity("深圳")).thenReturn(List.of(spot3));
        when(spotService.listByCity("珠海")).thenReturn(List.of(spot3));

        String result = planService.generatePlanWithAI(
                42L, "广州,深圳,珠海", 2, "middle", "美食,购物", "moderate");

        assertNotNull(result);
        assertTrue(result.startsWith("已生成"),
                "预期结果以'已生成'开头，实际='" + result + "'");
    }
}
