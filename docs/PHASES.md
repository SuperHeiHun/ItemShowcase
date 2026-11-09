# 开发阶段计划与状态

按规范要求，逐阶段推进：每阶段先构建、测试、修复，再进入下一阶段。

## 阶段一：基础工程 —— 【进行中 / Phase 1】

- [x] 仓库结构、构建脚本布局
- [x] common-core：统一消息模型（TextSegment / ItemSegment / ItemGroupSegment / SectionSegment / ShowcaseMessage）
- [x] common-core：宏解析器（`[i]` `[inv]` `[ec]` `[c]` `[pack]`，多宏组合，保留原文与格式）
- [x] common-core：配置系统（客户端 + 服务端，JSON，热加载，错误配置不崩溃）
- [x] common-core：网络协议模型（Envelope / PacketType / ClientCapabilities / ShowcaseWireCodec / ItemStackPayload）
- [x] common-core：SPI（ItemContainerProvider / AccessoryProvider / PlayerDataViews / IntegrationRegistry）
- [x] common-core：校验与限流（最大物品数 / 数据大小 / 嵌套深度 / 频率限制）
- [x] common-core 单元测试（JUnit 5）
- [x] common-core 构建为 Java 8 字节码（保证 1.16.5 目标可用）

## 阶段二：聊天展示 MVP（Forge 1.20.1）

- [x] Forge 1.20.1 目标工程（ForgeGradle 6 + Gradle 8.8 + Java 17 + MixinGradle）
- [x] 服务端聊天事件接入 + 宏解析 + 真实库存读取（主手 / 背包 / 盔甲 / 副手 / 末影箱）
- [x] 网络通道 itemshowcase:main（握手 / 快捷展示请求 / 结构化消息）+ 原版客户端回退消息
- [x] 客户端握手（能力协商，避免双发）
- [x] 聊天物品图标渲染（原版 renderItem：数量 / 耐久 / 附魔光效）+ 原版 Tooltip
- [x] 鼠标悬停映射（Mixin 注入范围仅 ChatComponent.render 头部 + 行 drawString + ChatScreen.render 尾部）
- [x] Ctrl+Enter 快捷发送（服务端菜单 + 槽位归属校验，客户端只提交菜单ID+槽位ID）
- [x] en_us.json / zh_cn.json
- [x] Forge 1.20.1 构建（BUILD SUCCESSFUL，jar 含合并后的 common-core）
- [ ] 客户端/服务端启动测试记录（进行中）

## 阶段三：饰品和背包 —— 【Forge 1.20.1 已完成】

- [x] CuriosProvider（Forge 1.20.1，Curios 5.14 API：动态枚举全部已注册槽位类型）
- [ ] TrinketsProvider（Fabric 各版本，随阶段四/五/六）
- [ ] AccessoriesProvider（1.21.1，随阶段五）
- [x] SophisticatedBackpacksIntegration（Forge 1.20.1：BackpackWrapper 公开 API，无固定 NBT 路径）
- [x] `[c]` `[pack]` 宏 + `[i]` 特殊背包处理（Forge 1.20.1）
- [x] ItemContainerProvider 插件注册机制（IntegrationSetup，新增集成只需一行注册）
- [x] mods.toml 可选依赖声明（curios / sophisticatedbackpacks missing 时安全降级）

> Trinkets（Fabric）与 Accessories（1.21.1）按阶段四/五的加载器架构接入各自目标工程，
> core 侧 SPI（AccessoryProvider / ItemContainerProvider）已就绪，无需改动。

## 阶段四：Fabric 1.20.1 适配（复用 common-core）

- [ ] Loom + Fabric API 目标工程搭建（targets/fabric-1.20.1）
- [ ] 聊天渲染 Mixin 移植（ChatComponent/ChatScreen）
- [ ] Fabric networking（C2S 握手 / 槽位请求 / S2C 展示消息）
- [ ] Curios → Trinkets 集成（TrinketsProvider）
- [ ] 宏解析 + Ctrl+Enter + 插件样式 GUI 移植

## 阶段五：1.21.1 适配（Forge / NeoForge / Fabric，Data Components 序列化 + 新聊天渲染 API）

### NeoForge 1.21.1（已完成，待实机复测 SB/SC）

- [x] 目标工程搭建（ModDevGradle 2.0.148 + NeoForge 21.1.265 + Java 21）
- [x] dc1211 序列化（ItemStack.STREAM_CODEC + RegistryAccess）
- [x] PayloadRegistrar 网络（C2S 握手 / 槽位请求 / S2C 展示消息）
- [x] 聊天渲染（ChatLineIconMixin @WrapOperation + ChatComponentMixin 行距 + ChatScreenMixin 悬停/点击）
- [x] 图标渲染修复（@At 包名笔误 net.minecraft.client.gui→net.minecraft.util.FormattedCharSequence）
- [x] 行距文字居中（上移 floorDiv(extra,2)+1 修正原版 1px 底部偏移）
- [x] 实机崩溃修复：构造期零配置读取（COMMON 也未加载）、握手包 unit codec 单例、Envelope 双重包裹解码失败
- [x] 插件样式 GUI（SnapshotViewerScreen 只读 + @IPNIgnore + 分页）
- [x] Curios 集成（9.5.1+1.21.1，CuriosAccessoryProvider，[c] 宏）
- [x] Sophisticated Backpacks 集成（3.26.20.2244，Modrinth Maven 坐标，[i]/[c] 背包内部展开）
- [ ] 实机复测：[i]/[inv]/[ec]/[c]/[pack] 全宏 + 图标 + 居中 + GUI + Ctrl+Enter + SB 背包展开

### Forge 1.21.1

- [ ] 目标工程搭建（ForgeGradle 6 + Forge 1.21.1）
- [ ] 网络与渲染移植（参照 NeoForge 版，注意 Forge 侧 mixin refmap 与事件差异）

### Fabric 1.21.1

- [ ] 目标工程搭建（Loom + Fabric API，dc1211 序列化复用）
- [ ] Trinkets / Accessories 集成（AccessoriesProvider）

> 依赖坐标备忘：SB `maven.modrinth:sophisticated-backpacks:1.21.1-3.26.20.2244`、
> SC `maven.modrinth:sophisticated-core:1.21.1-1.5.31.2460`（Modrinth Maven，精确匹配用户整合包版本；
> cursemaven 8181786/8181881 是 Forge 版不可用）。Curios `top.theillusivec4.curios:curios-neoforge:9.5.1+1.21.1`。

## 阶段六：1.16.5 适配（Forge / Fabric，Java 8 + 旧 NBT + 旧聊天组件）

## 阶段七：全面测试和发布

- [ ] 7 个目标全部独立构建（构建矩阵日志）
- [ ] 客户端 + Dedicated Server 启动测试记录
- [ ] 可选集成（SB / Curios / Trinkets / Accessories）测试
- [ ] 原版客户端回退测试
- [ ] 兼容性表格 + 安装文档
