package com.it.gmall.order.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.it.bean.enums.ProcessStatus;
import com.it.service.OrderService;
import org.springframework.core.annotation.Order;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class OrderConsumer {

    @Reference
    private OrderService orderService;

    @JmsListener(destination = "PAYMENT_RESULT_QUEUE",containerFactory = "jmsQueueListener")
    public  void  consumerPaymentResult(MapMessage mapMessage) throws JMSException {
        /*获取订单id*/
        String orderId = mapMessage.getString("orderId");
        String result = mapMessage.getString("result");
        /*判断支付是否成功*/
        if("success".equals(result)){
            /*更改订单状态*/
            orderService.updateOrderStatus(orderId, ProcessStatus.PAID);
            // 通知减库存
            orderService.sendOrderStatus(orderId);
            /*更改订单状态*/
            orderService.updateOrderStatus(orderId, ProcessStatus.NOTIFIED_WARE);
        }
    }


    @JmsListener(destination = "SKU_DEDUCT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumeSkuDeduct(MapMessage mapMessage) throws JMSException {
        String orderId = mapMessage.getString("orderId");
        String  status = mapMessage.getString("status");
        if("DEDUCTED".equals(status)){
            orderService.updateOrderStatus(orderId, ProcessStatus.DELEVERED);
        }
    }
}
