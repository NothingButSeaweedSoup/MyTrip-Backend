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

    private RedisKeys() {}
}
