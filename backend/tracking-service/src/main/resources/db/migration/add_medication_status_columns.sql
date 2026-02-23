-- Add medication status tracking columns
-- Run this manually if Hibernate auto-update doesn't work

-- Add status column (enum as varchar)
ALTER TABLE medications 
ADD COLUMN IF NOT EXISTS status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE';

-- Add start_date column
ALTER TABLE medications 
ADD COLUMN IF NOT EXISTS start_date DATE;

-- Add end_date column  
ALTER TABLE medications 
ADD COLUMN IF NOT EXISTS end_date DATE;

-- Add updated_at timestamp
ALTER TABLE medications 
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

-- Update existing records to set updated_at to created_at
UPDATE medications 
SET updated_at = created_at 
WHERE updated_at IS NULL;

-- Optional: Set start_date for existing medications based on prescription session date
-- (This requires joining through prescriptions to sessions)
UPDATE medications m
SET start_date = s.session_date::date
FROM prescriptions p
JOIN sessions s ON p.session_id = s.id
WHERE m.prescription_id = p.id 
  AND m.start_date IS NULL;

-- Verify the changes
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'medications'
  AND column_name IN ('status', 'start_date', 'end_date', 'updated_at')
ORDER BY column_name;
