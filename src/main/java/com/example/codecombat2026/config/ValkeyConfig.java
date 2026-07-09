package com.example.codecombat2026.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Two separate Valkey connection pools:
 *
 * 1. apiRedisTemplate  — for API requests (cache GET/SET)
 *    - commandTimeout: 2s (fast fail if Valkey is slow)
 *    - pool size: 20 connections
 *
 * 2. workerRedisTemplate — for judge workers (BRPOP queue)
 *    - commandTimeout: 10s (allows BRPOP to block for 3s without timeout)
 *    - pool size: 16 connections (2 per worker × 8 workers)
 *
 * Previously both shared one pool — workers' BRPOP blocked API requests.
 */
@Configuration
public class ValkeyConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.username}")
    private String username;

    @Value("${spring.data.redis.password}")
    private String password;

    // ─── Shared server config ─────────────────────────────────────────────────

    private RedisStandaloneConfiguration serverConfig() {
        RedisStandaloneConfiguration cfg = new RedisStandaloneConfiguration();
        cfg.setHostName(host);
        cfg.setPort(port);
        // Only set username if non-empty (local Redis doesn't use ACL username)
        if (username != null && !username.isBlank()) {
            cfg.setUsername(username);
        }
        if (password != null && !password.isBlank()) {
            cfg.setPassword(password);
        }
        return cfg;
    }

    private ClientOptions clientOptions(int connectTimeoutMs) {
        return ClientOptions.builder()
                .socketOptions(SocketOptions.builder()
                        .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                        .build())
                .autoReconnect(true)
                .build();
    }

    // ─── Pool 1: API requests (fast timeout) ─────────────────────────────────

    @Bean(name = "apiConnectionFactory")
    @Primary
    public LettuceConnectionFactory apiConnectionFactory() {
        GenericObjectPoolConfig<io.lettuce.core.api.StatefulConnection<?, ?>> pool =
                new GenericObjectPoolConfig<>();
        pool.setMaxTotal(20);
        pool.setMaxIdle(10);
        pool.setMinIdle(5);
        pool.setMaxWait(Duration.ofMillis(1000));
        pool.setTestOnBorrow(false); // skip test — faster borrow
        pool.setTestWhileIdle(true);

        LettucePoolingClientConfiguration cfg = LettucePoolingClientConfiguration.builder()
                .poolConfig(pool)
                .clientOptions(clientOptions(3000))
                .commandTimeout(Duration.ofMillis(2000)) // fast fail for API
                .build();

        return new LettuceConnectionFactory(serverConfig(), cfg);
    }

    // ─── Pool 2: Worker queue (long timeout for BRPOP) ───────────────────────

    @Bean(name = "workerConnectionFactory")
    public LettuceConnectionFactory workerConnectionFactory() {
        GenericObjectPoolConfig<io.lettuce.core.api.StatefulConnection<?, ?>> pool =
                new GenericObjectPoolConfig<>();
        pool.setMaxTotal(16); // 2 per worker × 8 workers
        pool.setMaxIdle(16);
        pool.setMinIdle(8);
        pool.setMaxWait(Duration.ofMillis(2000));
        pool.setTestOnBorrow(false);
        pool.setTestWhileIdle(true);

        LettucePoolingClientConfiguration cfg = LettucePoolingClientConfiguration.builder()
                .poolConfig(pool)
                .clientOptions(clientOptions(3000))
                .commandTimeout(Duration.ofSeconds(10)) // allows BRPOP 3s block + overhead
                .build();

        return new LettuceConnectionFactory(serverConfig(), cfg);
    }

    // ─── Templates ───────────────────────────────────────────────────────────

    /**
     * Primary template — used by all services (ContestService, ProblemService, etc.)
     * Uses the fast API connection pool.
     */
    @Bean(name = "stringRedisTemplate")
    @Primary
    public StringRedisTemplate stringRedisTemplate(
            @org.springframework.beans.factory.annotation.Qualifier("apiConnectionFactory")
            LettuceConnectionFactory factory) {
        StringRedisTemplate t = new StringRedisTemplate();
        t.setConnectionFactory(factory);
        t.setKeySerializer(new StringRedisSerializer());
        t.setValueSerializer(new StringRedisSerializer());
        t.setHashKeySerializer(new StringRedisSerializer());
        t.setHashValueSerializer(new StringRedisSerializer());
        t.afterPropertiesSet();
        return t;
    }

    /**
     * Worker template — used only by SubmissionWorkerPool for BRPOP.
     * Uses the long-timeout worker connection pool.
     */
    @Bean(name = "workerRedisTemplate")
    public StringRedisTemplate workerRedisTemplate(
            @org.springframework.beans.factory.annotation.Qualifier("workerConnectionFactory")
            LettuceConnectionFactory factory) {
        StringRedisTemplate t = new StringRedisTemplate();
        t.setConnectionFactory(factory);
        t.setKeySerializer(new StringRedisSerializer());
        t.setValueSerializer(new StringRedisSerializer());
        t.afterPropertiesSet();
        return t;
    }

    /**
     * Generic RedisTemplate<String,Object> — used where a non-string-typed
     * template is required (e.g. PrivateContestMetricsConfig's queue-length
     * gauge). Backed by the same fast API connection pool. Keys are strings;
     * values use JDK serialization (only LLEN/size ops are used today, so the
     * value serializer choice is not significant).
     */
    @Bean(name = "objectRedisTemplate")
    public org.springframework.data.redis.core.RedisTemplate<String, Object> objectRedisTemplate(
            @org.springframework.beans.factory.annotation.Qualifier("apiConnectionFactory")
            LettuceConnectionFactory factory) {
        org.springframework.data.redis.core.RedisTemplate<String, Object> t =
                new org.springframework.data.redis.core.RedisTemplate<>();
        t.setConnectionFactory(factory);
        t.setKeySerializer(new StringRedisSerializer());
        t.setHashKeySerializer(new StringRedisSerializer());
        t.afterPropertiesSet();
        return t;
    }

    // ─── ObjectMapper ─────────────────────────────────────────────────────────

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
