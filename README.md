# Quizizz — Nền Tảng Quiz Trực Tuyến Thời Gian Thực

Ứng dụng quiz multiplayer thời gian thực tương tự Kahoot/Quizizz, xây dựng với Spring Boot và React.

---

## Công Nghệ Sử Dụng

| Thành phần | Công nghệ |
|---|---|
| Backend | Spring Boot 3.5.6 (Java 21) |
| Frontend | React 19 + TypeScript + Vite |
| Database | PostgreSQL 17 |
| Cache | Redis |
| File Storage | MinIO (S3-compatible) |
| Real-time | Socket.IO (netty-socketio) |
| Auth | JWT (cookie-based) |
| AI | Google Gemini API |
| Email | Spring Mail + Thymeleaf |
| API Docs | Swagger / OpenAPI |

---

## Cấu Trúc Dự Án

```
doan/
├── src/
│   ├── main/
│   │   ├── java/org/example/quizizz/
│   │   │   ├── common/                         # Xử lý chung
│   │   │   │   ├── cache/                      # Redis cache (auth, game, presence)
│   │   │   │   ├── config/                     # Spring configs (Security, CORS, JWT, Minio, Socket.IO...)
│   │   │   │   ├── constants/                  # Enum & hằng số (Role, Permission, RoomMode...)
│   │   │   │   ├── event/                      # Spring Events cho vòng đời phòng chơi
│   │   │   │   ├── exception/                  # Global exception handler
│   │   │   │   └── security/                   # Rate limiting, resource ownership
│   │   │   ├── controller/
│   │   │   │   ├── api/                        # REST endpoints
│   │   │   │   │   ├── AuthController          # Đăng nhập, đăng ký, refresh token
│   │   │   │   │   ├── RoomController          # Tạo/quản lý phòng chơi
│   │   │   │   │   ├── ExamController          # Bộ đề thi
│   │   │   │   │   ├── QuestionController      # Câu hỏi
│   │   │   │   │   ├── TopicController         # Chủ đề
│   │   │   │   │   ├── LeaderboardController   # Bảng xếp hạng
│   │   │   │   │   ├── ProfileController       # Hồ sơ người dùng
│   │   │   │   │   ├── AIController            # Sinh câu hỏi bằng AI
│   │   │   │   │   ├── UserController          # Quản lý người dùng (Admin)
│   │   │   │   │   └── RoleController          # Quản lý vai trò (Admin)
│   │   │   │   └── socketio/                   # Socket.IO event handlers
│   │   │   │       ├── listener/               # Xử lý sự kiện phòng & game
│   │   │   │       ├── broadcast/              # Phát sóng sự kiện đến client
│   │   │   │       └── session/                # Quản lý phiên kết nối
│   │   │   ├── service/                        # Business logic
│   │   │   │   └── helper/                     # Game timer, tính điểm, achievement
│   │   │   ├── model/                          # Entity & DTO theo domain
│   │   │   ├── repository/                     # Spring Data JPA repositories
│   │   │   ├── mapper/                         # MapStruct entity <-> DTO
│   │   │   └── security/                       # JWT provider & auth filters
│   │   ├── resources/
│   │   │   ├── config/                         # application*.yml theo profile
│   │   │   └── templates/                      # Email templates (Thymeleaf)
│   │   ├── docker/                             # Docker Compose files & scripts
│   │   └── webapp/                             # Frontend React
│   │       └── src/
│   │           ├── features/
│   │           │   ├── admin/                  # Dashboard, quản lý user/role/topic
│   │           │   ├── teacher/                # Tạo đề, câu hỏi, thống kê, AI generator
│   │           │   └── player/                 # Dashboard, phòng chơi, bảng xếp hạng
│   │           ├── components/
│   │           │   ├── room/                   # WaitingRoom, GameRoom, GameResults...
│   │           │   └── profile/                # Hồ sơ, avatar, đổi mật khẩu
│   │           ├── pages/
│   │           │   ├── auth/                   # Login, Register, Forgot Password...
│   │           │   ├── dashboard/              # Hero, Stats, Quick Actions sections
│   │           │   └── game/                   # GamePlay
│   │           ├── services/                   # API clients & Socket.IO client
│   │           ├── stores/                     # Zustand state management
│   │           ├── routers/                    # React Router v7 + route guards
│   │           ├── hooks/                      # Custom hooks
│   │           └── styles/                     # CSS theo feature/component
│   └── test/                                   # JUnit 5 + Testcontainers
├── docs/                                       # Hướng dẫn deploy, CI/CD
├── pom.xml
└── README.md
```

---

## Tài Khoản Demo

### Admin
| Trường | Giá trị |
|---|---|
| Username | `admin` |
| Password | `admin123` |
| Email | `admin@quizizz.com` |

**Quyền hạn:** Toàn quyền — quản lý người dùng, vai trò, phân quyền, chủ đề, xem toàn bộ dữ liệu hệ thống.

---

### Teacher
| Trường | Giá trị |
|---|---|
| Username | `user01` |
| Password | `password123` |
| Email | `user01@example.com` |

**Quyền hạn:** Tạo và quản lý bộ đề, câu hỏi, chủ đề; tạo phòng chơi; kick người chơi; mời người chơi; sinh câu hỏi bằng AI; xem thống kê.

---

### Player
| Trường | Giá trị |
|---|---|
| Username | `player` |
| Password | `player123` |
| Email | `player@quizizz.com` |

**Quyền hạn:** Tham gia phòng chơi, trả lời câu hỏi, xem điểm số, xem bảng xếp hạng, quản lý hồ sơ cá nhân.

---

## Chạy Local

### Yêu cầu
- Java 21
- Node.js 20+
- Docker Desktop

### Bước 1 — Khởi động infrastructure (PostgreSQL, Redis, MinIO)

```powershell
docker ps
```
Nếu các container `quizizz-postgres`, `quizizz-redis`, `quizizz-minio` chưa chạy:
```powershell
.\src\main\docker\docker-up.ps1
```

### Bước 2 — Khởi động Backend

```powershell
$env:DB_USERNAME = "quizizz_app"
$env:DB_PASSWORD = "mZWHP6NbjDz3Oazm"
$env:DB_URL = "jdbc:postgresql://localhost:5432/quizizz?TimeZone=Asia/Ho_Chi_Minh"
$env:SPRING_DOCKER_COMPOSE_ENABLED = "false"
.\mvnw.cmd spring-boot:run -Pdev-no-frontend-build
```

Backend chạy tại: http://localhost:8080

### Bước 3 — Khởi động Frontend

```powershell
cd src\main\webapp
npm ci          # chỉ cần chạy lần đầu
npm run dev
```

Frontend chạy tại: http://localhost:5173

### Các URL quan trọng

| Dịch vụ | URL |
|---|---|
| Frontend | http://localhost:5173 |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| MinIO Console | http://localhost:9001 |

---

## Spring Profiles

| Profile | Mục đích |
|---|---|
| `dev` | Local với Docker Compose tự động |
| `dev-no-frontend-build` | Backend-only, không build frontend |
| `docker` | Full Docker Compose stack |
| `prod` | Production |

---

## Biến Môi Trường

Tạo file `src/main/docker/.env` từ `.env.example`:

```env
POSTGRES_DB=quizizz
POSTGRES_USER=quizizz_app
POSTGRES_PASSWORD=...
MINIO_ROOT_USER=...
MINIO_ROOT_PASSWORD=...
JWT_SECRET_KEY=...
GEMINI_API_KEY=...
MAIL_USERNAME=...
MAIL_PASSWORD=...
```
