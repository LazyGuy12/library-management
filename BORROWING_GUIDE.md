# 📚 Hướng dẫn Nghiệp vụ Mượn Sách (Borrowing System)

## 📋 Tổng quan

Hệ thống Mượn Sách được thiết kế với Logic nghiệp vụ hoàn thiện, bao gồm các kiểm tra:
- ✅ Kiểm tra tính hợp lệ của thẻ độc giả
- ✅ Kiểm tra số lượng sách tối đa
- ✅ Kiểm tra trạng thái sách
- ✅ Tạo bản ghi mượn (Loan)
- ✅ Cập nhật trạng thái sách

---

## 🏗️ Kiến trúc Code

### 1️⃣ **Models (Model Layer)**

#### `Book.java`
```java
@Document(collection = "books")
public class Book {
    private String id;
    private String title;
    private String category;
    private String author;
    private String imageUrl;
    private int quantity;              // Deprecated - dùng status thay thế
    private String status = "AVAILABLE"; // AVAILABLE hoặc BORROWED
}
```

#### `Loan.java` - Bản ghi mượn sách
```java
@Document(collection = "loans")
public class Loan {
    private String id;               // ID bản ghi mượn
    private String bookId;           // ID sách mượn
    private String userId;           // ID người mượn
    private LocalDate borrowDate;    // Ngày mượn
    private LocalDate dueDate;       // Hạn trả (14 ngày sau)
    private LocalDate returnDate;    // Ngày thực tế trả
    private String status;           // ACTIVE (chưa trả) hoặc RETURNED
    private int lateFee;             // Tiền phạt nếu quá hạn
    private LocalDateTime createdAt; // Thời gian tạo
}
```

#### `User.java` - Thông tin thẻ độc giả
```java
public class User {
    private String id;
    private String mssv;             // MSSV/Tài khoản đăng nhập
    private String fullName;
    private String email;
    private String idCard;           // Số hiệu thẻ (LIB-2026-XXXX)
    private LocalDate expiryDate;    // Hạn thẻ
    private String status;           // ACTIVE hoặc INACTIVE
}
```

### 2️⃣ **Exception Classes (Custom Exceptions)**

| Exception | Mục đích |
|-----------|---------|
| `BorrowingException` | Exception gốc cho tất cả lỗi mượn sách |
| `CardNotFoundException` | Thẻ độc giả không tồn tại |
| `CardInactiveException` | Thẻ không hoạt động (INACTIVE) |
| `CardExpiredException` | Thẻ đã hết hạn (EXPIRED) |
| `BookNotAvailableException` | Sách không có sẵn (BORROWED) |
| `BorrowLimitExceededException` | Vượt quá 3 cuốn mượn tối đa |

---

## 🎯 Logic Nghiệp vụ (BorrowingService)

### Quy tắc Mượn Sách

1. **Kiểm tra Thẻ Độc Giả**
   - Thẻ phải tồn tại
   - Status = "ACTIVE" (đang hoạt động)
   - expiryDate ≥ hôm nay (chưa hết hạn)
   
   ```java
   // Nếu vi phạm → CardNotFoundException, CardInactiveException, CardExpiredException
   ```

2. **Kiểm tra Số Lượng Sách Đang Mượn**
   - Đếm số Loan có status = "ACTIVE" của user
   - Nếu ≥ 3 cuốn → không cho mượn thêm
   
   ```java
   // Ví dụ: User đã mượn 3 cuốn → BorrowLimitExceededException
   ```

3. **Kiểm tra Trạng Thái Sách**
   - Book.status phải = "AVAILABLE"
   - Nếu = "BORROWED" → không cho mượn
   
   ```java
   // Nếu vi phạm → BookNotAvailableException
   ```

4. **Tạo Bản Ghi Mượn (Loan)**
   ```java
   Loan loan = Loan.builder()
       .bookId(bookId)
       .userId(userId)
       .borrowDate(LocalDate.now())           // Ngày mượn hôm nay
       .dueDate(LocalDate.now().plusDays(14)) // Hạn trả: 14 ngày
       .status("ACTIVE")                      // Trạng thái: Chưa trả
       .lateFee(0)                            // Chưa có phạt
       .createdAt(LocalDateTime.now())
       .build();
   ```

5. **Cập Nhật Trạng Thái Sách**
   ```java
   book.setStatus("BORROWED");  // Đánh dấu sách đang mượn
   bookRepository.save(book);
   ```

### Sơ đồ Quy trình

```
User nhấn "Mượn sách"
    ↓
[1] Lấy thông tin Book
    ↓
[2] Check Book.status = "AVAILABLE"?
    └─ NO → BookNotAvailableException
    ↓ YES
[3] Lấy thông tin User
    ↓
[4] Validate thẻ độc giả
    ├─ Tồn tại?          → CardNotFoundException
    ├─ Status = ACTIVE?  → CardInactiveException
    └─ Chưa hết hạn?     → CardExpiredException
    ↓ OK
[5] Check số lượng sách mượn < 3?
    └─ NO (≥3) → BorrowLimitExceededException
    ↓ YES
[6] Tạo Loan record
    ↓
[7] Update Book.status = "BORROWED"
    ↓
✅ Mượn thành công! Hạn trả: DD/MM/YYYY
```

---

## 🚀 Cách Sử Dụng

### A. Với Web Interface (Form)

**Endpoint:** `GET /admin/borrow-book/{bookId}`

```html
<a href="/admin/borrow-book/507f1f77bcf86cd799439011" class="btn btn-primary">
    Mượn sách
</a>
```

**Phản hồi:**
- ✅ Thành công → Redirect + thông báo "Mượn sách thành công! Hạn trả: DD/MM/YYYY"
- ❌ Lỗi → Redirect + thông báo lỗi cụ thể

**Thông báo Lỗi Ví dụ:**
```
❌ "Bạn đã mượn 3/3 cuốn. Không thể mượn thêm sách nữa!"
❌ "Cuốn sách 'Java Programming' hiện không có sẵn để mượn"
❌ "Thẻ độc giả của bạn đã hết hạn vào ngày: 2026-03-01"
```

### B. Với REST API

**POST** `/api/borrow/{bookId}`
```bash
curl -X POST http://localhost:8080/api/borrow/507f1f77bcf86cd799439011 \
  -H "Content-Type: application/json"
```

**Response (Thành công):**
```json
{
  "loanId": "507f1f77bcf86cd799439012",
  "bookTitle": "Java Programming",
  "userName": "Nguyễn Văn A",
  "borrowDate": "04/04/2026",
  "dueDate": "18/04/2026",
  "status": "ACTIVE",
  "lateFee": 0,
  "message": "✅ Mượn sách thành công! Hạn trả: 18/04/2026"
}
```

**Response (Lỗi):**
```json
{
  "errorCode": "BORROW_LIMIT_EXCEEDED",
  "message": "Bạn đã mượn 3/3 cuốn. Không thể mượn thêm sách nữa!"
}
```

---

## 💾 Database Structure

### Collection: `books`
```javascript
{
  "_id": ObjectId("..."),
  "title": "Java Programming",
  "author": "Robert C. Martin",
  "category": "Công nghệ thông tin",
  "isbn": "978-0134685991",
  "imageUrl": "/uploads/1712234567890_cover.jpg",
  "quantity": 5,              // Tổng số bản (legacy, có thể bỏ)
  "status": "BORROWED"        // AVAILABLE hoặc BORROWED
}
```

### Collection: `loans`
```javascript
{
  "_id": ObjectId("..."),
  "bookId": ObjectId("book_id"),
  "userId": ObjectId("user_id"),
  "borrowDate": ISODate("2026-04-04"),
  "dueDate": ISODate("2026-04-18"),
  "returnDate": null,              // null: chưa trả, Date: đã trả
  "status": "ACTIVE",              // ACTIVE hoặc RETURNED
  "lateFee": 0,
  "createdAt": ISODate("2026-04-04T13:23:00Z"),
  "notes": "Mượn sách từ thư viện"
}
```

### Collection: `users`
```javascript
{
  "_id": ObjectId("..."),
  "mssv": "2108110123",
  "fullName": "Nguyễn Văn A",
  "email": "21081001@student.hufi.edu.vn",
  "idCard": "LIB-2026-2108110123",
  "expiryDate": ISODate("2027-04-04"),  // Hạn thẻ
  "status": "ACTIVE",                    // ACTIVE hoặc INACTIVE
  "role": "USER"
}
```

---

## 🔍 Ví dụ Thực Tế

### Scenario 1: Mượn Sách Thành Công
```
User: Nguyễn Văn A (MSSV: 2108110123)
  - Thẻ: ACTIVE, hạn đến 2027-04-04 ✅
  - Đang mượn: 2 cuốn → <3 ✅
  
Book: "Clean Code" 
  - Status: AVAILABLE ✅
  
→ Kết quả: ✅ MƯỢN THÀNH CÔNG
  Hạn trả: 18/04/2026
```

### Scenario 2: Lỗi - Thẻ Hết Hạn
```
User: Trần Thị B (MSSV: 2108110124)
  - Thẻ: ACTIVE nhưng hạn đến 2026-02-01 ❌
  
Book: "Design Patterns"
  - Status: AVAILABLE ✅
  
→ Kết quả: ❌ CARD_EXPIRED
  "Thẻ độc giả của bạn đã hết hạn vào ngày: 2026-02-01. 
   Vui lòng gia hạn thẻ tại thư viện!"
```

### Scenario 3: Lỗi - Vượt Giới Hạn Mượn
```
User: Lê Văn C (MSSV: 2108110125)
  - Thẻ: ACTIVE, chưa hết hạn ✅
  - Đang mượn: 3 cuốn → =3 ❌
  
Book: "Spring in Action"
  - Status: AVAILABLE ✅
  
→ Kết quả: ❌ BORROW_LIMIT_EXCEEDED
  "Bạn đã mượn 3/3 cuốn. Không thể mượn thêm sách nữa!"
```

---

## 📝 Code Pattern: Clean Code

### Principles sử dụng:

1. **Single Responsibility Principle (SRP)**
   - `BorrowingService` chỉ xử lý logic mượn sách
   - Mỗi exception có một lý do rõ ràng
   
2. **Dependency Injection**
   - Sử dụng `@Autowired` để inject dependency
   - Dễ test và maintain
   
3. **Transactional Consistency**
   - `@Transactional` đảm bảo atomicity
   - Tất cả thay đổi database hoặc không có gì
   
4. **Custom Exception Hierarchy**
   - `BorrowingException` base class
   - Các exception cụ thể extends từ base
   - Có `errorCode` để client xử lý

5. **Detailed Comments**
   - Mỗi method có JavaDoc
   - Giải thích logic phức tạp
   - Ví dụ sử dụng

---

## 🧪 Testing Tips

### Unit Test cho BorrowingService:
```java
@Test
void testBorrowBook_Success() {
    // Given
    Book book = new Book();
    book.setStatus("AVAILABLE");
    
    User user = new User();
    user.setStatus("ACTIVE");
    user.setExpiryDate(LocalDate.now().plusYears(1));
    
    // When
    Loan loan = borrowingService.borrowBook(bookId, userId);
    
    // Then
    assertEquals("ACTIVE", loan.getStatus());
    assertEquals(LocalDate.now(), loan.getBorrowDate());
}

@Test
void testBorrowBook_CardExpired() {
    // Given
    User user = new User();
    user.setExpiryDate(LocalDate.now().minusDays(1)); // Hết hạn
    
    // When & Then
    assertThrows(CardExpiredException.class, () -> {
        borrowingService.borrowBook(bookId, userId);
    });
}
```

---

## 🎓 Tổng Kết

**Ưu điểm của hệ thống:**
- ✅ Logic kinh doanh rõ ràng
- ✅ Exception message chi tiết (thân thiện người dùng)
- ✅ Database consistency (@Transactional)
- ✅ Clean Code practices
- ✅ Dễ mở rộng thêm chức năng (trả sách quá hạn, phạt tiền, v.v.)

**Có thể mở rộng:**
- 📌 Tính tiền phạt quá hạn
- 📌 Gửi reminder email trước hạn trả
- 📌 Theo dõi lịch sử mượn/trả
- 📌 Đặt chỗ sách (Reservation)
- 📌 Tăng giới hạn mượn dựa trên loại thẻ

---

**Version:** 1.0  
**Last Updated:** 04/04/2026  
**Author:** AI Assistant  
