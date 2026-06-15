package ${basePackage}.module.${module}.api.${domain}.vo;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * ${businessName} Request DTO
 *
 * @author ${author}
 * @date ${date}
 */
@Data
public class ${entity}ReqDTO {

    @NotNull(message = "编号不能为空")
    private Long id;

    @NotEmpty(message = "名称不能为空")
    @Size(max = 50, message = "名称长度不能超过50字符")
    private String name;

}
