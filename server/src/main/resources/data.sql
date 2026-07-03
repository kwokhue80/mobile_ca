-- =====================================================
-- Test Seed Data For MobileCA Server
-- Purpose: provide realistic records for auth, chat, logs, activity, and dashboard flows.
-- =====================================================

-- -----------------------------------------------------
-- Section: users
-- Notes: UUID-style identifiers match CHAR(36) keys in schema. Test password in plain text: Password@123
-- -----------------------------------------------------
INSERT INTO users (id, email_address, password_hash, `enabled`) VALUES
('11111111-1111-1111-1111-111111111111', 'user1@test.com', '$2a$10$r3JNHwzHVFXKBwKpGO.mae9ss.giQCW9gN8NytwOHjswBsIS5E38m', TRUE),
('22222222-2222-2222-2222-222222222222', 'user2@test.com',   '$2a$10$r3JNHwzHVFXKBwKpGO.mae9ss.giQCW9gN8NytwOHjswBsIS5E38m', TRUE),
('33333333-3333-3333-3333-333333333333', 'disableduser@test.com',   '$2a$10$r3JNHwzHVFXKBwKpGO.mae9ss.giQCW9gN8NytwOHjswBsIS5E38m', FALSE);

-- -----------------------------------------------------
-- Section: user profiles
-- Notes: one profile record per user.
-- -----------------------------------------------------
INSERT INTO user_profiles (user_id, full_name, date_of_birth, gender, height_cm) VALUES
('11111111-1111-1111-1111-111111111111', 'Alice Tan', '1998-03-14', 'FEMALE', 162.50),
('22222222-2222-2222-2222-222222222222', 'Ben Lim',   '1995-11-22', 'MALE',   175.20),
('33333333-3333-3333-3333-333333333333', 'Cara Ng',   '2000-07-08', 'FEMALE', 168.00);

-- -----------------------------------------------------
-- Section: user goals
-- Notes: composite primary key supports multiple goal types per user.
-- -----------------------------------------------------
INSERT INTO user_goals (user_id, goal_type, target_value) VALUES
('11111111-1111-1111-1111-111111111111', 'STEPS', 9000.00),
('11111111-1111-1111-1111-111111111111', 'WATER_ML', 2200.00),
('22222222-2222-2222-2222-222222222222', 'STEPS', 7000.00),
('22222222-2222-2222-2222-222222222222', 'SLEEP_MINUTES', 450.00),
('33333333-3333-3333-3333-333333333333', 'CALORIES_BURNED', 500.00);

-- -----------------------------------------------------
-- Section: chat sessions and messages
-- Notes: explicit IDs keep references deterministic.
-- -----------------------------------------------------
INSERT INTO chat_sessions (id, user_id, title, is_active) VALUES
(1001, '11111111-1111-1111-1111-111111111111', 'Weekly Wellness Plan', TRUE),
(1002, '11111111-1111-1111-1111-111111111111', 'Meal Adjustments', TRUE),
(1003, '22222222-2222-2222-2222-222222222222', 'Sleep Improvement Tips', TRUE);

INSERT INTO chat_messages (id, session_id, sender_type, message_text) VALUES
(2001, 1001, 'USER', 'Daily step target missed for two consecutive days.'),
(2002, 1001, 'AI',   'Add a 20-minute evening walk after dinner for consistency.'),
(2003, 1002, 'USER', 'Afternoon energy dip after lunch.'),
(2004, 1002, 'AI',   'Reduce sugary drinks and add protein at lunch.'),
(2005, 1003, 'USER', 'Sleep duration under six hours during weekdays.'),
(2006, 1003, 'AI',   'Set fixed wind-down time at 10:30 PM and avoid screens.');

INSERT INTO chat_recommendations (id, message_id, recommendation_type, recommendation_text) VALUES
(3001, 2002, 'EXERCISE', 'Walk 20 minutes at moderate pace after dinner on weekdays.'),
(3002, 2004, 'NUTRITION', 'Target 25g protein during lunch and replace soda with water.'),
(3003, 2006, 'SLEEP', 'Use a consistent bedtime routine and cap caffeine after 2 PM.');

-- -----------------------------------------------------
-- Section: metric logs
-- Notes: values satisfy CHECK constraints in schema.
-- -----------------------------------------------------
INSERT INTO sleep_logs (id, user_id, start_time, end_time, sleep_quality_score) VALUES
(4001, '11111111-1111-1111-1111-111111111111', '2026-07-01 23:10:00', '2026-07-02 06:30:00', 78),
(4002, '22222222-2222-2222-2222-222222222222', '2026-07-01 00:20:00', '2026-07-01 06:05:00', 65),
(4003, '11111111-1111-1111-1111-111111111111', '2026-07-02 23:40:00', '2026-07-03 07:00:00', 83);

INSERT INTO food_logs (id, user_id, meal_type, food_name, calories_kcal, logged_at) VALUES
(5001, '11111111-1111-1111-1111-111111111111', 'BREAKFAST', 'Oatmeal with banana', 380, '2026-07-02 08:05:00'),
(5002, '11111111-1111-1111-1111-111111111111', 'LUNCH', 'Chicken rice', 650, '2026-07-02 12:30:00'),
(5003, '22222222-2222-2222-2222-222222222222', 'DINNER', 'Grilled salmon bowl', 540, '2026-07-02 19:15:00');

INSERT INTO weight_logs (id, user_id, weight_kg, logged_at) VALUES
(6001, '11111111-1111-1111-1111-111111111111', 58.40, '2026-07-02 07:10:00'),
(6002, '22222222-2222-2222-2222-222222222222', 73.20, '2026-07-02 07:25:00');

INSERT INTO hydration_logs (id, user_id, logged_at, volume_ml) VALUES
(7001, '11111111-1111-1111-1111-111111111111', '2026-07-02 09:20:00', 500),
(7002, '11111111-1111-1111-1111-111111111111', '2026-07-02 14:10:00', 350),
(7003, '22222222-2222-2222-2222-222222222222', '2026-07-02 16:40:00', 600);

INSERT INTO exercise_logs (id, user_id, exercise_type, duration_minutes, distance_km, calories_burned_kcal, logged_at) VALUES
(8001, '11111111-1111-1111-1111-111111111111', 'RUNNING', 35, 5.20, 340, '2026-07-02 18:20:00'),
(8002, '22222222-2222-2222-2222-222222222222', 'CYCLING', 45, 14.80, 420, '2026-07-02 06:50:00'),
(8003, '11111111-1111-1111-1111-111111111111', 'YOGA', 25, NULL, 120, '2026-07-03 06:40:00');

INSERT INTO mood_logs (id, user_id, logged_at, mood_rating, notes) VALUES
(9001, '11111111-1111-1111-1111-111111111111', '2026-07-02 21:00:00', 4, 'Good focus across work and training.'),
(9002, '22222222-2222-2222-2222-222222222222', '2026-07-02 22:10:00', 3, 'Low energy after short sleep duration.'),
(9003, '11111111-1111-1111-1111-111111111111', '2026-07-03 21:05:00', 5, 'Strong recovery and steady hydration.');

-- -----------------------------------------------------
-- Section: homepage activity feed
-- Notes: source_log_id maps to corresponding metric log IDs.
-- -----------------------------------------------------
INSERT INTO activity_logs (id, user_id, source_log_id, activity_type, title, `description`, recorded_at) VALUES
(10001, '11111111-1111-1111-1111-111111111111', 7001, 'HYDRATION', 'Water intake logged', '500 ml recorded', '2026-07-02 09:20:00'),
(10002, '11111111-1111-1111-1111-111111111111', 8001, 'EXERCISE',  'Evening run logged', '5.20 km in 35 minutes', '2026-07-02 18:20:00'),
(10003, '11111111-1111-1111-1111-111111111111', 4001, 'SLEEP',     'Night sleep recorded', '440 minutes, quality 78', '2026-07-02 06:30:00'),
(10004, '22222222-2222-2222-2222-222222222222', 9002, 'MOOD',      'Mood check-in logged', 'Rating 3 with short-sleep note', '2026-07-02 22:10:00');

-- -----------------------------------------------------
-- Section: daily dashboard summary
-- Notes: one summary row per user per date.
-- -----------------------------------------------------
INSERT INTO daily_wellness_summary (
	id,
	user_id,
	summary_date,
	total_water_ml,
	total_calories_intake,
	total_calories_burned,
	total_exercise_minutes,
	sleep_minutes,
	sleep_quality_score,
	mood_score,
	weight_kg
) VALUES
(11001, '11111111-1111-1111-1111-111111111111', '2026-07-02', 1800, 1680, 460, 60, 440, 78, 4, 58.40),
(11002, '22222222-2222-2222-2222-222222222222', '2026-07-02', 1400, 1540, 420, 45, 345, 65, 3, 73.20);

