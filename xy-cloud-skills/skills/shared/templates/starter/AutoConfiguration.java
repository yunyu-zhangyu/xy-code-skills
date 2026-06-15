package ${basePackage}.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * ${starterName} 自动配置
 *
 * @author ${author}
 * @date ${date}
 */
@AutoConfiguration
@ConditionalOnClass({XxxTemplate.class})
@EnableConfigurationProperties({${entity}Properties.class})
public class ${entity}AutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public XxxTemplate xxxTemplate(${entity}Properties properties) {
        return new XxxTemplate(properties);
    }

}
