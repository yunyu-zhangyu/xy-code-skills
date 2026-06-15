---
name: m1-requirement-clarify
description: 通过结构化提问澄清业务需求，输出需求规格说明。用于编码前梳理不清晰的需求。
trigger: 用户说"帮我澄清需求"、"需求梳理"或描述一个模糊的业务功能时激活。
inputs: []
outputs:
  - ${SKILLS_ROOT}/shared/outputs/m1-需求/req-{tag}-{module}.md
  - ${SKILLS_ROOT}/m1-需求澄清/outputs/req-{tag}-{module}-{timestamp}.md
tools:
  - ask_user
params:
  SKILLS_ROOT: "$(cd \"$(dirname \"$0\")\" && pwd)"
# 启动时自动读取 shared/references/MANIFEST.md 获取 base-package 和项目结构
---

# M1: 需求澄清 (Requirement Clarify)

## 启动 — 加载知识包

```bash
# 1. 读取手写规范索引（通用）
cat "${SKILLS_ROOT}/shared/references/MANIFEST.md" 2>/dev/null || true

# 2. 检测项目知识包（knowledge/ 目录）
PROJECT_LIST=$(ls -d "${SKILLS_ROOT}/knowledge/"*/ 2>/dev/null | xargs -n1 basename 2>/dev/null)
PROJECT_COUNT=$(echo "${PROJECT_LIST}" | wc -l)

if [ "${PROJECT_COUNT}" -eq 1 ]; then
  # 只有一个知识包，自动使用
  PROJECT_NAME="${PROJECT_LIST}"
elif [ "${PROJECT_COUNT}" -gt 1 ]; then
  # 多个知识包，让用户选择
  echo "检测到多个项目知识包："
  echo "${PROJECT_LIST}"
  echo "请输入当前项目名称（从以上列表中选择）："
  read PROJECT_NAME
else
  # 无知识包，降级
  PROJECT_NAME=""
fi

if [ -n "${PROJECT_NAME}" ] && [ -f "${SKILLS_ROOT}/knowledge/${PROJECT_NAME}/references/MANIFEST.md" ]; then
  # 有知识包：读取项目配置
  echo "使用知识包：${PROJECT_NAME}"
  BASE_PACKAGE=$(grep "^| base-package" "${SKILLS_ROOT}/knowledge/${PROJECT_NAME}/references/MANIFEST.md" | awk -F'|' '{print $3}' | xargs)
  KNOWLEDGE_AVAILABLE=true
else
  # 无知识包：降级模式
  KNOWLEDGE_AVAILABLE=false
  echo "[提示] 未检测到项目知识包，部分信息将在问答中收集。建议先运行 M0 项目知识提取。"
fi
```

## 流程

激活后，依次提问 Q0~Q7，每个问题含引导模板和追问条件，全部回答后进入确认环节（Q7），确认后生成需求规格文件。

```
Q0: 需求标识(tag) → Q1: 定位模块 → Q2: 功能点+业务命名 → Q2.5: 开发类型+模式选择
  → Q3: 数据实体+命名推导 → Q4: 特殊逻辑 → Q5: 多表关联判断 → Q6: 附加资料 → Q7: 确认环节 → 输出
```

> 每个问题用户回答模糊时触发追问（最多 2 轮），仍模糊则标记 `[待确认]` 推给 Q7 统一处理。

## Q0: 需求标识

> 请给这个需求一个简短标识（`{tag}`），用于隔离所有输出文件。
>
> - 格式：kebab-case，小写字母和连字符，如 `price`、`data-item`、`demand-v2`
> - 长度：不超过 30 个字符
> - 作用：这个标识将用于 M1~M7 所有输出文件名和目录名
>
> 直接回车使用模块名作为缺省标识。

**追问条件**：

| 触发条件 | 追问 |
|----------|------|
| 含空格或大写字母 | "请使用 kebab-case 格式：全小写字母和连字符，如 `price`、`data-item`" |
| 过于泛化（test/demo/abc） | "建议用业务含义更明确的词，如 `energy-price`，便于后续文件识别" |
| 超 30 字符 | "标识建议不超过 30 个字符，可以简化为更短的词吗？" |
| 直接回车 | "未输入标识，将使用模块名作为缺省标识，确认吗？" |

用户确认后，将标识存入 `{tag}`。

## Q1: 定位模块

> 这个需求属于哪个业务域/模块？
>
> 先检查知识包是否存在（启动阶段已检测）：
> 1. 如果 `KNOWLEDGE_AVAILABLE=true`，执行 `cat "${SKILLS_ROOT}/knowledge/${PROJECT_NAME}/references/项目结构速查.md" 2>/dev/null` 读取模块信息
>
> **有知识包** → 从「项目模块全景」表和「模块路径速查表」中读取当前可用模块列表（优先展示类型为"业务"的模块），以编号列表呈现供用户选择。若用户选择的模块不在当前列表中，将其作为新增模块处理。
>
> **无知识包** → 直接询问用户：
>   "你的项目有哪些业务模块？请列举模块名和对应的源码目录路径（如 模块A → src/main/java/...），我帮你补充项目结构。"

**追问条件**：

| 触发条件 | 追问 |
|----------|------|
| 选"其他"或不确定 | "大致描述一下这个功能负责什么，我帮你判断归属哪个模块" |

输出变量：`{module}`（模块名小写）。

## Q2: 核心功能点与业务命名

> 这个功能的中文业务名称叫什么？（用于代码注释、Swagger 界面标题等）
>
> 参考示例：电价管理、数据项配置、合同管理、用户权限配置
>
> 业务名称：________

**知识包提示（如有）**：在用户说出业务名称后，检查知识包中的组件索引是否匹配相关领域：
```bash
# 可选：如果用户说"文件上传"，检查组件索引有无相关类
cat "${SKILLS_ROOT}/knowledge/${PROJECT_NAME}/references/组件索引速查.md" 2>/dev/null | grep -i "${businessName关键词}" | head -5
```
如果有匹配的已有组件，提示用户：
> "项目中已有 {组件名}（{组件用途}），你的需求是否基于此扩展？还是全新的功能？"

用户给出业务名称后，存入 `{businessName}`。

> 接下来请列出 1~5 个核心功能点，参考以下模板：
>
> ```
> □ 创建/新增          — 新增一条数据记录
> □ 修改/编辑          — 修改已有数据
> □ 删除              — 删除数据
> □ 查询（分页）        — 按条件分页查询列表
> □ 查看详情           — 查询单条数据详情
> □ 导入（Excel）       — 批量导入数据
> □ 导出（Excel）       — 导出数据为 Excel
> □ 统计分析 / 报表     — 数据统计或报表生成
> □ 审核 / 审批         — 业务审核流程
> □ 其他               — 请描述
>
> 请列出这个功能的具体操作（可直接用编号或自由描述）：
> ```

**追问条件**：

| 触发条件 | 追问 | 最大轮次 |
|----------|------|----------|
| 只说业务名没列功能点 | "功能点呢？比如这个{businessName}，有哪些具体操作？" | 1 |
| 功能只有"管理"类泛词 | "具体包括哪些操作？增删改查都包含吗？是否有导入导出、统计分析？" | 1 |
| 功能点仅 1~2 个 | "只有这些吗？是否需要查询、导出或其他操作？" | 1 |
| 功能点 > 5 个 | "建议先聚焦前 5 个核心功能点，其他后续补充" | - |
| 提到"审批"/"审核" | 标记`需关注审批`，Q4 追问时自动提醒补充审批流 | - |

输出变量：
- `{businessName}`：中文业务名
- `{features}`：功能点列表（保留用户原始描述，按创建/修改/删除/查询/导入/导出/统计/审核分类标注）
- `{dev_type}`：开发类型（见 Q2.5）
- `{patterns}`：业务模式标记列表（见 Q2.5）

## Q2.5: 开发类型与模式选择

> 这个需求属于哪种开发类型？需要哪些额外的代码模式？
>
> **第一步：开发类型**
> ```
> [a] 标准业务 CRUD（默认）— Controller + Service + Mapper + VO
> [b] 业务 CRUD + Feign RPC 接口 — 还需生成 Api/Impl/DTO（供其他模块调用）
> [c] 框架组件/Starter 开发 — AutoConfiguration + Properties
> ```
>
> **第二步：业务模式（可多选，默认仅标准 CRUD）**
> ```
> □ Excel 导入导出     → 需 @Excel 注解 + ExcelUtils
> □ 定时任务（XXL-Job） → 需 @XxlJob 处理器
> □ MQ 异步消息        → 需 Spring Event Producer/Consumer
> □ 文件上传/下载       → 需 FileApi 集成
> □ WebSocket 推送     → 需 WebSocketSenderApi
> ```

**追问条件**：

| 触发条件 | 追问 |
|----------|------|
| Q2 勾选了"导入/导出"但此处未选 Excel | "前面功能点中提到了导入/导出，是否需要生成 Excel 相关代码？" |
| 选了"框架组件/Starter" | "Starter 开发属于框架层，不在业务模块中。确认这个需求是组件开发而非业务开发？" |
| 选了 MQ 但模块是 eos | "eos 模块目前使用 Spring Event 模式，确认要使用此模式？" |
| 选"其他" | "请描述需要哪种模式，我来判断是否有对应的模板支持" |

输出变量：
- `{dev_type}`：`business-crud`（默认）/ `business-rpc` / `component-starter`
- `{patterns}`：已选模式列表，如 `[excel, xxl-job]`，无则为空数组

> 这两个变量会传递给 M3（组件查询）和 M4（代码生成），决定启用哪些模板和组件。

## Q3: 数据实体与命名推导

> 涉及哪些核心数据实体？请列出实体名称即可，字段设计和实体关系由 M2 数据模型阶段处理。
>
> 实体列表参考格式：
>
> ```
> 实体1: EnergyPrice（电价）
> 实体2: PriceTemplate（电价模板）
> ```
>
> **知识包提示（如有）**：读取代码规范速查.md 中的 DO 继承体系：
> ```bash
> cat "${SKILLS_ROOT}/knowledge/${PROJECT_NAME}/references/代码规范速查.md" 2>/dev/null | grep -A3 "DO.*继承\|BaseDO\|TenantBaseDO"
> ```
> 如果检测到项目使用 TenantBaseDO（多租户）或 BaseDO，在提问中提示：
> > "项目中使用 {BaseDO/TenantBaseDO} 作为实体基类（含 createTime/updateTime/creator/updater/deleted{/tenantId}），确认继承此基类？"
>
> 为后续代码生成做准备，还需确认以下规范：
>
> **1. 实体英文命名（PascalCase）**
> 如 `EnergyPrice`、`PriceTemplate`
>
> **2. Controller 方法命名和 URL 路径风格**
> - 优先参考项目命名规范（如有）
> - 无规范时使用通用命名方式：`create`/`update`/`delete`/`get`/`getList`/`getPage`，URL 对应 `/create`/`/update`/`/delete`/`/get`/`/list`/`/page`
>
> **3. Service 方法命名风格**
> - 优先参考项目命名规范文档（后续会补充）
> - 无规范时使用通用命名方式
>
> **4. 领域包名（domain）命名规则**
> 如 `energyprice`（全小写）

**追问条件**：

| 触发条件 | 追问 | 最大轮次 |
|----------|------|----------|
| 英文实体名不会写 | "中文名是什么？我来帮你转换成英文命名" | 1 |
| 实体超过 3 个 | "建议先聚焦主要业务实体，哪些是核心表？" | 1 |
| 实体名与包名不一致 | "实体名 {X} 对应的领域包名建议为 {x}，确认吗？" | 1 |

用户回答后，自动推导命名变量（仅在 Q7 确认环节展示，无需用户操心）：

| 变量 | 推导规则 | 示例 |
|------|----------|------|
| `{entity}` | 用户输入的实体英文名 → PascalCase | EnergyPrice |
| `{domain}` | `{entity}` 全小写 | energyprice |
| `{resource}` | `{entity}` 首字母小写 | energyPrice |
| `{tableName}` | `{module}_{domain}` | eos_energy_price |

输出变量：
- `{entities}`：实体列表（每个含中文名、英文名）
- `{controllerStyle}`：Controller 命名风格（项目规范/通用）
- `{serviceStyle}`：Service 命名风格（项目规范/通用）

## Q4: 特殊逻辑

> 有无特殊业务逻辑？请从以下类型选择（可多选）：
>
> ```
> □ 无特殊逻辑
> □ 审批流           → 流程节点？谁审批？
> □ 状态机           → 有哪些状态？如何流转？
> □ 定时任务         → 触发条件？执行内容？
> □ 消息/通知推送     → 推送给谁？推送时机？
> □ 外部接口/系统调用  → 调用哪个系统？接口协议？
> □ 复杂计算/校验规则  → 什么规则？
> □ 数据导入/导出     → 导入模板？导出字段？
> □ 多租户隔离        → 数据按租户隔离吗？
> □ 其他             → 请描述
> ```

**追问条件**：

| 触发条件 | 追问 | 最大轮次 |
|----------|------|----------|
| 回答"有"但没具体说 | "具体是哪类特殊逻辑？以上类型中是否有匹配的？" | 2 |
| 勾选审批流 | "审批节点有哪些？每个节点谁审批？（如：提交→主管审核→财务审核）" | 1 |
| 勾选状态机 | "有哪些业务状态？状态之间如何流转？（如：草稿→待审核→已生效→已失效）" | 1 |
| 勾选定时任务 | "什么时机触发（cron 表达式）？执行什么操作？" | 1 |
| 勾选外部接口 | "对接哪个系统？数据是单向还是双向同步？" | 1 |
| Q2 提到"审批"/"审核"但 Q4 没选 | "前面提到有审核功能，是否需要补充勾选审批流？" | 1 |
| 选"无特殊逻辑"但 Q2/Q3 暗示复杂行为 | 温和提醒："看起来有{xx}逻辑，确认不需要勾选吗？" | 1 |

输出变量：`{special_logic}`（特殊逻辑列表，含类型 + 简要描述）。

## Q5: 多表关联判断

> 这些实体之间是否需要多表关联查询？
>
> 例如：查询电价列表时，需要同时展示电价对应的模板名称。
>
> ```
> [1] 不需要 — 都是单表操作，使用 LambdaQueryWrapperX
> [2] 需要   — 多表 Join 查询，使用 MPJLambdaWrapperX
> [3] 不确定
> ```

**追问条件**：

| 触发条件 | 追问 |
|----------|------|
| 选"不确定" | "检查功能点中是否有跨实体查询的场景。比如'查询某实体时，同时展示关联实体的名称或信息'" |

> 此信息将传递给 M3 组件查询技能，决定使用哪个查询构造器。

输出变量：`{multi_table}`（true/false）。

## Q6: 附加资料

> 有没有需求文档、原型图、接口文档等参考资料？（可选）
>
> 提供链接或文件路径可以让后续微技能生成更准确的内容。

**追问条件**：

| 触发条件 | 追问 |
|----------|------|
| 说"有"但没给具体信息 | "方便提供文档链接或文件路径吗？" |

输出变量：`{references}`（可选，无则记为空）。

## Q7: 确认环节

收集完所有信息后，汇总展示确认清单：

```
===== 需求信息确认 =====

【基础信息】
  需求标识(tag):      {tag}
  所属模块(module):   {module}
  业务名称:           {businessName}

【命名变量（M4 代码生成使用）】
  实体名(entity):     {entity}
  领域包名(domain):   {domain}
  资源名(resource):   {resource}
  数据库表名(tableName): {tableName}


【核心功能点】
  1. 分类 | 操作 | 说明
  ...

【数据实体】（字段设计和关联关系详见 M2 数据模型）
  1. {entity}（{中文名}）

【特殊逻辑】
  {special_logic 逐项列出}

【多表关联】
  {需要/不需要} → 推荐查询构造器: {MPJLambdaWrapperX / LambdaQueryWrapperX}

【参考资料】
  {references 或 无}

============================

以上信息是否正确？请逐项检查：
  [Y] 全部正确，生成需求规格文件
  [N] 需要修改 → 输入修改项编号及修正内容
```

**用户确认（Y）** → 写入输出文件。

**用户要求修改（N）**：
1. 用户输入编号或字段名 + 修正内容
2. 更新对应变量
3. 重新输出确认清单
4. 重复直到用户确认 Y

## 追问机制规则

### 通用原则

1. **轻量**：每个追问控制在 1~2 句话，不展开长篇讨论
2. **引导优先**：先给选项/模板引导，再追问（降低模糊回答概率）
3. **有限回退**：每个问题最多追问 2 轮 → 仍模糊则标记 `[待确认]` 并继续（不卡流程）
4. **确认兜底**：所有 `[待确认]` 标记在 Q7 确认环节突出展示，让用户最后统一处理

### 追问触发矩阵

| 问题 | 触发条件 | 最大轮次 | 回退动作 |
|------|----------|----------|----------|
| Q0 | tag 含非法字符 | 1 | 使用模块名缺省 |
| Q0 | tag 过于泛化 | 1 | 接受但给出建议 |
| Q2 | 只有"管理"类泛词 | 1 | 标记[待确认] |
| Q2 | 功能点 < 2 | 1 | 标记[待确认] |
| Q3 | 实体名与包名不一致 | 1 | 使用用户确认的命名 |
| Q3 | 英文实体名未提供 | 1 | 使用中文拼音/缩写占位 |
| Q4 | "有"但无详情 | 2 | 标记[待确认] |
| Q4 | Q2提审批但Q4没选 | 1 | 标记[待确认] |
| Q5 | "不确定" | 1 | 标记[待确认] |

## 输出

用户确认后，生成需求规格文件：

### 1. 写入本地归档（存档轨）
将完整内容写入 `${SKILLS_ROOT}/m1-requirement-clarify/outputs/req-{tag}-{module}-{timestamp}.md`
> `{timestamp}` 通过 `date +%Y%m%d-%H%M` 获取，用于区分历史版本

### 2. 同步到流水线总线（总线轨）
将完整内容写入 `${SKILLS_ROOT}/shared/outputs/m1-需求/req-{tag}-{module}.md`
> 总线轨文件无时间戳，相同 tag 重新运行时自然覆盖，下游技能始终读取最新版本
- 按 CLAUDE.md 规范更新 `shared/outputs/STATUS.md`

```markdown
---
tag: {tag}
type: requirement
module: {module}
multi_table: {true/false}
version: v1
created: {current-date}
---

# [需求规格] {module} - {businessName}

## 基本信息

| 字段 | 值 |
|------|-----|
| 需求标识 | {tag} |
| 所属模块 | {module} |
| 业务名称 | {businessName} |
| 实体名 entity | {entity} |
| 领域包名 domain | {domain} |
| 资源名 resource | {resource} |
| 数据库表名 tableName | {tableName} |

## 核心功能点

| # | 分类 | 操作 | 说明 |
|---|------|------|------|
| 1 | 创建 | ... | ... |

## 数据实体

### {EntityName}（{中文名}）

- **entity**: {EntityName}
- **domain**: {domain}
- **tableName**: {table_name}

> 实体字段设计、校验规则和关联关系由 M2 数据模型技能细化。

## 特殊逻辑

- {类型}: {描述}

## 多表关联查询

- **需要**: {是/否}
- **场景**: {具体场景描述}
- **推荐查询构造器**: {LambdaQueryWrapperX / MPJLambdaWrapperX}

## 约束和假设

1. ...

## 参考资料

- {文档/链接}

## 下一步建议

建议按以下顺序继续：
1. **M2 数据模型设计** → 细化每个实体的字段定义、校验规则、唯一约束和关联关系
2. **M3 组件查询** → 确认所需组件和查询构造器
3. **M4 代码生成** → 基于以上输入生成完整代码
```

写入后告知用户文件路径及下一步建议（运行 M2 数据模型设计）。
