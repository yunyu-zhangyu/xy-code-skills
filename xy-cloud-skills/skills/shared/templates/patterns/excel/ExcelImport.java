// === 新建 ${entity}ImportExcelVO.java ===
// 放在 controller/admin/${domain}/vo/ 目录下
//
// @Data
// public class ${entity}ImportExcelVO {
//     @Excel(name = "名称")
//     private String name;
//
//     @Excel(name = "状态", readConverterExp = "0=禁用,1=启用")
//     private Integer status;
// }

// === 在 ${entity}Controller.java 中添加导入方法 ===
/**
 * 导入 ${businessName} Excel
 */
@PostMapping("/import-excel")
@PreAuthorize("@ss.hasPermission('${module}:${resource}:import')")
public CommonResult<String> import${entity}(@RequestParam("file") MultipartFile file) throws Exception {
    List<${entity}ImportExcelVO> importList = ExcelUtils.read(file, ${entity}ImportExcelVO.class);
    int successCount = ${resource}Service.import${entity}List(importList);
    return CommonResult.success("导入成功，共处理 " + successCount + " 条记录");
}

// === 在 ${entity}ServiceImpl.java 中添加批量导入方法 ===
// @Override
// @Transactional(rollbackFor = Exception.class)
// public int import${entity}List(List<${entity}ImportExcelVO> importList) {
//     List<${entity}DO> entityList = BeanUtils.toBean(importList, ${entity}DO.class);
//     int count = 0;
//     for (${entity}DO entity : entityList) {
//         ${resource}Mapper.insert(entity);
//         count++;
//     }
//     return count;
// }
