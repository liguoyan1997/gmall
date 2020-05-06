package com.it.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.it.bean.CartInfo;
import com.it.bean.SkuInfo;
import com.it.gmall.config.CookieUtil;
import com.it.service.ManagerService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@Component
public class CartCookieHandler {

    // 定义购物车名称
    private String cookieCartName = "CART";
    // 设置cookie 过期时间
    private int COOKIE_CART_MAXAGE=7*24*3600;

    @Reference
    private ManagerService managerService;

    public void addToCart(HttpServletRequest request, HttpServletResponse response, String skuId, String userId, Integer skuNum) {
        /**
         * 查看购物车中是否有该商品
         * true数量相加
         * false直接添加
         */
        /*从cookie获取购物车数据*/
        String cookieValue = CookieUtil.getCookieValue(request, cookieCartName, true);
        /*解析cookie 不能为空*/
        /*如果cookie中没有商品 用boolean判断*/
        List<CartInfo> cartInfoList = new ArrayList<>();
        boolean ifExist=false;
        if(StringUtils.isNotEmpty(cookieValue)){
            /*改字符串包含多个CartInfo实体类*/
            cartInfoList = JSON.parseArray(cookieValue, CartInfo.class);
            /*遍历购物车中商品集合*/
            for (CartInfo cartInfo:cartInfoList){
                /*数量相加*/
                cartInfo.setSkuNum(cartInfo.getSkuNum()+skuNum);
                /*将购物车中加个初始化为实时价格*/
                cartInfo.setCartPrice(cartInfo.getSkuPrice());
                /*将变量更改为true*/
                ifExist = true;
            }
        }
        /*如果在购物车中没有该商品 为true*/
        if(!ifExist){
            SkuInfo skuInfo = managerService.getSkuInfo(skuId);
            CartInfo cartInfo = new CartInfo();
            cartInfo.setSkuId(skuId);
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setUserId(userId);
            cartInfo.setSkuNum(skuNum);
            cartInfoList.add(cartInfo);
        }
        /*将最终的集合放入cookie*/
        CookieUtil.setCookie(request,response,cookieCartName, JSON.toJSONString(cartInfoList),COOKIE_CART_MAXAGE,true);
    }

    public List<CartInfo> getCartList(HttpServletRequest request) {
        String cookieValue = CookieUtil.getCookieValue(request, cookieCartName, true);
        if(StringUtils.isNotEmpty(cookieValue)){
            return JSON.parseArray(cookieValue,CartInfo.class);
        }
        return null;
    }

    /**
     * 删除未登录状态下的购物车
     * @param request
     * @param response
     */
    public void deleteCartCookie(HttpServletRequest request, HttpServletResponse response) {
        CookieUtil.deleteCookie(request,response,cookieCartName);
    }

    /*未登录*/
    public void checkCart(HttpServletRequest request, HttpServletResponse response, String skuId, String isChecked) {
        List<CartInfo> cartList = getCartList(request);
        if (cartList!=null && cartList.size()>0){
            for (CartInfo cartInfo : cartList){
                if(cartInfo.getSkuId().equals(skuId)){
                    cartInfo.setIsChecked(isChecked);
                }
            }
        }
        /*购物车集合写回cookie*/
        CookieUtil.setCookie(request,response,cookieCartName,JSON.toJSONString(cartList),COOKIE_CART_MAXAGE,true);
    }
}
