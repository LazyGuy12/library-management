package library_management;

import library_management.models.User;
import library_management.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.time.LocalDate;

@SpringBootApplication
public class LibraryManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibraryManagementApplication.class, args);
    }

    @Bean
    CommandLineRunner initData(UserRepository userRepo, BCryptPasswordEncoder encoder) {
        return args -> {
            // Kiểm tra theo MSSV thay vì Username
            if (userRepo.findByMssv("admin").isEmpty()) {
                User admin = new User();
                admin.setMssv("admin");
                admin.setFullName("Quản Trị Viên");
                admin.setPassword(encoder.encode("123456"));
                admin.setRole("ADMIN"); // Lưu String cho đơn giản
                admin.setIdCard("ADMIN-MASTER");
                admin.setStatus("ACTIVE");
                admin.setExpiryDate(LocalDate.now().plusYears(99));
                
                userRepo.save(admin);
                System.out.println(">>> Da tao tai khoan Admin: admin / 123456");
            }
        };
    }
}