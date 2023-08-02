package cn.itcast.mq.spring;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Yushun Shao
 * @date 2023/5/10 19:35
 * @description: springAMQP test
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class SpringAmqpTest {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    public void testSendMessage2SimpleQueue(){
        String queueName = "simple.queue";
        String message = "hello, spring amqp";
        rabbitTemplate.convertAndSend(queueName, message);
    }

    @Test
    public void testSendMessage2WorkQueue() throws InterruptedException {
        String queueName = "simple.queue";
        String message = "hello, message__";
        for (int i = 1; i <= 50; i++){
            rabbitTemplate.convertAndSend(queueName, message + i);
            Thread.sleep(20);
        }
    }

    @Test
    public void testSendFanoutExchange(){
        // 交换机名称
        String exchangeName = "fanout.test";
        // 消息
        String message = "hello, everyone!";
        // 发送
        rabbitTemplate.convertAndSend(exchangeName, "", message);
    }

    @Test
    public void testSendDirectExchange(){
        // 交换机名称
        String exchangeName = "direct.test";

//        String message = "hello, blue!";
//        rabbitTemplate.convertAndSend(exchangeName, "blue", message);
//        String message = "hello, yellow!";
//        rabbitTemplate.convertAndSend(exchangeName, "yellow", message);
        String message = "hello, red!";
        rabbitTemplate.convertAndSend(exchangeName, "red", message);
    }

    @Test
    public void testSendTopicExchange(){
        String exchangeName = "topic.test";
//        String message = "中国新闻";
//        rabbitTemplate.convertAndSend(exchangeName, "china.news", message);
//        String message = "中国天气";
//        rabbitTemplate.convertAndSend(exchangeName, "china.weather", message);
        String message = "美国新闻";
        rabbitTemplate.convertAndSend(exchangeName, "usa.news", message);
    }

    /**
     * SpringAMQP消息的序列化和反序列化底层使用MessageConverter实现，默认为JDK的序列化
     * 这里引入json依赖，配置类使用自定义MessageConverter覆盖默认，实现json序列化
     * 发送方和接收方必须使用相同的MessageConverter
     */
    @Test
    public void testSendObjectQueue(){
        Map<String, Object> msg = new HashMap<>();
        msg.put("name", "张三");
        msg.put("age", 21);
        rabbitTemplate.convertAndSend("object.queue", msg);
    }

}
