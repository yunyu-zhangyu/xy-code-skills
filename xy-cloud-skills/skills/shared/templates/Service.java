/* =========================================================================
 * 【AI 生成标记】— 首次自动生成，请逐项审查确认。
 *
 * █ 生命周期管理:
 *   1. 【AI生成】标记 = 未审查，随时可删除回退
 *   2. 人工确认无误后 → 搜索替换 "【AI生成】" 为 "" 即可清理
 *   3. 接口方法签名与返回值类型是业务契约，请重点审查参数类型是否正确
 *
 * █ 可搜索：全局 grep "【AI生成】" 定位所有待审查代码
 * ========================================================================= */

package ${basePackage}.module.${module}.service.${domain};

import ${basePackage}.framework.common.pojo.PageResult;
import ${basePackage}.module.${module}.controller.admin.${domain}.vo.${entity}SaveReqVO;
import ${basePackage}.module.${module}.controller.admin.${domain}.vo.${entity}PageReqVO;
import ${basePackage}.module.${module}.controller.admin.${domain}.vo.${entity}ListReqVO;
import ${basePackage}.module.${module}.dal.dataobject.${domain}.${entity}DO;

import java.util.List;

/**
 * ${businessName} Service 接口
 * <p>
 * 定义 ${businessName} 的核心业务操作契约。
 * 实现类通过 @Transactional 控制写操作的事务边界。
 *
 * @author ${author}
 * @date ${date}
 */
public interface ${entity}Service {

    /**
     * 创建${businessName}
     *
     * @param reqVO 创建请求参数（非空校验已在 VO 层通过 @Valid 完成）
     * @return 新创建的${businessName} ID
     */
    Long create${entity}(${entity}SaveReqVO reqVO);

    /**
     * 更新${businessName}
     * <p>
     * 只更新传入的非 null 字段（由 BeanUtils.toBean + updateById 行为决定）。
     * 若传入的 id 不存在，抛出 ${entity}_NOT_FOUND 异常。
     *
     * @param reqVO 更新请求参数（必须包含 id）
     */
    void update${entity}(${entity}SaveReqVO reqVO);

    /**
     * 删除${businessName}
     * <p>
     * 逻辑删除或物理删除由 DO 的继承父类决定：
     * extends BaseDO/TenantBaseDO 为逻辑删除（deleted=1），否则为物理删除。
     *
     * @param id ${businessName}编号
     */
    void delete${entity}(Long id);

    /**
     * 根据编号获得${businessName}
     *
     * @param id ${businessName}编号
     * @return ${businessName} DO，不存在时返回 null
     */
    ${entity}DO get${entity}(Long id);

    /**
     * 获得${businessName}列表
     *
     * @param reqVO 查询参数（支持按名称模糊搜索、按状态筛选）
     * @return ${businessName}列表，无结果时返回空列表（非 null）
     */
    List<${entity}DO> get${entity}List(${entity}ListReqVO reqVO);

    /**
     * 获得${businessName}分页
     *
     * @param pageReqVO 分页查询参数
     * @return 分页结果，无数据时 total=0, list 为空列表
     */
    PageResult<${entity}DO> get${entity}Page(${entity}PageReqVO pageReqVO);
}
