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
    
    // Ngày mượn
    private LocalDate borrowDate;
    
    // Ngày hẹn trả (theo quy định 14 ngày)
    private LocalDate dueDate;
    
    // Ngày thực tế trả lại (null nếu chưa trả)
    private LocalDate returnDate;
    
    // Trạng thái: ACTIVE (chưa trả), RETURNED (đã trả)
    private String status; // "ACTIVE" hoặc "RETURNED"
    
    // Số tiền phạt (nếu quá hạn)
    private int lateFee;
    
    // Số lượng sách mượn
    private int quantity;
    
    // Thời gian tạo bản ghi
    private LocalDateTime createdAt;
    
    // Ghi chú thêm
    private String notes;
}
