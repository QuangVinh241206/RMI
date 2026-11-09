# HỆ THỐNG CHUYỂN TIỀN TRỰC TUYẾN SỬ DỤNG JAVA RMI & JAVAFX
## ĐỒNG BỘ HÓA DỮ LIỆU TRÊN 2 SERVER (CHẠY TRÊN 2 MÁY THỰC VÀ CÁC MÁY ẢO)

---

## 1. Yêu cầu đề bài (Theo hình ảnh)
* **Đề tài số 5**: RMI Đồng bộ hóa dữ liệu trên 2 Server.
* **Môi trường triển khai**: Server chạy trên 2 máy thực và các Client chạy trên các máy ảo (VMware / VirtualBox).

---

## 2. Kiến trúc hệ thống & Cơ chế đồng bộ (Architecture)

```
                 +-------------------------------------------------------+
                 |            JAVA RMI PEER-TO-PEER SYNC                |
                 |  (Đồng bộ giao dịch, tài khoản & Catch-up Snapshot)   |
                 +--------------------------^----------------------------+
                                            |
                      +---------------------+---------------------+
                      |                                           |
                      v                                           v
       +-------------------------------+           +-------------------------------+
       |       MÁY CHỦ THỰC 1          |           |       MÁY CHỦ THỰC 2          |
       |  (Physical Machine 1)         |           |  (Physical Machine 2)         |
       |  - Server ID: SERVER_1        |           |  - Server ID: SERVER_2        |
       |  - RMI Registry: Port 1099    |           |  - RMI Registry: Port 1100    |
       |  - Database: bank_server1.db  |           |  - Database: bank_server2.db  |
       |  - Giao diện Server Dashboard |           |  - Giao diện Server Dashboard |
       +---------------+---------------+           +---------------+---------------+
                       ^                                           ^
                       |              (RMI Calls)                  |
                       +-------------------+-----------------------+
                                           |
              +----------------------------+----------------------------+
              |                                                         |
              v                                                         v
   +-----------------------+                                 +-----------------------+
   |   MÁY ẢO 1 (VMware)   |                                 |   MÁY ẢO 2 (VMware)   |
   |      CLIENT APP       |                                 |      CLIENT APP       |
   | - Đăng nhập / Đăng ký |                                 | - Đăng nhập / Đăng ký |
   | - Chuyển tiền 24/7    |                                 | - Chuyển tiền 24/7    |
   | - Tự động Failover    |                                 | - Tự động Failover    |
   +-----------------------+                                 +-----------------------+
```

### Các đặc điểm kỹ thuật nổi bật:
1. **Đồng bộ hóa 2 chiều tức thì (Real-time Bi-directional Replication)**:
   - Khi có bất kỳ giao dịch nào (Chuyển tiền, Nạp tiền, Rút tiền, Mở tài khoản) phát sinh trên Server 1, Server 1 xử lý cục bộ với transaction database đảm bảo tính toàn vẹn (ACID), sau đó lập tức gọi RMI `receiveReplication(payload)` sang Server 2.
   - Server 2 cập nhật số dư, số phiên bản (version) và lịch sử giao dịch tương ứng ngay trong tích tắc mà không gây vòng lặp đồng bộ vô hạn (chống echo loop nhờ Server ID tracking).
2. **Tự động bắt kịp dữ liệu khi Server bật lại (Automatic Catch-up Sync)**:
   - Cơ chế Heartbeat định kỳ (3 giây) theo dõi tình trạng sống/chết của Peer Server.
   - Khi một server bị tắt/sập rồi được khởi động lại, nó sẽ tự động gửi yêu cầu `requestFullSyncSnapshot()` đến server đang sống để kéo toàn bộ các giao dịch đã bị bỏ lỡ trong lúc offline.
3. **Chuyển đổi dự phòng tự động (Transparent Client Failover)**:
   - Client được cấu hình kết nối tới cả Server 1 và Server 2.
   - Nếu Server 1 gặp sự cố hoặc mất mạng, Client sẽ tự động phát hiện và chuyển tuyến (Failover) sang Server 2 để người dùng tiếp tục giao dịch mà không bị gián đoạn.
   - Người dùng cũng có thể bấm nút "Chuyển Server" trên giao diện để kiểm chứng số dư và lịch sử trên cả 2 server là 100% đồng nhất.
4. **Lưu trữ bền vững riêng biệt (Isolated SQLite Database)**:
   - Mỗi server sở hữu file CSDL SQLite riêng biệt (`data/bank_server1.db` và `data/bank_server2.db`), phản ánh chân thực môi trường phân tán giữa các máy chủ.

---

## 3. Danh sách tài khoản thử nghiệm có sẵn

Hệ thống đã tự động khởi tạo sẵn các tài khoản demo để bạn có thể thử nghiệm ngay:

| Số tài khoản | Chủ tài khoản | Mã PIN mặc định | Số dư ban đầu |
|:---:|:---:|:---:|:---:|
| `100001` | Nguyen Van A | `123456` | 10.000.000 đ |
| `100002` | Tran Thi B | `123456` | 5.000.000 đ |
| `100003` | Le Van C | `123456` | 2.500.000 đ |

*(Bạn cũng có thể tạo tài khoản mới ngay trên ứng dụng Client bằng tab "Mở Tài Khoản")*

---

## 4. Hướng dẫn chạy thử nghiệm

### Cách 1: Chạy thử nghiệm nhanh trên cùng 1 máy tính (Local Test)
1. Chạy file `run-server1.bat` để mở giao diện Giám sát Server 1 (Port 1099).
2. Chạy file `run-server2.bat` để mở giao diện Giám sát Server 2 (Port 1100).
   - *Trên cả 2 màn hình Server sẽ hiển thị trạng thái xanh lá: `PEER: CONNECTED`.*
3. Chạy file `run-client.bat` để mở giao diện Client (có thể mở nhiều cửa sổ Client cùng lúc để mô phỏng nhiều người dùng).
4. Thực hiện chuyển tiền từ tài khoản `100001` sang `100002`:
   - Quan sát tab Live Logs và Bảng tài khoản trên cả 2 Server: dữ liệu đồng bộ tức thì sang cả 2 Server!

---

### Cách 2: Triển khai trên 2 máy thực và các máy ảo (Theo đúng đề bài)

#### Bước 1: Chuẩn bị mạng
- Kết nối Máy thực 1 và Máy thực 2 vào cùng mạng LAN (chung Wi-Fi hoặc cắm dây LAN).
- Đặt mạng của các máy ảo trong VMware/VirtualBox sang chế độ **Bridged (Bridge Networking)** để máy ảo nhận được địa chỉ IP trong cùng dải mạng LAN với 2 máy thực.
- Kiểm tra địa chỉ IP bằng lệnh `ipconfig` trên mỗi máy:
  - Ví dụ:
    - Máy thực 1: `192.168.1.10`
    - Máy thực 2: `192.168.1.20`
    - Máy ảo 1: `192.168.1.31`
    - Máy ảo 2: `192.168.1.32`
- Đảm bảo mở tường lửa (Firewall) cho Java hoặc cổng `1099` và `1100`.

#### Bước 2: Chạy Server 1 trên Máy thực 1
- Trên Máy thực 1, mở `run-server1.bat`.
- Trên thanh cấu hình Server, nhập:
  - **Host/IP**: `192.168.1.10` *(IP của Máy thực 1)*
  - **Port**: `1099`
  - **Peer Host**: `192.168.1.20` *(IP của Máy thực 2)*
  - **Peer Port**: `1100`
- Bấm **Khởi động**.

#### Bước 3: Chạy Server 2 trên Máy thực 2
- Trên Máy thực 2, mở `run-server2.bat`.
- Trên thanh cấu hình Server, nhập:
  - **Host/IP**: `192.168.1.20` *(IP của Máy thực 2)*
  - **Port**: `1100`
  - **Peer Host**: `192.168.1.10` *(IP của Máy thực 1)*
  - **Peer Port**: `1099`
- Bấm **Khởi động**.
- Lúc này cả 2 máy thực sẽ kết nối RMI với nhau và bắt đầu đồng bộ hóa.

#### Bước 4: Chạy Client trên các Máy ảo (VMware)
- Copy thư mục chương trình vào máy ảo hoặc chia sẻ qua Shared Folder / Mạng.
- Trên máy ảo, mở `run-client.bat`.
- Tại màn hình đăng nhập, mở rộng mục **"Cấu hình IP / Port của 2 Máy Chủ RMI"**:
  - **Server 1**: Nhập `192.168.1.10` - Port: `1099`
  - **Server 2**: Nhập `192.168.1.20` - Port: `1100`
  - Bấm **Lưu & Kiểm tra kết nối** (sẽ thấy đèn xanh `● Online` cho cả 2 máy chủ).
- Tiến hành đăng nhập và thực hiện chuyển tiền giữa các tài khoản!

---

## 5. Kịch bản báo cáo / Chấm điểm ấn tượng với Giảng viên

1. **Minh họa đồng bộ thời gian thực**:
   - Để cửa sổ Server 1, Server 2 và Client cạnh nhau.
   - Từ Client, thực hiện chuyển tiền từ tài khoản A sang B.
   - Chỉ cho giảng viên thấy: Ngay khi giao dịch hoàn tất, cả 2 bảng tài khoản và log trên Server 1 lẫn Server 2 đều đồng thời nhảy số dư và ghi nhận cùng một mã giao dịch.
2. **Minh họa tính sẵn sàng và tự phục hồi (Fault Tolerance)**:
   - Đang giao dịch bình thường, bấm nút **"Dừng"** trên Server 1 (mô phỏng Server 1 bị sập).
   - Trên Client, bấm nút **"Chuyển Server"** hoặc thực hiện giao dịch tiếp theo -> Client tự động chuyển tuyến sang Server 2 và giao dịch vẫn thành công bình thường.
3. **Minh họa Catch-up Sync khi phục hồi**:
   - Trong khi Server 1 đang tắt, thực hiện 2-3 giao dịch nạp/chuyển tiền trên Server 2.
   - Bấm **"Khởi động"** lại Server 1.
   - Server 1 khởi động lên, tự động bắt tay với Server 2 và kéo toàn bộ các giao dịch vừa phát sinh về CSDL cục bộ của nó -> Cả 2 Server lại đồng bộ hoàn toàn!
