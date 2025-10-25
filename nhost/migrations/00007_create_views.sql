-- Migration: Create views
-- Created: 2025-10-24
-- Description: Create the 4 views used in the application

-- View: activities_with_status
-- Purpose: Shows activities with a computed status priority based on current datetime
CREATE OR REPLACE VIEW activities_with_status AS
SELECT
    activities.id,
    activities.name,
    activities.short_description,
    activities.start_datetime,
    activities.end_datetime,
    activities.type,
    activities.allowed_gender,
    address.district,
    address.state,
    CASE
        WHEN (now() >= activities.start_datetime) AND (now() <= activities.end_datetime) THEN 0
        WHEN (now() < activities.start_datetime) THEN 1
        ELSE 2
    END AS status_priority
FROM activities
LEFT JOIN address ON (activities.address_id = address.id);

-- View: arya_samaj_with_address
-- Purpose: Joins arya_samaj with full address details
CREATE OR REPLACE VIEW arya_samaj_with_address AS
SELECT
    s.id,
    s.name,
    s.media_urls,
    s.created_at,
    s.description,
    a.basic_address,
    a.state,
    a.district,
    a.pincode,
    a.latitude,
    a.longitude,
    a.vidhansabha
FROM arya_samaj s
JOIN address a ON (s.address_id = a.id);

-- View: member_in_organisation
-- Purpose: Lists all members who are part of at least one organisation
CREATE OR REPLACE VIEW member_in_organisation AS
SELECT DISTINCT
    m.id,
    m.name,
    m.phone_number,
    m.profile_image
FROM member m
JOIN organisational_member om ON (om.member_id = m.id);

-- View: member_not_in_family
-- Purpose: Lists all members who are not part of any family
CREATE OR REPLACE VIEW member_not_in_family AS
SELECT
    m.id,
    m.name,
    m.phone_number,
    m.profile_image,
    m.address_id
FROM member m
LEFT JOIN family_member fm ON (fm.member_id = m.id)
WHERE fm.member_id IS NULL;

