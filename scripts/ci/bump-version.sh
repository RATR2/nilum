#!/usr/bin/env bash
set -euo pipefail

NEW_VERSION="$1"

sed -i "s/version = \"[^\"]*\"/version = \"${NEW_VERSION}\"/" build.gradle.kts

git config user.name "github-actions[bot]"
git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
git add build.gradle.kts
git commit -m "chore: bump version to ${NEW_VERSION} [ci-bump]"
git push
