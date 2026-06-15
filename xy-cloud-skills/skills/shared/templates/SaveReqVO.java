/* =========================================================================
 * 【AI 生成标记】— 首次自动生成，请逐项审查确认。
 *
 * █ 生命周期管理:
 *   1. 【AI生成】标记 = 未审查，随时可删除回退
 *   2. 人工确认无误后 → 搜索替换 "【AI生成】" 为 "" 即可清理
 *   3. 重点审查 @Schema(description) 与实际字段含义是否一致
 *
 * █ 可搜索：全局 grep "【AI生成】" 定位所有待审查代码
 * ========================================================================= */

package ${basePackage}.module.${module}.controller.admin.${domain}.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

/**
 * ${businessName} 新增/修改 Request VO
 *
 * @author ${author}
 * @date ${date}
 */
@Schema(description = "管理后台 - ${businessName} 新增/修改 Request VO")
@Data
public class ${entity}SaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotEmpty(message = "名称不能为空")
    @Size(max = 50, message = "名称长度不能超过50字符")
    private String name;

}
