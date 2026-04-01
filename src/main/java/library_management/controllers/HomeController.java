package library_management.controllers;

import java.security.Principal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model; // SỬA LẠI IMPORT NÀY
import org.springframework.web.bind.annotation.GetMapping;
import library_management.repository.UserRepository;

@Controller
public class HomeController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/")
    public String index(Model model, Principal principal) {
        if (principal != null) {
            // Lấy MSSV của người đang đăng nhập
            String currentMssv = principal.getName();
            
            // Tìm thông tin User để hiển thị lên Thẻ độc giả
            userRepository.findByMssv(currentMssv).ifPresent(user -> {
                model.addAttribute("user", user);
            });
        }
        return "home"; // Trả về file home.html
    }
}