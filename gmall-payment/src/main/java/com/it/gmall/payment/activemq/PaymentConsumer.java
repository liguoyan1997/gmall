package com.it.gmall.payment.activemq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.it.bean.PaymentInfo;
import com.it.service.PaymentService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class PaymentConsumer {

    @Reference
    private PaymentService paymentService;

    @JmsListener(destination = "PAYMENT_RESULT_CHECK_QUEUE",containerFactory = "jmsQueueListener")
    public void consumeSkuDeduct(MapMessage mapMessage) throws JMSException {

        // 获取消息队列中的数据
        String outTradeNo = mapMessage.getString("outTradeNo");
        int delaySec = mapMessage.getInt("delaySec");
        int checkCount = mapMessage.getInt("checkCount");

        // 创建一个paymentInfo
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(outTradeNo);
        /*获取orderId*/
        PaymentInfo paymentInfoQuery = paymentService.getPaymentInfo(paymentInfo);
        // 调用 paymentService.checkPayment(paymentInfoQuery);
        /*判断是否支付成功*/
        boolean flag = paymentService.checkPayment(paymentInfoQuery);
        System.out.println("检查结果："+flag);
        if (!flag && checkCount!=0){
            // 还需要继续检查
            System.out.println("检查的次数："+checkCount);
            paymentService.sendDelayPaymentResult(outTradeNo,delaySec,checkCount-1);
        }
    }
}
