package library_management.services;

import library_management.models.User;
import library_management.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void registerStudent(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("USER"); // Mặc định là Sinh viên

        // Tự động tạo mã thẻ: LIB - Năm - MSSV
        String cardId = "LIB-" + LocalDate.now().getYear() + "-" + user.getMssv();
        user.setIdCard(cardId);

        // Thẻ mặc định có hạn 1 năm và trạng thái hoạt động
        user.setExpiryDate(LocalDate.now().plusYears(1));
        user.setStatus("ACTIVE");

        userRepository.save(user);
    }
}
