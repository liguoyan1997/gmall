package com.it.gmall.payment.activemq;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;

import javax.jms.*;

public class ProducerTest {

    /*
    * 1.创建连接工厂
    * 2.创建连接
    * 3.打开连接
    * 4.创建session
    * 5.创建队列
    * 6.创建消息提供者
    * 7.创建消息对象
    * 7_1.当异步时 要提交
    * 8.发送消息
    * 9.关闭
    * */
    public static void main(String[] args) throws JMSException {
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory("tcp://192.168.245.131:61616");
        Connection connection = activeMQConnectionFactory.createConnection();
        connection.start();
        /*第一个参数 表示是否开启事务
        * 第二个参数 表示开启/关闭事务的配置参数
        * */
//        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        /*开启事务   必须要提交*/
        Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
        Queue it = session.createQueue("it-true");
        /*生产者*/
        MessageProducer producer = session.createProducer(it);
        /*创建消息对象*/
        ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
        activeMQTextMessage.setText("真的很累");
        producer.send(activeMQTextMessage);

        /*开启事务时*/
        session.commit();
        producer.close();
        session.close();
        connection.close();
    }
}
