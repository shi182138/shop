package com.example.common.to;

import lombok.Data;

@Data
public class SocialUserTo {
    /**
     * 令牌
     */
    private String access_token;


    /**
     * 令牌过期时间
     */
    private long expires_in;

    /**
     * 该社交用户的唯一标识
     */
    private String id;


    /**
     * 第三方用户名称
     */
    private String name;

    /**
     * 头像
     */
    private String  avatar_url;
}
