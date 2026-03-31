package library_management.repository;

import library_management.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username); //Tim kiem nguoi dung dua tren username
    Boolean existsByUsername(String username); //Kiem tra username da ton tai hay khong
}