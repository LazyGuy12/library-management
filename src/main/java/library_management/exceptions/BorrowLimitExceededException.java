package library_management.exceptions;

/**
 * Exception khi người dùng đã mượn đủ số lượng tối đa (3 cuốn)
 */
public class BorrowLimitExceededException extends BorrowingException {
    
    private static final int MAX_BORROW_LIMIT = 3;
    
    public BorrowLimitExceededException(int currentBorrowCount) {
        super(
            String.format(
                "Bạn đã mượn %d/%d cuốn. Không thể mượn thêm sách nữa!",
                currentBorrowCount,
                MAX_BORROW_LIMIT
            ),
            "BORROW_LIMIT_EXCEEDED"
        );
    }
}
