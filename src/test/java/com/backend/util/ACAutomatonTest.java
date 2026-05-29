package com.backend.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ACAutomatonTest {

    private ACAutomaton automaton;

    @BeforeEach
    void setUp() {
        automaton = new ACAutomaton();
        automaton.build(List.of("赌博", "毒品", "色情", "暴力", "代开发票", "枪支"));
    }

    @Test
    void shouldMatchFirstSensitiveWord() {
        assertNotNull(automaton.match("在线赌博网站推荐"));
        assertEquals("赌博", automaton.match("在线赌博网站推荐"));
    }

    @Test
    void shouldReturnNullForCleanText() {
        assertNull(automaton.match("大理洱海边的日出，治愈了我所有的疲惫"));
    }

    @Test
    void shouldMatchAllSensitiveWords() {
        String text = "毒品和赌博都是违法行为，色情内容也不行，涉及暴力的也要禁止";
        List<String> hits = automaton.matchAll(text);
        assertTrue(hits.contains("毒品"));
        assertTrue(hits.contains("赌博"));
        assertTrue(hits.contains("色情"));
        assertTrue(hits.contains("暴力"));
    }

    @Test
    void shouldHandleNullInput() {
        assertNull(automaton.match(null));
        assertNull(automaton.match(""));
        assertTrue(automaton.matchAll(null).isEmpty());
        assertTrue(automaton.matchAll("").isEmpty());
    }

    @Test
    void shouldMatchChineseSensitiveWords() {
        assertNotNull(automaton.match("购买毒品违法"));
        assertNotNull(automaton.match("代开发票"));
    }

    @Test
    void shouldMatchWithEmptyKeywordList() {
        ACAutomaton empty = new ACAutomaton();
        empty.build(List.of());
        assertNull(empty.match("任何文本"));
    }

    @Test
    void shouldMatchOverlappingKeywords() {
        ACAutomaton ac = new ACAutomaton();
        // "枪支" overlaps with "枪支弹药" conceptually, but here they're separate entries
        ac.build(List.of("枪支", "枪支弹药"));

        assertNotNull(ac.match("贩卖枪支弹药"));
        // Should match at least one of them
        String hit = ac.match("贩卖枪支弹药");
        assertTrue("枪支".equals(hit) || "枪支弹药".equals(hit));
    }
}
