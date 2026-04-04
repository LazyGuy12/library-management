package library_management.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO (Data Transfer Object) để trả về thông tin bản ghi mượn sách
 * Dùng để trả về thông tin mượn sách cho API hoặc view
 */
@Data
@Builder
public class LoanResponseDTO {
    
    private String loanId;                                  // ID của bản ghi mượn
    private String bookTitle;                               // Tên sách mượn
    private String userName;                                // Tên người mượn
    
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate borrowDate;                           // Ngày mượn
    
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate dueDate;                              // Ngày hạn trả
    
    private String status;                                  // Trạng thái (ACTIVE/RETURNED)
    private int lateFee;                                    // Tiền phạt (nếu có)
    private String message;                                 // Thông báo cho người dùng
    
    /**
     * Factory method để tạo DTO từ Loan và thông tin bổ sung
     */
    public static LoanResponseDTO of(String loanId, String bookTitle, String userName,
                                      LocalDate borrowDate, LocalDate dueDate,
                                      String status, int lateFee, String message) {
        return LoanResponseDTO.builder()
            .loanId(loanId)
            .bookTitle(bookTitle)
            .userName(userName)
            .borrowDate(borrowDate)
            .dueDate(dueDate)
            .status(status)
            .lateFee(lateFee)
            .message(message)
            .build();
    }
}
