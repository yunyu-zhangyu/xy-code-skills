---
name: m5-边界分析
description: 分析数据模型中各字段的边界条件，从 VO 注解和业务约束推导 P0/P1/P2 边界值，输出测试边界清单。
trigger: 用户说"边界分析"、"分析测试边界"或在 M2 之后运行。
inputs:
  - ${SKILLS_ROOT}/shared/outputs/m2-数据模型/dm-*.md
  - ${SKILLS_ROOT}/shared/references/MANIFEST.md → test-standards
  - $(cat "${SKILLS_ROOT}/knowledge/${PROJECT_NAME}/references/项目结构速查.md" 2>/dev/null || echo "知识包不存在，需运行时询问用户")
# VO 文件路径在运行时从知识包（项目结构速查.md）获取或询问用户
outputs:
  - ${SKILLS_ROOT}/shared/outputs/m5-边界/bd-{tag}-{entity}.md
  - ${SKILLS_ROOT}/m5-边界分析/outputs/bd-{tag}-{entity}-{timestamp}.md
tools:
  - bash: cat, ls, find, date
  - ask_user
params:
  SKILLS_ROOT: "$(cd \"$(dirname \"$0\")\" && pwd)"
# 启动时自动读 MANIFEST.md + 检测知识包
---

# M5: 边界分析 (Boundary Analysis) v2

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

### Step 1 — 读取参考文档
执行 `cat "${SKILLS_ROOT}/shared/references/MANIFEST.md"` 获得文档清单，从清单中找到 `test-standards` 对应的文件名并 `cat` 读取该文件获取边界分析类型和模板（如果 `cat` 报错文件不存在，则执行 `ls` 扫描目录通过文件 H1 标题推断内容重建映射）

### Step 2 — 读取数据模型
从 `${SKILLS_ROOT}/shared/outputs/m2-数据模型/` 下找 `dm-{tag}-*.md`。

**输入检查：**
```bash
ls -t "${SKILLS_ROOT}/shared/outputs/m2-数据模型/dm-${tag}-"*.md 2>/dev/null | head -3
```

- 有 → 从文件名提取 `{tag}`（`dm-{tag}-{entity}.md`），读取 DM 获取字段定义
- 无 → 让用户描述要测试的实体和字段，并询问 `{tag}` 和 `{entity}`

**上游 Schema 校验**：读取 DM 文件后校验：
```bash
head -20 "$DM_FILE" | grep -q "^tag:" || echo "[警告] 缺少 tag 字段"
head -20 "$DM_FILE" | grep -q "table_name" || echo "[警告] 缺少 table_name 定义"
head -20 "$DM_FILE" | grep -q "字段" || echo "[提示] 未检测到字段定义，边界分析可能不完整"
```

从 DM 中同时提取 `module` 字段（用于定位项目源码中对应的模块目录）。

### Step 3 — 解析 VO 注解（新增）

从 DM 中读取 `entity` 和 `module` 名称后，搜索匹配的 VO 类：

```bash
# 查找 SaveReqVO（增改请求——含最多校验注解）
# 先尝试从知识包获取模块路径
MODULE_PATH=$(grep -A1 "| ${module} |" "${SKILLS_ROOT}/knowledge/${PROJECT_NAME}/references/项目结构速查.md" 2>/dev/null | grep -oP 'D:\\[^|]+' || echo "")
if [ -n "${MODULE_PATH}" ]; then
  # 有知识包：用精确路径搜索
  find "${MODULE_PATH}" -name "*${entity}*VO*.java" -o -name "*${entity}*ReqVO*.java" 2>/dev/null
else
  # 无知识包：询问用户
  echo "未检测到知识包，请提供 ${module} 模块的源码目录路径："
  read MODULE_PATH
  find "${MODULE_PATH}" -name "*${entity}*VO*.java" -o -name "*${entity}*ReqVO*.java" 2>/dev/null
fi
```

读取找到的 `*SaveReqVO.java`（优先）或 `*ReqVO.java`，按以下规则提取字段级注解：

| VO 注解（javax/jakarta） | 映射边界 | 优先级 |
|---|---|---|
| `@NotNull` / `@NotEmpty` / `@NotBlank` | null / 空串 | **P0** |
| `@Size(max=N)` / `@Length(max=N)` | 超长字符串 (N+1字符) | **P0** |
| `@Min(N)` | 低于最小值 (N-1 或负数) | **P0** |
| `@Max(N)` | 超过最大值 (N+1) | **P0** |
| `@Email` | 非法邮件格式 | **P0** |
| `@Pattern(regex=...)` | 不匹配正则的非法值 | **P0** |
| `@Positive` / `@PositiveOrZero` | 负数 | **P0** |
| `@DecimalMin` / `@DecimalMax` | 精度溢出 | **P1** |
| `@Future` / `@Past` | 时间越界 | **P1** |

**一致性检查：** 对比 DM 字段长度与 `@Size(max=N)` 值，不一致时记录 WARNING 到输出的 `warnings` 列表。

### Step 4 — 交互式关联扩展（新增）

分析 DM 时识别以下情况，**通过 AskUserQuestion 向用户提问**：

- **外键字段**：字段名以 `_id` / `Id` 结尾（如 `package_type` 枚举依赖），且值区间暗示引用另一实体
- **枚举/字典字段**：字段注释或 Schema example 标明取值集合（如 `0:免费 1:标准 2:企业 3:定制`）
- **关联注释**：DM "关系"或"索引"章节提及外键关联

**提问模板：**
```
检测到 {entity}.{field}({type}) 取值 {value-range}，
疑似关联 {suggested-entity} 字典/实体。
是否要 (a) 扩展分析 {suggested-entity} 边界
(b) 仅注明关联依赖
(c) 跳过？
```

- 选 (a) → 递归读取关联实体的 DM 并追加分析（同 Step 2）
- 选 (b) → 在边界行的"来源"列标注"依赖 {entity}.{field}"
- 选 (c) → 仅标记"待补充"

### Step 5 — 按 6 种边界类型逐字段分析

对每个字段，遍历以下类型，结合 Step 2（DM）和 Step 3（VO）的信息生成边界记录。每条记录同步推导优先级。

---

## 6 种边界类型 + 优先级推导

### 1. 数值边界
- 适用: Integer, Long, BigDecimal, Double
- **P0**: `@Min`/`@Max` 越界、负数（`@Positive`）、零值（必填）
- **P1**: 精度溢出（BigDecimal scale 超限）
- **P2**: 极大值/极小值（边界震荡）

### 2. 字符串边界
- 适用: String, 枚举 code
- **P0**: null（`@NotNull`/`@NotEmpty`）、空串（`@NotEmpty`）、超长（`@Size(max=N)`）、SQL 注入字符
- **P1**: 邮箱格式错误（`@Email`）、正则不匹配（`@Pattern`）
- **P2**: Unicode 特殊字符、超长边界（N+1 附近取值）

### 3. 时间边界
- 适用: LocalDateTime, LocalDate
- **P0**: null（必填字段）
- **P1**: 未来时间（`@Future`）、过去时间（`@Past`）、时间先后关系颠倒
- **P2**: 格式错误、夏令时/时区边界

### 4. 关联边界
- 适用: 外键/关系字段
- **P0**: 不存在的外键 ID
- **P1**: 已删除的关联记录、不同租户的关联
- **P2**: 循环引用、自我引用

### 5. 状态边界
- 适用: 有 status 字段的实体
- **P0**: 非法状态值（超出枚举范围）
- **P1**: 非法状态转换（已禁用→操作）、重复操作（已启用→再启用）
- **P2**: 已删除后的操作

### 6. 业务规则
- 适用: 所有含业务逻辑的字段
- **P0**: 唯一性冲突（唯一索引字段）、权限不足
- **P1**: 业务约束违反（如到期租户创建用户）
- **P2**: 跨字段组合约束

---

## 组合场景推导（新增）

生成字段级边界后，基于以下规则自动推导组合场景：

| 规则 | 示例 | 优先级 |
|------|------|--------|
| 同一实体的 P0 边界两两组合 | name=null + status=非法值 | P2 |
| 时间先后关系颠倒 | expire_date < create_time | P2 |
| 状态 + 操作的非法组合 | 已禁用租户 → 创建用户 | P2 |
| 关联 + 主实体组合 | 不存在的 packageType + name=超长 | P2 |

最多生成 5 条组合场景，避免输出膨胀。

---

## 输出

### 输出格式（v2 — 人可读 + AI 可解析）

```markdown
---
tag: {tag}
type: boundary
entity: {entity}
module: {module}
version: v2
created: {current-date}
summary:
  total: {总数}
  P0: {P0数量}
  P1: {P1数量}
  P2: {P2数量}
  by_type: {数值: N, 字符串: N, 时间: N, 关联: N, 状态: N, 业务: N}
  warnings:
    - "VO @Size(max=30) 与 DM varchar(50) 不一致（name）"
    - "字段 contactPhone 在 VO 中无校验注解"
---

# [边界清单] {entity}

## {field-name} ({type})
| 边界类型 | 输入值 | 预期结果 | 优先级 | 来源 |
|----------|--------|----------|--------|------|
| 空值 | null | 校验失败(NotNull) | **P0** | @NotEmpty on name |
| 超长 | "a"×51 | 校验失败 | **P0** | @Size(max=50) |
| SQL注入 | "';DROP TABLE..." | 校验失败 | **P0** | 安全规范 |
| Unicode | "用户🎉名" | 校验失败 | **P2** | 通用边界 |

## 组合场景
| 场景 | 前置条件 | 操作 | 预期结果 | 优先级 |
|------|----------|------|----------|--------|
| name=null + status=非法值 | - | POST /tenant | 返回校验错误 | **P2** |
```
> `{timestamp}` 通过 `date +%Y%m%d-%H%M` 获取

### 1. 写入本地归档（存档轨）
将完整内容写入 `${SKILLS_ROOT}/m5-边界分析/outputs/bd-{tag}-{entity}-{timestamp}.md`

### 2. 同步到流水线总线（总线轨）
将完整内容写入 `${SKILLS_ROOT}/shared/outputs/m5-边界/bd-{tag}-{entity}.md`
- 按 CLAUDE.md 规范更新 `shared/outputs/STATUS.md`
