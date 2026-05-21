# EtherosFramework — Yosemite

轻量级依赖注入与钩子框架，专为 Paper 系 Minecraft 服务端插件设计，支持跨插件 Bean 共享。

## 适用范围

- **Java 17**
- **Minecraft 1.18 – 1.20.4**（Paper 及其下游）

## 核心能力

| 模块 | 功能 |
|------|------|
| **DI 容器** | `@Service` / `@GlobalService` / `@Configuration` / `@Bean` 自动发现与实例化 |
| **依赖注入** | `@Autowired` / `@GlobalAutowired` 字段、构造器、Setter 注入，支持按名称和按类型 |
| **跨插件共享** | `@GlobalService` 将 Bean 自动注册到 `SharedContext`，其他插件可通过 `@Autowired` 透明获取 |
| **属性注入** | `@Value("key")` 从 `application.properties` 读取配置值 |
| **生命周期** | `@PostConstruct` / `@PreDestroy` 回调 |
| **钩子系统** | `HookManager` 接口 + `Event` 事件模型，支持优先级和取消传播 |
| **ASM 扫描** | 基于 ASM 9.0 的字节码注解扫描，不触发类加载 |

## 快速开始

### 引用

**Gradle**：
```kotlin
dependencies {
    implementation("cn.skilfully.etheros:EtherosFramework-Yosemite:1.0.4")
}
```

**Maven**：
```xml
<dependency>
    <groupId>cn.skilfully.etheros</groupId>
    <artifactId>EtherosFramework-Yosemite</artifactId>
    <version>1.0.4</version>
</dependency>
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
        SharedContext.syncFrom(...); // 如需跨插件共享
    }
}
```

### 跨插件 DI（Paper ClassLoader 父子链）

```
ServerClassLoader
 └─ PluginClassLoader[EtherosCore]
      ├── SharedContext (static fields — 全局唯一)
      └─ PluginClassLoader[Addon] (depend: [EtherosCore])
           └── @Autowired Foo foo → SharedContext.getBean(Foo.class) ✓
```

标注 `@GlobalService` 的 Bean 在容器启动后自动注册到 `SharedContext`，依赖 EtherosCore 的子插件通过 `@Autowired` 即可类型安全获取。

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
└── hook/
    ├── core/          — HookManager (interface)
    ├── entity/        — Event, Priority
    └── exception/     — HookException
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
Event event = new Event("player.join", Map.of("player", player));

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

## 联系
QQ群：870666822
