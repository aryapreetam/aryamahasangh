-- Update storage URLs from Supabase to Nhost
-- Generated: 2025-10-24 12:32:42
-- Updated: 2025-10-24 (Added organisation.logo, family.photos, learning.thumbnail_url)
-- Note: Replace <your-supabase-project-id> and <your-nhost-subdomain> with actual values

-- Update member.profile_image URLs
UPDATE member SET profile_image = REPLACE(
  profile_image,
  'https://<your-supabase-project-id>.supabase.co/storage/v1/object/public/',
  'https://<your-nhost-subdomain>.storage.<your-nhost-region>.nhost.run/v1/files/'
) WHERE profile_image IS NOT NULL AND profile_image != '';

-- Update organisation.logo URLs
UPDATE organisation SET logo = REPLACE(
  logo,
  'https://<your-supabase-project-id>.supabase.co/storage/v1/object/public/',
  'https://<your-nhost-subdomain>.storage.<your-nhost-region>.nhost.run/v1/files/'
) WHERE logo IS NOT NULL AND logo != '';

-- Update learning.thumbnail_url URLs
UPDATE learning SET thumbnail_url = REPLACE(
  thumbnail_url,
  'https://<your-supabase-project-id>.supabase.co/storage/v1/object/public/',
  'https://<your-nhost-subdomain>.storage.<your-nhost-region>.nhost.run/v1/files/'
) WHERE thumbnail_url IS NOT NULL AND thumbnail_url != '';

-- Update activities.media_files URLs (array column)
UPDATE activities SET media_files = ARRAY(
  SELECT REPLACE(
    unnest(media_files),
    'https://<your-supabase-project-id>.supabase.co/storage/v1/object/public/',
    'https://<your-nhost-subdomain>.storage.<your-nhost-region>.nhost.run/v1/files/'
  )
) WHERE media_files IS NOT NULL AND array_length(media_files, 1) > 0;

-- Update activities.overview_media_urls URLs (array column)
UPDATE activities SET overview_media_urls = ARRAY(
  SELECT REPLACE(
    unnest(overview_media_urls),
    'https://<your-supabase-project-id>.supabase.co/storage/v1/object/public/',
    'https://<your-nhost-subdomain>.storage.<your-nhost-region>.nhost.run/v1/files/'
  )
) WHERE overview_media_urls IS NOT NULL AND array_length(overview_media_urls, 1) > 0;

-- Update family.photos URLs (array column)
UPDATE family SET photos = ARRAY(
  SELECT REPLACE(
    unnest(photos),
    'https://<your-supabase-project-id>.supabase.co/storage/v1/object/public/',
    'https://<your-nhost-subdomain>.storage.<your-nhost-region>.nhost.run/v1/files/'
  )
) WHERE photos IS NOT NULL AND array_length(photos, 1) > 0;

-- Verify updates
SELECT 'member.profile_image' as table_column, COUNT(*) as updated_count
FROM member
WHERE profile_image LIKE 'https://<your-nhost-subdomain>.storage.<your-nhost-region>.nhost.run%'
UNION ALL
SELECT 'organisation.logo', COUNT(*)
FROM organisation
WHERE logo LIKE 'https://<your-nhost-subdomain>.storage.<your-nhost-region>.nhost.run%'
UNION ALL
SELECT 'learning.thumbnail_url', COUNT(*)
FROM learning
WHERE thumbnail_url LIKE 'https://<your-nhost-subdomain>.storage.<your-nhost-region>.nhost.run%'
UNION ALL
SELECT 'activities.media_files', COUNT(*)
FROM activities
WHERE EXISTS (
  SELECT 1 FROM unnest(media_files) AS url
  WHERE url LIKE 'https://<your-nhost-subdomain>.storage.<your-nhost-region>.nhost.run%'
)
UNION ALL
SELECT 'activities.overview_media_urls', COUNT(*)
FROM activities
WHERE EXISTS (
  SELECT 1 FROM unnest(overview_media_urls) AS url
  WHERE url LIKE 'https://<your-nhost-subdomain>.storage.<your-nhost-region>.nhost.run%'
)
UNION ALL
SELECT 'family.photos', COUNT(*)
FROM family
WHERE EXISTS (
  SELECT 1 FROM unnest(photos) AS url
  WHERE url LIKE 'https://<your-nhost-subdomain>.storage.<your-nhost-region>.nhost.run%'
);

