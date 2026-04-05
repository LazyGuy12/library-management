package library_management.services;

import library_management.models.*;
import library_management.exceptions.*;
import library_management.repository.BookRepository;
import library_management.repository.FineRepository;
import library_management.repository.LoanRepository;
import library_management.repository.NotificationRepository;
import library_management.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service xử lý logic nghiệp vụ mượn sách - APPOINTMENT BASED FLOW
 * 
 * Flow mới:
 * 1. User đặt lịch mượn sách (appointmentTime) → PENDING, -1 quantity
 * 2. Admin xác nhận đã lấy sách → PICKED_UP, borrowDate = hôm nay, dueDate = appointmentTime + 14 ngày
 * 3. User trả sách → RETURNED, tính phí từ dueDate nếu quá hạn
 * 4. Admin có thể cancel PENDING → +1 quantity, nhập lý do
 * 
 * Quy tắc:
 * - Thẻ phải ACTIVE, chưa hết hạn để đặt lịch
 * - Tối đa 3 cuốn đang PENDING + PICKED_UP (chưa trả/hủy)
 * - Phí phạt: 5.000₫/cuốn/ngày, tính từ dueDate
 */
@Service
public class BorrowingService {
    
    private static final Logger logger = LoggerFactory.getLogger(BorrowingService.class);
    
    private static final int MAX_BORROW_LIMIT = 3;
    private static final int BORROW_DURATION_DAYS = 14;
    private static final long LATE_FEE_PER_DAY = 5000; // 5.000₫/cuốn/ngày
    
    @Autowired
    private LoanRepository loanRepository;
    
    @Autowired
    private BookRepository bookRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private FineRepository fineRepository;
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    // ===================== NEW FLOW =====================
    
    /**
     * USER REQUEST BORROW - Đặt lịch mượn sách
     * 
     * Bước:
     * 1. Validate thẻ độc giả (ACTIVE, không hết hạn)
     * 2. Validate giới hạn mượn (PENDING + PICKED_UP ≤ 3)
     * 3. Validate sách có trong kho
     * 4. Tạo Loan với status = PENDING
     * 5. -1 quantity sách
     * 6. Tính dueDate = appointmentTime + số ngày tùy chỉnh (tối đa 14)
     * 
     * @param userId User ID
     * @param bookId Book ID
     * @param appointmentTime Lúc user chọn đến nhận sách
     * @param dueDate Ngày hạn trả (LocalDate, tối đa 14 ngày từ appointmentTime)
     * @return Loan vừa tạo (PENDING)
     */
    @Transactional
    public Loan requestBorrow(String userId, String bookId, LocalDateTime appointmentTime, LocalDate dueDate) {
        logger.info("📅 User {} đặt lịch mượn sách {} vào {}, hạn trả {}", userId, bookId, appointmentTime, dueDate);
        
        // Validate user & card
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BorrowingException("Không tìm thấy người dùng", "USER_NOT_FOUND"));
        validateCard(user);
        
        // Validate borrow limit (PENDING + PICKED_UP ≤ 3)
        validateBorrowLimit(userId);
        
        // Validate book exists & available
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new BorrowingException("Không tìm thấy sách", "BOOK_NOT_FOUND"));
        
        if (book.getQuantity() <= 0) {
            throw new BookNotAvailableException("Sách '" + book.getTitle() + "' không còn");
        }
        
        // Validate dueDate
        LocalDate appointmentDate = appointmentTime.toLocalDate();
        if (dueDate == null) {
            dueDate = appointmentDate.plusDays(BORROW_DURATION_DAYS);
        }
        if (dueDate.isBefore(appointmentDate)) {
            throw new BorrowingException("Ngày hạn trả phải sau ngày nhận sách", "INVALID_DATE");
        }
        if (dueDate.isAfter(appointmentDate.plusDays(BORROW_DURATION_DAYS))) {
            throw new BorrowingException("Ngày hạn trả tối đa là " + BORROW_DURATION_DAYS + " ngày", "EXCEEDS_MAX_DAYS");
        }
        
        // Create Loan with PENDING status
        Loan loan = Loan.builder()
            .userId(userId)
            .bookId(bookId)
            .appointmentTime(appointmentTime)
            .dueDate(dueDate)
            .status(LoanStatus.PENDING)
            .quantity(1)
            .lateFee(0)
            .createdAt(LocalDateTime.now())
            .notes("Đặt lịch mượn sách - Hẹn lấy: " + appointmentTime)
            .build();
        
        Loan savedLoan = loanRepository.save(loan);
        
        // Decrease book quantity
        book.setQuantity(book.getQuantity() - 1);
        if (book.getQuantity() <= 0) {
            book.setStatus("BORROWED");
        }
        bookRepository.save(book);
        
        logger.info("✅ Loan PENDING created: {} | appointmentTime: {} | dueDate: {}", 
            savedLoan.getId(), appointmentTime, dueDate);
        
        return savedLoan;
    }
    
    /**
     * ADMIN PICKUP BOOK - Admin xác nhận đã lấy sách
     * 
     * Bước:
     * 1. Tìm Loan với status = PENDING
     * 2. Cập nhật: status = PICKED_UP, borrowDate = hôm nay
     * 3. dueDate vẫn giữ (đã tính từ appointmentTime)
     * 
     * @param loanId Loan ID
     * @return Loan cập nhật
     */
    @Transactional
    public Loan pickupBook(String loanId) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new BorrowingException("Không tìm thấy bản ghi mượn", "LOAN_NOT_FOUND"));
        
        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new BorrowingException("Chỉ có thể xác nhận PENDING loans", "INVALID_STATUS");
        }
        
        // Update status & set borrowDate = today
        loan.setStatus(LoanStatus.PICKED_UP);
        loan.setBorrowDate(LocalDate.now());
        Loan updatedLoan = loanRepository.save(loan);
        
        logger.info("✅ Loan PICKED_UP: {} | borrowDate: {} | dueDate: {}", 
            loanId, loan.getBorrowDate(), loan.getDueDate());
        
        return updatedLoan;
    }
    
    /**
     * ADMIN CANCEL BORROW - Hủy đặt lịch mượn
     * 
     * Bước:
     * 1. Tìm Loan (PENDING hoặc PICKED_UP)
     * 2. Cập nhật: status = CANCELLED, cancelReason = input
     * 3. +1 quantity sách
     * 
     * @param loanId Loan ID
     * @param cancelReason Lý do hủy
     * @return Loan cập nhật
     */
    @Transactional
    public Loan cancelBorrow(String loanId, String cancelReason) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new BorrowingException("Không tìm thấy bản ghi mượn", "LOAN_NOT_FOUND"));
        
        if (loan.getStatus() == LoanStatus.RETURNED || loan.getStatus() == LoanStatus.CANCELLED) {
            throw new BorrowingException("Không thể hủy loan đã trả hoặc đã hủy", "INVALID_STATUS");
        }
        
        // Update status & reason
        loan.setStatus(LoanStatus.CANCELLED);
        loan.setCancelReason(cancelReason);
        Loan updatedLoan = loanRepository.save(loan);
        
        // Increase book quantity
        Book book = bookRepository.findById(loan.getBookId())
            .orElseThrow(() -> new BorrowingException("Không tìm thấy sách", "BOOK_NOT_FOUND"));
        book.setQuantity(book.getQuantity() + loan.getQuantity());
        book.setStatus("AVAILABLE");
        bookRepository.save(book);
        
        // Tạo notification cho user
        try {
            Notification notification = Notification.builder()
                .userId(loan.getUserId())
                .type("CANCEL_BORROW")
                .title("Lệnh Mượn Sách Bị Hủy")
                .message("Lệnh mượn sách '" + book.getTitle() + "' đã bị hủy")
                .reason(cancelReason)
                .loanId(loanId)
                .bookId(loan.getBookId())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .actionUrl("/user/borrow-history")
                .build();
            notificationRepository.save(notification);
        } catch (Exception e) {
            logger.error("Lỗi khi tạo notification cho cancel borrow: {}", e.getMessage());
        }
        
        logger.warn("🚫 Loan CANCELLED: {} | Reason: {}", loanId, cancelReason);
        
        return updatedLoan;
    }
    
    /**
     * RETURN BOOK - Trả sách (tính phí nếu quá hạn)
     * 
     * Bước:
     * 1. Tìm Loan (PICKED_UP)
     * 2. Tính phí: nếu returnDate > dueDate
     * 3. Cập nhật status = RETURNED, returnDate, lateFee
     * 4. Nếu có phí → Tạo Fine (PENDING), khóa thẻ (SUSPENDED)
     * 5. +1 quantity sách
     * 
     * @param loanId Loan ID
     * @return Loan cập nhật
     */
    @Transactional
    public Loan returnBook(String loanId) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new BorrowingException("Không tìm thấy bản ghi mượn", "LOAN_NOT_FOUND"));
        
        if (loan.getStatus() != LoanStatus.PICKED_UP) {
            throw new BorrowingException("Chỉ có thể trả PICKED_UP loans", "INVALID_STATUS");
        }
        
        User user = userRepository.findById(loan.getUserId())
            .orElseThrow(() -> new BorrowingException("Không tìm thấy người dùng", "USER_NOT_FOUND"));
        
        LocalDate today = LocalDate.now();
        long lateFee = 0;
        
        // Calculate late fee if overdue
        if (today.isAfter(loan.getDueDate())) {
            long daysLate = today.toEpochDay() - loan.getDueDate().toEpochDay();
            lateFee = daysLate * loan.getQuantity() * LATE_FEE_PER_DAY;
            logger.warn("⚠️ Quá hạn {} ngày, phí phạt: {} đ", daysLate, lateFee);
        }
        
        // Update loan
        loan.setStatus(LoanStatus.RETURNED);
        loan.setReturnDate(today);
        loan.setLateFee(lateFee);
        Loan updatedLoan = loanRepository.save(loan);
        
        // Create fine if overdue
        if (lateFee > 0) {
            Fine fine = Fine.builder()
                .userId(user.getId())
                .loanId(loanId)
                .fineType("LATE_FEE")
                .amount(lateFee)
                .reason(String.format("Quá hạn trả sách: %d ngày, mức phạt %d đ/cuốn/ngày", 
                    today.toEpochDay() - loan.getDueDate().toEpochDay(), LATE_FEE_PER_DAY))
                .createdDate(today)
                .status("PENDING")
                .build();
            fineRepository.save(fine);
            
            // Suspend card if fine unpaid
            user.setStatus("SUSPENDED");
            userRepository.save(user);
            logger.warn("🔒 Thẻ SUSPENDED do phí chưa thanh toán");
        }
        
        // Increase book quantity
        Book book = bookRepository.findById(loan.getBookId())
            .orElseThrow(() -> new BorrowingException("Không tìm thấy sách", "BOOK_NOT_FOUND"));
        book.setQuantity(book.getQuantity() + loan.getQuantity());
        book.setStatus("AVAILABLE");
        bookRepository.save(book);
        
        logger.info("✅ Book RETURNED: {} | returnDate: {} | lateFee: {}", 
            loanId, today, lateFee);
        
        return updatedLoan;
    }
    
    // ===================== VALIDATION & HELPERS =====================
    
    /**
     * Validate card (thẻ phải ACTIVE, chưa hết hạn)
     */
    private void validateCard(User user) {
        if (user.getStatus() == null) {
            throw new CardNotFoundException(user.getId());
        }
        
        if (user.getStatus().equals("SUSPENDED")) {
            throw new CardInactiveException("Thẻ bị khóa do vi phạm");
        }
        
        if (user.getStatus().equals("EXPIRED") || 
            (user.getExpiryDate() != null && user.getExpiryDate().isBefore(LocalDate.now()))) {
            throw new CardExpiredException(user.getExpiryDate() != null ? user.getExpiryDate().toString() : "Thẻ hết hạn");
        }
        
        if (!user.getStatus().equals("ACTIVE")) {
            throw new CardInactiveException("Thẻ không hoạt động");
        }
    }
    
    /**
     * Validate borrow limit (PENDING + PICKED_UP ≤ 3)
     */
    private void validateBorrowLimit(String userId) {
        // Count loans with PENDING or PICKED_UP status
        long pendingCount = loanRepository.countByUserIdAndStatus(userId, LoanStatus.PENDING.toString());
        long pickedUpCount = loanRepository.countByUserIdAndStatus(userId, LoanStatus.PICKED_UP.toString());
        long totalActive = pendingCount + pickedUpCount;
        
        logger.info("📊 User loans - PENDING: {}, PICKED_UP: {}, Total: {}", 
            pendingCount, pickedUpCount, totalActive);
        
        if (totalActive >= MAX_BORROW_LIMIT) {
            throw new BorrowLimitExceededException((int) totalActive);
        }
    }
    
    /**
     * ADMIN QUICK BORROW - Mượn nhanh (khách hàng đến trực tiếp tại quầy)
     * 
     * Bước:
     * 1. Validate thẻ (ACTIVE, không hết hạn)
     * 2. Validate giới hạn mượn (PENDING + PICKED_UP ≤ 3)
     * 3. Validate sách có trong kho
     * 4. Tạo Loan với status = PICKED_UP ngay lập tức
     * 5. Set borrowDate = hôm nay, dueDate = ngày được chỉ định
     * 6. -1 quantity sách
     * 
     * @param userId User ID
     * @param bookId Book ID
     * @param dueDate Ngày hạn trả (LocalDate)
     * @return Loan vừa tạo (PICKED_UP)
     */
    @Transactional
    public Loan quickBorrow(String userId, String bookId, LocalDate dueDate) {
        logger.info("⚡ Admin xử lý mượn nhanh: userId={}, bookId={}, dueDate={}", userId, bookId, dueDate);
        
        // Validate user & card
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BorrowingException("Không tìm thấy người dùng", "USER_NOT_FOUND"));
        validateCard(user);
        
        // Validate borrow limit
        validateBorrowLimit(userId);
        
        // Validate book exists & available
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new BorrowingException("Không tìm thấy sách", "BOOK_NOT_FOUND"));
        
        if (book.getQuantity() <= 0) {
            throw new BookNotAvailableException("Sách '" + book.getTitle() + "' không còn");
        }
        
        // Validate dueDate
        LocalDate today = LocalDate.now();
        if (dueDate == null) {
            dueDate = today.plusDays(BORROW_DURATION_DAYS);
        }
        if (dueDate.isBefore(today)) {
            throw new BorrowingException("Ngày hạn trả không thể là ngày quá khứ", "INVALID_DATE");
        }
        if (dueDate.isAfter(today.plusDays(BORROW_DURATION_DAYS))) {
            throw new BorrowingException("Ngày hạn trả tối đa là " + BORROW_DURATION_DAYS + " ngày", "EXCEEDS_MAX_DAYS");
        }
        
        // Create loan as PICKED_UP immediately (customer taking it now at counter)
        Loan loan = Loan.builder()
            .userId(userId)
            .bookId(bookId)
            .appointmentTime(LocalDateTime.now())
            .borrowDate(today)
            .dueDate(dueDate)
            .status(LoanStatus.PICKED_UP)
            .quantity(1)
            .lateFee(0)
            .createdAt(LocalDateTime.now())
            .notes("Mượn nhanh tại quầy - Admin xử lý trực tiếp")
            .build();
        
        Loan savedLoan = loanRepository.save(loan);
        
        // Decrease book quantity
        book.setQuantity(book.getQuantity() - 1);
        if (book.getQuantity() <= 0) {
            book.setStatus("BORROWED");
        }
        bookRepository.save(book);
        
        logger.info("✅ Quick Borrow completed: {} | borrowDate: {} | dueDate: {}", 
            savedLoan.getId(), today, dueDate);
        
        return savedLoan;
    }
    
    /**
     * Get loan by ID
     */
    public Optional<Loan> getLoanById(String loanId) {
        return loanRepository.findById(loanId);
    }
    
    /**
     * Get all loans by user
     */
    public List<Loan> getLoansByUserId(String userId) {
        return loanRepository.findByUserId(userId);
    }
}
