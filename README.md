# xy-cloud-skills

微技能引擎 + xy-cloud 项目知识包。包含 8 个微技能（M0~M7），支持多项目隔离。

> 项目特有信息（源码路径、模块列表、技术栈）已迁移到 `skills/knowledge/xy-cloud/references/`，
> 由 M0 自动管理，不再硬编码在此文件中。

## 目录结构

```
xy-cloud-skills/
├── README.md                # 技能仓库入口
├── skills/
│   ├── README.md            # 技能体系说明
│   ├── m0-项目扫描/              # 项目知识提取（M0）
│   ├── m1-需求澄清/              # 需求澄清（M1）
│   ├── m2-数据模型/              # 数据模型设计（M2）
│   ├── m3-组件查询/              # 组件查询（M3）
│   ├── m4-生成契约/              # 生成实现契约（M4）
│   ├── m5-边界分析/              # 边界分析（M5）
│   ├── m6-测试任务/              # 测试任务（M6）
│   ├── m7-测试执行/              # 测试执行（M7）
│   ├── knowledge/               # M0 知识包输出，按项目隔离
│   │   └── xy-cloud/references/ #     当前项目知识
│   └── shared/
│       ├── references/          #     手写规范（跨项目通用）
│       ├── templates/           #     代码模板（${basePackage}）
│       └── outputs/             #     流水线通信
└── 开发参考手册/            # 项目规范参考（完整版）
```

## 使用方式

在 skills/ 目录下针对具体微技能启动 Claude Code：

```
cd skills/m1-需求澄清/
claude . --skill SKILL.md
```

完整开发流程：M1 → M2 → M3 → M4 → M5 → M6 → M7
