package library_management.models;

/**
 * Enum đại diện cho các trạng thái của Loan (mượn sách)
 * 
 * PENDING: Chờ nhận sách tại thư viện (sau khi user đặt lịch)
 * PICKED_UP: Admin xác nhận đã lấy sách → Bắt đầu tính ngày mượn
 * RETURNED: Đã trả sách
 * CANCELLED: Hủy đặt lịch (user hoặc admin)
 */
public enum LoanStatus {
    PENDING,      // Chờ nhận sách tại thư viện
    PICKED_UP,    // Admin xác nhận đã lấy sách
    RETURNED,     // Đã trả sách
    CANCELLED     // Hủy đặt lịch
}
