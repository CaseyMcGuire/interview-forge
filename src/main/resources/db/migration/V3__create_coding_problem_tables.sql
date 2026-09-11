CREATE TABLE problems (
  id BIGSERIAL PRIMARY KEY,
  slug TEXT NOT NULL,
  title TEXT NOT NULL,
  statement_markdown TEXT NOT NULL,
  difficulty TEXT NOT NULL,
  checker_kind TEXT NOT NULL DEFAULT 'EXACT_JSON',
  created_by_user_id BIGINT NOT NULL,
  published_at TIMESTAMPTZ,
  archived_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_problems_created_by_user_id
    FOREIGN KEY (created_by_user_id) REFERENCES users (id) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX idx_problems_slug_unique ON problems (slug);
CREATE INDEX idx_problems_created_by_user ON problems (created_by_user_id);

CREATE TABLE languages (
  id BIGSERIAL PRIMARY KEY,
  key TEXT NOT NULL,
  display_name TEXT NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_languages_key_unique ON languages (key);

CREATE TABLE problem_languages (
  id BIGSERIAL PRIMARY KEY,
  problem_id BIGINT NOT NULL,
  language_id BIGINT NOT NULL,
  starter_code TEXT NOT NULL,
  solution_filename TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_problem_languages_problem_id
    FOREIGN KEY (problem_id) REFERENCES problems (id) ON DELETE RESTRICT,
  CONSTRAINT fk_problem_languages_language_id
    FOREIGN KEY (language_id) REFERENCES languages (id) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uq_problem_languages_problem_language
  ON problem_languages (problem_id, language_id);
CREATE INDEX idx_problem_languages_language ON problem_languages (language_id);

CREATE TABLE judge_configurations (
  id BIGSERIAL PRIMARY KEY,
  problem_language_id BIGINT NOT NULL,
  runtime_key TEXT NOT NULL,
  harness_source TEXT NOT NULL,
  checker_source TEXT,
  time_limit_ms INTEGER NOT NULL,
  memory_limit_mb INTEGER NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_judge_configurations_problem_language_id
    FOREIGN KEY (problem_language_id) REFERENCES problem_languages (id) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX idx_judge_configurations_problem_language_id_unique
  ON judge_configurations (problem_language_id);

CREATE TABLE test_cases (
  id BIGSERIAL PRIMARY KEY,
  problem_id BIGINT NOT NULL,
  position INTEGER NOT NULL,
  visibility TEXT NOT NULL,
  input_json JSONB NOT NULL,
  expected_output_json JSONB NOT NULL,
  explanation_markdown TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_test_cases_problem_id
    FOREIGN KEY (problem_id) REFERENCES problems (id) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uq_test_cases_problem_position ON test_cases (problem_id, position);

CREATE TABLE submissions (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  problem_id BIGINT NOT NULL,
  problem_language_id BIGINT NOT NULL,
  runtime_key TEXT NOT NULL,
  source_code TEXT NOT NULL,
  kind TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'QUEUED',
  verdict TEXT,
  total_cases INTEGER NOT NULL,
  passed_cases INTEGER NOT NULL DEFAULT 0,
  runtime_ms BIGINT,
  peak_memory_mb INTEGER,
  public_error_message TEXT,
  started_at TIMESTAMPTZ,
  finished_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_submissions_user_id
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
  CONSTRAINT fk_submissions_problem_id
    FOREIGN KEY (problem_id) REFERENCES problems (id) ON DELETE RESTRICT,
  CONSTRAINT fk_submissions_problem_language_id
    FOREIGN KEY (problem_language_id) REFERENCES problem_languages (id) ON DELETE RESTRICT
);

CREATE INDEX idx_submissions_user_created_at ON submissions (user_id, created_at);
CREATE INDEX idx_submissions_user_problem_created_at ON submissions (user_id, problem_id, created_at);
CREATE INDEX idx_submissions_problem ON submissions (problem_id);
CREATE INDEX idx_submissions_problem_language ON submissions (problem_language_id);

CREATE TABLE submission_test_results (
  id BIGSERIAL PRIMARY KEY,
  submission_id BIGINT NOT NULL,
  test_case_id BIGINT,
  position INTEGER NOT NULL,
  source TEXT NOT NULL,
  input_json JSONB NOT NULL,
  expected_output_json JSONB,
  outcome TEXT NOT NULL DEFAULT 'PENDING',
  actual_output_json JSONB,
  stdout TEXT,
  stderr TEXT,
  runtime_ms BIGINT,
  peak_memory_mb INTEGER,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_submission_test_results_submission_id
    FOREIGN KEY (submission_id) REFERENCES submissions (id) ON DELETE RESTRICT,
  CONSTRAINT fk_submission_test_results_test_case_id
    FOREIGN KEY (test_case_id) REFERENCES test_cases (id) ON DELETE SET NULL
);

CREATE UNIQUE INDEX uq_submission_test_results_submission_position
  ON submission_test_results (submission_id, position);
CREATE INDEX idx_submission_test_results_test_case ON submission_test_results (test_case_id);
