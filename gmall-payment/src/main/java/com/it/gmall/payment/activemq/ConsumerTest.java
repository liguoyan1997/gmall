package com.it.gmall.payment.activemq;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

public class ConsumerTest {
    public static void main(String[] args) throws JMSException {

        /*
         * 1.创建连接工厂
         * 2.创建连接
         * 3.打开连接
         * 4.创建session
         * 5.创建队列
         * 6.创建消息消费者
         * 7.创建消息监听器
         * 8.消费消息
         * */
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory(ActiveMQConnection.DEFAULT_USER,ActiveMQConnection.DEFAULT_PASSWORD,"tcp://192.168.245.131:61616");
        Connection connection = activeMQConnectionFactory.createConnection();
        connection.start();
        /*第一个参数 表示是否开启事务
         * 第二个参数 表示开启/关闭事务的配置参数
         * */
//        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        /*开启事务   必须要提交*/
        Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
        Queue it = session.createQueue("it-true");

        MessageConsumer consumer = session.createConsumer(it);
        /*消息的监听器*/
        consumer.setMessageListener(new MessageListener() {
            @Override
            public void onMessage(Message message) {
                if(message instanceof TextMessage){
                    try {
                        String text = ((TextMessage) message).getText();
                        System.out.println("接收的消息为:"+text);
                    } catch (JMSException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}
