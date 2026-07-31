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
# A real PAT doesn't have that restriction - but actions/checkout leaves a
# persistent `http.https://github.com/.extraheader` Authorization header (its
# own GITHUB_TOKEN) in the local git config for the whole job, and that header
# applies to ANY https://github.com/... URL regardless of embedded userinfo,
# silently overriding the PAT in the URL below. `-c http...extraheader=`
# clears it for just this invocation so the PAT is what actually authenticates.
REMOTE="https://x-access-token:${NILUM_BUILDS_TOKEN}@github.com/RATR2/nilum.git"
git -c http.https://github.com/.extraheader= push "${REMOTE}" HEAD:main

# Source-code versioning, separate from nilum-builds' own release/<version>
# branch of built artifacts: release/<version> here marks the exact nilum
# source commit that version was cut from.
git -c http.https://github.com/.extraheader= push "${REMOTE}" "HEAD:refs/heads/release/${NEW_VERSION}"
