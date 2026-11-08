# ItemShowcase

跨 Minecraft 版本、跨加载器的聊天物品展示 MOD。主包名：`com.heihun.Itemshowcase`（MOD ID：`itemshowcase`）。

玩家可以在聊天消息中使用宏（`[i]` 手持物品、`[inv]` 背包、`[ec]` 末影箱、`[c]` 饰品、`[pack]` 饰品背包），
或使用 `Ctrl+Enter` 快捷发送悬停物品。安装了 ItemShowcase 的客户端会在聊天中直接渲染
**真实物品图标**（原版 ItemRenderer 绘制，含数量覆盖、耐久、附魔光效）并显示原版物品 Tooltip；
未安装的客户端收到可读的文字回退消息 + 原版 `show_item` 悬停。

## 支持目标（7 个构建）

| Minecraft | Loader | Java | 目录 |
|-----------|--------|------|------|
| 1.16.5 | Forge | 8 | `targets/forge-1.16.5` |
| 1.16.5 | Fabric | 8 | `targets/fabric-1.16.5` |
| 1.20.1 | Forge | 17 | `targets/forge-1.20.1` |
| 1.20.1 | Fabric | 17 | `targets/fabric-1.20.1` |
| 1.21.1 | Forge | 21 | `targets/forge-1.21.1` |
| 1.21.1 | NeoForge | 21 | `targets/neoforge-1.21.1` |
| 1.21.1 | Fabric | 21 | `targets/fabric-1.21.1` |

## 仓库结构（按规范分层）

```
common-core/        跨版本核心（纯 Java 8 字节码，零 Minecraft 依赖）
                    - 宏解析器 MacroParser
                    - 结构化消息模型 ShowcaseMessage / TextSegment / ItemSegment / ItemGroupSegment / SectionSegment
                    - Showcase 网络协议（Envelope + ShowcaseWireCodec + ClientCapabilities）
                    - 配置系统（JSON、热加载、容错）
                    - SPI：ItemContainerProvider / AccessoryProvider / PlayerDataViews
                    - 校验与限流（MessageValidator / RateLimiter）
minecraft-common/   Minecraft 概念层的统一接口（无具体版本 API，随阶段二引入）
targets/*           每个构建目标一个独立 Gradle 构建（ForgeGradle / NeoGradle+MDG / Fabric Loom 互相隔离）
docs/               架构说明、阶段计划、兼容性矩阵、安装文档
```

## 核心设计原则

- 服务端权威：服务端解析宏、读取真实库存（背包 / 末影箱 / 饰品 / 背包内部库存），客户端不提交 ItemStack。
- 不硬编码物品 ID、饰品槽位名、物品白名单；全部走注册表与动态 API。
- 可选 API（Curios / Trinkets / Accessories / Sophisticated Backpacks / Traveler's Backpack）通过
  `ItemContainerProvider`、`AccessoryProvider` 插件式接入，缺失时安全降级。
- 1.16.5 / 1.20.1 使用物品 NBT 序列化，1.21.1 使用 Data Components 序列化，互不混用。
- 原版客户端收到可读回退文本，不会收到无法阅读的数据包。

## 开发阶段

见 [docs/PHASES.md](docs/PHASES.md)。

## 构建

每个目标目录是独立 Gradle 构建（自带 wrapper 与正确的 Gradle 版本）：

```powershell
cd targets/forge-1.20.1
.\gradlew.bat build
```

`common-core` 通过 Composite Build（includeBuild + 依赖替换）被各目标直接以源码消费，无需先发布。
