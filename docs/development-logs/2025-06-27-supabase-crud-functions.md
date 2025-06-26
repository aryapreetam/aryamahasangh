# Development Log: June 27, 2025

## Supabase CRUD Functions for Arya Samaj Management Platform

### Overview

Today's session focused on creating comprehensive CRUD (Create, Read, Update, Delete) functions for the Arya Samaj
Management Platform's Supabase backend. The main accomplishments include:

1. **Created Arya Samaj CRUD functions** (insert, update, delete)
2. **Created Member CRUD functions** (insert, update, delete)
3. **Created Family CRUD functions** (insert, update, delete)
4. **Established localization standards** for database responses
5. **Updated project rules** for consistent error handling

---

## 1. Initial Database Analysis

### Database Structure Explored

- **Tables examined**: `address`, `arya_samaj`, `member`, `samaj_member`, `family`, `family_member`, and related tables
- **Key relationships identified**:
    - `arya_samaj` → `address` (foreign key: address_id)
    - `samaj_member` → `arya_samaj`, `member` (foreign keys)
    - `member` → `address`, `arya_samaj`, `member` (referrer)

### Sample Data Analysis

- Examined existing member records to understand data structure
- Found members like "आचार्य जितेन्द्र आर्य", "आचार्य डॉ० महेशचन्द्र आर्य"

---

## 2. Arya Samaj CRUD Functions

### 2.1 Insert Function: `insert_arya_samaj_details()`

**Purpose**: Creates a complete Arya Samaj record with address and member assignment in a single transaction.

**Function Signature**:

```sql
CREATE OR REPLACE FUNCTION insert_arya_samaj_details(
    p_samaj_name TEXT,
    p_samaj_description TEXT,
    p_basic_address TEXT,
    p_state TEXT,
    p_district TEXT,
    p_samaj_media_urls TEXT[] DEFAULT '{}',
    p_pincode TEXT DEFAULT NULL,
    p_latitude DOUBLE PRECISION DEFAULT NULL,
    p_longitude DOUBLE PRECISION DEFAULT NULL,
    p_vidhansabha TEXT DEFAULT NULL,
    p_member_id TEXT DEFAULT NULL,
    p_post TEXT DEFAULT 'प्रधान',
    p_priority SMALLINT DEFAULT 1
)
RETURNS JSON
```

**Process Flow**:

1. **Insert Address**: Creates address record → gets `address_id`
2. **Insert Arya Samaj**: Uses `address_id` → gets `arya_samaj_id`
3. **Select Member**: Uses provided `member_id` or picks first available member
4. **Insert Samaj Member**: Creates relationship with post and priority

**Testing**:

```sql
-- Test 1: Delhi Pradesh Arya Samaj
SELECT insert_arya_samaj_details(
    'आर्य समाज दिल्ली प्रदेश',
    'दिल्ली प्रदेश का मुख्य आर्य समाज केंद्र जो वैदिक धर्म के प्रचार-प्रसार में संलग्न है।',
    'सेक्टर-15, द्वारका',
    'दिल्ली',
    'दक्षिण पश्चिम दिल्ली',
    '{}',
    '110078',
    28.5921,
    77.0460,
    'द्वारका विधानसभा'
);

-- Test 2: Saket Arya Samaj
SELECT insert_arya_samaj_details(
    'आर्य समाज मंदिर साकेत',
    'साकेत मेट्रो स्टेशन के निकट स्थित आर्य समाज मंदिर...',
    'साकेत मेट्रो स्टेशन के निकट, साकेत', 
    'दिल्ली',
    'दक्षिण दिल्ली',
    ARRAY['https://picsum.photos/800/600?random=3', 'https://picsum.photos/800/600?random=4'],
    '110017',
    28.5245,
    77.2066,
    'साकेत विधानसभा'
);
```

**Success Response**:

```json
{
  "success": true,
  "address_id": "2154a928-558f-4dac-bb9f-e0c0fbfcc6a1",
  "arya_samaj_id": "01007743-a354-42c1-b762-7a254493f306",
  "samaj_member_id": "bfbe4cc9-5d81-4c36-9759-c84e3f91f426",
  "assigned_member_id": "a5e86746-1a65-4f03-8204-3133591f46bc",
  "message_code": "ARYA_SAMAJ_CREATED_SUCCESSFULLY"
}
```

### 2.2 Update Function: `update_arya_samaj_details()`

**Purpose**: Updates existing Arya Samaj and its address information.

**Function Signature**:

```sql
CREATE OR REPLACE FUNCTION update_arya_samaj_details(
    p_arya_samaj_id TEXT,
    p_samaj_name TEXT DEFAULT NULL,
    p_samaj_description TEXT DEFAULT NULL,
    p_basic_address TEXT DEFAULT NULL,
    p_state TEXT DEFAULT NULL,
    p_district TEXT DEFAULT NULL,
    p_samaj_media_urls TEXT[] DEFAULT NULL,
    p_pincode TEXT DEFAULT NULL,
    p_latitude DOUBLE PRECISION DEFAULT NULL,
    p_longitude DOUBLE PRECISION DEFAULT NULL,
    p_vidhansabha TEXT DEFAULT NULL
)
RETURNS JSON
```

**Key Features**:

- Only updates non-null parameters (using COALESCE)
- Validates Arya Samaj exists before updating
- Updates both `arya_samaj` and related `address` records

**Testing**:

```sql
-- Test update with selective fields
SELECT update_arya_samaj_details(
    '7660d11e-c6ff-4229-b458-6088210e52da',
    NULL, -- don't change name
    'साकेत मेट्रो स्टेशन के निकट स्थित प्रमुख आर्य समाज मंदिर...', -- updated description
    'A-2/3, साकेत मेट्रो स्टेशन के सामने', -- updated address
    NULL, -- don't change state
    NULL, -- don't change district
    ARRAY['https://picsum.photos/800/600?random=5', 'https://picsum.photos/800/600?random=6'] -- updated media urls
);
```

### 2.3 Delete Function: `delete_arya_samaj()`

**Purpose**: Safely deletes Arya Samaj and all related records.

**Function Signature**:

```sql
CREATE OR REPLACE FUNCTION delete_arya_samaj(
    p_arya_samaj_id TEXT
)
RETURNS JSON
```

**Deletion Order** (maintains referential integrity):

1. Delete from `samaj_member` table
2. Update `member` records (remove arya_samaj_id reference)
3. Delete from `arya_samaj` table
4. Delete associated `address` record

**Response includes**:

- Counts of affected records
- IDs of deleted records
- Success/error status

---

## 3. Member CRUD Functions

### 3.1 Insert Function: `insert_member_details()`

**Purpose**: Creates a new member with comprehensive validation.

**Function Signature**:

```sql
CREATE OR REPLACE FUNCTION insert_member_details(
    p_name TEXT,
    p_phone_number TEXT,
    p_profile_image TEXT DEFAULT NULL,
    p_educational_qualification TEXT DEFAULT NULL,
    p_email TEXT DEFAULT NULL,
    p_dob DATE DEFAULT NULL,
    p_address_id TEXT DEFAULT NULL,
    p_arya_samaj_id TEXT DEFAULT NULL,
    p_joining_date DATE DEFAULT NULL,
    p_temp_address_id TEXT DEFAULT NULL,
    p_referrer_id TEXT DEFAULT NULL,
    p_occupation TEXT DEFAULT NULL,
    p_introduction TEXT DEFAULT NULL,
    p_gender TEXT DEFAULT NULL
)
RETURNS JSON
```

**Validations**:

- `referrer_id` exists in member table
- `arya_samaj_id` exists in arya_samaj table
- `address_id` exists in address table
- `temp_address_id` exists in address table
- `gender` converts to enum type

**Testing**:

```sql
SELECT insert_member_details(
    'टेस्ट सदस्य',
    '9876543210',
    NULL, -- profile_image
    'स्नातक',
    'test@example.com',
    '1990-01-01'::DATE,
    NULL, -- address_id
    NULL, -- arya_samaj_id
    '2024-01-01'::DATE, -- joining_date
    NULL, -- temp_address_id
    NULL, -- referrer_id
    'इंजीनियर',
    'यह एक टेस्ट सदस्य है।',
    'MALE'
);
```

### 3.2 Update Function: `update_member_details()`

**Purpose**: Updates existing member information with validation.

**Key Features**:

- Validates member exists
- Validates all foreign key references
- Only updates provided fields (COALESCE pattern)
- Handles gender enum conversion

### 3.3 Delete Function: `delete_member()`

**Purpose**: Safely deletes member and handles all related records.

**Deletion Process**:

1. Delete from `activity_member`
2. Delete from `organisational_member`
3. Delete from `samaj_member`
4. Delete from `family_member`
5. Update referrals (set referrer_id to NULL)
6. Delete the member record

**Response includes detailed statistics**:

- Number of deleted activity memberships
- Number of deleted organizational memberships
- Number of deleted samaj memberships
- Number of deleted family memberships
- Number of affected referrals

---

## 4. Family CRUD Functions

### 4.1 Insert Function: `insert_family_details()`

**Purpose**: Creates a new family with a designated head member.

**Function Signature**:

```sql
CREATE OR REPLACE FUNCTION insert_family_details(
    p_family_name TEXT,
    p_head_member_id TEXT,
    p_address_id TEXT DEFAULT NULL,
    p_arya_samaj_id TEXT DEFAULT NULL,
    p_photos TEXT[] DEFAULT NULL
)
RETURNS JSON
```

**Key Features**:

- Validates head member exists and is not already head of another family
- Validates foreign key references (address, arya_samaj)
- Automatically creates family_member record with `is_head=true` and `relation_to_head='SELF'`
- Supports family photos array for multiple images

**Process Flow**:

1. **Member Validation**: Verify head member exists and is not already a family head
2. **Foreign Key Validation**: Check address and arya_samaj references if provided
3. **Family Creation**: Insert family record with provided details
4. **Head Member Assignment**: Create family_member relationship with head status

**Testing**:

```sql
SELECT insert_family_details(
    'टेस्ट परिवार',
    'a5e86746-1a65-4f03-8204-3133591f46bc', -- existing member as head
    NULL, -- no address
    NULL, -- no arya samaj
    ARRAY['https://picsum.photos/800/600?random=10', 'https://picsum.photos/800/600?random=11']
);
```

**Success Response**:

```json
{
  "success": true,
  "family_id": "6a382392-9efe-4b4f-a8c1-30cd33336eca",
  "family_member_id": "30b216b5-c69f-4baa-911c-33ba2620887a",
  "head_member_id": "a5e86746-1a65-4f03-8204-3133591f46bc",
  "message_code": "FAMILY_CREATED_SUCCESSFULLY"
}
```

### 4.2 Update Function: `update_family_details()`

**Purpose**: Updates existing family information.

**Function Signature**:

```sql
CREATE OR REPLACE FUNCTION update_family_details(
    p_family_id TEXT,
    p_family_name TEXT DEFAULT NULL,
    p_address_id TEXT DEFAULT NULL,
    p_arya_samaj_id TEXT DEFAULT NULL,
    p_photos TEXT[] DEFAULT NULL
)
RETURNS JSON
```

**Key Features**:

- Only updates non-null parameters (using COALESCE)
- Validates family exists before updating
- Validates foreign key references
- Supports partial updates for any combination of fields

**Testing**:

```sql
SELECT update_family_details(
    '6a382392-9efe-4b4f-a8c1-30cd33336eca',
    'अपडेटेड टेस्ट परिवार', -- updated name
    NULL, -- don't change address
    NULL, -- don't change arya samaj
    ARRAY['https://picsum.photos/800/600?random=12', 'https://picsum.photos/800/600?random=13', 'https://picsum.photos/800/600?random=14'] -- updated photos
);
```

### 4.3 Delete Function: `delete_family()`

**Purpose**: Safely deletes family and all related family_member records.

**Function Signature**:

```sql
CREATE OR REPLACE FUNCTION delete_family(
    p_family_id TEXT
)
RETURNS JSON
```

**Deletion Process**:

1. Delete from `family_member` table (maintains referential integrity)
2. Delete from `family` table
3. Return detailed statistics about deleted records

**Response includes**:

- Deleted family ID
- Count of deleted family members
- Success/error status

**Testing**:

```sql
SELECT delete_family('6a382392-9efe-4b4f-a8c1-30cd33336eca');
```

**Success Response**:

```json
{
  "success": true,
  "deleted_family_id": "6a382392-9efe-4b4f-a8c1-30cd33336eca",
  "deleted_family_members": 1,
  "message_code": "FAMILY_DELETED_SUCCESSFULLY"
}
```

### 4.4 Family-Specific Error Codes

**Error Codes Added**:

- `FAMILY_NOT_FOUND` - When provided family ID doesn't exist
- `HEAD_MEMBER_NOT_FOUND` - When specified head member doesn't exist
- `MEMBER_ALREADY_FAMILY_HEAD` - When member is already head of another family
- `ERROR_CREATING_FAMILY` - General family creation error
- `ERROR_UPDATING_FAMILY_DETAILS` - General family update error
- `ERROR_DELETING_FAMILY` - General family deletion error

**Success Message Codes**:

- `FAMILY_CREATED_SUCCESSFULLY`
- `FAMILY_UPDATED_SUCCESSFULLY`
- `FAMILY_DELETED_SUCCESSFULLY`

### 4.5 Family Structure Insights

**Database Design Patterns**:

- **Family-Member Relationship**: One-to-many with family_member junction table
- **Family Head Concept**: Single member marked as `is_head=true` with `relation_to_head='SELF'`
- **Family Relations**: Comprehensive enum for family relationships (21 types from SELF to OTHER)
- **Optional Associations**: Nullable foreign keys to address and arya_samaj
- **Media Support**: Array field for multiple family photos

**Family Relation Enum Values**:
SELF, FATHER, MOTHER, HUSBAND, WIFE, SON, DAUGHTER, BROTHER, SISTER, GRANDFATHER, GRANDMOTHER, GRANDSON, GRANDDAUGHTER,
UNCLE, AUNT, COUSIN, NEPHEW, NIECE, GUARDIAN, RELATIVE, OTHER

---

## 5. Localization Standards Implementation

### 5.1 Problem Identified

Initially, functions returned Hindi messages directly:

```json
{
  "message": "आर्य समाज विवरण सफलतापूर्वक जोड़े गए"
}
```

**Issues**:

- Hard to add new languages
- Database changes required for message updates
- Not scalable for international users

### 5.2 Solution Implemented

Changed to English message codes:

```json
{
  "message_code": "ARYA_SAMAJ_CREATED_SUCCESSFULLY"
}
```

### 5.3 Message Code Standards

**Success Codes**:

- `ARYA_SAMAJ_CREATED_SUCCESSFULLY`
- `ARYA_SAMAJ_UPDATED_SUCCESSFULLY`
- `ARYA_SAMAJ_DELETED_SUCCESSFULLY`
- `MEMBER_CREATED_SUCCESSFULLY`
- `MEMBER_UPDATED_SUCCESSFULLY`
- `MEMBER_DELETED_SUCCESSFULLY`
- `FAMILY_CREATED_SUCCESSFULLY`
- `FAMILY_UPDATED_SUCCESSFULLY`
- `FAMILY_DELETED_SUCCESSFULLY`

**Error Codes**:

- `ARYA_SAMAJ_NOT_FOUND`
- `MEMBER_NOT_FOUND`
- `REFERRER_NOT_FOUND`
- `ADDRESS_NOT_FOUND`
- `TEMP_ADDRESS_NOT_FOUND`
- `NO_MEMBERS_AVAILABLE`
- `ERROR_CREATING_ARYA_SAMAJ`
- `ERROR_UPDATING_ARYA_SAMAJ_DETAILS`
- `ERROR_DELETING_ARYA_SAMAJ`
- `ERROR_CREATING_MEMBER`
- `ERROR_UPDATING_MEMBER_DETAILS`
- `ERROR_DELETING_MEMBER`
- `FAMILY_NOT_FOUND`
- `HEAD_MEMBER_NOT_FOUND`
- `MEMBER_ALREADY_FAMILY_HEAD`
- `ERROR_CREATING_FAMILY`
- `ERROR_UPDATING_FAMILY_DETAILS`
- `ERROR_DELETING_FAMILY`

### 5.4 Client-Side Localization Pattern

**Kotlin Implementation Example**:

```kotlin
val messageLocalizations = mapOf(
    "hi" to mapOf(
        "ARYA_SAMAJ_CREATED_SUCCESSFULLY" to "आर्य समाज विवरण सफलतापूर्वक जोड़े गए",
        "ARYA_SAMAJ_UPDATED_SUCCESSFULLY" to "आर्य समाज विवरण सफलतापूर्वक अपडेट किए गए",
        "MEMBER_NOT_FOUND" to "सदस्य नहीं मिला",
        "ERROR_UPDATING_MEMBER_DETAILS" to "सदस्य विवरण अपडेट करने में त्रुटि हुई"
    ),
    "en" to mapOf(
        "ARYA_SAMAJ_CREATED_SUCCESSFULLY" to "Arya Samaj details created successfully",
        "MEMBER_NOT_FOUND" to "Member not found",
        "ERROR_UPDATING_MEMBER_DETAILS" to "Error updating member details"
    )
)

fun localizeMessage(messageCode: String, language: String): String {
    return messageLocalizations[language]?.get(messageCode) ?: messageCode
}
```

---

## 6. Project Rules Updates

### 6.1 Added to firebender.json

```json
"All Supabase database functions must return English message codes and error codes instead of localized text to enable client-side localization. Success responses use 'message_code' field with values like 'RECORD_CREATED_SUCCESSFULLY'. Error responses use 'error_code' field with values like 'RECORD_NOT_FOUND', 'ERROR_CREATING_RECORD'. Always include 'success' boolean field. For errors, optionally include 'error_details' field with technical information. Never use Hindi or other localized languages in database function responses. Client-side code handles message localization based on user preferences."
```

### 6.2 Benefits of This Approach

- **Scalable**: Easy to add new languages without database changes
- **Maintainable**: Message changes don't require migrations
- **Consistent**: Same error codes across all functions
- **Flexible**: Client controls message presentation
- **Debugging-friendly**: Technical details in `error_details` field

---

## 7. Testing Results

### 7.1 Successful Test Cases

**Arya Samaj Functions**:
✅ Insert: Created 2 Arya Samaj records with complete address and member assignment
✅ Update: Successfully updated description, address, and media URLs
✅ Delete: Safely deleted test record with proper cleanup

**Member Functions**:
✅ Insert: Created member with validation
✅ Update: Updated selective fields while preserving others
✅ Delete: Deleted member with proper relationship cleanup

**Family Functions**:
✅ Insert: Created family with head member and validation
✅ Update: Updated family details with validation
✅ Delete: Deleted family and related family members

### 7.2 Error Handling Verified

- Non-existent ID validation
- Foreign key constraint validation
- Proper error code responses
- Technical error details included

---

## 8. Media URL Management

### 8.1 Image URL Testing

- Initially tried Wikipedia URLs (didn't work)
- Switched to Lorem Picsum placeholder images: `https://picsum.photos/800/600?random=N`
- Successfully added media URLs to Arya Samaj records

### 8.2 Future Recommendations

- Set up Supabase Storage for actual image uploads
- Implement image upload functionality in the app
- Replace placeholder URLs with actual temple photos

---

## 9. Database Schema Insights

### 9.1 Key Tables Structure

- **address**: Stores location information with lat/lng, vidhansabha
- **arya_samaj**: Main entity with name, description, media_urls array
- **member**: Comprehensive member info with multiple address references
- **samaj_member**: Junction table for member-samaj relationships with posts/priority
- **family**: Stores family information
- **family_member**: Junction table for family-member relationships with relation types

### 9.2 Relationship Patterns

- Address sharing between entities (member, arya_samaj)
- Self-referential relationships (member.referrer_id)
- Multiple relationship types (activity_member, organisational_member, samaj_member, family_member)

---

## 10. Next Steps Recommendations

### 10.1 Immediate

1. **Test functions in app**: Integrate with GraphQL/Apollo client
2. **Add localization**: Implement client-side message mapping
3. **Real images**: Set up Supabase Storage for actual photos

### 10.2 Future Enhancements

1. **Bulk operations**: Create functions for bulk insert/update
2. **Search functions**: Advanced search with filters
3. **Audit trail**: Track changes with timestamps and user info
4. **Validation**: Add more business rule validations

### 10.3 Similar Functions Needed

Based on today's pattern, consider creating CRUD functions for:

- `organisation` management
- `activity` management
- `address` management

---

## 11. Code Quality Standards Established

### 11.1 Function Structure

- Clear parameter naming with `p_` prefix
- Comprehensive validation before operations
- Proper transaction handling with exception blocks
- Detailed return objects with success flags

### 11.2 Error Handling Pattern

- English error codes for consistency
- Technical details in `error_details` field
- Specific validation error codes
- Comprehensive logging information

### 11.3 Testing Approach

- Create test data
- Verify operations
- Test edge cases
- Clean up test data

This comprehensive approach ensures maintainable, scalable, and robust database operations for the Arya Samaj Management
Platform.
