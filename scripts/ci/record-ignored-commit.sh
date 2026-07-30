#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."
source scripts/ci/lib.sh

CHECKOUT_DIR="$(mktemp -d)"
clone_nilum_builds "${CHECKOUT_DIR}"
checkout_matching_branch "${CHECKOUT_DIR}" "${BRANCH_NAME}"

update_readme "${CHECKOUT_DIR}" "${COMMIT_SHA}" "$(echo "${COMMIT_MESSAGE}" | head -1)" "false"

cd "${CHECKOUT_DIR}"
git add README.md
if ! git diff --cached --quiet; then
  git commit -m "chore: record ignored commit ${COMMIT_SHA} [ci-skip-build]"
  git push -u origin "${BRANCH_NAME}"
fi
