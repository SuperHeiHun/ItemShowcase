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
- **点击入口打开原生箱子界面**：ChestMenu（GENERIC_9x1~9x6 按行数）+ ContainerScreen（1.20.1 官方映射名，
  不是 ChestScreen！），自定义 ReadOnlyChestMenu 覆写 clicked/quickMoveStack 为空 → 完全只读且不发包；
  渲染/玩家背包栏全部原版。参考用户 Bukkit 插件 HBetterChat 的"存快照+点击查看"交互；
  配置开关 clickViewGui（默认 true，false=平铺旧行为）。数据已在 SHOWCASE_MESSAGE 里，纯客户端实现
- **[i] 命中容器展开的组（HAND 组带 title）也作为入口**，点击查看背包内部
- 入口图标：inv=Items.CHEST、ec=Items.ENDER_CHEST（UI 按钮图标），pack/curios=组内第一个真实物品
- 物品数据存 LRU 缓存（id→List<ChatEntry>，客户端解码真实 ItemStack）
- Mixin 4 处：render HEAD（帧重置）、行 drawString Redirect（扫描占位 run 画图标；文字与图标整体下移
  floorDiv(extraLineSpacing,2) 实现行块内垂直居中）、ChatScreen render TAIL（悬停 Tooltip）+
  ChatScreen mouseClicked HEAD（入口点击）+ getLineHeight RETURN（行距，默认+7 → 行步进 16px）
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
