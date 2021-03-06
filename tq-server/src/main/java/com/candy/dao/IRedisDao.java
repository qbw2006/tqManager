package com.candy.dao;

import java.util.List;

import com.candy.service.OperationTask;

public interface IRedisDao {

    /**
     * 查询所有redis服务器
     * 
     * @return
     */
    List<RedisServerEntity> listAllServer();

    /**
     * 根据id查询redis-server-entity
     * 
     * @param id
     * @return
     */
    RedisServerEntity findServerById(String id);

    /**
     * 修改RedisServerEntity
     * 
     * @param rse
     */
    void updateServer(RedisServerEntity rse);

    /**
     * 删除redis entity
     * 
     * @param id
     */
    void deleteServer(String id);

    /**
     * 添加redis服务器
     * 
     * @param rse
     * @return
     */
    String addServer(RedisServerEntity rse);

    /**
     * 添加检查结果
     * 
     * @param rr
     */
    void addResult(RedisResult rr);

    /**
     * 添加检查结果到历史库中，用于图表展示
     * 
     * @param rr
     */
    void addResultForHistory(RedisResult rr);

    List<RedisResult> listHistoryResult(String id);

    /**
     * 查询检查结果
     * 
     * @return
     */
    List<RedisResult> listResult();

    /**
     *
     * 删除检查结果
     * 
     * @param id
     */
    void deleteResult(String id);

    /**
     * 添加启动或者停止任务
     * 
     * @param ot
     */
    void addTask(OperationTask ot);

}
