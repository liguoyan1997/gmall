package com.it.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.it.bean.CartInfo;
import com.it.bean.SkuInfo;
import com.it.bean.UserInfo;
import com.it.gmall.config.LoginRequire;
import com.it.service.CartService;
import com.it.service.ManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@Controller
public class CartController {

    @Reference
    private CartService cartService;

    @Reference
    private ManagerService managerService;

    @Autowired
    private CartCookieHandler cartCookieHandler;

    @RequestMapping("addToCart")
    @LoginRequire(autoRedirect = false)
    public String addToCart(HttpServletRequest request, HttpServletResponse response){
        String skuNum = request.getParameter("skuNum");
        String skuId = request.getParameter("skuId");

        String userId = (String) request.getAttribute("userId");
        if(userId!=null){
            /*用户登录的情况*/
            cartService.addToCart(skuId,userId,Integer.parseInt(skuNum));
        }else{
            /*用户未登录的情况*/
            cartCookieHandler.addToCart(request,response,skuId,userId,Integer.parseInt(skuNum));
        }
        SkuInfo skuInfo = managerService.getSkuInfo(skuId);
        request.setAttribute("skuInfo",skuInfo);
        request.setAttribute("skuNum",skuNum);
        return "success";
    }

    @RequestMapping("cartList")
    @LoginRequire(autoRedirect = false)
    public String cartList(HttpServletRequest request,HttpServletResponse response){
        /*获取用户Id*/
        String userId = (String) request.getAttribute("userId");
        List<CartInfo> cartList = null;
        if(userId!=null){
            /*用户登录的情况下 合并购物车*/
            List<CartInfo> cartListCK = cartCookieHandler.getCartList(request);
            if(cartListCK!=null){
                /*用户登录的情况cookie中有数据*/
                cartList = cartService.mergeToCartList(cartListCK,userId);
                // 删除cookie中的购物车
                cartCookieHandler.deleteCartCookie(request,response);
            }else {
                /*用户登录的情况cookie中没有数据*/
                cartList = cartService.getCartList(userId);
            }
        }else{
            /*用户未登录的情况*/
            cartList = cartCookieHandler.getCartList(request);
        }
        request.setAttribute("cartInfoList",cartList);
        return "cartList";
    }

    /**
     * 显示选中时候的购物车
     * @param request
     * @param response
     */
    @RequestMapping("checkCart")
    @ResponseBody
    @LoginRequire(autoRedirect = false)
    public void checkCart(HttpServletRequest request,HttpServletResponse response){
        String isChecked = request.getParameter("isChecked");
        String skuId = request.getParameter("skuId");
        Object userId = request.getAttribute("userId");
        if(userId!=null){
            /*登录的情况下*/
            cartService.checkCart(skuId,isChecked,userId);
        }else{
            /*未登录的情况下*/
            cartCookieHandler.checkCart(request,response,skuId,isChecked);
        }
    }

    /**
     * 去结算
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("toTrade")
    @LoginRequire(autoRedirect = true)
    public String toTrade(HttpServletRequest request,HttpServletResponse response){
        String userId = (String) request.getAttribute("userId");
        /*获取未登录状态下购物车的数据*/
        List<CartInfo> cookieHandlerCartList = cartCookieHandler.getCartList(request);
        /*当未登录状态下cookie中有数据时*/
        if (cookieHandlerCartList!=null && cookieHandlerCartList.size()>0){
            /*当cookie中有数据时合并购物车*/
            cartService.mergeToCartList(cookieHandlerCartList, userId);
            /*删除cookie1中购物车的数据*/
            cartCookieHandler.deleteCartCookie(request,response);
        }
        return "redirect://order.gmall.com/trade";
    }
}
