# xy-cloud API 集成规范参考

> **来源**: xy-cloud 项目源码（`platform-spring-boot-starter-rpc`、`platform-spring-boot-starter-mq`、`platform-module-*-api`、`platform-module-*-biz`）
>
> **技术栈**: Spring Cloud OpenFeign + Redis Pub/Sub + RabbitMQ / RocketMQ + WebSocket
>
> **说明**: 本文档系统梳理了 xy-cloud 项目中模块间 API 集成的全部核心机制与规范，涵盖 Feign RPC 同步调用、消息队列异步通信、WebSocket 实时推送三种集成方式，供开发者查阅和遵循。

---

## 1. API 集成体系全景

xy-cloud 项目采用微服务架构，模块间通信支持三种方式，分别适用于不同的业务场景：

| 集成方式 | 通信模式 | 适用场景 | 核心依赖 |
|---------|---------|---------|---------|
| Feign RPC | 同步请求-响应 | 实时查询、数据校验、跨模块业务编排 | `platform-spring-boot-starter-rpc` |
| 消息队列 (MQ) | 异步发布-订阅 | 事件通知、日志记录、解耦业务 | `platform-spring-boot-starter-mq` |
| WebSocket | 服务端主动推送 | 实时通知、数据推送、消息提醒 | `platform-spring-boot-starter-websocket` |

三种方式的选型原则：

- **Feign RPC** 用于需要实时返回结果的场景，如获取用户信息、校验数据、跨模块查询等。
- **消息队列** 用于不需要实时返回结果的场景，如操作日志记录、异步通知、事件驱动等。
- **WebSocket** 用于服务端主动推送消息到客户端的场景，如实时告警、消息提醒等。

---

## 2. Feign 接口定义规范

### 2.1 RPC 基础常量

**类路径**: `cn.shutan.platform.framework.common.enums.RpcConstants`

```java
public interface RpcConstants {
    String RPC_API_PREFIX = "/rpc-api";
}
```

所有 Feign 接口的路径统一以 `/rpc-api` 为前缀，与普通 HTTP REST 接口（通常通过 Gateway 暴露）区分开，避免路径冲突，同时便于在安全配置中统一放行。

### 2.2 ApiConstants 常量定义

每个模块的 api 子模块中均定义一个 `ApiConstants` 接口，声明该模块的基本信息：

| 常量 | 说明 | 示例值 |
|------|------|--------|
| `NAME` | 服务注册名称，对应 Feign 的 `name` 属性 | `system-server` |
| `PREFIX` | RPC 路径前缀 | `/rpc-api/system` |
| `VERSION` | API 版本号 | `1.0.0` |

各模块的实际定义：

```java
// platform-module-system-api/enums/ApiConstants.java
public interface ApiConstants {
    String NAME = "system-server";
    String PREFIX = RpcConstants.RPC_API_PREFIX + "/system";
    String VERSION = "1.0.0";
}

// platform-module-infra-api/enums/ApiConstants.java
NAME = "infra-server", PREFIX = "/rpc-api/infra"

// platform-module-eos-api/enums/ApiConstants.java
NAME = "eos-server", PREFIX = "/rpc-api/eos"

// platform-module-eos-demand-response-api/enums/ApiConstants.java
NAME = "demand-server", PREFIX = "/rpc-api/demand"
```

> **说明**: `NAME` 对应 Spring Cloud 注册中心中的服务名，Feign 通过该名称进行服务发现和负载均衡。`PREFIX` 用于统一所有 Feign 接口的路径前缀，避免每个接口重复编写公共前缀。

### 2.3 @FeignClient 注解

Feign 客户端接口使用 `@FeignClient` 注解声明，标准模板如下：

```java
@FeignClient(name = ApiConstants.NAME)
// TODO 芋艿：fallbackFactory =
public interface DeptApi {
    String PREFIX = ApiConstants.PREFIX + "/dept";

    @GetMapping(PREFIX + "/list-all")
    CommonResult<List<DeptRespDTO>> getDeptList();

    @GetMapping(PREFIX + "/get")
    CommonResult<DeptRespDTO> getDept(@RequestParam("id") Long id);
}
```

关键规范点：

| 规范项 | 说明 |
|--------|------|
| `name` 属性 | 固定引用 `ApiConstants.NAME`，与服务注册名保持一致 |
| `PREFIX` 常量 | 在接口内定义一个 `PREFIX` 常量，值为 `ApiConstants.PREFIX + "/{entity}"` |
| HTTP 方法注解 | 使用 `@GetMapping`、`@PostMapping`、`@PutMapping`、`@DeleteMapping` |
| 参数注解 | 必须显式使用 `@RequestParam`、`@RequestBody`、`@PathVariable` |
| 返回值类型 | 统一使用 `CommonResult<T>` |

> **注意**: OpenFeign 要求请求参数注解必须显式声明，否则会报错。`@RequestParam("id")` 中的 value 值不可省略。

### 2.4 DTO / VO 命名差异

不同模块在请求/响应对象的命名上存在差异，这与模块的开发阶段或团队风格有关：

**system / infra 模块 — 使用 DTO 后缀**:

| 命名格式 | 示例 |
|---------|------|
| `XxxReqDTO` | `FileCreateReqDTO`, `LoginLogCreateReqDTO` |
| `XxxRespDTO` | `FileRespDTO`, `AdminUserRespDTO`, `DeptRespDTO` |
| 存放位置 | `dto/` 子包 |

**eos 模块 — 使用 VO 后缀**:

| 命名格式 | 示例 |
|---------|------|
| `XxxReqVO` | `UserLoadCurveReqVO` |
| `XxxRespVO` | `DeviceBasicInfoRespVO`, `DeptBasicRespVO` |
| 存放位置 | `vo/` 子包 |

> **说明**: 尽管命名风格不统一，但两种模式的职责相同——`Req` 后缀表示请求参数封装，`Resp` 后缀表示响应数据封装。开发新模块时建议与所依赖模块的风格保持一致。

### 2.5 CommonResult 统一响应

所有 Feign 接口方法的返回值必须使用 `CommonResult<T>` 包装，实现统一的结果处理和异常传播。

**类路径**: `cn.shutan.platform.framework.common.pojo.CommonResult`

```java
public class CommonResult<T> {
    private Integer code;    // 状态码（0=成功，非0=异常）
    private String msg;      // 提示消息
    private T data;          // 数据载体
}
```

**核心方法**:

| 方法 | 说明 |
|------|------|
| `isSuccess()` | 判断调用是否成功（`code == 0`） |
| `checkError()` | 调用失败时抛出 `ServiceException`，中断当前流程 |
| `getCheckedData()` | 检查并获取数据——若失败则抛出异常，成功则返回 `data` |
| `success(data)` | 静态工厂方法，创建成功结果 |

**使用示例**:

```java
// 推荐：使用 getCheckedData() — 失败直接抛异常，无需手动判断
List<DeptRespDTO> depts = deptApi.getDeptList().getCheckedData();

// 等效于：
CommonResult<List<DeptRespDTO>> result = deptApi.getDeptList();
if (result.isSuccess()) {
    List<DeptRespDTO> depts = result.getData();
} else {
    throw new ServiceException(result.getCode(), result.getMsg());
}
```

> **说明**: `getCheckedData()` 是推荐的使用方式，它在远程调用失败时直接将异常抛出，简化了调用方的错误处理代码。`checkError()` 适用于需要额外处理后再抛出的场景。

---

## 3. Feign 自动配置

### 3.1 框架级 RpcAutoConfiguration

`platform-spring-boot-starter-rpc` 模块提供了一系列自动配置类，在服务启动时自动注册通用的 Feign 客户端：

**类路径**: `cn.shutan.platform.framework.rpc.config`

| 自动配置类 | 注册的 Feign 客户端 | 功能说明 |
|-----------|-------------------|---------|
| `PlatformSecurityRpcAutoConfiguration` | `OAuth2TokenApi`, `PermissionApi` | 安全认证 + 登录用户拦截器 |
| `PlatformOperateLogRpcAutoConfiguration` | `OperateLogApi` | 操作日志 |
| `PlatformApiLogRpcAutoConfiguration` | `ApiAccessLogApi`, `ApiErrorLogApi` | API 访问日志和错误日志 |
| `PlatformDictRpcAutoConfiguration` | `DictDataApi` | 字典数据 |
| `PlatformTenantRpcAutoConfiguration` | `TenantApi` | 多租户 + 租户拦截器 |
| `PlatformDataPermissionRpcAutoConfiguration` | (拦截器) | 数据权限请求拦截器 |
| `PlatformEnvRpcAutoConfiguration` | (拦截器) | 环境标签请求拦截器 |

**典型实现**（以安全模块为例）:

```java
@Configuration
@EnableFeignClients(clients = {OAuth2TokenApi.class, PermissionApi.class})
public class PlatformSecurityRpcAutoConfiguration {

    @Bean
    public LoginUserRequestInterceptor loginUserRequestInterceptor() {
        return new LoginUserRequestInterceptor();
    }
}
```

> **说明**: 框架级自动配置的 Feign 客户端对所有引入 `platform-spring-boot-starter-rpc` 依赖的模块自动生效，无需每个模块重复声明。

### 3.2 模块级 RpcConfiguration

每个业务模块（biz 模块）内部需要定义一个 `RpcConfiguration`，声明该模块需要使用的 Feign 客户端：

**类路径**: `cn.shutan.platform.module.{module}.framework.rpc.config.RpcConfiguration`

```java
@Configuration
@EnableFeignClients(clients = {
    FileApi.class,
    WebSocketSenderApi.class,
    ConfigApi.class
})
public class RpcConfiguration {
}
```

> **说明**: 模块级 `RpcConfiguration` 仅声明本模块需要的 Feign 客户端，与框架级自动配置互补。每个模块只需引入自身业务依赖的 Feign 客户端，避免加载不需要的 Bean，减少启动时间和内存占用。

---

## 4. 请求拦截器链

Feign 客户端在发送请求时，会经过一系列请求拦截器（`RequestInterceptor`），自动向请求 Header 中注入上下文信息，实现微服务间的上下文透传。

系统共定义了 4 个请求拦截器，按执行顺序排列如下：

### 4.1 LoginUserRequestInterceptor — 用户上下文透传

**类路径**: `cn.shutan.platform.framework.security.core.rpc.LoginUserRequestInterceptor`

**职责**: 将当前登录用户信息传递到下游服务。

**工作流程**:

```
服务 A (发起调用)
    → LoginUserRequestInterceptor 拦截 Feign 请求
        → 从 SecurityFrameworkUtils 获取 LoginUser
        → 序列化为 JSON 字符串
        → URLEncoder 编码
        → 写入请求 Header: login-user
    → 服务 B (接收请求)
        → LoginUserRequestInterceptor 读取 login-user Header
        → URLDecoder 解码
        → 反序列化为 LoginUser 对象
        → 设置到当前线程的 SecurityContextHolder
```

> **说明**: 下游服务通过此机制无需再次校验 Token 即可获取当前用户信息。`login-user` Header 仅在内部服务间传递，Gateway 层会过滤掉外部请求中的该 Header，防止伪造。

### 4.2 TenantRequestInterceptor — 租户上下文透传

**类路径**: `cn.shutan.platform.framework.tenant.core.rpc.TenantRequestInterceptor`

**职责**: 将当前租户 ID 传递到下游服务。

**工作流程**:

```
→ 从 TenantContextHolder 获取当前 tenantId
→ 写入请求 Header: tenant-id
→ 下游服务通过 TenantContextWebFilter 或 TenantRequestInterceptor 读取该 Header
→ 设置到当前线程的 TenantContextHolder
```

> **说明**: 多租户模式下，所有服务间调用必须携带租户 ID，否则下游服务无法正确过滤数据。该拦截器确保了租户上下文在服务间的一致传递。

### 4.3 EnvRequestInterceptor — 环境标签透传

**类路径**: `cn.shutan.platform.framework.env.rpc.EnvRequestInterceptor`

**职责**: 传递环境标签，实现环境隔离（如开发/测试/生产环境的隔离）。

### 4.4 DataPermissionRequestInterceptor — 数据权限控制

**类路径**: `cn.shutan.platform.framework.datapermission.core.rpc.DataPermissionRequestInterceptor`

**职责**: 透传数据权限开关。

**工作流程**:

```
→ 读取当前线程数据权限配置状态
→ 写入请求 Header: data-permission-enable
→ 下游服务 DataPermissionRpcWebFilter 读取该 Header
→ 根据 Header 值决定是否禁用数据权限过滤
```

**拦截器链总结**:

| 拦截器 | Header 名 | 携带数据 | 来源 |
|--------|----------|---------|------|
| `LoginUserRequestInterceptor` | `login-user` | LoginUser JSON | SecurityContextHolder |
| `TenantRequestInterceptor` | `tenant-id` | 租户 ID | TenantContextHolder |
| `EnvRequestInterceptor` | 环境标签 | 环境标识 | 环境上下文 |
| `DataPermissionRequestInterceptor` | `data-permission-enable` | 数据权限开关 | 数据权限上下文 |

> **说明**: 这 4 个拦截器由框架级自动配置统一注册，对项目中所有 Feign 调用自动生效，业务代码无需手动处理 Header 的传递和接收。

### 4.5 拦截器链端到端执行顺序

```
调用方（Consumer）                           目标服务（Provider）

Service 层调用
  │
  ▼
deptApi.getDeptList()
  │
  ▼
Feign 动态代理构建 HTTP 请求
  │
  ▼
┌─────────────────────────────┐
│  RequestInterceptor 链       │    拦截器按以下顺序执行：
│                              │
│  ① LoginUserRequestInterceptor   │
│     SecurityContextHolder        │
│     └→ JSON 序列化 LoginUser     │
│        └→ URLEncoder 编码        │
│           └→ Header: login-user  │
│                                  │
│  ② TenantRequestInterceptor      │
│     TenantContextHolder           │
│     └→ Header: tenant-id         │
│                                  │
│  ③ EnvRequestInterceptor         │
│     环境上下文                    │
│     └→ Header: env-tag           │
│                                  │
│  ④ DataPermissionRequestInterceptor │
│     数据权限上下文                 │
│     └→ Header: data-permission-enable │
│                                  │
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────┐
│  HTTP 请求发送               │
│  GET /rpc-api/dept/list     │
│  Headers:                   │
│    login-user: %7B%22id...  │
│    tenant-id: t001          │
│    env-tag: dev             │
│    data-permission-enable: true │
└──────────────┬──────────────────┘
               │
               │  网络传输 + Nacos 服务发现
               ▼
┌─────────────────────────────┐
│  目标服务接收                 │
│                              │
│  ⑤ TenantContextWebFilter    │
│     读取 Header[tenant-id]   │
│     → setTenantId("t001")   │
│                              │
│  ⑥ TokenAuthenticationFilter │
│     读取 Header[login-user]  │
│     → 反序列化 LoginUser     │
│     → 写入 SecurityContext   │
│                              │
│  ⑦ 路由到 @RestController   │
│     DeptApiImpl.getDeptList()│
│     → 执行业务逻辑            │
│     → 返回 CommonResult<T>   │
└──────────────┬──────────────────┘
               │
               ▼
响应返回调用方
  │
  ▼
CommonResult<T>.getCheckedData()
  │  code != 0 → 抛出 ServiceException
  │  code == 0 → 返回 T 数据
  ▼
调用方继续业务逻辑
```

> **执行顺序记忆口诀**: "用户先行，租户跟上；环境标签，权限断后"。拦截器链的顺序是固定的，由 Spring 的 `@Order` 或注册顺序决定。

---

## 5. API 实现规范

### 5.1 同接口双角色模式

xy-cloud 采用接口双角色设计——同一接口同时作为 Feign 客户端契约和 `@RestController` 契约：

```
┌─────────────────────┐           ┌──────────────────────┐
│    api 模块           │           │    biz 模块            │
│                      │           │                       │
│  DeptApi (接口)       │◄─────────│  DeptApiImpl (实现)     │
│  ├─ @FeignClient     │  implements│  ├─ @RestController   │
│  ├─ @GetMapping      │           │  ├─ @Validated        │
│  ├─ CommonResult<T>  │           │  └─ @Resource DeptSrv │
│  └─ default 方法      │           │                       │
└─────────────────────┘           └──────────────────────┘
         │                                  │
         │ Feign RPC                        │ 本地调用
         ▼                                  ▼
   ┌──────────────┐               ┌────────────────┐
   │ 调用方服务     │               │ DeptService     │
   │ (eos-biz 等)  │               │ (biz 内部)      │
   └──────────────┘               └────────────────┘
```

**接口定义（api 模块）**:

```java
// platform-module-system-api/api/DeptApi.java
@FeignClient(name = ApiConstants.NAME)
public interface DeptApi {
    String PREFIX = ApiConstants.PREFIX + "/dept";

    @GetMapping(PREFIX + "/list-all")
    CommonResult<List<DeptRespDTO>> getDeptList();

    @GetMapping(PREFIX + "/get")
    CommonResult<DeptRespDTO> getDept(@RequestParam("id") Long id);
}
```

**接口实现（biz 模块）**:

```java
// platform-module-system-biz/api/DeptApiImpl.java
@RestController
@Validated
public class DeptApiImpl implements DeptApi {

    @Resource
    private DeptService deptService;

    @Override
    public CommonResult<List<DeptRespDTO>> getDeptList() {
        List<DeptDO> list = deptService.getDeptList();
        return CommonResult.success(BeanUtils.toBean(list, DeptRespDTO.class));
    }

    @Override
    public CommonResult<DeptRespDTO> getDept(@RequestParam("id") Long id) {
        DeptDO dept = deptService.getDept(id);
        return CommonResult.success(BeanUtils.toBean(dept, DeptRespDTO.class));
    }
}
```

> **说明**: 接口定义在 `api` 模块中，实现类在 `biz` 模块中。这种设计使得 `api` 模块可以被打包为轻量依赖，供其他模块的 Feign 调用方使用，无需引入完整的 `biz` 模块及其所有依赖。

### 5.2 默认方法封装 — getCheckedData

为了进一步简化调用方的使用，Feign 接口中可以定义 `default` 方法，封装 `CommonResult` 的解包逻辑：

```java
@FeignClient(name = ApiConstants.NAME)
public interface DeptApi {
    String PREFIX = ApiConstants.PREFIX + "/dept";

    @GetMapping(PREFIX + "/list-all")
    CommonResult<List<DeptRespDTO>> getDeptList();

    @GetMapping(PREFIX + "/get")
    CommonResult<DeptRespDTO> getDept(@RequestParam("id") Long id);

    // 默认方法 — 直接返回解包后的数据，简化调用方代码
    default List<DeptRespDTO> getDeptListData() {
        return getDeptList().getCheckedData();
    }

    default DeptRespDTO getDeptData(Long id) {
        return getDept(id).getCheckedData();
    }
}
```

> **说明**: 默认方法并非强制要求，但对于高频调用的接口，提供默认封装可以显著简化调用方的代码量。命名约定在原始方法名后加 `Data` 或 `Checked` 后缀，以示区分。

---

## 6. 消息队列集成

### 6.1 Redis MQ（主力方案）

xy-cloud 以 Redis 消息队列作为主要的异步通信方案，支持 Pub/Sub 和 Stream 两种模式。

#### 6.1.1 消息模型

| 模型 | 基类 | 特性 |
|------|------|------|
| Pub/Sub | `AbstractRedisChannelMessage` | 简单发布-订阅，消息即发即失 |
| Stream | `AbstractRedisStreamMessage` | 支持消费组，消息可回溯，支持重试 |

**类路径**: `cn.shutan.platform.framework.mq.redis.core.message.AbstractRedisChannelMessage`

```java
public abstract class AbstractRedisChannelMessage {
    // 由子类实现，返回 Channel 名称
    public abstract String getChannel();
}

public abstract class AbstractRedisStreamMessage {
    // 由子类实现，返回 Stream Key 名称
    public abstract String getStreamKey();
}
```

#### 6.1.2 RedisMQTemplate — 统一发送入口

**类路径**: `cn.shutan.platform.framework.mq.redis.core.RedisMQTemplate`

```java
public class RedisMQTemplate {
    // 发送 Pub/Sub 消息
    public void send(AbstractRedisChannelMessage message) { ... }

    // 发送 Stream 消息
    public void send(AbstractRedisStreamMessage message) { ... }
}
```

#### 6.1.3 Pub/Sub 模式示例

```java
// 1. 定义消息
public class DeptUpdateMessage extends AbstractRedisChannelMessage {
    public static final String CHANNEL = "system.dept.update";

    private Long deptId;
    private String deptName;

    // getter / setter ...

    @Override
    public String getChannel() {
        return CHANNEL;
    }
}

// 2. 发送消息
@Resource
private RedisMQTemplate redisMQTemplate;

DeptUpdateMessage message = new DeptUpdateMessage();
message.setDeptId(deptId);
message.setDeptName(deptName);
redisMQTemplate.send(message);

// 3. 消费消息（定义 @Component + @RedisChannelMessageListener）
@Component
public class DeptUpdateConsumer {

    @RedisChannelMessageListener(channel = DeptUpdateMessage.CHANNEL)
    public void onMessage(DeptUpdateMessage message) {
        Long deptId = message.getDeptId();
        // 处理业务逻辑...
    }
}
```

#### 6.1.4 Stream 模式说明

Stream 模式相比 Pub/Sub 增加了以下能力：

- **消费组**: 支持多个消费者组成消费组，消息在组内负载均衡。
- **消息回溯**: 消费者可回溯未处理或处理失败的消息。
- **重试机制**: 通过 `RedisPendingMessageResendJob` 定时重试未确认的消息。

```java
// 配置 Stream 消息监听
@Configuration
public class StreamConfig {

    @Bean
    public RedisStreamMessageListenerContainer listenerContainer(
            RedisMQTemplate redisMQTemplate) {
        RedisStreamMessageListenerContainer container =
                new RedisStreamMessageListenerContainer(redisMQTemplate);
        container.setAutoAcknowledge(false);  // 关闭自动确认，启用重试
        return container;
    }

    @Bean
    public RedisPendingMessageResendJob pendingMessageResendJob(
            RedisMQTemplate redisMQTemplate) {
        return new RedisPendingMessageResendJob(redisMQTemplate);
    }
}
```

> **说明**: `autoAcknowledge(false)` 配合 `RedisPendingMessageResendJob` 实现了消息的可靠投递。当消费者处理消息时抛出异常，消息不会被确认，重试任务会重新投递该消息。消息拦截器链（`RedisMessageInterceptor.beforeSend` / `afterSend`）提供了发送前后的扩展点，可用于记录发送日志、注入消息头等。

### 6.2 RabbitMQ（条件启用）

**类路径**: `cn.shutan.platform.framework.mq.rabbitmq.config.PlatformRabbitMQAutoConfiguration`

RabbitMQ 作为可选的消息中间件，通过条件判断自动装配——当 classpath 中存在 `RabbitTemplate` 时自动启用：

```java
@Configuration
@ConditionalOnClass(RabbitTemplate.class)
public class PlatformRabbitMQAutoConfiguration {

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
```

### 6.3 RocketMQ（条件启用）

RocketMQ 同样作为可选项，提供了租户相关的钩子支持：

| 钩子 | 类名 | 职责 |
|------|------|------|
| 消费端租户钩子 | `TenantRocketMQConsumeMessageHook` | 消费消息时恢复租户上下文 |
| 发送端租户钩子 | `TenantRocketMQSendMessageHook` | 发送消息时注入租户上下文 |

> **说明**: Redis MQ 是项目的默认主力方案，零依赖、轻量级。RabbitMQ 和 RocketMQ 仅在有特殊需求（如已有基础设施、要求消息持久化更强保证）时通过引入对应 Starter 启用。

---

## 7. WebSocket 集成

### 7.1 可切换 Sender 架构

WebSocket 模块采用可切换的 Sender 架构，通过配置 `platform.websocket.sender-type` 切换底层实现：

| sender-type | 实现方式 | 适用场景 |
|------------|---------|---------|
| `local` | 进程内直接推送 | 单节点部署 |
| `redis` | Redis Pub/Sub 广播 | 多节点部署（推荐） |
| `rocketmq` | RocketMQ 广播 | 已有 RocketMQ 基础设施 |
| `rabbitmq` | RabbitMQ 广播 | 已有 RabbitMQ 基础设施 |
| `kafka` | Kafka 广播 | 已有 Kafka 基础设施 |

**配置示例**:

```yaml
platform:
  websocket:
    sender-type: redis    # 多节点部署推荐使用 redis
```

> **说明**: 多节点部署时必须使用 `redis` 或消息队列模式，否则 WebSocket 消息只会发送到当前节点，无法到达其他节点上的客户端。`local` 模式仅适用于开发或单节点部署。

### 7.2 WebSocketSenderApi

**类路径**: `cn.shutan.platform.module.infra.api.websocket.WebSocketSenderApi`

WebSocket 的发送功能通过 Feign 接口暴露给其他模块调用，定义在 infra 模块中：

```java
@FeignClient(name = ApiConstants.NAME)
public interface WebSocketSenderApi {
    String PREFIX = ApiConstants.PREFIX + "/websocket/sender";

    // 发送给指定用户类型 + 用户 ID
    @PostMapping(PREFIX + "/send-by-user")
    CommonResult<Boolean> send(
            @RequestParam("userType") Integer userType,
            @RequestParam("userId") Long userId,
            @RequestParam("messageType") String messageType,
            @RequestParam("messageContent") String messageContent);

    // 发送给指定 Session
    @PostMapping(PREFIX + "/send-by-session")
    CommonResult<Boolean> send(
            @RequestParam("sessionId") String sessionId,
            @RequestParam("messageType") String messageType,
            @RequestParam("messageContent") String messageContent);

    // 默认方法 — 自动序列化为 JSON
    default <T> void sendObject(Integer userType, Long userId,
                                String messageType, T data) {
        send(userType, userId, messageType, JsonUtils.toJsonString(data));
    }

    default <T> void sendObject(String sessionId,
                                String messageType, T data) {
        send(sessionId, messageType, JsonUtils.toJsonString(data));
    }
}
```

**调用示例**:

```java
@Service
public class AlarmService {

    @Resource
    private WebSocketSenderApi webSocketSenderApi;

    public void sendAlarm(Long userId, AlarmVO alarm) {
        // 使用 sendObject 默认方法自动序列化
        webSocketSenderApi.sendObject(
            UserTypeEnum.ADMIN.getValue(),
            userId,
            "alarm.notification",
            alarm
        );
    }
}
```

> **说明**: `sendObject` 默认方法将业务对象自动 JSON 序列化后再发送，调用方无需手动处理序列化。`messageType` 字段用于客户端区分消息类型，通常使用点号分隔的命名空间（如 `alarm.notification`、`system.config.update`）。

---

## 8. 跨模块调用最佳实践

### 8.1 完整案例：eos-biz 调用 system-server

以 eos 模块的业务服务调用 system 模块的部门接口为例，展示完整的跨模块调用流程：

**步骤一：声明 Feign 客户端**（在 eos-biz 模块的 `RpcConfiguration` 中）：

```java
@Configuration
@EnableFeignClients(clients = {
    DeptApi.class,           // system 模块 — 部门接口
    DictDataApi.class,       // system 模块 — 字典接口
    AdminUserApi.class,      // system 模块 — 用户接口
    FileApi.class,           // infra 模块 — 文件接口
    WebSocketSenderApi.class // infra 模块 — WebSocket 发送
})
public class RpcConfiguration {
}
```

**步骤二：注入并使用 Feign 客户端**：

```java
@Service
public class DeviceBasicInfoServiceImpl implements DeviceBasicInfoService {

    @Resource
    private DeptApi deptApi;
    @Resource
    private DictDataApi dictDataApi;
    @Resource
    private AdminUserApi adminUserApi;

    public void processDeviceData(Long deptId) {
        // 推荐方式：使用 getCheckedData()，失败即抛异常
        List<DeptRespDTO> depts = deptApi.getDeptList().getCheckedData();

        // 批量查询字典数据
        DictDataRespVO dict = dictDataApi.getDictData(
            DictDataConstants.DICT_TYPE_DEVICE_STATUS,
            deviceStatus
        ).getCheckedData();

        // 获取用户信息
        AdminUserRespDTO user = adminUserApi.getUser(userId).getCheckedData();
    }
}
```

### 8.2 注意事项

**依赖管理**:

- 调用方模块需要在 `pom.xml` 中引入被调用方的 `api` 模块依赖（而非 `biz` 模块）。
- 例如 eos-biz 需要引入 `platform-module-system-api`，而不是 `platform-module-system-biz`。

**Spring Boot 包扫描**:

- 由于 Feign 客户端接口和被调用方（`@RestController` 实现）分别在不同的包路径下，需要确保两者的包都在 Spring Boot 的组件扫描范围内。
- 框架级自动配置通过 `spring.factories` 或 `@Import` 注册，模块级配置通过 `@ComponentScan` 或显式配置覆盖。

**异步场景**:

- 在异步线程或 MQ 消费者中调用 Feign 接口时，需要注意用户上下文和租户上下文可能为空。
- 使用 `TenantUtils.execute(tenantId, () -> { ... })` 手动指定租户上下文。
- 对于不需要用户上下文的场景，确保 Feign 接口不会因为缺少 `login-user` Header 而拒绝服务。

**性能考虑**:

- Feign 调用是同步阻塞的，避免在循环中逐个调用 Feign 接口，应优先使用批量接口（如 `listAll`、`listByIds`）。
- 对于高频调用的接口，考虑在调用方增加本地缓存（如 Caffeine），减少 RPC 调用次数。

**错误处理**:

- 始终使用 `getCheckedData()` 或 `checkError()` 处理 Feign 调用的异常情况。
- 后端服务的 `ServiceException` 会通过 `CommonResult` 透传到调用方，调用方无需单独处理 Feign 层面的错误。

**网关路径与 RPC 路径的区别**:

| 路径类型 | 前缀 | 路由方式 | 认证要求 |
|---------|------|---------|---------|
| 前端 REST 接口 | `/api/**` | 通过 Gateway 路由 | 需要 Token 认证 |
| 内部 RPC 接口 | `/rpc-api/**` | 直接通过 Feign 调用 | 内部网络，Header 透传 |

> **说明**: RPC 接口不应该通过 Gateway 对外暴露，同样前端也不应直接调用 RPC 路径。两者在安全配置和路由规则上完全分离。

---

## 9. 附录

### 9.1 Maven 依赖速查

**必选依赖**:

```xml
<!-- Web 基础 -->
<dependency>
    <groupId>cn.shutan.platform</groupId>
    <artifactId>platform-spring-boot-starter-web</artifactId>
</dependency>

<!-- 安全认证（Feign 拦截器依赖） -->
<dependency>
    <groupId>cn.shutan.platform</groupId>
    <artifactId>platform-spring-boot-starter-security</artifactId>
</dependency>
```

**RPC 调用**:

```xml
<!-- Feign 与拦截器链 |
<dependency>
    <groupId>cn.shutan.platform</groupId>
    <artifactId>platform-spring-boot-starter-rpc</artifactId>
</dependency>
```

**消息队列**:

```xml
<!-- Redis MQ（主力方案） -->
<dependency>
    <groupId>cn.shutan.platform</groupId>
    <artifactId>platform-spring-boot-starter-mq</artifactId>
</dependency>
```

**WebSocket**:

```xml
<dependency>
    <groupId>cn.shutan.platform</groupId>
    <artifactId>platform-spring-boot-starter-websocket</artifactId>
</dependency>
```

### 9.2 关键文件路径速查

| 文件 | 路径 |
|------|------|
| RPC 基础常量 | `platform-common/enums/RpcConstants.java` |
| CommonResult | `platform-common/pojo/CommonResult.java` |
| Feign 自动配置（框架级） | `platform-spring-boot-starter-rpc/config/` |
| 登录用户拦截器 | `platform-spring-boot-starter-security/rpc/LoginUserRequestInterceptor.java` |
| 租户拦截器 | `platform-spring-boot-starter-biz-tenant/rpc/TenantRequestInterceptor.java` |
| 数据权限拦截器 | `platform-spring-boot-starter-biz-data-permission/rpc/DataPermissionRequestInterceptor.java` |
| 环境拦截器 | `platform-spring-boot-starter-env/rpc/EnvRequestInterceptor.java` |
| Redis MQ 模板 | `platform-spring-boot-starter-mq/redis/core/RedisMQTemplate.java` |
| WebSocket Sender API | `platform-module-infra-api/api/websocket/WebSocketSenderApi.java` |
| 模块 ApiConstants 示例 | `platform-module-system-api/enums/ApiConstants.java` |
| 模块 RpcConfiguration 示例 | `platform-module-system-biz/framework/rpc/config/RpcConfiguration.java` |
