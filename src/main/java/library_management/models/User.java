package library_management.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public class User {

    @Id
    private String id;
    private String mssv;      // Tài khoản đăng nhập (Ví dụ: 2108110123)
    private String password;
    private String fullName;
    private String email;
    private String role;      // "ADMIN" hoặc "USER"

    // Thông tin thẻ độc giả
    private String idCard;     // Tự động sinh (Ví dụ: LIB-2026-2108110123)
    private LocalDate expiryDate;
    // Trạng thái: "ACTIVE" (hoạt động), "EXPIRED" (hết hạn), "SUSPENDED" (bị khóa do vi phạm)
    private String status;
    
    // Ngày gia hạn thẻ lần cuối
    private LocalDate lastRenewedDate;
    
    // Số lần gia hạn trong năm này
    private int renewalCount;

    // ===== GETTER SETTER =====
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getMssv() {
        return mssv;
    }
    public void setMssv(String mssv) {
        this.mssv = mssv;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public String getFullName() {
        return fullName;
    }
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }
    public String getIdCard() {
        return idCard;
    }
    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }
    public LocalDate getExpiryDate() {
        return expiryDate;
    }
    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDate getLastRenewedDate() {
        return lastRenewedDate;
    }
    
    public void setLastRenewedDate(LocalDate lastRenewedDate) {
        this.lastRenewedDate = lastRenewedDate;
    }
    
    public int getRenewalCount() {
        return renewalCount;
    }
    
    public void setRenewalCount(int renewalCount) {
        this.renewalCount = renewalCount;
    }
}