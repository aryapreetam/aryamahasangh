#!/bin/bash

# 🛡️ Backup & Rollback Generator
# 
# Comprehensive backup and rollback utility for Supabase migrations with:
# - Complete environment backups before migration
# - Automatic rollback script generation
# - Rollback procedure validation
# - Backup retention and cleanup management
# - State-based recovery planning

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKUP_BASE_DIR="backups"
ROLLBACK_SCRIPTS_DIR="rollback-scripts"
MAX_BACKUP_AGE_DAYS=30
BACKUP_TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Load environment configuration
if [ ! -f ".env.migration" ]; then
    echo "❌ .env.migration file not found!"
    echo "📋 Create .env.migration with your environment configuration"
    exit 1
fi

source .env.migration

# Logging functions
log_info() {
    echo "ℹ️ $(date '+%Y-%m-%d %H:%M:%S') [INFO] $1"
    echo "$(date '+%Y-%m-%d %H:%M:%S') [INFO] $1" >> "${BACKUP_BASE_DIR}/backup_${BACKUP_TIMESTAMP}.log"
}

log_warning() {
    echo "⚠️ $(date '+%Y-%m-%d %H:%M:%S') [WARNING] $1"
    echo "$(date '+%Y-%m-%d %H:%M:%S') [WARNING] $1" >> "${BACKUP_BASE_DIR}/backup_${BACKUP_TIMESTAMP}.log"
}

log_error() {
    echo "🚨 $(date '+%Y-%m-%d %H:%M:%S') [ERROR] $1"
    echo "$(date '+%Y-%m-%d %H:%M:%S') [ERROR] $1" >> "${BACKUP_BASE_DIR}/backup_${BACKUP_TIMESTAMP}.log"
}

# Utility functions
create_directories() {
    mkdir -p "${BACKUP_BASE_DIR}/${BACKUP_TIMESTAMP}"
    mkdir -p "${ROLLBACK_SCRIPTS_DIR}/${BACKUP_TIMESTAMP}"
    mkdir -p "${BACKUP_BASE_DIR}/metadata"
}

# Database backup functions
backup_database_schema() {
    local project_id=$1
    local environment_name=$2
    local output_file=$3
    
    log_info "Backing up database schema for $environment_name..."
    
    supabase link --project-ref "$project_id" --quiet
    
    if supabase db dump --linked --schema-only --file "$output_file"; then
        log_info "✅ Schema backup completed: $output_file"
        
        # Verify backup file
        if [ -s "$output_file" ]; then
            local line_count=$(wc -l < "$output_file")
            log_info "📊 Schema backup contains $line_count lines"
        else
            log_error "❌ Schema backup file is empty!"
            return 1
        fi
    else
        log_error "❌ Schema backup failed for $environment_name"
        return 1
    fi
}

backup_database_data() {
    local project_id=$1
    local environment_name=$2
    local output_file=$3
    
    log_info "Backing up database data for $environment_name..."
    
    supabase link --project-ref "$project_id" --quiet
    
    if supabase db dump --linked --data-only --file "$output_file"; then
        log_info "✅ Data backup completed: $output_file"
        
        # Verify backup file
        if [ -s "$output_file" ]; then
            local line_count=$(wc -l < "$output_file")
            log_info "📊 Data backup contains $line_count lines"
        else
            log_warning "⚠️ Data backup file is empty (might be expected for new environments)"
        fi
    else
        log_error "❌ Data backup failed for $environment_name"
        return 1
    fi
}

backup_database_complete() {
    local project_id=$1
    local environment_name=$2
    local output_file=$3
    
    log_info "Creating complete database backup for $environment_name..."
    
    supabase link --project-ref "$project_id" --quiet
    
    if supabase db dump --linked --file "$output_file"; then
        log_info "✅ Complete backup created: $output_file"
        
        # Verify and get statistics
        if [ -s "$output_file" ]; then
            local size=$(du -h "$output_file" | cut -f1)
            local line_count=$(wc -l < "$output_file")
            log_info "📊 Complete backup: $size, $line_count lines"
        else
            log_error "❌ Complete backup file is empty!"
            return 1
        fi
    else
        log_error "❌ Complete backup failed for $environment_name"
        return 1
    fi
}

# Configuration backup functions
backup_environment_config() {
    local project_id=$1
    local environment_name=$2
    local backup_dir=$3
    
    log_info "Backing up environment configuration for $environment_name..."
    
    local config_dir="${backup_dir}/config"
    mkdir -p "$config_dir"
    
    # Save environment details
    cat > "${config_dir}/environment_details.json" << EOF
{
  "environment_name": "$environment_name",
  "project_id": "$project_id",
  "backup_timestamp": "$BACKUP_TIMESTAMP",
  "backup_type": "pre_migration",
  "supabase_cli_version": "$(supabase --version 2>/dev/null || echo 'unknown')",
  "created_by": "$(whoami)",
  "host": "$(hostname)"
}
EOF

    # Save project configuration if available
    if supabase projects list --output json > "${config_dir}/projects.json" 2>/dev/null; then
        log_info "✅ Project list saved"
    else
        log_warning "⚠️ Could not retrieve project list"
    fi
    
    # Save current migration state
    supabase link --project-ref "$project_id" --quiet
    if supabase migration list --output json > "${config_dir}/migrations.json" 2>/dev/null; then
        log_info "✅ Migration history saved"
    else
        log_warning "⚠️ Could not retrieve migration history"
    fi
    
    log_info "✅ Environment configuration backup completed"
}

# Storage backup functions  
backup_storage_inventory() {
    local project_id=$1
    local environment_name=$2
    local backup_dir=$3
    
    log_info "Creating storage inventory for $environment_name..."
    
    local storage_dir="${backup_dir}/storage"
    mkdir -p "$storage_dir"
    
    # Create Kotlin script to inventory storage
    cat > "${storage_dir}/inventory_storage.main.kts" << 'EOF'
#!/usr/bin/env kotlin

@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("io.github.jan-tennert.supabase:supabase-kt:2.0.0")
@file:DependsOn("io.github.jan-tennert.supabase:storage-kt:2.0.0")
@file:DependsOn("io.github.jan-tennert.supabase:postgrest-kt:2.0.0")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import java.io.File
import java.util.Properties

fun loadConfig(): Properties {
    val props = Properties()
    try {
        File("../../.env.migration").inputStream().use { props.load(it) }
    } catch (e: Exception) {
        println("⚠️ Could not load migration config: ${e.message}")
    }
    return props
}

data class FileReference(
    val table: String,
    val column: String,
    val path: String,
    val url: String,
    val bucket: String,
    val fileName: String
)

fun extractFilePathFromUrl(url: String): String? {
    val regex = Regex("""/storage/v1/object/public/([^?]+)""")
    return regex.find(url)?.groupValues?.get(1)
}

runBlocking {
    val config = loadConfig()
    val projectUrl = System.getenv("PROJECT_URL") ?: config.getProperty("SOURCE_URL")
    val serviceKey = System.getenv("SERVICE_KEY") ?: config.getProperty("SOURCE_SERVICE_KEY")
    
    if (projectUrl == null || serviceKey == null) {
        println("❌ Missing PROJECT_URL or SERVICE_KEY environment variables")
        return@runBlocking
    }
    
    val client = createSupabaseClient(projectUrl, serviceKey) {
        install(Storage)
        install(Postgrest)
    }
    
    println("📦 Creating storage inventory...")
    
    val fileReferences = mutableListOf<FileReference>()
    
    // Scan for file references in database tables
    val queries = listOf(
        Triple("organisation", "logo", "single"),
        Triple("member", "profile_image", "single"),
        Triple("activities", "media_files", "array"),
        Triple("activities", "overview_media_urls", "array"),
        Triple("learning", "thumbnail_url", "single"),
        Triple("family", "photos", "array"),
        Triple("arya_samaj", "media_urls", "array")
    )
    
    for ((table, column, type) in queries) {
        try {
            println("🔍 Scanning $table.$column...")
            
            val data = client.from(table)
                .select(columns = arrayOf(column))
                .decodeList<JsonObject>()
                .filter { 
                    val value = it[column]
                    value != null && value != JsonNull
                }
            
            for (record in data) {
                val value = record[column]
                
                when (type) {
                    "single" -> {
                        if (value is JsonPrimitive && value.isString && value.content.isNotBlank()) {
                            val path = extractFilePathFromUrl(value.content)
                            if (path != null) {
                                val pathParts = path.split('/', limit = 2)
                                if (pathParts.size == 2) {
                                    fileReferences.add(
                                        FileReference(
                                            table = table,
                                            column = column,
                                            path = path,
                                            url = value.content,
                                            bucket = pathParts[0],
                                            fileName = pathParts[1]
                                        )
                                    )
                                }
                            }
                        }
                    }
                    
                    "array" -> {
                        if (value is JsonArray) {
                            for (url in value) {
                                if (url is JsonPrimitive && url.isString && url.content.isNotBlank()) {
                                    val path = extractFilePathFromUrl(url.content)
                                    if (path != null) {
                                        val pathParts = path.split('/', limit = 2)
                                        if (pathParts.size == 2) {
                                            fileReferences.add(
                                                FileReference(
                                                    table = table,
                                                    column = column,
                                                    path = path,
                                                    url = url.content,
                                                    bucket = pathParts[0],
                                                    fileName = pathParts[1]
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("⚠️ Error scanning $table.$column: ${e.message}")
        }
    }
    
    // Remove duplicates based on path
    val uniqueFiles = fileReferences.distinctBy { it.path }
    
    println("📊 Found ${uniqueFiles.size} unique files in ${fileReferences.size} references")
    
    // Save inventory to JSON
    val inventoryJson = buildString {
        appendLine("{")
        appendLine("  \"inventory_timestamp\": \"${java.time.LocalDateTime.now()}\",")
        appendLine("  \"total_files\": ${uniqueFiles.size},")
        appendLine("  \"total_references\": ${fileReferences.size},")
        appendLine("  \"files\": [")
        
        uniqueFiles.forEachIndexed { index, file ->
            appendLine("    {")
            appendLine("      \"table\": \"${file.table}\",")
            appendLine("      \"column\": \"${file.column}\",")
            appendLine("      \"path\": \"${file.path}\",")
            appendLine("      \"url\": \"${file.url}\",")
            appendLine("      \"bucket\": \"${file.bucket}\",")
            appendLine("      \"fileName\": \"${file.fileName}\"")
            append("    }")
            if (index < uniqueFiles.size - 1) appendLine(",")
            else appendLine()
        }
        
        appendLine("  ],")
        appendLine("  \"bucket_summary\": {")
        
        val bucketGroups = uniqueFiles.groupBy { it.bucket }
        bucketGroups.entries.forEachIndexed { index, (bucket, files) ->
            append("    \"$bucket\": ${files.size}")
            if (index < bucketGroups.size - 1) appendLine(",")
            else appendLine()
        }
        
        appendLine("  }")
        appendLine("}")
    }
    
    File("storage_inventory.json").writeText(inventoryJson)
    
    // Create simple CSV for easy viewing
    val csvContent = buildString {
        appendLine("bucket,fileName,table,column,url")
        uniqueFiles.forEach { file ->
            appendLine("${file.bucket},${file.fileName},${file.table},${file.column},${file.url}")
        }
    }
    
    File("storage_inventory.csv").writeText(csvContent)
    
    println("✅ Storage inventory saved:")
    println("   📄 storage_inventory.json")
    println("   📊 storage_inventory.csv")
    
    bucketGroups.forEach { (bucket, files) ->
        println("   📦 $bucket: ${files.size} files")
    }
}
EOF

    # Execute storage inventory
    cd "$storage_dir"
    PROJECT_URL="$SOURCE_URL" SERVICE_KEY="$SOURCE_SERVICE_KEY" kotlin inventory_storage.main.kts
    cd - > /dev/null
    
    if [ -f "${storage_dir}/storage_inventory.json" ]; then
        log_info "✅ Storage inventory completed"
        local file_count=$(grep -o '"total_files":' "${storage_dir}/storage_inventory.json" | wc -l)
        log_info "📊 Found files to track for rollback"
    else
        log_warning "⚠️ Storage inventory incomplete"
    fi
}

# Rollback script generation functions
generate_database_rollback() {
    local backup_file=$1
    local rollback_script=$2
    local environment_name=$3
    
    log_info "Generating database rollback script for $environment_name..."
    
    cat > "$rollback_script" << EOF
#!/bin/bash

# 🔄 Database Rollback Script
# Generated: $BACKUP_TIMESTAMP
# Environment: $environment_name
# 
# ⚠️  WARNING: This will completely restore the database to its pre-migration state
# ⚠️  All data changes since migration will be LOST!

set -e

ROLLBACK_TIMESTAMP=\$(date +%Y%m%d_%H%M%S)

echo "🔄 STARTING DATABASE ROLLBACK"
echo "=============================="
echo "⚠️  Environment: $environment_name"
echo "⚠️  Backup from: $BACKUP_TIMESTAMP"
echo "⚠️  Rollback time: \$ROLLBACK_TIMESTAMP"
echo ""

read -p "⚠️  Are you sure you want to proceed? This will DELETE current data! (yes/no): " confirm
if [ "\$confirm" != "yes" ]; then
    echo "❌ Rollback cancelled"
    exit 1
fi

echo "📋 Step 1: Creating pre-rollback snapshot..."
supabase db dump --linked --file "pre_rollback_snapshot_\$ROLLBACK_TIMESTAMP.sql" || echo "⚠️ Could not create snapshot"

echo "📋 Step 2: Resetting target database..."
supabase db reset --linked

echo "📋 Step 3: Restoring from backup..."
if [ -f "$backup_file" ]; then
    cat "$backup_file" | supabase db reset --linked --sql
    echo "✅ Database restored from backup"
else
    echo "❌ Backup file not found: $backup_file"
    exit 1
fi

echo "📋 Step 4: Verifying restoration..."
supabase db remote --linked exec "
SELECT 
    schemaname,
    COUNT(*) as table_count
FROM pg_tables 
WHERE schemaname = 'public'
GROUP BY schemaname;
"

echo ""
echo "🎉 DATABASE ROLLBACK COMPLETED"
echo "==============================="
echo "📊 Restored from: $backup_file"
echo "📅 Original backup: $BACKUP_TIMESTAMP"
echo "📅 Rollback completed: \$ROLLBACK_TIMESTAMP"
echo ""
echo "📋 Next steps:"
echo "   1. Verify application functionality"
echo "   2. Check data integrity"
echo "   3. Update any environment-specific configurations"
EOF

    chmod +x "$rollback_script"
    log_info "✅ Database rollback script generated: $rollback_script"
}

generate_complete_rollback() {
    local backup_dir=$1
    local environment_name=$2
    
    log_info "Generating complete rollback script for $environment_name..."
    
    local rollback_script="${ROLLBACK_SCRIPTS_DIR}/${BACKUP_TIMESTAMP}/complete_rollback_${environment_name}.sh"
    
    cat > "$rollback_script" << EOF
#!/bin/bash

# 🔄 Complete Environment Rollback Script
# Generated: $BACKUP_TIMESTAMP
# Environment: $environment_name
# 
# ⚠️  WARNING: This will restore the complete environment to pre-migration state
# ⚠️  Database, configurations, and file references will be restored

set -e

ROLLBACK_TIMESTAMP=\$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="$backup_dir"

echo "🔄 COMPLETE ENVIRONMENT ROLLBACK"
echo "================================="
echo "⚠️  Environment: $environment_name"  
echo "⚠️  Backup from: $BACKUP_TIMESTAMP"
echo "⚠️  Rollback time: \$ROLLBACK_TIMESTAMP"
echo ""

# Safety confirmation
read -p "⚠️  Are you sure you want to proceed? This will RESET the entire environment! (yes/no): " confirm
if [ "\$confirm" != "yes" ]; then
    echo "❌ Rollback cancelled"
    exit 1
fi

# Step 1: Create emergency snapshot
echo "📋 Step 1: Creating emergency snapshot..."
mkdir -p "emergency_snapshots/\$ROLLBACK_TIMESTAMP"
supabase db dump --linked --file "emergency_snapshots/\$ROLLBACK_TIMESTAMP/pre_rollback.sql" || echo "⚠️ Could not create emergency snapshot"

# Step 2: Database rollback
echo "📋 Step 2: Rolling back database..."
if [ -f "\$BACKUP_DIR/complete_backup.sql" ]; then
    supabase db reset --linked
    cat "\$BACKUP_DIR/complete_backup.sql" | supabase db reset --linked --sql
    echo "✅ Database rollback completed"
else
    echo "❌ Database backup not found: \$BACKUP_DIR/complete_backup.sql"
    exit 1
fi

# Step 3: Configuration rollback (manual step)
echo "📋 Step 3: Configuration rollback..."
echo "ℹ️  Manual steps required:"
if [ -f "\$BACKUP_DIR/config/environment_details.json" ]; then
    echo "   📄 Review: \$BACKUP_DIR/config/environment_details.json"
    echo "   🔧 Restore authentication settings in Supabase Dashboard"
    echo "   🔧 Restore storage bucket permissions" 
    echo "   🔧 Restore GraphQL configuration"
    echo "   🔧 Restore Realtime subscriptions"
fi

# Step 4: File reference verification
echo "📋 Step 4: File reference verification..."
if [ -f "\$BACKUP_DIR/storage/storage_inventory.json" ]; then
    echo "ℹ️  Storage inventory available: \$BACKUP_DIR/storage/storage_inventory.json"
    echo "ℹ️  Verify file URLs point to correct environment"
else
    echo "⚠️ No storage inventory found"
fi

# Step 5: Validation
echo "📋 Step 5: Post-rollback validation..."
echo "🔍 Running basic validation..."

supabase db remote --linked exec "
-- Verify core tables exist
SELECT 
    'Table Verification' as check_type,
    tablename,
    'EXISTS' as status
FROM pg_tables 
WHERE schemaname = 'public' 
  AND tablename IN ('organisation', 'member', 'activities', 'arya_samaj', 'family')
ORDER BY tablename;
"

echo ""
echo "🎉 ROLLBACK COMPLETED"
echo "===================="
echo "✅ Database restored from $BACKUP_TIMESTAMP"
echo "✅ Configuration references provided"
echo "✅ Storage inventory available"
echo ""
echo "📋 MANUAL VERIFICATION REQUIRED:"
echo "   1. Test application functionality"
echo "   2. Verify authentication works"
echo "   3. Check storage file access"
echo "   4. Validate data integrity"
echo "   5. Review environment configurations"
echo ""
echo "📁 Rollback artifacts saved in: emergency_snapshots/\$ROLLBACK_TIMESTAMP/"
EOF

    chmod +x "$rollback_script"
    log_info "✅ Complete rollback script generated: $rollback_script"
}

# Validation functions
validate_rollback_procedures() {
    local backup_dir=$1
    local environment_name=$2
    
    log_info "Validating rollback procedures for $environment_name..."
    
    local validation_log="${backup_dir}/rollback_validation.log"
    
    echo "🔍 ROLLBACK PROCEDURE VALIDATION" > "$validation_log"
    echo "=================================" >> "$validation_log"
    echo "Timestamp: $(date)" >> "$validation_log"
    echo "Environment: $environment_name" >> "$validation_log"
    echo "" >> "$validation_log"
    
    local validation_passed=true
    
    # Check backup files exist
    echo "📋 Checking backup files..." >> "$validation_log"
    
    local required_files=(
        "complete_backup.sql"
        "schema_backup.sql"
        "data_backup.sql"
        "config/environment_details.json"
    )
    
    for file in "${required_files[@]}"; do
        if [ -f "${backup_dir}/${file}" ]; then
            echo "  ✅ $file exists" >> "$validation_log"
            
            # Check file is not empty
            if [ -s "${backup_dir}/${file}" ]; then
                echo "     📊 File size: $(du -h "${backup_dir}/${file}" | cut -f1)" >> "$validation_log"
            else
                echo "     ⚠️ File is empty" >> "$validation_log"
                validation_passed=false
            fi
        else
            echo "  ❌ $file missing" >> "$validation_log"
            validation_passed=false
        fi
    done
    
    # Check rollback scripts exist and are executable
    echo "" >> "$validation_log"
    echo "📋 Checking rollback scripts..." >> "$validation_log"
    
    local rollback_scripts_dir="${ROLLBACK_SCRIPTS_DIR}/${BACKUP_TIMESTAMP}"
    if [ -d "$rollback_scripts_dir" ]; then
        for script in "$rollback_scripts_dir"/*.sh; do
            if [ -f "$script" ]; then
                local script_name=$(basename "$script")
                if [ -x "$script" ]; then
                    echo "  ✅ $script_name is executable" >> "$validation_log"
                else
                    echo "  ⚠️ $script_name is not executable" >> "$validation_log"
                    validation_passed=false
                fi
            fi
        done
    else
        echo "  ❌ Rollback scripts directory missing" >> "$validation_log"
        validation_passed=false
    fi
    
    # Validate backup content
    echo "" >> "$validation_log"
    echo "📋 Validating backup content..." >> "$validation_log"
    
    if [ -f "${backup_dir}/complete_backup.sql" ]; then
        local table_count=$(grep -c "CREATE TABLE" "${backup_dir}/complete_backup.sql" || echo "0")
        local function_count=$(grep -c "CREATE.*FUNCTION" "${backup_dir}/complete_backup.sql" || echo "0")
        local view_count=$(grep -c "CREATE VIEW" "${backup_dir}/complete_backup.sql" || echo "0")
        
        echo "  📊 Database objects in backup:" >> "$validation_log"
        echo "     • Tables: $table_count" >> "$validation_log"
        echo "     • Functions: $function_count" >> "$validation_log" 
        echo "     • Views: $view_count" >> "$validation_log"
        
        if [ "$table_count" -gt 0 ]; then
            echo "  ✅ Backup contains database objects" >> "$validation_log"
        else
            echo "  ⚠️ Backup may not contain expected database objects" >> "$validation_log"
            validation_passed=false
        fi
    fi
    
    # Final validation result
    echo "" >> "$validation_log"
    if [ "$validation_passed" = true ]; then
        echo "🎉 VALIDATION PASSED - Rollback procedures are ready" >> "$validation_log"
        log_info "✅ Rollback validation passed"
    else
        echo "❌ VALIDATION FAILED - Issues found with rollback procedures" >> "$validation_log"
        log_warning "⚠️ Rollback validation failed - check $validation_log"
    fi
    
    # Display validation summary
    cat "$validation_log"
    
    return $([ "$validation_passed" = true ] && echo 0 || echo 1)
}

# Cleanup functions
cleanup_old_backups() {
    log_info "Cleaning up old backups (older than $MAX_BACKUP_AGE_DAYS days)..."
    
    if [ -d "$BACKUP_BASE_DIR" ]; then
        local deleted_count=0
        
        find "$BACKUP_BASE_DIR" -maxdepth 1 -type d -name "20*" -mtime +$MAX_BACKUP_AGE_DAYS | while read -r old_backup; do
            if [ -d "$old_backup" ]; then
                local backup_name=$(basename "$old_backup")
                log_info "🗑️ Deleting old backup: $backup_name"
                rm -rf "$old_backup"
                ((deleted_count++))
            fi
        done
        
        # Clean up old rollback scripts
        if [ -d "$ROLLBACK_SCRIPTS_DIR" ]; then
            find "$ROLLBACK_SCRIPTS_DIR" -maxdepth 1 -type d -name "20*" -mtime +$MAX_BACKUP_AGE_DAYS -exec rm -rf {} +
        fi
        
        log_info "✅ Cleanup completed"
    else
        log_info "ℹ️ No backup directory found, skipping cleanup"
    fi
}

# Main backup functions
create_environment_backup() {
    local project_id=$1
    local environment_name=$2
    
    log_info "🛡️ CREATING COMPLETE BACKUP FOR $environment_name"
    log_info "=================================================="
    
    local backup_dir="${BACKUP_BASE_DIR}/${BACKUP_TIMESTAMP}"
    
    # Step 1: Database backups
    log_info "📋 Step 1: Creating database backups..."
    
    backup_database_complete "$project_id" "$environment_name" "${backup_dir}/complete_backup.sql"
    backup_database_schema "$project_id" "$environment_name" "${backup_dir}/schema_backup.sql" 
    backup_database_data "$project_id" "$environment_name" "${backup_dir}/data_backup.sql"
    
    # Step 2: Configuration backup
    log_info "📋 Step 2: Backing up environment configuration..."
    backup_environment_config "$project_id" "$environment_name" "$backup_dir"
    
    # Step 3: Storage inventory
    log_info "📋 Step 3: Creating storage inventory..."
    backup_storage_inventory "$project_id" "$environment_name" "$backup_dir"
    
    # Step 4: Generate rollback scripts
    log_info "📋 Step 4: Generating rollback scripts..."
    generate_database_rollback "${backup_dir}/complete_backup.sql" "${ROLLBACK_SCRIPTS_DIR}/${BACKUP_TIMESTAMP}/database_rollback_${environment_name}.sh" "$environment_name"
    generate_complete_rollback "$backup_dir" "$environment_name"
    
    # Step 5: Validate backup
    log_info "📋 Step 5: Validating backup and rollback procedures..."
    if validate_rollback_procedures "$backup_dir" "$environment_name"; then
        log_info "✅ Backup validation successful"
    else
        log_warning "⚠️ Backup validation found issues"
    fi
    
    # Save backup manifest
    create_backup_manifest "$backup_dir" "$environment_name" "$project_id"
    
    log_info "🎉 BACKUP COMPLETED"
    log_info "=================="
    log_info "📁 Backup location: $backup_dir"
    log_info "🔄 Rollback scripts: ${ROLLBACK_SCRIPTS_DIR}/${BACKUP_TIMESTAMP}/"
    log_info "📊 Backup timestamp: $BACKUP_TIMESTAMP"
}

create_backup_manifest() {
    local backup_dir=$1
    local environment_name=$2
    local project_id=$3
    
    local manifest_file="${backup_dir}/backup_manifest.json"
    
    cat > "$manifest_file" << EOF
{
  "backup_info": {
    "timestamp": "$BACKUP_TIMESTAMP",
    "environment_name": "$environment_name",
    "project_id": "$project_id",
    "backup_type": "complete_pre_migration",
    "created_by": "$(whoami)",
    "hostname": "$(hostname)",
    "cli_version": "$(supabase --version 2>/dev/null || echo 'unknown')"
  },
  "backup_files": {
    "complete_backup": "complete_backup.sql",
    "schema_backup": "schema_backup.sql", 
    "data_backup": "data_backup.sql",
    "environment_config": "config/environment_details.json",
    "storage_inventory": "storage/storage_inventory.json"
  },
  "rollback_scripts": {
    "database_rollback": "../rollback-scripts/$BACKUP_TIMESTAMP/database_rollback_${environment_name}.sh",
    "complete_rollback": "../rollback-scripts/$BACKUP_TIMESTAMP/complete_rollback_${environment_name}.sh"
  },
  "file_sizes": {
    "complete_backup_mb": "$(du -m "${backup_dir}/complete_backup.sql" 2>/dev/null | cut -f1 || echo '0')",
    "schema_backup_kb": "$(du -k "${backup_dir}/schema_backup.sql" 2>/dev/null | cut -f1 || echo '0')",
    "data_backup_mb": "$(du -m "${backup_dir}/data_backup.sql" 2>/dev/null | cut -f1 || echo '0')"
  },
  "retention": {
    "max_age_days": $MAX_BACKUP_AGE_DAYS,
    "auto_cleanup_enabled": true
  }
}
EOF

    log_info "✅ Backup manifest created: $manifest_file"
}

# Usage and help functions
show_usage() {
    cat << EOF
🛡️ Backup & Rollback Generator

USAGE:
    $0 [COMMAND] [OPTIONS]

COMMANDS:
    backup-source              Create complete backup of source environment
    backup-target              Create complete backup of target environment  
    backup-both                Create backups of both source and target
    rollback-target            Execute rollback for target environment
    validate-backup TIMESTAMP  Validate specific backup
    cleanup                    Remove old backups
    list-backups              Show available backups
    help                      Show this help message

EXAMPLES:
    $0 backup-source           # Backup source environment before migration
    $0 backup-target           # Backup target environment before migration
    $0 backup-both             # Backup both environments
    $0 validate-backup 20250117_143022  # Validate specific backup
    $0 rollback-target         # Roll back target to last backup
    $0 cleanup                 # Clean up old backups

ENVIRONMENT:
    Requires .env.migration with SOURCE_PROJECT_ID, TARGET_PROJECT_ID, etc.

FILES:
    backups/TIMESTAMP/         Complete backup data
    rollback-scripts/TIMESTAMP/ Generated rollback scripts
EOF
}

list_available_backups() {
    log_info "📋 Available Backups:"
    log_info "==================="
    
    if [ ! -d "$BACKUP_BASE_DIR" ]; then
        log_info "ℹ️ No backups found"
        return
    fi
    
    for backup_dir in "$BACKUP_BASE_DIR"/20*; do
        if [ -d "$backup_dir" ]; then
            local backup_name=$(basename "$backup_dir")
            local manifest_file="${backup_dir}/backup_manifest.json"
            
            if [ -f "$manifest_file" ]; then
                local env_name=$(grep -o '"environment_name": "[^"]*"' "$manifest_file" | cut -d'"' -f4)
                local size=$(du -sh "$backup_dir" | cut -f1)
                log_info "📦 $backup_name - $env_name ($size)"
            else
                local size=$(du -sh "$backup_dir" | cut -f1)
                log_info "📦 $backup_name - Unknown environment ($size)"
            fi
        fi
    done
}

# Main execution
main() {
    local command=${1:-help}
    
    # Create directories
    create_directories
    
    case $command in
        "backup-source")
            if [ -z "$SOURCE_PROJECT_ID" ]; then
                log_error "❌ SOURCE_PROJECT_ID not set in .env.migration"
                exit 1
            fi
            create_environment_backup "$SOURCE_PROJECT_ID" "source"
            ;;
            
        "backup-target")
            if [ -z "$TARGET_PROJECT_ID" ]; then
                log_error "❌ TARGET_PROJECT_ID not set in .env.migration"
                exit 1
            fi
            create_environment_backup "$TARGET_PROJECT_ID" "target"
            ;;
            
        "backup-both")
            if [ -z "$SOURCE_PROJECT_ID" ] || [ -z "$TARGET_PROJECT_ID" ]; then
                log_error "❌ SOURCE_PROJECT_ID and TARGET_PROJECT_ID must be set in .env.migration"
                exit 1
            fi
            create_environment_backup "$SOURCE_PROJECT_ID" "source"
            create_environment_backup "$TARGET_PROJECT_ID" "target"
            ;;
            
        "validate-backup")
            local backup_timestamp=$2
            if [ -z "$backup_timestamp" ]; then
                log_error "❌ Please specify backup timestamp"
                exit 1
            fi
            
            local backup_dir="${BACKUP_BASE_DIR}/${backup_timestamp}"
            if [ ! -d "$backup_dir" ]; then
                log_error "❌ Backup not found: $backup_dir"
                exit 1
            fi
            
            validate_rollback_procedures "$backup_dir" "unknown"
            ;;
            
        "rollback-target")
            log_info "🔍 Finding latest target backup..."
            local latest_backup=$(ls -1 "$BACKUP_BASE_DIR" | grep "^20" | sort -r | head -n1)
            
            if [ -z "$latest_backup" ]; then
                log_error "❌ No backups found"
                exit 1
            fi
            
            local rollback_script="${ROLLBACK_SCRIPTS_DIR}/${latest_backup}/complete_rollback_target.sh"
            if [ -x "$rollback_script" ]; then
                log_info "🔄 Executing rollback script: $rollback_script"
                bash "$rollback_script"
            else
                log_error "❌ Rollback script not found or not executable: $rollback_script"
                exit 1
            fi
            ;;
            
        "cleanup")
            cleanup_old_backups
            ;;
            
        "list-backups")
            list_available_backups
            ;;
            
        "help"|*)
            show_usage
            ;;
    esac
}

# Execute main function
main "$@"
