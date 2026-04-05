package library_management.services;

import library_management.models.Book;
import library_management.models.Loan;
import library_management.models.User;
import library_management.repository.BookRepository;
import library_management.repository.LoanRepository;
import library_management.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service xử lý tạo dữ liệu test cho các chức năng:
 * - Thẻ độc giả hết hạn / sắp hết hạn
 * - Loan quá hạn
 * - Tính phí phạt
 * 
 * ⚠️ Chỉ hoạt động khi app.test-endpoints.enabled=true
 */
@Service
@ConditionalOnProperty(name = "app.test-endpoints.enabled", havingValue = "true")
public class TestDataService {

    private static final Logger logger = LoggerFactory.getLogger(TestDataService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private BorrowingService borrowingService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Tạo user với thẻ độc giả hết hạn hoặc sắp hết hạn
     * 
     * @param days Số ngày từ hôm nay (âm = quá khứ, dương = tương lai)
     * @return Map chứa thông tin user vừa tạo
     * @throws Exception nếu có lỗi
     */
    @Transactional
    public Map<String, Object> createUserWithExpiredCard(int days) throws Exception {
        
        logger.info("🔧 TEST: Tạo user với thẻ hết hạn trong {} ngày", days);
        
        // Tạo MSSV unique
        String mssv = "TEST" + System.currentTimeMillis();
        String email = "test-" + mssv + "@example.com";
        
        User user = new User();
        user.setMssv(mssv);
        user.setPassword(passwordEncoder.encode("password123"));
        user.setFullName("Test User Expire - " + days + " days");
        user.setEmail(email);
        user.setRole("USER");
        
        // Đặt thẻ hết hạn vào ngày cụ thể
        LocalDate expiryDate = LocalDate.now().plusDays(days);
        user.setExpiryDate(expiryDate);
        user.setIdCard("LIB-2026-" + mssv);
        
        // Kiểm tra trạng thái thẻ dựa trên số ngày
        String status;
        if (days <= 0) {
            status = "EXPIRED";
        } else if (days <= 30) {
            status = "ACTIVE"; // Sắp hết hạn nhưng vẫn ACTIVE
        } else {
            status = "ACTIVE";
        }
        user.setStatus(status);
        user.setLastRenewedDate(LocalDate.now().minusDays(365));
        user.setRenewalCount(0);
        
        User savedUser = userRepository.save(user);
        
        logger.info("✅ User created: {} | Status: {} | Expiry: {}", 
            savedUser.getMssv(), status, expiryDate);
        
        // Trả về response map
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "✅ User tạo thành công! Thẻ " + status);
        response.put("userId", savedUser.getId());
        response.put("mssv", savedUser.getMssv());
        response.put("idCard", savedUser.getIdCard());
        response.put("expiryDate", savedUser.getExpiryDate());
        response.put("status", savedUser.getStatus());
        response.put("note", "Trạng thái: " + (days <= 0 ? "HẾT HẠN" : "SẮP HẾT HẠN"));
        
        return response;
    }

    /**
     * Tạo loan quá hạn để test tính phí phạt
     * 
     * @param mssv Mã số sinh viên
     * @param bookId ID của sách
     * @param daysOverdue Số ngày quá hạn
     * @return Map chứa thông tin loan vừa tạo
     * @throws Exception nếu không tìm thấy user hoặc book
     */
    @Transactional
    public Map<String, Object> createOverdueLoan(String mssv, String bookId, int daysOverdue) 
            throws Exception {
        
        logger.info("🔧 TEST: Tạo loan quá hạn {} ngày cho user {}", daysOverdue, mssv);
        
        // Tìm user
        Optional<User> userOpt = userRepository.findByMssv(mssv);
        if (!userOpt.isPresent()) {
            String error = "❌ Không tìm thấy user với MSSV: " + mssv;
            logger.error(error);
            throw new IllegalArgumentException(error);
        }
        User user = userOpt.get();
        
        // Tìm book
        Optional<Book> bookOpt = bookRepository.findById(bookId);
        if (!bookOpt.isPresent()) {
            String error = "❌ Không tìm thấy book với ID: " + bookId;
            logger.error(error);
            throw new IllegalArgumentException(error);
        }
        Book book = bookOpt.get();
        
        // Tạo loan với ngày quá hạn
        // Logic: Nếu quá hạn 5 ngày → mượn 19 ngày trước, hạn 5 ngày trước
        LocalDate borrowDate = LocalDate.now().minusDays(14 + daysOverdue);
        LocalDate dueDate = LocalDate.now().minusDays(daysOverdue);
        
        Loan loan = new Loan();
        loan.setUserId(user.getId());
        loan.setBookId(book.getId());
        loan.setBorrowDate(borrowDate);
        loan.setDueDate(dueDate);
        loan.setStatus("ACTIVE"); // Chưa trả
        loan.setQuantity(1);
        loan.setLateFee(0);
        loan.setCreatedAt(LocalDateTime.now());
        
        Loan savedLoan = loanRepository.save(loan);
        
        logger.info("✅ Loan created: {} | Borrow: {} | Due: {} | Overdue: {} days", 
            savedLoan.getId(), borrowDate, dueDate, daysOverdue);
        
        // Tính tiền phạt dự tính
        long estimatedFine = (long) daysOverdue * 5000;
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "✅ Loan quá hạn tạo thành công!");
        response.put("loanId", savedLoan.getId());
        response.put("userId", savedLoan.getUserId());
        response.put("bookId", savedLoan.getBookId());
        response.put("borrowDate", savedLoan.getBorrowDate());
        response.put("dueDate", savedLoan.getDueDate());
        response.put("daysOverdue", daysOverdue);
        response.put("estimatedFine", estimatedFine + " VND (5.000/ngày)");
        response.put("nextStep", "POST /api/test/return-overdue/" + savedLoan.getId());
        
        return response;
    }

    /**
     * Trả sách quá hạn → tự động tính phí + khóa thẻ
     * 
     * @param loanId ID của loan cần trả
     * @return Map chứa kết quả xử lý
     * @throws Exception nếu không tìm thấy loan
     */
    @Transactional
    public Map<String, Object> returnOverdueLoan(String loanId) throws Exception {
        
        logger.info("🔧 TEST: Trả sách quá hạn - Loan ID: {}", loanId);
        
        Optional<Loan> loanOpt = loanRepository.findById(loanId);
        if (!loanOpt.isPresent()) {
            String error = "❌ Không tìm thấy loan: " + loanId;
            logger.error(error);
            throw new IllegalArgumentException(error);
        }
        
        Loan loan = loanOpt.get();
        
        // Gọi borrowingService.returnBook() - logic xử lý trả sách
        borrowingService.returnBook(loanId, loan.getBookId());
        
        // Lấy thông tin loan sau khi trả
        Loan updatedLoan = loanRepository.findById(loanId).get();
        User user = userRepository.findById(updatedLoan.getUserId()).get();
        
        logger.info("✅ Loan returned: {} | Late Fee: {} | User Status: {}", 
            loanId, updatedLoan.getLateFee(), user.getStatus());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "✅ Trả sách thành công!");
        response.put("loanId", updatedLoan.getId());
        response.put("status", updatedLoan.getStatus());
        response.put("returnDate", updatedLoan.getReturnDate());
        response.put("lateFee", updatedLoan.getLateFee() + " VND");
        response.put("userCardStatus", user.getStatus());
        
        if (updatedLoan.getLateFee() > 0) {
            response.put("note", "⚠️ Thẻ độc giả đã bị KHÓA do vi phạm. Admin phải xác nhận thanh toán phí.");
            response.put("nextStep", "Truy cập /admin/cards để xác nhận thanh toán");
        }
        
        return response;
    }
}
