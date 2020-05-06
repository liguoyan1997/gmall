package com.it.gmall.payment;

import com.it.gmall.config.ActiveMQUtil;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.jms.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallPaymentApplicationTests {

    @Autowired
    private ActiveMQUtil activeMQUtil;

    @Test
    public void contextLoads() {
    }

    @Test
    public void activeMq() throws JMSException {
        Connection connection = activeMQUtil.getConnection();
//        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory("tcp://192.168.245.131:61616");
//        Connection connection = activeMQConnectionFactory.createConnection();
        connection.start();
        /*第一个参数 表示是否开启事务
         * 第二个参数 表示开启/关闭事务的配置参数
         * */
//        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        /*开启事务   必须要提交*/
        Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
        Queue it = session.createQueue("it-tool");
        /*生产者*/
        MessageProducer producer = session.createProducer(it);
        /*创建消息对象*/
        ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
        activeMQTextMessage.setText("太烦了......");
        producer.send(activeMQTextMessage);

        /*开启事务时*/
        session.commit();
        producer.close();
        session.close();
        connection.close();
    }
}
