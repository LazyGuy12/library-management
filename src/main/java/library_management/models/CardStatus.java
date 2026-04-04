package library_management.models;

/**
 * Enum đại diện cho trạng thái của thẻ độc giả
 * ACTIVE: Thẻ còn hiệu lực, có thể mượn sách
 * INACTIVE: Thẻ không còn hiệu lực, không thể mượn sách
 * EXPIRED: Thẻ hết hạn, cần gia hạn để tiếp tục sử dụng
 */
public enum CardStatus {
    ACTIVE("Đang hoạt động"),
    INACTIVE("Không hoạt động"),
    EXPIRED("Hết hạn");

    private final String displayName;

    CardStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
