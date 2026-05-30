package com.backend.service;

import com.backend.dto.SensitiveWordListVO;

public interface SensitiveWordService {

    SensitiveWordListVO listWords();

    void addWord(String word);

    void removeWord(String word);

    void reload();
}
