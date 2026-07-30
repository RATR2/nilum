#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."
source scripts/ci/lib.sh

OUT_DIR="$(mktemp -d)"
build_and_collect_jars "${OUT_DIR}"

CHECKOUT_DIR="$(mktemp -d)"
clone_nilum_builds "${CHECKOUT_DIR}"
checkout_matching_branch "${CHECKOUT_DIR}" "${BRANCH_NAME}"

mkdir -p "${CHECKOUT_DIR}/builds/${COMMIT_SHA}"
cp -r "${OUT_DIR}"/. "${CHECKOUT_DIR}/builds/${COMMIT_SHA}/"

cd "${CHECKOUT_DIR}"
git add builds
git commit -m "${COMMIT_MESSAGE}"
git push -u origin "${BRANCH_NAME}"
