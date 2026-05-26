package com.staycommerce.config.redis;

public record RedisNodeInfo(
        String host,
        int port
) { }
