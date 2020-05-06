package com.it.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.it.bean.*;
import com.it.gmall.config.LoginRequire;
import com.it.service.CartService;
import com.it.service.ManagerService;
import com.it.service.OrderService;
import com.it.service.UserInfoServive;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class OrderController {

    /*调用提供者的service*/
    /*引用服务*/
    @Reference
    private UserInfoServive userInfoServive;

    @Reference
    private CartService cartService;

    @Reference
    private OrderService orderService;

    @Reference
    private ManagerService managerService;

    /*进入订单页面*/
    @RequestMapping("/trade")
    @LoginRequire
    public String order(HttpServletRequest request){
        /*收货地址显示*/
        String userId = (String) request.getAttribute("userId");
        UserAddress userAddress =  new UserAddress();
        userAddress.setUserId(userId);
        List<UserAddress> userAddressList = userInfoServive.getUserAdddressById(userAddress);
        request.setAttribute("userAddressList",userAddressList);
        /*送货清单*/
        /*数据来源  勾选的购物车*/
        // 得到选中的购物车列表
        List<CartInfo> cartCheckedList = cartService.getCartCheckedList(userId);
        // 声明一个集合  存储订单明细
        List<OrderDetail> orderDetailList=new ArrayList<>(cartCheckedList.size());
        for(CartInfo cartInfo:cartCheckedList){
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            orderDetail.setOrderPrice(cartInfo.getCartPrice());
            orderDetailList.add(orderDetail);
        }
        request.setAttribute("orderDetailList",orderDetailList);
        /*计算总金额*/
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(orderDetailList);
        /*调用计算总金额*/
        orderInfo.sumTotalAmount();
        request.setAttribute("totalAmount",orderInfo.getTotalAmount());
        /*生成流水号*/
        String tradeNo = orderService.getTradeNo(userId);
        request.setAttribute("tradeNo",tradeNo);
        return "trade";
    }

    @RequestMapping("/ListById")
    @ResponseBody
    public List<UserAddress> getUserAddressById(UserAddress userAddress){
        return userInfoServive.getUserAdddressById(userAddress);
    }

    @RequestMapping("submitOrder")
    @LoginRequire
    public String submitOrder(OrderInfo orderInfo,HttpServletRequest request){
        // 检查tradeCode
        String userId = (String) request.getAttribute("userId");
        orderInfo.setUserId(userId);
        /*查看是否是重复提交*/
        String tradeNo = request.getParameter("tradeNo");
        /*调用比较方法*/
        boolean flag = orderService.checkTradeCode(userId, tradeNo);
        /*是重复提交*/
        if(!flag){
            request.setAttribute("errMsg","订单已提交,不能重复提交");
            return "tradeFail";
        }
        /*验证库存*/
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for(OrderDetail orderDetail: orderDetailList){
            boolean b = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
            if (!b){
                request.setAttribute("errMsg",orderDetail.getSkuName()+"商品库存不足!");
                return "tradeFail";
            }
            SkuInfo skuInfo = managerService.getSkuInfo(orderDetail.getSkuId());
            int i = skuInfo.getPrice().compareTo(orderDetail.getOrderPrice());
            if(i!=0){
                request.setAttribute("errMsg",orderDetail.getSkuName()+"商品实时价格不匹配!");
                cartService.loadCartCache(userId);
                return "tradeFail";
            }

        }
        /*保存订单*/
        String orderId = orderService.saveOrder(orderInfo);
        /*删除流水号*/
        orderService.delTradeCode(userId);
        /*跳转到支付页面*/
        return "redirect://payment.gmall.com/index?orderId="+orderId;
    }

    @RequestMapping("orderSplit")
    @ResponseBody
    public String orderSplit(HttpServletRequest request){
        /*获取订单ID*/
        String orderId = request.getParameter("orderId");
        String wareSkuMap = request.getParameter("wareSkuMap");
        // 返回子订单集合
        List<OrderInfo> subOrderInfoList = orderService.splitOrder(orderId,wareSkuMap);
        /*创建一个集合来存储map*/
        List<Map> wareMapList=new ArrayList<>();
        for (OrderInfo orderInfo : subOrderInfoList) {
            Map map = orderService.initWareOrder(orderInfo);
            wareMapList.add(map);
        }
        return JSON.toJSONString(wareMapList);
    }
}
