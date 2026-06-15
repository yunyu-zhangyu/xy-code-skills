<!-- 精简版，完整版见：D:\ZHYwork\Skills\xy-cloud-skills\开发参考手册\Swagger注解规范参考.md -->

# Swagger注解规范速查

> 用于 AI 代码生成时参考 | OpenAPI 3 | 包：`io.swagger.v3.oas.annotations.*`

## 1. 各层使用的 Swagger 注解

| 文件 | 必须的注解 | 包路径 |
|------|-----------|--------|
| **Controller** | `@Tag(name="管理后台 - {业务名}")` 类级别 | `tags.Tag` |
| | `@Operation(summary=...)` 每个方法 | `Operation` |
| | `@Parameter` 单个 @RequestParam 参数 | `Parameter` |
| **VO(SaveReqVO/RespVO等)** | `@Schema(description=..., example=...)` 每个字段 | `media.Schema` |
| **FeignApi** | `@Operation`（可选） | `Operation` |
| **DO** | 不必 @Schema，可用 `//` 注释 | — |

## 2. 包导入警告

```java
// ❌ 错误（Swagger 2，已废弃）
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

// ✅ 正确（OpenAPI 3，当前使用）
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Hidden;
```

## 3. @Schema 字段示例值规范

| 字段类型 | 推荐 example |
|----------|-------------|
| Long/id | `"1024"`, `"2048"` |
| String/名称 | `"峰时电价"`, `"张三"` |
| Integer/状态 | `"0"`, `"1"` |
| BigDecimal/金额 | `"1.05"`, `"100.00"` |
| LocalDateTime | `"2026-06-01 12:00:00"` |

## 4. @Schema description 写法

```java
// ❌ 差：字段名已自解释
@Schema(description = "价格名称")

// ✅ 好：说明格式/单位/枚举含义
@Schema(description = "价格名称（支持模糊搜索）")
@Schema(description = "价格值（元/kWh，保留两位小数）")
@Schema(description = "状态 0=启用 1=停用 2=冻结")
```

## 5. 注解速查对照（Swagger 2 → OpenAPI 3）

| 功能 | Swagger 2（废弃） | OpenAPI 3（当前） |
|------|-------------------|-------------------|
| API 分组 | `@Api` | `@Tag` |
| 接口描述 | `@ApiOperation` | `@Operation` |
| 参数说明 | `@ApiParam` | `@Parameter` |
| 模型描述 | `@ApiModel` | `@Schema`（注解类或字段） |
| 模型属性 | `@ApiModelProperty` | `@Schema` |
| 隐藏 API | `@ApiIgnore` | `@Hidden` |
