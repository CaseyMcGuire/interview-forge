ALTER TABLE users ADD COLUMN role TEXT NOT NULL DEFAULT 'USER';
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('USER', 'ADMIN'));

-- The initial authoring form provides Kotlin starter code.
INSERT INTO languages (key, display_name)
VALUES ('kotlin', 'Kotlin')
ON CONFLICT (key) DO NOTHING;
