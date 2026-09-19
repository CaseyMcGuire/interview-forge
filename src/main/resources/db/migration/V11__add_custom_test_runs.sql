ALTER TABLE judge_configurations ADD COLUMN reference_solution_code TEXT;

CREATE TABLE custom_test_suites (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  problem_language_id BIGINT NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_custom_test_suites_user_id
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
  CONSTRAINT fk_custom_test_suites_problem_language_id
    FOREIGN KEY (problem_language_id) REFERENCES problem_languages (id) ON DELETE RESTRICT
);

CREATE INDEX idx_custom_test_suites_user ON custom_test_suites (user_id);
CREATE INDEX idx_custom_test_suites_problem_language ON custom_test_suites (problem_language_id);
CREATE INDEX idx_custom_test_suites_expires_at ON custom_test_suites (expires_at);

CREATE TABLE custom_test_cases (
  id BIGSERIAL PRIMARY KEY,
  custom_test_suite_id BIGINT NOT NULL,
  position INTEGER NOT NULL,
  input_json JSONB NOT NULL,
  expected_output_json JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_custom_test_cases_custom_test_suite_id
    FOREIGN KEY (custom_test_suite_id) REFERENCES custom_test_suites (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uq_custom_test_cases_suite_position ON custom_test_cases (custom_test_suite_id, position);

CREATE TABLE test_runs (
  id BIGSERIAL PRIMARY KEY,
  custom_test_suite_id BIGINT NOT NULL,
  source_code TEXT NOT NULL,
  total_cases INTEGER NOT NULL,
  passed_cases INTEGER NOT NULL DEFAULT 0,
  runtime_ms BIGINT,
  public_error_message TEXT,
  started_at TIMESTAMPTZ,
  finished_at TIMESTAMPTZ,
  status TEXT NOT NULL DEFAULT 'QUEUED',
  outcome TEXT,
  case_results JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_test_runs_custom_test_suite_id
    FOREIGN KEY (custom_test_suite_id) REFERENCES custom_test_suites (id) ON DELETE CASCADE
);

CREATE INDEX idx_test_runs_suite_status ON test_runs (custom_test_suite_id, status);
CREATE INDEX idx_test_runs_status_created_at ON test_runs (status, created_at);
