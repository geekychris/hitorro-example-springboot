#!/bin/bash

# Hitorro React Test App - Quick Start Script

echo "🚀 Starting Hitorro React Test App..."
echo ""

# Check if node_modules exists
if [ ! -d "node_modules" ]; then
    echo "📦 Installing dependencies..."
    npm install
    echo ""
fi

echo "✅ Starting development server..."
echo "   Frontend: http://localhost:3000"
echo "   Backend:  http://localhost:8080 (must be running)"
echo ""
echo "Press Ctrl+C to stop"
echo ""

npm run dev
