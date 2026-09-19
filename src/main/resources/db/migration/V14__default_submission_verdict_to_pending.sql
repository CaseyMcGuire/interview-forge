-- Finished attempts without a recorded verdict cannot be reported as pending.
UPDATE submissions
SET verdict = CASE
  WHEN status = 'FINISHED' THEN 'INTERNAL_ERROR'
  ELSE 'PENDING'
END
WHERE verdict IS NULL;

ALTER TABLE submissions
  ALTER COLUMN verdict SET DEFAULT 'PENDING',
  ALTER COLUMN verdict SET NOT NULL;
