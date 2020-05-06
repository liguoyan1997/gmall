package com.it.gmall.config;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisUtil {

    /*创建连接池*/
    private JedisPool jedisPool;

    /*初始化连接池*/
    public  void  initJedisPool(String host,int port,int database){
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        // 总数
        jedisPoolConfig.setMaxTotal(200);
        // 获取连接时等待的最大毫秒
        jedisPoolConfig.setMaxWaitMillis(10*1000);
        // 最少剩余数
        jedisPoolConfig.setMinIdle(10);
        // 开启缓冲池
        jedisPoolConfig.setBlockWhenExhausted(true);
        // 在获取连接时，检查是否有效
        jedisPoolConfig.setTestOnBorrow(true);
        // 创建连接池
        jedisPool = new  JedisPool(jedisPoolConfig,host,port,20*1000);
    }

    /*获取Jedis*/
    public Jedis getJedis(){
        Jedis resource = jedisPool.getResource();
        return resource;
    }
}
