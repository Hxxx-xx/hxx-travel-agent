package com.hxx.travel.agent.api;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 应用程序入口
 *
 * @author hxx
 */
@SpringBootApplication(scanBasePackages = "com.hxx.travel.agent")
@MapperScan("com.hxx.travel.agent.trip.mapper")
public class HxxTravelAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(HxxTravelAgentApplication.class, args);
    }

    @Bean
    @ConditionalOnMissingBean(StringRedisTemplate.class)
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
