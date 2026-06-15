# xy-cloud Starter 开发规范速查

> 环境：Spring Boot 2.7.18 | JDK 8 | 根包：`cn.shutan.platform.framework`
> 参考：`platform-spring-boot-starter-*` 系列（见 `组件索引速查.md`）

---

## 1. Starter 模块目录结构

每个 Starter 是一个独立 Maven 模块，位于 `platform-framework/` 下：

```
platform-framework/
└── platform-spring-boot-starter-{name}/
    └── src/main/java/cn/shutan/platform/framework/{name}/
        ├── config/
        │   ├── XxxAutoConfiguration.java    # 自动配置（核心）
        │   └── XxxProperties.java           # 配置属性绑定（可选）
        ├── core/                             # 核心逻辑
        ├── xxx/                              # 功能分包
        └── enums/                            # 模块枚举（可选）

    src/main/resources/
    └── META-INF/spring/
        └── org.springframework.boot.autoconfigure.AutoConfiguration.imports  # SPI 注册
```

### pom.xml 关键配置

```xml
<artifactId>platform-spring-boot-starter-{name}</artifactId>

<dependencies>
    <!-- 最小依赖原则：只引入必要项 -->
    <dependency>
        <groupId>cn.shutan.platform</groupId>
        <artifactId>platform-spring-boot-starter-web</artifactId>  <!-- 按需 -->
    </dependency>
    <!-- 无特殊需求时仅依赖 spring-boot-starter -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

---

## 2. AutoConfiguration 设计模式

### 2.1 核心注解组合

| 注解 | 说明 |
|------|------|
| `@AutoConfiguration` | Spring Boot 2.7+ 替代 `@Configuration`，配合新的 SPI 机制 |
| `@ConditionalOnClass` | classpath 存在指定类时才加载（解耦可选依赖） |
| `@ConditionalOnMissingBean` | 容器中没有该 Bean 时才创建（允许用户覆盖） |
| `@ConditionalOnProperty` | 配置项控制开关 |
| `@EnableConfigurationProperties` | 激活 `@ConfigurationProperties` 绑定 |

### 2.2 模板

```java
@AutoConfiguration
@ConditionalOnClass({RedisTemplate.class})               // 可选依赖检查
@EnableConfigurationProperties({XxxProperties.class})    // 属性绑定
public class XxxAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean                             // 允许覆盖
    public XxxTemplate xxxTemplate(XxxProperties properties) {
        return new XxxTemplate(properties);
    }

}
```

### 2.3 条件注解速查

| 注解 | 触发条件 | 典型场景 |
|------|---------|---------|
| `@ConditionalOnClass` | classpath 存在指定类 | 可选依赖解耦（如 MQ 类型判断） |
| `@ConditionalOnMissingBean` | 容器不存在指定 Bean | 提供默认实现，允许用户覆盖 |
| `@ConditionalOnBean` | 容器存在指定 Bean | 依赖其他 Starter 的 Bean |
| `@ConditionalOnProperty` | 配置项匹配 | 功能开关控制 |
| `@ConditionalOnWebApplication` | 当前为 Web 应用 | Web 专属配置 |
| `@ConditionalOnMissingClass` | classpath 不存在指定类 | 降级配置 |

**条件判断顺序**：`@ConditionalOnClass` 应放在类级别（控制整个配置类是否加载），`@ConditionalOnMissingBean` 放在方法级别（控制单个 Bean）。

---

## 3. SPI 注册方式

### 3.1 Spring Boot 2.7+（当前项目使用）

**文件路径**：
```
src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

**文件内容**（每行一个全限定类名）：
```
cn.shutan.platform.framework.xxx.config.XxxAutoConfiguration
```

### 3.2 传统方式（spring.factories，兼容旧版）

```
src/main/resources/META-INF/spring.factories
```

```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
cn.shutan.platform.framework.xxx.config.XxxAutoConfiguration
```

> 本项目使用 Spring Boot 2.7.18，采用新的 `AutoConfiguration.imports` 方式。旧 `spring.factories` 方式仍兼容但不推荐。

---

## 4. @ConfigurationProperties 属性绑定

### 4.1 模板规范

```java
@ConfigurationProperties(prefix = "platform.xxx")   // 统一 platform. 前缀
@Validated
public class XxxProperties {

    /** 主机地址 */
    @NotEmpty(message = "主机地址不能为空")
    private String host = "127.0.0.1";

    /** 端口号 */
    @NotNull(message = "端口不能为空")
    private Integer port = 8080;

    // getters & setters 必须
}
```

### 4.2 属性前缀命名规范

| 类型 | 前缀格式 | 示例 |
|------|---------|------|
| 框架级 | `platform.{name}` | `platform.redis`、`platform.mybatis` |
| 业务级 | `platform.{biz}` | `platform.tenant`、`platform.data-permission` |

### 4.3 IDEA Metadata 提示

添加依赖后自动生成 `spring-configuration-metadata.json`：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-configuration-processor</artifactId>
    <optional>true</optional>
</dependency>
```

---

## 5. 设计原则

### 5.1 最小依赖

- Starter 只依赖必要框架包，业务模块自行添加业务依赖
- 可选集成使用 `@ConditionalOnClass` 解耦

### 5.2 自动配置但可覆盖

- 提供开箱即用的自动配置
- 用户可通过 `@ConditionalOnMissingBean` 覆盖默认 Bean
- 所有配置项应有合理默认值

### 5.3 无侵入

- Starter 不应强制修改用户代码
- 通过 Filter/Interceptor/MetaObjectHandler 等扩展点生效
- 不要求业务 Bean 继承特定 Starter 类（除非必要）

### 5.4 示例：MyBatis Starter 结构

参考 `platform-spring-boot-starter-mybatis`：

| 组件 | 说明 |
|------|------|
| `BaseMapperX` | Mapper 基类（扩展 MPJBaseMapper） |
| `BaseDO` | DO 基类（含审计字段） |
| `DefaultDBFieldHandler` | 自动填充处理器 |
| `LambdaQueryWrapperX` | 查询包装器增强（eqIfPresent 等） |
| `EncryptTypeHandler` | 加密处理器 |

---

## 6. 现有 Starter 速查

| Starter | 核心 Bean | 条件注解 | 描述 |
|---------|----------|---------|------|
| mybatis | BaseMapperX / MetaObjectHandler | — | MyBatis-Plus 增强 |
| web | GlobalExceptionHandler / LoginUserFilter | `@ConditionalOnWebApplication` | Web 层统一处理 |
| security | SecurityFrameworkUtils / AuthFilter | `@ConditionalOnProperty` | 认证授权 |
| redis | RedisTemplate / CacheConfig | `@ConditionalOnClass` | Redis 配置 |
| mq | RedisMQTemplate / StreamConsumer | `@ConditionalOnClass` | 消息队列 |
| protection | IdempotentAspect / RateLimiterAspect | `@ConditionalOnProperty` | 防重/限流 |
| tenant | TenantDatabaseInterceptor | `@ConditionalOnProperty` | 多租户 |
| data-permission | DataPermissionInterceptor | `@ConditionalOnProperty` | 数据权限 |

---

## 7. 开发流程

1. **建模块** — 在 `platform-framework/` 下创建 `platform-spring-boot-starter-{name}` 目录
2. **配 pom** — 添加最小依赖，引入 `spring-boot-starter` 及必要框架包
3. **写配置** — 创建 `XxxProperties.java`（`@ConfigurationProperties`）
4. **写自动配置** — 创建 `XxxAutoConfiguration.java`（`@AutoConfiguration` + `@Bean`）
5. **注册 SPI** — 在 `META-INF/spring/` 创建 `AutoConfiguration.imports`
6. **加处理器** — 可选添加 `spring-boot-configuration-processor`（IDE 提示）
7. **编译验证** — `mvn compile -pl platform-spring-boot-starter-{name}`

---

## 8. 注意事项

1. `@AutoConfiguration` 在 Spring Boot 2.7+ 替换 `@Configuration` 用于自动配置类
2. `AutoConfiguration.imports` 文件位置固定，无后缀名
3. `@ConfigurationProperties` 的 Bean 需通过 `@EnableConfigurationProperties` 注册
4. `spring.factories` 方式已废弃，新项目使用 `AutoConfiguration.imports`
5. Starter 不要自动扫描包（`@ComponentScan`），通过 `@Bean` 显式注册
