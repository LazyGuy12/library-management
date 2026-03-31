package library_management;

import library_management.models.ERole;
import library_management.models.User;
import library_management.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Set;

@SpringBootApplication
public class LibraryManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(LibraryManagementApplication.class, args);
	}

    @Bean
    CommandLineRunner initData(UserRepository userRepo, BCryptPasswordEncoder encoder) {
        return args -> {
            if (userRepo.findByUsername("admin").isEmpty()) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(encoder.encode("123456"));
                admin.setRoles(Set.of(ERole.ROLE_ADMIN));
                userRepo.save(admin);
                System.out.println(">>> Da tao tai khoan Admin: admin / 123456");
            }
        };
    }
}

