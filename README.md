# 🏆 Hệ Thống Đấu Giá Trực Tuyến (Online Auction Platform)

## 1. Mô tả ngắn gọn bài toán và phạm vi hệ thống
Hệ thống phần mềm hỗ trợ đấu giá tài sản trực tuyến thời gian thực (Real-time).
* **Phạm vi hệ thống:** Ứng dụng hoạt động theo mô hình Client-Server đa luồng. Cung cấp nền tảng cho người bán (Seller) đăng tải tài sản, thiết lập thời gian/giá khởi điểm và người mua (Bidder) tham gia trả giá trực tiếp. Hệ thống tích hợp thuật toán đặt giá tự động (Auto-bid), đếm ngược thời gian phiên hoàn toàn tự động.

## 2. Công nghệ sử dụng, môi trường chạy và yêu cầu cài đặt
* **Ngôn ngữ phát triển:** Java 17+
* **Giao diện người dùng (GUI):** JavaFX
* **Cơ sở dữ liệu:** SQLite. CSDL tự động khởi tạo trong lần chạy đầu tiên, không cần cài đặt Database Server bên ngoài.
* **Kiến trúc mạng:** TCP/Socket thuần kết hợp giao thức truyền tải định dạng JSON (Gson).
* **Bảo mật:** Băm mật khẩu người dùng theo chuẩn an toàn PBKDF2.
* **Môi trường chạy:** Yêu cầu máy tính cài đặt sẵn JRE/JDK 17 trở lên.

## 3. Cấu trúc thư mục và các module chính
Dự án được phân chia thành 3 module độc lập, quản lý tập trung thông qua Maven:

```
bidding-system/ (Thư mục gốc)
├── pom.xml
├── README.md (File tài liệu hướng dẫn)
├── data/ (Chứa `auction.db` và dữ liệu ảnh cục bộ)
├── logs/ (Chứa các file ghi nhận lỗi và nhật ký hoạt động của server)
│
├── common/ (Module CHUNG: chứa các thành phần cả Server và Client đều sử dụng)
│   ├── pom.xml
│   └── src/main/java/com/bidding/common/
│       ├── enums/         (Các hằng số hệ thống: AuctionStatus, ItemType, UserRole...)
│       ├── model/         (Các thực thể: Auction, AutoBid, Item (Art, Electronics...), User...)
│       └── payload/       (Các lớp định dạng gói tin Request/Response cũ)
│
├── server/ (Module SERVER: Xử lý logic nghiệp vụ, kết nối CSDL, chạy ngầm)
│   ├── pom.xml
│   └── src/main/java/com/bidding/server/
│       ├── core/          (Logic cốt lõi: AuctionService, AuthService, PasswordHasher...)
│       ├── database/      (Khởi tạo cấu trúc và quản lý kết nối CSDL WAL Mode)
│       ├── exception/     (Các lớp ngoại lệ tự định nghĩa: AuctionClosedException...)
│       ├── network/       (Quản lý Socket: AuctionServer, ClientHandler, Command Pattern)
│       ├── repository/    (Các lớp DAO xử lý truy vấn: AuctionDAO, UserDAO, ItemDAO...)
│       └── ServerApplication.java (Class chứa hàm main() khởi động Server)
│
└── client/ (Module CLIENT: Xử lý giao diện người dùng JavaFX)
    ├── pom.xml
    ├── src/main/java/action/
    │   ├── Authentication/ (Các Handle xử lý Đăng nhập, Đăng ký, Đổi mật khẩu...)
    │   ├── Core/           (Gốc khởi chạy: lớp `laucher`, StartScence)
    │   ├── MainUI/         (Các Handle xử lý giao diện quản trị Admin và sảnh Lobby)
    │   ├── SellingJobs/    (Các Handle quản lý luồng người bán đăng đồ và người mua trả giá)
    │   └── Utilities/      (Xử lý luồng mạng phía Client: SocketClient, SocketListener)
    └── src/main/resources/
        ├── assets/         (Tài nguyên tĩnh: hình ảnh, icon, profile pic...)
        └── views/          (Chứa toàn bộ file giao diện `.fxml` thiết kế bằng SceneBuilder)
```

## 4. Vị trí các file `.jar`
Các file Executable Fat JAR được định vị tại:
* **Server:** `server/target/server-1.0-SNAPSHOT.jar`
* **Client:** `client/target/client-1.0-SNAPSHOT.jar`

## 5. Hướng dẫn chạy Server/Client theo thứ tự cụ thể
Hệ thống yêu cầu chạy Server trước để mở cổng mạng, sau đó mới khởi động Client. Vui lòng mở 2 cửa sổ Terminal/Command Prompt song song.

**Bước 1: Khởi động Server**
1. Mở Terminal tại thư mục chứa file jar của Server.
2. Gõ lệnh: `java -jar server-1.0-SNAPSHOT.jar`
   *(Server sẽ thông báo "AuctionServer is running on port 888" và tự động load Database).*

**Bước 2: Khởi động Client**
1. Mở một Terminal mới tại thư mục chứa file jar của Client.
2. Gõ lệnh: `java -jar client-1.0-SNAPSHOT.jar`
   *(Có thể mở nhiều Terminal chạy Client cùng lúc để kiểm thử các tính năng).*

> **Tài khoản Admin kiểm thử:**
> * Username: `admin`
> * Password: `admin123`

## 6. Danh sách chức năng đã hoàn thành
* [x] Đăng nhập, Đăng ký, Quên mật khẩu (Bảo vệ thông tin bằng PBKDF2).
* [x] Phân quyền người dùng chi tiết (Admin, Seller, Bidder).
* [x] Quản lý phiên đấu giá (Tạo mới, Duyệt, Cập nhật trạng thái tự động).
* [x] Đấu giá trực tiếp thời gian thực (Broadcasting qua Socket).
* [x] Thuật toán Auto-bid (Tự động tính toán bước giá bảo vệ người mua).
* [x] Chống Snipe (Tự động gia hạn thời gian khi có lượt đặt giá ở giây cuối).
* [x] Quản lý ví điện tử nội bộ (Nạp tiền, Admin duyệt, tự động trừ tiền).

## 7. Link báo cáo PDF và video demo
* **Báo cáo PDF:** [📄 Xem Báo cáo Bài tập lớn (PDF)](./docs/OnlineAuction.pdf)
* **Video Demo:**