package com.eldercare.controller;

import com.eldercare.common.Result;
import com.eldercare.common.ResultCode;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 应用健康检查：探测应用、MySQL、Redis 状态。
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final DataSource dataSource;
    private final StringRedisTemplate stringRedisTemplate;

    public HealthController(DataSource dataSource, StringRedisTemplate stringRedisTemplate) {
        this.dataSource = dataSource;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @GetMapping
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("app", "UP");

        boolean mysqlUp = checkMysql();
        boolean redisUp = checkRedis();

        data.put("mysql", mysqlUp ? "UP" : "DOWN");
        data.put("redis", redisUp ? "UP" : "DOWN");

        boolean allUp = mysqlUp && redisUp;
        data.put("status", allUp ? "UP" : "DOWN");

        if (!allUp) {
            Result<Map<String, Object>> result = Result.fail(ResultCode.INTERNAL_ERROR, "依赖服务不可用");
            result.setData(data);
            return result;
        }
        return Result.success(data);
    }

    private boolean checkMysql() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean checkRedis() {
        try {
            Boolean ok = stringRedisTemplate.execute((RedisCallback<Boolean>) connection -> {
                String pong = connection.ping();
                return pong != null && "PONG".equalsIgnoreCase(pong.trim());
            });
            return Boolean.TRUE.equals(ok);
        } catch (Exception ex) {
            return false;
        }
    }
}
