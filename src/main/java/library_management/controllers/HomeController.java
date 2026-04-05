package library_management.controllers;

import java.security.Principal;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import library_management.repository.FineRepository;
import library_management.repository.UserRepository;
import library_management.repository.BookRepository;

@Controller
public class HomeController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;
    
    @Autowired
    private FineRepository fineRepository;

    @GetMapping("/")
    public String index(Model model, Principal principal) {
        if (principal != null) {
            // Lấy MSSV của người đang đăng nhập
            String currentMssv = principal.getName();
            
            // Tìm thông tin User để hiển thị lên Thẻ độc giả
            userRepository.findByMssv(currentMssv).ifPresent(user -> {
                model.addAttribute("user", user);
                
                // Thêm thông báo về trạng thái thẻ
                String cardNotification = null;
                String cardNotificationType = "info";
                
                // Kiểm tra trạng thái thẻ
                if ("SUSPENDED".equals(user.getStatus())) {
                    long pendingFines = fineRepository.countByUserIdAndStatus(user.getId(), "PENDING");
                    if (pendingFines > 0) {
                        cardNotification = String.format("🔒 Thẻ bị khóa do có %d phiếu phạt chưa thanh toán. Vui lòng liên hệ admin để thanh toán.", pendingFines);
                        cardNotificationType = "danger";
                    }
                } else if ("EXPIRED".equals(user.getStatus())) {
                    cardNotification = "⚠️ Thẻ độc giả hết hạn! Vui lòng liên hệ admin gia hạn.";
                    cardNotificationType = "warning";
                } else if ("ACTIVE".equals(user.getStatus()) && user.getExpiryDate() != null) {
                    // Kiểm tra thẻ sắp hết hạn (< 30 ngày)
                    long daysLeft = user.getExpiryDate().toEpochDay() - LocalDate.now().toEpochDay();
                    if (daysLeft > 0 && daysLeft <= 30) {
                        cardNotification = String.format("📅 Thẻ độc giả sắp hết hạn trong %d ngày. Hãy gia hạn trước!", daysLeft);
                        cardNotificationType = "warning";
                    }
                }
                
                if (cardNotification != null) {
                    model.addAttribute("cardNotification", cardNotification);
                    model.addAttribute("cardNotificationType", cardNotificationType);
                }
            });
        }
        // Thêm danh sách sách vào model để hiển thị trên trang home
        model.addAttribute("books", bookRepository.findAll());
        return "home"; // Trả về file home.html
    }
}