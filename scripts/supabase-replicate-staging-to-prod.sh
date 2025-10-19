#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored messages
print_message() {
    echo -e "${2}${1}${NC}"
}

# Function to check if command exists
check_command() {
    if ! command -v $1 &> /dev/null; then
        print_message "Error: $1 is not installed. Please install it first." "$RED"
        exit 1
    fi
}

# Check if required commands are installed
check_command pg_dump
check_command pg_restore

# Staging project details (SOURCE)
STAGING_PROJECT_ID="ftnwwiwmljcwzpsawdmf"
STAGING_HOST="db.${STAGING_PROJECT_ID}.supabase.co"
STAGING_PORT="5432"
STAGING_DB="postgres"
STAGING_USER="postgres"

# Production project details (TARGET)
PROD_PROJECT_ID="jusbsyslwvrdmdwdsvfk"
PROD_HOST="db.${PROD_PROJECT_ID}.supabase.co"
PROD_PORT="5432"
PROD_DB="postgres"
PROD_USER="postgres"

# Function to get password securely
get_password() {
    local prompt=$1
    read -sp "$prompt: " password
    echo "$password"
}

print_message "🚀 SUPABASE STAGING TO PRODUCTION MIGRATION" "$BLUE"
print_message "=============================================" "$BLUE"
print_message "Source:  Staging Environment (${STAGING_PROJECT_ID})" "$YELLOW"
print_message "Target:  Production Environment (${PROD_PROJECT_ID})" "$YELLOW"
print_message "Process: Complete database replication (schema + data + infrastructure)" "$YELLOW"
print_message "" "$NC"

print_message "⚠️  WARNING: This will overwrite your production database with staging data." "$RED"
print_message "⚠️  This includes all tables, views, functions, RLS policies, and data." "$RED"
print_message "⚠️  Auth users table will be EXCLUDED to preserve production authentication." "$YELLOW"
print_message "" "$NC"

read -p "Do you want to continue? (y/N) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]
then
    print_message "Operation cancelled." "$YELLOW"
    exit 1
fi

# Get passwords
print_message "Please enter your Supabase database passwords:" "$YELLOW"
STAGING_PASSWORD=$(get_password "Staging Database Password")
echo
PROD_PASSWORD=$(get_password "Production Database Password")
echo

# Create a temporary directory for the dump
TEMP_DIR=$(mktemp -d)
DUMP_FILE="${TEMP_DIR}/staging_dump.dump"

print_message "📡 Testing staging database connection..." "$YELLOW"
if ! PGPASSWORD="$STAGING_PASSWORD" psql -h "$STAGING_HOST" -U "$STAGING_USER" -d "$STAGING_DB" -p "$STAGING_PORT" -c "SELECT current_database();" > /dev/null 2>&1; then
    print_message "❌ Error: Could not connect to staging database. Please check:" "$RED"
    print_message "   1. Your staging database password is correct" "$RED"
    print_message "   2. Your IP is allowed in Supabase dashboard (Database → Network Restrictions)" "$RED"
    print_message "   3. The database is running and accessible" "$RED"
    rm -rf "$TEMP_DIR"
    exit 1
fi

print_message "✅ Staging database connection successful." "$GREEN"

print_message "📡 Testing production database connection..." "$YELLOW"
if ! PGPASSWORD="$PROD_PASSWORD" psql -h "$PROD_HOST" -U "$PROD_USER" -d "$PROD_DB" -p "$PROD_PORT" -c "SELECT current_database();" > /dev/null 2>&1; then
    print_message "❌ Error: Could not connect to production database. Please check:" "$RED"
    print_message "   1. Your production database password is correct" "$RED"
    print_message "   2. Your IP is allowed in Supabase dashboard (Database → Network Restrictions)" "$RED"
    print_message "   3. The database is running and accessible" "$RED"
    rm -rf "$TEMP_DIR"
    exit 1
fi

print_message "✅ Production database connection successful." "$GREEN"

print_message "📦 Exporting staging database..." "$YELLOW"
print_message "   📋 Including: Schema, data, views, functions, RLS policies, triggers" "$YELLOW"
print_message "   🚫 Excluding: auth.users table (preserving production authentication)" "$YELLOW"
print_message "   ⏳ This might take a few minutes depending on your database size..." "$YELLOW"

# Run pg_dump with verbose output, excluding auth.users table
PGPASSWORD="$STAGING_PASSWORD" pg_dump \
    -h "$STAGING_HOST" \
    -U "$STAGING_USER" \
    -d "$STAGING_DB" \
    -p "$STAGING_PORT" \
    -Fc \
    -v \
    --exclude-table=auth.users \
    --exclude-table=auth.audit_log_entries \
    --exclude-table=auth.refresh_tokens \
    --exclude-table=auth.sessions \
    --exclude-table=auth.flow_state \
    --exclude-table=auth.identities \
    --exclude-table=auth.instances \
    --exclude-table=auth.mfa_amr_claims \
    --exclude-table=auth.mfa_challenges \
    --exclude-table=auth.mfa_factors \
    --exclude-table=auth.one_time_tokens \
    --exclude-table=auth.saml_providers \
    --exclude-table=auth.saml_relay_states \
    --exclude-table=auth.schema_migrations \
    --exclude-table=auth.sso_domains \
    --exclude-table=auth.sso_providers \
    -f "$DUMP_FILE" 2>&1 | while read -r line; do
        print_message "   $line" "$BLUE"
    done

if [ $? -ne 0 ]; then
    print_message "❌ Error: Failed to export staging database." "$RED"
    print_message "Please check:" "$RED"
    print_message "   1. Your staging database password is correct" "$RED"
    print_message "   2. You have sufficient permissions to export the database" "$RED"
    print_message "   3. There's enough disk space in the temporary directory" "$RED"
    rm -rf "$TEMP_DIR"
    exit 1
fi

print_message "✅ Staging database exported successfully." "$GREEN"

print_message "📥 Importing to production database..." "$YELLOW"
print_message "   ⏳ This might take a few minutes depending on your database size..." "$YELLOW"

# Run pg_restore with verbose output
PGPASSWORD="$PROD_PASSWORD" pg_restore \
    -h "$PROD_HOST" \
    -U "$PROD_USER" \
    -d "$PROD_DB" \
    -p "$PROD_PORT" \
    --clean \
    --if-exists \
    -v \
    "$DUMP_FILE" 2>&1 | while read -r line; do
        print_message "   $line" "$BLUE"
    done

if [ $? -ne 0 ]; then
    print_message "❌ Error: Failed to import to production database." "$RED"
    print_message "Please check:" "$RED"
    print_message "   1. Your production database password is correct" "$RED"
    print_message "   2. You have sufficient permissions to import the database" "$RED"
    print_message "   3. There's enough disk space in the production database" "$RED"
    rm -rf "$TEMP_DIR"
    exit 1
fi

# Cleanup
rm -rf "$TEMP_DIR"

print_message "🎉 DATABASE MIGRATION COMPLETED SUCCESSFULLY!" "$GREEN"
print_message "✅ Your production database (${PROD_PROJECT_ID}) now contains:" "$GREEN"
print_message "   • Complete schema from staging (tables, views, functions)" "$GREEN"
print_message "   • All data from staging environment" "$GREEN"
print_message "   • RLS policies and security configuration" "$GREEN"
print_message "   • GraphQL configuration and realtime settings" "$GREEN"
print_message "   • Auth tables preserved (staging auth users excluded)" "$GREEN"
print_message "" "$NC"

print_message "📋 NEXT STEPS:" "$YELLOW"
print_message "   1. Run storage file migration: ./migrate-storage-staging-to-prod.sh" "$YELLOW"
print_message "   2. Update file URLs: ./update-urls-staging-to-prod.sh" "$YELLOW"  
print_message "   3. Test application functionality" "$YELLOW"
print_message "   4. Update local.properties to use production environment" "$YELLOW"
print_message "" "$NC"
