package library_management.controllers;

import library_management.services.TestDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller cho test dữ liệu
 * 
 * Endpoint này giúp test các chức năng mà không cần chờ ngày thực tế:
 * - Thẻ độc giả hết hạn / sắp hết hạn
 * - Loan quá hạn → Tính phí phạt + Khóa thẻ
 * 
 * ⚠️ Chỉ hoạt động khi app.test-endpoints.enabled=true (dev profile)
 * Tắt hoàn toàn ở production
 */
@RestController
@RequestMapping("/api/test")
@ConditionalOnProperty(name = "app.test-endpoints.enabled", havingValue = "true")
public class TestDataController {

    private static final Logger logger = LoggerFactory.getLogger(TestDataController.class);

    @Autowired
    private TestDataService testDataService;

    /**
     * Tạo user với thẻ độc giả hết hạn hoặc sắp hết hạn
     * 
     * @param days Số ngày từ hôm nay (âm = quá khứ, dương = tương lai)
     * @return Thông tin user vừa tạo
     */
    @PostMapping("/user-expire")
    public ResponseEntity<?> createUserWithExpiredCard(
            @RequestParam(defaultValue = "0") int days) {
        
        try {
            Map<String, Object> result = testDataService.createUserWithExpiredCard(days);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Lỗi tạo user: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                Map.of("success", false, "error", e.getMessage())
            );
        }
    }

    /**
     * Tạo loan quá hạn (để test tính phí phạt và khóa thẻ)
     * 
     * @param mssv Mã số sinh viên
     * @param bookId ID của sách
     * @param daysOverdue Số ngày quá hạn
     * @return Thông tin loan vừa tạo
     */
    @PostMapping("/overdue-loan")
    public ResponseEntity<?> createOverdueLoan(
            @RequestParam String mssv,
            @RequestParam String bookId,
            @RequestParam(defaultValue = "5") int daysOverdue) {
        
        try {
            Map<String, Object> result = testDataService.createOverdueLoan(mssv, bookId, daysOverdue);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Lỗi tạo loan: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                Map.of("success", false, "error", e.getMessage())
            );
        }
    }

    /**
     * Trả sách quá hạn → tự động tính phí + khóa thẻ
     * 
     * @param loanId ID của loan cần trả
     * @return Kết quả xử lý
     */
    @PostMapping("/return-overdue/{loanId}")
    public ResponseEntity<?> returnOverdueLoan(@PathVariable String loanId) {
        
        try {
            Map<String, Object> result = testDataService.returnOverdueLoan(loanId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Lỗi trả sách: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                Map.of("success", false, "error", e.getMessage())
            );
        }
    }

    /**
     * Lấy danh sách test endpoints có sẵn
     */
    @GetMapping("/info")
    public ResponseEntity<?> getTestInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("status", "✅ Test endpoints ENABLED");
        info.put("note", "⚠️ Chỉ hoạt động khi app.test-endpoints.enabled=true");
        return ResponseEntity.ok(info);
    }
}
