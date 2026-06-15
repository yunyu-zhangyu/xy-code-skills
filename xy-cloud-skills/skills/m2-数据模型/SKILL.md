---
name: m2-数据模型
description: 明确数据实体、字段定义、类型和业务规则，输出数据模型定义文件。
trigger: 用户说"设计数据模型"、"定义实体"或在 M1 之后运行。
inputs:
  - ${SKILLS_ROOT}/shared/outputs/m1-需求/req-*.md
  - ${SKILLS_ROOT}/shared/references/MANIFEST.md → database-design
outputs:
  - ${SKILLS_ROOT}/shared/outputs/m2-数据模型/dm-{tag}-{entity}.md
  - ${SKILLS_ROOT}/m2-数据模型/outputs/dm-{tag}-{entity}-{timestamp}.md
tools:
  - bash: cat, ls, date, sed
  - ask_user
params:
  SKILLS_ROOT: "$(cd \"$(dirname \"$0\")\" && pwd)"
# 启动时自动读 MANIFEST.md + 检测知识包
---

# M2: 数据模型设计 (Data Model Design)

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

1. **读取参考文档**：先执行 `cat "${SKILLS_ROOT}/shared/references/MANIFEST.md"` 获得文档清单，从清单中找到 `database-design` 对应的文件名并 `cat` 读取该文件获取表命名规范、DO 继承体系、字段约定（如果 `cat` 报错文件不存在，则执行 `ls` 扫描目录通过文件 H1 标题推断内容重建映射）
2. 检查 `${SKILLS_ROOT}/shared/outputs/m1-需求/` 是否有最近的 req-*.md 文件
   - 有 → 执行输入检查命令，取最新文件从中提取 `{tag}`，然后用 tag 精确定位当前需求文件
   - 无 → 直接请用户描述要设计的业务实体，并询问需求标识 `{tag}`
3. 每实体循环提问字段详情
4. 生成 dm-*.md 文件（文件名带 tag）

## 输入检查

```bash
# 取最新需求文件提取 tag，再用 tag 精确定位（避免历史文件干扰）
latest=$(ls -t ${SKILLS_ROOT}/shared/outputs/m1-需求/req-*.md 2>/dev/null | head -1)
if [ -n "$latest" ]; then
  tag=$(echo "$latest" | sed -n 's/.*req-\(.*\)-\(.*\)\.md/\1/p')
  ls -t "${SKILLS_ROOT}/shared/outputs/m1-需求/req-${tag}-"*.md 2>/dev/null | head -1
fi
```

如有输入文件，先读取并与用户确认使用哪个。

**上游 Schema 校验**：读取 req 文件后，校验必要字段：
```bash
head -20 "$INPUT_FILE" | grep -q "^tag:" || echo "[警告] 缺少 tag 字段，后续 tag 过滤可能失败"
head -20 "$INPUT_FILE" | grep -q "^module:" || echo "[警告] 缺少 module 字段，需手动确认目标模块"
head -20 "$INPUT_FILE" | grep -q "entity" || echo "[警告] 未检测到实体定义，请确认上游 M1 输出完整"
```

> 若上游 M1 输出（req-*.md）可用，其中已包含实体字段初步定义。M2 可直接基于此细化字段类型、长度、校验规则，无需重新引导用户输入实体信息。

## 实体提问循环

对每个实体，依次提问：

1. **实体名**：这个实体叫什么？（如 `EnergyPrice`）
2. **字段清单**：有哪些关键字段？类型、长度、是否必填？
3. **校验规则**：字段有哪些校验规则？（唯一性、取值范围、格式）
4. **租户隔离**：该实体的数据是否需要按租户隔离？
   - 是 → DO 继承 `TenantBaseDO`，DDL 包含 `tenant_id` 字段
   - 否 → DO 继承 `BaseDO`
   > 系统级表（字典/菜单/配置等全局共享数据）不需要租户隔离。
   > ⚠️ 租户表选错继承会导致租户间数据互相可见，且无任何报错。
5. **实体关系**：与其他实体的关系是什么？（1:1 / 1:N / N:M）
6. **唯一约束**：有哪些唯一性约束或业务规则？

## 字段类型参考（xy-cloud）

| 类型 | Java | 数据库 |
|------|------|--------|
| 主键 | Long | bigint(20) |
| 名称 | String(≤50) | varchar(50) |
| 编码 | String(≤50) | varchar(50) UNIQUE |
| 状态 | Integer / String | tinyint(3)（system/infra）/ varchar(8)（eos） |
| 金额 | Long(分) | bigint(20) |
| 比率 | BigDecimal(18,6) | decimal(18,6) |
| 日期时间 | LocalDateTime | datetime |
| 备注 | String(≤500) | varchar(500) |
| 租户ID | Long | bigint(20) |
| DO 继承 | TenantBaseDO / BaseDO | 租户表选 TenantBaseDO，系统表选 BaseDO（选错导致数据泄露！） |

## 输出

### 1. 写入本地归档（存档轨）
将完整内容写入 `${SKILLS_ROOT}/m2-数据模型/outputs/dm-{tag}-{entity-name}-{timestamp}.md`
> `{timestamp}` 通过 `date +%Y%m%d-%H%M` 获取

### 2. 同步到流水线总线（总线轨）
将完整内容写入 `${SKILLS_ROOT}/shared/outputs/m2-数据模型/dm-{tag}-{entity-name}.md`
- 按 CLAUDE.md 规范更新 `shared/outputs/STATUS.md`

格式：
```markdown
---
tag: {tag}
type: data-model
entity: {entity-name}
module: {module}
version: v1
created: {current-date}
---

# [数据模型] {entity-name}

## 表名
`{table_name}`

## 字段定义
| 字段名 | 类型 | 长度 | 必填 | 说明 | 校验 |
|--------|------|------|------|------|------|

## 索引
| 索引名 | 字段 | 类型 |
|--------|------|------|

## 关系
{实体关系描述}
```
