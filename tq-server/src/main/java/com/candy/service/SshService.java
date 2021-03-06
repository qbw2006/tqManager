package com.candy.service;

import java.util.Map;

import javax.websocket.Session;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.candy.dao.IRedisDao;
import com.candy.dao.RedisServerEntity;
import com.candy.ssh.SshClient;
import com.google.common.collect.Maps;

@Service
public class SshService {

	@Autowired
	private IRedisDao rDao; 
	private Map<String, SshClient> clients = Maps.newConcurrentMap();
	
	public void createClient(String id, Session session) 
	{
		SshClient client = null;
		if (clients.containsKey(id))
		{
			client = clients.get(id);
		}
		else
		{
			RedisServerEntity rse = rDao.findServerById(id);
	        //做多个用户处理的时候，可以在这个地方来 ,判断用户id和
	        //初始化客户端
	        client = new SshClient(rse, session);

	        //连接服务器
	        client.connect();
	        clients.put(id, client);
		}
	}
	
	public void closeClient(String id) 
	{
		SshClient client = clients.remove(id);
		
		client.disconnect();
	}
	
	public void sendMessage(String id, String message)
	{
		SshClient client = clients.get(id);
		client.write(message);
	}
}
