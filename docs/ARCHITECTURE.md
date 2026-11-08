# 架构说明

## 分层

```
┌──────────────────────────────────────────────────────────────────┐
│ targets/forge-1.20.1  targets/fabric-1.20.1  ... （7 个独立构建） │
│  - loader glue（注册网络通道 / 按键 / 配置 / Mixin）              │
│  - 版本适配层：InventoryReader / AccessoryReader / ChatRenderAdapter │
│  - ItemSerializer（116: NBT | 1201: NBT | 1211: DataComponents）  │
│  - integrations：Curios / Trinkets / Accessories / SB（可选）     │
├──────────────────────────────────────────────────────────────────┤
│ minecraft-common（阶段二引入）：版本无关的 MC 概念接口            │
├──────────────────────────────────────────────────────────────────┤
│ common-core：纯 Java 8 字节码，零 MC 依赖                         │
└──────────────────────────────────────────────────────────────────┘
```

## 关键数据流（服务端权威）

1. 玩家聊天：`看看我 [i] [c]`
2. **服务端**聊天事件（不绕过聊天验证/签名/取消机制）→ `MacroParser.parse()`
3. 服务端在**服务器线程**读取真实数据：
   - `[i]`：主手 ItemStack；若是容器类物品（如 Sophisticated Backpacks）→ `ItemContainerProvider.getContents()`
   - `[inv]`：主背包 + 快捷栏 + 盔甲 + 副手（非空槽位）
   - `[ec]`：服务端 `player.getInventoryEnderChest()`（禁止客户端伪造）
   - `[c]`：已安装的饰品 API 动态枚举全部已注册槽位（不硬编码槽位名），按槽位类型分组
   - `[pack]`：饰品 API 扫描装备物品 → `ItemContainerProvider.supports()` 识别背包 → 读取内部库存
4. `ShowcaseMessageFactory` 生成结构化 `ShowcaseMessage`（保留原文文字段 + 物品分组）
5. `MessageValidator`（物品数 / 单物品与整条消息字节上限 / 嵌套深度）+ `RateLimiter`
6. 广播：
   - ItemShowcase 客户端（握手时声明能力）→ `SHOWCASE_MESSAGE` 结构化包
   - 原版客户端 → 可读回退文本 + 原版 show_item HoverEvent

## 客户端渲染

- 客户端解码 `ItemStackPayload` → 真实 `ItemStack`（按版本各自的序列化格式）
- ChatRenderAdapter（每版本独立）注入聊天行绘制：
  - 物品占位文本按图标宽度预留水平空间
  - 用原版 `ItemRenderer`/`GuiGraphics` 绘制真实模型图标（数量 / 耐久条 / 附魔光效 / 自定义模型 / BEWLR）
  - 悬停 → 原版 `screen.getTooltipFromItem`（不做自己的 tooltip 系统）
  - 点击事件保持原版行为兼容
- 专用渲染失败时降级为占位字符并记录日志，不影响整条聊天。

## 协议

- 通道 `itemshowcase:main`，Envelope：`magic | protocolVersion | typeId | requestId | payloadLen | payload`
- 包类型：SHOWCASE_REQUEST / SHOWCASE_MESSAGE / SELECTED_ITEM_REQUEST / ACCESSORY_SNAPSHOT /
  INVENTORY_SNAPSHOT / ENDER_CHEST_SNAPSHOT / BACKPACK_SNAPSHOT / CLIENT_CAPABILITY_HANDSHAKE
- ItemStack 载荷带 `dialect` 标记（`nbt116` / `nbt1201` / `dc1211`），不跨格式假设。
- Ctrl+Enter：客户端只发 `menuId + slotIndex`，服务端校验当前打开菜单与槽位归属后**重新**读取 ItemStack。
