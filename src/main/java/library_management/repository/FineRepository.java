package library_management.repository;

import library_management.models.Fine;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Repository để truy vấn dữ liệu phiếu phạt độc giả
 */
public interface FineRepository extends MongoRepository<Fine, String> {
    
    /**
     * Lấy tất cả phiếu phạt của một người dùng
     * @param userId ID của người dùng
     * @return Danh sách phiếu phạt
     */
    List<Fine> findByUserId(String userId);
    
    /**
     * Lấy phiếu phạt chưa thanh toán của một người dùng
     * @param userId ID của người dùng
     * @param status Trạng thái ("PENDING")
     * @return Danh sách phiếu phạt chưa thanh toán
     */
    List<Fine> findByUserIdAndStatus(String userId, String status);
    
    /**
     * Tìm phiếu phạt theo ID phiếu mượn
     * @param loanId ID của phiếu mượn
     * @return Danh sách phiếu phạt liên quan
     */
    List<Fine> findByLoanId(String loanId);
    
    /**
     * Đếm số phiếu phạt chưa thanh toán của một người dùng
     * @param userId ID của người dùng
     * @param status Trạng thái
     * @return Số lượng phiếu phạt
     */
    long countByUserIdAndStatus(String userId, String status);
}
