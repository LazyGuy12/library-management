package library_management.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashSet;
import java.util.Set;

@Document(collection = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    private String id; // MongoDB dùng String làm ID mặc định

    @Indexed(unique = true)
    private String username;

    private String password;
    private String fullName;

    // Quan trọng: Lưu một danh sách các Role cho User này
    private Set<ERole> roles = new HashSet<>();
}
