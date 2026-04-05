# 🧪 Hướng Dẫn Test - Nghiệp Vụ Mượn Sách (Appointment-Based Flow)

## ⚡ Quick Start (5 phút)

### **Bước 1: Enable Test Data**

Mở file: `src/main/resources/application-dev.properties`
```properties
app.test-endpoints.enabled=true
```

### **Bước 2: Chạy app với dev profile**

```bash
# PowerShell (Windows)
$env:SPRING_PROFILES_ACTIVE="dev"
./mvnw.cmd spring-boot:run

# Bash (macOS/Linux)
export SPRING_PROFILES_ACTIVE=dev
./mvnw spring-boot:run
```

### **Bước 3: Test data tự động được tạo**

```
✅ Created 3 test books
✅ Created user HET_HAN_THE (expired card)
✅ Created user QUA_HAN (overdue loan + fine)
✅ Ready to test!
```

---

## 📋 Flow Mượn Sách (Appointment-Based)

### **Quy Trình Mới:**

```
1️⃣ USER ĐẶT LỊCH
   ↓
   POST /borrowing/request-borrow
   - Chọn sách
   - Chọn appointmentTime (ngày + giờ lên thư viện)
   - Loan status = PENDING
   - Book quantity -1
   - dueDate = appointmentTime + 14 ngày
   
2️⃣ ADMIN XÁC NHẬN ĐÃ LẤY
   ↓
   POST /borrowing/{loanId}/pickup
   - Loan status = PENDING → PICKED_UP
   - borrowDate = hôm nay
   - dueDate vẫn giữ (từ appointmentTime)
   
3️⃣ USER TRẢ SÁCH
   ↓
   POST /borrowing/{loanId}/return
   - Loan status = PICKED_UP → RETURNED
   - returnDate = hôm nay
   - If returnDate > dueDate:
     - Tính phí = (days overdue) × quantity × 5.000₫
     - Tạo Fine (PENDING)
     - Suspend card (User.status = SUSPENDED)
   - Book quantity +1
```

---

## 🎯 Test Scenarios

### **Scenario 1: User Hết Hạn Thẻ**

**Dữ liệu test:**
```
MSSV: HET_HAN_THE
Mật khẩu: password123
Thẻ: LIB-2026-EXPIRED-001
Status: EXPIRED (5 ngày trước)
```

**Kiểm tra:**
1. Login: http://localhost:8080/login
2. Vào `/borrowing/borrow-form` → ⚠️ Cảnh báo "Thẻ hết hạn"
3. Không thể đặt lịch mượn (button disabled)
4. Vào `/admin/cards` → Thẻ status = EXPIRED (vàng)

---

### **Scenario 2: Đặt Lịch Mượn (PENDING)**

**Bước 1: User đặt lịch**
- Login: http://localhost:8080/login (USER bất kỳ)
- Vào `/borrowing/borrow-form`
- Chọn sách (vd: "Java Programming")
- Chọn appointmentTime: **05/04/2026 14:00**
- Nhấn "Đặt Lịch Mượn"
- ✅ Success: "Đặt lịch mượn 'Java Programming' thành công! Hẹn lấy: 05/04/2026 14:00 | Hạn trả: 19/04/2026"

**Kiểm tra Database:**
```javascript
db.loans.findOne()
// {
//   "status": "PENDING",      // Chờ nhận sách
//   "appointmentTime": ISODate("2026-04-05T14:00:00Z"),
//   "dueDate": ISODate("2026-04-19"),   // 14 ngày sau
//   "borrowDate": null,       // Chưa có
//   "returnDate": null
// }

db.books.findOne({title: "Java Programming"})
// {
//   "quantity": 2            // Đã -1 (từ 3)
// }
```

---

### **Scenario 3: Admin Xác Nhận Đã Lấy (PENDING → PICKED_UP)**

**Bước 1: Xem danh sách loans**
- Admin vào: `/admin/borrow-history`
- Thấy danh sách tất cả loans (PENDING, PICKED_UP, RETURNED, CANCELLED)
- Status: **"PENDING"** (chờ nhận)
- appointmentTime: **05/04/2026 14:00**

**Bước 2: Admin click "Đã Lấy Sách"**
- Click button **"Đã Lấy Sách"** trên dòng PENDING loan
- ✅ Status thay đổi: **PENDING → PICKED_UP**
- ✅ borrowDate được set = hôm nay (05/04/2026)
- dueDate vẫn = 19/04/2026

**Kiểm tra Database:**
```javascript
db.loans.findOne({status: "PICKED_UP"})
// {
//   "status": "PICKED_UP",
//   "borrowDate": ISODate("2026-04-05"),
//   "dueDate": ISODate("2026-04-19"),
//   "appointmentTime": ISODate("2026-04-05T14:00:00Z")
// }
```

---

### **Scenario 4: User Trả Sách (Đúng Hạn)**

**Bước 1: User trả sách**
- Login: User đã mượn sách
- Vào `/user/borrow-history`
- Thấy sách status = **"PICKED_UP"**
- Click "Trả Sách"
- ✅ Thành công: "Trả sách thành công!"

**Kiểm tra:**
```javascript
db.loans.findOne({status: "RETURNED"})
// {
//   "status": "RETURNED",
//   "returnDate": ISODate("2026-04-05"),
//   "lateFee": 0              // Không quá hạn
// }

db.books.findOne({title: "Java Programming"})
// {
//   "quantity": 3             // Cộng lại
// }
```

---

### **Scenario 5: User Trả Sách (QUÁ HẠN) ⚠️**

**Dữ liệu test:**
```
MSSV: QUA_HAN
appointmentTime: 22/10/2026 14:00 (thời gian tự chọn)
dueDate: 05/11/2026
returnDate: hôm nay (05/04/2026) → 152 ngày quá hạn!
```

**Bước 1: Admin đặt lịch mượn chiều đó (test)**
- Chạy curl hoặc tạo loan with **appointmentTime = 22/10/2026**
- dueDate sẽ là **05/11/2026**

**Bước 2: Hôm nay (05/04/2026) return book**
- POST `/user/{loanId}/return`
- ✅ Quá hạn: 152 ngày
- ✅ Phí phạt: **152 × 5.000 = 760.000₫**
- ✅ Tạo Fine (PENDING)
- ✅ Thẻ SUSPENDED

**Kiểm tra Database:**
```javascript
db.loans.findOne({status: "RETURNED"})
// {
//   "status": "RETURNED",
//   "returnDate": ISODate("2026-04-05"),
//   "lateFee": 760000         // Quá hạn 152 ngày
// }

db.fines.findOne({userId: "..."})
// {
//   "status": "PENDING",
//   "amount": 760000,
//   "reason": "Quá hạn trả sách: 152 ngày, mức phạt 5000 đ/cuốn/ngày"
// }

db.users.findOne({mssv: "QUA_HAN"})
// {
//   "status": "SUSPENDED"     // Bị khóa
// }
```

**Bước 3: Admin xác nhận thanh toán**
- Vào `/admin/cards` → Tìm user QUA_HAN
- Status = **SUSPENDED** (đỏ)
- Click "Xem chi tiết"
- Thấy phiếu phạt 760.000₫
- Click "Đã Thanh Toán"
- ✅ Fine status = PAID
- ✅ User status = ACTIVE (mở khóa)

---

### **Scenario 6: Admin Hủy Đặt Lịch (PENDING/PICKED_UP → CANCELLED)**

**Bước 1: User đặt lịch nhưng không đến**
- Loan status = PENDING
- Book quantity -1

**Bước 2: Admin hủy**
- Admin vào `/admin/borrow-history`
- Click "Hủy Đặt Lịch" trên PENDING loan
- Nhập lý do: "Người dùng không đến lên thư viện"
- ✅ Status = CANCELLED
- ✅ Book quantity +1 (hoàn lại)

**Kiểm tra:**
```javascript
db.loans.findOne({status: "CANCELLED"})
// {
//   "status": "CANCELLED",
//   "cancelReason": "Người dùng không đến lên thư viện"
// }

db.books.findOne({title: "Java Programming"})
// {
//   "quantity": 3             // Hoàn lại
// }
```

---

### **Scenario 7: Max 3 Cuốn Mượn**

**Kiểm tra giới hạn:**

**Bước 1: User thứ 1 - Đặt 3 cuốn**
- Đặt lịch: Java Programming (PENDING)
- Đặt lịch: Spring in Action (PENDING)
- Đặt lịch: Clean Code (PENDING)
- ✅ Tất cả thành công

**Bước 2: Thứ 4 thì bị lỗi**
- Cố đặt lịch cuốn thứ 4
- ❌ Error: "Bạn đã mượn tối đa 3 cuốn!"

**Quy tắc:**
- Count loans with status = PENDING hoặc PICKED_UP
- Nếu totalActive ≥ 3 → Không cho đặt thêm

---

## 🔄 Trạng Thái Loan Chi Tiết

| Status | Ý Nghĩa | Có thể trả? | Có thể hủy? |
|--------|---------|------------|-----------|
| **PENDING** | Chờ nhận sách tại thư viện | ❌ Không | ✅ Có |
| **PICKED_UP** | Admin xác nhận đã lấy sách | ✅ Có | ⚠️ Có (nếu chưa quá hạn) |
| **RETURNED** | Đã trả sách | ❌ Không được | ❌ Không |
| **CANCELLED** | Hủy đặt lịch | ❌ Không | ❌ Không |

---

## 💳 Quản Lý Thẻ Độc Giả

### **Trạng Thái Thẻ:**
- **ACTIVE**: Hoạt động, có thể mượn sách
- **EXPIRED**: Hết hạn, không thể mượn. Cần gia hạn tại `/admin/cards`
- **SUSPENDED**: Bị khóa do có phiếu phạt chưa thanh toán. Admin phải xác nhận thanh toán để mở khóa

### **Gia Hạn Thẻ:**
- Thêm 365 ngày
- Endpoint: `POST /admin/cards/{userId}/renew`

### **Xác Nhận Thanh Toán Phạt:**
- Admin vào `/admin/cards/{userId}`
- Thấy danh sách phiếu phạt (nếu có PENDING)
- Click "Đã Thanh Toán" trên phiếu
- ✅ Fine status = PAID
- ✅ Nếu tất cả fines PAID → User status = ACTIVE

---

## 🧪 Manual Test với API (Optional)

Nếu muốn test bypass UI:

### **Create test user:**
```bash
POST /api/test/user-expire?days=0
# Response: user với thẻ EXPIRED
```

### **Create overdue loan:**
```bash
POST /api/test/overdue-loan?mssv=TEST123&bookId=book1&days=7
# Response: Loan PENDING với appointmentTime đã set
```

---

## ⚠️ Lưu Ý Quan Trọng

| Điểm | Chi tiết |
|-----|---------|
| **borrowDate** | ❌ NOT set khi PENDING. Chỉ set khi PICKED_UP (admin click) |
| **dueDate** | ✅ Set luôn = appointmentTime + 14 ngày (PENDING lúc) |
| **Phí muộn** | Tính từ **dueDate**, không phải borrowDate |
| **Max loans** | Tính PENDING + PICKED_UP, không bao gồm RETURNED/CANCELLED |
| **Thẻ bị khóa** | Khi có Fine PENDING, tự động SUSPENDED |

---

## 📊 Database Collections

### **loans**
- appointmentTime (LocalDateTime): thời gian user chọn lên nhận
- borrowDate (LocalDate): null → set khi admin click "Đã lấy"
- dueDate (LocalDate): = appointmentTime + 14 ngày
- status (LoanStatus): PENDING → PICKED_UP → RETURNED
- cancelReason (String): nếu status = CANCELLED

### **users**
- status: ACTIVE / EXPIRED / SUSPENDED
- expiryDate (LocalDate): hạn dùng thẻ

### **fines**
- status: PENDING → PAID
- amount: 5.000₫/cuốn/ngày
- createdDate: lúc return book quá hạn

---

## 🔒 Production Safety

**Test endpoints CHỈ hoạt động khi:**
1. `app.test-endpoints.enabled=true` 
2. Profile = `dev`

**Production (default):**
- `app.test-endpoints.enabled=false`
- Test endpoints DISABLED
- ✅ An toàn 100%

---

**Updated:** April 5, 2026  
**Version:** 2.0 (Appointment-Based Booking Flow)

**Thay đổi chính từ v1.0 → v2.0:**
- ✅ User đặt lịch (PENDING) thay vì mượn ngay
- ✅ appointmentTime + dueDate được tính chính xác
- ✅ Admin xác nhận "Đã lấy" (PICKED_UP)
- ✅ Max 3 cuốn (PENDING + PICKED_UP)
- ✅ Hỗ trợ cancel with reason
- ✅ LoanStatus enum cho 4 trạng thái


## ⚡ Quick Start (2 phút)

### **Bước 1: Enable Test Data trong application-dev.properties**

Mở file: `src/main/resources/application-dev.properties`

Kiểm tra đã có dòng này chưa:
```properties
app.test-endpoints.enabled=true
```

Nếu chưa → Thêm vào. Nếu có → OK ✅

### **Bước 2: Chạy ứng dụng với dev profile**

```bash
# PowerShell (Windows)
$env:SPRING_PROFILES_ACTIVE="dev"
./mvnw.cmd spring-boot:run

# Bash (macOS/Linux)
export SPRING_PROFILES_ACTIVE=dev
./mvnw spring-boot:run
```

### **Bước 3: Test data tự động được tạo**

Khi ứng dụng startup, sẽ tự động insert vào MongoDB:

📌 **TestDataInitializer chạy tự động (dev profile + app.test-endpoints.enabled=true):**
```
✅ Created test books (Java Programming, Spring in Action, Clean Code)
✅ Created user (Expired Card): MSSV=HET_HAN_THE, Thẻ hết hạn
✅ Created user (Overdue Book): MSSV=QUA_HAN, Mượn sách quá hạn 7 ngày
```

**Không cần gọi API, dữ liệu đã có sẵn!** 🎉

⚠️ **Lưu ý:**
- TestDataInitializer **chỉ chạy** khi cả 2 điều kiện true:
  1. Profile = `dev`
  2. Property `app.test-endpoints.enabled=true`
- Nếu chạy **không có** dev profile → Không tạo test data (production mode)
- Nếu muốn disable test data: Set `app.test-endpoints.enabled=false` hoặc xóa property

---

## 📋 Test Data Tự Động Tạo

### **1️⃣ User - Thẻ Hết Hạn**

**Để test: Không thể mượn sách + Hiển thị cảnh báo**

```
MSSV: HET_HAN_THE
Mật khẩu: password123
Thẻ ID: LIB-2026-EXPIRED-001
Trạng thái: EXPIRED (hết hạn từ 5 ngày trước)
Email: expired-card@test.com
```

**Kiểm tra:**
1. Login: http://localhost:8080/login
2. Username: `HET_HAN_THE`, Password: `password123`
3. Vào `/` → Sẽ hiển thị: **"⚠️ Thẻ độc giả hết hạn"**

---

### **2️⃣ User - Mượn Sách Quá Hạn**

**Để test: Tính phí phạt (35.000₫) + Khóa thẻ**

```
MSSV: QUA_HAN
Mật khẩu: password123
Thẻ ID: LIB-2026-OVERDUE-001
Trạng thái: ACTIVE (thẻ còn hạn)
Sách mượn: Java Programming
Mượn: 21 ngày trước
Hạn: 7 ngày trước
Quá hạn: 7 ngày
Phí dự tính: 35.000 VND
Email: overdue-book@test.com
```

**Kiểm tra - Trả sách quá hạn:**
1. Login: http://localhost:8080/login
2. Username: `QUA_HAN`, Password: `password123`
3. Vào `/user/borrow-history` → Thấy 1 sách đang mượn (quá hạn)
4. Xem chi tiết → Loanid sẽ được hiển thị
5. **Admin trả sách (hoặc dùng API):**
   ```bash
   curl -X POST "http://localhost:8080/api/test/return-overdue/{loanId}"
   ```
   → Tự động tạo Fine + SUSPEND card

---

### **3️⃣ Các Cuốn Sách Test**

- **Java Programming** - Herbert Schildt (Có sẵn 3 cuốn)
- **Spring in Action** - Craig Walls (Có sẵn 2 cuốn)
- **Clean Code** - Robert C. Martin (Có sẵn 1 cuốn)

---

## 📋 Manual Test Endpoints (Tùy Chọn)

Nếu muốn tạo thêm test data hoặc cần data khác, vẫn có sẵn test endpoints:

### **Endpoint 1: Tạo User thẻ hết hạn**
```bash
POST /api/test/user-expire?days=0
```

### **Endpoint 2: Tạo Loan quá hạn**
```bash
POST /api/test/overdue-loan?mssv=MSSV&bookId=BookID&days=5
```

### **Endpoint 3: Trả sách quá hạn**
```bash
POST /api/test/return-overdue/{loanId}
```

---

## 🎯 Scenarios Test Sẵn Có

### **1️⃣ Tạo User với Thẻ Hết Hạn / Sắp Hết Hạn**

**Endpoint:**
```
POST /api/test/user-expire?days=<SỐ_NGÀY>
```

**Tham số:**
| Tham số | Giá trị | Kết quả |
|--------|--------|--------|
| days=0 | Hôm nay | Thẻ EXPIRED (hết hạn ngay) |
| days=-5 | 5 ngày trước | Thẻ EXPIRED (đã qua hạn) |
| days=10 | 10 ngày sau | Thẻ ACTIVE (sắp hết hạn) |
| days=30 | 30 ngày sau | Thẻ ACTIVE (bình thường) |

**Ví dụ Curl:**

```bash
# Tạo user với thẻ hết hạn hôm nay
curl -X POST "http://localhost:8080/api/test/user-expire?days=0"

# Tạo user với thẻ hết hạn 5 ngày trước
curl -X POST "http://localhost:8080/api/test/user-expire?days=-5"

# Tạo user với thẻ sắp hết hạn (10 ngày nữa)
curl -X POST "http://localhost:8080/api/test/user-expire?days=10"
```

**Response ✅:**
```json
{
  "success": true,
  "message": "✅ User tạo thành công! Thẻ EXPIRED",
  "userId": "60a2c3b4e5f6g7h8i9j0k1l2",
  "mssv": "TEST1709624000000",
  "idCard": "LIB-2026-TEST1709624000000",
  "expiryDate": "2026-04-05",
  "status": "EXPIRED",
  "note": "Trạng thái: HẾT HẠN"
}
```

**💡 Lưu ý:** 
- Mỗi call tạo user mới (MSSV = `TEST<timestamp>`)
- Lấy `mssv` từ response để dùng ở bước 2
- Lấy `idCard` nếu cần test login

---

### **2️⃣ Tạo Loan Quá Hạn (Để Test Tính Phí Phạt)**

**Endpoint:**
```
POST /api/test/overdue-loan?mssv=<MSSV>&bookId=<ID_SÁCH>&days=<NGÀY_QUÁ_HẠN>
```

**Tham số:**
- `mssv`: MSSV của user (lấy từ bước 1, hoặc user hiện tại)
- `bookId`: ID của cuốn sách (ID thực từ DB)
- `days`: Số ngày quá hạn (mặc định = 5)

**Ví dụ Curl:**

```bash
# Lấy ID sách từ trang chủ hoặc MongoDB
# Giả sử: mssv=TEST1709624000000, bookId=book123

# Tạo loan quá hạn 5 ngày
curl -X POST "http://localhost:8080/api/test/overdue-loan?mssv=TEST1709624000000&bookId=book123&days=5"

# Tạo loan quá hạn 7 ngày
curl -X POST "http://localhost:8080/api/test/overdue-loan?mssv=TEST1709624000000&bookId=book123&days=7"

# Tạo loan quá hạn 14 ngày (phạt lớn)
curl -X POST "http://localhost:8080/api/test/overdue-loan?mssv=TEST1709624000000&bookId=book123&days=14"
```

**Response ✅:**
```json
{
  "success": true,
  "message": "✅ Loan quá hạn tạo thành công!",
  "loanId": "60a2c3b4e5f6g7h8i9j0k1l3",
  "userId": "60a2c3b4e5f6g7h8i9j0k1l2",
  "bookId": "book123",
  "borrowDate": "2026-03-21",
  "dueDate": "2026-04-04",
  "daysOverdue": 5,
  "estimatedFine": "25000 VND (5.000/ngày)",
  "nextStep": "POST /api/test/return-overdue/60a2c3b4e5f6g7h8i9j0k1l3"
}
```

**💡 Cách tính ngày:**
- Nếu `days=5` (quá hạn 5 ngày)
- Return ngày = Hôm nay
- Due ngày = Hôm nay - 5 ngày
- Borrow ngày = Due ngày - 14 ngày (hạn mặc định 14 ngày)

**💰 Phí phạt:** `5.000₫ × số ngày quá hạn × số lượng`

---

### **3️⃣ Trả Sách Quá Hạn → Tự Động Tính Phí + Khóa Thẻ**

**Endpoint:**
```
POST /api/test/return-overdue/{loanId}
```

**Tham số:**
- `loanId`: ID của loan (lấy từ bước 2)

**Ví dụ Curl:**

```bash
# Trả sách quá hạn (tính phí + khóa thẻ tự động)
curl -X POST "http://localhost:8080/api/test/return-overdue/60a2c3b4e5f6g7h8i9j0k1l3"
```

**Response ✅:**
```json
{
  "success": true,
  "message": "✅ Trả sách thành công!",
  "loanId": "60a2c3b4e5f6g7h8i9j0k1l3",
  "status": "RETURNED",
  "returnDate": "2026-04-05",
  "lateFee": "25000 VND",
  "userCardStatus": "SUSPENDED",
  "note": "⚠️ Thẻ độc giả đã bị KHÓA do vi phạm. Admin phải xác nhận thanh toán phí.",
  "nextStep": "Truy cập /admin/cards để xác nhận thanh toán"
}
```

**Những gì xảy ra tự động:**
- ✅ Tạo **Fine record** với `status = PENDING`
- ✅ Cập nhật Loan: `status = RETURNED`, `returnDate = hôm nay`
- ✅ Tính phí: `25.000 VND` (5 ngày × 5.000₫/ngày)
- ✅ Khóa thẻ: `User.status = SUSPENDED`

---

## 🎯 Các Scenario Test

## 🎯 Các Scenario Test

## 🎯 Scenarios Test Sẵn Có

### **Scenario 1: User Hết Hạn Thẻ - Không Thể Mượn Sách**

**Mục đích:** Test xem user thẻ hết hạn có thể mượn sách không

**Dữ liệu test:** 
```
MSSV: HET_HAN_THE
Mật khẩu: password123
```

**Bước test:**
1. Login: http://localhost:8080/login (HET_HAN_THE / password123)
2. Vào trang chủ → ⚠️ Thấy cảnh báo "Thẻ độc giả hết hạn"
3. Cố mượn sách → ❌ Sẽ báo lỗi "Thẻ đã hết hạn"
4. Vào `/admin/cards` (Admin) → Thẻ status = **EXPIRED** (nền vàng)

---

### **Scenario 2: Quá Hạn Trả Sách - Tính Phí + Khóa Thẻ**

**Mục đích:** Test toàn bộ quy trình phí phạt

**Dữ liệu test:**
```
MSSV: QUA_HAN
Mật khẩu: password123
Sách: Java Programming
Mượn: 21 ngày trước
Hạn: 7 ngày trước (quá 7 ngày)
Phí dự tính: 35.000 VND
```

**Bước test:**

**Bước 1: Xem loan quá hạn**
- Login: QUA_HAN / password123
- Vào `/user/borrow-history` → Thấy 1 sách đang mượn (quá hạn)
- Click "Chi tiết" → Lấy **Loan ID**

**Bước 2: Admin trả sách quá hạn**
- Dùng API hoặc form trả sách
- Chạy: `curl -X POST "http://localhost:8080/api/test/return-overdue/{LoanID}"`

**Bước 3: Kiểm tra kết quả**
- ✅ Tạo Fine record (PENDING, 35.000 VND)
- ✅ Suspend card: User status = **SUSPENDED**
- ✅ Loan status = **RETURNED**

**Bước 4: Admin xác nhận thanh toán**
- Vào `/admin/cards` → Tìm user QUA_HAN
- Status = **SUSPENDED** (nền đỏ)
- Click "Xem chi tiết" → Thấy phiếu phạt 35.000 VND
- Click "Đã Thanh Toán"
- ✅ Fine status = **PAID**
- ✅ User status = **ACTIVE** (mở khóa)

---

### **Scenario 3: User Bình Thường - Mượn & Trả Sách**

**Mục đích:** Test chức năng bình thường

**Cách làm:**
1. Đăng ký tài khoản mới
2. Vào `/borrowing/borrow-form`
3. Chọn sách (Java Programming, Spring in Action, Clean Code)
4. Trả sách trong 14 ngày
5. ✅ Không có phí phạt

---

## 🛠️ Cấu Trúc Code

### **TestDataInitializer.java** (Auto-generate)
```
- Chạy tự động khi startup (dev profile)
- Tạo 3 cuốn sách test
- Tạo user HET_HAN_THE (thẻ hết hạn)
- Tạo user QUA_HAN (loan quá hạn 7 ngày)
```

### **TestDataService.java** (Business Logic)
```
- createUserWithExpiredCard(days)
- createOverdueLoan(mssv, bookId, daysOverdue)
- returnOverdueLoan(loanId)
```

### **TestDataController.java** (HTTP API)
```
- POST /api/test/user-expire?days=N
- POST /api/test/overdue-loan?mssv=X&bookId=Y&days=Z
- POST /api/test/return-overdue/{loanId}
```

---

## 🔒 Bảo Mật

## 🔒 Bảo Mật & Production

**⚠️ QUAN TRỌNG:**
- Test endpoints & TestDataInitializer **CHỈ** hoạt động khi `app.test-endpoints.enabled=true`
- **KHÔNG BAO GIỜ** để `enabled=true` ở production
- Mặc định đã tắt (`enabled=false`) trong `application.properties` (bảo vệ production)
- Chỉ bật trong `application-dev.properties` (dev mode)

**Cách quản lý:**

| Scenario | Cách Làm | Kết Quả |
|----------|---------|--------|
| **Test với test data** | `SPRING_PROFILES_ACTIVE=dev` + `app.test-endpoints.enabled=true` | ✅ Auto-generate test data on startup |
| **Chạy bình thường (sạch)** | `SPRING_PROFILES_ACTIVE=dev` + `app.test-endpoints.enabled=false` | ✅ No auto test data |
| **Production mode** | Không set dev profile + `app.test-endpoints.enabled=false` (default) | ✅ Hoàn toàn sạch & an toàn |

**Kiểm tra nhanh:**
```bash
# Test nếu endpoints hoạt động hay không
curl http://localhost:8080/api/test/user-expire?days=0
```

**Kết quả nếu production (đúng):**
```json
{
  "error": "Not Found",
  "status": 404
}
```

**Kết quả nếu dev mode enabled (đúng):**
```json
{
  "success": true,
  "message": "...",
  ...
}
```

---

## 📚 Tài Liệu Liên Quan

| File | Mục đích |
|------|---------|
| [TestDataService.java](src/main/java/library_management/services/TestDataService.java) | Business logic tạo test data |
| [TestDataController.java](src/main/java/library_management/controllers/TestDataController.java) | HTTP endpoints |
| [BorrowingService.java](src/main/java/library_management/services/BorrowingService.java) | Logic mượn/trả + tính phí |
| [CardController.java](src/main/java/library_management/controllers/CardController.java) | Admin xác nhận thanh toán |
| [application.properties](src/main/resources/application.properties) | Config (mặc định disabled) |
| [application-dev.properties](src/main/resources/application-dev.properties) | Config dev profile (enabled) |

---

## 💡 Mẹo & Troubleshooting

### **Q: Test endpoint không hoạt động?**
A: Kiểm tra:
1. Đang chạy với profile `dev` không? `$env:SPRING_PROFILES_ACTIVE="dev"`
2. Property `app.test-endpoints.enabled=true` đã được set trong `application-dev.properties`?
3. Kiểm tra log: tìm message `ConditionalOnProperty` hoặc `Test endpoints enabled`
4. Verify endpoint: curl http://localhost:8080/api/test/user-expire?days=0

### **Q: Test data không auto-generate khi startup?**
A: 
1. Kiểm tra `application-dev.properties` có `app.test-endpoints.enabled=true` không?
2. Kiểm tra chạy với dev profile không? `$env:SPRING_PROFILES_ACTIVE="dev"`
3. Xem logs: tìm `TestDataInitializer` message
4. Lưu ý: TestDataInitializer **chỉ tạo** nếu user test chưa tồn tại (không duplicate)

### **Q: Không tìm thấy book/user?**
A:
1. Tạo sách trước: http://localhost:8080/admin/add-book
2. Copy bookId từ danh sách hoặc MongoDB: `db.books.find()`
3. Hoặc dùng MSSV user hiện tại (2108110123 nếu đã đăng ký)

### **Q: Phí phạt không đúng?**
A: Công thức:
```
Phí = Số ngày quá hạn × 5.000₫/ngày × Số lượng sách
```

Ví dụ: 7 ngày × 5.000 × 1 cuốn = 35.000₫

### **Q: Muốn test lại từ đầu?**
A: Xoá collections MongoDB:
```javascript
db.loans.deleteMany({})
db.fines.deleteMany({})
db.users.deleteMany({})
```

Hoặc chỉ xoá user test:
```javascript
db.users.deleteMany({ mssv: { $regex: "^TEST" } })
```

---

## 🎓 Quy Trình Nghiệp Vụ (Business Logic)

### **Mượn Sách:**
```
User request → Validate card (ACTIVE?) → Check quantity → Create Loan → Reduce stock
```

### **Trả Sách Quá Hạn:**
```
Return request 
  → Check if overdue
  → If YES:
    - Calculate lateFee = days × quantity × 5.000
    - Create Fine(PENDING)
    - Suspend card (status=SUSPENDED)
  → Update Loan(RETURNED)
  → Increase stock
```

### **Admin Xác Nhận Thanh Toán:**
```
Confirm payment
  → Check pending fines
  → If YES: Fine(PENDING) → Fine(PAID)
  → If all fines PAID && card not expired: 
    - Card(SUSPENDED) → Card(ACTIVE)
```

---

**Cập nhật:** April 5, 2026  
**Version:** 1.1 (Refactored TestDataInitializer - Require explicit enabled property)

**Thay đổi mới nhất:**
- ✅ TestDataInitializer giờ **không chạy tự động** nếu property không được set
- ✅ Thêm `matchIfMissing=false` để bảo vệ production
- ✅ Cần set `app.test-endpoints.enabled=true` trong `application-dev.properties`
- ✅ Tách TestDataService & TestDataController cho module hóa tốt hơn


