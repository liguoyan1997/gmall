package com.it.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.it.bean.UserInfo;
import com.it.gmall.passport.config.JwtUtil;
import com.it.service.ManagerService;
import com.it.service.UserInfoServive;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassPostController {

    @Value("${token.key}")
    private String key;

    @Reference
    private UserInfoServive userInfoServive;

    @RequestMapping("index")
    public String index(HttpServletRequest request){
        String originUrl = request.getParameter("originUrl");
        request.setAttribute("originUrl",originUrl);
        return "index";
    }

    @RequestMapping("login")
    @ResponseBody
    public String login(UserInfo userInfo,HttpServletRequest request){
        // 取得ip地址
        String remoteAddr  = request.getHeader("X-forwarded-for");
        /*验证用户名密码是否存在*/
        UserInfo info = userInfoServive.login(userInfo);
        if (info!=null){
            // 生成token
            Map map = new HashMap();
            map.put("userId", info.getId());
            map.put("nickName", info.getNickName());
            /*加密*/
            /**
             * key中放入
             */
            String token = JwtUtil.encode(key, map, remoteAddr);
            return token;
        }else {
            return "fail";
        }
    }

    /*解密*/
    @RequestMapping("verify")
    @ResponseBody
    public String verify(HttpServletRequest request){
        String token = request.getParameter("token");
        /*获取服务器IP*/
        String salt = request.getHeader("X-forwarded-for");;
        /*解密*/
        Map<String, Object> decode = JwtUtil.decode(token, key, salt);
        if(decode!=null&&decode.size()>0){
            String userId = (String) decode.get("userId");
            UserInfo userInfo = userInfoServive.verify(userId);
            if(userInfo!=null){
                return "success";
            }
        }
        return "fail";
    }
}
