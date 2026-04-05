package library_management.controllers;

import library_management.models.Book;
import library_management.models.Loan;
import library_management.models.User;
import library_management.exceptions.BorrowingException;
import library_management.repository.BookRepository;
import library_management.repository.UserRepository;
import library_management.repository.LoanRepository;
import library_management.services.BorrowingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MVC Controller xử lý các yêu cầu mượn sách
 * Endpoints:
 * - GET /borrowing/borrow-form : Hiển thị form mượn sách
 * - POST /borrowing/borrow : Mượn sách
 * - POST /borrowing/return/{loanId} : Trả sách
 */
@Controller
@RequestMapping("/borrowing")
public class BorrowingController {
    
    @Autowired
    private BorrowingService borrowingService;
    
    @Autowired
    private BookRepository bookRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private LoanRepository loanRepository;
    
    /**
     * Hiển thị form mượn sách (lựa chọn sách và số lượng)
     */
    @GetMapping("/borrow-form")
    public String showBorrowForm(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        
        // Lấy danh sách sách có sẵn
        List<Book> availableBooks = bookRepository.findAll()
            .stream()
            .filter(book -> book.getQuantity() > 0)
            .collect(Collectors.toList());
        
        model.addAttribute("books", availableBooks);
        
        // Lấy thông tin user
        Optional<User> userOpt = userRepository.findByMssv(principal.getName());
        if (userOpt.isPresent()) {
            model.addAttribute("user", userOpt.get());
        }
        
        return "borrowing/borrow-form";
    }
    
    /**
     * Xử lý mượn sách (form submission)
     * 
     * @param bookId ID của cuốn sách
     * @param quantity Số lượng mượn
     * @param principal Thông tin người dùng đăng nhập
     * @return Redirect tới lịch sử mượn hoặc form với lỗi
     */
    @PostMapping("/borrow")
    public String borrowBook(@RequestParam String bookId, 
                            @RequestParam(defaultValue = "1") int quantity,
                            Principal principal, 
                            RedirectAttributes redirectAttributes) {
        try {
            // Kiểm tra xem user có đăng nhập không
            if (principal == null) {
                redirectAttributes.addFlashAttribute("error", "Bạn cần đăng nhập để mượn sách");
                return "redirect:/login";
            }
            
            // Gọi service mượn sách
            Loan loan = borrowingService.borrowBook(bookId, principal.getName(), quantity);
            
            // Lấy thông tin sách
            Book book = bookRepository.findById(bookId).orElse(null);
            
            // Thêm success message
            redirectAttributes.addFlashAttribute("success", 
                String.format("✅ Mượn sách '%s' thành công! Hạn trả: %s", 
                    book != null ? book.getTitle() : "N/A",
                    loan.getDueDate()));
            
            return "redirect:/user/borrow-history";
            
        } catch (BorrowingException e) {
            // Lỗi từ logic mượn sách
            redirectAttributes.addFlashAttribute("error", "❌ " + e.getMessage());
            return "redirect:/borrowing/borrow-form";
                
        } catch (Exception e) {
            // Lỗi không mong muốn
            redirectAttributes.addFlashAttribute("error", "❌ Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/borrowing/borrow-form";
        }
    }
    
    /**
     * Xử lý trả sách (form submission)
     * 
     * @param loanId ID của bản ghi mượn
     * @param principal Thông tin người dùng đăng nhập
     * @return Redirect tới lịch sử mượn
     */
    @PostMapping("/return/{loanId}")
    public String returnBook(@PathVariable String loanId, 
                            Principal principal,
                            RedirectAttributes redirectAttributes) {
        try {
            // Lấy thông tin Loan để kiểm tra quyền
            Optional<Loan> loanOpt = loanRepository.findById(loanId);
            if (!loanOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "❌ Không tìm thấy phiếu mượn");
                return "redirect:/admin/borrow-history";
            }
            
            Loan loan = loanOpt.get();
            
            // Gọi service trả sách
            Loan returnedLoan = borrowingService.returnBook(loanId, loan.getBookId());
            
            // Thêm success message
            String message = "✅ Trả sách thành công!";
            if (returnedLoan.getLateFee() > 0) {
                message += String.format(" Phí phạt quá hạn: %.0f đ", returnedLoan.getLateFee());
            }
            redirectAttributes.addFlashAttribute("success", message);
            
            return "redirect:/admin/borrow-history";
            
        } catch (BorrowingException e) {
            redirectAttributes.addFlashAttribute("error", "❌ " + e.getMessage());
            return "redirect:/admin/borrow-history";
                
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "❌ Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/admin/borrow-history";
        }
    }
}
