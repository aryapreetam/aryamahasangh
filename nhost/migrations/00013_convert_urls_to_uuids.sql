-- Convert all Nhost storage URLs to UUID-only format
-- Nhost Best Practice: Store only file UUIDs instead of full URLs

-- 1. Update member.profile_image
UPDATE member
SET profile_image = substring(profile_image from '[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}')
WHERE profile_image LIKE 'https://%';

-- 2. Update organisation.logo
UPDATE organisation
SET logo = substring(logo from '[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}')
WHERE logo LIKE 'https://%';

-- 3. Update learning.thumbnail_url
UPDATE learning
SET thumbnail_url = substring(thumbnail_url from '[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}')
WHERE thumbnail_url LIKE 'https://%';

-- 4. Update activities.media_files (array column)
UPDATE activities
SET media_files = array(
    SELECT substring(url from '[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}')
    FROM unnest(media_files) AS url
)
WHERE EXISTS (
    SELECT 1 FROM unnest(media_files) AS url WHERE url LIKE 'https://%'
);

-- 5. Update activities.overview_media_urls (array column)
UPDATE activities
SET overview_media_urls = array(
    SELECT substring(url from '[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}')
    FROM unnest(overview_media_urls) AS url
)
WHERE EXISTS (
    SELECT 1 FROM unnest(overview_media_urls) AS url WHERE url LIKE 'https://%'
);

-- 6. Update family.photos (array column)
UPDATE family
SET photos = array(
    SELECT substring(url from '[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}')
    FROM unnest(photos) AS url
)
WHERE EXISTS (
    SELECT 1 FROM unnest(photos) AS url WHERE url LIKE 'https://%'
);

-- Verification queries
SELECT 'member.profile_image' as column_name, COUNT(*) as uuid_count
FROM member
WHERE profile_image ~ '^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$'
UNION ALL
SELECT 'organisation.logo', COUNT(*)
FROM organisation
WHERE logo ~ '^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$'
UNION ALL
SELECT 'learning.thumbnail_url', COUNT(*)
FROM learning
WHERE thumbnail_url ~ '^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$'
UNION ALL
SELECT 'activities.media_files', COUNT(*)
FROM activities
WHERE EXISTS (
    SELECT 1 FROM unnest(media_files) AS url
    WHERE url ~ '^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$'
)
UNION ALL
SELECT 'activities.overview_media_urls', COUNT(*)
FROM activities
WHERE EXISTS (
    SELECT 1 FROM unnest(overview_media_urls) AS url
    WHERE url ~ '^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$'
);

