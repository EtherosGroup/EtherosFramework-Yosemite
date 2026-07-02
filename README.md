# EtherosFramework — Yosemite

轻量级依赖注入与钩子框架，专为 Minecraft 服务端插件设计，支持跨插件 Bean 共享。

## 适用范围（Yosemite版本）

- **Java 17**
- **Minecraft 1.17 – 1.20.4**
- **适用品牌：```Bukkit及其下游 | Folia | Velocity | BungeeCord及其下游```**

## 核心能力

| 模块 | 功能                                                                      |
|------|-------------------------------------------------------------------------|
| **DI 容器** | `@Service` / `@GlobalService` / `@Configuration` / `@Bean` 自动发现与实例化     |
| **依赖注入** | `@Autowired` / `@GlobalAutowired` 字段、构造器、Setter 注入，支持按名称和按类型            |
| **跨插件共享** | `@GlobalService` 将 Bean 自动注册到 `SharedContext`，其他插件可通过 `@Autowired` 透明获取 |
| **属性注入** | `@Value("key")` 从 `application.properties` 读取配置值                        |
| **生命周期** | `@PostConstruct` / `@PreDestroy` 回调                                     |
| **钩子系统** | `HookManager` 接口 + `Event` 事件模型，支持优先级和取消传播                              |
| **ASM 扫描** | 基于 ASM 的字节码注解扫描，不触发类加载                                                   |

## 快速开始

***如果你还不会构建产物（你的插件）请看这里[BUILD_ARTIFACT.md](help/BUILD_ARTIFACT.md)***\
***如果构建出来的插件运行报错比如 `ClassNotFound` 请看完 [BUILD_ARTIFACT.md](help/BUILD_ARTIFACT.md)***

### 选择依赖模式

框架根据是否需要**跨插件 Bean 共享**分为两种依赖模式：

---

#### 模式 A：独立使用（不跨插件共享）

每个用到框架的插件各自 shade 一份，互不干扰。

**build.gradle**（每个插件都要这样配）：

```groovy
plugins {
    id 'java'
    id 'com.gradleup.shadow' version '8.3.6'
}

dependencies {
    implementation 'cn.skilfully.etheros:EtherosFramework-Yosemite:1.2.0'
}
```

---

#### 模式 B：跨插件共享 Bean（API 宿主 + 子插件）

一个插件作为 API 宿主，提供 `@GlobalService` bean；其他子插件依赖宿主，共享同一份 `SharedContext`。

**宿主插件 build.gradle**：

```groovy
plugins {
    id 'java'
    id 'com.gradleup.shadow' version '8.3.6'
}

dependencies {
    implementation 'cn.skilfully.etheros:EtherosFramework-Yosemite:1.2.0'
}
```

**宿主插件 plugin.yml**：

```yaml
name: ExampleCore
main: com.example.core.ExampleCore
version: 1.0.0
api-version: 1.17
```

**子插件 build.gradle**：

```groovy
plugins {
    id 'java'
}

dependencies {
    compileOnly 'cn.skilfully.etheros:EtherosFramework-Yosemite:1.2.0'
}
```

**子插件 plugin.yml**：

```yaml
name: ExampleAddon
main: com.example.addon.ExampleAddon
version: 1.0.0
api-version: 1.17
depend: [ExampleCore]
```

---

#### 运行时类加载链（模式 B）

```
Minecraft Server (parent ClassLoader)
 └─ ExampleCore PluginClassLoader
      ├── SharedContext.class  (static 字段 — 全局唯一)
      ├── lib.asm.*  (relocated ASM)
      └─ ExampleAddon PluginClassLoader (depend: [ExampleCore])
           └── @Autowired Foo foo → SharedContext.getBean(Foo.class) ✓
```

### 基本用法

```java
@Service
public class UserService {
    @Autowired
    private DatabaseManager db;

    @PostConstruct
    void init() {
        db.connect();
    }
}
```

```java
// 主插件入口
public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        ApplicationContext.run(MyPlugin.class);
    }
}
```

### 跨插件 DI

宿主插件中标注 `@GlobalService` 的 Bean 在容器启动后自动注册到 `SharedContext`，子插件通过 `@Autowired` 即可获取：

```java
// 宿主插件
@GlobalService
public class DbManager implements DatabaseManager {
    public void connect() { /* ... */ }
}
```

```java
// 子插件 — 自动从 SharedContext 回退获取
@Service
public class UserModule {
    @Autowired
    private DatabaseManager db;   // 来自宿主插件
}
```

## 包结构

```
cn.skilfully.etheros.etherosframework
├── di/
│   ├── annotation/   — @Service, @GlobalService, @Autowired, @GlobalAutowired,
│   │                    @Configuration, @Bean, @Value, @PostConstruct, @PreDestroy, @Prototype
│   ├── core/          — ApplicationContext, BeanDefinition, BeanRegistry, SharedContext, PropertyLoader
│   ├── scanner/       — ClassPathScanner (ASM)
│   ├── lifecycle/     — LifecycleProcessor
│   └── exception/     — BeanCreationException, BeanNotFoundException, CircularDependencyException
├── hook/
│   ├── core/          — HookManager (interface)
│   ├── entity/        — HookEvent, Priority
│   └── exception/     — HookException
└── utils/
```

## 注解速览

| 注解 | Target | 说明 |
|------|--------|------|
| `@Service` | TYPE | 声明一个 Bean，`value` 指定名称 |
| `@GlobalService` | TYPE | 同 `@Service`，额外注册到跨插件 SharedContext |
| `@Autowired` | FIELD / CONSTRUCTOR / PARAMETER | 注入 Bean（本地 → SharedContext 回退），`value` 按名称，`required` 控制可选 |
| `@GlobalAutowired` | FIELD / CONSTRUCTOR / PARAMETER | 仅从 SharedContext 注入 |
| `@Configuration` | TYPE | 标记配置类 |
| `@Bean` | METHOD | 工厂方法，`name` 指定 Bean 名称 |
| `@Value` | FIELD / PARAMETER | 从 `application.properties` 注入属性值 |
| `@PostConstruct` | METHOD | 注入完成后调用 |
| `@PreDestroy` | METHOD | 容器关闭前调用 |
| `@Prototype` | TYPE | 原型作用域，每次获取新建实例 |

## 钩子系统

```java
// 定义事件
HookEvent event = new HookEvent();
event.setData(Map.of("player", player));

// 注册钩子
hookManager.register("player.join", e -> {
    Player p = (Player) e.getData().get("player");
    p.sendMessage("Welcome!");
}, Priority.HIGH);

// 触发
hookManager.callEvent("player.join", event);

// 取消传播
event.setCancelled(true);
```

`HookManager` 接口需由宿主插件实现并注册为 `@GlobalService`，子插件通过 `@Autowired` 获取同一实例。

## 生命周期顺序

### 启动

```
实例化（依赖优先） → 字段注入 → 工厂方法 → @PostConstruct → 注册到 SharedContext
```

`@PostConstruct` 按**实例化顺序**执行，即被依赖者先于依赖者：

```
A 依赖 B（B → A 创建顺序）
B.@PostConstruct  →  A.@PostConstruct
```

### 关闭

`context.shutdown()` 按**实例化逆序**销毁，即依赖者先于被依赖者：

```
A 依赖 B（B → A 创建顺序，逆序为 A → B）
A.@PreDestroy  →  B.@PreDestroy
```

### 循环依赖

存在 `A → B → A` 等循环依赖时，容器抛出 `CircularDependencyException`，插件启用失败。

## 联系

QQ群：870666822
