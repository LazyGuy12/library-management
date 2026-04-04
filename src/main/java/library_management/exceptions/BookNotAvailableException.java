package library_management.exceptions;

/**
 * Exception khi cuốn sách không có sẵn để mượn
 * (sách đang mượn hoặc không còn trong hệ thống)
 */
public class BookNotAvailableException extends BorrowingException {
    
    public BookNotAvailableException(String bookTitle) {
        super("Cuốn sách '" + bookTitle + "' hiện không có sẵn để mượn", "BOOK_NOT_AVAILABLE");
    }
}
