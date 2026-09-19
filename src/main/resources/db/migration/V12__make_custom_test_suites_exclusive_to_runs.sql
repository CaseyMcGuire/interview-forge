-- Every custom run gets its own suite; reruns must not reuse a previous suite.
CREATE UNIQUE INDEX idx_test_runs_custom_test_suite_id_unique ON test_runs (custom_test_suite_id);

-- The unique suite lookup now covers both relationship loading and cleanup checks.
DROP INDEX idx_test_runs_suite_status;
