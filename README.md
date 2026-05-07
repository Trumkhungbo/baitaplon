# 🏆 Hệ Thống Đấu Giá Trực Tuyến (Online Auction Platform)
Đây là đồ án môn học xây dựng hệ thống đấu giá thời gian thực (Real-time) áp dụng kiến trúc **Java Multi-module** chuẩn mực. Dự án là sự kết hợp giữa giao diện người dùng JavaFX hiện đại và hệ thống Backend xử lý đồng thời (Concurrency) hiệu suất cao qua giao thức TCP/IP Socket.

---

## 🌟 Tính Năng Nổi Bật (Key Features)

- **Đấu giá thời gian thực (Real-time Bidding):** Cập nhật giá thầu và trạng thái phiên đấu giá ngay lập tức cho toàn bộ Client nhờ cơ chế Broadcast Socket.
- **Auto-Bidding Engine (Chống bắn tỉa):** Hệ thống tự động tính toán thời gian và gia hạn (Anti-sniping) ở những giây cuối cùng để đảm bảo công bằng.
- **Kiến trúc Multi-module:** Phân tách hoàn toàn logic theo nguyên lý SOLID (Client - Server - Common), tối ưu hóa việc quản lý mã nguồn và làm việc nhóm.
- **Bảo mật & Phiên đăng nhập:** Xác thực người dùng an toàn, cấp quyền linh hoạt (Admin/Seller/Bidder).
- **Giao diện Cao cấp (Premium UI):** Trải nghiệm hệ thống mượt mà, thân thiện với người dùng nhờ bộ thư viện **AtlantaFX** và hệ thống icon **Ikonli**.
- **Lưu trữ CSDL (Persistence):** Quản lý toàn bộ giao dịch, lịch sử đấu giá an toàn với cơ sở dữ liệu SQLite.

---

## 💻 Công Nghệ Sử Dụng (Tech Stack)

### Core Technologies
- **Ngôn ngữ (Language):** Java 21
- **Quản lý gói (Build System):** Maven 3.x
- **Giao tiếp mạng (Network):** Java Socket API (TCP/IP)
- **Luồng (Concurrency):** ExecutorService, Thread Pool, ConcurrentHashMap
---


## Project Structure
```text
bidding-system/ (Thư mục gốc của toàn bộ dự án)
├── pom.xml (Nếu dùng Maven) hoặc build.gradle (Nếu dùng Gradle)
└── README.md (File cực kỳ quan trọng để ghi chú cách chạy app cho giảng viên)

common/ (Module CHUNG: chứa code cả Server và Client đều cần)
└── src/main/java/com/bidding/common/
    ├── model/         (Chứa Entity, Item, User, Bidder, Electronics...)
    ├── enums/         (Chứa các hằng số: AuctionStatus, UserRole...)
    ├── payload/       (Chứa các lớp định dạng dữ liệu gửi qua Socket, ví dụ: BidRequest, ResponseMsg)
    └── utils/         (Các hàm tiện ích dùng chung: validate email, format tiền tệ...)

server/ (Module SERVER: Xử lý logic nghiệp vụ, chạy ngầm)
└── src/main/java/com/bidding/server/
    ├── ServerApplication.java (Class chứa hàm main() để khởi động Server)
    ├── network/        (Chứa ServerSocket, ClientHandler để lắng nghe kết nối)
    ├── core/           (Chứa Auction, AuctionManager - Singleton, AutoBidThread)
    ├── factory/        (Chứa ItemFactory...)
    └── repository/     (Nơi xử lý đọc/ghi dữ liệu vào file TXT/JSON hoặc Database)

client/ (Module CLIENT: Giao diện người dùng JavaFX)
├── src/main/java/com/bidding/client/
│   ├── ClientApplication.java (Class kế thừa Application của JavaFX để chạy app)
│   ├── controller/      (Controller điều khiển giao diện: LoginController, AuctionRoomController...)
│   └── network/         (SocketClient để gửi/nhận dữ liệu với Server)
└── src/main/resources/com/bidding/client/
    ├── views/           (Chứa toàn bộ file giao diện .fxml tạo từ SceneBuilder)
    ├── css/             (Chứa file style để làm đẹp giao diện)
    └── assets/          (Hình ảnh, icon sản phẩm...)
```
---
