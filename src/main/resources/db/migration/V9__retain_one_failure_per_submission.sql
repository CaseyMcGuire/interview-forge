-- Dropping the discriminator must not turn historical example/custom runs into official submissions.
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM submissions WHERE kind <> 'SUBMIT') THEN
    RAISE EXCEPTION 'Remove or migrate non-official submissions before dropping submissions.kind';
  END IF;
END $$;

ALTER TABLE submissions DROP COLUMN kind;

-- Rename in place so existing failure snapshots and their IDs are preserved.
ALTER TABLE submission_test_results RENAME TO submission_failures;
ALTER SEQUENCE submission_test_results_id_seq RENAME TO submission_failures_id_seq;
ALTER TABLE submission_failures RENAME CONSTRAINT submission_test_results_pkey TO submission_failures_pkey;
ALTER TABLE submission_failures RENAME CONSTRAINT fk_submission_test_results_submission_id TO fk_submission_failures_submission_id;
ALTER TABLE submission_failures RENAME CONSTRAINT fk_submission_test_results_test_case_id TO fk_submission_failures_test_case_id;

DROP INDEX uq_submission_test_results_submission_position;
ALTER TABLE submission_failures
  ADD CONSTRAINT uq_submission_failures_submission UNIQUE (submission_id);

ALTER INDEX idx_submission_test_results_test_case RENAME TO idx_submission_failures_test_case;
