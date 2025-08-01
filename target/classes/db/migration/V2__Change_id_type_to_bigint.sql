-- 1. Удаляем старый DEFAULT, если есть
ALTER TABLE users ALTER COLUMN id DROP DEFAULT;

-- 2. Меняем тип на BIGINT
ALTER TABLE users ALTER COLUMN id TYPE BIGINT;

-- 3. Создаём новую последовательность, если нужно
CREATE SEQUENCE IF NOT EXISTS users_id_seq;

-- 4. Привязываем последовательность к колонке
ALTER TABLE users ALTER COLUMN id SET DEFAULT nextval('users_id_seq');

-- 5. Устанавливаем текущее значение последовательности (по max id)
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
