# Development Log — 2025-07-09

## Refactoring and Modernization of Compose Form Validation Patterns

### **1. Self-Validation Pattern for Form Components**

- Migrated AddressComponent, ImagePickerComponent, and GenderDropdown (when needed) to a modern "self-validation"
  pattern.
- Instead of external validation booleans and duplicated validation code in form screens, each component now:
    - Accepts configuration (isMandatory, custom labels, etc)
    - Accepts a validateFields flag to trigger error states only after attempted submit (not instantly on load)
    - Optionally reports its own valid/invalid state back to the parent via callback
- Screens only need to provide the config and listen for validity (if they need to aggregate for submit button, etc).
- **Rationale:**
    - Prevented validation inconsistencies and duplicate logic
    - Simplifies form composition — just set config and trigger validation once
    - Makes reuse/maintenance much easier, aligning with Compose/MVI best practices

### **2. AddressComponent Enhancements**

- Extended `AddressFieldsConfig` to allow precise per-field required/optional settings
- Component manages fieldwise error display internally, reporting isValid only after form submit is attempted
- Retained backwards compatibility for legacy usages

### **3. ImagePickerComponent Major Refactor**

- Previously, parent screens had to call `validateImagePickerState` and manage `attachedDocumentsError` etc
- Component now:
    - Shows error *only* after validation is triggered
    - Child can set `isMandatory` & parent just triggers revalidation, listens to validity callback
    - Error shown under field, not in screen code
- All previous call sites updated accordingly

### **4. GenderDropdown Customizability**

- Enhanced component to allow for custom mapped display names per enum value, preserving backwards compatibility
- Allows different screens to show context-specific Hindi/usage labels for gender selection enums (e.g. GenderAllowed vs
  Gender)

### **5. Pattern Consistency Across Features**

- Updated CreateActivityFormScreen to drive all field validation via single source-of-truth state (one trigger boolean
  per field for validation, isValid booleans per field for final submission block)
- Encouraged future work to migrate other forms (`AddAryaSamajFormScreen`, etc) to use same pattern

### **Summary**

> Adopted a consistently Composable, MVI-inspired, single-source-of-truth pattern for all field validation across the
project. This brings Compose forms up to par with best state-driven UI architecture (Compose/React) and will help avoid
persistent past bugs with error timing, required/optional inconsistency, and error state leakage. Screens now only
orchestrate, all field logic is self-contained and fully testable.
