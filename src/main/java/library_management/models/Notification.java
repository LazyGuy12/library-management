package library_management.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Model đại diện cho một thông báo cho user
 * Lưu trữ các sự kiện: hủy mượn, sách quá hạn, thẻ hết hạn, etc
 */
@Document(collection = "notifications")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Notification {
    
    @Id
    private String id;
    
    // ID của user nhận thông báo
    private String userId;
    
    // Loại thông báo: CANCEL_BORROW, OVERDUE_BOOK, EXPIRED_CARD, BOOK_RETURNED, etc
    private String type;
    
    // Tiêu đề thông báo
    private String title;
    
    // Nội dung chi tiết
    private String message;
    
    // ID của Loan (nếu liên quan)
    private String loanId;
    
    // ID của Book (nếu liên quan)
    private String bookId;
    
    // Lý do (cho các notification có lý do, ví dụ: cancel reason)
    private String reason;
    
    // Đã đọc hay chưa
    private boolean isRead;
    
    // Thời gian tạo notification
    private LocalDateTime createdAt;
    
    // Link để xem chi tiết (nếu có)
    private String actionUrl;
}
