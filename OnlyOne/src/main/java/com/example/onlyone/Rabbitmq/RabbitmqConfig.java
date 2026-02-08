package com.example.onlyone.Rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class RabbitmqConfig {

    // ============ 文章队列配置 ============
    @Bean
    public DirectExchange articlePushExchange() {
        return new DirectExchange("articlePushExchange", true, false);
    }

    @Bean
    public Queue articlePushQueue() {

        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "articlePush.dlx.exchange");
        // 指定死信路由键
        args.put("x-dead-letter-routing-key", "articlePush.dlx.routing.key");

        return new Queue("articlePushQueue", true);
    }

    @Bean
    public Binding articlePushBinding() {
        return BindingBuilder.bind(articlePushQueue()).to(articlePushExchange()).with("articlePush");
    }

    // ============ 互动队列配置 ============

    @Bean
    public TopicExchange actionExchange() {
        return new TopicExchange("actionExchange", true, false);
    }

    //点赞队列
    @Bean
    public Queue loveQueue() {
        return new Queue("loveQueue", true);
    }
    //关注队列
    @Bean
    public Queue followQueue() {
        return new Queue("followQueue", true);
    }
    //评论队列
    @Bean
    public Queue commentBinding() {
        return new Queue("commentBinding", true);
    }


    // ==================== 文章推送死信队列配置 ====================

    // 死信交换器
    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange("articlePush.dlx.exchange", true, false);
    }

    // 死信队列
    @Bean
    public Queue dlxQueue() {
        return QueueBuilder.durable("dlx.queue")
                .withArgument("x-queue-mode", "lazy")  // 惰性队列，磁盘存储
                .build();
    }

    // 死信队列绑定
    @Bean
    public Binding dlxBinding() {
        return BindingBuilder.bind(dlxQueue())
                .to(dlxExchange())
                .with("articlePush.dlx.routing.key");
    }




    //消息格式转换器（json格式）
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }


    //自定义配置
    //设置生产者确认回调
    //设置生产者返回回调
    //消息持久化
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {

        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());

        //发送消息到交换机失败触发
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("消息发送失败到交换机, correlationData: {}, cause: {}", correlationData, cause);
            }
        });

        //交换机发送到队列失败后触发
        rabbitTemplate.setReturnsCallback(returned -> {
            log.error("消息无法路由到队列: exchange={}, routingKey={}, replyText={}, message={}",
                    returned.getExchange(),
                    returned.getRoutingKey(),
                    returned.getReplyText(),
                    returned.getMessage());

        });

        //保证到达队列后失败的强制触发
        rabbitTemplate.setMandatory(true);

        return rabbitTemplate;
    }






}
