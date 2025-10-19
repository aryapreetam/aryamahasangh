#!/bin/bash

# Migration Recovery Assistant
# Handles partial failures and resumes incomplete migrations
# Makes production migrations safer and more reliable

set -e  # Exit on any error

# Configuration
RECOVERY_STATE_FILE=".migration_recovery_state"
RECOVERY_LOG_FILE="migration_recovery_$(date +%Y%m%d_%H%M%S).log"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'  
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# State tracking
declare -A PHASE_STATUS
CURRENT_PHASE=""
TOTAL_PHASES=6

# Migration phases
PHASES=(
    "configuration"
    "database_export" 
    "database_import"
    "infrastructure"
    "storage_files"
    "verification"
)

# Log function
log() {
    local level=$1
    local message=$2
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo -e "${timestamp} [$level] $message" | tee -a "$RECOVERY_LOG_FILE"
}

# Print colored output
print_status() {
    local color=$1
    local message=$2
    echo -e "${color}$message${NC}"
    log "INFO" "$message"
}

# Load recovery state
load_recovery_state() {
    if [ -f "$RECOVERY_STATE_FILE" ]; then
        log "INFO" "Loading recovery state from $RECOVERY_STATE_FILE"
        while IFS='=' read -r key value; do
            if [[ $key =~ ^[a-zA-Z_][a-zA-Z0-9_]*$ ]]; then
                PHASE_STATUS["$key"]="$value"
            fi
        done < "$RECOVERY_STATE_FILE"
        
        # Display loaded state
        print_status "$BLUE" "📋 LOADED MIGRATION STATE:"
        for phase in "${PHASES[@]}"; do
            status=${PHASE_STATUS[$phase]:-"not_started"}
            case $status in
                "completed") echo -e "   ✅ $phase: completed" ;;
                "in_progress") echo -e "   🔄 $phase: in progress" ;;
                "failed") echo -e "   ❌ $phase: failed" ;;
                *) echo -e "   ⏳ $phase: not started" ;;
            esac
        done
        echo
    else
        log "INFO" "No recovery state found, starting fresh migration"
        initialize_recovery_state
    fi
}

# Save recovery state
save_recovery_state() {
    log "INFO" "Saving recovery state to $RECOVERY_STATE_FILE"
    > "$RECOVERY_STATE_FILE"
    for phase in "${PHASES[@]}"; do
        echo "${phase}=${PHASE_STATUS[$phase]:-not_started}" >> "$RECOVERY_STATE_FILE"
    done
    echo "current_phase=$CURRENT_PHASE" >> "$RECOVERY_STATE_FILE"
    echo "timestamp=$(date '+%Y-%m-%d %H:%M:%S')" >> "$RECOVERY_STATE_FILE"
}

# Initialize recovery state
initialize_recovery_state() {
    log "INFO" "Initializing migration recovery state"
    for phase in "${PHASES[@]}"; do
        PHASE_STATUS[$phase]="not_started"
    done
    save_recovery_state
}

# Mark phase as started
start_phase() {
    local phase=$1
    CURRENT_PHASE=$phase
    PHASE_STATUS[$phase]="in_progress"
    save_recovery_state
    print_status "$BLUE" "🚀 Starting phase: $phase"
}

# Mark phase as completed
complete_phase() {
    local phase=$1
    PHASE_STATUS[$phase]="completed"
    save_recovery_state
    print_status "$GREEN" "✅ Completed phase: $phase"
}

# Mark phase as failed
fail_phase() {
    local phase=$1
    local error_message=$2
    PHASE_STATUS[$phase]="failed"
    save_recovery_state
    print_status "$RED" "❌ Failed phase: $phase - $error_message"
    log "ERROR" "Phase $phase failed: $error_message"
}

# Check if phase is completed
is_phase_completed() {
    local phase=$1
    [ "${PHASE_STATUS[$phase]}" = "completed" ]
}

# Check if phase failed
is_phase_failed() {
    local phase=$1
    [ "${PHASE_STATUS[$phase]}" = "failed" ]
}

# Get next phase to execute
get_next_phase() {
    for phase in "${PHASES[@]}"; do
        if [[ "${PHASE_STATUS[$phase]}" != "completed" ]]; then
            echo "$phase"
            return 0
        fi
    done
    echo "all_completed"
}

# Execute phase with retry logic
execute_phase_with_retry() {
    local phase=$1
    local max_retries=${2:-3}
    local retry_count=0
    
    while [ $retry_count -lt $max_retries ]; do
        if [ $retry_count -gt 0 ]; then
            print_status "$YELLOW" "⚠️ Retry attempt $retry_count/$max_retries for phase: $phase"
            sleep $((retry_count * 2))  # Exponential backoff
        fi
        
        start_phase "$phase"
        
        if execute_phase "$phase"; then
            complete_phase "$phase"
            return 0
        else
            retry_count=$((retry_count + 1))
            if [ $retry_count -lt $max_retries ]; then
                print_status "$YELLOW" "⚠️ Phase $phase failed, will retry in $((retry_count * 2)) seconds..."
            else
                fail_phase "$phase" "Max retries exceeded"
                return 1
            fi
        fi
    done
}

# Execute individual migration phase
execute_phase() {
    local phase=$1
    
    case $phase in
        "configuration")
            execute_configuration_phase
            ;;
        "database_export")
            execute_database_export_phase
            ;;
        "database_import")
            execute_database_import_phase
            ;;
        "infrastructure")
            execute_infrastructure_phase
            ;;
        "storage_files") 
            execute_storage_files_phase
            ;;
        "verification")
            execute_verification_phase
            ;;
        *)
            log "ERROR" "Unknown phase: $phase"
            return 1
            ;;
    esac
}

# Phase implementations
execute_configuration_phase() {
    log "INFO" "Executing configuration validation phase"
    
    # Check for .env.migration file
    if [ ! -f ".env.migration" ]; then
        log "ERROR" ".env.migration file not found"
        return 1
    fi
    
    # Source configuration
    source .env.migration
    
    # Validate required variables
    local required_vars=("SOURCE_PROJECT_ID" "SOURCE_URL" "SOURCE_SERVICE_KEY" "TARGET_PROJECT_ID" "TARGET_URL" "TARGET_SERVICE_KEY")
    for var in "${required_vars[@]}"; do
        if [ -z "${!var}" ]; then
            log "ERROR" "Required variable $var is not set"
            return 1
        fi
    done
    
    # Test connectivity
    log "INFO" "Testing source environment connectivity"
    if ! supabase link --project-ref "$SOURCE_PROJECT_ID" >/dev/null 2>&1; then
        log "ERROR" "Cannot connect to source environment"
        return 1
    fi
    
    log "INFO" "Testing target environment connectivity"
    if ! supabase link --project-ref "$TARGET_PROJECT_ID" >/dev/null 2>&1; then
        log "ERROR" "Cannot connect to target environment"
        return 1
    fi
    
    log "INFO" "Configuration phase completed successfully"
    return 0
}

execute_database_export_phase() {
    log "INFO" "Executing database export phase"
    source .env.migration
    
    local export_file="migration_export_$(date +%Y%m%d_%H%M%S).sql"
    
    # Check if export already exists and is recent
    if [ -f "complete_export.sql" ] && [ $(($(date +%s) - $(stat -c %Y "complete_export.sql"))) -lt 3600 ]; then
        log "INFO" "Recent export file found, skipping re-export"
        return 0
    fi
    
    # Link to source and export
    supabase link --project-ref "$SOURCE_PROJECT_ID"
    
    log "INFO" "Starting database export from source environment"
    if ! supabase db dump --linked --file "$export_file"; then
        log "ERROR" "Database export failed"
        return 1
    fi
    
    # Create symbolic link for consistency
    ln -sf "$export_file" "complete_export.sql"
    
    log "INFO" "Database export completed: $export_file"
    return 0
}

execute_database_import_phase() {
    log "INFO" "Executing database import phase"
    source .env.migration
    
    if [ ! -f "complete_export.sql" ]; then
        log "ERROR" "Export file not found. Cannot proceed with import."
        return 1
    fi
    
    # Link to target environment  
    supabase link --project-ref "$TARGET_PROJECT_ID"
    
    # Check if target needs reset
    log "INFO" "Checking target environment state"
    local table_count=$(supabase db remote --linked exec "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';" | tail -n +3 | head -n 1 | xargs)
    
    if [ "$table_count" -gt 0 ]; then
        log "INFO" "Target environment has $table_count tables. Resetting..."
        if ! supabase db reset --linked; then
            log "ERROR" "Failed to reset target environment"
            return 1
        fi
    fi
    
    log "INFO" "Starting database import to target environment"
    if ! cat complete_export.sql | supabase db reset --linked --sql; then
        log "ERROR" "Database import failed"
        return 1
    fi
    
    log "INFO" "Database import completed successfully"
    return 0
}

execute_infrastructure_phase() {
    log "INFO" "Executing infrastructure configuration phase"
    source .env.migration
    
    # Link to target environment
    supabase link --project-ref "$TARGET_PROJECT_ID"
    
    log "INFO" "Configuring GraphQL settings"
    if ! supabase db remote --linked exec "
        COMMENT ON SCHEMA public IS '@graphql({\"inflect_names\": true})';
        COMMENT ON TABLE arya_samaj IS '@graphql({\"totalCount\": {\"enabled\": true}})';
        COMMENT ON TABLE family IS '@graphql({\"totalCount\": {\"enabled\": true}})';
        COMMENT ON TABLE family_member IS '@graphql({\"totalCount\": {\"enabled\": true}})';
        COMMENT ON TABLE member IS '@graphql({\"totalCount\": {\"enabled\": true}})';
    "; then
        log "ERROR" "GraphQL configuration failed"
        return 1
    fi
    
    log "INFO" "Configuring Realtime subscriptions"
    if ! supabase db remote --linked exec "
        ALTER PUBLICATION supabase_realtime ADD TABLE public.arya_samaj;
        ALTER PUBLICATION supabase_realtime ADD TABLE public.family;
        ALTER PUBLICATION supabase_realtime ADD TABLE public.family_member;
        ALTER PUBLICATION supabase_realtime ADD TABLE public.member;
        ALTER PUBLICATION supabase_realtime ADD TABLE public.organisational_member;
        ALTER PUBLICATION supabase_realtime ADD TABLE public.satr_registration;
        ALTER PUBLICATION supabase_realtime ADD TABLE public.activities;
        ALTER PUBLICATION supabase_realtime ADD TABLE public.organisation;
    "; then
        log "ERROR" "Realtime configuration failed"
        return 1
    fi
    
    log "INFO" "Infrastructure configuration completed successfully"
    return 0
}

execute_storage_files_phase() {
    log "INFO" "Executing storage files migration phase"
    
    # Check if storage migration script exists
    if [ ! -f "scripts/migrate-storage-files.main.kts" ]; then
        log "WARN" "Storage migration script not found, skipping file migration"
        return 0
    fi
    
    # Check if SKIP_STORAGE is set
    if [ "$SKIP_STORAGE" = "true" ]; then
        log "INFO" "Storage migration skipped (SKIP_STORAGE=true)"
        return 0
    fi
    
    log "INFO" "Starting storage files migration"
    if ! kotlin scripts/migrate-storage-files.main.kts; then
        log "ERROR" "Storage files migration failed"
        return 1
    fi
    
    # URL updates
    source .env.migration
    supabase link --project-ref "$TARGET_PROJECT_ID"
    
    local source_ref=$(echo $SOURCE_URL | sed 's/https:\/\/\(.*\)\.supabase\.co/\1/')
    local target_ref=$(echo $TARGET_URL | sed 's/https:\/\/\(.*\)\.supabase\.co/\1/')
    
    log "INFO" "Updating database URLs from $source_ref to $target_ref"
    if ! supabase db remote --linked exec "
        UPDATE organisation SET logo = REPLACE(logo, '$source_ref.supabase.co', '$target_ref.supabase.co') WHERE logo LIKE '%$source_ref%';
        UPDATE member SET profile_image = REPLACE(profile_image, '$source_ref.supabase.co', '$target_ref.supabase.co') WHERE profile_image LIKE '%$source_ref%';
        UPDATE learning SET thumbnail_url = REPLACE(thumbnail_url, '$source_ref.supabase.co', '$target_ref.supabase.co') WHERE thumbnail_url LIKE '%$source_ref%';
        UPDATE activities SET media_files = (SELECT array_agg(REPLACE(url_elem, '$source_ref.supabase.co', '$target_ref.supabase.co')) FROM unnest(media_files) AS url_elem) WHERE array_to_string(media_files, '|') LIKE '%$source_ref%';
        UPDATE activities SET overview_media_urls = (SELECT array_agg(REPLACE(url_elem, '$source_ref.supabase.co', '$target_ref.supabase.co')) FROM unnest(overview_media_urls) AS url_elem) WHERE array_to_string(overview_media_urls, '|') LIKE '%$source_ref%';
    "; then
        log "ERROR" "URL updates failed"
        return 1
    fi
    
    log "INFO" "Storage files migration completed successfully"
    return 0
}

execute_verification_phase() {
    log "INFO" "Executing verification phase"
    source .env.migration
    
    # Link to target environment
    supabase link --project-ref "$TARGET_PROJECT_ID"
    
    # Test basic connectivity
    log "INFO" "Testing target environment connectivity"
    if ! supabase db remote --linked exec "SELECT 1;" >/dev/null 2>&1; then
        log "ERROR" "Target environment connectivity test failed"
        return 1
    fi
    
    # Check database objects
    log "INFO" "Verifying database objects"
    local table_count=$(supabase db remote --linked exec "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';" | tail -n +3 | head -n 1 | xargs)
    if [ "$table_count" -lt 5 ]; then
        log "ERROR" "Insufficient tables in target environment ($table_count found, expected at least 5)"
        return 1
    fi
    
    # Check for critical tables
    local critical_tables=("organisation" "member" "activities")
    for table in "${critical_tables[@]}"; do
        local exists=$(supabase db remote --linked exec "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = '$table';" | tail -n +3 | head -n 1 | xargs)
        if [ "$exists" -eq 0 ]; then
            log "ERROR" "Critical table '$table' not found in target environment"
            return 1
        fi
    done
    
    # Check RLS policies  
    log "INFO" "Verifying RLS policies"
    local policy_count=$(supabase db remote --linked exec "SELECT COUNT(*) FROM pg_policies WHERE schemaname = 'public';" | tail -n +3 | head -n 1 | xargs)
    if [ "$policy_count" -eq 0 ]; then
        log "WARN" "No RLS policies found in target environment"
    fi
    
    log "INFO" "Verification phase completed successfully"
    return 0
}

# Generate recovery report
generate_recovery_report() {
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    local report_file="migration_recovery_report_$(date +%Y%m%d_%H%M%S).md"
    
    cat > "$report_file" << EOF
# Migration Recovery Report

**Generated:** $timestamp  
**Recovery Log:** $RECOVERY_LOG_FILE

## Phase Status

| Phase | Status | Notes |
|-------|--------|-------|
EOF

    for phase in "${PHASES[@]}"; do
        local status=${PHASE_STATUS[$phase]:-"not_started"}
        local icon="⏳"
        case $status in
            "completed") icon="✅" ;;
            "in_progress") icon="🔄" ;;
            "failed") icon="❌" ;;
        esac
        echo "| $phase | $icon $status | |" >> "$report_file"
    done

    cat >> "$report_file" << EOF

## Recovery Commands

To resume from a specific phase:
\`\`\`bash
./scripts/migration-recovery.sh --resume-from=PHASE_NAME
\`\`\`

To reset and start over:
\`\`\`bash  
./scripts/migration-recovery.sh --reset
\`\`\`

To skip problematic phases:
\`\`\`bash
export SKIP_STORAGE=true  # Skip storage migration
./scripts/migration-recovery.sh --resume
\`\`\`

## Troubleshooting

Check the recovery log for detailed error information:
\`\`\`bash
tail -f $RECOVERY_LOG_FILE
\`\`\`

EOF

    print_status "$BLUE" "📝 Recovery report generated: $report_file"
}

# Show usage
show_usage() {
    cat << EOF
Migration Recovery Assistant

USAGE:
    $0 [OPTIONS]

OPTIONS:
    --resume              Resume migration from last incomplete phase
    --resume-from=PHASE   Resume migration from specific phase
    --reset              Reset recovery state and start fresh
    --status             Show current migration status
    --list-phases        List all migration phases
    --help               Show this help message

PHASES:
    configuration     Validate configuration and connectivity
    database_export   Export database from source environment
    database_import   Import database to target environment  
    infrastructure    Configure GraphQL, Realtime, etc.
    storage_files     Migrate storage files and update URLs
    verification      Verify migration completed successfully

EXAMPLES:
    # Start new migration with recovery support
    $0
    
    # Resume failed migration
    $0 --resume
    
    # Resume from specific phase
    $0 --resume-from=storage_files
    
    # Check migration status
    $0 --status
    
    # Reset and start over
    $0 --reset

ENVIRONMENT VARIABLES:
    SKIP_STORAGE=true    Skip storage file migration phase
    MAX_RETRIES=3        Maximum retry attempts per phase (default: 3)

EOF
}

# Show current migration status
show_migration_status() {
    load_recovery_state
    
    print_status "$BLUE" "📊 MIGRATION STATUS REPORT"
    echo "=================================="
    
    local completed=0
    local failed=0
    local in_progress=0
    local not_started=0
    
    for phase in "${PHASES[@]}"; do
        local status=${PHASE_STATUS[$phase]:-"not_started"}
        case $status in
            "completed") 
                echo -e "   ✅ $phase: completed"
                ((completed++))
                ;;
            "in_progress") 
                echo -e "   🔄 $phase: in progress"
                ((in_progress++))
                ;;
            "failed") 
                echo -e "   ❌ $phase: failed"
                ((failed++))
                ;;
            *) 
                echo -e "   ⏳ $phase: not started"
                ((not_started++))
                ;;
        esac
    done
    
    echo
    echo "Summary:"
    echo "  ✅ Completed: $completed/$TOTAL_PHASES"
    echo "  🔄 In Progress: $in_progress"  
    echo "  ❌ Failed: $failed"
    echo "  ⏳ Not Started: $not_started"
    
    if [ $completed -eq $TOTAL_PHASES ]; then
        print_status "$GREEN" "🎉 Migration completed successfully!"
    elif [ $failed -gt 0 ]; then
        print_status "$RED" "⚠️ Migration has failed phases - use --resume to continue"
    elif [ $in_progress -gt 0 ]; then
        print_status "$YELLOW" "🔄 Migration is currently in progress"
    else
        print_status "$BLUE" "📋 Migration is ready to start"
    fi
}

# List all phases
list_phases() {
    print_status "$BLUE" "📋 MIGRATION PHASES"
    echo "==================="
    for i in "${!PHASES[@]}"; do
        local phase=${PHASES[$i]}
        printf "%d. %-18s %s\n" $((i+1)) "$phase" "($(echo $phase | sed 's/_/ /g'))"
    done
}

# Reset recovery state
reset_recovery_state() {
    print_status "$YELLOW" "🔄 Resetting migration recovery state..."
    
    if [ -f "$RECOVERY_STATE_FILE" ]; then
        rm -f "$RECOVERY_STATE_FILE"
        log "INFO" "Removed recovery state file"
    fi
    
    # Clean up migration artifacts
    rm -f complete_export.sql migration_export_*.sql
    
    print_status "$GREEN" "✅ Recovery state reset. Ready for fresh migration."
}

# Main execution
main() {
    print_status "$BLUE" "🛠️ MIGRATION RECOVERY ASSISTANT"
    print_status "$BLUE" "================================"
    
    # Parse command line arguments
    case "${1:-}" in
        "--help")
            show_usage
            exit 0
            ;;
        "--status")
            show_migration_status
            exit 0
            ;;
        "--list-phases")
            list_phases
            exit 0
            ;;
        "--reset")
            reset_recovery_state
            exit 0
            ;;
        "--resume")
            load_recovery_state
            ;;
        --resume-from=*)
            load_recovery_state
            local target_phase="${1#*=}"
            if [[ ! " ${PHASES[@]} " =~ " ${target_phase} " ]]; then
                print_status "$RED" "❌ Invalid phase: $target_phase"
                list_phases
                exit 1
            fi
            # Mark previous phases as completed
            for phase in "${PHASES[@]}"; do
                if [ "$phase" = "$target_phase" ]; then
                    break
                fi
                PHASE_STATUS[$phase]="completed"
            done
            save_recovery_state
            ;;
        "")
            # Default: start fresh or resume
            load_recovery_state
            ;;
        *)
            print_status "$RED" "❌ Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
    
    # Execute migration phases
    local max_retries=${MAX_RETRIES:-3}
    local overall_success=true
    
    while true; do
        local next_phase=$(get_next_phase)
        
        if [ "$next_phase" = "all_completed" ]; then
            print_status "$GREEN" "🎉 All migration phases completed successfully!"
            break
        fi
        
        print_status "$BLUE" "➡️ Next phase: $next_phase"
        
        if ! execute_phase_with_retry "$next_phase" "$max_retries"; then
            print_status "$RED" "💥 Migration failed at phase: $next_phase"
            overall_success=false
            break
        fi
        
        print_status "$GREEN" "✅ Phase $next_phase completed successfully"
        echo
    done
    
    # Generate final report
    generate_recovery_report
    
    if [ "$overall_success" = true ]; then
        print_status "$GREEN" "🎊 MIGRATION COMPLETED SUCCESSFULLY!"
        print_status "$GREEN" "📋 All phases executed without errors"
        
        # Clean up recovery state on success
        rm -f "$RECOVERY_STATE_FILE"
    else
        print_status "$RED" "💥 MIGRATION INCOMPLETE"
        print_status "$YELLOW" "📋 Use '$0 --resume' to continue from where it failed"
        print_status "$BLUE" "📝 Check the recovery log for details: $RECOVERY_LOG_FILE"
        exit 1
    fi
}

# Trap signals to save state
trap 'fail_phase "$CURRENT_PHASE" "Interrupted by signal"; exit 130' INT TERM

# Execute main function
main "$@"
