-- Migration: Create registration tables
-- Created: 2025-10-24
-- Description: Create satr_registration, course_registrations, admission, and book_orders tables

-- Table: satr_registration
CREATE TABLE satr_registration (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),
    fullname TEXT NOT NULL,
    gender TEXT NOT NULL,
    mobile TEXT NOT NULL,
    aadhar_no TEXT,
    educational_qualification TEXT,
    address TEXT,
    inspiration_source TEXT,
    inspiration_source_name TEXT,
    inspiration_source_no TEXT,
    has_trained_arya_in_family BOOLEAN,
    trained_arya_name TEXT,
    trained_arya_no TEXT,
    activity_id TEXT NOT NULL REFERENCES activities(id) ON DELETE CASCADE ON UPDATE NO ACTION
);

-- Table: course_registrations
CREATE TABLE course_registrations (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    name TEXT,
    satr_date TIMESTAMPTZ DEFAULT now(),
    satr_place TEXT DEFAULT '',
    recommendation TEXT,
    activity_id TEXT REFERENCES activities(id) ON DELETE CASCADE ON UPDATE NO ACTION,
    payment_receipt_url TEXT
);

-- Table: admission
CREATE TABLE admission (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid(),
    studentName TEXT,
    aadharNo TEXT,
    dob TEXT,
    bloodGroup TEXT,
    previousClass TEXT,
    marksObtained TEXT,
    schoolName TEXT,
    fatherName TEXT,
    fatherOccupation TEXT,
    fatherQualification TEXT,
    motherName TEXT,
    motherOccupation TEXT,
    motherQualification TEXT,
    fullAddress TEXT,
    mobileNo TEXT,
    alternateMobileNo TEXT,
    attachedDocuments TEXT[],
    studentPhoto TEXT,
    studentSignature TEXT,
    parentSignature TEXT
);

-- Table: book_orders
CREATE TABLE book_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fullname TEXT NOT NULL,
    address TEXT,
    city TEXT,
    district TEXT,
    state TEXT,
    mobile TEXT,
    pincode TEXT,
    country TEXT,
    district_officer_name TEXT,
    district_officer_number TEXT,
    payment_receipt_url TEXT,
    is_fulfilled BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT now()
);

