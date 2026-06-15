/* =========================================================================
 * 【AI 生成标记】— 首次自动生成，请逐项审查确认。
 *
 * █ 生命周期管理:
 *   1. 【AI生成】标记 = 未审查，随时可删除回退
 *   2. 人工确认无误后 → 搜索替换 "【AI生成】" 为 "" 即可清理
 *   3. DO 的继承选择（BaseDO vs TenantBaseDO）是本文件最高风险点，请优先审查
 *
 * █ 可搜索：全局 grep "【AI生成】" 定位所有待审查代码
 * ========================================================================= */

package ${basePackage}.module.${module}.dal.dataobject.${domain};

import ${basePackage}.framework.mybatis.core.dataobject.BaseDO;
// 多租户场景：改为 import ${basePackage}.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * ${businessName} DO
 *
 * <p>【AI 生成·核查要点】
 * - 继承选择（BaseDO vs TenantBaseDO）在本文件下方以 ═══ 框标注，是【最高风险的审查点】
 * - 选错 BaseDO（本应是 TenantBaseDO 却用了 BaseDO）→ 租户间数据互相可见，不报错不抛异常
 * - @TableName 中的 ${tableName} 需与数据库实际表名一致
 * - JSON 字段必须配合 @TableName(autoResultMap = true)，否则查询返回 null（静默失败）
 *
 * @author ${author}
 * @date ${date}
 */
@TableName("${tableName}")
// ⚠️ 若 DO 包含 JSON 字段(@TableField(typeHandler = JacksonTypeHandler.class))，必须改为：
// @TableName(value = "${tableName}", autoResultMap = true)
// 否则 JSON 字段查询返回 null（不会报错！静默失败）
@KeySequence("${tableName}_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ${entity}DO extends BaseDO {
// ═══════════════════════════════════════════════════
//  DO 继承选择（必须二选一，不可选错！）
// ═══════════════════════════════════════════════════
//  【BaseDO】系统级表（字典/菜单/岗位/配置/通知/日志等全局共享数据）
//       → public class ${entity}DO extends BaseDO
//       → 不含 tenantId 字段，跨租户共享
//
//  【TenantBaseDO】租户级表（部门/用户/角色/业务数据等按租户隔离的数据）
//       → public class ${entity}DO extends TenantBaseDO
//       → 含 tenantId 字段，MyBatis-Plus TenantInterceptor 自动拼接 SQL
//
//  ⚠️ 静默失败警告：
//  租户表选错为 BaseDO 不会产生任何异常或错误日志——只是 tenant_id 不会被拦截器追加，
//  结果就是全租户数据互相可见。这是 xy-cloud 中最隐蔽的数据安全问题，没有之一。
// ═══════════════════════════════════════════════════
//  多租户场景取消下行注释，注释掉上文的 "extends BaseDO"：
// public class ${entity}DO extends TenantBaseDO {

    /** 主键 ID（自增策略，由 @KeySequence 管理） */
    @TableId
    private Long id;

    // ═══════════════════════════════════════════════════════════
    //  常用字段模式（根据实际需求取消注释并调整类型）
    // ═══════════════════════════════════════════════════════════
    //  /** 名称（如价格名称、分类名称等） */
    //  private String name;
    //
    //  /** 状态（system/infra 模块用 Integer，见 CommonStatusEnum：0=启用 1=停用） */
    //  private Integer status;
    //
    //  /** 状态（eos 模块用 String，业务编码如 "active"、"inactive"） */
    //  private String status;
    //
    //  /** JSON 字段示例：标签列表（必须配合 @TableName(autoResultMap = true) 使用） */
    //  @TableField(typeHandler = JacksonTypeHandler.class)
    //  private List<String> tags;
    //
    //  /** 非数据库字段（查询时临时存储关联数据） */
    //  @TableField(exist = false)
    //  private String extraField;
    // ═══════════════════════════════════════════════════════════

}
