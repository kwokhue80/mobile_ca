-- 1. Drop Tables In Reverse Order of Dependencies
DROP TABLE IF EXISTS daily_wellness_summary;
DROP TABLE IF EXISTS activity_records;
DROP TABLE IF EXISTS mood_logs;
DROP TABLE IF EXISTS exercise_logs;
DROP TABLE IF EXISTS hydration_logs;
DROP TABLE IF EXISTS weight_logs;
DROP TABLE IF EXISTS food_logs;
DROP TABLE IF EXISTS sleep_logs;
DROP TABLE IF EXISTS chat_recommendations;
DROP TABLE IF EXISTS chat_messages;
DROP TABLE IF EXISTS chat_sessions;
DROP TABLE IF EXISTS user_goals;
DROP TABLE IF EXISTS user_profiles;
DROP TABLE IF EXISTS users;

-- 2. Create Tables In Order of Dependencies
-- 2.1 User tables
CREATE TABLE users (
    id							CHAR(36)		NOT NULL,
    email_address				VARCHAR(100)	NOT NULL,
    password_hash				VARCHAR(255)	NOT NULL,
    `enabled`					BOOLEAN			NOT NULL DEFAULT TRUE,
    created_at					DATETIME		NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at					DATETIME		NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT uk_users_email_address UNIQUE (email_address)
);

CREATE TABLE user_profiles (
    user_id						CHAR(36)		NOT NULL,
    full_name					VARCHAR(100)	NOT NULL,
    date_of_birth				DATE			NOT NULL,
    gender						VARCHAR(20)		NOT NULL,
    height_cm					DECIMAL(5,2)	NOT NULL,
    created_at					DATETIME		NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at					DATETIME		NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id),
    CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_profile_height CHECK (height_cm > 0)
);

CREATE TABLE user_goals (
    user_id						CHAR(36)		NOT NULL,
    goal_type					VARCHAR(50)		NOT NULL,
    target_value				DECIMAL(8,2)	NOT NULL,
    created_at					DATETIME		NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at					DATETIME		NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, goal_type),
    CONSTRAINT fk_user_goals_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_user_goals_target CHECK (target_value > 0)
);

-- 2.2 Chat tables
CREATE TABLE chat_sessions (
    id							BIGINT			NOT NULL AUTO_INCREMENT,
    user_id						CHAR(36)		NOT NULL,
    title						VARCHAR(255)	DEFAULT 'New Chat',
    is_active					BOOLEAN			NOT NULL DEFAULT TRUE,
    created_at					DATETIME		NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at					DATETIME		NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_chat_session_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE chat_messages (
    id							BIGINT			NOT NULL AUTO_INCREMENT,
    session_id					BIGINT			NOT NULL,
    sender_type					VARCHAR(50)		NOT NULL,
    message_text				TEXT			NOT NULL,
    created_at					DATETIME		NOT NULL DEFAULT CURRENT_TIMESTAMP, -- not updatable

    PRIMARY KEY (id),
    CONSTRAINT fk_message_session FOREIGN KEY (session_id) REFERENCES chat_sessions(id) ON DELETE CASCADE
);

CREATE TABLE chat_recommendations (
    id							BIGINT			NOT NULL AUTO_INCREMENT,
    message_id					BIGINT			NOT NULL,
    recommendation_type			VARCHAR(100)	NOT NULL,
    recommendation_text			TEXT			NOT NULL,
    created_at					DATETIME		NOT NULL DEFAULT CURRENT_TIMESTAMP, -- not updatable

    PRIMARY KEY (id),
    CONSTRAINT fk_recommendation_message FOREIGN KEY (message_id) REFERENCES chat_messages(id) ON DELETE CASCADE
);

-- 2.3 Metric tables
CREATE TABLE sleep_logs (
    id							BIGINT			NOT NULL AUTO_INCREMENT,
    user_id						CHAR(36)		NOT NULL,
    start_time					DATETIME		NOT NULL,
    end_time					DATETIME		NOT NULL,
    duration_minutes			INT GENERATED ALWAYS AS (TIMESTAMPDIFF(MINUTE, start_time, end_time)) STORED,
    sleep_quality_score			INT				DEFAULT NULL,
    created_at					DATETIME		NOT NULL DEFAULT CURRENT_TIMESTAMP, -- not updatable

    PRIMARY KEY (id),
    CONSTRAINT fk_sleep_logs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_sleep_time CHECK (end_time > start_time),
    CONSTRAINT chk_sleep_quality CHECK (sleep_quality_score IS NULL OR sleep_quality_score BETWEEN 1 AND 10)
);

CREATE TABLE food_logs (
    id							BIGINT			NOT NULL AUTO_INCREMENT,
    user_id						CHAR(36)		NOT NULL,
    meal_type					VARCHAR(20)     DEFAULT NULL,
    food_name					VARCHAR(255)	NOT NULL,
    calories_kcal				INT				NOT NULL,
    logged_at					DATETIME		NOT NULL,
    created_at					DATETIME		NOT NULL DEFAULT CURRENT_TIMESTAMP, -- not updatable

    PRIMARY KEY (id),
    CONSTRAINT fk_food_logs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_food_calories CHECK (calories_kcal >= 0)
);

CREATE TABLE weight_logs (
    id							BIGINT			NOT NULL AUTO_INCREMENT,
    user_id						CHAR(36)		NOT NULL,
    weight_kg					DECIMAL(5,2)	NOT NULL,
    logged_at					DATETIME		NOT NULL,
    created_at					DATETIME		NOT NULL DEFAULT CURRENT_TIMESTAMP, -- not updatable

    PRIMARY KEY (id),
    CONSTRAINT fk_weight_logs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_weight_value CHECK (weight_kg > 0)
);

CREATE TABLE hydration_logs (
    id							BIGINT			NOT NULL AUTO_INCREMENT,
    user_id						CHAR(36)		NOT NULL,
    logged_at					DATETIME		NOT NULL,
    volume_ml					INT				NOT NULL,
    created_at					DATETIME		NOT NULL DEFAULT CURRENT_TIMESTAMP, -- not updatable

    PRIMARY KEY (id),
    CONSTRAINT fk_hydration_logs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_hydration_volume CHECK (volume_ml > 0)
);

CREATE TABLE exercise_logs (
    id							BIGINT			NOT NULL AUTO_INCREMENT,
    user_id						CHAR(36)		NOT NULL,
    exercise_type				VARCHAR(100)	NOT NULL,
    duration_minutes			INT				NOT NULL,
    distance_km					DECIMAL(6,2)    DEFAULT NULL,
    calories_burned_kcal		INT				NOT NULL,
    logged_at					DATETIME		NOT NULL,
    start_time					DATETIME		DEFAULT NULL,
    end_time					DATETIME		DEFAULT NULL,
    created_at					DATETIME		NOT NULL DEFAULT CURRENT_TIMESTAMP, -- not updatable

    PRIMARY KEY (id),
    CONSTRAINT fk_exercise_logs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_exercise_duration CHECK (duration_minutes > 0),
    CONSTRAINT chk_exercise_distance CHECK (distance_km IS NULL OR distance_km >= 0),
    CONSTRAINT chk_exercise_calories CHECK (calories_burned_kcal >= 0)
);

CREATE TABLE mood_logs (
    id							BIGINT			NOT NULL AUTO_INCREMENT,
    user_id						CHAR(36)		NOT NULL,
    logged_at					DATETIME		NOT NULL,
    mood_rating					INT				NOT NULL,
    notes						VARCHAR(255)    DEFAULT NULL,
    created_at					DATETIME		NOT NULL DEFAULT CURRENT_TIMESTAMP, -- not updatable

    PRIMARY KEY (id),
    CONSTRAINT fk_mood_logs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_mood_rating CHECK (mood_rating BETWEEN 1 AND 10)
);

-- 2.4 Homepage activity and dashboard summary
CREATE TABLE activity_records (
    id							BIGINT			NOT NULL AUTO_INCREMENT,
    user_id						CHAR(36)		NOT NULL,
    source_log_id				BIGINT			NOT NULL, -- References specific log
    activity_type				VARCHAR(50)		NOT NULL, -- e.g. HYDRATION
    title						VARCHAR(100)	NOT NULL, -- e.g. Water logged
    `description`				VARCHAR(255)    DEFAULT NULL, -- e.g. 500 ml (stored here for easy lookup)
    recorded_at					DATETIME		NOT NULL,
    created_at					DATETIME		NOT NULL DEFAULT CURRENT_TIMESTAMP, -- not updatable

    PRIMARY KEY (id),
    CONSTRAINT fk_activity_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE daily_wellness_summary (
    id							BIGINT			NOT NULL AUTO_INCREMENT,
    user_id						CHAR(36)		NOT NULL,
    summary_date				DATE			NOT NULL,
    total_water_ml				INT				NOT NULL DEFAULT 0,
    total_calories_intake		INT				NOT NULL DEFAULT 0,
    total_calories_burned		INT				NOT NULL DEFAULT 0,
    total_exercise_minutes		INT				NOT NULL DEFAULT 0,
    total_distance_km			DECIMAL(6,2)	NOT NULL DEFAULT 0,
    sleep_minutes				INT				DEFAULT NULL,
    sleep_quality_score			DECIMAL(4,2)	DEFAULT NULL,
    mood_score					DECIMAL(4,2)    DEFAULT NULL,
    weight_kg					DECIMAL(5,2)	DEFAULT NULL,
    created_at					DATETIME		NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at					DATETIME		NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_summary_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_summary_user_date UNIQUE (user_id, summary_date),
    CONSTRAINT chk_summary_water CHECK (total_water_ml >= 0),
    CONSTRAINT chk_summary_calories_intake CHECK (total_calories_intake >= 0),
    CONSTRAINT chk_summary_calories_burned CHECK (total_calories_burned >= 0),
    CONSTRAINT chk_summary_exercise_minutes CHECK (total_exercise_minutes >= 0),
    CONSTRAINT chk_summary_distance CHECK (total_distance_km >= 0),
    CONSTRAINT chk_summary_sleep_minutes CHECK (sleep_minutes IS NULL OR sleep_minutes >= 0),
    CONSTRAINT chk_summary_sleep_quality CHECK (sleep_quality_score IS NULL OR sleep_quality_score BETWEEN 1 AND 10),
    CONSTRAINT chk_summary_mood CHECK (mood_score IS NULL OR mood_score BETWEEN 1 AND 10),
    CONSTRAINT chk_summary_weight CHECK (weight_kg IS NULL OR weight_kg > 0)
);

-- 3. Indexes for quick lookup
CREATE INDEX idx_chat_sessions_user_time ON chat_sessions (user_id, created_at DESC);
CREATE INDEX idx_chat_messages_session_time ON chat_messages (session_id, created_at ASC);

CREATE INDEX idx_sleep_user_time ON sleep_logs (user_id, start_time DESC);
CREATE INDEX idx_food_user_time ON food_logs (user_id, logged_at DESC);
CREATE INDEX idx_weight_user_time ON weight_logs (user_id, logged_at DESC);
CREATE INDEX idx_hydration_user_time ON hydration_logs (user_id, logged_at DESC);
CREATE INDEX idx_exercise_user_time ON exercise_logs (user_id, logged_at DESC);
CREATE INDEX idx_mood_user_time ON mood_logs (user_id, logged_at DESC);

CREATE INDEX idx_activity_user_time ON activity_records (user_id, recorded_at DESC);
CREATE INDEX idx_activity_user_type_time ON activity_records (user_id, activity_type, recorded_at DESC);