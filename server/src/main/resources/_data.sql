-- 1. Users
INSERT INTO
    users (
        id,
        email_address,
        password_hash,
        enabled
    )
VALUES
    (
        '11111111-1111-1111-1111-111111111111',
        'user1@test.com',
        '$2a$10$r3JNHwzHVFXKBwKpGO.mae9ss.giQCW9gN8NytwOHjswBsIS5E38m',
        true
    ),
    (
        '22222222-2222-2222-2222-222222222222',
        'user2@test.com',
        '$2a$10$r3JNHwzHVFXKBwKpGO.mae9ss.giQCW9gN8NytwOHjswBsIS5E38m',
        true
    ),
    (
        '33333333-3333-3333-3333-333333333333',
        'disableduser@test.com',
        '$2a$10$r3JNHwzHVFXKBwKpGO.mae9ss.giQCW9gN8NytwOHjswBsIS5E38m',
        false
    );

-- 2. User Profiles
INSERT INTO
    user_profiles (
        id,
        user_id,
        full_name,
        date_of_birth,
        gender,
        height_cm
    )
VALUES
    (
        'aaaaa001-0000-0000-0000-000000000000',
        '11111111-1111-1111-1111-111111111111',
        'Alice Smith',
        '1990-05-15',
        'FEMALE',
        165.0
    ),
    (
        'aaaaa002-0000-0000-0000-000000000000',
        '22222222-2222-2222-2222-222222222222',
        'Bob Jones',
        '1985-11-20',
        'MALE',
        180.5
    );

-- 3. User Goals (Note: Adhering to the UNIQUE constraint on user_id + goal_type)
INSERT INTO
    user_goals (id, user_id, goal_type, target_value)
VALUES
    (
        'bbbbb001-0000-0000-0000-000000000000',
        '11111111-1111-1111-1111-111111111111',
        'STEPS',
        10000
    ),
    (
        'bbbbb002-0000-0000-0000-000000000000',
        '11111111-1111-1111-1111-111111111111',
        'WATER_ML',
        2500
    ),
    (
        'bbbbb003-0000-0000-0000-000000000000',
        '22222222-2222-2222-2222-222222222222',
        'EXERCISE_DAYS',
        4
    );

-- 4. Wellness Records (Static daily metrics)
INSERT INTO
    wellness_records (
        id,
        user_id,
        logical_date,
        water_intake_ml,
        stress_level,
        weight_kg
    )
VALUES
    (
        'ccccc001-0000-0000-0000-000000000000',
        '11111111-1111-1111-1111-111111111111',
        '2026-07-01',
        1800,
        3,
        65.5
    ),
    (
        'ccccc002-0000-0000-0000-000000000000',
        '11111111-1111-1111-1111-111111111111',
        '2026-07-02',
        2100,
        2,
        65.3
    ),
    (
        'ccccc003-0000-0000-0000-000000000000',
        '22222222-2222-2222-2222-222222222222',
        '2026-07-02',
        1500,
        4,
        82.1
    );

-- 5. Sleep Sessions (Chronological tracking)
INSERT INTO
    sleep_sessions (
        id,
        user_id,
        logical_date,
        start_time,
        end_time,
        sleep_quality
    )
VALUES
    (
        'ddddd001-0000-0000-0000-000000000000',
        '11111111-1111-1111-1111-111111111111',
        '2026-07-01',
        '2026-06-30 23:00:00',
        '2026-07-01 07:00:00',
        4
    ),
    (
        'ddddd002-0000-0000-0000-000000000000',
        '11111111-1111-1111-1111-111111111111',
        '2026-07-02',
        '2026-07-01 22:30:00',
        '2026-07-02 06:45:00',
        5
    );

-- 6. Meal Sessions
INSERT INTO
    meal_sessions (
        id,
        user_id,
        logical_date,
        meal_type,
        food_items,
        calories_cal,
        protein_g,
        carbs_g,
        fat_g
    )
VALUES
    (
        'eeeee001-0000-0000-0000-000000000000',
        '11111111-1111-1111-1111-111111111111',
        '2026-07-02',
        'BREAKFAST',
        'Oatmeal, Banana, Coffee',
        350,
        10,
        60,
        5
    ),
    (
        'eeeee002-0000-0000-0000-000000000000',
        '11111111-1111-1111-1111-111111111111',
        '2026-07-02',
        'LUNCH',
        'Grilled Chicken Salad',
        450,
        40,
        15,
        20
    );

-- 7. Exercise Sessions
INSERT INTO
    exercise_sessions (
        id,
        user_id,
        logical_date,
        exercise_type,
        distance_km,
        duration_mins,
        calories_burned
    )
VALUES
    (
        'fffff001-0000-0000-0000-000000000000',
        '11111111-1111-1111-1111-111111111111',
        '2026-07-01',
        'RUNNING',
        5.2,
        30,
        320
    ),
    (
        'fffff002-0000-0000-0000-000000000000',
        '22222222-2222-2222-2222-222222222222',
        '2026-07-02',
        'CYCLING',
        15.0,
        45,
        450
    );

-- 8. Chat Sessions
INSERT INTO
    chat_sessions (id, user_id, title, is_active)
VALUES
    (
        '99999999-0000-0000-0000-000000000001',
        '11111111-1111-1111-1111-111111111111',
        'Hydration Advice',
        true
    );

-- 9. Chat Messages
INSERT INTO
    chat_messages (id, session_id, sender_type, message_text)
VALUES
    (
        '88888888-0000-0000-0000-000000000001',
        '99999999-0000-0000-0000-000000000001',
        'USER',
        'I keep getting headaches in the afternoon.'
    ),
    (
        '88888888-0000-0000-0000-000000000002',
        '99999999-0000-0000-0000-000000000001',
        'AI',
        'Headaches can often be a sign of dehydration. Based on your records, you only drank 1500ml yesterday. I suggest increasing your water intake today.'
    );

-- 10. Chat Recommendations (Tied to the AI message above)
INSERT INTO
    chat_recommendations (
        id,
        message_id,
        recommendation_type,
        recommendation_text
    )
VALUES
    (
        '77777777-0000-0000-0000-000000000001',
        '88888888-0000-0000-0000-000000000002',
        'NUTRITION',
        'Drink a glass of water right now.'
    );