package com.it.service;

import com.it.bean.UserAddress;
import com.it.bean.UserInfo;

import java.util.List;

public interface UserInfoServive {
    List<UserInfo> getUserInfoListAll();

    List<UserAddress> getUserAdddressById(UserAddress userAddress);

    void addUser(UserInfo userInfo);

    void updateUser(UserInfo userInfo);

    void updateUserByName(String name, UserInfo userInfo);

    void delUser(UserInfo userInfo);

    /**
     * 登录方法
     * @param userInfo
     * @return
     */
    UserInfo login(UserInfo userInfo);

    /***
     * 用户认证
     * @param userId
     * @return
     */
    UserInfo verify(String userId);
}
