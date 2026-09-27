ALTER TABLE judge_configurations
  ADD COLUMN test_driver_type TEXT NOT NULL DEFAULT 'MAIN_FUNCTION';
