package com.netease.lib.redistemplatetool.util;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Order
public class RedisToolEnvironmentMapperPostProcessor implements EnvironmentPostProcessor {
    private static final Map<String, String> REDIS_CONFIG_MAPPER = new HashMap<>();

    //File文件类型  数据权限
    static {
        REDIS_CONFIG_MAPPER.put("extensions.redis_template_tool.custom.redisHost", "spring.redis.host");
        REDIS_CONFIG_MAPPER.put("extensions.redis_template_tool.custom.redisPort", "spring.redis.port");
        REDIS_CONFIG_MAPPER.put("extensions.redis_template_tool.custom.redisDatabase", "spring.redis.database");
        REDIS_CONFIG_MAPPER.put("extensions.redis_template_tool.custom.redisPassword", "spring.redis.password");
        REDIS_CONFIG_MAPPER.put("extensions.redis_template_tool.custom.redisSentinelMaster", "spring.redis.sentinel.master");
        REDIS_CONFIG_MAPPER.put("extensions.redis_template_tool.custom.redisSentinelNodes", "spring.redis.sentinel.nodes");
        REDIS_CONFIG_MAPPER.put("extensions.redis_template_tool.custom.redisClusterNodes", "spring.redis.cluster.nodes");
        REDIS_CONFIG_MAPPER.put("extensions.redis_template_tool.custom.redisSlaveHost", "spring.redis.slave.host");
        REDIS_CONFIG_MAPPER.put("extensions.redis_template_tool.custom.redisSlavePort", "spring.redis.slave.port");
        REDIS_CONFIG_MAPPER.put("extensions.redis_template_tool.custom.redisSentinelPassword", "spring.redis.sentinel.password");
        REDIS_CONFIG_MAPPER.put("extensions.redis_template_tool.custom.springRedisTimeout", "spring.redis.timeout");
        REDIS_CONFIG_MAPPER.put("extensions.redis_template_tool.custom.springRedisSsl", "spring.redis.ssl");
        REDIS_CONFIG_MAPPER.put("extensions.redis_template_tool.custom.springRedisLettucePoolMaxActive", "spring.redis.lettuce.pool.max-active");
        REDIS_CONFIG_MAPPER.put("extensions.redis_template_tool.custom.springRedisLettucePoolMaxIdle", "spring.redis.lettuce.pool.max-idle");
        REDIS_CONFIG_MAPPER.put("extensions.redis_template_tool.custom.springRedisLettucePoolMinIdle", "spring.redis.lettuce.pool.min-idle");
        REDIS_CONFIG_MAPPER.put("extensions.redis_template_tool.custom.springRedisLettuceCommandTimeout", "spring.redis.lettuce.command-timeout");
        REDIS_CONFIG_MAPPER.put("extensions.redis_template_tool.custom.springRedisLettuceShutdownTimeout", "spring.redis.lettuce.shutdown-timeout");
        REDIS_CONFIG_MAPPER.put("extensions.redis_template_tool.custom.springRedisLettuceClientName", "spring.redis.lettuce.client-name");
    }

    /**
     * Post-process the given {@code environment}.
     *
     * @param environment the environment to post-process
     * @param application the application to which the environment belongs
     */
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> mapperProperties = new HashMap<>();
        REDIS_CONFIG_MAPPER.forEach((key, value) -> {
            if (environment.containsProperty(value)) {
                return;
            }
            if (environment.containsProperty(key)) {
                String property = environment.getProperty(key);
                if (!StringUtils.isEmpty(property)) {
                    mapperProperties.put(value, property);
                }
            }
        });

        if (mapperProperties.isEmpty()) {
            return;
        }
        environment.getPropertySources().addLast(new MapPropertySource("REDIS_CONFIG_MAPPER", mapperProperties));
    }

}