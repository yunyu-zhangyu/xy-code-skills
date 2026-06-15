/* =========================================================================
 * 【AI 生成标记】— 首次自动生成，请逐项审查确认。
 *
 * █ 生命周期管理:
 *   1. 【AI生成】标记 = 未审查，随时可删除回退
 *   2. 人工确认无误后 → 搜索替换 "【AI生成】" 为 "" 即可清理
 *
 * █ 可搜索：全局 grep "【AI生成】" 定位所有待审查代码
 * ========================================================================= */

package ${basePackage}.module.${module}.controller.admin.${domain}.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * ${businessName} 列表 Request VO
 *
 * @author ${author}
 * @date ${date}
 */
@Schema(description = "管理后台 - ${businessName} 列表 Request VO")
@Data
public class ${entity}ListReqVO {

    @Schema(description = "名称", example = "张三")
    private String name;

    @Schema(description = "状态", example = "0")
    private Integer status;

}
