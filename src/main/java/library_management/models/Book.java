package library_management.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "books")
@Data
public class Book {
    @Id
    private String id;
    private String title;
    private String category;
    private String author;
    private int quantity;
    private String isbn;
}
