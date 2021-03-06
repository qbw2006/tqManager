package com.candy.service.patrol;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.candy.dao.IRedisDao;
import com.candy.dao.RedisResult;
import com.candy.dao.RedisServerEntity;
import com.candy.dao.redis.TqRedisClient;
import com.candy.utils.TqLog;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import redis.clients.jedis.JedisPoolConfig;

@Service
public class RedisPatroller {
    @Autowired
    private IRedisDao rDao;

    private Map<String, TqRedisClient> connections = Maps.newConcurrentMap();

    /**
     * 连接需要事先创建，使用时效率就会很高。
     */
    @PostConstruct
    public void init() {
        createClient();
        TqLog.getDailyLog().info("RedisPatroller start [successfully]");
    }

    private void createClient() {
        for (RedisServerEntity rs : rDao.listAllServer()) {
            TqRedisClient tqRedis = null;
            try {
                if (isNew(rs.getId())) {
                    tqRedis = createTQRedisClient(rs);
                    TqLog.getDailyLog().info("create redis client, info = {}", rs.getId());
                    connections.put(rs.getId(), tqRedis);
                }
            } catch (Exception e) {
                TqLog.getErrorLog().warn("create redis client. info = {}", rs);
            }
        }

    }

    private boolean isNew(String id) {
        return (!connections.containsKey(id) || connections.get(id) == null);
    }

    /**
     * 定期从数据库中取出所有redis服务器配置，针对新加的服务器，要创建其连接。
     */
    @Scheduled(cron = "${redis.schedule}")
    public void createRedisClient() {
        createClient();
    }

    @Scheduled(cron = "${redis.schedule}")
    public void patrol() {
        List<RedisResult> result = checkRedis();

        TqLog.getDailyLog().debug("check result = {}", JSON.toJSONString(result));

        // 保存检查结果，新结果覆盖旧结果
        result.forEach(v -> rDao.addResult(v));

        // 保存检查结果，新结果不会覆盖旧结果
        result.forEach(v -> rDao.addResultForHistory(v));
    }

    /**
     * 因为JedisConnection断线不会重连,所以要使用带池的RedisConnectionFactory。
     * 这样redis宕机后RedisConnectionFactory便会重连
     * 
     * @param rs
     * @return
     */
    private TqRedisClient createTQRedisClient(RedisServerEntity rs) {
        JedisConnectionFactory jcf = new JedisConnectionFactory();
        jcf.setHostName(rs.getRedisHost());
        jcf.setPort(rs.getRedisPort());
        jcf.setClientName(rs.getId());

        if (!StringUtils.isEmpty(rs.getRedisPassword())) {
            jcf.setPassword(rs.getRedisPassword());
        }

        JedisPoolConfig jpc = new JedisPoolConfig();
        // 设置最大等待链接的时间
        // bug场景：redis在线--》掉线--》在线，如果不设置MaxWaitMillis会出现卡死。
        jpc.setMaxWaitMillis(3000);

        jcf.setPoolConfig(jpc);
        jcf.afterPropertiesSet();

        return new TqRedisClient(new StringRedisTemplate(jcf));
    }

    private List<RedisResult> checkRedis() {
        List<RedisResult> res = Lists.newArrayList();
        Date date = new Date();

        for (Entry<String, TqRedisClient> en : connections.entrySet()) {
            RedisServerEntity rse = rDao.findServerById(en.getKey());
            TqRedisClient tqRedis = en.getValue();
            RedisResult rr = JSON.parseObject(JSON.toJSONString(rse), RedisResult.class);
            // 赋值
            rr.setId(rse.getId());
            rr.setDate(date);

            try {
                Map<Object, Object> p = new TreeMap<>(tqRedis.info());
                rr.setInfo(p);
                rr.setAlive(true);

            } catch (Exception e) {
                TqLog.getErrorLog().warn("redis client is disconnect.  id = {} ", rse.getId());
            }

            res.add(rr);
        }
        return res;
    }

    public void deleteClient(String id) {
        connections.remove(id);
    }
}
