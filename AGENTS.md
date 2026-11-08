# ItemShowcase 开发记忆

跨版本 Minecraft 聊天物品展示 MOD。**所有交流使用中文。**

## 项目关键信息

- **主包名**：`com.heihun.Itemshowcase`（注意 I 大写）；MOD ID：`itemshowcase`（必须全小写）
- **完整开发规范**：见仓库根 README.md / docs/PHASES.md / docs/ARCHITECTURE.md / docs/COMPATIBILITY.md
- **开发顺序（规范强制）**：阶段一 common-core → 阶段二 Forge 1.20.1 MVP → 阶段三饰品/背包 → 阶段四 Fabric 1.20.1 → 阶段五 1.21.1 → 阶段六 1.16.5 → 阶段七测试发布。每阶段先构建测试修复再进入下一阶段。

## 构建环境（本机）

- **GRADLE_USER_HOME** = `E:\GradleCache`（代理配置在其 gradle.properties，不在 C:\Users\...\.gradle）
- **HTTP 代理**：`127.0.0.1:7890`（已写入 E:\GradleCache\gradle.properties 的 systemProp）
- **JDK（Zulu，C:\Program Files\Java\）**：zulu8.94.0.17 / zulu17.66.19 / zulu21.50.19 / zulu25.34.17
- 全局 Gradle 9.1 位于 D:\App_data\gradle-9.1.0
- 各目标工程的 `gradle.properties` 用 `org.gradle.java.home` 指定运行 JVM（FG6 用 zulu17），
  `org.gradle.java.installations.paths` 列出全部 JDK 供 toolchain 探测（common-core 需要 21）
- 每个目标目录是**独立 Gradle 构建**（自带 wrapper）：forge-1.20.1 = Gradle 8.8 + ForgeGradle 6 + MixinGradle 0.7
- common-core 通过 Composite Build（settings.gradle 的 includeBuild + dependencySubstitution）被目标以源码消费

## 分层与约束

- `common-core`：**纯 Java 8 字节码**（options.release = 8，禁用 var/record/List.of），
  零 Minecraft 依赖；构建脚本必须兼容老 Gradle（7.6+，因为 1.16.5 的 FG5 会 includeBuild 它）
- 核心模型不引用任何版本的 ItemStack：物品数据用 `ItemStackPayload`（byte[] + dialect 方言标记
  nbt116 / nbt1201 / dc1211）；文本用 `TextPayload`（LITERAL / TRANSLATABLE / JSON）
- 服务端权威：客户端只发菜单ID+槽位ID，服务端重读真实物品；末影箱走服务端真实数据
- 可选集成（Curios / SB）用 `ModList.isLoaded` 检测后才实例化集成类（类加载安全），
  依赖全部 compileOnly/runtimeOnly，**不打包进产物**

## 1.20.1 实战经验（重要，避免重复踩坑）

- **验证 MC API 一律用 javap 查 FG 映射 jar**（不要凭记忆）：
  `E:\GradleCache\caches\forge_gradle\minecraft_user_repo\net\minecraftforge\forge\1.20.1-47.3.0_mapped_official_1.20.1\forge-1.20.1-47.3.0_mapped_official_1.20.1.jar`
- 已踩过的 API 坑：`ChatComponent.render` = `(GuiGraphics;III)V`（无 boolean）；
  行绘制调用 5 参 `drawString(Font,FormattedCharSequence,III)I`（无 boolean）；
  `HoverEvent.ItemStackInfo`（不是 ItemInfo）；`NbtIo.readCompressed(InputStream)` 单参；
  `Player.getEnderChestInventory()` → `PlayerEnderChestContainer`；
  `Font.getSplitter()`；Curios 5.14 用 `CuriosApi.getCuriosInventory(entity)`（getCuriosHandler 已过时）；
  SB 的 `IBackpackWrapper`/`BackpackWrapper` 在 `backpack.wrapper` 包，用 `new BackpackWrapper(stack)`
- **Mixin @At target 字符串里类全名必须精确**（FormattedCharSequence 是 net.minecraft.util 不是 client.gui）；
  查 SRG 名用 build/createMcpToSrg/output.tsrg
- **FormattedCharSink 的 index 参数在换行后的行序列中不可靠**（不保证行内序号）——
  ChatIconOverlay 已改为自计行内序号 + StringSplitter 度量前缀
- ChatComponent.getLineHeight() = 原版"聊天间距"设置换算值；行距扩展注入其 RETURN

## 当前聊天渲染模型（Forge 1.20.1）

- 客户端 S2C 结构化包 → 占位组件：`名字 + [图标占位6空格(颜色编码0x010000|id<<8|idx)] + [物品名]`
- **两种占位条目**（ShowcaseMessageHandler.ChatEntry）：ItemEntry（[i] 平铺图标）与
  ContainerEntry（[inv]/[ec]/[c]/[pack] → 可点击入口，图标 + "[背包 xN]"）
- **点击入口打开原生箱子界面**（SnapshotViewerScreen extends ContainerScreen；1.20.1 官方映射名是
  ContainerScreen 不是 ChestScreen！），ReadOnlyChestMenu 覆写 clicked/quickMoveStack 为空 → 完全只读且不发包；
  **@IPNIgnore**（org.anti_ad.mc.ipn.api；compileOnly org.anti_ad.mc:IPN-forge-1.20:1.10.9 @ maven.ipn-mod.org）
  禁用 Inventory Profiles Next 注入
- **GUI 布局（参照 HBetterChat 插件）**：[inv] → 9x6（顶两行装饰玻璃板+玩家头颅（SkullOwner NBT）+
  盔甲/副手行 + 主背包27格 + 快捷栏9格）；[ec] → 9x5（装饰+头颅+27格）；[pack]/[i]容器 → 平铺+分页（左右键+按钮）
- **[i] 命中容器展开的组（HAND 组带 title）也作为入口**，点击查看背包内部
- **宏别名**：MacroType 支持 token 数组，PACK 别名 "[p]"；解析器按主 token 长度降序匹配
- **限额**：平铺物品 maxItemsPerMessage=54；**容器内部展开独立限额 maxContainerContents=108**
  （depth>=1 在 MessageValidator 单独计数，工厂不消耗消息预算）——解决大背包只显示 54 格
- 行距：extraLineSpacing 默认 28（行步进 9+28=37px）；文字/图标整体**上移** floorDiv(extra,2)（用户要求向上偏移）
- **Forge 1.20.1 配置**：已切换到 Forge 原生 TOML（ForgeConfigs：ForgeConfigSpec + ModConfigEvent 自动热重载），
  config/itemshowcase-common.toml（服务端）+ itemshowcase-client.toml（客户端）；值同步回 common-core 配置对象；
  JSON ConfigManager 仅剩其他目标版本使用；ModConfigEvent 包路径 = net.minecraftforge.fml.event.config
- **阶段五已开始（NeoForge 1.21.1）**：targets/neoforge-1.21.1（MDG 2.0.148 + NeoForge 21.1.265 + Gradle 8.8 + Java 21），
  编译通过（itemshowcase-neoforge-21.1-1.0.0.jar）。1.21.1 关键差异（javap 已验证）：
  ChatComponent.render=(GuiGraphics;IIIZ)V（恢复 boolean）；getLineHeight 为 private int（可注入）；
  网络用 CustomPacketPayload + RegisterPayloadHandlersEvent/PayloadRegistrar（StreamCodec 泛型可用
  RegistryFriendlyByteBuf 的超类 FriendlyByteBuf）；IPayloadContext.player() 返回 Player（非 Optional）；
  ItemStack 网络 = ItemStack.STREAM_CODEC（RegistryFriendlyByteBuf + RegistryAccess）；
  1.21.1 无 MinecraftServer.getServer() → 用 NeoForge ServerLifecycleHooks.getCurrentServer() 或 player.level().getServer()；
  组件化：setHoverName → stack.set(DataComponents.CUSTOM_NAME,..)，玩家头 → DataComponents.PROFILE + ResolvableProfile(GameProfile)；
  NeoForge 运行时 = Mojang 官方映射 → mixin 无需 refmap；
  **Mixin 冲突教训**：聊天行绘制不要用 @Redirect（用户实机环境 malilib/tweakeroo 等也注入
  ChatComponent.render 的 drawString → 0/1 targets 硬崩溃）；用 MixinExtras @WrapOperation
  （NeoForge 21.1 内置 0.5.5 运行时；编译用 compileOnly com.llamalad7:mixinextras-common:0.4.1——
  注意 0.4.1 包名是 com.llamalad7 无 io.github 前缀）；次要注入点加 require=0 优雅降级不崩游戏；
  Curios neoforge 9.5.1+1.21.1（api 分类器）：CuriosApi.getCuriosInventory(entity) 返回 Optional；
  **NeoForge 配置加载时机教训（实机二次踩坑后修正）**：**COMMON 和 CLIENT 配置在 mod 构造期都未加载**
  （ModConfigEvent.Loading 在构造之后才触发）——构造期必须零配置读取，值同步只放在
  ModConfigEvent.Loading/Reloading（syncServer/syncClient），否则 ConfigValue.get() 抛
  "Cannot get config value before config is loaded" mod 实例创建失败；防御手段 ModConfigSpec.isLoaded()，
  加载前保留 common-core 默认值（字段内联默认 + sanitize 兜底）；
  待办：SB/SC 1.21.1 的 cursemaven 8181786/8181881 是 Forge 版（引用 net.minecraftforge.items 不可用），
  需查 NeoForge 版文件 ID 后恢复 SophisticatedBackpacksIntegration（代码在 git 历史可找回）；
  用户整合包（1.21.1 汇流来世，NeoForge 21.1.255）装有 SB/SC/Curios，可作实机集成验证；
  **NeoForge unit codec 教训**：无字段 payload 用 StreamCodec.unit(INSTANCE) 时，发送必须复用 INSTANCE
  （PacketDistributor.sendToServer(ClientHandshakePayload.INSTANCE)）——new 新实例会在编码期抛
  "Can't encode '新实例', expected '单例'" → EncoderException → 进服断线（Forge 1.20.1 SimpleChannel
  无此检查所以未暴露）；构造器私有化防再犯；
  **移植对称性教训**：NeoForge 的 sendShowcaseMessage 曾多包一层 Envelope 信封，而 S2C 端按裸
  ShowcaseMessage 解码 → 每条消息 FormatException 全部丢弃（症状：读字符串长度前缀对不上、未知段类型 ID）；
  各平台收发两端必须与参考实现（Forge 1.20.1 = 裸 encodeToBytes 字节）逐字节对称，新目标先对照检查；
  **Mixin 静默失效排查法**：整合包 versions 目录下 `.mixin.out\class\` 有混合后字节码导出，
  javap 直接确认注入是否真的织入（处理器被复制 ≠ 调用点被改写）；占位 run 拍平/检测逻辑可用
  独立 Java 程序 + compiledWithNeoForge jar 离线验证（需补 en_us.json 资源）；
  **@Inject 与 @WrapOperation 混居同一 mixin 类的坑**：MixinExtras 0.5.5 延迟注入阶段 wrap 的
  调用点改写被静默跳过（require=0 掩盖），拆成独立 mixin 类后正常（justzoom 对照：纯 wrap 类正常）；
  文字在加高槽内居中 = 上移 floorDiv(extra,2)+1（+1 修正原版槽内自带的 1px 底部偏移，extra=0 时不偏移）；
  mixin 内加一次性日志（"聊天行渲染钩子已生效"）用于实机确认钩子真实生效
- **注意**：用户本地 config/itemshowcase-client.json 里已有旧值会覆盖新默认值（合并加载），调默认值后要提醒用户删旧字段
- 物品名优先用客户端解码 ItemStack 的 hover name（服务端 plainName 仅回退）
- 1.20.1 的 mouseScrolled 是 3 参（4 参是 1.20.2+）

## 用户偏好

- **游戏内测试（runClient 等）由用户自己做**，我不启动游戏，只保证构建通过
- 需要时给用户列出测试要点即可

## 构建命令

```powershell
# common-core（41 个测试）
cd common-core; gradle --no-daemon build
# Forge 1.20.1
cd targets/forge-1.20.1; .\gradlew.bat --no-daemon build
# 编译错误日志约定：输出重定向到 build-log-N.txt（序号递增），用 Select-String 查"错误|FAILED"
```

## 进度（截至最近提交）

- ✅ 阶段一：common-core 完成（41 测试通过，Java 8 字节码）
- ✅ 阶段二：Forge 1.20.1 MVP（BUILD SUCCESSFUL，jar 已含合并 common-core + refmap）
- ✅ 阶段三（Forge 1.20.1）：CuriosProvider + SophisticatedBackpacksIntegration + [c][pack][i]展开
- ✅ GUI 查看功能（参考 HBetterChat 插件交互）：[inv]/[ec]/[c]/[pack] 聊天内显示入口，点击打开 SnapshotViewerScreen
- 🔧 已修复：图标全部叠在玩家名处（行内序号）、物品名显示注册表 ID（客户端解码取名）、间距/行距
- ⏭️ 下一步：阶段四 Fabric 1.20.1（Loom + Fabric API，复用 core；聊天渲染 Mixin 移植 + 网络用 Fabric networking），
  然后阶段五 1.21.1（Forge/NeoForge/Fabric，Data Components 序列化 dc1211）、阶段六 1.16.5（Java 8）

## 依赖坐标备忘（1.20.1）

- Curios：`https://maven.theillusivec4.top/`，`top.theillusivec4.curios:curios-forge:5.14.1+1.20.1:api`(compileOnly) / 无分类器(runtimeOnly)
- Sophisticated Backpacks：`curse.maven:sophisticated-backpacks-422301:5881224`
- Sophisticated Core：`curse.maven:sophisticated-core-618298:9166087`（均经 `https://www.cursemaven.com/`）
