package library_management.controllers;

import library_management.models.Fine;
import library_management.models.User;
import library_management.repository.FineRepository;
import library_management.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Controller xử lý quản lý thẻ độc giả (dành cho Admin)
 * - Xem danh sách thẻ
 * - Xem chi tiết thẻ
 * - Gia hạn thẻ
 * - Xem phiếu phạt chưa thanh toán
 * - Xác nhận thanh toán phiếu phạt (mở khóa thẻ)
 */
@Controller
@RequestMapping("/admin/cards")
public class CardController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private FineRepository fineRepository;
    
    /**
     * Hiển thị danh sách thẻ độc giả của người dùng (chỉ role=USER, không bao gồm role=ADMIN)
     * Admin là quản lý, không cần thẻ độc giả
     */
    @GetMapping
    public String listCards(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        
        // Lấy danh sách người dùng với vai trò USER (loại bỏ admin)
        List<User> users = userRepository.findAll().stream()
            .filter(user -> user.getRole() != null && user.getRole().equals("USER"))
            .toList();
        
        model.addAttribute("users", users);
        model.addAttribute("currentUser", principal.getName());
        
        return "admin/card-list";
    }
    
    /**
     * Xem chi tiết thẻ độc giả của một người dùng
     */
    @GetMapping("/{userId}")
    public String viewCardDetail(@PathVariable String userId, Model model, 
                                 Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }
        
        // Lấy thông tin user
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            redirectAttributes.addFlashAttribute("error", "❌ Không tìm thấy thẻ độc giả");
            return "redirect:/admin/cards";
        }
        
        User user = userOpt.get();
        
        // Lấy phiếu phạt chưa thanh toán
        List<Fine> pendingFines = fineRepository.findByUserIdAndStatus(userId, "PENDING");
        
        // Tính tổng phí phạt chưa thanh toán
        long totalPendingFine = pendingFines.stream()
            .mapToLong(Fine::getAmount)
            .sum();
        
        model.addAttribute("user", user);
        model.addAttribute("pendingFines", pendingFines);
        model.addAttribute("totalPendingFine", totalPendingFine);
        
        return "admin/card-detail";
    }
    
    /**
     * Gia hạn thẻ độc giả
     */
    @PostMapping("/{userId}/renew")
    public String renewCard(@PathVariable String userId, 
                           @RequestParam(defaultValue = "365") int days,
                           Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }
        
        // Lấy thông tin user
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            redirectAttributes.addFlashAttribute("error", "❌ Không tìm thấy thẻ độc giả");
            return "redirect:/admin/cards";
        }
        
        User user = userOpt.get();
        
        // Gia hạn thẻ
        LocalDate newExpiryDate = LocalDate.now().plusDays(days);
        user.setExpiryDate(newExpiryDate);
        user.setStatus("ACTIVE");
        user.setLastRenewedDate(LocalDate.now());
        user.setRenewalCount(user.getRenewalCount() + 1);
        
        userRepository.save(user);
        
        redirectAttributes.addFlashAttribute("success", 
            String.format("✅ Gia hạn thẻ thành công! Hạn mới: %s", newExpiryDate));
        
        return "redirect:/admin/cards/" + userId;
    }
    
    /**
     * Xác nhận thanh toán phiếu phạt (mở khóa thẻ)
     */
    @PostMapping("/fine/{fineId}/pay")
    public String payFine(@PathVariable String fineId, 
                         Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }
        
        // Lấy thông tin phiếu phạt
        Optional<Fine> fineOpt = fineRepository.findById(fineId);
        if (!fineOpt.isPresent()) {
            redirectAttributes.addFlashAttribute("error", "❌ Không tìm thấy phiếu phạt");
            return "redirect:/admin/cards";
        }
        
        Fine fine = fineOpt.get();
        String userId = fine.getUserId();
        
        // Cập nhật phiếu phạt
        fine.setStatus("PAID");
        fine.setPaidDate(LocalDate.now());
        fine.setConfirmedBy(principal.getName());
        fine.setConfirmedAt(LocalDateTime.now());
        fineRepository.save(fine);
        
        // Kiểm tra xem còn phiếu phạt chưa thanh toán không
        long pendingCount = fineRepository.countByUserIdAndStatus(userId, "PENDING");
        
        // Nếu không còn phiếu phạt, mở khóa thẻ
        if (pendingCount == 0) {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                
                // Nếu thẻ chưa hết hạn, mở khóa
                if (user.getExpiryDate() != null && 
                    user.getExpiryDate().isAfter(LocalDate.now())) {
                    user.setStatus("ACTIVE");
                    userRepository.save(user);
                }
            }
        }
        
        redirectAttributes.addFlashAttribute("success", 
            String.format("✅ Xác nhận thanh toán phí phạt %d đ thành công!", fine.getAmount()));
        
        return "redirect:/admin/cards/" + userId;
    }
    
    /**
     * Xem chi tiết phiếu phạt
     */
    @GetMapping("/fine/{fineId}")
    public String viewFineDetail(@PathVariable String fineId, Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        
        Optional<Fine> fineOpt = fineRepository.findById(fineId);
        if (!fineOpt.isPresent()) {
            return "redirect:/admin/cards";
        }
        
        Fine fine = fineOpt.get();
        Optional<User> userOpt = userRepository.findById(fine.getUserId());
        
        model.addAttribute("fine", fine);
        if (userOpt.isPresent()) {
            model.addAttribute("user", userOpt.get());
        }
        
        return "admin/fine-detail";
    }
}
