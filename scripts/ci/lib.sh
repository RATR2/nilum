#!/usr/bin/env bash
# Shared helpers for the build-and-publish CI scripts.

read_version() {
  grep -oE 'version = "[^"]*"' build.gradle.kts | head -1 | cut -d'"' -f2
}

build_and_collect_jars() {
  local out_dir="$1"
  local version sha jar module

  version="$(read_version)"
  sha="${COMMIT_SHA}"

  ./gradlew build mergeClientJars --console=plain

  while IFS= read -r jar; do
    module="$(basename "$(dirname "$(dirname "$(dirname "${jar}")")")")"
    mkdir -p "${out_dir}/${module}"
    cp "${jar}" "${out_dir}/${module}/nilum-${version}-${sha}.jar"
  done < <(find . -path '*/build/libs/*.jar' ! -name '*-slim.jar' | grep -v '^\./build/libs/')

  if [ -f "build/libs/nilum-client-${version}.jar" ]; then
    mkdir -p "${out_dir}/nilum-client"
    cp "build/libs/nilum-client-${version}.jar" "${out_dir}/nilum-client/nilum-${version}-${sha}.jar"
  fi
}

clone_nilum_builds() {
  local dest="$1"
  git clone "https://x-access-token:${NILUM_BUILDS_TOKEN}@github.com/RATR2/nilum-builds.git" "${dest}"
  git -C "${dest}" config user.name "github-actions[bot]"
  git -C "${dest}" config user.email "41898282+github-actions[bot]@users.noreply.github.com"
}

checkout_matching_branch() {
  local dir="$1" branch="$2"
  if git -C "${dir}" ls-remote --exit-code --heads origin "${branch}" >/dev/null 2>&1; then
    git -C "${dir}" checkout "${branch}"
  else
    git -C "${dir}" checkout -b "${branch}"
  fi
}
