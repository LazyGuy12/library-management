package library_management.controllers;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import library_management.models.Book;
import library_management.models.Loan;
import library_management.models.LoanStatus;
import library_management.models.Notification;
import library_management.models.User;
import library_management.dto.LoanHistoryDTO;
import library_management.exceptions.BorrowingException;
import library_management.repository.UserRepository;
import library_management.repository.BookRepository;
import library_management.repository.LoanRepository;
import library_management.repository.NotificationRepository;
import library_management.services.BorrowingService;

/**
 * Controller xử lý yêu cầu của User
 * - Xem lịch sử mượn sách
 * - Trả sách
 * - Hủy đặt lịch
 */
@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;
    
    @Autowired
    private LoanRepository loanRepository;
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private BorrowingService borrowingService;
    
    /**
     * Xem lịch sử mượn sách của user (PENDING, PICKED_UP, RETURNED, CANCELLED)
     */
    @GetMapping("/borrow-history")
    public String viewMyBorrowHistory(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }
        
        Optional<User> userOpt = userRepository.findByMssv(principal.getName());
        if (!userOpt.isPresent()) {
            return "redirect:/login";
        }
        
        User user = userOpt.get();
        model.addAttribute("user", user);
        
        List<Loan> userLoans = loanRepository.findByUserId(user.getId());
        
        List<LoanHistoryDTO> loanDTOs = userLoans.stream()
            .map(loan -> {
                Book book = bookRepository.findById(loan.getBookId()).orElse(null);
                return LoanHistoryDTO.builder()
                    .loanId(loan.getId())
                    .bookTitle(book != null ? book.getTitle() : "N/A")
                    .bookCode(book != null ? book.getIsbn() : "N/A")
                    .bookAuthor(book != null ? book.getAuthor() : "N/A")
                    .readerMssv(user.getMssv())
                    .appointmentTime(loan.getAppointmentTime())
                    .borrowDate(loan.getBorrowDate())
                    .dueDate(loan.getDueDate())
                    .returnDate(loan.getReturnDate())
                    .status(loan.getStatus() != null ? loan.getStatus().toString() : "UNKNOWN")
                    .lateFee(loan.getLateFee())
                    .quantity(loan.getQuantity() > 0 ? loan.getQuantity() : 1)
                    .readerExpiryDate(user.getExpiryDate())
                    .readerStatus(user.getStatus())
                    .cancelReason(loan.getCancelReason())
                    .build();
            })
            .collect(Collectors.toList());
        
        model.addAttribute("loans", loanDTOs);
        return "user/user-borrow-history";
    }
    
    /**
     * Xem chi tiết một giao dịch mượn của user
     */
    @GetMapping("/borrow-detail/{loanId}")
    public String viewMyBorrowDetail(@PathVariable String loanId, Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }
        
        Optional<User> userOpt = userRepository.findByMssv(principal.getName());
        if (!userOpt.isPresent()) {
            return "redirect:/login";
        }
        
        User user = userOpt.get();
        model.addAttribute("user", user);
        
        Optional<Loan> loanOpt = loanRepository.findById(loanId);
        if (!loanOpt.isPresent()) {
            return "redirect:/user/borrow-history";
        }
        
        Loan loan = loanOpt.get();
        
        // Kiểm tra quyền: loan phải thuộc user hiện tại
        if (!loan.getUserId().equals(user.getId())) {
            return "redirect:/user/borrow-history";
        }
        
        Optional<Book> bookOpt = bookRepository.findById(loan.getBookId());
        Book book = bookOpt.orElse(null);
        
        LoanHistoryDTO loanDTO = LoanHistoryDTO.builder()
            .loanId(loan.getId())
            .bookTitle(book != null ? book.getTitle() : "N/A")
            .bookCode(book != null ? book.getIsbn() : "N/A")
            .bookAuthor(book != null ? book.getAuthor() : "N/A")
            .readerMssv(user.getMssv())
            .appointmentTime(loan.getAppointmentTime())
            .borrowDate(loan.getBorrowDate())
            .dueDate(loan.getDueDate())
            .returnDate(loan.getReturnDate())
            .status(loan.getStatus() != null ? loan.getStatus().toString() : "UNKNOWN")
            .lateFee(loan.getLateFee())
            .quantity(loan.getQuantity() > 0 ? loan.getQuantity() : 1)
            .readerExpiryDate(user.getExpiryDate())
            .readerStatus(user.getStatus())
            .cancelReason(loan.getCancelReason())
            .build();
        
        model.addAttribute("loanDetail", loanDTO);
        return "user/user-borrow-detail";
    }
    
    /**
     * Trả sách (User)
     * POST /user/{loanId}/return
     */
    @PostMapping("/{loanId}/return")
    public String returnBook(@PathVariable String loanId,
                            RedirectAttributes attributes) {
        try {
            borrowingService.returnBook(loanId);
            attributes.addFlashAttribute("success", "✅ Trả sách thành công!");
        } catch (BorrowingException e) {
            attributes.addFlashAttribute("error", "❌ " + e.getMessage());
        }
        return "redirect:/user/borrow-history";
    }
    
    /**
     * Hủy đặt lịch (User)
     * POST /user/{loanId}/cancel
     */
    @PostMapping("/{loanId}/cancel")
    public String cancelBorrow(@PathVariable String loanId,
                              @RequestParam String cancelReason,
                              RedirectAttributes attributes) {
        try {
            borrowingService.cancelBorrow(loanId, cancelReason);
            attributes.addFlashAttribute("success", "✅ Hủy đặt lịch thành công!");
        } catch (BorrowingException e) {
            attributes.addFlashAttribute("error", "❌ " + e.getMessage());
        }
        return "redirect:/user/borrow-history";
    }

    /**
     * Hiển thị form đặt lịch mượn sách với sách được chọn sẵn
     * GET /user/borrow?bookId=xxx
     */
    @GetMapping("/borrow")
    public String showBorrowForm(@RequestParam(required = false) String bookId,
                                Model model,
                                Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        // Lấy user hiện tại
        Optional<User> userOpt = userRepository.findByMssv(principal.getName());
        if (!userOpt.isPresent()) {
            return "redirect:/login";
        }
        
        User user = userOpt.get();
        model.addAttribute("user", user);

        // Lấy sách được chọn (nếu bookId được truyền)
        if (bookId != null && !bookId.isEmpty()) {
            Optional<Book> bookOpt = bookRepository.findById(bookId);
            if (bookOpt.isPresent()) {
                model.addAttribute("book", bookOpt.get());
            } else {
                model.addAttribute("error", "❌ Không tìm thấy sách!");
            }
        }

        return "user/borrow-form";
    }

    /**
     * Đặt lịch mượn sách (User request)
     * POST /user/borrow
     */
    @PostMapping("/borrow")
    public String requestBorrow(@RequestParam String bookId,
                               @RequestParam String appointmentTime,
                               @RequestParam String dueDate,
                               Principal principal,
                               RedirectAttributes attributes) {
        try {
            if (principal == null) {
                return "redirect:/login";
            }

            Optional<User> userOpt = userRepository.findByMssv(principal.getName());
            if (!userOpt.isPresent()) {
                return "redirect:/login";
            }

            User user = userOpt.get();

            // Parse appointment time từ format datetime-local
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            LocalDateTime appointmentDateTime = LocalDateTime.parse(appointmentTime, formatter);
            
            // Parse due date
            LocalDate dueDateObj = LocalDate.parse(dueDate);

            // Gọi service để tạo loan
            borrowingService.requestBorrow(user.getId(), bookId, appointmentDateTime, dueDateObj);
            
            attributes.addFlashAttribute("success", 
                "✅ Đặt lịch mượn sách thành công! Vui lòng đến thư viện vào đúng thời gian để nhận sách.");
            return "redirect:/user/borrow-history";

        } catch (BorrowingException e) {
            attributes.addFlashAttribute("error", "❌ " + e.getMessage());
            return "redirect:/user/borrow?bookId=" + bookId;
        } catch (Exception e) {
            attributes.addFlashAttribute("error", "❌ Lỗi: " + e.getMessage());
            return "redirect:/user/borrow?bookId=" + bookId;
        }
    }

    /**
     * Xem tất cả notifications của user (từ database)
     * Đánh dấu tất cả thành đã đọc
     * GET /user/notifications
     */
    @GetMapping("/notifications")
    public String viewNotifications(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }
        
        Optional<User> userOpt = userRepository.findByMssv(principal.getName());
        if (!userOpt.isPresent()) {
            return "redirect:/login";
        }
        
        User user = userOpt.get();
        model.addAttribute("user", user);
        
        // Lấy tất cả notifications từ database, sắp xếp mới nhất trước
        List<Notification> dbNotifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        
        // Mark all as read
        for (Notification notif : dbNotifications) {
            if (!notif.isRead()) {
                notif.setRead(true);
                notificationRepository.save(notif);
            }
        }
        
        // Convert để template dùng
        List<java.util.Map<String, Object>> notifications = new java.util.ArrayList<>();
        for (Notification notif : dbNotifications) {
            java.util.Map<String, Object> notifMap = new java.util.HashMap<>();
            notifMap.put("id", notif.getId());
            notifMap.put("type", notif.getType());
            notifMap.put("title", notif.getTitle());
            notifMap.put("message", notif.getMessage());
            notifMap.put("reason", notif.getReason());
            notifMap.put("isRead", notif.isRead());
            notifMap.put("createdAt", notif.getCreatedAt() != null 
                ? notif.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "N/A");
            
            java.util.Map<String, String> data = new java.util.HashMap<>();
            if ("CANCEL_BORROW".equals(notif.getType())) {
                data.put("reason", notif.getReason() != null ? notif.getReason() : "Không có lý do");
                data.put("bookTitle", "Sách không xác định");
                if (notif.getBookId() != null) {
                    Optional<Book> bookOpt = bookRepository.findById(notif.getBookId());
                    if (bookOpt.isPresent()) {
                        data.put("bookTitle", bookOpt.get().getTitle());
                    }
                }
            }
            notifMap.put("data", data);
            notifications.add(notifMap);
        }
        
        model.addAttribute("notifications", notifications);
        model.addAttribute("notificationCount", 0); // Đã mark as read hết nên count = 0
        
        return "user/notifications";
    }
    
    /**
     * Xóa một notification
     * POST /user/notifications/{notificationId}/delete
     */
    @PostMapping("/notifications/{notificationId}/delete")
    public String deleteNotification(@PathVariable String notificationId, 
                                    Principal principal,
                                    RedirectAttributes attributes) {
        if (principal == null) {
            return "redirect:/login";
        }
        
        try {
            Optional<Notification> notifOpt = notificationRepository.findById(notificationId);
            if (notifOpt.isPresent()) {
                Notification notif = notifOpt.get();
                Optional<User> userOpt = userRepository.findByMssv(principal.getName());
                if (userOpt.isPresent() && notif.getUserId().equals(userOpt.get().getId())) {
                    notificationRepository.deleteById(notificationId);
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        
        return "redirect:/user/notifications";
    }
    
    /**
     * Xóa all notifications của user
     * POST /user/notifications/delete-all
     */
    @PostMapping("/notifications/delete-all")
    public String deleteAllNotifications(Principal principal, RedirectAttributes attributes) {
        if (principal == null) {
            return "redirect:/login";
        }
        
        try {
            Optional<User> userOpt = userRepository.findByMssv(principal.getName());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                List<Notification> userNotifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
                notificationRepository.deleteAll(userNotifications);
                attributes.addFlashAttribute("success", "✅ Xóa tất cả thông báo thành công");
            }
        } catch (Exception e) {
            // Ignore
        }
        
        return "redirect:/user/notifications";
    }
}
