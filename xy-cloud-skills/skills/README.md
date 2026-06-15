# 微技能体系 (Micro-Skills)

通用技能引擎 + 可插拔知识包。包含 8 个微技能（M0~M7），每个技能职责单一、可在独立的 Claude Code 窗口中运行。支持多项目共存。

## 快速开始

### 首次使用（新项目）

```
步骤 1: 运行 M0 项目知识提取 ← 只需要一次
  → 输入项目源码根路径
  → 给项目起个名字（如 xy-cloud）
  → M0 自动扫描模块结构、组件索引、代码规范
  → 生成知识包到 knowledge/{项目名}/references/

步骤 2: 运行 M1~M7
  → 自动检测 knowledge/ 目录，加载知识包
  → 精准推荐组件、填充模板路径
```

### 切换项目

运行 M0，输入新项目名和新源码路径 → 生成到 `knowledge/{新项目名}/` → 旧知识包保留。
启动 M1~M7 时检测到多个知识包会自动提示选择。

### 已有知识包，直接使用

跳过 M0，M1~M7 启动时自动检测 `knowledge/` 目录加载。

> **无知识包时**：M1~M7 退化为纯方法论，每一步直接询问用户项目信息，不阻塞。

## 技能一览

| # | 微技能 | 目录 | 职责 | 总线轨（流水线通信） | 存档轨（历史归档） |
|---|--------|------|------|---------------------|--------------------|
| M0 | 项目扫描 | `m0-项目扫描/` | 从源码提取知识包 | — | — |
| M1 | 需求澄清 | `m1-需求澄清/` | 通过提问澄清业务需求 | `shared/outputs/m1-需求/` | `m1-需求澄清/outputs/` |
| M2 | 数据模型 | `m2-数据模型/` | 明确数据实体和字段 | `shared/outputs/m2-数据模型/` | `m2-数据模型/outputs/` |
| M3 | 组件查询 | `m3-组件查询/` | 查找可复用组件 | `shared/outputs/m3-组件/` | `m3-组件查询/outputs/` |
| M4 | 生成契约 | `m4-生成契约/` | 生成 YAML 实现契约 | `shared/outputs/m4-契约/` | `m4-生成契约/outputs/` |
| M5 | 边界分析 | `m5-边界分析/` | 分析测试边界 | `shared/outputs/m5-边界/` | `m5-边界分析/outputs/` |
| M6 | 测试任务 | `m6-测试任务/` | 生成测试任务书 | `shared/outputs/m6-测试任务/` | `m6-测试任务/outputs/` |
| M7 | 测试执行 | `m7-测试执行/` | 分析测试失败原因，给出修复建议 | `shared/outputs/m7-测试报告/` | `m7-测试执行/outputs/` |

> **模型推荐**：M4 代码生成涉及生产代码输出，推荐使用能力更强的模型（如 DeepSeek Pro），其余环节使用默认模型即可平衡成本与效果。

## 使用方式

每个微技能在独立的 Claude Code 窗口中运行：

```
# 示例：启动需求澄清微技能
cd m1-requirement-clarify/
claude . --skill SKILL.md
```

## 常用场景

| 场景 | 步骤 |
|------|------|
| 完整开发流程 | M1 → M2 → M3 → M4 → M5 → M6 → M7 |
| 需求明确直接编码 | M4 (直接描述需求) |
| 已有代码查组件 | M3 |
| 写测试但不知测哪些边界 | M5 |
| 已有边界清单生成测试 | M6 |

## 协作方式

微技能间通过 `shared/outputs/`（相对于 SKILLS_ROOT）中的文件通信。每个需求使用**唯一标识（tag）** 隔离输出文件，避免多需求产物冲突：

```
M1 写入 shared/outputs/m1-需求/req-{tag}-{module}.md                    ← 用户在此输入 tag
  → M2 从 req 文件名提取 {tag}，写入 shared/outputs/m2-数据模型/dm-{tag}-{entity}.md
    → M3 按 {tag} 过滤输入，写入 shared/outputs/m3-组件/cq-{tag}-{module}-components.md
      → M4 按 {tag} 过滤输入，写入 shared/outputs/m4-契约/{tag}.yaml（YAML 实现契约）
        → M5 按 {tag} 过滤输入，写入 shared/outputs/m5-边界/bd-{tag}-{entity}.md
          → M6 按 {tag} 过滤输入，写入 shared/outputs/m6-测试任务/{tag}.md
            → M7 从输入获取 {tag}，写入 shared/outputs/m7-测试报告/tr-{tag}-{module}-{date}.md
```

**需求标识（tag）**：短 kebab-case 字符串（如 `price`、`data-item`），M1 首次引入，下游技能自动从文件名继承。独立运行 M3/M4 等技能时需手动输入。

各 md 输出文件统一包含 YAML frontmatter（`tag`、`type`、`module`、`version`、`created`），标题带 `[类型]` 前缀，便于识别和脚本处理。

## 依赖

- 项目源码：运行 M0 时输入的路径
- 项目代码规范：`knowledge/{项目名}/references/代码规范速查.md`（M0 生成）
- 可复用组件索引：`knowledge/{项目名}/references/组件索引速查.md`（M0 生成）
- 项目结构映射：`knowledge/{项目名}/references/项目结构速查.md`（M0 生成）
- 手写规范（通用）：`shared/references/` 下的各规范文档

## 模板文件

M4 代码生成使用以下模板（`shared/templates/` 目录下）：

| 模板 | 用途 |
|------|------|
| `Controller.java` | CRUD REST 控制器 |
| `Service.java` | 服务接口 |
| `ServiceImpl.java` | 服务实现 |
| `Mapper.java` | MyBatis-Plus Mapper（含 selectPage 查询） |
| `DO.java` | 数据对象（Lombok + @KeySequence） |
| `SaveReqVO.java` | 创建/更新请求 VO（Lombok） |
| `PageReqVO.java` | 分页请求 VO（Lombok） |
| `ListReqVO.java` | 列表查询请求 VO（Lombok） |
| `RespVO.java` | 响应 VO（Lombok + Excel 导出注解） |
| `Mapper.xml` | MyBatis XML 映射文件（必须生成，含规范注释块） |
