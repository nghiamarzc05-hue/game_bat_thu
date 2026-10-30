# 🌾 Nông Trại & Thú Bảo Miền Tây – RPG 2D

![RPG Miền Tây](https://img.shields.io/badge/Genre-2D%20Canvas%20RPG-yellow)
![Language](https://img.shields.io/badge/Language-Nam%20B%E1%BB%99%20Vi%E1%BB%87t%20Nam-green)
![License](https://img.shields.io/badge/License-MIT-blue)

Game nhập vai 2D phong cách **Nông Trại & Săn Bắt Thú Bảo Miền Tây** chạy mượt mà trực tiếp trên trình duyệt web (HTML5 Canvas), sử dụng thuần ngôn ngữ Nam Bộ cực kỳ thân thiện và dí dỏm!

---

## 🌟 Tính Năng Nổi Bật

### 🌾 1. Nông Trại & Bếp Nấu Miền Tây
* **Trồng trọt & Thu hoạch**: Gieo hạt Lúa Vàng, Bắp Nếp, Chuối Xiêm, Sen Hồng.
* **Bếp Nấu Bà Tư**: Chế biến *Cơm Lúa Trắng, Bánh Bắp Ngọt, Chuối Nướng Mật, Chè Sen Nhung* giúp hồi máu cấp tốc hoặc tăng Máu tối đa cho Thú Bảo.

### 🐾 2. Hệ Thống Thú Bảo (15 Loài · 6 Hệ Thuộc Tính)
* **Xuất hiện trực tiếp trên Map (Wild Pet NPCs)**: Thú hoang di chuyển tự do bằng Pixel Physics, tự đổi hướng, nảy tường và cảnh báo dấu `!` khi bạn lại gần.
* **6 Hệ Thuộc tính**: ⚡ **Lôi** · ⛰️ **Thổ** · 🔥 **Hỏa** · 💧 **Thủy** · 🌿 **Mộc** · ✨ **Đặc Biệt**.
* **7 Cấp Tư Chất (S, A, B, C, D, E, F)**:
  - **Sức mạnh khởi điểm Cấp 1**: Tư chất S (+25% chỉ số gốc), F (-30% chỉ số gốc).
  - **Tăng trưởng khi Level Up**: S (+15% chỉ số/cấp), F (+4% chỉ số/cấp).
* **Đồ họa SVG Animated**: Thú cưng như *Gà Đá Chọi* và *Chó Cỏ Giữ Nhà* sở hữu hiệu ứng hoạt họa SVG cực nét (thở, nháy sét, lơ lửng đá).
* **Quản lý & Bán Pet**: Xem trang thông tin chi tiết từng Pet, xuất trận hoặc **Bán Pet** lấy Tiền.

### ⚔️ 3. Cơ Chế Chiến Đấu & Bắt Thú
* **Đụng là đánh**: Đi lại gần đụng thú hoang trên map để mở màn hình chiến đấu.
* **Hành động phong phú**: *Đánh Luôn*, *Tung Chiêu*, *Quăng Nơm (Bắt thú)*, *Tẩu Thoát*.
* **Tự động hồi máu**: Thú bảo tự hồi **2% MaxHP/giây** khi đứng ngoài trận đấu.

### 🗺️ 4. Thế Giới 8 Bản Đồ (Biomes)
* **8 Bản đồ**: 🏡 Nông Trại, 🌾 Đồng Ruộng, 🌫️ Rừng Sương Mù, 🪷 Hồ Sen, ⛏️ Hầm Mỏ, 🏔️ Núi Cao, 🌿 Đồng Cỏ, 🌊 Biển Khơi.
* **Hiệu ứng đồ họa**: Vignette tối góc, hạt sáng/đom đóm lơ lửng, cổng dịch chuyển huyền ảo, Minimap góc dưới màn hình.

### 💾 5. Hệ Thống Lưu Game & Hướng Dẫn Tân Thủ
* **📖 Sổ Tay Hướng Dẫn Tân Thủ**: Ấn nút `📖` trên thanh HUD để xem lại toàn bộ bí kíp trồng chọt, bắt thú, tư chất S-F và chiến đấu. Nếu là người chơi mới, bảng hướng dẫn sẽ tự động bật lên khi lần đầu vào game!
* **💾 Lưu & Tải Tiến Trình (LocalStorage)**:
  - Game tự động lưu sau mỗi **30 giây**, hoặc sau khi **thắng trận / bắt pet mới / mua bán**.
  - Người chơi có thể bấm nút `💾` trên thanh HUD để lưu chủ động bất kỳ lúc nào.
  - Mỗi khi F5 hoặc mở lại trình duyệt, tiến trình game (Tiền, Bản đồ hiện tại, Vị trí nhân vật, Túi đồ, Thú bảo) sẽ được khôi phục 100%!
  - Có sẵn nút **Xóa Dữ Liệu & Chơi Lại Từ Đầu** trong sổ tay tân thủ.

### 🎮 6. Điều Khiển
* **Di chuyển**: Phím `WASD` / Mũi tên.
* **Click-to-move**: Click chuột trên map để nhân vật tự dò đường tới vị trí mong muốn.
* **Tương tác**: Phím `E` / `Space` hoặc bước vào ô cổng dịch chuyển.

---

## 🚀 Hướng Dẫn Đăng Lên GitHub Pages (Miễn Phí)

1. Tạo một repository mới trên GitHub (Ví dụ: `nong-trai-mien-tay`).
2. Tải các file trong thư mục này lên repository (`index.html`, `README.md`, `LICENSE`, `.gitignore`).
3. Vào phần **Settings** -> **Pages** của repository trên GitHub.
4. Tại mục **Build and deployment / Branch**, chọn `main` (hoặc `master`) và chọn thư mục `/ (root)` -> ấn **Save**.
5. Đợi khoảng 1-2 phút, GitHub sẽ cấp cho bạn một đường link game dạng:
   `https://<ten-user>.github.io/nong-trai-mien-tay/`

---

## 🛠️ Công Nghệ Sử Dụng
* **HTML5 Canvas & Pure Vanilla JavaScript (ES6+)**
* **TailwindCSS (CDN)** cho UI glassmorphism hiện đại
* **FontAwesome 6.4** cho icons
* **SVG & CSS Animations** cho hiệu ứng Thú Bảo

---

## 📄 Giấy Phép
Dự án được phát hành theo giấy phép [MIT License](LICENSE).
