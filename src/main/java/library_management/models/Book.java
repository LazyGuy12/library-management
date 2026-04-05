package library_management.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Model đại diện cho một cuốn sách trong thư viện
 */
@Document(collection = "books")
@Data
public class Book {
    @Id
    private String id;
    
    private String title;           // Tên sách
    private String category;        // Thể loại
    private String author;          // Tác giả
    private String imageUrl;        // URL ảnh bìa
    private int quantity;           // Số lượng (đã bị deprecated, dùng status thay thế)
    private String isbn;            // Mã ISBN
    private String description;     // Mô tả sách
    
    // Trạng thái của cuốn sách (AVAILABLE: có sẵn, BORROWED: đang mượn)
    // Default là AVAILABLE khi tạo sách mới
    private String status = BookStatus.AVAILABLE.name();
}
 