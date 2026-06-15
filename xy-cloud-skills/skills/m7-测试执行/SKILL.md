---
name: m7-test-execution
description: 解读测试报告，分析失败原因，给出修复建议。输入测试输出，输出结构化分析报告。
trigger: 用户说"分析测试失败"、"看看测试报告"、"测试为什么挂了"时激活。
inputs:
  - ${SKILLS_ROOT}/shared/references/MANIFEST.md → test-standards
  - $(cat "${SKILLS_ROOT}/knowledge/${PROJECT_NAME}/references/项目结构速查.md" 2>/dev/null || echo "知识包不存在，需运行时询问用户")
outputs:
  - ${SKILLS_ROOT}/shared/outputs/m7-测试报告/tr-{tag}-{module}-{date}.md
  - ${SKILLS_ROOT}/m7-测试执行/outputs/tr-{tag}-{module}-{date}-{timestamp}.md
tools:
  - bash: cat, ls, grep, date
  - ask_user
params:
  SKILLS_ROOT: "$(cd \"$(dirname \"$0\")\" && pwd)"
# 启动时自动读 MANIFEST.md + 检测知识包
---

# M7: 测试报告解读 (Test Report Analysis)

M7 **不执行测试**。M7 的任务是分析用户提供的测试输出（本地运行结果、CI 日志、Surefire 报告等），
分类失败原因、交叉引用源码上下文、给出可操作的修复建议。

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
fi
```

## 流程

### Q0: 需求标识 {tag}

> 这次要分析哪个需求的测试报告？（输入 tag，如 `oss-file-client`）
>
> 如果已有 M5/M6 产物，可从文件名提取：
> ```bash
> ls "${SKILLS_ROOT}/shared/outputs/m6-测试任务/"*.md 2>/dev/null | sed 's/.*\/\(.*\)\.md/\1/'
> ```

### Q1: 输入测试报告

> 将测试输出粘贴给我，或提供以下类型的文件路径：
>
> ```
> [1] 终端标准输出（从 mvn test / gradle test 复制）
> [2] Surefire XML 报告路径（target/surefire-reports/*.xml）
> [3] CI 控制台日志文本
> [4] 直接粘贴失败栈信息
> [5] 已有测试源文件路径（我来分析代码层面的问题）
> ```
>
> **提示**：仅粘贴失败部分即可，不需要全部输出。

### Q2: 上下文补充（可选）

> 这个测试之前跑过吗？是新增的测试还是一直失败？
>
> ```
> [1] 新增测试，首次运行
> [2] 回归失败（之前通过，现在挂了）
> [3] 间歇性失败（有时通过有时不通过）
> [4] 不确定
> ```

## 分析引擎

收到测试输出后，按以下维度分类并分析：

### 1. 失败分类

| 类别 | 特征关键词 | 优先级 |
|------|-----------|--------|
| **断言失败** | `AssertionError`, `expected:`, `but was:`, `Expected:`, `Actual:` | 高 |
| **空指针** | `NullPointerException`, `NullPointer` | 高 |
| **Bean/Mock 配置** | `BeanCreationException`, `UnsatisfiedDependencyException`, `MissingBean` | 高 |
| **数据库** | `SQLException`, `DataIntegrityViolation`, `TransactionSystemException` | 中 |
| **环境/网络** | `ConnectException`, `SocketTimeout`, `UnknownHost` | 中 |
| **Mock 行为缺失** | `UnnecessaryStubbingException`, `MissingMethodInvocation` | 中 |
| **配置错误** | `IllegalArgumentException`, `IllegalStateException` | 中 |
| **超时** | `TimeoutException`, `TestTimedOutException` | 低 |
| **文件/IO** | `FileNotFoundException`, `IOException` | 低 |

### 2. 源码交叉引用（有知识包时）

从失败堆栈中提取失败的 assert 行或异常抛出行，映射到源码：

```bash
# 从栈中取第一个项目内的调用位置
FAIL_SOURCE=$(echo "${STACK_TRACE}" | grep -oP "${BASE_PACKAGE//\./\\.}\.[^:]+:\d+" | head -1)
if [ -n "${FAIL_SOURCE}" ] && [ -n "${PROJECT_NAME}" ]; then
  FAIL_FILE=$(echo "${FAIL_SOURCE}" | cut -d: -f1 | tr '.' '/')
  FAIL_LINE=$(echo "${FAIL_SOURCE}" | cut -d: -f2)
  # 从知识包的企业模块映射表反向定位物理文件
fi
```

### 3. 变更影响分析（回归失败）

如果用户标记为回归失败，分析：

> 上次提交涉及哪些代码变更？
> 检查关联的 Service / Mapper / Controller 是否有相关改动（由用户补充或手动提供变更范围）

### 4. 修复建议生成

对每种失败类型给出具体建议：

| 失败类型 | 分析要点 | 建议模板 |
|----------|---------|---------|
| 断言失败 | 对比 expected 和 actual 的值差异 | "预期 {X} 但得到 {Y}，检查 {被测方法} 的 {条件} 逻辑" |
| 空指针 | 找 null 对象的类型 | "{对象} 为 null，确保 Mock 了 {对象} 的 {方法} 或补全 @Mock 注解" |
| Bean 配置 | 找缺失的 Bean 类型 | "缺少 {Bean}，检查测试类是否加了 @SpringBootTest 或 @MockBean({Bean}.class)" |
| Mock 缺失 | 找未 Mock 的调用链 | "Mock 了 {A} 但未配置 when({A}.{method})，补充 thenReturn/thenThrow" |

## 输出

### 1. 写入本地归档（存档轨）
将完整内容写入 `${SKILLS_ROOT}/m7-测试执行/outputs/tr-{tag}-{module}-{date}-{timestamp}.md`

### 2. 同步到流水线总线（总线轨）
将完整内容写入 `${SKILLS_ROOT}/shared/outputs/m7-测试报告/tr-{tag}-{module}-{date}.md`
- 按 CLAUDE.md 规范更新 `shared/outputs/STATUS.md`

格式：

```markdown
---
tag: {tag}
type: test-report
module: {module}
version: v1
created: {current-date}
---

# [测试报告] {module} - {date}

## 输入来源
- 类型: {终端输出 / Surefire 报告 / CI日志 / 源码分析}
- 范围: {测试类或包名}

## 失败概览
- 总计: X | 通过: Y | 失败: Z | 成功率: {Y/X}%

## 失败详情

### 失败 1: {TestClass}.{testMethod}
- **类型**: {断言失败 / 空指针 / Mock 配置 ...}
- **错误信息**: {关键一行}
- **源码位置**: {文件:行号}
- **原因分析**: {推理过程}
- **建议修复**:
  ```java
  // 示例代码或配置修改
  ```

### 失败 2: ...

## 模式总结（多次分析后生效）
- **高频失败模式**: {反复出现的失败类型}
- **趋势**: {新增/减少/持续}
- **建议改进方向**: {测试基础设施 / Mock 策略 / 测试数据准备}

## 结论

{简要总结，哪些问题可以快速修复，哪些需要深入排查}
```
