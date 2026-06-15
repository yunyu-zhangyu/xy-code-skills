// === 在 mq/consumer/${domain}/ 下新建 ${entity}Consumer.java ===
package ${basePackage}.module.${module}.mq.consumer.${domain};

import ${basePackage}.module.${module}.mq.message.${domain}.${entity}Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * ${businessName} 消息消费者（基于 Spring Event）
 *
 * @author ${author}
 * @date ${date}
 */
@Slf4j
@Component
public class ${entity}Consumer {

    @Resource
    private ${entity}Service ${resource}Service;

    @Async
    @EventListener
    public void on${entity}Event(${entity}Message message) {
        log.info("[${resource}Consumer] 收到消息: {}", message);
        try {
            // TODO: 处理消息逻辑
            ${resource}Service.handleEvent(message);
        } catch (Exception e) {
            log.error("[${resource}Consumer] 消息处理异常: {}", message, e);
        }
    }
}
