package com.backend.service;

import com.backend.dto.ModelConfigUpdateRequest;
import com.backend.dto.ModelConfigVO;

public interface ModelConfigService {

    ModelConfigVO getConfig();

    void updateConfig(ModelConfigUpdateRequest request);
}
