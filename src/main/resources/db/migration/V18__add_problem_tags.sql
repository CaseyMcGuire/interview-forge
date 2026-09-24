CREATE TABLE tags (
  id BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_tags_name_unique ON tags (name);

CREATE TABLE problem_tags (
  id BIGSERIAL PRIMARY KEY,
  problem_id BIGINT NOT NULL,
  tag_id BIGINT NOT NULL,
  CONSTRAINT fk_problem_tags_problem_id
    FOREIGN KEY (problem_id) REFERENCES problems (id) ON DELETE CASCADE,
  CONSTRAINT fk_problem_tags_tag_id
    FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uq_problem_tags_problem_tag ON problem_tags (problem_id, tag_id);
CREATE INDEX idx_problem_tags_tag ON problem_tags (tag_id);
