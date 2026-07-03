-- 1. Drop Tables In Reverse Order of Dependencies
DROP TABLE IF EXISTS exercise_sessions;
DROP TABLE IF EXISTS meal_sessions;
DROP TABLE IF EXISTS sleep_sessions;
DROP TABLE IF EXISTS wellness_records;
DROP TABLE IF EXISTS chat_recommendations;
DROP TABLE IF EXISTS chat_messages;
DROP TABLE IF EXISTS chat_sessions;
DROP TABLE IF EXISTS user_goals;
DROP TABLE IF EXISTS user_profiles;
DROP TABLE IF EXISTS users;

-- 2. Create Tables In Order of Dependencies (users -> user_profiles -> user_goals -> chat_sessions -> chat_messages -> chat_recommendations -> wellness_records -> sleep_sessions -> meal_sessions -> exercise_sessions)
CREATE TABLE users (
    id                  CHAR(36)        NOT NULL,
    email_address       VARCHAR(100)    NOT NULL,
    password_hash       VARCHAR(255)    NOT NULL,
    `enabled`           BOOLEAN         NOT NULL        DEFAULT TRUE,
    created_at          DATETIME        NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL        DEFAULT CURRENT_TIMESTAMP       ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT uk_users_email_address   UNIQUE (email_address)
);

CREATE TABLE user_profiles (
    id                  CHAR(36)        NOT NULL,
    user_id             CHAR(36)        NOT NULL,
    full_name           VARCHAR(100)    NOT NULL,
    date_of_birth       DATE            NOT NULL,
    gender              VARCHAR(20)     NOT NULL,
    height_cm           DECIMAL(5,2)    NOT NULL,
    
    created_at          DATETIME        NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_profile_user UNIQUE (user_id)
);

CREATE TABLE user_goals (
    id                  CHAR(36)        NOT NULL,
    user_id             CHAR(36)        NOT NULL,
    goal_type           VARCHAR(50)     NOT NULL,       -- e.g., 'STEPS', 'CALORIES', 'DISTANCE', 'EXERCISE_DAYS'
    target_value        DECIMAL(8,2)    NOT NULL,
    
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_user_goals_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_goals_user_type UNIQUE (user_id, goal_type) 
);

CREATE TABLE chat_sessions (
    id                  CHAR(36)        NOT NULL,
    user_id             CHAR(36)        NOT NULL,
    title               VARCHAR(255)    DEFAULT 'New Chat',
    is_active           BOOLEAN         NOT NULL        DEFAULT TRUE,
    
    created_at          DATETIME        NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_chat_session_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE chat_messages (
    id                  CHAR(36)        NOT NULL,
    session_id          CHAR(36)        NOT NULL,
    sender_type         VARCHAR(50)     NOT NULL,       -- e.g., 'USER', 'AI', 'SYSTEM'
    message_text        TEXT            NOT NULL,
    
    created_at          DATETIME        NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_message_session FOREIGN KEY (session_id) REFERENCES chat_sessions(id) ON DELETE CASCADE
);

CREATE TABLE chat_recommendations (
    id                  CHAR(36)        NOT NULL,
    message_id          CHAR(36)        NOT NULL,
    recommendation_type VARCHAR(100)    NOT NULL,       -- e.g., 'NUTRITION', 'EXERCISE', 'SLEEP'
    recommendation_text TEXT            NOT NULL,
    
    created_at          DATETIME        NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_recommendation_message FOREIGN KEY (message_id) REFERENCES chat_messages(id) ON DELETE CASCADE
);

CREATE TABLE wellness_records (
    id                  CHAR(36)        NOT NULL,
    user_id             CHAR(36)        NOT NULL,
    logical_date        DATE            NOT NULL,
    water_intake_ml     INT             DEFAULT 0,
    stress_level        INT             DEFAULT NULL,   -- e.g., scale of 1-5
    weight_kg           DECIMAL(5,2)    DEFAULT NULL,
    
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_wellness_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_wellness_user_date UNIQUE (user_id, logical_date) 
);

CREATE TABLE sleep_sessions (
    id                  CHAR(36)        NOT NULL,
    user_id             CHAR(36)        NOT NULL,
    logical_date        DATE            NOT NULL,
    start_time          DATETIME        NOT NULL,
    end_time            DATETIME        NOT NULL,
    sleep_quality       INT             DEFAULT NULL,   -- e.g., scale of 1-5
    
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_sleep_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE meal_sessions (
    id                  CHAR(36)        NOT NULL,
    user_id             CHAR(36)        NOT NULL,
    logical_date        DATE            NOT NULL,
    meal_type           VARCHAR(50)     NOT NULL,       -- e.g., 'BREAKFAST', 'LUNCH', 'DINNER', 'SNACK'
    food_items          TEXT            NOT NULL,       -- comma-separated list of food items
    calories_cal        DECIMAL(8,2)    DEFAULT NULL,
    protein_g           DECIMAL(8,2)    DEFAULT NULL,
    carbs_g             DECIMAL(8,2)    DEFAULT NULL,
    fat_g               DECIMAL(8,2)    DEFAULT NULL,

    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_meals_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE exercise_sessions (
    id                  CHAR(36)        NOT NULL,
    user_id             CHAR(36)        NOT NULL,
    logical_date        DATE            NOT NULL,
    exercise_type       VARCHAR(100)    NOT NULL,       -- e.g., 'RUNNING', 'CYCLING', 'SWIMMING'
    distance_km         DECIMAL(8,2)    NOT NULL,
    duration_mins       INT             NOT NULL,
    calories_burned     DECIMAL(8,2)    DEFAULT NULL,

    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_exercise_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);