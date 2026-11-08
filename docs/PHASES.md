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

## 阶段五：1.21.1 适配（Forge / NeoForge / Fabric，Data Components 序列化 + 新聊天渲染 API）

## 阶段六：1.16.5 适配（Forge / Fabric，Java 8 + 旧 NBT + 旧聊天组件）

## 阶段七：全面测试和发布

- [ ] 7 个目标全部独立构建（构建矩阵日志）
- [ ] 客户端 + Dedicated Server 启动测试记录
- [ ] 可选集成（SB / Curios / Trinkets / Accessories）测试
- [ ] 原版客户端回退测试
- [ ] 兼容性表格 + 安装文档
