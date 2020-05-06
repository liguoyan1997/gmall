package com.it.gmall.user.controller;

import com.it.bean.UserInfo;
import com.it.service.UserInfoServive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class UserInfoController {

    @Autowired
    private UserInfoServive userInfoService;

    @RequestMapping("List")
    @ResponseBody
    public List<UserInfo> list(){
        return userInfoService.getUserInfoListAll();
    }

    @RequestMapping("add")
    public String add(UserInfo userInfo){
        userInfo = new UserInfo("1","2");
        userInfoService.addUser(userInfo);
        return "/List";
    }

    @RequestMapping("editId")
    public String editId(UserInfo userInfo){
        userInfo.setId("5");
        userInfo.setName("张三");
        userInfoService.updateUser(userInfo);
        return "/List";
    }

    @RequestMapping("editName")
    public String editName(UserInfo userInfo){
        userInfo.setName("张三");
        userInfo.setLoginName("zhansan");
        userInfoService.updateUserByName(userInfo.getName(),userInfo);
        return "/List";
    }

    @RequestMapping("delete")
    @ResponseBody
    public String delete(UserInfo userInfo){
        userInfo.setId("5");
        userInfoService.delUser(userInfo);
        return "OK";
    }
}

