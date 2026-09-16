-- Execution uses the current runtime and judge settings for the submission's problem language.
ALTER TABLE submissions DROP COLUMN runtime;

CREATE INDEX idx_submissions_status_created_at ON submissions (status, created_at);
