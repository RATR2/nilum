#!/usr/bin/env bash
# Shared helpers for the build-and-publish CI scripts.

read_version() {
  grep -oE 'version = "[^"]*"' build.gradle.kts | head -1 | cut -d'"' -f2
}

# Builds the Gradle project and copies every module's jar into
# $1/<module>/nilum-<version>-<sha>.jar. Each module gets its own
# subfolder so the (deliberately module-agnostic) jar filename can stay
# exactly "nilum-<version>-<sha>.jar" without modules overwriting each other.
build_and_collect_jars() {
  local out_dir="$1"
  local version sha jar module

  version="$(read_version)"
  sha="${COMMIT_SHA}"

  ./gradlew build --console=plain

  while IFS= read -r jar; do
    module="$(basename "$(dirname "$(dirname "$(dirname "${jar}")")")")"
    mkdir -p "${out_dir}/${module}"
    cp "${jar}" "${out_dir}/${module}/nilum-${version}-${sha}.jar"
  done < <(find . -path '*/build/libs/*.jar')
}

clone_nilum_builds() {
  local dest="$1"
  git clone "https://x-access-token:${NILUM_BUILDS_TOKEN}@github.com/RATR2/nilum-builds.git" "${dest}"
  git -C "${dest}" config user.name "github-actions[bot]"
  git -C "${dest}" config user.email "41898282+github-actions[bot]@users.noreply.github.com"
}
