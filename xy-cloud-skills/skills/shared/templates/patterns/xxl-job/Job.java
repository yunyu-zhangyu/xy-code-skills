package ${basePackage}.module.${module}.job.${domain};

import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * ${businessName} 定时任务
 *
 * @author ${author}
 * @date ${date}
 */
@Slf4j
@Component
public class ${entity}Job {

    @Resource
    private ${entity}Service ${resource}Service;

    /**
     * ${businessName} 定时处理
     * 建议 cron: 0 0/5 * * * ? （每5分钟）
     */
    @XxlJob("${resource}JobHandler")
    public void execute() {
        log.info("[${resource}Job] 定时任务开始执行");
        try {
            // TODO: 实现定时任务逻辑
            ${resource}Service.xxx();
        } catch (Exception e) {
            log.error("[${resource}Job] 定时任务执行异常", e);
            throw e;
        }
        log.info("[${resource}Job] 定时任务执行完成");
    }
}
