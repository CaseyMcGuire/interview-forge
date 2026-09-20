ALTER TABLE custom_test_suite_runs
  ADD COLUMN user_id BIGINT,
  ADD COLUMN problem_language_id BIGINT,
  ADD COLUMN expires_at TIMESTAMPTZ;

UPDATE custom_test_suite_runs AS run
SET user_id = suite.user_id,
    problem_language_id = suite.problem_language_id,
    expires_at = suite.expires_at
FROM custom_test_suites AS suite
WHERE suite.id = run.custom_test_suite_id;

ALTER TABLE custom_test_suite_runs
  ALTER COLUMN user_id SET NOT NULL,
  ALTER COLUMN problem_language_id SET NOT NULL,
  ALTER COLUMN expires_at SET NOT NULL,
  ADD CONSTRAINT fk_custom_test_suite_runs_user_id
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
  ADD CONSTRAINT fk_custom_test_suite_runs_problem_language_id
    FOREIGN KEY (problem_language_id) REFERENCES problem_languages (id) ON DELETE RESTRICT;

-- Keep case IDs and run IDs stable; retained results and grading jobs already reference them.
ALTER TABLE custom_test_cases ADD COLUMN custom_test_suite_run_id BIGINT;

UPDATE custom_test_cases AS test_case
SET custom_test_suite_run_id = run.id
FROM custom_test_suite_runs AS run
WHERE run.custom_test_suite_id = test_case.custom_test_suite_id;

ALTER TABLE custom_test_cases
  ALTER COLUMN custom_test_suite_run_id SET NOT NULL,
  ADD CONSTRAINT fk_custom_test_cases_custom_test_suite_run_id
    FOREIGN KEY (custom_test_suite_run_id) REFERENCES custom_test_suite_runs (id) ON DELETE CASCADE,
  DROP COLUMN custom_test_suite_id;

CREATE UNIQUE INDEX uq_custom_test_cases_run_position ON custom_test_cases (custom_test_suite_run_id, position);
CREATE INDEX idx_custom_test_suite_runs_user ON custom_test_suite_runs (user_id);
CREATE INDEX idx_custom_test_suite_runs_problem_language ON custom_test_suite_runs (problem_language_id);
CREATE INDEX idx_custom_test_suite_runs_expires_at ON custom_test_suite_runs (expires_at);

ALTER TABLE custom_test_suite_runs DROP COLUMN custom_test_suite_id;
DROP TABLE custom_test_suites;
