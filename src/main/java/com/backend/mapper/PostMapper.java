package com.backend.mapper;

import com.backend.entity.Post;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface PostMapper extends BaseMapper<Post> {

    /** 批量更新热度分 */
    @Update("UPDATE post SET hot_score = ROUND(" +
            "LOG10(GREATEST(views, 0) + 1) * 0.3 + " +
            "LOG10(GREATEST(likes, 0) + 1) * 0.4 + " +
            "LOG10(GREATEST(comment_count, 0) + 1) * 0.2 + " +
            "(1 / (DATEDIFF(NOW(), create_time) + 3)) * 0.1" +
            ", 4), score_updated_at = NOW() " +
            "WHERE status = 1 AND (score_updated_at IS NULL OR score_updated_at < NOW() - INTERVAL 30 MINUTE)")
    int batchUpdateHotScore();

    @Select("SELECT * FROM post WHERE hash_id = #{hashId}")
    Post selectByHashId(@Param("hashId") String hashId);

    @Update("UPDATE post SET views = views + 1 WHERE post_id = #{postId}")
    int incrementViews(@Param("postId") Long postId);

    /** 全文检索已审核帖子，返回 post_id 和匹配分数 */
    @Select("SELECT post_id, MATCH(title, content) AGAINST(#{keyword} IN BOOLEAN MODE) AS score " +
            "FROM post WHERE status = 1 AND MATCH(title, content) AGAINST(#{keyword} IN BOOLEAN MODE) " +
            "ORDER BY score DESC")
    List<KeywordMatch> fulltextSearch(@Param("keyword") String keyword);

    /** 全文检索（无索引兜底）：用 LIKE 匹配标题和内容 */
    @Select("SELECT post_id, 1.0 AS score FROM post WHERE status = 1 AND " +
            "(title LIKE CONCAT('%', #{keyword}, '%') OR content LIKE CONCAT('%', #{keyword}, '%'))")
    List<KeywordMatch> likeSearch(@Param("keyword") String keyword);

    /** 通过标签名模糊匹配关联的帖子 */
    @Select("SELECT DISTINCT p.post_id, 0.8 AS score FROM post p " +
            "JOIN post_tag pt ON p.post_id = pt.post_id " +
            "JOIN tag t ON pt.tag_id = t.tag_id " +
            "WHERE p.status = 1 AND t.name LIKE CONCAT('%', #{keyword}, '%')")
    List<KeywordMatch> tagSearch(@Param("keyword") String keyword);
}




