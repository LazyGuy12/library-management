package library_management.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO để display lịch sử mượn sách trên view
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanHistoryDTO {
    
    private String loanId;              // ID bản ghi mượn
    private String bookTitle;           // Tên sách
    private String bookAuthor;          // Tác giả sách
    private String bookCode;            // Mã ISBN sách
    
    private String readerCardId;        // Mã thẻ độc giả
    private String readerName;          // Tên người mượn
    private String readerMssv;          // MSSV người mượn
    private LocalDate readerExpiryDate; // Hạn dùng thẻ
    private String readerStatus;        // Trạng thái thẻ (ACTIVE/INACTIVE)
    
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate borrowDate;       // Ngày mượn
    
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate dueDate;          // Hạn trả
    
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate returnDate;       // Ngày thực tế trả (null nếu chưa trả)
    
    private String status;              // ACTIVE hoặc RETURNED
    private int lateFee;                // Tiền phạt nếu quá hạn
    private int quantity;               // Số lượng sách mượn
}
