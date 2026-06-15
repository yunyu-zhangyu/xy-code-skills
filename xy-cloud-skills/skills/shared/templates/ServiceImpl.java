/* =========================================================================
 * 【AI 生成标记】— 首次自动生成，请逐项审查确认。
 *
 * █ 生命周期管理:
 *   1. 【AI生成】标记 = 未审查，随时可删除回退
 *   2. 人工确认无误后 → 搜索替换 "【AI生成】" 为 "" 即可清理
 *   3. 若需修改 → 修改后移除对应方法的 【AI生成】标记
 *
 * █ 可搜索：全局 grep "【AI生成】" 定位所有待审查代码
 * ========================================================================= */

package ${basePackage}.module.${module}.service.${domain};

import ${basePackage}.framework.common.pojo.PageResult;
import ${basePackage}.framework.mybatis.core.query.LambdaQueryWrapperX;
import ${basePackage}.framework.common.util.object.BeanUtils;
import ${basePackage}.module.${module}.controller.admin.${domain}.vo.${entity}SaveReqVO;
import ${basePackage}.module.${module}.controller.admin.${domain}.vo.${entity}PageReqVO;
import ${basePackage}.module.${module}.controller.admin.${domain}.vo.${entity}ListReqVO;
import ${basePackage}.module.${module}.dal.dataobject.${domain}.${entity}DO;
import ${basePackage}.module.${module}.dal.mysql.${domain}.${entity}Mapper;
import ${basePackage}.framework.mybatis.core.dataobject.BaseDO;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.validation.Valid;
import java.util.List;

/**
 * ${businessName} Service 实现类
 *
 * <p>【AI 生成·核查要点】
 * - 写操作都标注了 @Transactional(rollbackFor = Exception.class)，审查事务边界是否正确
 * - 关键业务校验已在方法注释中标记 【核查点】，需按实际业务补充
 * - BeanUtils.toBean 字段映射：确认 reqVO 字段名与 DO 一致，不一致时需手动转换
 * - 每个方法均标注了 【AI生成】，审查无误后请移除标记
 *
 * @author ${author}
 * @date ${date}
 */
@Service
public class ${entity}ServiceImpl implements ${entity}Service {

    @Resource
    private ${entity}Mapper ${resource}Mapper;

    // ========== 【AI生成】CRUD 实现 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 【AI生成】创建${businessName}
     *
     * <p>将请求 VO 转换为 DO 后插入数据库，自动填充基础审计字段。</p>
     *
     * @param reqVO 创建请求参数
     * @return 新记录的主键 ID
     */
    public Long create${entity}(${entity}SaveReqVO reqVO) {
        // → BeanUtils.toBean 按字段名自动映射（字段名不一致时会丢失数据，请审查）
        ${entity}DO entity = BeanUtils.toBean(reqVO, ${entity}DO.class);

        // === 【核查点】是否有唯一性校验？如 name 不能重复、code 不能重复 ===
        // 示例：if (${resource}Mapper.selectByName(reqVO.getName()) != null) {
        //          throw new ServiceException(${entity}_NAME_DUPLICATE);
        //      }

        // → 自动填充 createTime、updateTime、deleted（由 BaseDO 的 DefaultDBFieldHandler 处理）
        ${resource}Mapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 【AI生成】更新${businessName}
     *
     * <p>根据 ID 更新记录，只更新传入的非 null 字段。</p>
     *
     * @param reqVO 更新请求参数（必须包含 id）
     */
    public void update${entity}(${entity}SaveReqVO reqVO) {
        // === 【核查点】更新前是否需校验记录存在？ ===
        // 示例：${entity}DO existing = ${resource}Mapper.selectById(reqVO.getId());
        //      if (existing == null) { throw new ServiceException(${entity}_NOT_FOUND); }

        // → 只更新传入的非 null 字段（BeanUtils.toBean + updateById 行为）
        ${entity}DO entity = BeanUtils.toBean(reqVO, ${entity}DO.class);
        ${resource}Mapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 【AI生成】删除${businessName}
     *
     * <p>根据 ID 删除记录。逻辑/物理删除由 DO 继承父类决定。</p>
     *
     * @param id ${businessName}编号
     */
    public void delete${entity}(Long id) {
        // === 【核查点】删除前是否需要校验记录存在或有关联数据？ ===
        // 示例：if (${resource}Mapper.selectById(id) == null) {
        //          throw new ServiceException(${entity}_NOT_FOUND);
        //      }
        ${resource}Mapper.deleteById(id);
        // → 逻辑/物理删除由 DO 继承父类决定：BaseDO → 逻辑(deleted=1)，无基类 → 物理
    }

    @Override
    /**
     * 【AI生成】根据编号获得${businessName}
     *
     * @param id ${businessName}编号
     * @return ${businessName} DO，不存在时返回 null
     */
    public ${entity}DO get${entity}(Long id) {
        // → 不存在时返回 null，Controller 层自行决定返回 null 还是抛异常
        return ${resource}Mapper.selectById(id);
    }

    @Override
    /**
     * 【AI生成】获得${businessName}列表
     *
     * @param reqVO 查询参数
     * @return ${businessName}列表，无数据时为空列表
     */
    public List<${entity}DO> get${entity}List(${entity}ListReqVO reqVO) {
        // → 无分页查询，适合下拉选择器等场景（大数据量时需评估是否需要分页）
        return ${resource}Mapper.selectList(new LambdaQueryWrapperX<${entity}DO>());
    }

    @Override
    /**
     * 【AI生成】获得${businessName}分页
     *
     * @param pageReqVO 分页查询参数（继承 PageParam，自动处理 pageNo/pageSize）
     * @return 分页结果，无数据时 total=0 且 list 为空列表
     */
    public PageResult<${entity}DO> get${entity}Page(${entity}PageReqVO pageReqVO) {
        // → 分页查询：pageReqVO 继承 PageParam，自动处理 pageNo/pageSize
        return ${resource}Mapper.selectPage(pageReqVO, new LambdaQueryWrapperX<${entity}DO>()
            .likeIfPresent(${entity}DO::getName, pageReqVO.getName())         // 名称模糊搜索
            .eqIfPresent(${entity}DO::getStatus, pageReqVO.getStatus())       // 状态筛选
            .orderByDesc(${entity}DO::getId));  // 默认按创建顺序倒序
    }
}
