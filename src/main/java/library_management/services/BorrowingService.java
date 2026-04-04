package library_management.services;

import library_management.models.Book;
import library_management.models.Loan;
import library_management.models.User;
import library_management.exceptions.*;
import library_management.repository.BookRepository;
import library_management.repository.LoanRepository;
import library_management.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service xử lý logic nghiệp vụ mượn sách
 * 
 * Quy tắc mượn sách:
 * 1. Thẻ độc giả phải tồn tại, ACTIVE và chưa hết hạn
 * 2. Người dùng chỉ được mượn tối đa 3 cuốn
 * 3. Cuốn sách phải có trạng thái AVAILABLE
 * 4. Mỗi sách chỉ có 1 bản được cập nhật khi mượn
 */
@Service
public class BorrowingService {
    
    private static final Logger logger = LoggerFactory.getLogger(BorrowingService.class);
    
    // Hằng số
    private static final int MAX_BORROW_LIMIT = 3;                    // Tối đa 3 cuốn/người
    private static final int BORROW_DURATION_DAYS = 14;               // Hạn trả: 14 ngày
    
    @Autowired
    private LoanRepository loanRepository;
    
    @Autowired
    private BookRepository bookRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Thực hiện mượn sách cho một người dùng (dùng Mã Thẻ Độc Giả)
     * 
     * @param bookId ID của cuốn sách cần mượn
     * @param idCard Mã thẻ độc giả (ví dụ: LIB-2026-2280601489)
     * @return Đối tượng Loan vừa tạo
     * @throws BorrowingException nếu không thỏa mãn điều kiện mượn
     */
    @Transactional
    public Loan borrowBook(String bookId, String idCard) {
        // Tìm user bằng mã thẻ độc giả
        User user = userRepository.findByIdCard(idCard)
            .orElseThrow(() -> new BorrowingException(
                "Không tìm thấy thẻ độc giả với mã: " + idCard, 
                "CARD_NOT_FOUND"
            ));
        
        return borrowBookByUserId(bookId, user.getId(), LocalDate.now().plusDays(BORROW_DURATION_DAYS), 1);
    }
    
    /**
     * Thực hiện mượn sách với thông tin chi tiết
     * 
     * @param bookId ID của cuốn sách
     * @param idCard Mã thẻ độc giả người mượn
     * @param dueDate Ngày hạn trả
     * @param quantity Số lượng mượn
     * @return Đối tượng Loan vừa tạo
     */
    @Transactional
    public Loan borrowBookWithDetails(String bookId, String idCard, LocalDate dueDate, int quantity) {
        // Tìm user bằng mã thẻ độc giả
        User user = userRepository.findByIdCard(idCard)
            .orElseThrow(() -> new BorrowingException(
                "Không tìm thấy thẻ độc giả với mã: " + idCard, 
                "CARD_NOT_FOUND"
            ));
        
        return borrowBookByUserId(bookId, user.getId(), dueDate, quantity);
    }
    
    /**
     * Thực hiện mượn sách cho một người dùng (sử dụng userId)
     * 
     * @param bookId ID của cuốn sách cần mượn
     * @param userId ID của người dùng mượn sách
     * @param dueDate Ngày hạn trả
     * @param quantity Số lượng
     * @return Đối tượng Loan vừa tạo
     * @throws BorrowingException nếu không thỏa mãn điều kiện mượn
     */
    @Transactional
    public Loan borrowBookByUserId(String bookId, String userId, LocalDate dueDate, int quantity) {
        
        logger.info("📚 Bắt đầu process mượn sách: bookId={}, userId={}, dueDate={}, quantity={}", 
            bookId, userId, dueDate, quantity);
        
        try {
            // ===== BƯỚC 1: Lấy thông tin sách =====
            logger.info("🔍 Step 1: Lấy thông tin sách (ID: {})", bookId);
            Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BorrowingException("Không tìm thấy cuốn sách", "BOOK_NOT_FOUND"));
            logger.info("✅ Tìm được sách: {}", book.getTitle());
            
            // ===== BƯỚC 2: Kiểm tra trạng thái sách =====
            logger.info("🔍 Step 2: Kiểm tra trạng thái sách (status: {})", book.getStatus());
            if (!book.getStatus().equals("AVAILABLE")) {
                throw new BookNotAvailableException(book.getTitle());
            }
            logger.info("✅ Sách có sẵn");
            
            // ===== BƯỚC 3: Lấy thông tin người dùng =====
            logger.info("🔍 Step 3: Lấy thông tin người dùng (ID: {})", userId);
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new BorrowingException("Không tìm thấy người dùng", "USER_NOT_FOUND"));
            logger.info("✅ Tìm được user: {}", user.getFullName());
            
            // ===== BƯỚC 4: Kiểm tra thẻ độc giả =====
            logger.info("🔍 Step 4: Validate thẻ độc giả (status: {})", user.getStatus());
            validateCard(user);
            logger.info("✅ Thẻ độc giả hợp lệ");
            
            // ===== BƯỚC 5: Kiểm tra số lượng sách đang mượn =====
            logger.info("🔍 Step 5: Kiểm tra số lượng sách đang mượn");
            validateBorrowLimit(userId);
            logger.info("✅ Chưa vượt quá giới hạn mượn");
            
            // ===== BƯỚC 6: Tạo bản ghi mượn (Loan) =====
            logger.info("🔍 Step 6: Tạo bản ghi mượn (Loan)");
            Loan loan = Loan.builder()
                .bookId(bookId)
                .userId(userId)
                .borrowDate(LocalDate.now())
                .dueDate(dueDate)
                .status("ACTIVE")
                .lateFee(0)
                .createdAt(LocalDateTime.now())
                .notes("Mượn sách từ thư viện - Số lượng: " + quantity)
                .build();
            
            logger.info("💾 Lưu Loan vào database...");
            Loan savedLoan = loanRepository.save(loan);
            logger.info("✅ Loan saved with ID: {}", savedLoan.getId());
            
            // ===== BƯỚC 7: Cập nhật trạng thái sách =====
            logger.info("🔍 Step 7: Cập nhật Book.status thành BORROWED");
            book.setStatus("BORROWED");
            bookRepository.save(book);
            logger.info("✅ Book updated to BORROWED");
            
            logger.info("🎉 Mượn sách thành công!");
            return savedLoan;
            
        } catch (BorrowingException e) {
            logger.error("❌ BorrowingException: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("❌ Lỗi không mong muốn:", e);
            throw e;
        }
    }
    
    /**
     * Kiểm tra tính hợp lệ của thẻ độc giả
     * Thẻ phải:
     * - Tồn tại
     * - Có trạng thái ACTIVE (hoạt động)
     * - Chưa hết hạn (expiryDate >= hôm nay)
     * 
     * @param user Thông tin người dùng
     * @throws CardNotFoundException nếu không có thẻ
     * @throws CardInactiveException nếu thẻ không hoạt động
     * @throws CardExpiredException nếu thẻ đã hết hạn
     */
    private void validateCard(User user) {
        // Kiểm tra thẻ có tồn tại không
        if (user.getStatus() == null || !user.getStatus().equals("ACTIVE")) {
            if (user.getStatus() == null) {
                throw new CardNotFoundException(user.getId());
            }
            throw new CardInactiveException();
        }
        
        // Kiểm tra thẻ có hết hạn chưa
        if (user.getExpiryDate() != null && user.getExpiryDate().isBefore(LocalDate.now())) {
            throw new CardExpiredException(user.getExpiryDate().toString());
        }
    }
    
    /**
     * Kiểm tra người dùng có vượt quá giới hạn mượn sách không
     * Quy tắc: Mỗi người dùng chỉ được mượn tối đa 3 cuốn
     * 
     * @param userId ID của người dùng
     * @throws BorrowLimitExceededException nếu đã mượn ≥ 3 cuốn
     */
    private void validateBorrowLimit(String userId) {
        // Đếm số sách hiện tại đang mượn (status = "ACTIVE")
        long activeLoanCount = loanRepository.countByUserIdAndStatus(userId, "ACTIVE");
        
        // Nếu đã mượn ≥ 3 cuốn, không cho mượn thêm
        if (activeLoanCount >= MAX_BORROW_LIMIT) {
            throw new BorrowLimitExceededException((int) activeLoanCount);
        }
    }
    
    /**
     * Trả sách (return book)
     * Cập nhật bản ghi mượn: status = "RETURNED", returnDate = hôm nay
     * Cập nhật trạng thái sách: status = "AVAILABLE"
     * 
     * @param loanId ID của bản ghi mượn
     * @param bookId ID của cuốn sách
     * @return Bản ghi mượn đã cập nhật
     */
    @Transactional
    public Loan returnBook(String loanId, String bookId) {
        // Lấy bản ghi mượn
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new BorrowingException("Không tìm thấy bản ghi mượn", "LOAN_NOT_FOUND"));
        
        // Cập nhật ngày trả và trạng thái
        loan.setReturnDate(LocalDate.now());
        loan.setStatus("RETURNED");
        Loan updatedLoan = loanRepository.save(loan);
        
        // Cập nhật trạng thái sách về AVAILABLE
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new BorrowingException("Không tìm thấy sách", "BOOK_NOT_FOUND"));
        book.setStatus("AVAILABLE");
        bookRepository.save(book);
        
        return updatedLoan;
    }
    
    /**
     * Lấy thông tin bản ghi mượn sách
     * @param loanId ID của bản ghi mượn
     * @return Optional chứa Loan nếu tìm thấy
     */
    public Optional<Loan> getLoanById(String loanId) {
        return loanRepository.findById(loanId);
    }
}
