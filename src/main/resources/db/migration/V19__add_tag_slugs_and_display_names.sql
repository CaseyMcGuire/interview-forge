ALTER TABLE tags RENAME COLUMN name TO display_name;
DROP INDEX idx_tags_name_unique;

ALTER TABLE tags ADD COLUMN slug TEXT;

-- Existing labels may map to the same slug; IDs give each existing tag a stable, unique identifier.
UPDATE tags SET slug = 'tag-' || id;

ALTER TABLE tags ALTER COLUMN slug SET NOT NULL;
CREATE UNIQUE INDEX idx_tags_slug_unique ON tags (slug);
