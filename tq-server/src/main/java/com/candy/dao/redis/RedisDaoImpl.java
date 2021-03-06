package com.candy.dao.redis;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.alibaba.fastjson.JSON;
import com.candy.dao.IRedisDao;
import com.candy.dao.IOperationTask;
import com.candy.dao.RedisResult;
import com.candy.dao.RedisServerEntity;
import com.candy.service.OperationTask;
import com.candy.utils.TqLog;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

@Repository
public class RedisDaoImpl implements IRedisDao, IOperationTask {

    private static final String SERVER_KEY = "redis-entity";
    private static final String RESULT_KEY = "redis-result";

    @Value("${redis.result.size}")
    private long size;

    @Autowired
    private TqRedisClient redisClient;

    @Override
    public List<RedisServerEntity> listAllServer() {
        List<RedisServerEntity> res = Lists.newArrayList();

        Map<Object, Object> resFromDb = redisClient.entries(SERVER_KEY);

        resFromDb.forEach((k, v) -> {
            res.add(JSON.parseObject((String) v, RedisServerEntity.class));
        });

        return res;
    }

    @Override
    public RedisServerEntity findServerById(String id) {
        Object res = redisClient.get(SERVER_KEY, id);

        return JSON.parseObject((String) res, RedisServerEntity.class);
    }

    @Override
    public void updateServer(RedisServerEntity rse) {
        addServer(rse);
    }

    @Override
    public void deleteServer(String id) {
        redisClient.delete(SERVER_KEY, id);
    }

    @Override
    public String addServer(RedisServerEntity rse) {
        redisClient.put(SERVER_KEY, rse.getId(), JSON.toJSONString(rse));
        return rse.getId();
    }

    @Override
    public void handleTask(String message) {
        OperationTask rt = JSON.parseObject(message, OperationTask.class);

        TqLog.getDailyLog().info("receive a task from redis = {} ", message);
        RedisServerEntity server = findServerById(rt.getId());
        JSch js = new JSch();
        try {

            // 创建session
            Session s = js.getSession(server.getServerUsername(), server.getRedisHost());
            s.setPassword(server.getServerPassword());
            s.setConfig("StrictHostKeyChecking", "no");
            s.connect();

            TqLog.getDailyLog().info("create a session. server info = {}", server.getServerInfo());

            // 生成命令
            String command = rt.getOpertion() == 0 ? server.getStopCommand() : server.getStartCommand();

            TqLog.getDailyLog().info("command = {}", command);

            // 执行
            String result = execCommandByJSch(s, command, Charsets.UTF_8);

            TqLog.getDailyLog().info("exec result = {}", result);

        } catch (JSchException e) {

            TqLog.getDailyLog().error("jsch [fail], server info = {}", server.getServerInfo(), e);

        } catch (IOException e) {

            TqLog.getDailyLog().error("io is error", e);
        }
    }

    public static String execCommandByJSch(Session session, String command, Charset encode)
            throws IOException, JSchException {
        ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
        InputStream in = channelExec.getInputStream();
        channelExec.setCommand(command);
        channelExec.setErrStream(System.err);
        channelExec.connect();
        String result = IOUtils.toString(in, encode);
        channelExec.disconnect();

        return result;
    }

    @Override
    public void addResult(RedisResult rr) {
        redisClient.put(RESULT_KEY, rr.getId(), JSON.toJSONString(rr));
    }

    @Override
    public void addTask(OperationTask ot) {
        redisClient.convertAndSend(JSON.toJSONString(ot));
    }

    @Override
    public List<RedisResult> listResult() {
        List<RedisResult> res = Lists.newArrayList();

        // 为了让结果有序
        Map<Object, Object> resFromDb = new TreeMap<>(redisClient.entries(RESULT_KEY));

        resFromDb.forEach((k, v) -> {
            res.add(JSON.parseObject((String) v, RedisResult.class));
        });
        return res;
    }

    @Override
    public void deleteResult(String id) {
        redisClient.delete(RESULT_KEY, id);
    }

    @Override
    public void addResultForHistory(RedisResult rr) {
        String key = rr.getId();
        
        //控制list的长度
        if(redisClient.size(key) >= size)
        {
            redisClient.leftPop(key);
        }
        
        redisClient.rightPush(rr.getId(), JSON.toJSONString(rr));
    }

    @Override
    public List<RedisResult> listHistoryResult(String id) {
        return redisClient.listAll(id, RedisResult.class);
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

}
