package library_management.repository;

import library_management.models.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    
    // Tìm tất cả notification của một user, sắp xếp theo thời gian mới nhất trước
    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);
    
    // Tìm unread notification của user
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(String userId);
    
    // Đếm unread notification
    long countByUserIdAndIsReadFalse(String userId);
}
