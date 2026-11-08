package com.heihun.Itemshowcase.core.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.heihun.Itemshowcase.core.config.ItemShowcaseConfig;
import com.heihun.Itemshowcase.core.macros.MacroParseResult;
import com.heihun.Itemshowcase.core.macros.MacroType;
import com.heihun.Itemshowcase.core.macros.ParsedSegment;
import com.heihun.Itemshowcase.core.spi.AccessorySlotData;
import com.heihun.Itemshowcase.core.spi.BackpackData;
import com.heihun.Itemshowcase.core.spi.PlayerDataViews;
import com.heihun.Itemshowcase.core.spi.SlotItem;
import com.heihun.Itemshowcase.core.spi.ItemStackView;
import com.heihun.Itemshowcase.core.text.TextPayload;

/**
 * 结构化展示消息构建器（服务端）。
 *
 * <p>把 {@link MacroParseResult}（宏解析）与 {@link PlayerDataViews}（真实库存数据）
 * 组装为 {@link ShowcaseMessage}。所有空物品在进入消息前被过滤；
 * 物品总数受配置上限约束，超出部分截断并附加提示文本。</p>
 *
 * <p>本类不依赖任何 Minecraft 类型，可用桩数据做完整单元测试。</p>
 */
public final class ShowcaseMessageFactory {

    private final PlayerDataViews views;
    private final ItemShowcaseConfig.Server config;

    public ShowcaseMessageFactory(PlayerDataViews views, ItemShowcaseConfig.Server config) {
        this.views = views;
        this.config = config;
    }

    /** 由解析结果构建完整消息。 */
    public ShowcaseMessage build(MacroParseResult parsed, UUID sender, String senderName) {
        List<ShowcaseSegment> segments = new ArrayList<>();
        ItemBudget budget = new ItemBudget(config.maxItemsPerMessage);

        for (ParsedSegment segment : parsed.segments()) {
            if (segment.isText()) {
                String text = segment.text();
                if (!text.isEmpty()) {
                    segments.add(new TextSegment(text));
                }
                continue;
            }
            MacroType macro = segment.macro();
            if (!config.isMacroEnabled(macro)) {
                segments.add(new TextSegment(TextPayload.translatable(macro.disabledKey())));
                continue;
            }
            switch (macro) {
                case ITEM:
                    appendHand(segments, budget);
                    break;
                case INVENTORY:
                    appendInventory(segments, budget);
                    break;
                case ENDER_CHEST:
                    appendEnderChest(segments, budget);
                    break;
                case CURIOS:
                    appendCurios(segments, budget);
                    break;
                case PACK:
                    appendPacks(segments, budget);
                    break;
                default:
                    // 未知宏不阻断消息：当作普通文字忽略
                    break;
            }
        }
        return new ShowcaseMessage(
                com.heihun.Itemshowcase.core.ItemShowcaseConstants.PROTOCOL_VERSION,
                UUID.randomUUID(), sender, senderName, System.currentTimeMillis(), segments);
    }

    // ---------------- 各宏展开 ----------------

    /** [i]：主手物品；若为可识别的容器物品（如 Sophisticated Backpacks）则同时展开内部库存。 */
    private void appendHand(List<ShowcaseSegment> out, ItemBudget budget) {
        SlotItem hand = views.getMainHand();
        if (hand == null || hand.isEmpty()) {
            out.add(new TextSegment(TextPayload.translatable(MacroType.ITEM.emptyKey())));
            return;
        }
        BackpackData backpack = null;
        if (config.expandContainerItems) {
            try {
                backpack = views.inspectBackpack(hand, config.maxNestingDepth);
            } catch (Throwable t) {
                // 容器读取失败按普通物品展示——聊天消息不能因为集成问题丢失
                com.heihun.Itemshowcase.core.logging.ItemShowcaseLogger.warn("读取容器物品内部库存失败", t);
            }
        }
        if (backpack != null && !backpack.contents().isEmpty()) {
            out.add(buildBackpackGroup(backpack, SourceKind.HAND, budget));
        } else {
            List<ItemSegment> items = new ArrayList<>(1);
            items.add(toItemSegment(hand, budget));
            out.add(new ItemGroupSegment(SourceKind.HAND, null, items));
        }
    }

    /** [inv]：快捷栏 + 主背包 + 盔甲 + 副手中的非空物品。 */
    private void appendInventory(List<ShowcaseSegment> out, ItemBudget budget) {
        List<SlotItem> inventory = views.getInventory();
        List<ItemSegment> items = collectNonEmpty(inventory, SourceKind.INVENTORY, budget);
        if (items.isEmpty()) {
            out.add(new TextSegment(TextPayload.translatable(MacroType.INVENTORY.emptyKey())));
            return;
        }
        out.add(new ItemGroupSegment(SourceKind.INVENTORY, null, items));
    }

    /** [ec]：服务端真实末影箱内容。 */
    private void appendEnderChest(List<ShowcaseSegment> out, ItemBudget budget) {
        List<SlotItem> enderChest = views.getEnderChest();
        List<ItemSegment> items = collectNonEmpty(enderChest, SourceKind.ENDER_CHEST, budget);
        if (items.isEmpty()) {
            out.add(new TextSegment(TextPayload.translatable(MacroType.ENDER_CHEST.emptyKey())));
            return;
        }
        out.add(new ItemGroupSegment(SourceKind.ENDER_CHEST, null, items));
    }

    /**
     * [c]：全部饰品槽位。生成单一分组（标题 = 翻译键"饰品"，客户端本地化），
     * 每个物品的 slotInfo 保留饰品槽位类型 ID（供客户端 GUI 分组显示）。
     * 槽位来自饰品 API 动态查询，本方法不做任何槽位名判断。
     */
    private void appendCurios(List<ShowcaseSegment> out, ItemBudget budget) {
        List<AccessorySlotData> accessories = views.getAccessories();
        if (accessories.isEmpty()) {
            out.add(new TextSegment(TextPayload.translatable(MacroType.CURIOS.emptyKey())));
            return;
        }
        List<ItemSegment> items = new ArrayList<>();
        for (AccessorySlotData slot : accessories) {
            if (slot.isEmpty()) {
                continue;
            }
            if (!budget.tryConsume()) {
                break;
            }
            items.add(new ItemSegment(
                    slot.stack().toPayload(),
                    slot.stack().getCount(),
                    slot.stack().plainDisplayName(),
                    null,
                    SourceKind.CURIO,
                    slot.slotTypeId(),
                    slot.slotIndex(),
                    0));
        }
        if (items.isEmpty()) {
            out.add(new TextSegment(TextPayload.translatable(MacroType.CURIOS.emptyKey())));
            return;
        }
        out.add(new ItemGroupSegment(SourceKind.CURIO,
                TextPayload.translatable(MacroType.CURIOS.titleKey()), items));
    }

    /** [pack]：饰品栏中装备的所有可识别背包及其内部物品。 */
    private void appendPacks(List<ShowcaseSegment> out, ItemBudget budget) {
        List<BackpackData> packs = views.getEquippedBackpacks();
        if (packs == null || packs.isEmpty()) {
            out.add(new TextSegment(TextPayload.translatable(MacroType.PACK.emptyKey())));
            return;
        }
        boolean any = false;
        for (BackpackData pack : packs) {
            if (pack == null || pack.isEmpty()) {
                continue;
            }
            out.add(buildBackpackGroup(pack, SourceKind.BACKPACK, budget));
            any = true;
        }
        if (!any) {
            out.add(new TextSegment(TextPayload.translatable(MacroType.PACK.emptyKey())));
        }
    }

    /** 构建一个“背包 + 内部物品”的分组。内容物不受消息级物品预算限制（独立限额 maxContainerContents 由校验器把关）。 */
    private ItemGroupSegment buildBackpackGroup(BackpackData backpack, SourceKind source, ItemBudget budget) {
        List<ItemSegment> items = new ArrayList<>();
        // 第一个元素是背包本身（消耗预算：它是直接展示的物品；槽位号 -1 = 容器本体）
        if (budget.tryConsume()) {
            ItemStackView stack = backpack.backpack();
            items.add(new ItemSegment(
                    stack.toPayload(),
                    stack.getCount(),
                    stack.plainDisplayName(),
                    null,
                    source,
                    backpack.providerId(),
                    -1,
                    0));
        }
        // 后面是内部物品（深度 1；槽位号 = 内容物列表索引，数量受 maxContainerContents 限制，校验器兜底）
        int depth = 1;
        if (depth <= config.maxNestingDepth) {
            int contentLimit = config.maxContainerContents;
            int added = 0;
            for (ItemStackView content : backpack.contents()) {
                if (added >= contentLimit) {
                    break;
                }
                if (content == null || content.isEmpty()) {
                    continue;
                }
                items.add(new ItemSegment(
                        content.toPayload(),
                        content.getCount(),
                        content.plainDisplayName(),
                        null,
                        SourceKind.BACKPACK,
                        backpack.providerId(),
                        added,
                        depth));
                added++;
            }
        }
        return new ItemGroupSegment(source, TextPayload.literal(backpack.backpack().plainDisplayName()), items);
    }

    /** 过滤空槽位并转为 ItemSegment，遵守预算。 */
    private List<ItemSegment> collectNonEmpty(List<SlotItem> slots, SourceKind source, ItemBudget budget) {
        List<ItemSegment> items = new ArrayList<>();
        if (slots == null) {
            return items;
        }
        for (SlotItem slot : slots) {
            if (slot == null || slot.isEmpty()) {
                continue;
            }
            if (!budget.tryConsume()) {
                break;
            }
            items.add(toItemSegment(slot, budget));
        }
        return items;
    }

    private ItemSegment toItemSegment(SlotItem slot, ItemBudget budget) {
        ItemStackView stack = slot.stack();
        return new ItemSegment(
                stack.toPayload(),
                stack.getCount(),
                stack.plainDisplayName(),
                null,
                slot.source() == null ? SourceKind.INVENTORY : slot.source(),
                null,
                slot.slotIndex(),
                0);
    }

    /** 消息级物品预算：跨所有宏共享，保证“单条消息最大物品数量”生效。 */
    private static final class ItemBudget {
        private int remaining;

        ItemBudget(int maxItems) {
            this.remaining = Math.max(1, maxItems);
        }

        boolean tryConsume() {
            if (remaining <= 0) {
                return false;
            }
            remaining--;
            return true;
        }
    }
}
