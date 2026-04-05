package library_management.repository;

import library_management.models.Loan;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

/**
 * Repository để truy vấn dữ liệu về các bản ghi mượn sách
 */
public interface LoanRepository extends MongoRepository<Loan, String> {
    
    /**
     * Lấy tất cả các bản ghi mượn sách của một người dùng
     * @param userId ID của người dùng
     * @return Danh sách các bản ghi mượn
     */
    List<Loan> findByUserId(String userId);
    
    /**
     * Đếm số cuốn sách hiện tại mà người dùng đang mượn (chưa trả)
     * @param userId ID của người dùng
     * @param status Trạng thái (ACTIVE hoặc RETURNED)
     * @return Số lượng sách đang mượn
     */
    long countByUserIdAndStatus(String userId, String status);
    
    /**
     * Lấy tất cả các bản ghi mượn chưa trả của một người dùng
     * @param userId ID của người dùng
     * @return Danh sách các bản ghi mượn chưa trả
     */
    List<Loan> findByUserIdAndStatus(String userId, String status);
    
    /**
     * Tìm bản ghi mượn của một cuốn sách cụ thể
     * @param bookId ID của sách
     * @param status Trạng thái mượn
     * @return Danh sách các bản ghi
     */
    List<Loan> findByBookIdAndStatus(String bookId, String status);
    
    /**
     * Kiểm tra xem sách đã được mượn bởi người dùng nào chưa
     * @param bookId ID của sách
     * @return true nếu sách đang được mượn, false nếu chưa
     */
    boolean existsByBookIdAndStatus(String bookId, String status);
}
