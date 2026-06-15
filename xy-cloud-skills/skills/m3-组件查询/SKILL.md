---
name: m3-组件查询
description: 根据功能描述在组件报告和项目源码中查找可复用组件，输出组件推荐清单。
trigger: 用户说"查组件"、"有没有XX组件"、"找可复用的XXX"时激活。
inputs:
  - ${SKILLS_ROOT}/shared/outputs/m1-需求/req-*.md
  - ${SKILLS_ROOT}/shared/outputs/m2-数据模型/dm-*.md
  - ${SKILLS_ROOT}/shared/references/MANIFEST.md → components-index, code-standards, security-auth, cache-usage, api-integration
outputs:
  - ${SKILLS_ROOT}/shared/outputs/m3-组件/cq-{tag}-{module}-components.md
  - ${SKILLS_ROOT}/m3-组件查询/outputs/cq-{tag}-{module}-components-{timestamp}.md
tools:
  - bash: cat, ls, grep, date
  - ask_user
params:
  SKILLS_ROOT: "$(cd \"$(dirname \"$0\")\" && pwd)"
# 启动时自动读 MANIFEST.md + 检测知识包
---

# M3: 组件查询 (Component Query)

## 启动 — 加载知识包

```bash
# 1. 读取手写规范索引（通用）
cat "${SKILLS_ROOT}/shared/references/MANIFEST.md" 2>/dev/null || true

# 2. 检测项目知识包
PROJECT_LIST=$(ls -d "${SKILLS_ROOT}/knowledge/"*/ 2>/dev/null | xargs -n1 basename 2>/dev/null)
PROJECT_COUNT=$(echo "${PROJECT_LIST}" | wc -l)
if [ "${PROJECT_COUNT}" -eq 1 ]; then
  PROJECT_NAME="${PROJECT_LIST}"
elif [ "${PROJECT_COUNT}" -gt 1 ]; then
  echo "检测到多个项目知识包："
  echo "${PROJECT_LIST}"
  echo "请输入当前项目名称："
  read PROJECT_NAME
else
  PROJECT_NAME=""
fi

if [ -n "${PROJECT_NAME}" ] && [ -f "${SKILLS_ROOT}/knowledge/${PROJECT_NAME}/references/MANIFEST.md" ]; then
  BASE_PACKAGE=$(grep "^| base-package" "${SKILLS_ROOT}/knowledge/${PROJECT_NAME}/references/MANIFEST.md" | awk -F'|' '{print $3}' | xargs)
  KNOWLEDGE_AVAILABLE=true
else
  KNOWLEDGE_AVAILABLE=false
  echo "[提示] 未检测到项目知识包，建议先运行 M0。"
fi
```

## 流程

0. 读取参考文档（询问用户或读取默认文件夹）
   - 有参考文档 → 直接匹配组件，跳至步骤 5
   - 无参考文档 → 继续步骤 1
1. Q1: 需要什么类型的组件？（CRUD / 导入导出 / 权限 / 缓存 / 消息 / 文件 / 工具类）
2. Q2: 是否涉及多表关联查询？
   - 若上游 M1 输出（req-*.md）可用，自动读取其 `多表关联查询` 章节的标记，**跳过此问题**
   - 无上游输入时提问：
     - 单表 → `LambdaQueryWrapperX`（标准查询构造器）
     - 多表 Join → `MPJLambdaWrapperX`（mybatis-plus-join 多表查询）
3. Q3: 是否需要防护能力？
   - 幂等控制 → `@Idempotent`
   - 接口限流 → `@RateLimiter`
   - API 签名 → `@ApiSignature`
4. 按两层回退查找（跳过组件报告，因已在步骤 0 查阅）
5. 判断搜索结果
   - 有匹配组件 → 步骤 6 生成前确认
   - 无匹配组件 → Q4 追问（可循环重试最多 2 次）
6. 生成前确认（汇总清单让用户确认）
7. 输出组件推荐清单

## 步骤 0：读取参考文档

优先通过文档获取组件信息，只有在无文档可用时才搜索源码。

### 方式 A：用户提供参考文档

> 是否有需求/设计文档、接口文档或组件参考文档？
>
> 提供文件路径后，自动读取并提取组件使用信息。

### 方式 B：读取默认参考文件夹

若用户未提供文档，按以下步骤读取 `shared/references/` 下的参考文档：

1. **读取文档清单**

   ```bash
   cat "${SKILLS_ROOT}/shared/references/MANIFEST.md"
   ```

2. **读取所需文档**：从第 1 步输出中找到以下逻辑名称对应的「文件名」，然后用 `cat` 读取

   | 逻辑名称 | 用途 |
   |----------|------|
   | `components-index` | 组件索引 |
   | `code-standards` | 代码规范（含组件映射信息） |
   | `security-auth` | 安全认证规范（权限/认证组件） |
   | `cache-usage` | 缓存使用规范（缓存组件） |
   | `api-integration` | API 集成规范（Feign/MQ 组件） |

3. **文件缺失处理**：如果第 2 步任何 `cat` 报错文件不存在，说明文件已被重命名。执行：

   ```bash
   ls "${SKILLS_ROOT}/shared/references/"*.md | grep -v MANIFEST.md
   ```

   逐文件读取 H1 标题推断其内容，对照原逻辑名称重建「逻辑名 → 文件名」映射，并提示用户更新 MANIFEST.md。

### 方式 C：读取上游 M1/M2 输出

若上游 M1 需求规格文件（`shared/outputs/m1-需求/req-${tag}*.md`）可用，自动读取其中的 `多表关联查询` 和组件相关信息。

若上游 M2 数据模型文件（`shared/outputs/m2-数据模型/dm-${tag}-*.md`）可用，遍历读取所有 dm 文件，从 `DO 继承` 字段提取 `TenantBaseDO` / `BaseDO` 决策，据此推荐对应的租户组件或基础组件。

### 判断逻辑

- **文档匹配** → 直接从文档中提取所需组件，跳转至步骤 5（判断搜索结果）
- **文档不匹配或无文档** → 继续步骤 1（提问）+ 步骤 4（源码搜索）

**上游 Schema 校验**：若读取了 req-*.md 或 dm-*.md，校验必要字段：
```bash
head -20 "$REQ_FILE" | grep -q "^tag:" || echo "[警告] 缺少 tag 字段"
head -20 "$REQ_FILE" | grep -q "^multi_table:" || { echo "[提示] 前置元数据未找到 multi_table 字段，尝试从正文读取"; grep -q "多表关联查询" "$REQ_FILE" || echo "[警告] 正文也未找到多表关联信息，需手动判断单表/多表查询"; }
head -20 "$DM_FILE" | grep -q "TenantBaseDO\|BaseDO" || echo "[提示] 未找到 DO 继承信息，需手动确认"
```

## 组件分类索引

| 分类 | 关键组件 | 说明 |
|------|----------|------|
| **通用基础** | CommonResult, PageResult, PageParam, BaseDO, TenantBaseDO, BaseMapperX | 所有模块必须 |
| **对象转换** | BeanUtils | DO↔VO↔DTO |
| **集合操作** | CollectionUtils | convertMap/convertList/filterList |
| **查询构造** | LambdaQueryWrapperX, MPJLambdaWrapperX | 单表/多表 |
| **安全** | @PreAuthorize, SecurityFrameworkUtils | 权限控制 |
| **防护** | @Idempotent, @RateLimiter | 幂等/限流 |
| **缓存** | CacheUtils, @Cacheable, @CacheEvict, RedisKeyConstants | Guava/Redis 缓存 |
| **消息** | RedisMQTemplate, AbstractRedisChannelMessage, AbstractRedisStreamMessage | MQ 消息 |
| **基础设施** | ConfigApi, FileApi, WebSocketSenderApi | 远程调用 |
| **工具类** | JsonUtils, ServletUtils, StrUtils, LocalDateTimeUtils | 通用工具 |

### 组件推荐规则

- **多租户数据 → 必须推荐 `TenantBaseDO`**：当需求涉及按租户隔离的数据时（部门/用户/角色等），自动推荐 `TenantBaseDO` 作为 DO 父类。⚠️ 选错为 `BaseDO` 会导致租户间数据互相可见。
- **JSON 字段 → 必须提醒 `autoResultMap = true`**：当 DO 包含 `@TableField(typeHandler = JacksonTypeHandler.class)` 时，必须提醒 `@TableName` 加上 `autoResultMap = true`。

## 两步回退查找

文档阅读步骤未匹配到组件时，按以下两层回退查找：

### 第 1 层：项目源码

按范围选择搜索路径（先读取知识包中的项目结构速查.md 获取模块路径）：

```bash
# 从知识包读取模块路径映射表
FRAMEWORK_PATH=$(grep "类型.*框架" "${SKILLS_ROOT}/knowledge/${PROJECT_NAME}/references/项目结构速查.md" -A1 2>/dev/null | grep -oP 'D:\\[^|]+' | head -1 || echo "")
if [ -n "${FRAMEWORK_PATH}" ]; then
  # 有知识包：用精确路径搜索
  grep -r -l "关键字" "${FRAMEWORK_PATH}" --include="*.java" | head -10
else
  # 无知识包：询问用户
  echo "未检测到知识包（项目结构速查.md），请提供框架模块的源码目录路径："
  read FRAMEWORK_PATH
  grep -r -l "关键字" "${FRAMEWORK_PATH}" --include="*.java" | head -10
fi
```

### 第 2 层：通用开源方案
当项目内无匹配时，推荐通用方案（Easy-Excel、Hutool 等）。

## Q4: 未找到匹配组件

当两层搜索均未返回结果时，进入追问循环（最多重试 2 轮）：

### Q4a：换关键词重试

**提示用户**："未在项目中发现匹配的组件。能否更详细描述所需功能？或提供其他关键词重新搜索？"

- 用户提供新关键词 → 回到**第 1 层**重新搜索（重试计数 +1）
- 用户放弃 → 进入 Q4b

### Q4b：搜索其他模块

**提示用户**："是否需要检查其他业务模块？"（从 `${SKILLS_ROOT}/knowledge/${PROJECT_NAME}/references/项目结构速查.md` §1 读取当前可用模块列表呈现给用户）

- 用户指定模块 → 在**第 2 层**中重点搜索该模块源码
- 用户否 → 进入 Q4c

### Q4c：确认后续行动

**提示用户**："项目中没有找到匹配的组件。是否需要：
1. 输出**新建组件建议**（接口定义、实现类、MyBatis-Plus 注册等）
2. 推荐**第三方开源方案**替代
3. **结束**，不输出组件推荐"

根据用户选择执行。

## 步骤 6：生成前确认

在写入输出文件前，汇总推荐组件清单并向用户确认：

```
===== 组件推荐确认 =====

【匹配组件】
  1. {组件名} — {用途} — {来源: 文档/源码/开源}

【是否满足需求？】
  [Y] 确认，生成组件推荐清单
  [N] 需要补充或调整
========================
```

用户确认后写入输出文件。

## 查询技巧

- **精确搜索类名**: 直接搜索类名（如 `LambdaQueryWrapperX`）
- **搜索注解使用**: 搜索 `@Idempotent`、`@RateLimiter` 查看使用示例
- **搜索 Feign 调用**: 在 `platform-module-*-api/` 中搜索 Feign 接口定义
- **搜索枚举**: 在 `enums/` 目录下搜索枚举定义

## 输出

### 1. 写入本地归档（存档轨）
将完整内容写入 `${SKILLS_ROOT}/m3-组件查询/outputs/cq-{tag}-{module}-components-{timestamp}.md`
> `{timestamp}` 通过 `date +%Y%m%d-%H%M` 获取

### 2. 同步到流水线总线（总线轨）
将完整内容写入 `${SKILLS_ROOT}/shared/outputs/m3-组件/cq-{tag}-{module}-components.md`

> `{tag}` 从上游输入获取（如 req-*.md 文件），无上游输入时询问用户。
- 按 CLAUDE.md 规范更新 `shared/outputs/STATUS.md`

格式：
```markdown
---
tag: {tag}
type: component
module: {module}
version: v1
created: {current-date}
dev_type: {dev_type}
patterns: [{pattern-list}]
---

# [组件推荐] {module}

## 基础组件

| 序号 | 组件 | 包路径 | 用途 | 来源 | 优先级 |
|------|------|--------|------|------|--------|
| 1 | Xxx | cn.shutan.... | 说明 | 文档/源码/开源 | 推荐 |

## 模式组件

（若上游 M1 设定了 patterns，则为每个 pattern 单独输出本节）

### {pattern-name}（如 excel-export）

| 组件 | 包路径 | 用途 | 参考源码 |
|------|--------|------|---------|
| ExcelUtils | ... | Excel 写入 | {Controller}.java L156 |
| @Excel | ... | 导出字段标记 | {RespVO}.java L23 |

#### M4 生成指令
- 需额外生成的文件: {Entity}RespVO.java（新增 @Excel 注解）、{Entity}Controller.java（新增 exportExcel 方法）
- 参考代码片段:

```java
// 来自 {source-controller}.java
@GetMapping("/export-excel")
public void exportExcel(@Validated PageReqVO reqVO, HttpServletResponse response) {
    List<EntityDO> list = service.getEntityList(reqVO);
    ExcelUtils.write(response, "{业务名}.xls", "数据", EntityRespVO.class, list);
}
```

## 使用示例
{代码片段}

### component-starter 专用节

（仅当 dev_type = `component-starter` 时输出本节）

#### 需新建的文件

| # | 文件 | 包路径 | 参考模板 | 参考源码路径 |
|---|------|--------|---------|------------|
| 1 | OSSFileClient.java | framework/file/core/client/oss/ | S3FileClient.java | .../S3FileClient.java |

#### 需修改的已有文件

| # | 文件 | 项目路径 | 操作 | 新增代码 |
|---|------|---------|------|---------|
| 1 | FileStorageEnum.java | framework/file/core/enums/FileStorageEnum.java | 在 S3 行后新增枚举值 | `OSS(21, OSSFileClientConfig.class, OSSFileClient.class),` |
| 2 | pom.xml | platform-module-infra-biz/pom.xml | 在 aws-java-sdk-s3 后新增依赖 | `<dependency>...aliyun-sdk-oss...</dependency>` |

> M4 根据本节生成文件：需新建→生成新文件；需修改→读取项目原始文件+注入标记注释+新增代码后完整输出。

```

### 未找到组件时的输出（Q4c 用户选择结束）

```markdown
---
tag: {tag}
type: component
module: {module}
version: v1
created: {current-date}
---

# [组件推荐] {module}

> 未在项目和组件库中找到匹配组件。
> 建议：{用户选择的后续行动}
```
