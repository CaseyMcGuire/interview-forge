-- Active runtimes are configured per language in application config.
-- Submission runtime snapshots remain unchanged.
ALTER TABLE judge_configurations DROP COLUMN runtime;
