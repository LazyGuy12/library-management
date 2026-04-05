package library_management.exceptions;

/**
 * Exception khi thẻ độc giả không hoạt động (không thể mượn sách)
 */
public class CardInactiveException extends BorrowingException {
    
    public CardInactiveException() {
        super(
            "Thẻ độc giả của bạn không hoạt động. " +
            "Vui lòng liên hệ với thư viện để kích hoạt thẻ!",
            "CARD_INACTIVE"
        );
    }
    
    public CardInactiveException(String message) {
        super(message, "CARD_INACTIVE");
    }
}
