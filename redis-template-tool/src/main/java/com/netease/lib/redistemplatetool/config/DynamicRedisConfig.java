package com.netease.lib.redistemplatetool.config;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.Optional;

@Configuration
public class DynamicRedisConfig {
    private static final Logger log = LoggerFactory.getLogger("LCAP_EXTENSION_LOGGER");

    @Resource
    private RedisConfig redisConfig;

    @Bean
    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        // 使用String序列化器
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
//        if (RedisModeEnum.URL_MODE.getKey().equals(redisConfig.getRedisMode())) {
//            RedisURI redisURI = RedisURI.create(redisConfig.getRedisUrl());
//            // 创建配置
////            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
////            config.setHostName(redisURI.getHost());
////            config.setPort(redisURI.getPort());
//            return createStandaloneConnectionFactory();
//        } else if (RedisModeEnum.CLUSTER_MODE.getKey().equals(redisConfig.getRedisMode())) {
//            return createClusterConnectionFactory();
//        } else if (RedisModeEnum.SINGLE_MODE.getKey().equals(redisConfig.getRedisMode())) {
        return createStandaloneConnectionFactory();
//        } else if (RedisModeEnum.SENTINEL_MODE.getKey().equals(redisConfig.getRedisMode())) {
//            return createSentinelConnectionFactory();
//        } else {
//            return null;
//        }
    }


    private LettuceConnectionFactory createStandaloneConnectionFactory() {
        // 单机模式配置
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        Optional.ofNullable(redisConfig.getRedisHost()).filter(h -> !StringUtils.isEmpty(h)).ifPresent(config::setHostName);
        Optional.ofNullable(redisConfig.getRedisPort()).ifPresent(config::setPort);
        Optional.ofNullable(redisConfig.getRedisPassword()).filter(p -> !StringUtils.isEmpty(p)).ifPresent(config::setPassword);
        Optional.ofNullable(redisConfig.getRedisUsername()).filter(u -> !StringUtils.isEmpty(u)).ifPresent(config::setUsername);
        Optional.ofNullable(redisConfig.getRedisDatabase()).ifPresent(config::setDatabase);
        return new LettuceConnectionFactory(config, buildLettuceClientConfiguration());
    }

    private LettuceClientConfiguration buildLettuceClientConfiguration() {
        LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder = LettucePoolingClientConfiguration.builder();
        Optional.ofNullable(redisConfig.getSpringRedisTimeout()).ifPresent(r -> builder.commandTimeout(Duration.ofSeconds(r)));
        Optional.ofNullable(redisConfig.getSpringRedisLettuceShutdownTimeout()).ifPresent(r -> builder.shutdownTimeout(Duration.ofSeconds(r)));
        Optional.ofNullable(redisConfig.getRedisClientName()).filter(h -> !StringUtils.isEmpty(h)).ifPresent(builder::clientName);
        Optional.ofNullable(redisConfig.getSpringRedisSsl()).filter("true"::equals).ifPresent(h -> builder.useSsl());
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        Optional.ofNullable(redisConfig.getSpringRedisLettucePoolMaxActive()).ifPresent(poolConfig::setMaxTotal);
        Optional.ofNullable(redisConfig.getSpringRedisLettucePoolMaxIdle()).ifPresent(poolConfig::setMaxIdle);
        Optional.ofNullable(redisConfig.getSpringRedisLettucePoolMinIdle()).ifPresent(poolConfig::setMinIdle);
        Optional.ofNullable(redisConfig.getSpringRedisLettucePoolMaxWait()).ifPresent(poolConfig::setMaxWaitMillis);
        builder.poolConfig(poolConfig);
        return builder.build();
    }

//    private LettuceConnectionFactory createClusterConnectionFactory() {
//        // 集群模式配置
//        RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration();
//        clusterConfig.addClusterNode(new RedisNode("10.242.41.153", 7001));
//        clusterConfig.addClusterNode(new RedisNode("10.242.41.153", 7002));
//        clusterConfig.addClusterNode(new RedisNode("10.242.41.153", 7003));
//        clusterConfig.setPassword("yourpassword"); // 集群密码
//        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
//                .commandTimeout(Duration.ofSeconds(2))
//                .shutdownTimeout(Duration.ZERO)
//                .build();
//
//        return new LettuceConnectionFactory(clusterConfig, clientConfig);
//
//    }
//
//    private LettuceConnectionFactory createSentinelConnectionFactory() {
//        // 哨兵模式配置
//        RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration()
//                .master("mymaster")
//                .sentinel("sentinel1", 26379)
//                .sentinel("sentinel2", 26379)
//                .sentinel("sentinel3", 26379);
//        sentinelConfig.setPassword("yourpassword");
//        return new LettuceConnectionFactory(sentinelConfig);
//    }
}