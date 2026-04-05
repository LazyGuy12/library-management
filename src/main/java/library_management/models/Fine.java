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
 * Model đại diện cho phiếu phạt độc giả
 * Lưu trữ thông tin phạt quá hạn, phạt vi phạm, v.v.
 */
@Document(collection = "fines")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Fine {
    
    @Id
    private String id;
    
    // ID của người dùng bị phạt
    private String userId;
    
    // ID của phiếu mượn liên quan (nếu có)
    private String loanId;
    
    // Loại phạt: "LATE_FEE" (quá hạn), "DAMAGE" (hỏng sách), "LOST" (mất sách)
    private String fineType;
    
    // Số tiền phạt
    private long amount;
    
    // Lý do phạt chi tiết
    private String reason;
    
    // Ngày tạo phiếu phạt
    private LocalDate createdDate;
    
    // Trạng thái: "PENDING" (chưa thanh toán), "PAID" (đã thanh toán)
    private String status; // "PENDING" hoặc "PAID"
    
    // Ngày thanh toán (null nếu chưa thanh toán)
    private LocalDate paidDate;
    
    // Người xác nhận thanh toán (admin)
    private String confirmedBy;
    
    // Thời gian xác nhận thanh toán
    private LocalDateTime confirmedAt;
    
    // Ghi chú
    private String notes;
}
