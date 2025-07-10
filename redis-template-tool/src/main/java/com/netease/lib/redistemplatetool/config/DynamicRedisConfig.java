package com.netease.lib.redistemplatetool.config;

import com.netease.lib.redistemplatetool.util.RedisModeEnum;
import io.lettuce.core.RedisURI;
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
import java.util.stream.Collectors;

@Configuration
public class DynamicRedisConfig {
    private static final Logger log = LoggerFactory.getLogger("LCAP_EXTENSION_LOGGER");

    @Resource
    private RedisConfig redisConfig;

    public static void main(String[] args) {
//        RedisURI redisUri = RedisURI.Builder.sentinel("sentinel-host1", 26379, "mymaster")
//                .withSentinel("sentinel-host2", 26379)
//                .withSentinel("sentinel-host3", 26379)
//                .withPassword("yourpassword")
//                .withDatabase(0)
//                .withTimeout(Duration.ofSeconds(2))
//                .withSsl(false)  // 如果需要SSL
//                .build();
//        rediss-sentinel://yourpassword@sentinel-host1,sentinel-host2,sentinel-host3?sentinelMasterId=mymaster&timeout=2s
//        redis-sentinel://yourpassword@sentinel-host1,sentinel-host2,sentinel-host3?sentinelMasterId=mymaster&timeout=2s

        RedisURI redisUri = RedisURI.Builder.sentinel("sentinel-host", 26379, "mymaster")
                .withSsl(true)
                .build();
        System.out.println(redisUri.toURI());
        System.out.println(1);
    }

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

    private void convertRedisUriToRedisConfig() {
        String scheme = redisConfig.getRedisUrl().split(":")[0];
        String redisMode = RedisModeEnum.getKeyByScheme(scheme);
        RedisURI redisURI = RedisURI.create(redisConfig.getRedisUrl());
        if (redisMode == null) {
            log.error("redisUri scheme is null,{}", redisConfig.getRedisUrl());
            throw new RuntimeException("redis url模式仅支持单机和哨兵");
        }
        redisConfig.setRedisMode(redisMode);
        if (RedisModeEnum.SINGLE_MODE.getKey().equals(redisMode)) {
            redisConfig.setRedisHost(redisURI.getHost());
            redisConfig.setRedisPort(redisURI.getPort());
        } else if (RedisModeEnum.SENTINEL_MODE.getKey().equals(redisMode)) {
            redisConfig.setRedisSentinelMaster(redisURI.getSentinelMasterId());
            redisConfig.setRedisSentinelNodes(redisURI.getSentinels().stream().map(s -> s.getHost() + ":" + s.getPort()).collect(Collectors.joining(",")));
        } else {
            log.error("不支持的redis url scheme,{}", redisConfig.getRedisUrl());
            throw new RuntimeException("不支持的redis url scheme");
        }
        Optional.ofNullable(redisURI.getPassword()).filter(p -> !StringUtils.isEmpty(p)).ifPresent(p -> redisConfig.setRedisPassword(String.valueOf(p)));
        Optional.ofNullable(redisURI.getUsername()).filter(p -> !StringUtils.isEmpty(p)).ifPresent(redisConfig::setRedisUsername);
        Optional.of(redisURI.getDatabase()).filter(p -> p != 0).ifPresent(redisConfig::setRedisDatabase);
        Optional.ofNullable(redisURI.getClientName()).filter(p -> !StringUtils.isEmpty(p)).ifPresent(redisConfig::setRedisClientName);
        Optional.of(redisURI.isSsl()).filter(p -> p).ifPresent(p -> redisConfig.setSpringRedisSsl(p + ""));
        Optional.of(redisURI.getTimeout().getSeconds()).filter(p -> p != 0).ifPresent(redisConfig::setSpringRedisTimeout);
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        if (RedisModeEnum.URL_MODE.getKey().equals(redisConfig.getRedisMode())) {
            convertRedisUriToRedisConfig();
        }
        if (RedisModeEnum.SINGLE_MODE.getKey().equals(redisConfig.getRedisMode())) {
            System.out.println("单机");
            return createStandaloneConnectionFactory();
        } else if (RedisModeEnum.CLUSTER_MODE.getKey().equals(redisConfig.getRedisMode())) {
            System.out.println("集群");
            return null;
//            return createClusterConnectionFactory();
        } else if (RedisModeEnum.SENTINEL_MODE.getKey().equals(redisConfig.getRedisMode())) {
            System.out.println("哨兵");
            return null;
//            return createSentinelConnectionFactory();
        } else {
            log.error("redis mode异常,{}", redisConfig.getRedisMode());
            throw new RuntimeException("不支持的redis mode");
        }
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