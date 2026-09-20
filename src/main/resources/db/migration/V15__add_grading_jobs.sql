CREATE TABLE grading_jobs (
  id BIGSERIAL PRIMARY KEY,
  submission_id BIGINT,
  custom_test_suite_run_id BIGINT,
  problem_language_id BIGINT NOT NULL,
  source_code TEXT NOT NULL,
  cases JSONB NOT NULL,
  status TEXT NOT NULL DEFAULT 'QUEUED',
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  started_at TIMESTAMPTZ,
  CONSTRAINT fk_grading_jobs_submission_id
    FOREIGN KEY (submission_id) REFERENCES submissions (id) ON DELETE CASCADE,
  CONSTRAINT fk_grading_jobs_custom_test_suite_run_id
    FOREIGN KEY (custom_test_suite_run_id) REFERENCES custom_test_suite_runs (id) ON DELETE CASCADE,
  CONSTRAINT fk_grading_jobs_problem_language_id
    FOREIGN KEY (problem_language_id) REFERENCES problem_languages (id) ON DELETE RESTRICT,
  CONSTRAINT chk_grading_jobs_one_origin
    CHECK (num_nonnulls(submission_id, custom_test_suite_run_id) = 1)
);

CREATE UNIQUE INDEX idx_grading_jobs_submission_id_unique ON grading_jobs (submission_id);
CREATE UNIQUE INDEX idx_grading_jobs_custom_test_suite_run_id_unique ON grading_jobs (custom_test_suite_run_id);
CREATE INDEX idx_grading_jobs_status_created_at ON grading_jobs (status, created_at);
CREATE INDEX idx_grading_jobs_problem_language ON grading_jobs (problem_language_id);
