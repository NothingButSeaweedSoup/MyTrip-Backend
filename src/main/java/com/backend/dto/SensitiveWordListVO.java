package com.backend.dto;

import java.util.List;

public class SensitiveWordListVO {

    private int baseTotal;
    private int customTotal;
    private List<String> words;

    public SensitiveWordListVO(int baseTotal, int customTotal, List<String> words) {
        this.baseTotal = baseTotal;
        this.customTotal = customTotal;
        this.words = words;
    }

    public int getBaseTotal() { return baseTotal; }
    public void setBaseTotal(int baseTotal) { this.baseTotal = baseTotal; }
    public int getCustomTotal() { return customTotal; }
    public void setCustomTotal(int customTotal) { this.customTotal = customTotal; }
    public List<String> getWords() { return words; }
    public void setWords(List<String> words) { this.words = words; }
}
