package library_management.controllers;

import library_management.models.Book;
import library_management.models.Loan;
import library_management.models.User;
import library_management.dto.LoanHistoryDTO;
import library_management.exceptions.BorrowingException;
import library_management.repository.BookRepository;
import library_management.repository.UserRepository;
import library_management.repository.LoanRepository;
import library_management.services.BorrowingService;
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
     * 
     * @param bookId ID của cuốn sách cần mượn
     * @param model Model để gửi dữ liệu về view
     * @return View form mượn sách
     */
    @GetMapping("/borrow-form/{id}")
    public String showBorrowForm(@PathVariable("id") String bookId, Model model, Principal principal) {
        // Lấy thông tin sách
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy sách"));
        
        // Kiểm tra sách có sẵn không
        if (!book.getStatus().equals("AVAILABLE")) {
            model.addAttribute("error", "Sách này hiện không có sẵn để mượn");
        }
        
        model.addAttribute("book", book);
        
        // Ngày hạn trả mặc định: 14 ngày từ hôm nay
        LocalDate defaultDueDate = LocalDate.now().plusDays(14);
        model.addAttribute("defaultDueDate", defaultDueDate);
        
        // Thêm thông tin user để hiển thị trên sidebar
        if (principal != null) {
            Optional<User> userOpt = userRepository.findByMssv(principal.getName());
            if (userOpt.isPresent()) {
                model.addAttribute("user", userOpt.get());
            }
        }
        
        return "admin/borrow-book";
    }

    /**
     * Lưu thông tin mượn sách từ form
     * 
     * @param bookId ID sách
     * @param idCard Mã thẻ độc giả
     * @param dueDate Ngày hạn trả
     * @param quantity Số lượng
     * @param attributes Flash attributes cho redirect
     * @return Redirect về trang lịch sử mượn hoặc về form nếu lỗi
     */
    @PostMapping("/save-borrow")
    public String saveBorrow(
            @RequestParam String bookId,
            @RequestParam String idCard,
            @RequestParam LocalDate dueDate,
            @RequestParam int quantity,
            RedirectAttributes attributes) {
        
        try {
            logger.info("🔍 Gọi BorrowingService.borrowBookWithDetails()...");
            // Gọi service mượn sách với chi tiết
            Loan loan = borrowingService.borrowBookWithDetails(bookId, idCard, dueDate, quantity);
            
            logger.info("✅ Mượn sách thành công! Loan ID: {}, Due Date: {}", loan.getId(), loan.getDueDate());
            
            // Mượn thành công
            attributes.addFlashAttribute("borrowSuccess",
                "✅ Mượn sách thành công! Hạn trả: " + loan.getDueDate() + 
                ". Vui lòng quay sách đúng hạn để tránh bị phạt.");
            
        } catch (BorrowingException e) {
            // Lỗi từ logic mượn
            logger.warn("⚠️ BorrowingException: {}", e.getMessage());
            attributes.addFlashAttribute("borrowError", e.getMessage());
            return "redirect:/admin/borrow-form/" + bookId;
            
        } catch (Exception e) {
            // Lỗi không mong muốn - In full stacktrace
            logger.error("❌ Exception không mong muốn khi mượn sách!", e);
            attributes.addFlashAttribute("borrowError", 
                "Có lỗi xảy ra: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            return "redirect:/admin/borrow-form/" + bookId;
        }
        
        return "redirect:/admin/borrow-history";
    }

    /**
     * Xem lịch sử mượn sách của toàn bộ thư viện (dành cho Admin)
     * 
     * @param principal Thông tin người dùng đăng nhập
     * @param model Model
     * @return View lịch sử mượn
     */
    @GetMapping("/borrow-history")
    public String viewBorrowHistory(Principal principal, Model model) {
        
        // Kiểm tra đăng nhập
        if (principal == null) {
            return "redirect:/login";
        }
        
        // Thêm thông tin user để hiển thị trên sidebar
        Optional<User> userOpt = userRepository.findByMssv(principal.getName());
        if (userOpt.isPresent()) {
            model.addAttribute("user", userOpt.get());
        }
        
        // Lấy TẤT CẢ loans trong thư viện (không chỉ của user hiện tại)
        List<Loan> allLoans = loanRepository.findAll();
        
        // Convert sang DTO để hiển thị (lấy thông tin sách và user)
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
                    .borrowDate(loan.getBorrowDate())
                    .dueDate(loan.getDueDate())
                    .returnDate(loan.getReturnDate())
                    .status(loan.getStatus())
                    .lateFee(loan.getLateFee())
                    .build();
            })
            .collect(Collectors.toList());
        
        model.addAttribute("loans", loanDTOs);
        return "admin/borrow-history";
    }
    
    /**
     * Xem chi tiết một giao dịch mượn sách
     * 
     * @param loanId ID bản ghi mượn
     * @param model Model
     * @return View chi tiết
     */
    @GetMapping("/borrow-detail/{loanId}")
    public String viewLoanDetail(@PathVariable String loanId, Model model, Principal principal) {
        
        try {
            // Thêm thông tin user để hiển thị trên sidebar
            if (principal != null) {
                Optional<User> userOpt = userRepository.findByMssv(principal.getName());
                if (userOpt.isPresent()) {
                    model.addAttribute("user", userOpt.get());
                }
            }
            
            // Lấy loan từ DB
            Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bản ghi mượn"));
            
            // Lấy thông tin sách
            Book book = bookRepository.findById(loan.getBookId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sách"));
            
            // Lấy thông tin người mượn
            User user = userRepository.findById(loan.getUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người mượn"));
            
            // Tạo DTO chi tiết
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
                .borrowDate(loan.getBorrowDate())
                .dueDate(loan.getDueDate())
                .returnDate(loan.getReturnDate())
                .status(loan.getStatus())
                .lateFee(loan.getLateFee())
                .build();
            
            model.addAttribute("loanDetail", detail);
            return "admin/borrow-detail";
            
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/admin/borrow-history";
        }
    }

    /**
     * Trả sách
     * 
     * @param loanId ID bản ghi mượn
     * @param attributes Flash attributes
     * @return Redirect về lịch sử
     */
    @GetMapping("/return-book/{loanId}")
    public String returnBook(
            @PathVariable String loanId,
            RedirectAttributes attributes) {
        
        try {
            // Lấy loan từ DB
            Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bản ghi mượn"));
            
            // Gọi service trả sách
            borrowingService.returnBook(loanId, loan.getBookId());
            
            attributes.addFlashAttribute("borrowSuccess", "✅ Trả sách thành công!");
            
        } catch (Exception e) {
            attributes.addFlashAttribute("borrowError", "Lỗi khi trả sách: " + e.getMessage());
        }
        
        return "redirect:/admin/borrow-history";
    }
}