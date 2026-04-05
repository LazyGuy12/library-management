package library_management.controllers;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import library_management.models.Loan;
import library_management.models.LoanStatus;
import library_management.models.User;
import library_management.repository.LoanRepository;
import library_management.repository.NotificationRepository;
import library_management.repository.UserRepository;

/**
 * Global model attributes for all controllers
 * Tự động thêm notification count vào mọi trang cho user đã login
 */
@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoanRepository loanRepository;
    
    @Autowired
    private NotificationRepository notificationRepository;

    /**
     * Tự động thêm notificationCount vào model của tất cả requests
     * Count từ database (unread notifications)
     */
    @ModelAttribute("notificationCount")
    public int addNotificationCount(Principal principal) {
        if (principal == null) {
            return 0;
        }
        
        try {
            Optional<User> userOpt = userRepository.findByMssv(principal.getName());
            if (!userOpt.isPresent()) {
                return 0;
            }
            
            User user = userOpt.get();
            
            // Count unread notifications từ database
            long count = notificationRepository.countByUserIdAndIsReadFalse(user.getId());
            return (int) count;
        } catch (Exception e) {
            return 0;
        }
    }
}
