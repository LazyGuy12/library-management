package library_management.exceptions;

/**
 * Exception khi không tìm thấy thẻ độc giả của người dùng
 */
public class CardNotFoundException extends BorrowingException {
    
    public CardNotFoundException(String userId) {
        super("Không tìm thấy thẻ độc giả của người dùng: " + userId, "CARD_NOT_FOUND");
    }
}
