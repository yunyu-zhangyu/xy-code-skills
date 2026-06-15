/* =========================================================================
 * 【AI 生成标记】— 首次自动生成，请逐项审查确认。
 *
 * █ 生命周期管理:
 *   1. 【AI生成】标记 = 未审查，随时可删除回退
 *   2. 人工确认无误后 → 搜索替换 "【AI生成】" 为 "" 即可清理
 *   3. 确认 Mapper XML 文件也已同步创建（否则运行时报 Invalid bound statement）
 *
 * █ 可搜索：全局 grep "【AI生成】" 定位所有待审查代码
 * ========================================================================= */

package ${basePackage}.module.${module}.dal.mysql.${domain};

import ${basePackage}.framework.common.pojo.PageResult;
import ${basePackage}.framework.mybatis.core.mapper.BaseMapperX;
import ${basePackage}.framework.mybatis.core.query.LambdaQueryWrapperX;
import ${basePackage}.module.${module}.controller.admin.${domain}.vo.${entity}PageReqVO;
import ${basePackage}.module.${module}.dal.dataobject.${domain}.${entity}DO;
import org.apache.ibatis.annotations.Mapper;

/**
 * ${businessName} Mapper 接口
 *
 * <p>继承 BaseMapperX 获得通用 CRUD 方法，自定义查询通过 default 方法或 XML 实现。</p>
 *
 * @author ${author}
 * @date ${date}
 * @see src/main/resources/mapper/${entity}Mapper.xml
 */
@Mapper
public interface ${entity}Mapper extends BaseMapperX<${entity}DO> {

    /**
     * 【AI生成】分页查询${businessName}
     *
     * <p>支持按名称模糊搜索、按状态筛选、按创建时间范围查询。</p>
     *
     * @param reqVO 分页查询参数
     * @return 分页结果
     */
    default PageResult<${entity}DO> selectPage(${entity}PageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<${entity}DO>()
                .likeIfPresent(${entity}DO::getName, reqVO.getName())             // 名称模糊搜索
                .eqIfPresent(${entity}DO::getStatus, reqVO.getStatus())           // 状态筛选
                .betweenIfPresent(${entity}DO::getCreateTime, reqVO.getCreateTime()) // 创建时间范围
                .orderByDesc(${entity}DO::getId));  // 默认按创建顺序倒序
    }

}
