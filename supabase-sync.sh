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

# Check if Supabase CLI is installed
check_command supabase

# Function to cleanup migrations
cleanup_migrations() {
    print_message "Cleaning up migrations..." "$YELLOW"
    rm -f supabase/migrations/*.sql
    print_message "Migrations cleaned up." "$GREEN"
}

# Function to sync from production to development
sync_prod_to_dev() {
    print_message "Syncing from production to development..." "$YELLOW"
    
    # Cleanup existing migrations
    cleanup_migrations
    
    # Link production project
    print_message "Linking production project..." "$YELLOW"
    supabase link --project-ref ftnwwiwmljcwzpsawdmf
    
    # Pull schema from production
    print_message "Pulling schema from production..." "$YELLOW"
    supabase db pull
    
    # Create migration with timestamp
    TIMESTAMP=$(date +%Y%m%d%H%M%S)
    print_message "Creating migration..." "$YELLOW"
    supabase migration new "prod_to_dev_${TIMESTAMP}"
    
    # Link development project
    print_message "Linking development project..." "$YELLOW"
    supabase link --project-ref afjtpdeohgdgkrwayayn
    
    # Reset the database in development
    print_message "Resetting development database..." "$YELLOW"
    supabase db reset
    
    # Push schema to development
    print_message "Pushing schema to development..." "$YELLOW"
    supabase db push
    
    print_message "Sync completed successfully!" "$GREEN"
}

# Function to sync from development to production
sync_dev_to_prod() {
    print_message "Syncing from development to production..." "$YELLOW"
    
    # Cleanup existing migrations
    cleanup_migrations
    
    # Link development project
    print_message "Linking development project..." "$YELLOW"
    supabase link --project-ref afjtpdeohgdgkrwayayn
    
    # Pull schema from development
    print_message "Pulling schema from development..." "$YELLOW"
    supabase db pull
    
    # Create migration with timestamp
    TIMESTAMP=$(date +%Y%m%d%H%M%S)
    print_message "Creating migration..." "$YELLOW"
    supabase migration new "dev_to_prod_${TIMESTAMP}"
    
    # Link production project
    print_message "Linking production project..." "$YELLOW"
    supabase link --project-ref ftnwwiwmljcwzpsawdmf
    
    # Push schema to production
    print_message "Pushing schema to production..." "$YELLOW"
    supabase db push
    
    print_message "Sync completed successfully!" "$GREEN"
}

# Main script
case "$1" in
    "prod-to-dev")
        sync_prod_to_dev
        ;;
    "dev-to-prod")
        sync_dev_to_prod
        ;;
    "cleanup")
        cleanup_migrations
        ;;
    *)
        print_message "Usage: $0 {prod-to-dev|dev-to-prod|cleanup}" "$RED"
        print_message "  prod-to-dev: Sync schema from production to development" "$YELLOW"
        print_message "  dev-to-prod: Sync schema from development to production" "$YELLOW"
        print_message "  cleanup: Clean up existing migrations" "$YELLOW"
        exit 1
        ;;
esac 