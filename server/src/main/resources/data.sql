INSERT INTO users (
    id,
    email_address,
    password_hash,
    enabled
) VALUES
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