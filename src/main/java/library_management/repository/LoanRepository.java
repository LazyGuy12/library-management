package library_management.repository;

import library_management.models.Loan;
import library_management.models.LoanStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

/**
 * Repository để truy vấn dữ liệu về các bản ghi mượn sách
 */
public interface LoanRepository extends MongoRepository<Loan, String> {
    
    /**
     * Lấy tất cả các bản ghi mượn sách của một người dùng
     */
    List<Loan> findByUserId(String userId);
    
    /**
     * Đếm số bản ghi mượn theo status (PENDING/PICKED_UP/RETURNED/CANCELLED)
     */
    long countByUserIdAndStatus(String userId, String status);
    
    long countByUserIdAndStatus(String userId, LoanStatus status);
    
    /**
     * Lấy tất cả các bản ghi mượn theo status
     */
    List<Loan> findByUserIdAndStatus(String userId, String status);
    
    List<Loan> findByUserIdAndStatus(String userId, LoanStatus status);
    
    /**
     * Tìm bản ghi mượn của một cuốn sách cụ thể
     */
    List<Loan> findByBookIdAndStatus(String bookId, String status);
    
    List<Loan> findByBookIdAndStatus(String bookId, LoanStatus status);
    
    /**
     * Kiểm tra xem sách đã được mượn chưa
     */
    boolean existsByBookIdAndStatus(String bookId, String status);
    
    boolean existsByBookIdAndStatus(String bookId, LoanStatus status);
}
