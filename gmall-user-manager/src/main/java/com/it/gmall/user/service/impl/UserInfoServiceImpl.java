package com.it.gmall.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.it.bean.UserAddress;
import com.it.bean.UserInfo;
import com.it.gmall.config.RedisUtil;
import com.it.gmall.user.mapper.UserAddressMapper;
import com.it.gmall.user.mapper.UserInfoMapper;
import com.it.service.UserInfoServive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

/*使用dubbo的service注解*/
/*暴露服务*/
@Service
public class UserInfoServiceImpl implements UserInfoServive {

    public String userKey_prefix="user:";
    public String userinfoKey_suffix=":info";
    public int userKey_timeOut=60*60*24;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private UserAddressMapper userAddressMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public List<UserInfo> getUserInfoListAll() {
        return userInfoMapper.selectAll();
    }

    @Override
    public List<UserAddress> getUserAdddressById(UserAddress userAddress) {
        return userAddressMapper.select(userAddress);
    }

    @Override
    public void addUser(UserInfo userInfo) {
        userInfoMapper.insertSelective(userInfo);
    }

    @Override
    public void updateUser(UserInfo userInfo) {
        userInfoMapper.updateByPrimaryKeySelective(userInfo);
    }

    @Override
    public void updateUserByName(String name, UserInfo userInfo) {
        Example example = new Example(UserInfo.class);
        example.createCriteria().andEqualTo("name",name);
        userInfoMapper.updateByExampleSelective(userInfo,example);
    }

    @Override
    public void delUser(UserInfo userInfo) {
        userInfoMapper.delete(userInfo);
    }

    @Override
    public UserInfo login(UserInfo userInfo) {
        String password = DigestUtils.md5DigestAsHex(userInfo.getPasswd().getBytes());
        Jedis jedis = redisUtil.getJedis();
        try {
            userInfo.setPasswd(password);
            /*查询前台传过来的用户名密码是否存在*/
            UserInfo info = userInfoMapper.selectOne(userInfo);
            /*如果存在时*/
            if(info != null){
                /*创建redis客户端*/
                /*创建有过期时间的redis*/
                jedis.setex(userKey_prefix+info.getId()+userinfoKey_suffix,userKey_timeOut, JSON.toJSONString(info));
                return info;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(jedis!=null){
                jedis.close();
            }
        }
        /*DB中不存在直接返回空*/
        return null;
    }

    @Override
    public UserInfo verify(String userId) {
        Jedis jedis = redisUtil.getJedis();
        try {
            String key = userKey_prefix+userId+userinfoKey_suffix;
            String userJson  = jedis.get(key);
            if(userJson!=null){
                /*将Json转化为对象*/
                UserInfo info = JSON.parseObject(userJson, UserInfo.class);
                return info;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis!=null){
                jedis.close();
            }
        }
        return null;
    }
}
