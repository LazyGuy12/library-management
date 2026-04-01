package library_management.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import library_management.models.User;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByMssv(String mssv);
}