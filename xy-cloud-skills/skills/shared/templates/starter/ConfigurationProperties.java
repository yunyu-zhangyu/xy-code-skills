package ${basePackage}.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * ${starterName} 配置属性
 *
 * @author ${author}
 * @date ${date}
 */
@ConfigurationProperties(prefix = "${propertiesPrefix}")
@Validated
public class ${entity}Properties {

    @NotEmpty(message = "主机地址不能为空")
    private String host = "127.0.0.1";

    @NotNull(message = "端口不能为空")
    private Integer port = 8080;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

}
