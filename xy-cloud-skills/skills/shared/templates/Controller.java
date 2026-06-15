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

package ${basePackage}.module.${module}.controller.admin.${domain};

import ${basePackage}.framework.apilog.core.annotation.ApiAccessLog;
import ${basePackage}.framework.common.pojo.CommonResult;
import ${basePackage}.framework.common.pojo.PageParam;
import ${basePackage}.framework.common.pojo.PageResult;
import ${basePackage}.framework.common.util.object.BeanUtils;
import ${basePackage}.framework.excel.core.util.ExcelUtils;
import ${basePackage}.module.${module}.controller.admin.${domain}.vo.${entity}PageReqVO;
import ${basePackage}.module.${module}.controller.admin.${domain}.vo.${entity}RespVO;
import ${basePackage}.module.${module}.controller.admin.${domain}.vo.${entity}SaveReqVO;
import ${basePackage}.module.${module}.dal.dataobject.${domain}.${entity}DO;
import ${basePackage}.module.${module}.service.${domain}.${entity}Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

import static ${basePackage}.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static ${basePackage}.framework.common.pojo.CommonResult.success;

/**
 * ${businessName} Controller
 *
 * <p>【AI 生成·核查要点】
 * - @PreAuthorize 权限字符串：格式 '{module}:{resource}:{action}'，需确认与后端权限数据一致
 * - @Tag name 呈现在 Swagger UI 分组标题，确认中文命名无歧义
 * - 每个方法都已标注 @Operation(summary=...)，确认 description 是否满足需要
 * - @Validated 类级别 + @Valid 参数级别：新增方法也需标注 @Valid
 * - 每个方法均有 @param/@return JavaDoc，审查参数描述是否准确
 *
 * @author ${author}
 * @date ${date}
 */
@Tag(name = "管理后台 - ${businessName}")
@RestController
@RequestMapping("/${module}/${resource}")
@Validated
public class ${entity}Controller {

    @Resource
    private ${entity}Service ${resource}Service;

    // ========== 【AI生成】增删改查接口 ==========

    /**
     * 【AI生成】创建${businessName}
     *
     * <p>新增一条${businessName}记录，返回自增主键 ID。</p>
     *
     * @param reqVO 创建请求参数（已在 VO 层通过 @Valid 完成字段校验）
     * @return 新记录 ID，封装在 CommonResult 中
     */
    @PostMapping("create")
    @Operation(summary = "创建${businessName}")
    @PreAuthorize("@ss.hasPermission('${module}:${resource}:create')")
    public CommonResult<Long> create${entity}(@Valid @RequestBody ${entity}SaveReqVO reqVO) {
        return success(${resource}Service.create${entity}(reqVO));
    }

    /**
     * 【AI生成】更新${businessName}
     *
     * <p>根据 ID 更新${businessName}，只更新传入的非 null 字段。</p>
     *
     * @param reqVO 更新请求参数（必须包含 id，其余字段按需传入）
     * @return 操作成功状态 true
     */
    @PutMapping("update")
    @Operation(summary = "更新${businessName}")
    @PreAuthorize("@ss.hasPermission('${module}:${resource}:update')")
    public CommonResult<Boolean> update${entity}(@Valid @RequestBody ${entity}SaveReqVO reqVO) {
        ${resource}Service.update${entity}(reqVO);
        return success(true);
    }

    /**
     * 【AI生成】删除${businessName}
     *
     * <p>根据 ID 删除${businessName}（逻辑删除或物理删除由 DO 父类决定）。</p>
     *
     * @param id ${businessName}编号
     * @return 操作成功状态 true
     */
    @DeleteMapping("delete")
    @Operation(summary = "删除${businessName}")
    @Parameter(name = "id", description = "${businessName}编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('${module}:${resource}:delete')")
    public CommonResult<Boolean> delete${entity}(@RequestParam("id") Long id) {
        ${resource}Service.delete${entity}(id);
        return success(true);
    }

    /**
     * 【AI生成】获得${businessName}
     *
     * <p>根据 ID 获取${businessName}详情，不存在时返回 null。</p>
     *
     * @param id ${businessName}编号
     * @return ${businessName}响应 VO，不存在时 data 字段为 null
     */
    @GetMapping("/get")
    @Operation(summary = "获得${businessName}")
    @Parameter(name = "id", description = "${businessName}编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('${module}:${resource}:query')")
    public CommonResult<${entity}RespVO> get${entity}(@RequestParam("id") Long id) {
        // → service.getXxx(id) 不存在时返回 null，前端需处理 null 展示
        ${entity}DO entity = ${resource}Service.get${entity}(id);
        return success(BeanUtils.toBean(entity, ${entity}RespVO.class));
    }

    /**
     * 【AI生成】获得${businessName}分页
     *
     * <p>支持按名称模糊搜索、按状态筛选、按创建时间范围查询，条件之间为 AND 关系。</p>
     *
     * @param pageReqVO 分页查询参数（继承 PageParam，自动处理 pageNo/pageSize）
     * @return 分页结果，无数据时 total=0 且 list 为空列表
     */
    @GetMapping("/page")
    @Operation(summary = "获得${businessName}分页",
               description = "支持按名称模糊搜索、按状态筛选、按创建时间范围查询，条件之间为 AND 关系")
    @PreAuthorize("@ss.hasPermission('${module}:${resource}:query')")
    public CommonResult<PageResult<${entity}RespVO>> get${entity}Page(@Valid ${entity}PageReqVO pageReqVO) {
        PageResult<${entity}DO> pageResult = ${resource}Service.get${entity}Page(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ${entity}RespVO.class));
    }

    /**
     * 【AI生成】获得${businessName}列表
     *
     * <p>获取${businessName}列表（无分页，适用于下拉选择器等场景）。</p>
     *
     * @param reqVO 列表查询参数
     * @return ${businessName}列表，无数据时为空列表
     */
    @GetMapping("/list")
    @Operation(summary = "获得${businessName}列表",
               description = "获取${businessName}列表（无分页，适用于下拉选择器等场景，数据量大时建议改为分页）")
    @PreAuthorize("@ss.hasPermission('${module}:${resource}:query')")
    public CommonResult<List<${entity}RespVO>> get${entity}List(${entity}ListReqVO reqVO) {
        List<${entity}DO> list = ${resource}Service.get${entity}List(reqVO);
        return success(BeanUtils.toBean(list, ${entity}RespVO.class));
    }

    /**
     * 【AI生成】导出${businessName} Excel
     *
     * <p>按查询条件导出${businessName}列表为 Excel 文件。</p>
     *
     * @param pageReqVO 导出筛选条件
     * @param response  HTTP 响应，用于写出 Excel 文件流
     */
    @GetMapping("/export-excel")
    @Operation(summary = "导出${businessName} Excel")
    @PreAuthorize("@ss.hasPermission('${module}:${resource}:export')")
    @ApiAccessLog(operateType = EXPORT)
    @Parameters({
        @Parameter(name = "name", description = "名称（模糊搜索）", example = "峰时"),
        @Parameter(name = "status", description = "状态 0=启用 1=停用", example = "0")
    })
    public void export${entity}Excel(@Valid ${entity}PageReqVO pageReqVO,
                                    HttpServletResponse response) throws IOException {
        // → 设置不分页，一次性导出所有匹配数据（大数据量时考虑分批导出）
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<${entity}DO> list = ${resource}Service.get${entity}Page(pageReqVO).getList();
        ExcelUtils.write(response, "${businessName}.xls", "${businessName}", ${entity}RespVO.class,
                BeanUtils.toBean(list, ${entity}RespVO.class));
    }

}
