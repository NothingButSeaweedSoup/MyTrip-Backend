package com.backend.service.impl;

import com.backend.common.BusinessException;
import com.backend.common.UnauthorizedException;
import com.backend.dto.ScenicSpotCreateRequest;
import com.backend.dto.ScenicSpotEditRequest;
import com.backend.dto.ScenicSpotVO;
import com.backend.entity.ScenicSpot;
import com.backend.entity.SpotTag;
import com.backend.entity.User;
import com.backend.mapper.ScenicSpotMapper;
import com.backend.mapper.SpotTagMapper;
import com.backend.mapper.UserMapper;
import com.backend.service.ScenicSpotService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class ScenicSpotServiceImpl extends ServiceImpl<ScenicSpotMapper, ScenicSpot>
    implements ScenicSpotService {

    private static final Logger log = LoggerFactory.getLogger(ScenicSpotServiceImpl.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SpotTagMapper spotTagMapper;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<ScenicSpot> listByCity(String city) {
        return lambdaQuery()
                .eq(ScenicSpot::getCity, city)
                .eq(ScenicSpot::getStatus, 0)
                .list();
    }

    @Override
    @Transactional
    public Long createSpot(Long adminUserId, ScenicSpotCreateRequest request) {
        checkAdminRole(adminUserId);
        if (lambdaQuery().eq(ScenicSpot::getName, request.getName())
                .eq(ScenicSpot::getCity, request.getCity()).exists()) {
            throw new BusinessException("该城市下已存在同名景点");
        }
        ScenicSpot spot = new ScenicSpot();
        spot.setName(request.getName());
        spot.setCity(request.getCity());
        spot.setAddress(request.getAddress());
        spot.setLatitude(request.getLatitude());
        spot.setLongitude(request.getLongitude());
        spot.setDescription(request.getDescription());
        spot.setRating(request.getRating());
        spot.setVisitDuration(request.getVisitDuration());
        spot.setOpenTime(request.getOpenTime());
        spot.setPhone(request.getPhone());
        spot.setCoverImage(request.getCoverImage());
        spot.setStatus(0);
        spot.setCreateTime(new Date());
        spot.setUpdateTime(new Date());
        if (!CollectionUtils.isEmpty(request.getTagIds())) {
            try {
                spot.setTags(objectMapper.writeValueAsString(request.getTagIds()));
            } catch (Exception ignored) {
            }
        }
        save(spot);
        if (!CollectionUtils.isEmpty(request.getTagIds())) {
            for (Integer tagId : request.getTagIds()) {
                SpotTag st = new SpotTag();
                st.setSpotId(spot.getSpotId());
                st.setTagId(tagId);
                spotTagMapper.insert(st);
            }
        }
        return spot.getSpotId();
    }

    @Override
    @Transactional
    public void updateSpot(Long adminUserId, Long spotId, ScenicSpotEditRequest request) {
        checkAdminRole(adminUserId);
        ScenicSpot spot = getById(spotId);
        if (spot == null) {
            throw new BusinessException("景点不存在");
        }
        if (StringUtils.hasText(request.getName())) spot.setName(request.getName());
        if (StringUtils.hasText(request.getCity())) spot.setCity(request.getCity());
        if (StringUtils.hasText(request.getAddress())) spot.setAddress(request.getAddress());
        if (request.getLatitude() != null) spot.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) spot.setLongitude(request.getLongitude());
        if (StringUtils.hasText(request.getDescription())) spot.setDescription(request.getDescription());
        if (request.getRating() != null) spot.setRating(request.getRating());
        if (request.getVisitDuration() != null) spot.setVisitDuration(request.getVisitDuration());
        if (StringUtils.hasText(request.getOpenTime())) spot.setOpenTime(request.getOpenTime());
        if (StringUtils.hasText(request.getPhone())) spot.setPhone(request.getPhone());
        if (StringUtils.hasText(request.getCoverImage())) spot.setCoverImage(request.getCoverImage());
        spot.setUpdateTime(new Date());
        if (request.getTagIds() != null) {
            try {
                spot.setTags(objectMapper.writeValueAsString(request.getTagIds()));
            } catch (Exception ignored) {
            }
            spotTagMapper.delete(new LambdaQueryWrapper<SpotTag>().eq(SpotTag::getSpotId, spotId));
            for (Integer tagId : request.getTagIds()) {
                SpotTag st = new SpotTag();
                st.setSpotId(spotId);
                st.setTagId(tagId);
                spotTagMapper.insert(st);
            }
        }
        updateById(spot);
    }

    @Override
    public IPage<ScenicSpotVO> listSpotsForAdmin(Long adminUserId, String city, String keyword, int page, int pageSize) {
        checkAdminRole(adminUserId);
        LambdaQueryWrapper<ScenicSpot> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(city)) {
            wrapper.eq(ScenicSpot::getCity, city);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(ScenicSpot::getName, keyword);
        }
        wrapper.orderByDesc(ScenicSpot::getUpdateTime);
        IPage<ScenicSpot> spotPage = page(new Page<>(page, pageSize), wrapper);
        return spotPage.convert(this::toScenicSpotVO);
    }

    @Override
    @Transactional
    public void updateSpotStatus(Long adminUserId, Long spotId, Integer status) {
        checkAdminRole(adminUserId);
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("无效的状态值");
        }
        ScenicSpot spot = getById(spotId);
        if (spot == null) {
            throw new BusinessException("景点不存在");
        }
        lambdaUpdate().eq(ScenicSpot::getSpotId, spotId)
                .set(ScenicSpot::getStatus, status)
                .set(ScenicSpot::getUpdateTime, new Date())
                .update();
    }

    @Override
    @Transactional
    public void importSpots(Long adminUserId, MultipartFile file) {
        checkAdminRole(adminUserId);
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.endsWith(".csv")) {
            throw new BusinessException("仅支持CSV文件");
        }
        try {
            importFromCsv(file);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("导入失败: " + e.getMessage());
        }
    }

    private void importFromCsv(MultipartFile file) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line = reader.readLine(); // skip header
            int count = 0;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (parts.length < 2) continue;
                ScenicSpot spot = new ScenicSpot();
                spot.setName(parts[0].trim().replace("\"", ""));
                spot.setCity(parts.length > 1 ? parts[1].trim().replace("\"", "") : "");
                spot.setAddress(parts.length > 2 ? parts[2].trim().replace("\"", "") : "");
                if (parts.length > 3 && !parts[3].trim().isEmpty()) {
                    spot.setLatitude(new BigDecimal(parts[3].trim().replace("\"", "")));
                }
                if (parts.length > 4 && !parts[4].trim().isEmpty()) {
                    spot.setLongitude(new BigDecimal(parts[4].trim().replace("\"", "")));
                }
                spot.setDescription(parts.length > 5 ? parts[5].trim().replace("\"", "") : "");
                spot.setStatus(0);
                spot.setCreateTime(new Date());
                spot.setUpdateTime(new Date());
                save(spot);
                count++;
            }
            log.info("批量导入景点完成，共导入 {} 条", count);
        }
    }

    private void checkAdminRole(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getRole() != 9) {
            throw new UnauthorizedException("无管理员权限");
        }
    }

    private ScenicSpotVO toScenicSpotVO(ScenicSpot spot) {
        List<String> tagNames = new ArrayList<>();
        if (spot.getTags() != null) {
            try {
                List<Integer> tagIds = objectMapper.readValue(spot.getTags().toString(),
                        new com.fasterxml.jackson.core.type.TypeReference<List<Integer>>() {});
                tagNames = tagIds.stream().map(String::valueOf).toList();
            } catch (Exception ignored) {
            }
        }
        return ScenicSpotVO.builder()
                .spotId(spot.getSpotId())
                .name(spot.getName())
                .city(spot.getCity())
                .address(spot.getAddress())
                .latitude(spot.getLatitude())
                .longitude(spot.getLongitude())
                .description(spot.getDescription())
                .tags(tagNames)
                .rating(spot.getRating())
                .visitDuration(spot.getVisitDuration())
                .openTime(spot.getOpenTime())
                .phone(spot.getPhone())
                .coverImage(spot.getCoverImage())
                .status(spot.getStatus())
                .build();
    }
}




