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

- [ ] minecraft-common 适配接口层
- [ ] Forge 1.20.1 目标工程（ForgeGradle 6 + Gradle 8.8 + Java 17）
- [ ] 服务端聊天事件接入 + 宏解析 + 真实库存读取（主手 / 背包 / 盔甲 / 副手 / 末影箱）
- [ ] 网络通道 + 结构化 ShowcaseMessage 下发 + 原版客户端回退消息
- [ ] 客户端握手（能力协商）
- [ ] 聊天物品图标渲染（ItemRenderer，数量 / 耐久 / 附魔光效）+ 原版 Tooltip
- [ ] 鼠标悬停映射、点击兼容、多行 / 滚动 / 缩放处理
- [ ] Ctrl+Enter 快捷发送（服务端菜单 + 槽位校验）
- [ ] en_us.json / zh_cn.json
- [ ] Forge 1.20.1 构建 + 客户端/服务端启动测试

## 阶段三：饰品和背包

- [ ] CuriosProvider（1.20.1 / 1.16.5 Forge，1.21.1 全家）
- [ ] TrinketsProvider（Fabric 各版本）
- [ ] AccessoriesProvider（1.21.1）
- [ ] SophisticatedBackpacksIntegration（IBackpackWrapper / INBTStorage 缓存，读取失败降级）
- [ ] `[c]` `[pack]` 宏 + `[i]` 特殊背包处理
- [ ] ItemContainerProvider 插件注册机制完善（为 Traveler's Backpack 等预留）

## 阶段四：Fabric 1.20.1 适配（复用 common-core）

## 阶段五：1.21.1 适配（Forge / NeoForge / Fabric，Data Components 序列化 + 新聊天渲染 API）

## 阶段六：1.16.5 适配（Forge / Fabric，Java 8 + 旧 NBT + 旧聊天组件）

## 阶段七：全面测试和发布

- [ ] 7 个目标全部独立构建（构建矩阵日志）
- [ ] 客户端 + Dedicated Server 启动测试记录
- [ ] 可选集成（SB / Curios / Trinkets / Accessories）测试
- [ ] 原版客户端回退测试
- [ ] 兼容性表格 + 安装文档
