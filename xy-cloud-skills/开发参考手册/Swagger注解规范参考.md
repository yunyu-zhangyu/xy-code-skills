# xy-cloud Swagger/OpenAPI 注解规范参考

> **来源**: xy-cloud 项目源码（`platform-spring-boot-starter-web`、实际 Controller/VO 代码）
>
> **技术栈**: SpringDoc OpenAPI 3 (springdoc-openapi) + Swagger 注解 (`io.swagger.v3.oas.annotations`)
>
> **说明**: 本文档是 AI 生成代码时 Swagger 注解的完整参考。xy-cloud 使用 OpenAPI 3 标准（`io.swagger.v3.oas.annotations.*` 包），非 Swagger 2 (`io.swagger.annotations.*`)。

---

## 1. 注解使用全景

| 注解 | 使用位置 | 作用 | Swagger UI 效果 |
|------|---------|------|-----------------|
| `@Tag` | Controller 类 | API 分组 | 接口列表的分组标题 |
| `@Operation` | Controller 方法 | 接口描述 | 接口的标题+详情 |
| `@Parameter` | Controller 方法参数 | 参数说明 | 接口参数的描述 |
| `@Schema` | VO/DO/DTO 字段、方法返回值 | 模型字段描述 | 请求/响应体的字段说明 |
| `@ApiResponse` | Controller 方法 | 响应说明 | 接口的可能响应 |
| `@Hidden` | 方法或类 | 隐藏 API | 不在 Swagger UI 显示 |
| `@Content` | 与 `@ApiResponse` 配合 | 响应内容类型 | 响应体的 MediaType |

---

## 2. @Tag — 类级别分组

### 2.1 使用位置

所有 Controller 类必须标注 `@Tag`。

### 2.2 格式

```java
@Tag(name = "管理后台 - {业务名}")
// 或
@Tag(name = "管理后台 - {业务名}", description = "可选：更长说明")
```

### 2.3 命名规则

| Controller 位置 | 前缀 | 示例 |
|-----------------|------|------|
| `controller/admin/` | `管理后台 - ` | `@Tag(name = "管理后台 - 能源价格")` |
| `controller/app/` | `APP 端 - ` | `@Tag(name = "APP 端 - 能源价格")` |
| 内部 Feign 实现（不加 @Tag） | — | 不标注，避免 Swagger 出现重复分组 |

### 2.4 示例

```java
@Tag(name = "管理后台 - 能源价格")
@RestController
@RequestMapping("/eos/energyprice")
@Validated
public class EnergyPriceController { ... }
```

### 2.5 常见错误

```java
// ❌ 错误：使用了 Swagger 2 的 @Api
@Api(tags = "能源价格")

// ❌ 错误：不写 name，只写 description
@Tag(description = "能源价格管理")  // Swagger UI 中显示为 "能源价格管理（default）"

// ✅ 正确
@Tag(name = "管理后台 - 能源价格")
```

---

## 3. @Operation — 方法级别接口描述

### 3.1 使用位置

Controller 中**每个公开方法**必须标注 `@Operation`。

### 3.2 属性说明

| 属性 | 必填 | 说明 | 示例 |
|------|------|------|------|
| `summary` | **是** | 短标题，Swagger UI 显示为接口名字 | `"创建能源价格"` |
| `description` | 推荐 | 详细说明，支持 HTML | `"支持按名称模糊搜索、按状态筛选"` |
| `deprecated` | 按需 | 标记已弃用 | 加 `@Deprecated` 注解一起使用 |
| `method` | 否 | 请求方法（已在 Mapping 注解声明） | 通常不写 |
| `tags` | 否 | 覆盖 @Tag | 通常不写（使用类级别的 @Tag） |

### 3.3 summary 命名规范

| 请求方法 | summary 格式 | 示例 |
|---------|-------------|------|
| `POST create` | `创建{businessName}` | `"创建能源价格"` |
| `PUT update` | `更新{businessName}` | `"更新能源价格"` |
| `DELETE delete` | `删除{businessName}` | `"删除能源价格"` |
| `GET get` | `获得{businessName}` | `"获得能源价格"` |
| `GET list` | `获得{businessName}列表` | `"获得能源价格列表"` |
| `GET page` | `获得{businessName}分页` | `"获得能源价格分页"` |
| `GET export` | `导出{businessName} Excel` | `"导出能源价格 Excel"` |

### 3.4 description 使用时机

以下场景**必须**写 `description`：

```java
// ✅ 复杂的查询接口需要 description 说明参数组合逻辑
@Operation(
    summary = "获得能源价格分页",
    description = "支持按名称模糊搜索（name）、按状态筛选（status）、按创建时间范围查询（createTime），条件之间为 AND 关系"
)

// ✅ 有前置条件或副作用的操作
@Operation(
    summary = "删除能源价格",
    description = "逻辑删除（deleted=1），已绑定的设备价格将使用默认阶梯价格替代"
)
```

### 3.5 简单 CRUD 可以省略 description

```java
// ✅ 简单 CRUD 只需要 summary
@Operation(summary = "创建能源价格")
@Operation(summary = "更新能源价格")
```

---

## 4. @Parameter — 参数级说明

### 4.1 使用位置

- `@RequestParam` 参数（如 id）
- `@PathVariable` 参数
- Controller 方法参数上

### 4.2 常见使用模式

```java
// 单个参数
@GetMapping("/get")
@Operation(summary = "获得能源价格")
@Parameter(name = "id", description = "能源价格编号", required = true, example = "1024")
public CommonResult<EnergyPriceRespVO> getEnergyPrice(@RequestParam("id") Long id) { ... }

// 多个参数需要分别标注
@DeleteMapping("delete")
@Operation(summary = "删除能源价格")
@Parameters({
    @Parameter(name = "id", description = "能源价格编号", required = true),
    @Parameter(name = "type", description = "删除类型 1=物理删除 2=逻辑删除", example = "1")
})
public CommonResult<Boolean> deleteEnergyPrice(
        @RequestParam("id") Long id,
        @RequestParam("type") Integer type) { ... }
```

### 4.3 什么时候可以省略 @Parameter

| 场景 | 是否必须 @Parameter |
|------|-------------------|
| 参数是 VO 对象（`@RequestBody` / `PageReqVO`） | 不必，VO 字段用 `@Schema` 描述 |
| 参数是 `@RequestParam` 的单个 id/主键 | **必须** |
| 参数是 `@PathVariable` | **必须** |
| 参数是 `HttpServletResponse` / `HttpServletRequest` | 不必，框架内置 |

---

## 5. @Schema — 模型字段级注解（最重要的注解）

### 5.1 使用位置

**所有 VO（SaveReqVO / PageReqVO / ListReqVO / RespVO）** 的每个字段**必须**标注 `@Schema`。

DO 实体字段可以使用 `//` 注释（不直接暴露给 API 调用方时）。

### 5.2 属性速查

| 属性 | 必填 | 说明 | 示例 |
|------|------|------|------|
| `description` | **是** | 字段中文说明 | `"价格名称"` |
| `requiredMode` | 按需 | `Schema.RequiredMode.REQUIRED` 标注必填 | 创建接口必填字段 |
| `example` | **推荐** | 示例值，Swagger UI 自动填充 | `"峰时电价"` |
| `hidden` | 按需 | `true` 时该字段不显示在 Swagger UI | 内部使用字段 |
| `implementation` | 按需 | 覆盖类型推断 | 复杂泛型时使用 |
| `allowableValues` | 按需 | 可选值列表 | `{"0", "1", "2"}` |

### 5.3 各层 VO 的 @Schema 参照

#### 5.3.1 SaveReqVO（创建/更新请求）

```java
@Schema(description = "管理后台 - 能源价格 新增/修改 Request VO")
public class EnergyPriceSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;  // 创建时不传（null），更新时必传

    @Schema(description = "价格名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "峰时电价")
    @NotBlank(message = "价格名称不能为空")
    private String name;

    @Schema(description = "价格值（元/kWh）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1.05")
    @NotNull(message = "价格值不能为空")
    @DecimalMin(value = "0", message = "价格值必须大于等于 0")
    private BigDecimal price;

    @Schema(description = "状态 0=启用 1=停用", example = "0")
    private Integer status;
}
```

#### 5.3.2 PageReqVO（分页请求）

```java
@Schema(description = "管理后台 - 能源价格 分页 Request VO")
public class EnergyPricePageReqVO extends PageParam {

    @Schema(description = "价格名称（模糊搜索）", example = "峰时")
    private String name;

    @Schema(description = "状态 0=启用 1=停用", example = "0")
    private Integer status;

    @Schema(description = "创建时间范围（左闭右开）", example = "[2026-01-01 00:00:00, 2026-12-31 23:59:59]")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] createTime;
}
```

> **注意**: 分页参数（pageNo、pageSize）已在 `PageParam` 父类中定义，子类 VO **不需要重复声明** `@Schema`。

#### 5.3.3 RespVO（响应）

```java
@Schema(description = "管理后台 - 能源价格 响应 VO")
public class EnergyPriceRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "价格名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "峰时电价")
    private String name;

    @Schema(description = "价格值（元/kWh）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1.05")
    private BigDecimal price;

    @Schema(description = "状态 0=启用 1=停用", example = "0")
    private Integer status;

    @Schema(description = "创建时间", example = "2026-06-01 12:00:00")
    private LocalDateTime createTime;
}
```

### 5.4 description 的最佳写法

```java
// ❌ 差：冗余，字段名已经说了
@Schema(description = "价格名称")

// ✅ 好：说明格式/单位/枚举含义
@Schema(description = "价格名称（支持模糊搜索）")
@Schema(description = "价格值（元/kWh，保留两位小数）")
@Schema(description = "状态 0=启用 1=停用 2=冻结")
@Schema(description = "生效时间（UTC+8，格式 yyyy-MM-dd HH:mm:ss）")

// ❌ 差：无 example，Swagger UI 需要手动输入
@Schema(description = "价格值")

// ✅ 好：有 example，Swagger UI 一键填充
@Schema(description = "价格值（元/kWh）", example = "1.05")
```

### 5.5 example 的选择规范

| 字段类型 | 推荐的 example |
|----------|---------------|
| Long/id | `1024`, `2048`, `1` |
| String/名称 | `"赵六"`, `"峰时电价"`, `"测试数据"` |
| Integer/状态 | `0`, `1` |
| BigDecimal/金额 | `"1.05"`, `"100.00"` |
| LocalDateTime | `"2026-06-01 12:00:00"` |
| Boolean | `true` |
| 数组 | `["a", "b"]` |

---

## 6. @ApiResponse — 响应说明

### 6.1 使用场景

以下场景推荐使用 `@ApiResponse`：

```java
@Operation(summary = "创建能源价格")
@ApiResponse(responseCode = "200", description = "返回新创建的能源价格 ID")
@ApiResponse(responseCode = "400", description = "参数校验失败，具体错误见 CommonResult.msg")
@ApiResponse(responseCode = "403", description = "权限不足")
public CommonResult<Long> createEnergyPrice(...) { ... }
```

### 6.2 简化规则

xy-cloud 项目使用 `CommonResult<T>` 统一响应，**不是每个方法都需要 @ApiResponse**。仅以下特殊场景需要：

| 场景 | 必须 @ApiResponse |
|------|-------------------|
| 返回非标准 HTTP 状态码 | 是（如 201 Created、204 No Content） |
| 特殊错误码需要文档说明 | 是（如 `@ApiResponse(responseCode = "200", description = "业务异常: ENERGY_PRICE_NOT_FOUND")`） |
| 标准 CRUD（200+CommonResult） | 不必，Swagger 自动推断 |

---

## 7. @Hidden — 隐藏 API

### 7.1 使用场景

| 场景 | 说明 |
|------|------|
| 内部 Feign 实现 | Feign 接口的 Controller 实现不应出现在 Swagger 中 |
| 管理用端点 | 如健康检查、内部回调接口 |
| 已废弃且不愿删除 | 配合 `@Deprecated` |

### 7.2 示例

```java
@Hidden // 该类所有接口不在 Swagger UI 显示
@RestController
@RequestMapping(RpcConstants.RPC_API_PREFIX + "/eos/energyprice")
public class EnergyPriceApiImpl implements EnergyPriceApi { ... }

// 或隐藏单个方法
@Hidden
@GetMapping("/internal/callback")
public String internalCallback(...) { ... }
```

---

## 8. 各层文件 Swagger 注解速查

| 文件 | 必须的 Swagger 注解 | 可选/不需要 |
|------|-------------------|------------|
| **Controller.java** | `@Tag(name=...)` 类级别 | `@ApiResponse`（标准 CRUD 省略） |
| | `@Operation(summary=...)` 每个方法 | |
| | `@Parameter` 单个参数 | |
| **SaveReqVO.java** | `@Schema(description=...)` 每个字段 | |
| **PageReqVO.java** | `@Schema(description=...)` 查询条件字段 | 分页字段（继承自 PageParam）不需要 |
| **RespVO.java** | `@Schema(description=...)` 每个字段 | |
| **DO.java** | 不必（不直接暴露给 API） | 可用 `//` 行内注释替代 |
| **Service 接口** | 不必（不是 Swagger 边界） | 使用 JavaDoc |
| **FeignApi.java** | `@Operation`（可选） | Feign 接口通常不带 @Tag |
| **DTO.java** | `@Schema(description=...)` 每个字段 | |

---

## 9. 常见错误示例

```java
// ❌ 1. 导错包（用了 Swagger 2 的 @Api 而不是 OpenAPI 3 的 @Tag）
import io.swagger.annotations.Api;           // 旧版 ❌
import io.swagger.v3.oas.annotations.tags.Tag;  // 新版 ✅

// ❌ 2. @Tag 不写 name
@Tag(description = "能源价格管理")
// Swagger UI 中显示为 "能源价格管理（default）" ❌

// ❌ 3. @Operation 不写 summary
@Operation(description = "创建一个新的能源价格")
// Swagger UI 显示方法路径而非标题 ❌

// ❌ 4. VO 字段缺少 @Schema
private String name;

// ❌ 5. @Schema 不写 example
@Schema(description = "价格名称")
private String name;
// Swagger UI 中该字段的示例值为空，调试时需要手动输入 ❌

// ❌ 6. 用 @ApiModel 代替 @Schema（Swagger 2 写法）
@ApiModel(value = "能源价格")
@ApiModelProperty(value = "价格名称")

// ✅ 正确
@Schema(description = "管理后台 - 能源价格 响应 VO")
@Schema(description = "价格名称", example = "峰时电价")
```

---

## 10. AI 生成 Swagger 注解检查清单

AI 完成每个 Controller 和 VO 文件后，必须检查以下 6 项：

| # | 检查项 | 通过标准 |
|---|--------|---------|
| 1 | import 包正确 | 全部使用 `io.swagger.v3.oas.annotations.*` |
| 2 | Controller @Tag | 类上有 `@Tag(name = "管理后台 - {业务名}")` |
| 3 | 方法 @Operation | 每个公开方法有 `@Operation(summary = "...")` |
| 4 | 参数 @Parameter | id 等 `@RequestParam` 参数标注了 `@Parameter` |
| 5 | VO 字段 @Schema | 每个字段有 `@Schema(description = "...")` |
| 6 | example 填写 | 每个 `@Schema` 有合理的 `example` 值 |

---

## 附录 A: Swagger 2 vs OpenAPI 3 注解对照

| 功能 | Swagger 2（已废弃） | OpenAPI 3（当前使用） | 包路径 |
|------|--------------------|---------------------|--------|
| API 分组 | `@Api` | `@Tag` | `io.swagger.v3.oas.annotations.tags.Tag` |
| 接口描述 | `@ApiOperation` | `@Operation` | `io.swagger.v3.oas.annotations.Operation` |
| 参数说明 | `@ApiParam` | `@Parameter` | `io.swagger.v3.oas.annotations.Parameter` |
| 模型描述 | `@ApiModel` | `@Schema` | `io.swagger.v3.oas.annotations.media.Schema` |
| 模型属性 | `@ApiModelProperty` | `@Schema`（共用） | 同上 |
| 响应说明 | `@ApiResponse` | `@ApiResponse` | `io.swagger.v3.oas.annotations.responses.ApiResponse` |
| 隐藏 API | `@ApiIgnore` | `@Hidden` | `io.swagger.v3.oas.annotations.Hidden` |

> **特别注意**: `@Schema` 同时替代了 Swagger 2 的 `@ApiModel`（类级别）和 `@ApiModelProperty`（字段级别）。
> DO NOT 将 Swagger 2 和 OpenAPI 3 注解混用 — 项目已经统一使用 OpenAPI 3。

## 附录 B: Swagger UI 调试建议

1. **全局参数**: Swagger UI 右上角的 `Authorize` 按钮可以设置 Bearer Token，避免每个请求手动输入
2. **Try it out**: 使用 `@Schema(example = "...")` 提供的示例值一键填充请求体
3. **枚举字段**: 在 `@Schema(description = "...")` 中直接写枚举含义（如 `"状态 0=启用 1=停用"`），Swagger 不自动解析 Java 枚举
