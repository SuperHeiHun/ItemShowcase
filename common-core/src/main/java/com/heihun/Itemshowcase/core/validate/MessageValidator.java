package com.heihun.Itemshowcase.core.validate;

import java.util.ArrayList;
import java.util.List;

import com.heihun.Itemshowcase.core.model.ItemGroupSegment;
import com.heihun.Itemshowcase.core.model.ItemSegment;
import com.heihun.Itemshowcase.core.model.ShowcaseMessage;
import com.heihun.Itemshowcase.core.model.ShowcaseSegment;

/**
 * 展示消息校验器：物品数量、数据体积、嵌套深度、空物品。
 *
 * <p>校验失败的消息不广播——由服务端在生成后、发送前调用。</p>
 */
public final class MessageValidator {

    private MessageValidator() {
    }

    /** 校验结果：ok == false 时 violations 描述所有违规项。 */
    public static final class ValidationResult {
        private final boolean ok;
        private final List<String> violations;

        private ValidationResult(boolean ok, List<String> violations) {
            this.ok = ok;
            this.violations = violations;
        }

        public boolean ok() {
            return ok;
        }

        public List<String> violations() {
            return violations;
        }

        @Override
        public String toString() {
            return "ValidationResult{ok=" + ok + ", violations=" + violations + "}";
        }
    }

    public static ValidationResult validate(ShowcaseMessage message, ShowcaseLimits limits) {
        List<String> violations = new ArrayList<>();
        int itemCount = 0;
        int totalBytes = 0;

        for (ShowcaseSegment segment : message.segments()) {
            if (segment instanceof ItemSegment) {
                itemCount += inspectItem((ItemSegment) segment, limits, violations);
            } else if (segment instanceof ItemGroupSegment) {
                ItemGroupSegment group = (ItemGroupSegment) segment;
                for (ItemSegment item : group.items()) {
                    itemCount += inspectItem(item, limits, violations);
                }
            }
        }

        if (itemCount > limits.maxItemsPerMessage()) {
            violations.add("物品数量超限: " + itemCount + " > " + limits.maxItemsPerMessage());
        }
        // 整条消息的字节规模（用协议编码后的大小衡量最准确）
        try {
            totalBytes = message.encodeToBytes().length;
        } catch (RuntimeException encodingFailure) {
            violations.add("消息编码失败: " + encodingFailure);
        }
        if (totalBytes > limits.maxMessageBytes()) {
            violations.add("消息数据量超限: " + totalBytes + " > " + limits.maxMessageBytes() + " 字节");
        }
        return new ValidationResult(violations.isEmpty(), violations);
    }

    /** 校验单个物品，返回有效物品数（0 表示该物品被拒绝）。 */
    private static int inspectItem(ItemSegment item, ShowcaseLimits limits, List<String> violations) {
        if (item.item().count() <= 0) {
            violations.add("空物品不允许发送: " + item.item().itemIdHint());
            return 0;
        }
        if (item.item().sizeBytes() > limits.maxItemBytes()) {
            violations.add("物品数据量超限 (" + item.item().itemIdHint() + "): "
                    + item.item().sizeBytes() + " > " + limits.maxItemBytes() + " 字节");
            return 0;
        }
        if (item.depth() > limits.maxNestingDepth()) {
            violations.add("物品嵌套深度超限 (" + item.item().itemIdHint() + "): "
                    + item.depth() + " > " + limits.maxNestingDepth());
            return 0;
        }
        return 1;
    }
}
