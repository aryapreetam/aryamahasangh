-- Migration: Configure Hasura Permissions (equivalent to Supabase RLS)
-- Created: 2025-10-24
-- Description: This file documents the Hasura permission structure
-- Note: Permissions are applied via Hasura metadata API, not SQL

-- PERMISSION PATTERN 1: Public SELECT + User ALL (13 tables)
-- Tables: address, organisation, app_labels, arya_samaj, activities, member, family,
--         activity_member, family_member, organisational_member, organisational_activity, samaj_member

-- PERMISSION PATTERN 2: Public SELECT + Public INSERT (2 tables)
-- Tables: satr_registration, course_registrations
-- Note: These allow anonymous registrations

-- PERMISSION PATTERN 3: No RLS / Open Access (3 tables)
-- Tables: admission, book_orders, learning
-- Note: These have no restrictions in Supabase

-- All permissions are configured through Hasura metadata API
-- See the applied metadata for exact permission rules

