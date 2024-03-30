package com.senfan.senfanojbackendjudgeservice.rabbitmq;

import com.rabbitmq.client.Channel;
import com.senfan.senfanojbackendcommon.common.ErrorCode;
import com.senfan.senfanojbackendcommon.exception.BusinessException;
import com.senfan.senfanojbackendjudgeservice.judge.JudgeService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class MyMessageConsumer {

    @Resource
    private JudgeService judgeService;
    @Resource
    private RedisTemplate<String, Integer> redisTemplate;

    // 指定程序监听的消息队列和确认机制
    @SneakyThrows
    @RabbitListener(queues = {"code_queue"}, ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("receiveMessage message = {}", message);
        long questionSubmitId = Long.parseLong(message);

        // 获取重试次数
        int retryCount = getRetryCountFromRedis(questionSubmitId);

        try {
            judgeService.doJudge(questionSubmitId);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("判题处理失败", e);
            // 判断是否达到最大重试次数
            if (retryCount < 3) {
                // 增加重试次数并重新发送消息到队列
                retryCount++;
                setRetryCountToRedis(questionSubmitId, retryCount);
                channel.basicNack(deliveryTag, false, true);
            } else {
                // 超过最大重试次数，拒绝消息
                channel.basicNack(deliveryTag, false, false);
            }
        }
    }

    private int getRetryCountFromRedis(long questionSubmitId) {
        String key = "doJudge:" + questionSubmitId;
        Integer retryCount = redisTemplate.opsForValue().get(key);
        if (retryCount != null) {
            return retryCount;
        }
        return 0;
    }

    private void setRetryCountToRedis(long questionSubmitId, int retryCount) {
        String key = "doJudge:" + questionSubmitId;
        redisTemplate.opsForValue().set(key, retryCount);
    }
}