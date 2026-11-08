package com.heihun.Itemshowcase.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.heihun.Itemshowcase.core.text.TextPayload;
import com.heihun.Itemshowcase.core.util.ByteReader;
import com.heihun.Itemshowcase.core.util.ByteWriter;

/**
 * 完整的结构化展示消息。
 *
 * <p>由服务端生成并通过 SHOWCASE_MESSAGE 包下发；mod 客户端解码后渲染，
 * 原版客户端收不到该包（服务端只给它发回退文本）。</p>
 */
public final class ShowcaseMessage {

    private final int protocolVersion;
    private final UUID messageId;
    private final UUID sender;
    private final String senderName;
    private final long timestamp;
    private final List<ShowcaseSegment> segments;

    public ShowcaseMessage(int protocolVersion, UUID messageId, UUID sender, String senderName,
                           long timestamp, List<ShowcaseSegment> segments) {
        this.protocolVersion = protocolVersion;
        this.messageId = (messageId == null) ? new UUID(0L, 0L) : messageId;
        this.sender = (sender == null) ? new UUID(0L, 0L) : sender;
        this.senderName = (senderName == null) ? "" : senderName;
        this.timestamp = timestamp;
        this.segments = Collections.unmodifiableList(new ArrayList<>(segments));
    }

    public int protocolVersion() {
        return protocolVersion;
    }

    public UUID messageId() {
        return messageId;
    }

    public UUID sender() {
        return sender;
    }

    public String senderName() {
        return senderName;
    }

    public long timestamp() {
        return timestamp;
    }

    public List<ShowcaseSegment> segments() {
        return segments;
    }

    /** 统计消息中的物品总数（含所有分组）。 */
    public int countItems() {
        int total = 0;
        for (ShowcaseSegment segment : segments) {
            if (segment instanceof ItemSegment) {
                total++;
            } else if (segment instanceof ItemGroupSegment) {
                total += ((ItemGroupSegment) segment).items().size();
            }
        }
        return total;
    }

    /** 展开为纯文本（原版回退的基础文本、日志用）。 */
    public String toPlainText(java.util.function.Function<String, String> localizer) {
        StringBuilder builder = new StringBuilder();
        appendPlainTextTo(builder, localizer);
        return builder.toString();
    }

    public void appendPlainTextTo(StringBuilder builder, java.util.function.Function<String, String> localizer) {
        for (ShowcaseSegment segment : segments) {
            if (segment instanceof TextSegment) {
                builder.append(resolveText(((TextSegment) segment).payload(), localizer));
            } else if (segment instanceof ItemSegment) {
                ItemSegment item = (ItemSegment) segment;
                builder.append('[').append(item.plainName());
                if (item.count() > 1) {
                    builder.append(" x").append(item.count());
                }
                builder.append(']');
            } else if (segment instanceof ItemGroupSegment) {
                ItemGroupSegment group = (ItemGroupSegment) segment;
                if (group.title() != null) {
                    builder.append(resolveText(group.title(), localizer)).append(": ");
                }
                boolean first = true;
                for (ItemSegment item : group.items()) {
                    if (!first) {
                        builder.append(", ");
                    }
                    first = false;
                    builder.append('[').append(item.plainName());
                    if (item.count() > 1) {
                        builder.append(" x").append(item.count());
                    }
                    builder.append(']');
                }
            } else if (segment instanceof SectionSegment) {
                builder.append(resolveText(((SectionSegment) segment).title(), localizer)).append(": ");
            }
        }
    }

    private static String resolveText(TextPayload payload, java.util.function.Function<String, String> localizer) {
        switch (payload.kind()) {
            case LITERAL:
                return payload.value();
            case TRANSLATABLE:
                if (localizer == null) {
                    return payload.value();
                }
                return localizer.apply(payload.value());
            case JSON:
            default:
                return payload.value();
        }
    }

    // ---------------- 线格式编解码 ----------------

    public void encode(ByteWriter writer) {
        writer.writeVarInt(protocolVersion);
        writer.writeUuid(messageId);
        writer.writeUuid(sender);
        writer.writeString(senderName);
        writer.writeLongBE(timestamp);
        writer.writeVarInt(segments.size());
        for (ShowcaseSegment segment : segments) {
            encodeSegment(segment, writer);
        }
    }

    private static void encodeSegment(ShowcaseSegment segment, ByteWriter writer) {
        writer.writeVarInt(segment.type().wireId());
        switch (segment.type()) {
            case TEXT:
                ((TextSegment) segment).payload().encode(writer);
                break;
            case ITEM:
                encodeItem((ItemSegment) segment, writer);
                break;
            case ITEM_GROUP: {
                ItemGroupSegment group = (ItemGroupSegment) segment;
                writer.writeVarInt(group.source().ordinal());
                writer.writeBoolean(group.title() != null);
                if (group.title() != null) {
                    group.title().encode(writer);
                }
                writer.writeVarInt(group.items().size());
                for (ItemSegment item : group.items()) {
                    encodeItem(item, writer);
                }
                break;
            }
            case SECTION:
                ((SectionSegment) segment).title().encode(writer);
                break;
            default:
                throw new IllegalStateException("未知片段类型: " + segment.type());
        }
    }

    private static void encodeItem(ItemSegment item, ByteWriter writer) {
        item.item().encode(writer);
        writer.writeVarInt(item.count());
        writer.writeString(item.plainName());
        writer.writeNullableString(item.displayNameJson());
        writer.writeVarInt(item.source().ordinal());
        writer.writeNullableString(item.slotInfo());
        writer.writeVarInt(item.depth());
    }

    public byte[] encodeToBytes() {
        ByteWriter writer = new ByteWriter(128);
        encode(writer);
        return writer.toByteArray();
    }

    public static ShowcaseMessage decodeFromBytes(byte[] bytes) {
        ByteReader reader = new ByteReader(bytes);
        ShowcaseMessage message = decode(reader);
        reader.expectEnd();
        return message;
    }

    public static ShowcaseMessage decode(ByteReader reader) {
        int protocolVersion = reader.readVarInt();
        UUID messageId = reader.readUuid();
        UUID sender = reader.readUuid();
        String senderName = reader.readString();
        long timestamp = reader.readLongBE();
        int segmentCount = reader.readVarInt();
        List<ShowcaseSegment> segments = new ArrayList<>(Math.min(segmentCount, 256));
        for (int i = 0; i < segmentCount; i++) {
            segments.add(decodeSegment(reader));
        }
        return new ShowcaseMessage(protocolVersion, messageId, sender, senderName, timestamp, segments);
    }

    private static ShowcaseSegment decodeSegment(ByteReader reader) {
        int wireId = reader.readVarInt();
        ShowcaseSegment.Type type = ShowcaseSegment.Type.byWireId(wireId);
        if (type == null) {
            throw new ByteReader.FormatException("未知片段类型 ID: " + wireId);
        }
        switch (type) {
            case TEXT:
                return new TextSegment(TextPayload.decode(reader));
            case ITEM:
                return decodeItem(reader);
            case ITEM_GROUP: {
                SourceKind source = SourceKind.values()[reader.readVarInt()];
                TextPayload title = reader.readBoolean() ? TextPayload.decode(reader) : null;
                int itemCount = reader.readVarInt();
                List<ItemSegment> items = new ArrayList<>(Math.min(itemCount, 256));
                for (int i = 0; i < itemCount; i++) {
                    items.add(decodeItem(reader));
                }
                return new ItemGroupSegment(source, title, items);
            }
            case SECTION:
                return new SectionSegment(TextPayload.decode(reader));
            default:
                throw new ByteReader.FormatException("未知片段类型: " + type);
        }
    }

    private static ItemSegment decodeItem(ByteReader reader) {
        com.heihun.Itemshowcase.core.protocol.ItemStackPayload payload =
                com.heihun.Itemshowcase.core.protocol.ItemStackPayload.decode(reader);
        int count = reader.readVarInt();
        String plainName = reader.readString();
        String displayNameJson = reader.readNullableString();
        SourceKind source = SourceKind.values()[reader.readVarInt()];
        String slotInfo = reader.readNullableString();
        int depth = reader.readVarInt();
        return new ItemSegment(payload, count, plainName, displayNameJson, source, slotInfo, depth);
    }
}
