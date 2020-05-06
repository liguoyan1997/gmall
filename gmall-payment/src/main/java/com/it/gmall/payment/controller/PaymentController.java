package com.it.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.it.bean.OrderInfo;
import com.it.bean.PaymentInfo;
import com.it.bean.enums.PaymentStatus;
import com.it.gmall.payment.config.AlipayConfig;
import com.it.service.OrderService;
import com.it.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PaymentController {

    @Reference
    private OrderService orderService;

    /*@Autowired  由于Service用的是阿里的dubbo的  所以不能用Service*/
    @Reference
    private PaymentService paymentService;

    @Autowired
    private AlipayClient alipayClient;

    @RequestMapping("index")
    public String index(String orderId, HttpServletRequest request){
        request.setAttribute("orderId",orderId);
        /*获取订单信息*/
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        /*总金额*/
        BigDecimal totalAmount = orderInfo.getTotalAmount();
        request.setAttribute("totalAmount",totalAmount);
        request.setAttribute("nickName",orderInfo.getConsignee());
        return "index";
    }

    @RequestMapping(value = "/alipay/submit",method = RequestMethod.POST)
    @ResponseBody
    public String submitPayment(HttpServletRequest request, HttpServletResponse response) {
        /**
         * 保存支付记录下 将数据放入数据库
         * 去重复，对账！ 幂等性=保证每一笔交易只能交易一次 {第三方交易编号outTradeNo}！
         */
        // 获取订单Id
        String orderId = request.getParameter("orderId");
        // 取得订单信息
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        // 保存支付信息
        PaymentInfo paymentInfo = new PaymentInfo();
        /*设置订单编号*/
        paymentInfo.setOrderId(orderId);
        /*设置支付宝交易凭证号*/
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        /*设置总金额*/
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        /*设置转账备注*/
        paymentInfo.setSubject("给李国琰转账");
        /*设置订单状态*/
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);
        /*设置订单生成日期*/
        paymentInfo.setCreateTime(new Date());
        /*保存到数据库*/
        paymentService.savePaymentInfo(paymentInfo);

        /*生成二维码*/
        /*参数做成配置文件，进行软编码*/
//        AlipayClient alipayClient =  new  DefaultAlipayClient( "https://openapi.alipay.com/gateway.do" , APP_ID, APP_PRIVATE_KEY, FORMAT, CHARSET, ALIPAY_PUBLIC_KEY, SIGN_TYPE);  //获得初始化的AlipayClient
        AlipayTradePagePayRequest alipayRequest =  new  AlipayTradePagePayRequest(); //创建API对应的request
        /*设置同步回调*/
//        alipayRequest.setReturnUrl( "http://domain.com/CallBack/return_url.jsp" );
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        /*设置异步回调*/
//        alipayRequest.setNotifyUrl( "http://domain.com/CallBack/notify_url.jsp" ); //在公共参数中设置回跳和通知地址
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);//在公共参数中设置回跳和通知地址
        /*二维码参数*/
        // 声明一个Map
        Map<String,Object> map=new HashMap<>();
        map.put("out_trade_no",paymentInfo.getOutTradeNo());
        map.put("product_code","FAST_INSTANT_TRADE_PAY");
        map.put("subject",paymentInfo.getSubject());
        map.put("total_amount",paymentInfo.getTotalAmount());
        /*把封装好的参数  传递给支付宝*/
        alipayRequest.setBizContent(JSON.toJSONString(map));
        /*alipayRequest.setBizContent( "{"  +
                "    \"out_trade_no\":\"20150320010101001\","  +
                "    \"product_code\":\"FAST_INSTANT_TRADE_PAY\","  +
                "    \"total_amount\":88.88,"  +
                "    \"subject\":\"Iphone6 16G\","  +
                "    \"body\":\"Iphone6 16G\","  +
                "    \"passback_params\":\"merchantBizType%3d3C%26merchantBizNo%3d2016010101111\","  +
                "    \"extend_params\":{"  +
                "    \"sys_service_provider_id\":\"2088511833207846\""  +
                "    }" +
                "  }" ); //填充业务参数*/
        String form= "" ;
        try  {
            form = alipayClient.pageExecute(alipayRequest).getBody();  //调用SDK生成表单
        }  catch  (AlipayApiException e) {
            e.printStackTrace();
        }
        response.setContentType("text/html;charset=UTF-8");
//        response.getWriter().write(form); //直接将完整的表单html输出到页面
//        response.getWriter().flush();
//        response.getWriter().close();
        /*调用延迟队列*/
        paymentService.sendDelayPaymentResult(paymentInfo.getOutTradeNo(),15,3);
        return form;
    }

    /**
     * 交易完成后  同步回调
     * @return
     */
    @RequestMapping(value = "/alipay/callback/return",method = RequestMethod.GET)
    public String callbackReturn(){
        return "redirect:"+AlipayConfig.return_order_url;
    }

    /**
     * 异步回调
     * @param paramMap
     * @param request
     * @return
     */
    @RequestMapping(value = "/alipay/callback/notify",method = RequestMethod.POST)
    @ResponseBody
    public String paymentNotify(@RequestParam Map<String,String> paramMap, HttpServletRequest request) {
        /*异步验签*/
//        Map<String, String> paramsMap = ... //将异步通知中收到的所有参数都存放到map中
        boolean signVerified = false; //调用SDK验证签名
        try {
            /* paramMap:异步通知中收到的所有参数  SIGN_TYPE:签名类型  alipay_public_key:用户自己的支付KEY码  charset:编码格式  */
            signVerified = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(signVerified){
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            /*对业务的二次校验*/
            /*只有交易状态为TRADE_SUCCESS--交易支付成功  或者TRADE_FINISHED--	交易结束，不可退款才算成功*/
            /*获取交易状态*/
            String trade_status = paramMap.get("trade_status");
            /*通过商户订单号查询支付记录    out_trade_no:商户订单号*/
            String out_trade_no = paramMap.get("out_trade_no");
            /*WAIT_BUYER_PAY	交易创建，等待买家付款
            TRADE_CLOSED	未付款交易超时关闭，或支付完成后全额退款
            TRADE_SUCCESS	交易支付成功
            TRADE_FINISHED	交易结束，不可退款*/
            /*交易状态*/
            if("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)){
                PaymentInfo paymentInfo = new PaymentInfo();
                /*将商户订单号保存到支付表中*/
                paymentInfo.setOutTradeNo(out_trade_no);
                /*通过商户订单号 查询支付信息*/
                PaymentInfo paymentInfoHas = paymentService.getPaymentInfo(paymentInfo);
                /*UNPAID("支付中"),
                PAID("已支付"),
                PAY_FAIL("支付失败"),
                ClOSED("已关闭");*/
                /*支付状态*/
                if (paymentInfoHas.getPaymentStatus()== PaymentStatus.PAID || paymentInfoHas.getPaymentStatus()== PaymentStatus.ClOSED) {
                    return "fail";
                }
                /*更新交易记录状态*/
                // 修改
                PaymentInfo paymentInfoUpd = new PaymentInfo();
                // 设置状态
                paymentInfoUpd.setPaymentStatus(PaymentStatus.PAID);
                // 设置创建时间
                paymentInfoUpd.setCallbackTime(new Date());
                // 设置内容
                paymentInfoUpd.setCallbackContent(paramMap.toString());
                paymentService.updatePaymentInfo(out_trade_no,paymentInfoUpd);
                /*发送消息队列给订单*/
                paymentService.sendPaymentResult(paymentInfo,"success");
                return "success";
            }
        }else{
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }
        return "failure";
    }

    // 发送验证
    @RequestMapping("sendPaymentResult")
    @ResponseBody
    public String sendPaymentResult(PaymentInfo paymentInfo,@RequestParam("result") String result){
        paymentService.sendPaymentResult(paymentInfo,result);
        return "sent payment result";
    }

    /*退款*/
    /*http://payment.gmall.com/refund?orderId=100*/
    @RequestMapping("refund")
    @ResponseBody
    public String refund(String orderId){
        boolean flag = paymentService.refund(orderId);
        System.out.println("flag:"+flag);
        return flag+"";
    }

    // 查询订单信息
    @RequestMapping("queryPaymentResult")
    @ResponseBody
    public String queryPaymentResult(HttpServletRequest request){
        String orderId = request.getParameter("orderId");
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderId(orderId);
        PaymentInfo paymentInfo1 = paymentService.getPaymentInfo(paymentInfo);
        boolean flag = paymentService.checkPayment(paymentInfo1);
        return ""+flag;
    }
}
