package com.backend.common;

public final class RedisKeys {

    public static final String POST_LIKES = "post:likes:";
    public static final String POST_VIEWS = "post:views:";
    public static final String POST_LIKED_SET = "post:liked:";
    public static final String COMMENT_LIKES = "comment:likes:";
    public static final String COMMENT_LIKED_SET = "comment:liked:";
    public static final String FAVORITE_SET = "favorite:set:";
    public static final String FAVORITE_COUNT = "favorite:count:";
    public static final String FAVORITE_USER = "favorite:user:";
    public static final String DIRTY_POSTS = "sync:dirty:posts";
    public static final String DIRTY_COMMENTS = "sync:dirty:comments";
    public static final String DIRTY_FAVORITES = "sync:dirty:favorites";
    public static final String TOKEN_BLACKLIST = "token:blacklist:";
    public static final String RATE_LIMIT = "rate_limit:";
    public static final String REVIEW_LOCK = "review:lock:";
    public static final String RECOMMEND_FEED = "rec:feed:";
    public static final String AI_REVIEW_PROMPT = "admin:ai:review_prompt";
    public static final String AI_REVIEW_PROMPT_UPDATE_TIME = "admin:ai:review_prompt_update_time";
    public static final String AI_IMAGE_REVIEW_PROMPT = "admin:ai:image_review_prompt";
    public static final String AI_IMAGE_REVIEW_PROMPT_UPDATE_TIME = "admin:ai:image_review_prompt_update_time";
    public static final String RECOMMEND_WEIGHT_HOT = "admin:recommend:weight_hot";
    public static final String RECOMMEND_WEIGHT_TAG = "admin:recommend:weight_tag";
    public static final String RECOMMEND_WEIGHT_FRESH = "admin:recommend:weight_fresh";
    public static final String RECOMMEND_WEIGHT_DIVERSITY = "admin:recommend:weight_diversity";
    public static final String RECOMMEND_TAG_MATCH_LIMIT = "admin:recommend:tag_match_limit";
    public static final String RECOMMEND_HOT_LIMIT = "admin:recommend:hot_limit";
    public static final String RECOMMEND_ITEM_CF_LIMIT = "admin:recommend:item_cf_limit";
    public static final String RECOMMEND_CONFIG_UPDATE_TIME = "admin:recommend:config_update_time";
    public static final String REVIEW_EMAIL_ENABLED = "admin:review_email:enabled";
    public static final String REVIEW_EMAIL_SUBJECT = "admin:review_email:subject";
    public static final String REVIEW_EMAIL_SMTP_HOST = "admin:review_email:smtp_host";
    public static final String REVIEW_EMAIL_SMTP_PORT = "admin:review_email:smtp_port";
    public static final String REVIEW_EMAIL_SMTP_USERNAME = "admin:review_email:smtp_username";
    public static final String REVIEW_EMAIL_SMTP_PASSWORD = "admin:review_email:smtp_password";
    public static final String REVIEW_EMAIL_INTERVAL_MINUTES = "admin:review_email:interval_minutes";
    public static final String REVIEW_EMAIL_LAST_AUDIT_ID = "review:email:last_audit_id";

    private RedisKeys() {}
}
