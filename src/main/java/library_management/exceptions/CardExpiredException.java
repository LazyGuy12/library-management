package library_management.exceptions;

/**
 * Exception khi thẻ độc giả đã hết hạn
 */
public class CardExpiredException extends BorrowingException {
    
    public CardExpiredException(String expiryDate) {
        super(
            "Thẻ độc giả của bạn đã hết hạn vào ngày: " + expiryDate + 
            ". Vui lòng gia hạn thẻ tại thư viện!",
            "CARD_EXPIRED"
        );
    }
}
