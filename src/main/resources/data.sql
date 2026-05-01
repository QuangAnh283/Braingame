-- =====================================================================
-- Quizizz seed data — dev profile only
-- Runs AFTER Hibernate creates tables (spring.jpa.defer-datasource-initialization=true).
-- Idempotent: every block has either ON CONFLICT DO NOTHING or a WHERE NOT EXISTS guard,
-- so restarting the app does not create duplicates.
-- Default password for every seeded user: "password123"
--   BCrypt hash below was generated with cost 10.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) Permissions  (matches DataInitializer — system rows, system_flag='1')
-- ---------------------------------------------------------------------
INSERT INTO permissions (permission_name, description, is_delete, system_flag, created_at, updated_at) VALUES
  ('user:manage',          'Quản lý người dùng và phân quyền', false, '1', NOW(), NOW()),
  ('user:manage_profile',  'Quản lý hồ sơ cá nhân',            false, '1', NOW(), NOW()),
  ('role:manage',          'Quản lý vai trò hệ thống',         false, '1', NOW(), NOW()),
  ('permission:manage',    'Quản lý quyền hạn',                false, '1', NOW(), NOW()),
  ('question:manage',      'Quản lý câu hỏi',                  false, '1', NOW(), NOW()),
  ('topic:manage',         'Quản lý chủ đề',                   false, '1', NOW(), NOW()),
  ('room:manage',          'Quản lý phòng chơi',               false, '1', NOW(), NOW()),
  ('room:join',            'Tham gia phòng chơi',              false, '1', NOW(), NOW()),
  ('room:kick_player',     'Kick người chơi khỏi phòng',       false, '1', NOW(), NOW()),
  ('room:leave',           'Rời khỏi phòng',                   false, '1', NOW(), NOW()),
  ('room:invite',          'Mời người chơi vào phòng',         false, '1', NOW(), NOW()),
  ('game:start',           'Bắt đầu trò chơi',                 false, '1', NOW(), NOW()),
  ('game:answer',          'Trả lời câu hỏi',                  false, '1', NOW(), NOW()),
  ('game:view_score',      'Xem điểm số',                      false, '1', NOW(), NOW()),
  ('rank:view',            'Xem bảng xếp hạng',                false, '1', NOW(), NOW())
ON CONFLICT (permission_name) DO NOTHING;

-- ---------------------------------------------------------------------
-- 2) Roles  (no unique key on role_name — use WHERE NOT EXISTS)
-- ---------------------------------------------------------------------
INSERT INTO roles (role_name, description, is_delete, system_flag, created_at, updated_at)
SELECT 'PLAYER', 'Người chơi - Có thể tham gia game, quản lý profile cá nhân', false, '1', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'PLAYER');

INSERT INTO roles (role_name, description, is_delete, system_flag, created_at, updated_at)
SELECT 'TEACHER', 'Giáo viên - Có thể tạo phòng, quản lý game, kick player', false, '1', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'TEACHER');

INSERT INTO roles (role_name, description, is_delete, system_flag, created_at, updated_at)
SELECT 'ADMIN',  'Quản trị viên - Toàn quyền quản lý hệ thống', false, '1', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'ADMIN');

-- ---------------------------------------------------------------------
-- 3) Role ↔ Permission mappings  (ADMIN gets all, TEACHER & PLAYER get subsets)
-- ---------------------------------------------------------------------
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, NOW(), NOW()
FROM roles r
CROSS JOIN permissions p
WHERE r.role_name = 'ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, NOW(), NOW()
FROM roles r
JOIN permissions p ON p.permission_name IN
  ('user:manage_profile','room:join','room:leave','game:answer','game:view_score','rank:view')
WHERE r.role_name = 'PLAYER'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, NOW(), NOW()
FROM roles r
JOIN permissions p ON p.permission_name IN
  ('user:manage_profile','topic:manage','question:manage',
   'room:manage','room:join','room:leave','room:kick_player',
   'room:invite','game:start','game:answer','game:view_score','rank:view')
WHERE r.role_name = 'TEACHER'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ---------------------------------------------------------------------
-- 4) Topics  (15)  — unique on name
-- ---------------------------------------------------------------------
INSERT INTO topics (name, description, created_at, updated_at) VALUES
  ('Toán học',           'Các câu hỏi về toán học phổ thông và nâng cao', NOW(), NOW()),
  ('Vật lý',             'Cơ, nhiệt, điện, quang, nguyên tử',              NOW(), NOW()),
  ('Hóa học',            'Hóa vô cơ, hữu cơ và hóa phân tích',             NOW(), NOW()),
  ('Sinh học',           'Sinh học tế bào, di truyền, tiến hóa',           NOW(), NOW()),
  ('Lịch sử',            'Lịch sử Việt Nam và thế giới',                   NOW(), NOW()),
  ('Địa lý',             'Địa lý tự nhiên, kinh tế, xã hội',               NOW(), NOW()),
  ('Ngữ văn',            'Văn học Việt Nam và thế giới',                   NOW(), NOW()),
  ('Tiếng Anh',          'Ngữ pháp, từ vựng và đọc hiểu',                  NOW(), NOW()),
  ('Tin học',            'Lập trình, thuật toán, cấu trúc dữ liệu',        NOW(), NOW()),
  ('GDCD',               'Giáo dục công dân và pháp luật',                 NOW(), NOW()),
  ('Âm nhạc',            'Nhạc lý và lịch sử âm nhạc',                     NOW(), NOW()),
  ('Mỹ thuật',           'Hội họa, điêu khắc, kiến trúc',                  NOW(), NOW()),
  ('Thể thao',           'Các môn thể thao và luật chơi',                  NOW(), NOW()),
  ('Kinh tế',            'Kinh tế học và tài chính',                       NOW(), NOW()),
  ('Khoa học thường thức','Kiến thức chung về khoa học đời sống',          NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ---------------------------------------------------------------------
-- 5) Users  (120 = 20 TEACHER + 100 PLAYER)  — unique on username & email
-- ---------------------------------------------------------------------
INSERT INTO users (username, password, email, full_name, phone_number, address, dob,
                   type_account, online, email_verified, is_delete, system_flag, created_at, updated_at)
SELECT
  'user' || LPAD(n::text, 3, '0'),
  '$2b$10$TmuDL4M1TaxrA39tuajIaeLkLnGP31Y1x6X5pOptS3C4paLp2GS9O',
  'user' || LPAD(n::text, 3, '0') || '@quizizz.com',
  'Seed User ' || LPAD(n::text, 3, '0'),
  '09' || LPAD(((n * 7919) % 100000000)::text, 8, '0'),
  'Địa chỉ ' || n || ', Quận ' || ((n % 12) + 1) || ', TP.HCM',
  (DATE '1990-01-01' + ((n * 37) % 12000) * INTERVAL '1 day')::date,
  CASE WHEN n <= 20 THEN 'TEACHER' ELSE 'PLAYER' END,
  (n % 5 = 0),
  true,
  false,
  '0',
  NOW(), NOW()
FROM generate_series(1, 120) AS n
ON CONFLICT (username) DO NOTHING;

-- ---------------------------------------------------------------------
-- 6) User ↔ Role  (each seeded user gets the role matching type_account)
-- ---------------------------------------------------------------------
INSERT INTO user_roles (user_id, role_id, created_at, updated_at)
SELECT u.id, r.id, NOW(), NOW()
FROM users u
JOIN roles r ON r.role_name = u.type_account
WHERE u.username LIKE 'user%'
ON CONFLICT (user_id, role_id) DO NOTHING;

-- ---------------------------------------------------------------------
-- 7) Exams  (30)  — only run when table is empty
-- ---------------------------------------------------------------------
INSERT INTO exams (topic_id, teacher_id, title, description, created_at, updated_at)
SELECT
  (SELECT id FROM topics                  ORDER BY id OFFSET ((n - 1) % 15) LIMIT 1),
  (SELECT id FROM users WHERE type_account = 'TEACHER' AND username LIKE 'user%'
                                           ORDER BY id OFFSET ((n - 1) % 20) LIMIT 1),
  'Đề thi ' || LPAD(n::text, 3, '0'),
  'Bộ đề số ' || n || ' — seed data',
  NOW(), NOW()
FROM generate_series(1, 30) AS n
WHERE NOT EXISTS (SELECT 1 FROM exams LIMIT 1);

-- ---------------------------------------------------------------------
-- 8) Questions  (10 per exam × 30 exams = 300)
-- ---------------------------------------------------------------------
INSERT INTO questions (exam_id, question_text, question_type, created_at, updated_at)
SELECT
  e.id,
  'Câu ' || q_n || ' của ' || e.title || ' — nội dung câu hỏi mẫu?',
  CASE (q_n % 3)
    WHEN 0 THEN 'TRUE_FALSE'
    WHEN 1 THEN 'SINGLE_CHOICE'
    ELSE       'MULTIPLE_CHOICE'
  END,
  NOW(), NOW()
FROM exams e
CROSS JOIN generate_series(1, 10) AS q_n
WHERE NOT EXISTS (SELECT 1 FROM questions LIMIT 1);

-- ---------------------------------------------------------------------
-- 9) Answers  (4 per question × 300 questions = 1200)  — answer 1 is correct
-- ---------------------------------------------------------------------
INSERT INTO answers (question_id, answer_text, is_correct)
SELECT
  q.id,
  'Đáp án ' || a_n || ' cho câu #' || q.id,
  (a_n = 1)
FROM questions q
CROSS JOIN generate_series(1, 4) AS a_n
WHERE NOT EXISTS (SELECT 1 FROM answers LIMIT 1);

-- ---------------------------------------------------------------------
-- 10) Rooms  (40)  — unique on room_code
-- ---------------------------------------------------------------------
INSERT INTO rooms (room_code, room_name, room_mode, topic_id, exam_id, is_private, owner_id,
                   status, max_players, question_count, countdown_time, has_game_history,
                   created_at, updated_at)
SELECT
  'ROOM' || LPAD(n::text, 5, '0'),
  'Phòng chơi số ' || n,
  CASE n % 2 WHEN 0 THEN 'CLASSIC' ELSE 'BATTLE' END,
  (SELECT id FROM topics ORDER BY id OFFSET ((n - 1) % 15) LIMIT 1),
  (SELECT id FROM exams  ORDER BY id OFFSET ((n - 1) % 30) LIMIT 1),
  (n % 3 = 0),
  (SELECT id FROM users WHERE type_account = 'TEACHER' AND username LIKE 'user%'
                        ORDER BY id OFFSET ((n - 1) % 20) LIMIT 1),
  CASE (n % 4) WHEN 0 THEN 'FINISHED' WHEN 1 THEN 'PLAYING' ELSE 'WAITING' END,
  10, 10, 30,
  (n % 4 = 0),
  NOW(), NOW()
FROM generate_series(1, 40) AS n
ON CONFLICT (room_code) DO NOTHING;

-- ---------------------------------------------------------------------
-- 11) Room players  (5 per room × 40 rooms = 200)
-- ---------------------------------------------------------------------
INSERT INTO room_players (room_id, user_id, is_host, join_order, status, time_taken, created_at, updated_at)
SELECT
  r.id,
  u.id,
  (gs = 1),
  gs,
  'ACTIVE',
  (gs * 12 + (r.rn % 7)),
  NOW(), NOW()
FROM (SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn FROM rooms) r
CROSS JOIN generate_series(1, 5) AS gs
CROSS JOIN LATERAL (
  SELECT id FROM users
  WHERE username LIKE 'user%' AND type_account = 'PLAYER'
  ORDER BY id
  OFFSET (((r.rn - 1) * 5 + gs - 1) % 100)
  LIMIT 1
) u
WHERE NOT EXISTS (SELECT 1 FROM room_players LIMIT 1)
ON CONFLICT (room_id, user_id) DO NOTHING;

-- ---------------------------------------------------------------------
-- 12) Game sessions  (one per room = 40)
-- ---------------------------------------------------------------------
INSERT INTO game_sessions (room_id, room_status, game_status, start_time, end_time, time_limit,
                           created_at, updated_at)
SELECT
  r.id,
  r.status,
  CASE r.status
    WHEN 'FINISHED' THEN 'COMPLETED'
    WHEN 'PLAYING'  THEN 'IN_PROGRESS'
    ELSE                 'WAITING'
  END,
  CASE WHEN r.status <> 'WAITING' THEN NOW() - INTERVAL '1 hour' END,
  CASE WHEN r.status = 'FINISHED' THEN NOW() - INTERVAL '30 minutes' END,
  30000000000,
  NOW(), NOW()
FROM rooms r
WHERE NOT EXISTS (SELECT 1 FROM game_sessions LIMIT 1);

-- ---------------------------------------------------------------------
-- 13) Game questions  (10 per session × 40 = 400)
-- ---------------------------------------------------------------------
INSERT INTO game_questions (game_session_id, question_id, question_order, time_limit,
                            created_at, updated_at)
SELECT
  gs.id,
  q.qid,
  q.q_order,
  30000000000,
  NOW(), NOW()
FROM game_sessions gs
JOIN rooms r ON r.id = gs.room_id
CROSS JOIN LATERAL (
  SELECT id AS qid, ROW_NUMBER() OVER (ORDER BY id) AS q_order
  FROM questions
  WHERE exam_id = r.exam_id
  ORDER BY id
  LIMIT 10
) q
WHERE NOT EXISTS (SELECT 1 FROM game_questions LIMIT 1)
ON CONFLICT (game_session_id, question_order) DO NOTHING;

-- ---------------------------------------------------------------------
-- 14) Game histories  (one per (session, player) = ~200)
-- ---------------------------------------------------------------------
INSERT INTO game_histories (game_session_id, user_id, score, correct_answers, total_questions,
                            created_at, updated_at)
SELECT
  gs.id,
  rp.user_id,
  ((gs.id * 37 + rp.user_id * 17) % 1000)::int,
  ((gs.id + rp.user_id) % 11)::int,
  10,
  NOW(), NOW()
FROM game_sessions gs
JOIN room_players rp ON rp.room_id = gs.room_id
WHERE NOT EXISTS (SELECT 1 FROM game_histories LIMIT 1)
ON CONFLICT (game_session_id, user_id) DO NOTHING;

-- ---------------------------------------------------------------------
-- 15) User answers  (4 per player per room × 200 = 800)
-- ---------------------------------------------------------------------
INSERT INTO user_answers (user_id, question_id, room_id, answer_id, is_correct, score,
                          time_taken, created_at, updated_at)
SELECT
  rp.user_id,
  q.qid,
  rp.room_id,
  a.aid,
  a.is_correct,
  CASE WHEN a.is_correct THEN 100 ELSE 0 END,
  (5 + ((rp.user_id + q.qid) % 25))::int,
  NOW(), NOW()
FROM room_players rp
JOIN rooms r ON r.id = rp.room_id
CROSS JOIN LATERAL (
  SELECT id AS qid
  FROM questions
  WHERE exam_id = r.exam_id
  ORDER BY id
  LIMIT 4
) q
CROSS JOIN LATERAL (
  SELECT id AS aid, is_correct
  FROM answers
  WHERE question_id = q.qid
  ORDER BY id
  OFFSET ((rp.user_id + q.qid) % 4)
  LIMIT 1
) a
WHERE NOT EXISTS (SELECT 1 FROM user_answers LIMIT 1)
ON CONFLICT (room_id, user_id, question_id) DO NOTHING;

-- ---------------------------------------------------------------------
-- 16) Player profiles  (one per seeded user = 120)
-- ---------------------------------------------------------------------
INSERT INTO player_profiles (user_id, age, average_score, total_play_time, created_at, updated_at)
SELECT
  u.id,
  (18 + (u.id % 40))::int,
  (100 + (u.id % 900))::double precision,
  ((u.id * 60) % 10000)::int,
  NOW(), NOW()
FROM users u
WHERE u.username LIKE 'user%'
ON CONFLICT (user_id) DO NOTHING;

-- ---------------------------------------------------------------------
-- 17) Ranks  (one per seeded user = 120)
-- ---------------------------------------------------------------------
INSERT INTO ranks (user_id, total_score, game_played, total_time, created_at, updated_at)
SELECT
  u.id,
  ((u.id * 23) % 5000)::int,
  (u.id % 50)::int,
  (u.id * 120)::bigint,
  NOW(), NOW()
FROM users u
WHERE u.username LIKE 'user%'
ON CONFLICT (user_id) DO NOTHING;
