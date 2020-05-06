package com.it.service;

import com.it.bean.PaymentInfo;

public interface PaymentService {

    /**
     * 保存交易记录
     * @param paymentInfo
     */
    void savePaymentInfo(PaymentInfo paymentInfo);

    /**
     * 通过out_trade_no查询支付记录
     * @param paymentInfo
     * @return
     */
    PaymentInfo getPaymentInfo(PaymentInfo paymentInfo);

    /**
     * 成功时更新交易状态
     * @param out_trade_no
     * @param paymentInfoUpd
     */
    void updatePaymentInfo(String out_trade_no, PaymentInfo paymentInfoUpd);

    /**
     * 退款
     * @param orderId
     * @return
     */
    boolean refund(String orderId);

    /**
     * 异步回调 向订单发送结果通知
     * @param paymentInfo
     * @param result
     */
    public void sendPaymentResult(PaymentInfo paymentInfo,String result);


    /**
     * 根据out_trade_no查询交易记录
     * @param paymentInfoQuery
     * @return
     */
    boolean checkPayment(PaymentInfo paymentInfoQuery);


    /**
     *
     * @param outTradeNo 交易单号
     * @param delaySec 每隔几秒查询一次
     * @param checkCount 查询的次数
     */
    public void sendDelayPaymentResult(String outTradeNo,int delaySec ,int checkCount);

    /**
     * 关闭过期的付款信息
     * @param id
     */
    void closePayment(String id);
}
