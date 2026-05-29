package com.backend.service;

import com.backend.dto.ScenicSpotCreateRequest;
import com.backend.dto.ScenicSpotEditRequest;
import com.backend.dto.ScenicSpotVO;
import com.backend.entity.ScenicSpot;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ScenicSpotService extends IService<ScenicSpot> {

    List<ScenicSpot> listByCity(String city);

    /** 管理员：新增景点 */
    Long createSpot(Long adminUserId, ScenicSpotCreateRequest request);

    /** 管理员：编辑景点 */
    void updateSpot(Long adminUserId, Long spotId, ScenicSpotEditRequest request);

    /** 管理员：景点列表（分页，含已下架） */
    IPage<ScenicSpotVO> listSpotsForAdmin(Long adminUserId, String city, String keyword, int page, int pageSize);

    /** 管理员：景点上架/下架 */
    void updateSpotStatus(Long adminUserId, Long spotId, Integer status);

    /** 管理员：批量导入景点 */
    void importSpots(Long adminUserId, MultipartFile file);
}
