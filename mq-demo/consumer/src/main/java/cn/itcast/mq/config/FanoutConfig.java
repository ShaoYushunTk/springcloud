package cn.itcast.mq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Yushun Shao
 * @date 2023/5/11 10:24
 * @description: Fanout config
 */
@Configuration
public class FanoutConfig {
    /**
     * 声明fanoutExchange交换机
     * @return
     */
    @Bean
    public FanoutExchange fanoutExchange(){
        return new FanoutExchange("fanout.test");
    }

    /**
     * 声明队列1
     * @return
     */
    @Bean
    public Queue fanoutQueue1(){
        return new Queue("fanout.queue1");
    }

    /**
     * 绑定队列1到fanoutExchange交换机
     * @param fanoutExchange
     * @param fanoutQueue1
     * @return
     */
    @Bean
    public Binding bindingQueue1(FanoutExchange fanoutExchange, Queue fanoutQueue1){
        return BindingBuilder.bind(fanoutQueue1).to(fanoutExchange);
    }

    /**
     * 声明队列2
     * @return
     */
    @Bean
    public Queue fanoutQueue2(){
        return new Queue("fanout.queue2");
    }

    /**
     * 绑定队列2到fanoutExchange交换机
     * @param fanoutExchange
     * @param fanoutQueue2
     * @return
     */
    @Bean
    public Binding bindingQueue2(FanoutExchange fanoutExchange, Queue fanoutQueue2){
        return BindingBuilder.bind(fanoutQueue2).to(fanoutExchange);
    }

    @Bean
    public Queue objectQueue(){
        return new Queue("object.queue");
    }
}
