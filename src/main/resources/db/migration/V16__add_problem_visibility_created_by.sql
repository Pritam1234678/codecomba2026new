-- Migration: Add visibility and created_by to problems table
-- Version: V16
-- Description: Extends problems table to support problem visibility and ownership for private contests

-- Add visibility column with enum check constraint
ALTER TABLE problems
ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC'
CHECK (visibility IN ('PUBLIC', 'PRIVATE_AVAILABLE', 'PRIVATE_OWNED', 'ADMIN_ONLY'));

-- Add created_by column to track Contest_Host ownership (nullable)
ALTER TABLE problems
ADD COLUMN created_by BIGINT NULL REFERENCES users(id) ON DELETE SET NULL;

-- Create index on visibility for filtering queries
CREATE INDEX idx_problems_visibility ON problems(visibility);

-- Create index on created_by for ownership queries
CREATE INDEX idx_problems_created_by ON problems(created_by);

-- Update all existing problems to have visibility=PUBLIC as default
-- (This is already the default value, but explicitly updating for clarity)
UPDATE problems SET visibility = 'PUBLIC' WHERE visibility IS NULL;

-- Add comment to visibility column for documentation
COMMENT ON COLUMN problems.visibility IS 'Problem visibility: PUBLIC (all contests), PRIVATE_AVAILABLE (private contests only), PRIVATE_OWNED (created by Contest_Host), ADMIN_ONLY (restricted)';

-- Add comment to created_by column for documentation
COMMENT ON COLUMN problems.created_by IS 'User ID of Contest_Host who created this problem (via AI generation). NULL for admin-created problems.';
