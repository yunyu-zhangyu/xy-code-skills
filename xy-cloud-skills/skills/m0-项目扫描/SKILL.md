---
name: m0-project-knowledge
description: 读项目源码提取知识包，输出到 knowledge/{project-name}/references/，供 M1~M7 使用。不同项目通过 knowledge/ 下的子目录隔离。
trigger: 首次使用技能体系时、项目结构发生重大变化时、切换项目时。
inputs:
  - ${PROJECT_ROOT}
outputs:
  - ${KNOWLEDGE_ROOT}/references/项目结构速查.md
  - ${KNOWLEDGE_ROOT}/references/组件索引速查.md
  - ${KNOWLEDGE_ROOT}/references/代码规范速查.md
  - ${KNOWLEDGE_ROOT}/references/MANIFEST.md
tools:
  - bash: cat, ls, find, grep, head, tail, wc, date
  - ask_user
params:
  SKILLS_ROOT: "$(cd "$(dirname "$0")" && pwd)"
---

# M0: 项目知识提取 (Project Knowledge Extraction)

## 职责

读取项目源码，自动提取以下信息写入 `knowledge/{project-name}/references/`：

| 产出文件 | 内容 | 提取方式 |
|---------|------|---------|
| 项目结构速查.md | 模块列表、目录树、模块↔包路径映射表 | `ls` 根目录 + 解析包声明 |
| 组件索引速查.md | 公共基类、工具类、枚举、工厂类清单 | `grep` 框架/公共模块 |
| 代码规范速查.md | 编码风格、注解模式、继承链、命名规律 | 读 3-5 个典型文件归纳 |
| MANIFEST.md | 所有文件的索引 + basePackage | 汇总 |

## 执行流程

### Step 0: 确认项目根路径

```bash
# 询问用户项目源码的绝对路径
echo "请输入项目源码根目录的绝对路径（如 /home/user/my-project 或 D:/work/my-project）："
read PROJECT_ROOT
```

拿到路径后，执行 `ls "${PROJECT_ROOT}"` 确认目录存在且包含源码（至少有一个 pom.xml / build.gradle 或 src/ 目录）。

> 如果用户输入的路径无效，提示重新输入，最多重试 3 次后退出。

### Step 0.5: 确认项目名

```bash
echo "请给这个项目一个短名称（用于隔离知识包目录），如 xy-cloud、my-project："
read PROJECT_NAME
# 限定 kebab-case，小写字母和连字符
PROJECT_NAME=$(echo "${PROJECT_NAME}" | tr '[:upper:]' '[:lower:]' | sed 's/[^a-z0-9-]/-/g')
```

定义知识包输出路径：
```bash
KNOWLEDGE_ROOT="${SKILLS_ROOT}/knowledge/${PROJECT_NAME}"
mkdir -p "${KNOWLEDGE_ROOT}/references"
```

> **切换项目**：下次运行 M0 时输入不同的 `PROJECT_NAME`，知识包自动输出到不同子目录，互不干扰。

### Step 1: 提取 BASE_PACKAGE

从项目源码中找一个典型的 `.java` 文件，读取其 `package` 声明：

```bash
# 找一个业务模块下的 Java 文件（跳过 test 目录）
TARGET_FILE=$(find "${PROJECT_ROOT}" -path "*/src/main/java/*.java" ! -path "*/test/*" 2>/dev/null | head -5 | head -1)

# 读取 package 声明
BASE_PACKAGE=$(head -5 "${TARGET_FILE}" | grep "^package" | sed 's/package //;s/;//;s/ *$//')
# 进一步提取根包（取前两段或三段）
# 如 cn.shutan.platform.module.infra → cn.shutan.platform
BASE_PACKAGE_ROOT=$(echo "$BASE_PACKAGE" | sed 's/\([a-z0-9.]*\.[a-z0-9]*\)\.module\..*/\1/')
```

如果 `find` 找不到 Java 文件，提示用户手动输入 BASE_PACKAGE。

> **输出变量**：`basePackage`, `basePackageRoot`

### Step 2: 扫描模块结构

```bash
# 列出项目根目录
ls -d "${PROJECT_ROOT}"/*/

# 如果是 Maven 多模块项目，读 pom.xml 的 modules
grep -oP '<module>\K[^<]+' "${PROJECT_ROOT}/pom.xml" 2>/dev/null
```

对每个业务模块（含 `src/main/java` 的模块），提取：

```bash
# 模块名称（目录名）
MODULE_DIR=$(basename "${MODULE_PATH}")

# 模块的 Java 包路径
# find src/main/java 下的第一个目录层级
MODULE_PACKAGE=$(find "${MODULE_PATH}/src/main/java" -type d -name "java" -prune -o -type d -print 2>/dev/null | head -2 | tail -1)
```

> **输出**：模块列表（模块名 | 目录相对路径 | Java 包路径 | 类型（框架/业务/网关））

### Step 3: 提取组件索引

对框架/公共模块（如 `platform-common`、`platform-framework`），搜索关键组件：

```bash
# 基础工具类（以 Utils 结尾的 public 类）
grep -r "public class.*Utils" "${FRAMEWORK_DIR}/src/main/java" --include="*.java" 2>/dev/null

# 基类（以 Base/Abstract 开头）
grep -r "^public abstract class\|^public class.*extends Base" "${FRAMEWORK_DIR}/src/main/java" --include="*.java" 2>/dev/null

# 枚举
grep -r "^public enum" "${FRAMEWORK_DIR}/src/main/java" --include="*.java" 2>/dev/null

# 工厂类
grep -r "class.*Factory" "${FRAMEWORK_DIR}/src/main/java" --include="*.java" 2>/dev/null
```

对每个组件，读取它的包路径和用途（从类注释的 H1 或 `@description` 提取），整理为表格。

> **输出**：组件索引表（类名 | 包路径 | 用途描述）

### Step 4: 分析代码骨架

从业务模块中选 3 个典型 CRUD 实现（Controller / Service / Mapper 各一），分析：

```bash
# 找一个 Controller
find "${BIZ_DIR}/src/main/java" -name "*Controller.java" 2>/dev/null | head -1

# 找一个 Service 接口
find "${BIZ_DIR}/src/main/java" -name "*Service.java" ! -name "*Impl*" 2>/dev/null | head -1

# 找一个 Mapper
find "${BIZ_DIR}/src/main/java" -name "*Mapper.java" 2>/dev/null | head -1

# 找一个 DO
find "${BIZ_DIR}/src/main/java" -name "*DO.java" 2>/dev/null | head -1
```

从这些文件中提取：

| 模式 | 提取方式 |
|------|---------|
| 包路径格式 | 读 package 声明 |
| Controller 继承/注解 | grep `@RestController` `@RequestMapping` `@Tag` |
| Controller 返回值类型 | grep `CommonResult<` 或自定义返回类型 |
| Service 接口风格 | grep 方法签名（create/update/delete/get/page） |
| Mapper 继承 | grep `extends`（BaseMapperX / BaseMapper） |
| DO 继承 | grep `extends`（BaseDO / TenantBaseDO） |
| 分页参数 | grep `PageParam` / `PageReqVO` |
| 权限注解 | grep `@PreAuthorize` |
| VO 命名 | ls `*ReqVO*` `*RespVO*` |
| 缓存注解 | grep `@Cacheable` `@CacheEvict` |

> **输出**：代码规范速查.md（含提取到的所有模式）

### Step 5: 检测测试风格

```bash
# 找测试文件
find "${PROJECT_ROOT}" -name "*Test.java" 2>/dev/null | head -5

# 如果是空（没有测试）
if [ $? -ne 0 ] || [ -z "$(find "${PROJECT_ROOT}" -name "*Test.java" 2>/dev/null | head -1)" ]; then
  echo "项目中暂无测试文件，测试规范暂不生成"
  TEST_STYLE="none"
else
  # 读第一个测试文件，判断框架
  TEST_FILE=$(find "${PROJECT_ROOT}" -name "*Test.java" 2>/dev/null | head -1)
  if grep -q "Mockito" "${TEST_FILE}"; then TEST_FRAMEWORK="Mockito"; fi
  if grep -q "SpringBootTest" "${TEST_FILE}"; then TEST_FRAMEWORK="SpringBootTest"; fi
  if grep -q "BaseMockitoUnitTest" "${TEST_FILE}"; then TEST_FRAMEWORK="BaseMockitoUnitTest"; fi
  TEST_STYLE="${TEST_FRAMEWORK}"
fi
```

### Step 6: 检测框架组件

扫描项目使用的框架组件（决定 M1 Q4 提示什么、M3 推荐什么）：

```bash
# 缓存
grep -r "cache\|@Cacheable\|@CacheEvict\|RedisTemplate" "${FRAMEWORK_DIR}/src/main/java" --include="*.java" 2>/dev/null | head -1
HAS_CACHE=$?

# 多租户
grep -r "tenant\|TenantContext\|TenantBaseDO" "${FRAMEWORK_DIR}/src/main/java" --include="*.java" 2>/dev/null | head -1
HAS_TENANT=$?

# 安全框架
grep -r "SecurityUtils\|@PreAuthorize\|@SaCheckPermission" "${FRAMEWORK_DIR}/src/main/java" --include="*.java" 2>/dev/null | head -1
HAS_SECURITY=$?

# MQ/消息
grep -r "RabbitTemplate\|RocketMQ\|Spring Event\|ApplicationEventPublisher" "${FRAMEWORK_DIR}/src/main/java" --include="*.java" 2>/dev/null | head -1
HAS_MQ=$?

# 定时任务
grep -r "XxlJob\|@Scheduled\|ScheduledTask" "${FRAMEWORK_DIR}/src/main/java" --include="*.java" 2>/dev/null | head -1
HAS_JOB=$?

# Excel
grep -r "EasyExcel\|ExcelUtils\|@Excel" "${FRAMEWORK_DIR}/src/main/java" --include="*.java" 2>/dev/null | head -1
HAS_EXCEL=$?

# Feign/RPC
grep -r "FeignClient\|@FeignClient\|OpenFeign" "${FRAMEWORK_DIR}/src/main/java" --include="*.java" 2>/dev/null | head -1
HAS_FEIGN=$?
```

### Step 7: 检测框架层目录树

```bash
# 框架模块的目录树（展示继承关系）
FRAMEWORK_DIR=$(find "${PROJECT_ROOT}" -maxdepth 2 -type d -name "*framework*" -o -name "*common*" 2>/dev/null | head -1)
if [ -n "${FRAMEWORK_DIR}" ]; then
  # 找核心包结构
  find "${FRAMEWORK_DIR}/src/main/java" -type d -maxdepth 4 2>/dev/null
fi
```

### Step 8: 写入输出文件

将 Step 1~7 的提取结果整理为 markdown 文件，写入 `${KNOWLEDGE_ROOT}/references/`。

#### 8.1 写入 项目结构速查.md

```markdown
# {项目名} 项目目录结构与生成文件映射

> 根包：`${basePackageRoot}` | M0 生成时间：{current-date}
> 此文件由 M0 自动生成，项目结构变化后重新运行 M0 更新。

## 1. 项目模块全景

{Step 2 提取的模块列表表}

## 2. 业务模块内部结构

{Step 2 提取的典型模块目录树}

## 3. 模块路径速查表

{Step 2 提取的模块路径映射表}

## 4. 框架能力检测

{Step 6 检测结果}

| 能力 | 状态 |
|------|------|
| 缓存 | ✅/❌ |
| 多租户 | ✅/❌ |
| 安全框架 | {类型} |
| MQ/消息 | {类型} |
| 定时任务 | {类型} |
| Excel | ✅/❌ |
| Feign RPC | ✅/❌ |
```

#### 8.2 写入 组件索引速查.md

```markdown
# {项目名} 可复用组件索引

> 根包：`${basePackageRoot}` | M0 生成时间：{current-date}
> 此文件由 M0 自动生成，新增公共组件后重新运行 M0 更新。

## 1. 通用基础类

{Step 3 提取的组件表，按模块分组}
```

#### 8.3 写入 代码规范速查.md

```markdown
# {项目名} 代码规范（源码归纳）

> 此文件由 M0 从现有源码中自动归纳，反映项目实际使用的编码约定，不一定是团队书面规范。
> 如团队有正式规范文档，以正式文档为准。

{Step 4 提取到的代码模式，每项含"推断来源"（引用具体文件）}
```

#### 8.4 更新 MANIFEST.md

```markdown
# 参考文档清单

## 项目配置
| 逻辑名称 | 值 | 说明 |
|---------|-----|------|
| base-package | ${basePackageRoot} | Java 包根，供模板填充 |

## 文档索引
| 逻辑名称 | 文件名 | 来源 | 被使用 |
|----------|--------|------|--------|
| project-map | 项目结构速查.md | M0 自动生成 | M4 |
| components-index | 组件索引速查.md | M0 自动生成 | M3, M4 |
| code-standards | 代码规范速查.md | M0 自动生成 | M3, M4 |
| ... | ...来自现有的手动文档... | 手写 | ... |

## 使用说明

- 来源为"M0 自动生成"的文件：项目结构变化后重新运行 M0 更新
- 来源为"手写"的文件：团队规范变更时手动修改
- 所有技能启动时均先读此文件获取路径和 basePackage
```

### Step 8.5: 生成 project-index.yaml（机器可读）

在参考文档之外，生成一份机器可读的 YAML 索引文件，供 M4 做路径验证使用。

```yaml
# 项目索引 — 由 M0 自动生成，供 M4 契约生成做路径验证
# 修改项目结构后重新运行 M0 更新此文件
project_name: ${PROJECT_NAME}
project_root: ${PROJECT_ROOT}
base_package: ${BASE_PACKAGE_ROOT}
generated: "${CURRENT_DATE}"

modules:
${MODULE_ENTRIES}
  # 每个模块的格式：
  # {module_name}:
  #   name: {module_name}
  #   path: {relative_path}
  #   src_main_java: {abs_path}/src/main/java/{package}
  #   package: {java_package}
  #   type: framework | business | gateway | unknown
```

生成逻辑：
```bash
# 从 Step 2 的扫描结果构建 YAML
YAML_FILE="${KNOWLEDGE_ROOT}/references/project-index.yaml"

echo "# 项目索引 — 由 M0 自动生成" > "${YAML_FILE}"
echo "# 修改项目结构后重新运行 M0 更新此文件" >> "${YAML_FILE}"
echo "project_name: ${PROJECT_NAME}" >> "${YAML_FILE}"
echo "project_root: ${PROJECT_ROOT}" >> "${YAML_FILE}"
echo "base_package: ${BASE_PACKAGE_ROOT}" >> "${YAML_FILE}"
echo "generated: \"$(date +%Y-%m-%d)\"" >> "${YAML_FILE}"
echo "" >> "${YAML_FILE}"
echo "modules:" >> "${YAML_FILE}"

# 遍历模块输出结构
for MODULE_PATH in "${MODULE_PATHS[@]}"; do
  MODULE_NAME=$(basename "${MODULE_PATH}")
  MODULE_PACKAGE=$(find "${MODULE_PATH}/src/main/java" -type d -name "java" -prune -o -type d -print 2>/dev/null | head -2 | tail -1)
  MODULE_RELATIVE=$(echo "${MODULE_PATH}" | sed "s|${PROJECT_ROOT}/||")
  
  # 判断模块类型
  if echo "${MODULE_PATH}" | grep -qiE "framework|common|core"; then
    MODULE_TYPE="framework"
  elif echo "${MODULE_PATH}" | grep -qiE "gateway|app"; then
    MODULE_TYPE="gateway"
  else
    MODULE_TYPE="business"
  fi
  
  echo "  ${MODULE_NAME}:" >> "${YAML_FILE}"
  echo "    name: ${MODULE_NAME}" >> "${YAML_FILE}"
  echo "    path: ${MODULE_RELATIVE}" >> "${YAML_FILE}"
  
  if [ -d "${MODULE_PATH}/src/main/java" ]; then
    SRC_PATH=$(find "${MODULE_PATH}/src/main/java" -type d | head -1)
    echo "    src_main_java: ${SRC_PATH}" >> "${YAML_FILE}"
  fi
  
  echo "    package: ${MODULE_PACKAGE}" >> "${YAML_FILE}"
  echo "    type: ${MODULE_TYPE}" >> "${YAML_FILE}"
done
```

此文件会被 M4 自动加载用于路径验证。如果项目没有此文件，M4 会降级为手动输入目标路径。

### Step 8.6: 生成上手指南（给人看）

基于扫描结果生成两篇对人友好的文档，写入 `knowledge/{project}/上手指南/`。

```bash
ONBOARDING_DIR="${KNOWLEDGE_ROOT}/上手指南"
mkdir -p "${ONBOARDING_DIR}"
```

**你的任务**：你刚刚扫描完这个项目，掌握了它的模块结构、代码模式和框架能力。现在请你以这些信息为基础，生成两份文档。

目标读者是**刚入职的新人**——技术基础有一些，但第一次接触这个项目，不知道从哪里开始、写代码时容易踩什么坑。

> 核心原则：
> - **不写模板，不下结论**。文档里的每件事，都必须基于你实际扫描到的代码。
> - **每项结论带证据**。比如提到"DO 继承 TenantBaseDO"，附上项目中的真实 DO 文件路径。
> - **不存在的内容不提**。如果项目没有 JSON 字段，就不说 `autoResultMap`。如果项目只用 MySQL，就不提 `@KeySequence`。

#### 8.6.1 写入 项目手册.md

回答三个问题：

1. **这个项目是做什么的？** → 一句话定位 + 技术栈摘要
2. **有哪些模块，各管什么？** → 按业务/框架/网关分组，每个模块一句话
3. **写一个新功能要几步？** → 找一个项目里最完整的 CRUD 例子（从 DO 到 Controller 的完整链路），以这个例子为线索，写出 8 步开发流程。每一步指向项目中的真实文件路径，说明框架自动做了哪些事。

#### 8.6.2 写入 避坑指南.md

**只写你从代码中发现确实存在的坑**。包括但不限于：

- DO 继承体系（BaseDO vs TenantBaseDO 都在用 → 说明各自的使用场景和选错后果。如果只用了其中一种，不提另一种）
- 框架自动处理但新人不知道的特性（逻辑删除自动过滤、字段自动填充等）
- 你扫描中注意到的可能导致运行时错误的模式

每个条目都附上**证据路径**（在哪看到了相关代码）。没有证据的不写。

#### 8.6.3 写入文件

将你生成的这两个文档写入：

```bash
cat > "${ONBOARDING_DIR}/项目手册.md"
cat > "${ONBOARDING_DIR}/避坑指南.md"
```

### Step 9: 告知用户

```
知识包已生成到 `${KNOWLEDGE_ROOT}/references/`，包含：
  - 项目结构速查.md（{N} 个模块）
  - 组件索引速查.md（{N} 个组件）
  - 代码规范速查.md（从源码归纳的编码约定）
  - project-index.yaml（机器可读索引，供 M4 路径验证）
  - MANIFEST.md（索引 + basePackage）

上手指南已生成到 `${KNOWLEDGE_ROOT}/上手指南/`，包含：
  - 项目手册.md（模块功能 + CRUD 流程）
  - 避坑指南.md（框架陷阱 + 注解检查清单）

后续 M1~M7 将自动读取知识包运行。
如需更新（如新增模块/重构），重新运行 M0 即可。
```

## 注意事项

1. **M0 是只读的**——只读源码，不修改任何项目文件
2. **知识包可以被覆盖**——相同项目重新运行 M0 会覆盖已有文件
3. **多项目共存**——切换项目时重新跑 M0，知识包自然替换
4. **无知识包时**——M1~M7 退化为纯方法论（每一步直接问用户），不阻塞
