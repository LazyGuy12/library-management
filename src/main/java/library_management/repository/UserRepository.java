package library_management.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import library_management.models.User;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByMssv(String mssv);
    
    /**
     * Tìm user bằng mã thẻ độc giả (idCard)
     * @param idCard Mã thẻ độc giả (ví dụ: LIB-2026-2280601489)
     * @return User nếu tìm thấy
     */
    Optional<User> findByIdCard(String idCard);
}