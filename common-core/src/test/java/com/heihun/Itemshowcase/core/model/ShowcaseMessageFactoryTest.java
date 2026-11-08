package com.heihun.Itemshowcase.core.model;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.heihun.Itemshowcase.core.macros.MacroParser;
import com.heihun.Itemshowcase.core.macros.MacroType;
import com.heihun.Itemshowcase.core.protocol.ItemStackPayload;
import com.heihun.Itemshowcase.core.spi.AccessorySlotData;
import com.heihun.Itemshowcase.core.spi.BackpackData;
import com.heihun.Itemshowcase.core.spi.ItemStackView;
import com.heihun.Itemshowcase.core.spi.PlayerDataViews;
import com.heihun.Itemshowcase.core.spi.SlotItem;
import com.heihun.Itemshowcase.core.config.ItemShowcaseConfig;
import com.heihun.Itemshowcase.core.text.TextPayload;
import com.heihun.Itemshowcase.core.validate.MessageValidator;
import com.heihun.Itemshowcase.core.validate.ShowcaseLimits;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShowcaseMessageFactoryTest {

    // ---------------- 桩实现 ----------------

    /** 测试用 ItemStackView：固定载荷，用于验证工厂逻辑与消息结构。 */
    private static final class StubView implements ItemStackView {
        private final String id;
        private final int count;
        private final String plainName;

        StubView(String id, int count, String plainName) {
            this.id = id;
            this.count = count;
            this.plainName = plainName;
        }

        @Override public Object platformStack() { return this; }
        @Override public int getCount() { return count; }
        @Override public boolean isEmpty() { return count <= 0; }
        @Override public String itemId() { return id; }
        @Override public String plainDisplayName() { return plainName; }
        @Override public ItemStackPayload toPayload() {
            return new ItemStackPayload(1, "stub", (id + ":" + plainName).getBytes(), count, id);
        }
    }

    private static final class StubViews implements PlayerDataViews {
        SlotItem mainHand = new SlotItem(new StubView("minecraft:stone", 1, "石头"), 0, SourceKind.HAND);
        List<SlotItem> inventory = Arrays.asList(
                new SlotItem(new StubView("minecraft:apple", 3, "苹果"), 0, SourceKind.INVENTORY),
                new SlotItem(new StubView("minecraft:air", 0, "空"), 1, SourceKind.INVENTORY), // 应被过滤
                new SlotItem(new StubView("minecraft:iron_sword", 1, "铁剑"), 2, SourceKind.INVENTORY));
        List<SlotItem> enderChest = Arrays.asList(
                new SlotItem(new StubView("minecraft:gold_ingot", 12, "金锭"), 0, SourceKind.ENDER_CHEST));
        List<AccessorySlotData> accessories = Arrays.asList(
                new AccessorySlotData("curios", "ring", 0, new StubView("testmod:ring_a", 1, "戒指 A")),
                new AccessorySlotData("curios", "ring", 1, new StubView("testmod:ring_b", 1, "戒指 B")),
                new AccessorySlotData("curios", "charm", 0, new StubView("testmod:charm", 1, "护符")),
                new AccessorySlotData("curios", "back", 0, null)); // 空槽位应被过滤
        List<BackpackData> equippedPacks = Arrays.asList(
                new BackpackData(new StubView("sophisticatedbackpacks:backpack", 1, "背包"),
                        Arrays.asList(new StubView("minecraft:dirt", 32, "泥土"),
                                new StubView("minecraft:oak_log", 7, "橡木原木")),
                        "sophisticatedbackpacks"));
        BackpackData inspectResult = null;

        @Override public SlotItem getMainHand() { return mainHand; }
        @Override public List<SlotItem> getInventory() { return inventory; }
        @Override public List<SlotItem> getEnderChest() { return enderChest; }
        @Override public List<AccessorySlotData> getAccessories() { return accessories; }
        @Override public List<BackpackData> getEquippedBackpacks() { return equippedPacks; }
        @Override public BackpackData inspectBackpack(SlotItem stack, int maxDepth) { return inspectResult; }
    }

    private static ItemShowcaseConfig.Server serverConfig() {
        ItemShowcaseConfig.Server config = new ItemShowcaseConfig.Server();
        config.applyDefaults();
        config.sanitize();
        return config;
    }

    private static ShowcaseMessage build(StubViews views, String chat) {
        ShowcaseMessageFactory factory = new ShowcaseMessageFactory(views, serverConfig());
        return factory.build(MacroParser.parse(chat), UUID.randomUUID(), "Tester");
    }

    // ---------------- 测试 ----------------

    @Test
    void plainTextMessageHasNoItemSegments() {
        ShowcaseMessage message = build(new StubViews(), "大家好");
        assertEquals(1, message.segments().size());
        assertEquals(ShowcaseSegment.Type.TEXT, message.segments().get(0).type());
        assertEquals(0, message.countItems());
    }

    @Test
    void handMacroProducesSingleItemGroup() {
        ShowcaseMessage message = build(new StubViews(), "看看 [i]");
        assertEquals(2, message.segments().size());
        assertTrue(message.segments().get(0) instanceof TextSegment);
        ItemGroupSegment group = (ItemGroupSegment) message.segments().get(1);
        assertEquals(SourceKind.HAND, group.source());
        assertEquals(1, group.items().size());
        assertEquals("minecraft:stone", group.items().get(0).item().itemIdHint());
        assertEquals(1, message.countItems());
    }

    @Test
    void inventoryMacroFiltersEmptySlots() {
        ShowcaseMessage message = build(new StubViews(), "[inv]");
        ItemGroupSegment group = (ItemGroupSegment) message.segments().get(0);
        assertEquals(2, group.items().size());
        assertEquals("minecraft:apple", group.items().get(0).item().itemIdHint());
        assertEquals("minecraft:iron_sword", group.items().get(1).item().itemIdHint());
    }

    @Test
    void curiosMacroGroupsBySlotType() {
        ShowcaseMessage message = build(new StubViews(), "[c]");
        // 结构：Section(ring) Group(ring) Section(charm) Group(charm) —— 空槽位被过滤
        long sections = message.segments().stream().filter(s -> s.type() == ShowcaseSegment.Type.SECTION).count();
        long groups = message.segments().stream().filter(s -> s.type() == ShowcaseSegment.Type.ITEM_GROUP).count();
        assertEquals(2, sections);
        assertEquals(2, groups);

        ItemGroupSegment ringGroup = (ItemGroupSegment) message.segments().get(1);
        assertEquals("ring", ringGroup.title().value());
        assertEquals(2, ringGroup.items().size());
        assertEquals("ring", ringGroup.items().get(0).slotInfo());
    }

    @Test
    void packMacroExpandsBackpackContents() {
        ShowcaseMessage message = build(new StubViews(), "[pack]");
        boolean found = false;
        for (ShowcaseSegment segment : message.segments()) {
            if (segment instanceof ItemGroupSegment) {
                ItemGroupSegment group = (ItemGroupSegment) segment;
                if (group.source() == SourceKind.BACKPACK) {
                    found = true;
                    // 背包本身 + 2 个内部物品
                    assertEquals(3, group.items().size());
                    assertEquals("sophisticatedbackpacks:backpack", group.items().get(0).item().itemIdHint());
                    assertEquals(SourceKind.BACKPACK, group.items().get(0).source());
                    // 内部物品深度 = 1
                    assertEquals(1, group.items().get(1).depth());
                }
            }
        }
        assertTrue(found, "[pack] 应生成 BACKPACK 分组");
    }

    @Test
    void handMacroExpandsContainerWhenInspectSucceeds() {
        StubViews views = new StubViews();
        views.inspectResult = new BackpackData(
                new StubView("sophisticatedbackpacks:backpack", 1, "背包"),
                Arrays.asList(new StubView("minecraft:cobblestone", 64, "圆石")),
                "sophisticatedbackpacks");
        ShowcaseMessage message = build(views, "[i]");
        ItemGroupSegment group = (ItemGroupSegment) message.segments().get(0);
        assertEquals(2, group.items().size()); // 背包 + 内部物品
        assertEquals(1, group.items().get(1).depth());
        assertTrue(group.title() != null);
    }

    @Test
    void emptyHandProducesTranslatableFallback() {
        StubViews views = new StubViews();
        views.mainHand = new SlotItem(new StubView("minecraft:air", 0, ""), 0, SourceKind.HAND);
        ShowcaseMessage message = build(views, "[i]");
        TextSegment segment = (TextSegment) message.segments().get(0);
        assertEquals(TextPayload.Kind.TRANSLATABLE, segment.payload().kind());
        assertEquals(MacroType.ITEM.emptyKey(), segment.payload().value());
    }

    @Test
    void disabledMacroProducesTranslatableNotice() {
        ItemShowcaseConfig.Server config = serverConfig();
        config.allowMacroCurios = false;
        ShowcaseMessage message = new ShowcaseMessageFactory(new StubViews(), config)
                .build(MacroParser.parse("饰品 [c]"), UUID.randomUUID(), "Tester");
        TextSegment segment = (TextSegment) message.segments().get(1);
        assertEquals(MacroType.CURIOS.disabledKey(), segment.payload().value());
    }

    @Test
    void itemBudgetTruncatesAcrossMacros() {
        ItemShowcaseConfig.Server config = serverConfig();
        config.maxItemsPerMessage = 3;
        ShowcaseMessage message = new ShowcaseMessageFactory(new StubViews(), config)
                .build(MacroParser.parse("[inv] [c]"), UUID.randomUUID(), "Tester");
        assertTrue(message.countItems() <= 3, "应被限制为 3 个物品, 实际 " + message.countItems());
    }

    @Test
    void messageWireRoundTrip() {
        ShowcaseMessage message = build(new StubViews(), "文字 [i] [c] 更多文字");
        byte[] encoded = message.encodeToBytes();
        ShowcaseMessage decoded = ShowcaseMessage.decodeFromBytes(encoded);

        assertEquals(message.senderName(), decoded.senderName());
        assertEquals(message.messageId(), decoded.messageId());
        assertEquals(message.segments().size(), decoded.segments().size());
        assertEquals(message.countItems(), decoded.countItems());

        ItemGroupSegment originalGroup = (ItemGroupSegment) message.segments().get(1);
        ItemGroupSegment decodedGroup = (ItemGroupSegment) decoded.segments().get(1);
        assertEquals(originalGroup.items().get(0).item().itemIdHint(),
                decodedGroup.items().get(0).item().itemIdHint());
        assertEquals(originalGroup.items().get(0).item().count(),
                decodedGroup.items().get(0).item().count());
    }

    @Test
    void validatorAcceptsNormalMessage() {
        ShowcaseMessage message = build(new StubViews(), "[inv] [c]");
        MessageValidator.ValidationResult result =
                MessageValidator.validate(message, ShowcaseLimits.fromConfig(serverConfig()));
        assertTrue(result.ok(), "应通过校验: " + result.violations());
    }

    @Test
    void validatorRejectsOversizedItem() {
        StubViews views = new StubViews();
        views.mainHand = new SlotItem(new ItemStackView() {
            @Override public Object platformStack() { return this; }
            @Override public int getCount() { return 1; }
            @Override public boolean isEmpty() { return false; }
            @Override public String itemId() { return "testmod:huge"; }
            @Override public String plainDisplayName() { return "巨大物品"; }
            @Override public ItemStackPayload toPayload() {
                return new ItemStackPayload(1, "stub", new byte[8192], 1, "testmod:huge");
            }
        }, 0, SourceKind.HAND);
        ShowcaseMessage message = build(views, "[i]");
        MessageValidator.ValidationResult result =
                MessageValidator.validate(message, ShowcaseLimits.fromConfig(serverConfig()));
        assertTrue(!result.ok(), "超大数据的物品应被拒绝");
    }

    @Test
    void plainTextRenderingContainsNames() {
        ShowcaseMessage message = build(new StubViews(), "看 [inv]");
        String plain = message.toPlainText(key -> key);
        assertTrue(plain.contains("苹果"));
        assertTrue(plain.contains("x3"));
    }
}
