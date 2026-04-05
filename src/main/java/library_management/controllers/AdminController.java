package library_management.controllers;

import library_management.models.Book;
import library_management.models.Loan;
import library_management.models.User;
import library_management.dto.LoanHistoryDTO;
import library_management.repository.BookRepository;
import library_management.repository.UserRepository;
import library_management.repository.LoanRepository;
import library_management.services.BorrowingService;
import library_management.exceptions.BorrowingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller xử lý các yêu cầu từ trang quản lý sách của Admin
 * Các chức năng:
 * - Thêm, sửa, xóa sách
 * - Mượn sách (cho người dùng)
 * - Xem lịch sử mượn sách
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private BookRepository bookRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private LoanRepository loanRepository;
    
    @Autowired
    private BorrowingService borrowingService;

    /**
     * Hiển thị form thêm sách mới
     */
    @GetMapping("/add-book")
    public String showAddForm(Model model, Principal principal) {
        model.addAttribute("book", new Book());
        model.addAttribute("books", bookRepository.findAll());
        
        // Thêm thông tin user để hiển thị trên sidebar
        if (principal != null) {
            Optional<User> userOpt = userRepository.findByMssv(principal.getName());
            if (userOpt.isPresent()) {
                model.addAttribute("user", userOpt.get());
            }
        }
        
        return "admin/add-book";
    }

    /**
     * Lưu sách mới hoặc cập nhật sách hiện tại
     */
    @PostMapping("/add-book")
    public String saveBook(@ModelAttribute Book book, @RequestParam("imageFile") MultipartFile file) {
        try {
            // FIX BUG 1: Thymeleaf renders null id as empty string "".
            // MongoDB sees non-null id and does UPDATE instead of INSERT.
            // Reset empty string id to null so MongoDB does a fresh INSERT.
            if (book.getId() != null && book.getId().isBlank()) {
                book.setId(null);
            }

            if (!file.isEmpty()) {
                // Use absolute path — relative paths fail with transferTo()
                String uploadDir = System.getProperty("user.dir") + "/src/main/resources/static/uploads/";
                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

                File uploadPath = new File(uploadDir);
                if (!uploadPath.exists()) uploadPath.mkdirs();

                file.transferTo(new File(uploadDir + fileName).getAbsoluteFile());
                book.setImageUrl("/uploads/" + fileName);

            } else if (book.getId() != null) {
                // UPDATE with no new image — preserve existing imageUrl from DB
                bookRepository.findById(book.getId()).ifPresent(oldBook ->
                    book.setImageUrl(oldBook.getImageUrl())
                );
            }

            bookRepository.save(book);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return "redirect:/admin/add-book?success";
    }

    /**
     * Hiển thị form sửa thông tin sách
     */
    @GetMapping("/edit-book/{id}")
    public String editBook(@PathVariable String id, Model model) {
        Optional<Book> book = bookRepository.findById(id);
        if (book.isPresent()) {
            model.addAttribute("book", book.get());
        } else {
            model.addAttribute("book", new Book());
        }
        model.addAttribute("books", bookRepository.findAll());
        return "admin/add-book";
    }

    /**
     * Xóa sách khỏi thư viện
     */
    @GetMapping("/delete-book/{id}")
    public String deleteBook(@PathVariable String id) {
        bookRepository.deleteById(id);
        return "redirect:/admin/add-book?success";
    }

    /**
     * Hiển thị form mượn sách với nhập mã thẻ, ngày trả, số lượng
     * ⚠️ DEPRECATED: Use /borrowing/borrow-form instead (appointment-based)
     */
    @GetMapping("/borrow-form/{id}")
    public String showBorrowForm(@PathVariable("id") String bookId, Model model, Principal principal) {
        model.addAttribute("error", "❌ Chương trình mượn sách đã được nâng cấp! Vui lòng dùng tính năng 'Đặt Lịch Mượn' trong menu người dùng.");
        return "redirect:/borrowing/borrow-form";
    }

    /**
     * Xem lịch sử mượn sách của toàn bộ thư viện (dành cho Admin)
     * Hiển thị tất cả loans (PENDING, PICKED_UP, RETURNED, CANCELLED)
     */
    @GetMapping("/borrow-history")
    public String viewBorrowHistory(Principal principal, Model model) {
        
        if (principal == null) {
            return "redirect:/login";
        }
        
        Optional<User> userOpt = userRepository.findByMssv(principal.getName());
        if (userOpt.isPresent()) {
            model.addAttribute("user", userOpt.get());
        }
        
        // Lấy tất cả loans
        List<Loan> allLoans = loanRepository.findAll();
        
        // Convert sang DTO
        List<LoanHistoryDTO> loanDTOs = allLoans.stream()
            .map(loan -> {
                Book book = bookRepository.findById(loan.getBookId()).orElse(null);
                User user = userRepository.findById(loan.getUserId()).orElse(null);
                
                return LoanHistoryDTO.builder()
                    .loanId(loan.getId())
                    .bookTitle(book != null ? book.getTitle() : "Unknown")
                    .bookAuthor(book != null ? book.getAuthor() : "Unknown")
                    .bookCode(book != null ? book.getIsbn() : "Unknown")
                    .readerCardId(user != null ? user.getIdCard() : "Unknown")
                    .readerName(user != null ? user.getFullName() : "Unknown")
                    .readerMssv(user != null ? user.getMssv() : "Unknown")
                    .readerExpiryDate(user != null ? user.getExpiryDate() : null)
                    .readerStatus(user != null ? user.getStatus() : "UNKNOWN")
                    .appointmentTime(loan.getAppointmentTime())
                    .borrowDate(loan.getBorrowDate())
                    .dueDate(loan.getDueDate())
                    .returnDate(loan.getReturnDate())
                    .status(loan.getStatus() != null ? loan.getStatus().toString() : "UNKNOWN")
                    .lateFee(loan.getLateFee())
                    .cancelReason(loan.getCancelReason())
                    .build();
            })
            .collect(Collectors.toList());
        
        model.addAttribute("loans", loanDTOs);
        return "admin/borrow-history";
    }
    
    /**
     * Xem chi tiết một giao dịch mượn sách
     */
    @GetMapping("/borrow-detail/{loanId}")
    public String viewLoanDetail(@PathVariable String loanId, Model model, Principal principal) {
        
        try {
            if (principal != null) {
                Optional<User> userOpt = userRepository.findByMssv(principal.getName());
                if (userOpt.isPresent()) {
                    model.addAttribute("user", userOpt.get());
                }
            }
            
            Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bản ghi mượn"));
            
            Book book = bookRepository.findById(loan.getBookId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sách"));
            
            User user = userRepository.findById(loan.getUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người mượn"));
            
            LoanHistoryDTO detail = LoanHistoryDTO.builder()
                .loanId(loan.getId())
                .bookTitle(book.getTitle())
                .bookAuthor(book.getAuthor())
                .bookCode(book.getIsbn())
                .readerCardId(user.getIdCard())
                .readerName(user.getFullName())
                .readerMssv(user.getMssv())
                .readerExpiryDate(user.getExpiryDate())
                .readerStatus(user.getStatus())
                .appointmentTime(loan.getAppointmentTime())
                .borrowDate(loan.getBorrowDate())
                .dueDate(loan.getDueDate())
                .returnDate(loan.getReturnDate())
                .status(loan.getStatus() != null ? loan.getStatus().toString() : "UNKNOWN")
                .lateFee(loan.getLateFee())
                .cancelReason(loan.getCancelReason())
                .build();
            
            model.addAttribute("loanDetail", detail);
            return "admin/borrow-detail";
            
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/admin/borrow-history";
        }
    }

    /**
     * Xác nhận lấy sách - chuyển từ PENDING → PICKED_UP, set borrowDate = today
     * GET /admin/{loanId}/pickup
     */
    @GetMapping("/{loanId}/pickup")
    public String confirmPickup(@PathVariable String loanId, RedirectAttributes attributes) {
        try {
            borrowingService.pickupBook(loanId);
            attributes.addFlashAttribute("borrowSuccess", "✅ Xác nhận lấy sách thành công!");
        } catch (Exception e) {
            attributes.addFlashAttribute("borrowError", "❌ Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/borrow-history";
    }

    /**
     * Form mượn nhanh - Admin nhập MSSV để mượn sách cho khách hàng đến trực tiếp
     * GET /admin/quick-borrow/{bookId}
     */
    @GetMapping("/quick-borrow/{bookId}")
    public String showQuickBorrowForm(@PathVariable String bookId, Model model, Principal principal) {
        try {
            if (principal != null) {
                Optional<User> userOpt = userRepository.findByMssv(principal.getName());
                if (userOpt.isPresent()) {
                    model.addAttribute("user", userOpt.get());
                }
            }
            
            Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sách"));
            
            model.addAttribute("book", book);
            return "admin/borrow-book";
            
        } catch (Exception e) {
            return "redirect:/admin/add-book?error=" + e.getMessage();
        }
    }

    /**
     * Xử lý mượn nhanh - Admin xử lý mượn sách cho khách hàng đến trực tiếp
     * POST /admin/quick-borrow/{bookId}
     */
    @PostMapping("/quick-borrow/{bookId}")
    public String processQuickBorrow(@PathVariable String bookId,
                                     @RequestParam String idCard,
                                     @RequestParam String dueDate,
                                     RedirectAttributes attributes) {
        try {
            User user = userRepository.findByIdCard(idCard)
                .orElseThrow(() -> new BorrowingException("Không tìm thấy độc giả với mã thẻ: " + idCard, "USER_NOT_FOUND"));
            
            // Check card status
            if (user.getStatus() == null || !user.getStatus().equals("ACTIVE")) {
                String errorMsg = "EXPIRED".equals(user.getStatus()) 
                    ? "Thẻ độc giả đã hết hạn" 
                    : "SUSPENDED".equals(user.getStatus())
                    ? "Thẻ độc giả bị khóa"
                    : "Thẻ độc giả không hoạt động";
                throw new BorrowingException("❌ " + errorMsg, "INVALID_CARD_STATUS");
            }
            
            LocalDate dueDateObj = LocalDate.parse(dueDate);
            Loan loan = borrowingService.quickBorrow(user.getId(), bookId, dueDateObj);
            
            Book book = bookRepository.findById(bookId).orElse(null);
            String bookTitle = book != null ? book.getTitle() : "Sách không xác định";
            
            attributes.addFlashAttribute("borrowSuccess", 
                String.format("✅ Mượn sách thành công!\nSách: %s\nHạn trả: %s", 
                    bookTitle, 
                    loan.getDueDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
            
        } catch (BorrowingException e) {
            attributes.addFlashAttribute("borrowError", "❌ " + e.getMessage());
        } catch (Exception e) {
            attributes.addFlashAttribute("borrowError", "❌ Lỗi: " + e.getMessage());
        }
        
        return "redirect:/admin/add-book";
    }

    /**
     * Trả sách (Admin)
     * GET /admin/{loanId}/return
     */
    @GetMapping("/{loanId}/return")
    public String returnBook(@PathVariable String loanId, RedirectAttributes attributes) {
        try {
            borrowingService.returnBook(loanId);
            attributes.addFlashAttribute("borrowSuccess", "✅ Trả sách thành công!");
        } catch (Exception e) {
            attributes.addFlashAttribute("borrowError", "❌ Lỗi khi trả sách: " + e.getMessage());
        }
        return "redirect:/admin/borrow-history";
    }
}