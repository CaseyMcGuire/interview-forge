ALTER TABLE judge_configurations RENAME COLUMN runtime_key TO runtime;
ALTER TABLE judge_configurations RENAME COLUMN harness_source TO test_driver_code;

-- Submissions copy the judge's runtime at enqueue time; keep the copied field's name aligned.
ALTER TABLE submissions RENAME COLUMN runtime_key TO runtime;
