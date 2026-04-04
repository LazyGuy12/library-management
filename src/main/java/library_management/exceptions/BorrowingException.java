package library_management.exceptions;

/**
 * Custom exception dành cho các lỗi liên quan đến mượn sách
 * Được throw khi không thỏa mãn điều kiện mượn
 */
public class BorrowingException extends RuntimeException {
    
    private final String errorCode;
    
    public BorrowingException(String message) {
        super(message);
        this.errorCode = "BORROWING_ERROR";
    }
    
    public BorrowingException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public BorrowingException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "BORROWING_ERROR";
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
