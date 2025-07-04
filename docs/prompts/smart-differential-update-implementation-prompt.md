# Smart Differential Update Implementation Prompt Template

**Context:**
You have a Compose Multiplatform app with Supabase backend. The `update[EntityName]` function in
`[EntityName]Repository` currently does full delete-and-recreate of relationships, which is inefficient. You want to
implement smart differential updates following the pattern used in `updateActivityDetails`.

**Current Problems:**

- `update[EntityName]` deletes ALL `[relationship_table_1]` and `[relationship_table_2]` records, then recreates them
- No atomicity - if something fails midway, data is inconsistent
- Performance issues - unnecessary delete/insert operations
- Not following the efficient pattern already established in activity/member updates

**Planned Solution:**
Create Supabase Postgres function `update_[entity_name]_details(p_request JSONB)` that:

- Takes a single JSON parameter containing `[entity_id]` and updates
- Uses COALESCE pattern to only update non-null fields
- Handles relationships intelligently with differential updates, not full replace
- Returns standardized JSON response with success/error codes

**ViewModel-based diffing:**

- Store original `[entity_name]` data when loading for edit
- Compare original vs new data in ViewModel
- Only send changed fields to database
- Handle "no changes" case gracefully

**Repository changes:**

- Add `update[EntityName]Smart()` method using RPC calls to the new function
- Use operation-specific response types not common response type
- Clean, type-safe parameter passing

**Key Requirements:**

- Follow existing activity update pattern (COALESCE in SQL, JSON parameters)
- ViewModel handles diffing logic not repository or screen
- Database function handles smart relationship updates not delete-all-recreate
- Operation-specific response type for better maintainability
- Type-safe RPC calls without manual JSON building

**Entity Details to Replace:**

- `[EntityName]` → The entity class name (e.g., `Member`, `Organization`, `Event`)
- `[entity_name]` → Lowercase entity name (e.g., `member`, `organization`, `event`)
- `[entity_id]` → Primary key field name (e.g., `member_id`, `organization_id`)
- `[main_table]` → Primary table name (e.g., `members`, `organizations`, `events`)
- `[relationship_table_1]` → First relationship table (e.g., `member_organizations`)
- `[relationship_table_2]` → Second relationship table (e.g., `member_skills`)
- `[enum_field_1]` → First enum field if any (e.g., `member_type`, `organization_status`)
- `[enum_type_1]` → First enum type name (e.g., `member_type_enum`, `organization_status_enum`)

**Specific Entity Information Needed:**

1. **Main Entity Table:** `[main_table]` with columns `[list_columns]`
2. **Relationship Tables:**
    - `[relationship_table_1]` connects to `[related_entity_1]`
    - `[relationship_table_2]` connects to `[related_entity_2]`
3. **Enum Fields:** `[enum_field_1]` of type `[enum_type_1]`, `[enum_field_2]` of type `[enum_type_2]`
4. **Special Fields:** Any timestamp, array, or complex type fields
5. **Input Data Class:** `[EntityName]InputData` structure
6. **Current Repository:** `[EntityName]Repository.kt` location
7. **Current ViewModel:** `[EntityName]ViewModel.kt` location
8. **UI Screen:** `Create[EntityName]FormScreen.kt` or equivalent

**Files to Modify:**

- Database: Create `update_[entity_name]_details` function via Supabase MCP
- `[EntityName]Repository.kt`: Add new method and data types
- `[EntityName]ViewModel.kt`: Add diffing logic and original data storage
- `Create[EntityName]FormScreen.kt`: Update to pass editing flag

**Critical Point:**
The database function should do SMART updates - not delete and recreate relationships. It should compare existing vs new
relationships and only add/remove what actually changed, similar to how activity updates work.

**Expected Deliverables:**

1. Complete database function with proper error handling
2. GraphQL mutation file
3. Repository interface and implementation updates
4. ViewModel enhancements with original data tracking
5. UI integration updates
6. Comprehensive development log documenting the implementation

---

## Example Usage:

### For Member Updates:

```
**Entity Details:**
- EntityName: Member
- entity_name: member  
- entity_id: member_id
- main_table: members
- relationship_table_1: member_organizations
- relationship_table_2: member_skills  
- enum_field_1: member_type
- enum_type_1: member_type_enum
- Input Data Class: MemberInputData
- Repository: MemberRepository.kt
- ViewModel: MembersViewModel.kt
- UI Screen: CreateMemberFormScreen.kt

**Specific Columns:**
- members: id, name, email, phone, address, member_type, profile_image, etc.
- member_organizations: member_id, organization_id, role, join_date
- member_skills: member_id, skill_name, proficiency_level
```

### For Organization Updates:

```
**Entity Details:**
- EntityName: Organization
- entity_name: organization
- entity_id: organization_id  
- main_table: organizations
- relationship_table_1: organization_members
- relationship_table_2: organization_activities
- enum_field_1: organization_type
- enum_type_1: organization_type_enum
- Input Data Class: OrganizationInputData
- Repository: OrganizationRepository.kt
- ViewModel: OrganizationsViewModel.kt
- UI Screen: CreateOrganizationFormScreen.kt
```

---

## Questions to Clarify Before Implementation:

1. **What is the exact entity name** and its related tables?
2. **What enum fields exist** and their corresponding enum types in the database?
3. **What are the relationship tables** and how do they connect entities?
4. **Are there any special field types** (arrays, JSON, complex objects)?
5. **What is the current input data structure** being used in the form?
6. **Where are the current repository and ViewModel files** located?
7. **Should this follow the exact same pattern** as activity updates or are there entity-specific requirements?

---

## Implementation Pattern Reference

Based on the successful `update_activity_details` implementation, here's the core pattern to follow:

### Database Function Structure:

```sql
CREATE OR REPLACE FUNCTION update_[entity_name]_details(p_request JSONB)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_[entity_id] TEXT;
    v_existing_[relationship_1] TEXT[];
    v_new_[relationship_1] TEXT[];
    -- Add more variables as needed
BEGIN
    -- Extract and validate entity ID
    v_[entity_id] := p_request->>'[entity_id]';
    
    -- Validation checks
    -- Update main entity fields using COALESCE/CASE patterns
    -- Handle relationships with differential logic
    -- Return success/error response
END;
$$;
```

### Key Patterns to Follow:

1. **Field Updates**: Use `COALESCE` for simple fields, `CASE` for enums/typed fields
2. **Relationship Updates**: Use `EXCEPT` for set differences, only add/remove what changed
3. **Error Handling**: Structured responses with specific error codes
4. **Transaction Safety**: Single atomic operation with rollback on failure

### Repository Pattern:

```kotlin
suspend fun update[EntityName]Smart(
  [entity_id]: String,
  original[EntityName]: [EntityName]?,
  new[EntityName]Data: [EntityName]InputData
): Result<Boolean>
```

### ViewModel Pattern:

```kotlin
private val _original[EntityName]Data = MutableStateFlow<[EntityName]?>(null)
val original[EntityName]Data: StateFlow<[EntityName]?> = _original[EntityName]Data.asStateFlow()

fun update[EntityName]Smart(id: String, input: [EntityName]InputData)
fun has[EntityName]DataChanged(newData: [EntityName]InputData): Boolean
```

**Do you have any questions about this template or would you like me to adjust anything for your specific use cases?**
