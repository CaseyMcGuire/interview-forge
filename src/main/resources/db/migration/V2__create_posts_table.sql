CREATE TABLE IF NOT EXISTS posts (
  id          BIGSERIAL PRIMARY KEY,
  title       TEXT      NOT NULL,
  content     TEXT      NOT NULL,
  created_at  TIMESTAMP NOT NULL,
  updated_at  TIMESTAMP NOT NULL,
  user_id     BIGINT    NOT NULL REFERENCES users(id)
);