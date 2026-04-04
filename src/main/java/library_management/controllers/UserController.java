package library_management.controllers;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import library_management.models.Book;
import library_management.models.Loan;
import library_management.models.User;
import library_management.dto.LoanHistoryDTO;
import library_management.repository.UserRepository;
import library_management.repository.BookRepository;
import library_management.repository.LoanRepository;

/**
 * Controller xử lý các yêu cầu liên quan đến user
 * - Xem lịch sử mượn sách của user
 * - Xem chi tiết giao dịch mượn sách
 */
@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;
    
    @Autowired
    private LoanRepository loanRepository;
    
    /**
     * Xem lịch sử mượn sách của user hiện tại
     */
    @GetMapping("/borrow-history")
    public String viewMyBorrowHistory(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }
        
        // Lấy user hiện tại
        Optional<User> userOpt = userRepository.findByMssv(principal.getName());
        if (!userOpt.isPresent()) {
            return "redirect:/login";
        }
        
        User user = userOpt.get();
        model.addAttribute("user", user);
        
        // Lấy tất cả loans của user này
        List<Loan> userLoans = loanRepository.findByUserId(user.getId());
        
        // Convert sang DTO để hiển thị
        List<LoanHistoryDTO> loanDTOs = userLoans.stream()
            .map(loan -> {
                Book book = bookRepository.findById(loan.getBookId()).orElse(null);
                return LoanHistoryDTO.builder()
                    .loanId(loan.getId())
                    .bookTitle(book != null ? book.getTitle() : "N/A")
                    .bookCode(book != null ? book.getIsbn() : "N/A")
                    .bookAuthor(book != null ? book.getAuthor() : "N/A")
                    .readerMssv(user.getMssv())
                    .borrowDate(loan.getBorrowDate())
                    .dueDate(loan.getDueDate())
                    .returnDate(loan.getReturnDate())
                    .status(loan.getStatus())
                    .lateFee(loan.getLateFee())
                    .quantity(loan.getQuantity() > 0 ? loan.getQuantity() : 1)
                    .readerExpiryDate(user.getExpiryDate())
                    .readerStatus(user.getStatus())
                    .build();
            })
            .collect(Collectors.toList());
        
        model.addAttribute("loans", loanDTOs);
        return "user/user-borrow-history";
    }
    
    /**
     * Xem chi tiết một giao dịch mượn sách của user
     */
    @GetMapping("/borrow-detail/{loanId}")
    public String viewMyBorrowDetail(@PathVariable String loanId, Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }
        
        // Lấy user hiện tại
        Optional<User> userOpt = userRepository.findByMssv(principal.getName());
        if (!userOpt.isPresent()) {
            return "redirect:/login";
        }
        
        User user = userOpt.get();
        model.addAttribute("user", user);
        
        // Lấy bản ghi mượn
        Optional<Loan> loanOpt = loanRepository.findById(loanId);
        if (!loanOpt.isPresent()) {
            return "redirect:/user/borrow-history";
        }
        
        Loan loan = loanOpt.get();
        
        // Kiểm tra loan này có phải của user hiện tại không
        if (!loan.getUserId().equals(user.getId())) {
            return "redirect:/user/borrow-history";
        }
        
        // Lấy thông tin sách
        Optional<Book> bookOpt = bookRepository.findById(loan.getBookId());
        Book book = bookOpt.orElse(null);
        
        // Tạo DTO để hiển thị
        LoanHistoryDTO loanDTO = LoanHistoryDTO.builder()
            .loanId(loan.getId())
            .bookTitle(book != null ? book.getTitle() : "N/A")
            .bookCode(book != null ? book.getIsbn() : "N/A")
            .bookAuthor(book != null ? book.getAuthor() : "N/A")
            .readerMssv(user.getMssv())
            .borrowDate(loan.getBorrowDate())
            .dueDate(loan.getDueDate())
            .returnDate(loan.getReturnDate())
            .status(loan.getStatus())
            .lateFee(loan.getLateFee())
            .quantity(loan.getQuantity() > 0 ? loan.getQuantity() : 1)
            .readerExpiryDate(user.getExpiryDate())
            .readerStatus(user.getStatus())
            .build();
        
        model.addAttribute("loanDetail", loanDTO);
        return "user/user-borrow-detail";
    }
}
