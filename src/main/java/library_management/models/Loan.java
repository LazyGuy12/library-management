package library_management.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Model đại diện cho một bản ghi mượn sách (Loan Record)
 * Lưu trữ thông tin: ai mượn cuốn sách nào, từ khi nào, đến khi nào
 */
@Document(collection = "loans")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Loan {
    
    @Id
    private String id;
    
    // ID của cuốn sách được mượn
    private String bookId;
    
    // ID của người dùng mượn sách
    private String userId;
    
    // Thời gian user chọn để lên thư viện nhận sách (appointment)
    // Lúc này sách -1 quantity, tính từ đây dueDate = appointmentTime + 14 ngày
    private LocalDateTime appointmentTime;
    
    // Ngày mượn (= appointmentTime.toLocalDate() khi admin click "Đã lấy")
    private LocalDate borrowDate;
    
    // Ngày hẹn trả (= appointmentTime + 14 ngày)
    private LocalDate dueDate;
    
    // Ngày thực tế trả lại (null nếu chưa trả)
    private LocalDate returnDate;
    
    // Trạng thái: PENDING (chờ nhận), PICKED_UP (đã lấy), RETURNED (đã trả), CANCELLED (hủy)
    private LoanStatus status;
    
    // Lý do hủy đặt lịch (nếu status = CANCELLED)
    private String cancelReason;
    
    // Số tiền phạt (nếu quá hạn), tính từ dueDate
    private long lateFee;
    
    // Số lượng sách mượn
    private int quantity;
    
    // Thời gian tạo bản ghi
    private LocalDateTime createdAt;
    
    // Ghi chú thêm
    private String notes;
}
