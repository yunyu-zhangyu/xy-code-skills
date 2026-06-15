---
name: m4-contract-generation
description: 综合需求规格、数据模型和组件推荐，生成 YAML Implementation Contract——机器可读的执行契约，含 create/modify 操作清单、验证路径、锚点定位。
trigger: M1/M2/M3 完成后运行。
inputs:
  - ${SKILLS_ROOT}/shared/outputs/m1-需求/req-*.md
  - ${SKILLS_ROOT}/shared/outputs/m2-数据模型/dm-*.md
  - ${SKILLS_ROOT}/shared/outputs/m3-组件/cq-*.md
  - ${SKILLS_ROOT}/knowledge/{project}/references/project-index.yaml
  - ${SKILLS_ROOT}/shared/references/MANIFEST.md
outputs:
  - ${SKILLS_ROOT}/shared/outputs/m4-契约/{tag}.yaml
  - ${SKILLS_ROOT}/m4-生成契约/outputs/{tag}-{timestamp}.yaml（存档）
tools:
  - bash: cat, ls, grep, date, mkdir, dirname
  - ask_user
params:
  SKILLS_ROOT: "$(cd "$(dirname "$0")" && pwd)"
---

# M4: 生成实现契约 (Implementation Contract)

## 职责

M4 **不生成代码**。M4 生成一份**机器可读的 YAML 契约**，精确描述：

- 哪些文件需要创建（路径、内容来源、参考文件）
- 哪些文件需要修改（路径、锚点、操作类型、新增内容）
- 路径是否已验证存在
- 哪些项需要人工确认

后续由代码执行 Agent（inline 同一对话 / standalone 独立对话）按契约逐条执行。

## 启动 — 加载知识包

```bash
# 1. 读取手写规范索引
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

# 3. 读取 project-index.yaml（M0 产出）
PROJECT_INDEX="${SKILLS_ROOT}/knowledge/${PROJECT_NAME}/references/project-index.yaml"
if [ -f "${PROJECT_INDEX}" ]; then
  echo "项目索引已加载：${PROJECT_INDEX}"
else
  echo "[警告] 未找到 project-index.yaml，建议先运行 M0-项目扫描。路径验证将不可用。"
fi

# 4. 确认目标项目根路径（用于路径验证）
if [ -f "${PROJECT_INDEX}" ]; then
  PROJECT_ROOT=$(grep "^project_root:" "${PROJECT_INDEX}" | sed 's/project_root: //')
fi
if [ -z "${PROJECT_ROOT}" ]; then
  echo "请输入目标项目的源码根目录绝对路径（如 D:/ZHYwork/xy-cloud）："
  read PROJECT_ROOT
fi
```

## 流程

### Step 1 — 读取上游输入

```bash
# 提示用户输入 tag
echo "请输入需求标识（tag），如 oss-file-client："
read TAG

# 读取 M1 需求规格
REQ_FILE=$(ls -t "${SKILLS_ROOT}/shared/outputs/m1-需求/req-${TAG}-"*.md 2>/dev/null | head -1)
if [ -z "${REQ_FILE}" ]; then
  echo "[错误] 未找到 M1 需求规格文件：req-${TAG}-*.md"
  exit 1
fi

# 读取 M2 数据模型（可能多个实体）
DM_FILES=$(ls -t "${SKILLS_ROOT}/shared/outputs/m2-数据模型/dm-${TAG}-"*.md 2>/dev/null)

# 读取 M3 组件推荐
CQ_FILE=$(ls -t "${SKILLS_ROOT}/shared/outputs/m3-组件/cq-${TAG}-"*.md 2>/dev/null | head -1)
if [ -z "${CQ_FILE}" ]; then
  echo "[警告] 未找到 M3 组件推荐文件，部分依赖信息可能缺失。"
fi
```

从上游提取元数据并展示给用户：

```
===== 上游输入摘要 =====
需求标识:   ${TAG}
模块:       {从 M1 提取}
业务名称:   {从 M1 提取}
开发类型:   {从 M1 提取}
实体:       {从 M1/M2 提取}
组件文件:   {CQ_FILE 存在/不存在}
========================
```

### Step 2 — 解析 M3 变更项，绑定路径

M3 组件推荐的输出中，`component-starter` 专用节包含两个关键表格：
- **需新建的文件**：文件路径、参考模板、参考源码路径
- **需修改的已有文件**：文件路径、操作类型、新增代码

对于每个变更项，执行路径绑定和验证。

#### 路径解析规则

```
路径解析流程：
1. 从 M3 拿到相对路径（如 infra/oss/OSSFileClient.java）
2. 从 project-index.yaml 查找模块的 src_main_java 路径
3. 拼接为绝对路径
4. 验证目标目录是否存在
```

#### 创建项验证

对每条 `action: create`，验证：
```bash
# 父目录必须存在
PARENT_DIR=$(dirname "${FULL_PATH}")
if [ -d "${PARENT_DIR}" ]; then
  echo "  ✓ 目录存在: ${PARENT_DIR}"
else
  echo "  ⚠ 目录不存在: ${PARENT_DIR}（标记 needs_review）"
  NEEDS_REVIEW=true
fi

# 文件不能已存在（除非用户确认覆盖）
if [ -f "${FULL_PATH}" ]; then
  echo "  ⚠ 文件已存在: ${FULL_PATH}（标记 needs_review）"
  NEEDS_REVIEW=true
fi
```

#### 修改项验证

对每条 `action: modify`，验证：
```bash
# 目标文件必须存在
if [ -f "${FULL_PATH}" ]; then
  echo "  ✓ 文件存在: ${FULL_PATH}"
else
  echo "  ⚠ 文件不存在: ${FULL_PATH}（标记 needs_review）"
  NEEDS_REVIEW=true
fi

# 锚点必须唯一匹配
ANCHOR_COUNT=$(grep -c "${ANCHOR}" "${FULL_PATH}" 2>/dev/null || echo 0)
if [ "${ANCHOR_COUNT}" -eq 1 ]; then
  echo "  ✓ 锚点唯一匹配: \"${ANCHOR}\""
elif [ "${ANCHOR_COUNT}" -eq 0 ]; then
  echo "  ⚠ 锚点无匹配: \"${ANCHOR}\"（标记 needs_review）"
  NEEDS_REVIEW=true
else
  echo "  ⚠ 锚点多匹配: \"${ANCHOR}\" 匹配 ${ANCHOR_COUNT} 处（标记 needs_review）"
  NEEDS_REVIEW=true
fi
```

### Step 3 — 构建 Changes 列表

将 Step 2 中每个变更项转为 YAML change 条目。来自 M3 组件推荐表的每行转换逻辑：

| M3 表字段 | 契约字段 |
|-----------|---------|
| 文件路径 | path（拼接为绝对路径） |
| 操作类型（新建/修改） | action: create / modify |
| 参考模板/参考源码路径 | reference_files |
| 新增代码 | content（如果是 inline）或 content_source: reference |
| 操作描述 | operation（如 insert_enum_constant） |
| 在 XXX 后新增 | anchor（定位字符串） |

**置信度规则**：
- M3 "需新建的文件"表格中的项 → needs_review: false（高置信度）
- M3 "需修改的已有文件"表格中的项 → needs_review: false（高置信度）
- Step 2 验证失败的项 → 标记 needs_review: true
- M3 推导但不明确的项（如 M3 说"可能需要新建配置文件"） → 标记 needs_review: true

### Step 4 — 批量确认

汇总所有变更项，以表格形式呈现给用户确认：

```
===== 实现契约预览 =====
Tag: ${TAG}
模块: ${module}
执行模式: [inline / standalone]（本次对话内执行，输入 inline；独立对话执行，输入 standalone）

代码风格约束：
  - 所有注解（除 getter/setter、@Override）同行或上一行加注释说明用途

变更清单：

  # | action | 文件 | 验证 | 需确认
  — | ------ | ---- | ---- | ------
  1 | create | .../oss/OSSFileClient.java | ✓ 目录存在 | —
  2 | modify | .../oss/FileStorageEnum.java | ✓ 文件+锚点存在 | —
  3 | create | .../oss/OSSFileClientConfig.java | ✓ 目录存在 | ⚠ M3推导，请确认是否需要此文件

需要确认的项：
  项 #3 — M3 推导可能需要 OSSFileClientConfig.java，是否保留？
  [Y] 保留  [N] 删除

还有其他需要调整的吗？输入编号操作或回车确认。
========================
```

**用户操作选项**：
- `回车` → 全部确认，生成契约
- `{编号} Y/N` → 逐个确认/拒绝
- `{编号} modify:xxx` → 修改某项内容

### Step 5 — 写入契约文件

YAML 契约模板：

```yaml
# 实现契约 — 由 M4 自动生成
contract:
  tag: "${TAG}"
  module: "${MODULE}"
  created: "${CURRENT_DATE}"
  execute_mode: "${EXECUTE_MODE}"    # inline | standalone

  # 上游输入来源
  sources:
    requirement: "${REQ_FILE}"
    data_models:
$(for f in ${DM_FILES}; do echo "      - \"${f}\""; done)
    component_query: "${CQ_FILE}"

  # 参考文件索引（M0 产出 + 模板）
  references:
    project_index: "${PROJECT_INDEX}"
    templates:
      - "${SKILLS_ROOT}/shared/templates"

  # — 代码生成约束 —
  constraints:
    annotation_comments: true
    # 生成的 Java 文件中，除 getter/setter 和 @Override 外，每个注解在同行或上一行加一行注释说明用途。
    # 注释简短，不解释业务含义。

  # — 变更清单 —
  changes:
    # 每次变更项的格式：
$(for change in "${CHANGES[@]}"; do echo "    - action: ${ACTION}")
$(echo "      path: \"${FULL_PATH}\"")
$(echo "      must_exist_after: true")
$(echo "      needs_review: ${NEEDS_REVIEW}")
$(if [ "${ACTION}" = "modify" ]; then
  echo "      anchor: \"${ANCHOR}\""
  echo "      operation: \"${OPERATION}\""
  echo "      content: |"
  echo "        ${CONTENT}"
fi)
$(if [ ${#REF_FILES[@]} -gt 0 ]; then
  echo "      reference_files:"
  for rf in "${REF_FILES[@]}"; do
    echo "        - \"${rf}\""
  done
fi)
done
```

写入两个位置：

```bash
# 存档轨（带时间戳）
TIMESTAMP=$(date +%Y%m%d-%H%M)
mkdir -p "${SKILLS_ROOT}/m4-生成契约/outputs/${TAG}-${TIMESTAMP}"
# 契约文件写入存档
cat > "${SKILLS_ROOT}/m4-生成契约/outputs/${TAG}-${TIMESTAMP}/${TAG}-contract.yaml" << 'CONTRACT_EOF'
# ... YAML 内容 ...
CONTRACT_EOF

# 总线轨（无时间戳，覆盖）
mkdir -p "${SKILLS_ROOT}/shared/outputs/m4-契约"
cat > "${SKILLS_ROOT}/shared/outputs/m4-契约/${TAG}.yaml" << 'CONTRACT_EOF'
# ... YAML 内容 ...
CONTRACT_EOF
```

### Step 6 — 更新 STATUS.md

按 CLAUDE.md 规范更新 `shared/outputs/STATUS.md`，M4 格填入 `{当前时间戳} 📋`（📋 = 契约已就绪）。

### Step 7 — 告知用户下一步

根据 execute_mode 给出不同指引：

**inline 模式**：
```
契约已生成：shared/outputs/m4-契约/${TAG}.yaml
执行模式：inline — 将在当前对话内按契约执行代码生成。

是否现在开始执行？
[Y] 开始执行
[N] 稍后手动执行
```

**standalone 模式**：
```
契约已生成：shared/outputs/m4-契约/${TAG}.yaml
执行模式：standalone — 请启动一次新的 Agent 对话，以契约文件为上下文执行。

启动方式：
  claude D:/ZHYwork/xy-cloud --skill-path "${SKILLS_ROOT}/skills/执行契约/SKILL.md"
```

## 不再执行的操作

- ❌ 不生成任何 .java / .xml 代码文件
- ❌ 不读取模板文件
- ❌ 不执行文件创建或修改
- ❌ 不执行代码规范检查
- ✅ 只生成 YAML 契约 + 路径验证 + 批量确认
