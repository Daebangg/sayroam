package com.daebangg.sayroam.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration(proxyBeanMethods = false)
public class RedisConfig {
    @Bean
    StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        // ConnectionFactory는 spring.data.redis.*를 사용하는 Boot 자동 설정에 맡긴다.
        return new StringRedisTemplate(connectionFactory);
    }
}
