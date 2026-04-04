package library_management.models;

/**
 * Enum đại diện cho trạng thái của cuốn sách
 * AVAILABLE: Sách còn trong thư viện, có thể mượn
 * BORROWED: Sách đang được mượn bởi ai đó
 */
public enum BookStatus {
    AVAILABLE("Có sẵn"),
    BORROWED("Đang mượn");

    private final String displayName;

    BookStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
