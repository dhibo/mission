#!/bin/bash

# OD ======> Script to increment version in pom.xml
echo "OD ======> Starting version increment process..."

# Get current version from pom.xml
CURRENT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
echo "OD ======> Current version: $CURRENT_VERSION"

# Extract version parts (assuming format X.Y.Z-SNAPSHOT)
if [[ $CURRENT_VERSION =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)(-SNAPSHOT)?$ ]]; then
    MAJOR=${BASH_REMATCH[1]}
    MINOR=${BASH_REMATCH[2]}
    PATCH=${BASH_REMATCH[3]}
    SNAPSHOT=${BASH_REMATCH[4]}
    
    # Increment patch version
    NEW_PATCH=$((PATCH + 1))
    
    # Construct new version
    if [[ -n "$SNAPSHOT" ]]; then
        NEW_VERSION="$MAJOR.$MINOR.$NEW_PATCH-SNAPSHOT"
    else
        NEW_VERSION="$MAJOR.$MINOR.$NEW_PATCH"
    fi
    
    echo "OD ======> New version: $NEW_VERSION"
    
    # Update pom.xml with new version
    mvn versions:set -DnewVersion=$NEW_VERSION -DgenerateBackupPoms=false
    
    if [ $? -eq 0 ]; then
        echo "OD ======> Version successfully updated to $NEW_VERSION"
        
        # Commit the version change
        git add pom.xml
        git commit -m "Auto-increment version to $NEW_VERSION"
        
        echo "OD ======> Version change committed"
    else
        echo "OD ======> Failed to update version"
        exit 1
    fi
else
    echo "OD ======> Invalid version format: $CURRENT_VERSION"
    exit 1
fi

echo "OD ======> Version increment completed successfully" 