package com.it.service;

import com.it.bean.OrderInfo;
import com.it.bean.enums.ProcessStatus;

import java.util.List;
import java.util.Map;

public interface OrderService {
    /**
     * 保存订单
     * @param orderInfo
     * @return
     */
    String saveOrder(OrderInfo orderInfo);

    /**
     * 生成流水号
     * @param userId
     * @return
     */
    String getTradeNo(String userId);

    /**
     * 比较流水号
     * @param userId
     * @param tradeCodeNo
     * @return
     */
    boolean checkTradeCode(String userId,String tradeCodeNo);

    /*
    * 删除流水号
    * */
    void  delTradeCode(String userId);

    /**
     * 验证库存
     * @param skuId
     * @param skuNum
     * @return
     */
    boolean checkStock(String skuId,Integer skuNum);

    /**
     * 根据orderId查询订单对象
     * @param orderId
     */
    OrderInfo getOrderInfo(String orderId);

    /**
     * 更新订单状态
     * @param orderId
     * @param paid
     */
    void updateOrderStatus(String orderId, ProcessStatus paid);

    /**
     * 通知减少库存
     * @param orderId
     */
    void sendOrderStatus(String orderId);

    /**
     * 查询过期的订单记录
     * @return
     */
    List<OrderInfo> getExpiredOrderList();

    /**
     * 关闭过期的订单记录
     * @param orderInfo
     */
    void execExpiredOrder(OrderInfo orderInfo);

    /*拆单*/
    List<OrderInfo> splitOrder(String orderId, String wareSkuMap);

    /**
     * 将orderInfo转换为map
     * @param orderInfo
     * @return
     */
    Map initWareOrder(OrderInfo orderInfo);
}
