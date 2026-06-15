---
name: m6-测试任务
description: 根据边界清单和 M4 任务书，生成「测试任务书」——打包测试上下文 + 边界清单 + 约束，供一次独立的 Agent 对话完成高质量测试代码生成。
trigger: |
  用户说"生成测试"、"写测试"或在 M5 之后运行。
  Claude Code 自身会根据上下文判断是否为 M5 边界分析完成后自动触发。
inputs:
  - ${SKILLS_ROOT}/shared/outputs/m5-边界/bd-*.md
  - ${SKILLS_ROOT}/shared/outputs/m4-契约/{tag}.yaml
  - ${SKILLS_ROOT}/shared/references/MANIFEST.md → test-standards
outputs:
  - ${SKILLS_ROOT}/shared/outputs/m6-测试任务/{tag}.md
  - ${SKILLS_ROOT}/m6-测试任务/outputs/{tag}-{timestamp}.md（存档）
tools:
  - bash: cat, ls, date, mkdir
  - ask_user
params:
  SKILLS_ROOT: "$(cd \"$(dirname \"$0\")\" && pwd)"
# 启动时自动读 MANIFEST.md + 检测知识包
---

# M6: 测试任务书

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

M6 **不生成测试代码**。M6 的任务是打包一份"测试任务书"——将 M5 边界分析和 M4 任务书整合为结构化测试上下文，供用户启动一次独立的 Agent 对话来生成测试代码。

## 为什么不在管道内生成测试代码

- 测试需要理解被测方法的行为、依赖注入方式、Mock 策略——这些在管道模板里无法充分表达
- 组件集成测试（如 OSSFileClientTest）没有通用模板，Agent 需要读项目中已有的同类测试来理解注释风格、断言方式、测试数据准备方式
- 测试模板驱动生成的测试往往结构对但语义空（方法签到了但没有针对被测逻辑的断言），不如 Agent 理解被测代码后自由生成

## 流程

### Step 1 — 读取 M5 边界分析

```bash
ls -t "${SKILLS_ROOT}/shared/outputs/m5-边界/bd-${tag}-"*.md 2>/dev/null | head -3
```

从边界文件提取：
- YAML front matter：`tag`、`entity`、`module`、`summary`（total/P0/P1/P2/by_type）
- 每条边界条目：`field`、`boundary_type`、`priority`、`source`

### Step 2 — 读取 M4 契约（获取上下文）

```bash
cat "${SKILLS_ROOT}/shared/outputs/m4-契约/${tag}.yaml" 2>/dev/null
```

从 M4 契约获取：
- `contract.module`（决定测试模块路径）
- `changes[].path`（被测类清单和包路径）
- `contract.execute_mode`（决定测试任务书的执行模式提示）

如果 M4 契约不存在，从 M5 的 YAML front matter 获取 `module` 和 `entity`。

### Step 3 — 索引测试规范

```bash
cat "${SKILLS_ROOT}/shared/references/MANIFEST.md"
```

从 MANIFEST 找到 `test-standards` 的文件名，记录文件名和用途。

根据 `dev_type` 判断是否需要额外索引：

| dev_type | 额外索引 |
|----------|---------|
| `business-crud` / `business-rpc` | 代码规范（了解被测方法命名）、数据库设计规范（了解 DO 字段） |
| `component-starter` | 无需额外索引 |

### Step 4 — 推导测试策略

| dev_type | 推荐测试类型 | 推荐基类 |
|----------|------------|---------|
| `business-crud` | Service 层 + Mapper 层 + Controller 层 | `BaseDbUnitTest` / `BaseMockitoUnitTest` |
| `business-rpc` | Service 层 + Feign Mock | 同上 |
| `component-starter` | 组件集成测试（纯 JUnit 5，无 Spring 容器） | 无基类 |

### Step 5 — 写入测试任务书

```markdown
# 测试任务书：${businessName}

| 元数据 | 值 |
|--------|-----|
| 需求标识 | `${tag}` |
| 所属模块 | `${module}` |
| 被测实体 | `${entity}` |
| 开发类型 | `${dev_type}` |
| 生成时间 | `${GEN_DATE}` |

## 边界概览（来自 M5）

| 指标 | 值 |
|------|-----|
| 总边界数 | ${total} |
| P0 | ${P0} |
| P1 | ${P1} |
| P2 | ${P2} |
| 边界类型分布 | ${by_type} |

## 被测对象（来自 M4 契约）

| 被测类 | 包路径 | 测试类型 |
|--------|--------|---------|
${从 M4 契约提取被测类路径}

## 测试策略

- **推荐类型**：${从 Step 4 推导的测试类型}
- **推荐基类**：${从 Step 4 推导的基类}
- **P0 全覆盖**：每个 P0 边界生成独立测试方法
- **P1 选覆盖**：≤ 5 条全生成，> 5 条选最关键 5 条
- **P2 视情况**：最多 2 个组合场景

## 项目测试规范参考

| 规范 | 文件名 | 用途 |
|------|--------|------|
| 测试规范 | ${test-standards 文件名} | 测试基类、工具类、编码规范 |

## 约束清单 ⚠️

### 测试范围约束
${从 M4 契约提取被测类路径}

### 边界覆盖约束
${从 M5 提取的关键边界提醒}

## 使用方式（审查暂存模式）

将本文件内容作为上下文，启动一次新的 Agent 对话：

> 根据以上测试任务书，将 ${entity} 的测试代码生成到 **skills 项目**的 `shared/outputs/m6-测试代码/${tag}/` 目录下。
> 先读取被测类源码和项目测试规范，理解依赖注入和 Mock 策略。
> 按边界概览中的 P0/P1/P2 分布生成测试方法。
> 遵守约束清单。参照项目中已有的同类测试文件的注释风格。

**人工审查**：生成完成后，review `m6-测试代码/${tag}/` 下的全部产物，确认无误后手动复制到 `xy-cloud` 项目对应模块的 `src/test/java/` 下。
```

### Step 6 — 写入输出

**存档轨**：
```bash
mkdir -p "${SKILLS_ROOT}/m6-测试任务/outputs/${tag}-${timestamp}"
```

**总线轨**：
```bash
mkdir -p "${SKILLS_ROOT}/shared/outputs/m6-测试任务"
```

### Step 7 — 更新 STATUS.md

按 CLAUDE.md 规范更新 `shared/outputs/STATUS.md`，M6 格填入 `{当前时间戳} 📋`（📋 = 测试任务书已就绪，待人工启动对话生成测试代码）。

## 不再执行的操作

以下限制仅针对 M6 技能本身（管道内步骤）：

- ❌ 不读取测试模板文件（ServiceDbTest.java 等）
- ❌ 不选择模板类型
- ❌ 不创建测试包路径目录
- ❌ 不生成任何 .java 测试文件
- ❌ 不执行生成约束检查清单

> 独立 Agent 对话**不受以上限制**。Agent 将测试代码生成到 `shared/outputs/m6-测试代码/{tag}/`（审查暂存区），
> 人工 review 后复制到项目对应模块的 `src/test/java/` 下。
