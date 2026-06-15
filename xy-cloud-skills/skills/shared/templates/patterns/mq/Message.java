// === 在 mq/message/${domain}/ 下新建 ${entity}Message.java ===
package ${basePackage}.module.${module}.mq.message.${domain};

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * ${businessName} 消息体
 *
 * @author ${author}
 * @date ${date}
 */
@Data
public class ${entity}Message implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 业务主键 ID
     */
    private Long id;

    /**
     * 事件类型
     */
    private Integer eventType;

    /**
     * 事件时间
     */
    private LocalDateTime eventTime;

}
