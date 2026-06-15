// === 在 mq/producer/${domain}/ 下新建 ${entity}Producer.java ===
package ${basePackage}.module.${module}.mq.producer.${domain};

import ${basePackage}.module.${module}.mq.message.${domain}.${entity}Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * ${businessName} 消息生产者（基于 Spring Event）
 *
 * @author ${author}
 * @date ${date}
 */
@Slf4j
@Component
public class ${entity}Producer {

    @Resource
    private ApplicationContext applicationContext;

    public void send(${entity}Message message) {
        log.info("[${resource}Producer] 发送消息: {}", message);
        applicationContext.publishEvent(message);
    }
}
