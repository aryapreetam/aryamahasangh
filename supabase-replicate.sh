#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
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

# Production project details
PROD_PROJECT_ID="ftnwwiwmljcwzpsawdmf"
PROD_HOST="db.${PROD_PROJECT_ID}.supabase.co"
PROD_PORT="5432"
PROD_DB="postgres"
PROD_USER="postgres"

# Development project details
DEV_PROJECT_ID="afjtpdeohgdgkrwayayn"
DEV_HOST="db.${DEV_PROJECT_ID}.supabase.co"
DEV_PORT="5432"
DEV_DB="postgres"
DEV_USER="postgres"

# Function to get password securely
get_password() {
    local prompt=$1
    read -sp "$prompt: " password
    echo "$password"
}

print_message "This script will replicate your production Supabase environment to development." "$YELLOW"
print_message "WARNING: This will overwrite your development database with production data." "$RED"
read -p "Do you want to continue? (y/N) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]
then
    print_message "Operation cancelled." "$YELLOW"
    exit 1
fi

# Get passwords
print_message "Please enter your Supabase database passwords:" "$YELLOW"
PROD_PASSWORD=$(get_password "Production Database Password")
echo
DEV_PASSWORD=$(get_password "Development Database Password")
echo

# Create a temporary directory for the dump
TEMP_DIR=$(mktemp -d)
DUMP_FILE="${TEMP_DIR}/prod_dump.dump"

print_message "Testing production database connection..." "$YELLOW"
if ! PGPASSWORD="$PROD_PASSWORD" psql -h "$PROD_HOST" -U "$PROD_USER" -d "$PROD_DB" -p "$PROD_PORT" -c "\l" > /dev/null 2>&1; then
    print_message "Error: Could not connect to production database. Please check:" "$RED"
    print_message "1. Your production database password is correct" "$RED"
    print_message "2. Your IP is allowed in Supabase dashboard (Database → Network Restrictions)" "$RED"
    print_message "3. The database is running and accessible" "$RED"
    rm -rf "$TEMP_DIR"
    exit 1
fi

print_message "Production database connection successful." "$GREEN"

print_message "Exporting production database..." "$YELLOW"
print_message "This might take a few minutes depending on your database size..." "$YELLOW"

# Run pg_dump with verbose output
PGPASSWORD="$PROD_PASSWORD" pg_dump \
    -h "$PROD_HOST" \
    -U "$PROD_USER" \
    -d "$PROD_DB" \
    -p "$PROD_PORT" \
    -Fc \
    -v \
    -f "$DUMP_FILE" 2>&1 | while read -r line; do
        print_message "$line" "$YELLOW"
    done

if [ $? -ne 0 ]; then
    print_message "Error: Failed to export production database." "$RED"
    print_message "Please check:" "$RED"
    print_message "1. Your production database password is correct" "$RED"
    print_message "2. You have sufficient permissions to export the database" "$RED"
    print_message "3. There's enough disk space in the temporary directory" "$RED"
    rm -rf "$TEMP_DIR"
    exit 1
fi

print_message "Production database exported successfully." "$GREEN"

print_message "Testing development database connection..." "$YELLOW"
if ! PGPASSWORD="$DEV_PASSWORD" psql -h "$DEV_HOST" -U "$DEV_USER" -d "$DEV_DB" -p "$DEV_PORT" -c "\l" > /dev/null 2>&1; then
    print_message "Error: Could not connect to development database. Please check:" "$RED"
    print_message "1. Your development database password is correct" "$RED"
    print_message "2. Your IP is allowed in Supabase dashboard (Database → Network Restrictions)" "$RED"
    print_message "3. The database is running and accessible" "$RED"
    rm -rf "$TEMP_DIR"
    exit 1
fi

print_message "Development database connection successful." "$GREEN"

print_message "Importing to development database..." "$YELLOW"
print_message "This might take a few minutes depending on your database size..." "$YELLOW"

# Run pg_restore with verbose output
PGPASSWORD="$DEV_PASSWORD" pg_restore \
    -h "$DEV_HOST" \
    -U "$DEV_USER" \
    -d "$DEV_DB" \
    -p "$DEV_PORT" \
    --clean \
    --if-exists \
    -v \
    "$DUMP_FILE" 2>&1 | while read -r line; do
        print_message "$line" "$YELLOW"
    done

if [ $? -ne 0 ]; then
    print_message "Error: Failed to import to development database." "$RED"
    print_message "Please check:" "$RED"
    print_message "1. Your development database password is correct" "$RED"
    print_message "2. You have sufficient permissions to import the database" "$RED"
    print_message "3. There's enough disk space in the development database" "$RED"
    rm -rf "$TEMP_DIR"
    exit 1
fi

# Cleanup
rm -rf "$TEMP_DIR"

print_message "Successfully replicated production environment to development!" "$GREEN"
print_message "Your development database at ${DEV_PROJECT_ID} now matches production." "$GREEN" 