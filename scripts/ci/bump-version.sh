#!/usr/bin/env bash
set -euo pipefail

NEW_VERSION="$1"

sed -i "s/version = \"[^\"]*\"/version = \"${NEW_VERSION}\"/" build.gradle.kts

git config user.name "github-actions[bot]"
git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
git add build.gradle.kts
# --allow-empty: if the version is already NEW_VERSION (e.g. a retry after this
# same bump already landed), there's nothing to stage and a plain `git commit`
# would fail outright under `set -e`, aborting the step before it can push.
git commit --allow-empty -m "chore: bump version to ${NEW_VERSION} [ci-bump]"

# Pushing with the default GITHUB_TOKEN would succeed but never trigger a new
# workflow run (GitHub suppresses that specifically to prevent trigger loops).
# A real PAT doesn't have that restriction, so the bot commit's own push here
# fires the workflow again - that second run is what actually builds and
# publishes to nilum-builds (see build.yml's "is_bot == true" step).
REMOTE="https://x-access-token:${NILUM_BUILDS_TOKEN}@github.com/RATR2/nilum.git"
git push "${REMOTE}" HEAD:main

# Source-code versioning, separate from nilum-builds' own release/<version>
# branch of built artifacts: release/<version> here marks the exact nilum
# source commit that version was cut from.
git push "${REMOTE}" "HEAD:refs/heads/release/${NEW_VERSION}"
