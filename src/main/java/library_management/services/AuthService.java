package library_management.services;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import library_management.repository.UserRepository;

import library_management.models.User;

@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public void registerStudent(User user) {
        // 1. Mã hóa mật khẩu bảo mật
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("USER");
        
        // 2. Tự động tạo Mã thẻ độc giả (IdCard)
        // Format: LIB - Năm hiện tại - MSSV
        String generatedCardId = "LIB-" + LocalDate.now().getYear() + "-" + user.getMssv();
        user.setIdCard(generatedCardId);
        
        // 3. Đặt thời gian sử dụng 1 năm kể từ ngày đăng ký
        user.setExpiryDate(LocalDate.now().plusYears(1));
        
        // 4. Trạng thái thẻ mặc định là Hoạt động
        user.setStatus("ACTIVE");
        
        userRepository.save(user);
    }
}
