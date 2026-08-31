#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."
NILUM_ROOT="$(pwd)"
source scripts/ci/lib.sh

OUT_DIR="$(mktemp -d)"
build_and_collect_jars "${OUT_DIR}"

VERSION="$(read_version)"

CHECKOUT_DIR="$(mktemp -d)"
clone_nilum_builds "${CHECKOUT_DIR}"

checkout_matching_branch "${CHECKOUT_DIR}" "release/${VERSION}"

cd "${CHECKOUT_DIR}"
mkdir -p "builds/${COMMIT_SHA}"
cp -r "${OUT_DIR}"/. "builds/${COMMIT_SHA}/"
update_readme "${CHECKOUT_DIR}" "${COMMIT_SHA}" "release: nilum ${VERSION} (${COMMIT_SHA})"
git add builds README.md
git commit -m "release: nilum ${VERSION} (${COMMIT_SHA})"
git push -u origin "release/${VERSION}"

cd "${NILUM_ROOT}"
publish_api_release "${VERSION}" "${COMMIT_SHA}"
