# 🔨 MiniBoom - Online Auction System (Hệ Thống Đấu Giá Trực Tuyến Thời Gian Thực)

![Java](https://img.shields.io/badge/Java-25-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-21-007396?style=for-the-badge&logo=java&logoColor=white)
![SQLite](https://img.shields.io/badge/SQLite-WAL_Mode-003B57?style=for-the-badge&logo=sqlite&logoColor=white)
![TCP/IP](https://img.shields.io/badge/Network-TCP_Socket-00599C?style=for-the-badge&logo=cisco&logoColor=white)

> **Bài tập lớn môn Lập trình nâng cao (LTNC) - Trường Đại học Công nghệ (UET) - ĐHQGHN**
>
> MiniBoom là dự án bài tập lớn môn Lập trình nâng cao mô phỏng một nền tảng đấu giá trực tuyến thời gian thực theo kiến trúc Desktop Client-Server. Hệ thống cho phép nhiều Client JavaFX giao tiếp đồng thời tới một TCP Server để thực hiện đăng nhập, đăng ký, quản lý ví điện tử, tạo phiên đấu giá, đặt thầu tự động (Auto-bid), chống bắn tỉa phút chót (Anti-snipe) và cập nhật biến động giá realtime.
>
> Dự án được xây dựng với mục tiêu phục vụ bài tập lớn và chạy thử nghiệm (demo) học phần dưới mô hình đa module (Multi-Module Maven), đảm bảo giải quyết tối ưu các bài toán về xử lý mạng bất đồng bộ, luồng dữ liệu thời gian thực và tranh chấp đồng thời (concurrency) ở tầng cơ sở dữ liệu thông qua sức mạnh của SQLite chế độ WAL (Write-Ahead Logging).
>
> Repository: [https://github.com/Trumkhungbo/baitaplon](https://github.com/Trumkhungbo/baitaplon)

---

## 1. Phạm Vi Hệ Thống

MiniBoom tập trung mô phỏng luồng nghiệp vụ đấu giá nội bộ với sự phân quyền rõ ràng và vòng đời khép kín:

### A. Phân Quyền Vai Trò Rõ Ràng (RBAC)
Hệ thống sử dụng enum `UserRole` để định tuyến 3 quyền hạn chuyên biệt:
* **BIDDER (Người Đấu Giá)**: Xem danh sách phòng đấu giá, tham gia trả giá thủ công (Manual Bid) hoặc thiết lập ủy quyền cho động cơ đấu giá tự động (Auto-Bid). Thực hiện thao tác thanh toán khi thắng thầu.
* **SELLER (Người Bán)**: Đăng tải sản phẩm (thuộc đa dạng chủng loại như Art, Electronics, Vehicle), thiết lập giá khởi điểm (Starting Price) và thời lượng đếm ngược. Người bán bị khóa chức năng tự trả giá cho chính sản phẩm của mình để đảm bảo minh bạch.
* **ADMIN (Quản Trị Viên)**: Quản lý tính hợp lệ của nền tảng, có quyền kiểm duyệt và phê duyệt (Approve) các phiên đấu giá đang ở trạng thái chờ (`PENDING`) để đẩy lên sàn giao dịch chính thức.

### B. Vòng Đời Phiên Đấu Giá & Cơ Chế Thanh Toán Hậu Kiểm (Post-Auction Checkout Engine)
Khác với mô hình giam tiền cứng nhắc, MiniBoom vận hành theo mô hình đấu giá eBay mở, tối ưu hóa trải nghiệm cạnh tranh:
* **Vòng đời trạng thái nghiêm ngặt**: Phiên đấu giá chuyển dịch tuần tự qua các bước: `PENDING` -> `OPEN` -> `RUNNING` -> `FINISHED` -> `PAID` / `CANCELED`.
* **Thanh toán nguyên tử (Atomic Payment)**: Khi phiên đếm ngược kết thúc (`FINISHED`), người thắng cuộc thực hiện gọi luồng thanh toán. Giao dịch kiểm tra chéo số dư ví: trừ tiền người mua, cộng tiền người bán và sinh lịch sử giao dịch trong cùng một tích tắc.
* **Bảo vệ rủi ro**: Nếu tài khoản người thắng không đủ số dư tại thời điểm thanh toán, hệ thống tự động hủy kết quả và chuyển phiên sang trạng thái `CANCELED`.

---

## 2. Công Nghệ & Môi Trường Hoạt Động

Dự án ứng dụng các công nghệ tiêu chuẩn và mạnh mẽ nhất trong hệ sinh thái Java Desktop, nhằm đạt được hiệu năng tối đa, xử lý đồng thời an toàn và mang lại trải nghiệm người dùng cao cấp:

| Nhóm Công Nghệ | Công Nghệ Áp Dụng |
| :--- | :--- |
| **Ngôn Ngữ** | **Java 25** (Phiên bản cực kỳ hiện đại, khai thác tối đa hiệu năng và quản lý luồng) |
| **Build System** | **Maven Multi-Module Project** (Kiến trúc chuẩn doanh nghiệp, tích hợp `maven-shade-plugin`) |
| **Client UI** | **JavaFX 21.0.1, FXML** (Giao diện Dark Mode sang trọng, mượt mà) |
| **Server Engine** | **TCP Socket đa luồng** (Multi-threading), kết hợp kiến trúc điều hướng Command Pattern |
| **Database** | **SQLite** (Kích hoạt chế độ **WAL - Write-Ahead Logging** tối ưu hóa tranh chấp I/O) |
| **Security** | **PBKDF2 Password Hashing** (Bảo mật mật khẩu thông qua thuật toán băm kèm Salt) |
| **Serialization**| **Gson** (Đóng gói và giải nén luồng dữ liệu mạng định dạng JSON siêu tốc) |
| **Testing** | **JUnit 5 & Mockito** (Unit/Integration Test), **TestFX** (Test tự động UI), **JaCoCo** (Code Coverage) |
| **Logging** | **SLF4J Facade & Console I/O** (Theo dõi và truy vết nhật ký hoạt động hệ thống) |
| **CI/CD Pipeline**| **GitHub Actions** (Quy trình tự động hóa kiểm thử và tích hợp liên tục) |

### Yêu Cầu Cài Đặt

* **Java Development Kit**: JDK 25 (Đảm bảo lệnh `java -version` hoạt động).
* **Apache Maven**: Phiên bản 3.8+.
* **Hệ Quản Trị CSDL**: **KHÔNG YÊU CẦU CÀI ĐẶT!** (Hệ thống sẽ tự động khởi tạo tệp tin `auction.db` và sinh toàn bộ bảng dữ liệu ngay trong lần chạy Server đầu tiên).
* **Hệ Điều Hành**: Windows, macOS hoặc Linux desktop (Yêu cầu có môi trường đồ họa để hiển thị giao diện Client JavaFX).

---

## 3. Cấu Trúc Thư Mục Dự Án Chi Tiết

Dự án được phân chia thành 3 module độc lập, tuân thủ chặt chẽ nguyên lý Clean Architecture:

```text
baitaplon/
├── pom.xml              (Quản lý dependencies tổng, cấu hình Java 25)
├── .github/workflows/   (Cấu hình CI/CD GitHub Actions)
├── data/                (Chứa `auction.db` tự động sinh ra và ảnh sản phẩm)
├── docs/                (Lưu trữ tài liệu báo cáo)
├── logs/                (Nhật ký hoạt động của máy chủ)
├── common/              (Module CHUNG: dùng cho cả Server và Client)
│   └── src/main/java/com/bidding/common/
│       ├── enums/       (AuctionStatus, DepositStatus, ItemType, UserRole)
│       ├── model/       (Item, User, Auction, AutoBid, BidTransaction, Entity)
│       └── payload/     (Các lớp DTO định dạng gói tin Request/Response mạng)
├── server/              (Module SERVER: Xử lý TCP, Đa luồng, Database)
│   └── src/main/java/com/bidding/server/
│       ├── core/        (Nghiệp vụ tách biệt: AuctionService, AuthService, PaymentService...)
│       ├── database/    (Cấu hình SQLite WAL Mode & DatabaseInitializer)
│       ├── exception/   (Các lớp ngoại lệ tùy chỉnh)
│       ├── network/     (Lắng nghe Socket, CommandDispatcher, BroadcastService)
│       ├── repository/  (Tầng DAO truy xuất DB: AuctionDAO, TransactionDAO...)
│       └── ServerApplication.java (Gốc khởi chạy Server)
└── client/              (Module CLIENT: Giao diện JavaFX 21 MVC)
    ├── src/main/java/action/
    │   ├── controller/  (Bộ điều khiển phân rã theo luồng: admin, auth, main, payment, SellingJobs)
    │   ├── Core/        (Điều phối cảnh SceneSwitch, StartScence và gốc khởi chạy)
    │   ├── model/       (Lớp mô hình UI cục bộ như AuctionCardItem, UserHolder...)
    │   └── network/     (Luồng mạng nền bất đồng bộ: CentralReceiver, SocketClient, SocketListener)
    └── src/main/resources/
        ├── assets/      (Tài nguyên tĩnh: hình ảnh, icon)
        └── views/       (Toàn bộ file thiết kế giao diện .fxml, app.css, global.css)
```

---

## 4. Vị Trí Các File .jar

Di chuyển vào thư mục gốc của dự án và chạy lệnh đóng gói của Maven:

```bash
mvn clean install
```

Sau khi quá trình biên dịch và kiểm thử thành công, plugin `maven-shade-plugin` sẽ tự động đóng gói gộp mọi thư viện (dependencies) cần thiết, tạo ra các tệp tin executable JAR độc lập (Fat JAR) nằm tại các đường dẫn sau:

| Artifact | Vị Trí File JAR | Lệnh Chạy Thực Thi |
| :--- | :--- | :--- |
| **Server Executable JAR** | `server/target/server-1.0-SNAPSHOT.jar` | `java -jar server/target/server-1.0-SNAPSHOT.jar` |
| **Client Executable JAR** | `client/target/client-1.0-SNAPSHOT.jar` | `java -jar client/target/client-1.0-SNAPSHOT.jar` |

---

## 5. Hướng Dẫn Khởi Chạy Hệ Thống & Cấu Hình

### A. Yêu Cầu Môi Trường Cực Tiểu
Trước khi khởi chạy ứng dụng phân phối, máy tính vận hành cần cài đặt sẵn:
* **Java Development Kit (JDK) 25:** Đảm bảo khi gõ lệnh `java -version` trong Terminal/Command Prompt hiển thị chính xác phiên bản 25.
* **Hệ Điều Hành:** Windows, macOS, hoặc Linux Desktop hỗ trợ môi trường đồ họa hiển thị để render giao diện đồ họa JavaFX Client.

### B. Cơ Chế Tự Động Khởi Tạo Cơ Sở Dữ Liệu
Hệ thống MiniBoom được tích hợp bộ công cụ tự động cung cấp dữ liệu mang lại đặc quyền **Zero-Setup**:
* **Không cần cài đặt CSDL bên ngoài**: Bạn không cần cài MySQL, XAMPP hay Docker Server.
* **Tự động sinh cấu trúc dữ liệu**: Ngay khi Server khởi chạy lần đầu tiên, file dữ liệu cục bộ `auction.db` sẽ tự động sinh ra trong thư mục hệ thống kèm cấu trúc hoàn chỉnh của 8 bảng quan hệ.
* **Tự động nạp tài khoản kiểm thử mặc định**: Hệ thống tự băm mật khẩu chuẩn PBKDF2 và nạp sẵn các tài khoản thử nghiệm vào DB bao gồm:
   * **Tài khoản Admin:** `admin` / Mật khẩu: `admin123`
   * **Tài khoản Seller:** Có sẵn các tài khoản từ `seller1`, `seller2`, `seller3`, `seller4` / Mật khẩu chung: `seller123`

### C. Quy Trình Khởi Chạy Server/Client Theo Thứ Tự Cụ Thể
Hệ thống yêu cầu chạy Server trước để mở cổng kết nối, sau đó mới khởi chạy ứng dụng Client của người dùng.

* **Bước 1: Khởi chạy Máy chủ (MiniBoom Server)**
  Mở Terminal tại thư mục chứa file JAR của Server và thực thi câu lệnh:

  ```bash
  java -jar server-1.0-SNAPSHOT.jar
  ```

  *Xác nhận trạng thái:* Màn hình Console hiển thị dòng thông báo thành công:
  `[DB] Schema khoi tao thanh cong.`
  `AuctionServer is running on port 888`

* **Bước 2: Khởi chạy Giao diện Người dùng (MiniBoom Client)**
  Mở một cửa sổ Terminal **mới** song song với Server và chạy lệnh:

  ```bash
  java -jar client-1.0-SNAPSHOT.jar
  ```

> [!TIP]
> **Mở nhiều Client song song:** Để kiểm thử luồng đặt giá thầu, quản lý ví và tính năng Auto-bid cạnh tranh thời gian thực giữa nhiều tài khoản khác nhau, bạn chỉ cần mở thêm các Terminal mới độc lập và gõ lại chính xác câu lệnh chạy Client ở Bước 2.

### D. Xử Lý Sự Cố Thường Gặp (Troubleshooting)
**Lỗi `Address already in use: bind` khi chạy Server:**
* *Nguyên nhân:* Cổng kết nối mặc định `888` của TCP Server đã bị chiếm dụng bởi một tiến trình chạy ngầm trước đó chưa được giải phóng hoàn toàn.
* *Khắc phục:* Tắt tất cả các tab console chạy Server cũ hoặc thực hiện khởi động lại máy tính để giải phóng cổng mạng.

---

## 6. Danh Sách Chức Năng Đã Hoàn Thành

Dự án đã hoàn thiện nhiều chức năng cơ bản và tích hợp thêm các chức năng nâng cao:

### 6.1. Quản Lý Tài Khoản & Phân Quyền (RBAC)
- [x] Đăng nhập, đăng ký và bảo vệ tài khoản bằng thuật toán băm mật khẩu chuẩn PBKDF2.
- [x] Phân quyền 3 vai trò chuyên biệt: BIDDER (Người mua), SELLER (Người bán), ADMIN (Quản trị viên).
- [x] Giao diện sảnh chờ (Lobby) và không gian làm việc thay đổi linh hoạt theo vai trò người dùng.

### 6.2. Quản Lý Sản Phẩm & Vòng Đời Phiên Đấu Giá
- [x] Đăng tải đa dạng danh mục sản phẩm (Art, Electronics, Vehicle) kèm hình ảnh, mô tả và giá khởi điểm.
- [x] Chuyển dịch vòng đời phiên đấu giá tự động, khép kín: `PENDING` -> `OPEN` -> `RUNNING` -> `FINISHED` -> `PAID` / `CANCELED`.
- [x] Cơ chế kiểm duyệt: Admin trực tiếp xem xét và phê duyệt (Approve) các phiên đấu giá hợp lệ lên sàn.

### 6.3. Động Cơ Đấu Giá Trực Tiếp (Live Bidding Engine)
- [x] Giao tiếp thời gian thực (Real-time) toàn diện qua TCP Socket và luồng sự kiện JSON (Gson).
- [x] Broadcast lập tức mọi biến động giá thầu và người dẫn đầu tới toàn bộ Client.
- [x] Xử lý An toàn Đồng thời (Concurrency Safe): Sử dụng `synchronized` kết hợp cơ chế `WAL Mode` của SQLite để triệt tiêu hoàn toàn lỗi *Race Condition* khi hàng chục người cùng đặt giá.

### 6.4. Ví Điện Tử & Thanh Toán Hậu Kiểm (Post-Auction Checkout)
- [x] Quản lý số dư người dùng, gửi yêu cầu nạp tiền (Deposit) và Admin quản lý phê duyệt.
- [x] **Thanh toán nguyên tử (Atomic Payment):** Khi phiên kết thúc, tiến hành trừ tiền người mua, cộng tiền người bán và sinh lịch sử lưu vết (`transactions`) trong cùng một giao dịch cơ sở dữ liệu.
- [x] Tự động phạt và hủy kết quả (chuyển trạng thái `CANCELED`) nếu tài khoản người thắng thầu không đủ số dư thanh toán.

### 6.5. Các Tính Năng Thuật Toán Nâng Cao
- [x] **Proxy Auto-Bid (Đấu giá tự động):** Thuật toán ngầm giúp Bidder phản đòn trả giá cao hơn đối thủ chỉ trong 1/1000 giây (dựa trên mức trần cấu hình). Xử lý chuẩn xác logic đấu thầu chéo (Cross-Proxy) bằng toán học.
- [x] **Anti-Snipe (Chống bắn tỉa phút chót):** Liên tục giám sát thời gian thực. Tự động gia hạn thêm **60 giây** nếu phát hiện có lệnh đặt giá hợp lệ lọt vào **30 giây** cuối cùng của phiên.

---
## 7. Tài Liệu Báo Cáo & Trạng Thái Nộp Bài

### 7.1. Liên Kết Quan Trọng
* **Báo cáo Kỹ thuật (PDF):** [📄 Xem Báo cáo Bài tập lớn](./docs/OnlineAuction.pdf)
* **Video Demo Thực Tế:** [🎥 Xem Video Đấu giá cạnh tranh](LINK_VIDEO_CUA_EM_O_DAY)

### 7.2. Thông Tin Triển Khai & Nộp Bài
* **Nhánh nộp bài chính thức:** `main`
* **Hạn cuối commit nộp bài:** `23:59, ngày 31/05/2026`
