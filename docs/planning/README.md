# Planning Documentation

This directory contains forward-looking planning documents and project roadmaps for the Arya Mahasangh platform.

## Directory Structure

```
docs/
├── planning/                           # Forward-looking plans & roadmaps
│   ├── README.md                       # This file
│   ├── 2025-06-27-crud-integration-plan.md # CRUD Integration Development Plan
│   └── [future-planning-documents]     # Additional planning documents
├── development-logs/                   # Historical development records
│   ├── 2025-06-26-initial-supabase-setup-and-functions.md
│   └── 2025-06-27-supabase-crud-functions.md
├── architecture/                       # Technical architecture docs
└── [other-documentation]               # General project documentation
```

## Document Types

### Development Plans

- **Purpose**: Step-by-step implementation plans with progress tracking
- **Format**: Markdown with checkboxes for task completion
- **Naming**: `YYYY-MM-DD-[feature-name]-plan.md`
- **Example**: `2025-06-27-crud-integration-plan.md`

### Roadmaps

- **Purpose**: High-level strategic planning and feature prioritization
- **Format**: Markdown with timeline and milestone tracking
- **Naming**: `YYYY-[quarter]-roadmap.md`
- **Example**: `2025-q3-roadmap.md`

### Architecture Decisions

- **Purpose**: Major technical decisions and their rationale
- **Format**: ADR (Architecture Decision Record) format
- **Naming**: `ADR-[sequence]-[decision-name].md`
- **Example**: `ADR-001-database-choice.md`

## How to Use

1. **Active Plans**: Documents marked as "IN PROGRESS" are currently being executed
2. **Progress Tracking**: Use checkboxes to mark completed tasks
3. **Status Updates**: Update document headers with current status and last modified date
4. **Cross-References**: Link related planning documents and development logs

## Related Directories

- `docs/development-logs/` - Historical records of completed work
- `docs/architecture/` - Technical architecture documentation
- `docs/` - General project documentation

## Workflow

1. **Create plans** in `docs/planning/`
2. **Execute and track progress** using the checkboxes
3. **Archive completed plans** to `docs/development-logs/` with completion notes
4. **Reference historical work** from `docs/development-logs/` when planning new features

## Guidelines

- Keep planning documents focused and actionable
- Update progress regularly to maintain accuracy
- Archive completed plans by moving them to `docs/development-logs/` with completion notes
- Use consistent formatting and naming conventions
- Include time estimates and completion criteria for each phase
- Cross-reference between planning and development-logs for context
