// === 在 ${entity}RespVO.java 的字段上添加 @Excel 注解 ===
// 示例：
// @Excel(name = "名称", order = 1)
// private String name;
//
// @Excel(name = "状态", order = 2, readConverterExp = "0=禁用,1=启用")
// private Integer status;
//
// @Excel(name = "创建时间", order = 3, format = "yyyy-MM-dd HH:mm:ss")
// private LocalDateTime createTime;

// === 在 ${entity}Controller.java 中添加导出方法 ===
/**
 * 导出 ${businessName} Excel
 */
@GetMapping("/export-excel")
@PreAuthorize("@ss.hasPermission('${module}:${resource}:export')")
public void export${entity}(${entity}PageReqVO pageReqVO, HttpServletResponse response) {
    pageReqVO.setPageSize(-1); // 不分页，导出全部
    List<${entity}DO> list = ${resource}Service.get${entity}List(pageReqVO);
    ExcelUtils.write(response, "${businessName}.xls", "数据", ${entity}RespVO.class,
            BeanUtils.toBean(list, ${entity}RespVO.class));
}
