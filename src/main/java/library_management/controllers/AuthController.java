package library_management.controllers;

import library_management.models.ERole;
import library_management.models.User;
import library_management.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Set;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    // 1. Trả về trang login
    @GetMapping("/login")
    public String login() {
        return "login"; // Trả về file login.html trong templates
    }

    // 2. Trả về trang đăng ký
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("user", new User());
        return "register"; // Trả về file register.html
    }

    // 3. Xử lý logic đăng ký người dùng mới
    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user) {
        // Mã hóa mật khẩu trước khi lưu
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Mặc định đăng ký xong là quyền USER (Độc giả)
        user.setRoles(Set.of(ERole.ROLE_USER));

        userRepository.save(user);
        return "redirect:/login"; // Đăng ký xong quay về login
    }
}