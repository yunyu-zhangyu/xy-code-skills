<!-- 来源: D:\ZHYwork\Skills\xy-cloud-skills\开发参考手册\API集成规范参考.md -->

# xy-cloud API 集成规范速查

## 1. Feign 接口定义模板

```java
// ========== 1. ApiConstants（api 子模块） ==========
public interface ApiConstants {
    String NAME = "system-server";           // spring.application.name
    String PREFIX = RpcConstants.RPC_API_PREFIX + "/system";  // /rpc-api/{module}
    String VERSION = "1.0.0";
}

// ========== 2. Feign 接口 ==========
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

### 模块 ApiConstants 速查

| 模块 | NAME | PREFIX |
|------|------|--------|
| system | system-server | `/rpc-api/system` |
| infra | infra-server | `/rpc-api/infra` |
| eos | eos-server | `/rpc-api/eos` |
| demand | demand-server | `/rpc-api/demand` |

---

## 2. DTO/VO 命名对照

| 模块 | 请求对象 | 响应对象 | 子包 |
|------|----------|----------|------|
| system / infra | `XxxReqDTO` | `XxxRespDTO` | `dto/` |
| eos | `XxxReqVO` | `XxxRespVO` | `vo/` |

---

## 3. RpcConfiguration 模板

```java
// 在 biz 模块中注册需要调用的 Feign 客户端
@Configuration
@EnableFeignClients(clients = {FileApi.class, WebSocketSenderApi.class, ConfigApi.class})
public class RpcConfiguration {}
```

---

## 4. API Impl 模板

```java
// 同一接口同时是 Feign 契约 和 @RestController 契约
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
    public CommonResult<DeptRespDTO> getDept(Long id) {
        DeptDO dept = deptService.getDept(id);
        return CommonResult.success(BeanUtils.toBean(dept, DeptRespDTO.class));
    }
}
```

---

## 5. 请求拦截器链

| 拦截器 | Header | 来源 | 接收端处理 |
|--------|--------|------|-----------|
| LoginUserRequestInterceptor | `login-user` | SecurityFrameworkUtils | LoginUserFilter 还原 |
| TenantRequestInterceptor | `tenant-id` | TenantContextHolder | TenantContextFilter 还原 |
| EnvRequestInterceptor | 环境标签 | EnvContextHolder | 环境隔离 |
| DataPermissionRequestInterceptor | `data-permission-enable` | DataPermissionHelper | DataPermissionRpcWebFilter 读取 |

---

## 6. 跨模块调用完整示例

```java
// eos-biz 中调用 system-server 的 Feign API
@Service
@EnableFeignClients(clients = {DeptApi.class, DictDataApi.class, AdminUserApi.class})
public class DeviceBasicInfoServiceImpl implements DeviceBasicInfoService {

    @Resource
    private DeptApi deptApi;
    @Resource
    private DictDataApi dictDataApi;

    @Override
    public void someMethod() {
        // 调用 Feign — 返回 CommonResult，用 getCheckedData() 解包
        List<DeptRespDTO> depts = deptApi.getDeptList().getCheckedData();

        // 或使用接口上的默认方法（如定义了 getDeptListData()）
        List<DeptRespDTO> depts = deptApi.getDeptListData();
    }
}
```

### CommonResult 常用方法

| 方法 | 说明 |
|------|------|
| `CommonResult.success(data)` | 创建成功响应 |
| `CommonResult.error(code, msg)` | 创建错误响应 |
| `result.isSuccess()` | 判断是否成功 |
| `result.checkError()` | 失败时抛出 ServiceException |
| `result.getCheckedData()` | 检查并获取数据（失败抛异常） |

---

## 7. 消息队列

### Redis MQ（主力方案）

```java
// ========== 消息体 ==========
// Pub/Sub 模式
public class XxxMessage extends AbstractRedisChannelMessage {
    private Long id;
    private String data;
    // getter/setter...
}

// Stream 模式（支持消费组）
public class XxxStreamMessage extends AbstractRedisStreamMessage {
    private Long id;
    // getter/setter...
}

// ========== 发送 ==========
@Resource
private RedisMQTemplate redisMQTemplate;

public void send(XxxMessage message) {
    redisMQTemplate.send(message);
}

// ========== 消费 ==========
@Component
public class XxxMessageConsumer extends AbstractRedisChannelMessageListener<XxxMessage> {
    @Override
    public void onMessage(XxxMessage message) {
        // 消费逻辑
    }
}
```

### RabbitMQ / RocketMQ

| MQ | 启用条件 | 特点 |
|----|----------|------|
| RabbitMQ | classpath 存在 RabbitTemplate | Jackson2JsonMessageConverter |
| RocketMQ | classpath 存在 | 支持租户钩子（TenantRocketMQ*Hook） |

---

## 8. WebSocket 发送

```java
// 通过 Feign 调用 infra 的 WebSocketSenderApi
@Resource
private WebSocketSenderApi webSocketSenderApi;

// 发送给指定用户
webSocketSenderApi.send(userType, userId, "messageType", "content");

// 使用 sendObject 自动序列化
webSocketSenderApi.sendObject(userType, userId, "notice", noticeData);
```

Sender 类型通过 `platform.websocket.sender-type` 配置：

| 类型 | 说明 |
|------|------|
| local | 进程内 |
| redis | Redis pub/sub |
| rocketmq / rabbitmq / kafka | MQ 作为消息总线 |

---

## 9. 注意事项

1. Feign 接口 `@RequestParam` 需要显式命名（`@RequestParam("id")`），依赖编译 `-parameters` 不可靠
2. `CommonResult` 不能使用 `Optional` 包装
3. 避免 Feign 链式调用过深（A→B→C 超过 3 层）
4. 跨模块调用不共享事务边界
5. Feign 默认超时由 Spring Cloud 配置（`feign.client.config.default.readTimeout`）
6. 广播模式使用 MQ（Redis Stream 支持消费组），点对点使用 Feign

---

## 10. Feign API 生成流程（M4 补充）

当业务模块需要暴露跨模块 RPC 接口时，在标准 CRUD 代码生成（M4）之外，额外创建以下文件：

### 10.1 创建清单

| # | 文件 | 目标模块 | 模板位置 |
|---|------|---------|---------|
| 1 | `XxxApi.java` | `xxx-api` | `shared/templates/rpc/FeignApi.java` |
| 2 | `XxxApiImpl.java` | `xxx-biz` | `shared/templates/rpc/ApiImpl.java` |
| 3 | `XxxRespDTO.java` | `xxx-api` | `shared/templates/rpc/RespDTO.java` |
| 4 | `XxxReqDTO.java` | `xxx-api` | `shared/templates/rpc/ReqDTO.java` |
| 5 | `ApiConstants.java` | `xxx-api`（模块级，仅首次） | `shared/templates/rpc/ApiConstants.java` |

### 10.2 放置路径

以 `module=eos, domain=energyprice, entity=EnergyPrice` 为例：

```
platform-module-eos/
├── platform-module-eos-api/
│   └── src/main/java/cn/shutan/platform/module/eos/
│       ├── api/energyprice/
│       │   ├── EnergyPriceApi.java              # Feign 接口
│       │   ├── dto/EnergyPriceRespDTO.java      # 响应 DTO
│       │   └── vo/EnergyPriceReqDTO.java        # 请求 DTO
│       └── enums/ApiConstants.java              # 模块常量
│
└── platform-module-eos-biz/
    └── src/main/java/cn/shutan/platform/module/eos/
        └── api/energyprice/
            └── EnergyPriceApiImpl.java           # Feign 实现
```

### 10.3 调用方配置

调用方（如 eos-biz 调用 system 的 API）需在 biz 模块添加 `RpcConfiguration`：

```java
@Configuration
@EnableFeignClients(clients = {EnergyPriceApi.class, DeptApi.class})
public class RpcConfiguration {}
```

并确保 biz 模块 pom.xml 依赖了对应 api 模块：
```xml
<dependency>
    <groupId>cn.shutan.platform</groupId>
    <artifactId>platform-module-eos-api</artifactId>
    <version>${project.version}</version>
</dependency>
```

### 10.4 命名对照

| 模块 | Feign 接口 | 请求对象 | 响应对象 |
|------|-----------|---------|---------|
| system / infra | `XxxApi` | `XxxReqDTO` | `XxxRespDTO` |
| eos | `XxxApi` | `XxxReqVO` | `XxxRespVO` |

> 注意：eos 模块使用 VO 后缀而非 DTO。生成后按模块风格重命名即可。

---

## 11. DTO 设计指南

### 11.1 字段原则

- **最小化**：只暴露调用方真正需要的字段，不直接返回 DO
- **只读**：DTO 中的集合字段返回不可变视图或副本，防止调用方意外修改
- **扁平化**：嵌套对象控制在 2 层以内，过深时拆分多个 DTO
- **类型安全**：使用 `Long` 而非 `Integer` 表示 ID，使用 `LocalDateTime` 而非 `String` 表示时间

### 11.2 转换

```java
// DO → RespDTO（单条）
CommonResult.success(BeanUtils.toBean(entity, XxxRespDTO.class));

// DO → RespDTO（列表）
CommonResult.success(BeanUtils.toBean(list, XxxRespDTO.class));

// ReqDTO → DO
XxxDO entity = BeanUtils.toBean(reqDTO, XxxDO.class);
```

### 11.3 校验（ReqDTO）

跨模块 DTO 的校验规则与 VO 一致：

```java
@Data
public class XxxReqDTO {
    @NotNull(message = "编号不能为空")
    private Long id;

    @NotEmpty(message = "名称不能为空")
    @Size(max = 50, message = "名称长度不能超过50字符")
    private String name;
}
```

---

## 12. ApiImpl 实现注意事项

1. **双契约**：`@RestController`（HTTP） + `implements XxxApi`（Feign）同时生效，无需重复编写映射
2. **无 `@RequestMapping`**：路径由 Feign 接口的 `@GetMapping`/`@PostMapping` 定义，ApiImpl 上不加类级路径
3. **无 `@PreAuthorize`**：Feign API 是模块间调用，不经过管理后台的权限拦截器，不做 `@PreAuthorize` 校验
4. **异常传播**：Service 层抛出的 `ServiceException` 通过 `CommonResult` 的 `getCheckedData()` 传播，无需 ApiImpl 额外处理
5. **返回值**：始终返回 `CommonResult.success(data)`，不直接返回 data

---

## Maven 依赖

```xml
platform-spring-boot-starter-web       <!-- 必须 -->
platform-spring-boot-starter-security  <!-- 必须（含 LoginUserRequestInterceptor） -->
platform-spring-boot-starter-mq        <!-- Redis MQ -->
```
