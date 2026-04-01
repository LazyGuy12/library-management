package library_management.controllers;

import library_management.models.User;
import library_management.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@Controller
public class AuthController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BCryptPasswordEncoder encoder;

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/register")
    public String showRegister(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute User user) {
        // 1. Mã hóa mật khẩu
        user.setPassword(encoder.encode(user.getPassword()));
        user.setRole("USER");
        
        // 2. Tự động tạo IdCard: LIB-2026-MSSV
        String cardId = "LIB-" + LocalDate.now().getYear() + "-" + user.getMssv();
        user.setIdCard(cardId);
        
        // 3. Hạn dùng 1 năm & Trạng thái hoạt động
        user.setExpiryDate(LocalDate.now().plusYears(1));
        user.setStatus("ACTIVE");
        
        userRepository.save(user);
        return "redirect:/login?registered";
    }
}