-- Migration: Create custom enum types
-- Created: 2025-10-24
-- Description: Create the three custom enum types needed for the application

-- Create gender_filter enum
CREATE TYPE gender_filter AS ENUM (
    'MALE',
    'FEMALE',
    'ANY'
);

-- Create activity_type enum
CREATE TYPE activity_type AS ENUM (
    'SESSION',
    'CAMP',
    'COURSE',
    'EVENT',
    'CAMPAIGN',
    'PROTECTION_SESSION',
    'BODH_SESSION'
);

-- Create family_relation enum
CREATE TYPE family_relation AS ENUM (
    'SELF',
    'FATHER',
    'MOTHER',
    'HUSBAND',
    'WIFE',
    'SON',
    'DAUGHTER',
    'BROTHER',
    'SISTER',
    'GRANDFATHER',
    'GRANDMOTHER',
    'GRANDSON',
    'GRANDDAUGHTER',
    'UNCLE',
    'AUNT',
    'COUSIN',
    'NEPHEW',
    'NIECE',
    'GUARDIAN',
    'RELATIVE',
    'OTHER'
);

