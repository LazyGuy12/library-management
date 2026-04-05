package library_management.controllers;

import library_management.models.Book;
import library_management.models.Loan;
import library_management.models.LoanStatus;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MVC Controller xử lý mượn sách - APPOINTMENT BASED FLOW
 * 
 * Endpoints:
 * - GET /borrowing/borrow-form : Form đặt lịch mượn sách (chọn sách + appointment time)
 * - POST /borrowing/request-borrow : Tạo Loan PENDING
 * - POST /borrowing/{loanId}/pickup : Admin xác nhận đã lấy (PENDING → PICKED_UP)
 * - POST /borrowing/{loanId}/cancel : Hủy đặt lịch (+ lý do)
 * - POST /borrowing/{loanId}/return : Trả sách (PICKED_UP → RETURNED)
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
     * ⚠️ DEPRECATED: Sử dụng POST /user/borrow thay vào đó
     * User đặt lịch mượn sách từ trang chủ, không phải từ đây
     */
    /**
     * Trả sách (user hoặc admin)
     * Chuyển PICKED_UP → RETURNED
     * Tính phí nếu quá hạn
     */
    @PostMapping("/{loanId}/return")
    public String returnBook(@PathVariable String loanId,
                            Principal principal,
                            RedirectAttributes redirectAttributes) {
        try {
            Optional<Loan> loanOpt = loanRepository.findById(loanId);
            if (!loanOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "❌ Không tìm thấy phiếu mượn");
                return "redirect:/user/borrow-history";
            }
            
            Loan returnedLoan = borrowingService.returnBook(loanId);
            
            String message = "✅ Trả sách thành công!";
            if (returnedLoan.getLateFee() > 0) {
                message += String.format(" Phí phạt: %.0f đ", returnedLoan.getLateFee());
            }
            redirectAttributes.addFlashAttribute("success", message);
            
            return "redirect:/user/borrow-history";
            
        } catch (BorrowingException e) {
            redirectAttributes.addFlashAttribute("error", "❌ " + e.getMessage());
            return "redirect:/user/borrow-history";
        }
    }
    
    /**
     * ADMIN: Xác nhận đã lấy sách
     * Chuyển PENDING → PICKED_UP
     */
    @PostMapping("/{loanId}/pickup")
    public String pickupBook(@PathVariable String loanId,
                            RedirectAttributes redirectAttributes) {
        try {
            Loan loan = borrowingService.pickupBook(loanId);
            redirectAttributes.addFlashAttribute("success",
                String.format("✅ Đã xác nhận lấy sách! Hạn trả: %s",
                    loan.getDueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
            return "redirect:/admin/borrow-history";
        } catch (BorrowingException e) {
            redirectAttributes.addFlashAttribute("error", "❌ " + e.getMessage());
            return "redirect:/admin/borrow-history";
        }
    }
    
    /**
     * ADMIN: Hủy đặt lịch mượn
     * Chuyển PENDING/PICKED_UP → CANCELLED
     * Cộng lại quantity sách
     */
    @PostMapping("/{loanId}/cancel")
    public String cancelBorrow(@PathVariable String loanId,
                              @RequestParam String cancelReason,
                              RedirectAttributes redirectAttributes) {
        try {
            borrowingService.cancelBorrow(loanId, cancelReason);
            redirectAttributes.addFlashAttribute("success",
                String.format("✅ Hủy đặt lịch thành công! Lý do: %s", cancelReason));
            return "redirect:/admin/borrow-history";
        } catch (BorrowingException e) {
            redirectAttributes.addFlashAttribute("error", "❌ " + e.getMessage());
            return "redirect:/admin/borrow-history";
        }
    }
}
