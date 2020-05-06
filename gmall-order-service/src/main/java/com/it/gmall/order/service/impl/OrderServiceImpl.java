package com.it.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.it.bean.OrderDetail;
import com.it.bean.OrderInfo;
import com.it.bean.enums.OrderStatus;
import com.it.bean.enums.ProcessStatus;
import com.it.gmall.HttpClientUtil;
import com.it.gmall.config.ActiveMQUtil;
import com.it.gmall.config.RedisUtil;
import com.it.gmall.order.mapper.OrderDetailMapper;
import com.it.gmall.order.mapper.OrderInfoMapper;
import com.it.service.OrderService;
import com.it.service.PaymentService;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.Queue;
import javax.jms.*;
import java.util.*;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ActiveMQUtil activeMQUtil;

    @Reference
    private PaymentService paymentService;

    @Transactional
    @Override
    public String saveOrder(OrderInfo orderInfo) {
        /*数据不完整*/
        /*总金额*/
        orderInfo.sumTotalAmount();
        // 设置创建时间
        orderInfo.setCreateTime(new Date());
        // 设置失效时间
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE,1);
        orderInfo.setExpireTime(calendar.getTime());
        // 生成第三方支付编号
        String outTradeNo="it"+System.currentTimeMillis()+""+new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        /*设置订单状态*/
        orderInfo.setOrderStatus(OrderStatus.UNPAID);
        /*进程状态*/
        orderInfo.setProcessStatus(ProcessStatus.UNPAID);
        /*只保存了一份订单*/
        orderInfoMapper.insertSelective(orderInfo);
        // 插入订单详细信息
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            /*设置orderId*/
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insertSelective(orderDetail);
        }
        return orderInfo.getId();
    }

    @Override
    public String getTradeNo(String userId) {
        Jedis jedis = redisUtil.getJedis();
        /*定义一个key*/
        String tradeNoKey="user:"+userId+":tradeCode";
        /*定义一个流水号*/
        String tradeCode = UUID.randomUUID().toString();
        /*放入缓存*/
        jedis.set(tradeNoKey,tradeCode);
        /*10分钟后过期*/
//        jedis.setex(tradeNoKey,10*60,tradeCode);
        jedis.close();
        return tradeCode;
    }

    @Override
    public boolean checkTradeCode(String userId, String tradeCodeNo) {
        Jedis jedis = redisUtil.getJedis();
        /*定义一个key*/
        String tradeNoKey="user:"+userId+":tradeCode";
        /*获取缓存中的流水号*/
        String tradeNo = jedis.get(tradeNoKey);
        jedis.close();
        if (tradeNo!=null && tradeNo.equals(tradeCodeNo)){
            return true;
        }else{
            return false;
        }
    }

    @Override
    public void delTradeCode(String userId) {
        Jedis jedis = redisUtil.getJedis();
        /*定义一个key*/
        String tradeNoKey="user:"+userId+":tradeCode";
        /*删除*/
        jedis.del(tradeNoKey);
        jedis.close();
    }

    // 验证库存
    @Override
    public boolean checkStock(String skuId,Integer skuNum){
        String result = HttpClientUtil.doGet("http://www.gware.com/hasStock?skuId=" + skuId + "&num=" + skuNum);
        if ("1".equals(result)){
            return  true;
        }else {
            return  false;
        }
    }

    @Override
    public OrderInfo getOrderInfo(String orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectByPrimaryKey(orderId);
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderId);
        List<OrderDetail> select = orderDetailMapper.select(orderDetail);
        orderInfo.setOrderDetailList(select);
        return orderInfo;
    }

    @Override
    public void updateOrderStatus(String orderId, ProcessStatus paid) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(paid);
        orderInfo.setOrderStatus(paid.getOrderStatus());
        orderInfoMapper.updateByPrimaryKeySelective(orderInfo);
    }

    @Override
    public void sendOrderStatus(String orderId) {
        Connection connection = activeMQUtil.getConnection();
        String orderJson = initWareOrder(orderId);
        try {
            connection.start();
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue queue = session.createQueue("ORDER_RESULT_QUEUE");
            MessageProducer producer = session.createProducer(queue);
            /*创建消息对象*/
            ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
            /**/
            /*orderInfo组成的json字符串*/
            activeMQTextMessage.setText(orderJson);
            producer.send(activeMQTextMessage);
            /*提交*/
            session.commit();
            /*关闭*/
            closeMQ(connection, session, producer);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<OrderInfo> getExpiredOrderList() {
        Example example = new Example(OrderInfo.class);
        example.createCriteria().andLessThan("expireTime",new Date()).andEqualTo("processStatus",ProcessStatus.UNPAID);
        return orderInfoMapper.selectByExample(example);
    }

    @Override
    @Async
    public void execExpiredOrder(OrderInfo orderInfo) {
        // 将订单状态改为关闭
        updateOrderStatus(orderInfo.getId(),ProcessStatus.CLOSED);
        // 关闭付款信息
        paymentService.closePayment(orderInfo.getId());
    }

    @Override
    public List<OrderInfo> splitOrder(String orderId, String wareSkuMap) {
        List<OrderInfo> subOrderInfoList = new ArrayList<>();
        // 1 先查询原始订单
        OrderInfo orderInfoOrigin = getOrderInfo(orderId);
        // 2 wareSkuMap 反序列化 转换为我们可以操作的对象
        List<Map> maps = JSON.parseArray(wareSkuMap, Map.class);
        // 3 遍历拆单方案
        if (maps!=null){
            for (Map map : maps) {
                /*获取仓库ID*/
                String wareId = (String) map.get("wareId");
                /*获取商品ID*/
                List<String> skuIds = (List<String>) map.get("skuIds");
                /*新的订单*/
                OrderInfo subOrderInfo = new OrderInfo();
                /*属性拷贝*/
                BeanUtils.copyProperties(orderInfoOrigin,subOrderInfo);
                subOrderInfo.setId(null);
                // 5 原来主订单，订单主表中的订单状态标志为拆单
                subOrderInfo.setParentOrderId(orderId);
                subOrderInfo.setWareId(wareId);
                // 6 明细表 根据拆单方案中的skuids进行匹配，得到那个的子订单
                List<OrderDetail> orderDetailList = orderInfoOrigin.getOrderDetailList();
                // 创建一个新的订单集合
                List<OrderDetail> subOrderDetailList = new ArrayList<>();
                /*原始的订单明细*/
                for (OrderDetail orderDetail : orderDetailList) {
                    /*仓库对应的商品ID*/
                    for (String skuId : skuIds) {
                        if (skuId.equals(orderDetail.getSkuId())){
                            orderDetail.setId(null);
                            subOrderDetailList.add(orderDetail);
                        }
                    }
                }

                /*将新的子订单集合放到订单中*/
                subOrderInfo.setOrderDetailList(subOrderDetailList);
                /*计算价格*/
                subOrderInfo.sumTotalAmount();
                // 7 保存到数据库中
                saveOrder(subOrderInfo);
                /*将新的子订单添加到集合中*/
                subOrderInfoList.add(subOrderInfo);
            }
        }
        /*更新原始订单状态*/
        updateOrderStatus(orderId,ProcessStatus.SPLIT);
        // 8 返回一个新生成的子订单列表
        return subOrderInfoList;
    }

    /**
     * 根据orderId将orderInfo变成字符串
     * @param orderId
     * @return
     */
    private String initWareOrder(String orderId) {
        /*根据orderId查orderInfo*/
        OrderInfo orderInfo = getOrderInfo(orderId);
        /*将orderInfo有用的信息保存到map*/
        Map map = initWareOrder(orderInfo);
        /*将map转化为json字符串*/
        return JSON.toJSONString(map);
    }

    @Override
    public Map initWareOrder(OrderInfo orderInfo) {
        /*给map的key赋值*/
        Map<String,Object> map = new HashMap<>();
        map.put("orderId",orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel",orderInfo.getConsigneeTel());
        map.put("orderComment",orderInfo.getOrderComment());
        map.put("orderBody","给李国琰");
        map.put("deliveryAddress",orderInfo.getDeliveryAddress());
        map.put("paymentWay","2");
        /*getWareId仓库ID*/
        map.put("wareId",orderInfo.getWareId());
        // 组合json
        List detailList = new ArrayList();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            Map detailMap = new HashMap();
            detailMap.put("skuId",orderDetail.getSkuId());
            detailMap.put("skuName",orderDetail.getSkuName());
            detailMap.put("skuNum",orderDetail.getSkuNum());
            detailList.add(detailMap);
        }
        map.put("details",detailList);
        return map;
    }

    private void closeMQ(Connection connection, Session session, MessageProducer producer) throws JMSException {
        producer.close();
        session.close();
        connection.close();
    }
}
