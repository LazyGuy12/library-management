package library_management.controllers;

import library_management.dto.LoanResponseDTO;
import library_management.models.Book;
import library_management.models.Loan;
import library_management.exceptions.BorrowingException;
import library_management.repository.BookRepository;
import library_management.repository.UserRepository;
import library_management.services.BorrowingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * REST API Controller xử lý các yêu cầu mượn sách
 * Endpoints:
 * - POST /api/borrow/{bookId} : Mượn sách
 * - POST /api/return/{loanId} : Trả sách
 */
@RestController
@RequestMapping("/api")
public class BorrowingAPIController {
    
    @Autowired
    private BorrowingService borrowingService;
    
    @Autowired
    private BookRepository bookRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * API để mượn sách
     * 
     * @param bookId ID của cuốn sách
     * @param principal Thông tin người dùng đăng nhập
     * @return ResponseEntity chứa thông tin mượn sách hoặc thông báo lỗi
     */
    @PostMapping("/borrow/{bookId}")
    public ResponseEntity<?> borrowBook(@PathVariable String bookId, Principal principal) {
        try {
            // Kiểm tra xem user có đăng nhập không
            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "Bạn cần đăng nhập để mượn sách"));
            }
            
            // Gọi service mượn sách
            Loan loan = borrowingService.borrowBook(bookId, principal.getName());
            
            // Lấy thông tin sách và người dùng
            Book book = bookRepository.findById(bookId).orElse(null);
            String userName = userRepository.findByMssv(principal.getName())
                .map(user -> user.getFullName())
                .orElse(principal.getName());
            
            // Tạo response DTO
            LoanResponseDTO response = LoanResponseDTO.of(
                loan.getId(),
                book != null ? book.getTitle() : "N/A",
                userName,
                loan.getBorrowDate(),
                loan.getDueDate(),
                loan.getStatus(),
                loan.getLateFee(),
                "✅ Mượn sách thành công! Hạn trả: " + loan.getDueDate()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (BorrowingException e) {
            // Lỗi từ logic mượn sách
            return ResponseEntity.badRequest()
                .body(new ErrorResponse(e.getErrorCode(), e.getMessage()));
                
        } catch (Exception e) {
            // Lỗi không mong muốn
            return ResponseEntity.internalServerError()
                .body(new ErrorResponse("INTERNAL_ERROR", "Có lỗi xảy ra: " + e.getMessage()));
        }
    }
    
    /**
     * API để trả sách
     * 
     * @param loanId ID của bản ghi mượn
     * @param bookId ID của cuốn sách
     * @return ResponseEntity chứa thông tin trả sách
     */
    @PostMapping("/return/{loanId}")
    public ResponseEntity<?> returnBook(@PathVariable String loanId, 
                                       @RequestParam String bookId) {
        try {
            Loan loan = borrowingService.returnBook(loanId, bookId);
            
            Book book = bookRepository.findById(bookId).orElse(null);
            
            LoanResponseDTO response = LoanResponseDTO.of(
                loan.getId(),
                book != null ? book.getTitle() : "N/A",
                "N/A",
                loan.getBorrowDate(),
                loan.getDueDate(),
                loan.getStatus(),
                loan.getLateFee(),
                "✅ Trả sách thành công!"
            );
            
            return ResponseEntity.ok(response);
            
        } catch (BorrowingException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse(e.getErrorCode(), e.getMessage()));
                
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(new ErrorResponse("INTERNAL_ERROR", "Có lỗi xảy ra: " + e.getMessage()));
        }
    }
    
    /**
     * Class để trả về lỗi
     */
    @SuppressWarnings("unused")
    private static class ErrorResponse {
        public String errorCode;
        public String message;
        
        public ErrorResponse(String errorCode, String message) {
            this.errorCode = errorCode;
            this.message = message;
        }
        
        public String getErrorCode() {
            return errorCode;
        }
        
        public String getMessage() {
            return message;
        }
    }
}
