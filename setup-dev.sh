#!/bin/bash

# Development Setup Script
# This script helps set up the development environment

echo "🚀 Setting up Arya Mahasangh development environment..."

# Check if secrets.properties exists
if [ ! -f "secrets.properties" ]; then
    echo "📋 Creating secrets.properties from template..."
    cp secrets.properties.template secrets.properties
    echo "✅ secrets.properties created!"
    echo ""
    echo "⚠️  IMPORTANT: Please edit secrets.properties and add your actual configuration values:"
    echo "   - Supabase URL and API key"
    echo "   - Server URLs for dev and prod"
    echo ""
    echo "📖 You can get these values from:"
    echo "   - Supabase Dashboard: https://app.supabase.com/"
    echo "   - Your server deployment"
    echo ""
else
    echo "✅ secrets.properties already exists"
fi

# Check if secrets.properties has been configured
if grep -q "your-dev-project.supabase.co" secrets.properties 2>/dev/null; then
    echo "⚠️  WARNING: secrets.properties still contains template values!"
    echo "   Please update it with your actual configuration."
    echo ""
fi

echo "🔧 Development setup complete!"
echo ""
echo "📝 Next steps:"
echo "   1. Edit secrets.properties with your actual values"
echo "   2. Run: ./gradlew build"
echo "   3. Start developing!"
echo ""
echo "🔒 Security reminder:"
echo "   - secrets.properties is gitignored and will never be committed"
echo "   - Use environment variables in production/CI"
echo "   - Never commit actual secrets to version control"