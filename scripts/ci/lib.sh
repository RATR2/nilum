#!/usr/bin/env bash
# Shared helpers for the build-and-publish CI scripts.

read_version() {
  grep -oE 'version = "[^"]*"' build.gradle.kts | head -1 | cut -d'"' -f2
}

# nilum-fabric/libs/*.jar and nilum-neoforge/libs/*.jar are gitignored (see
# nilum-fabric/libs/attach.md), so CI has to fetch them itself before Gradle configures those
# modules. Bumping a vendored version means updating its filename in the module's
# build.gradle.kts and the URL/hash below together.
FABRIC_IRIS_JAR_DIR="nilum-fabric/libs"
FABRIC_IRIS_JAR_NAME="iris-fabric-1.10.7+mc1.21.11.jar"
FABRIC_IRIS_JAR_URL="https://cdn.modrinth.com/data/YL57xq9U/versions/fDpuVzVr/iris-fabric-1.10.7%2Bmc1.21.11.jar"
FABRIC_IRIS_JAR_SHA1="aae8567bd9ea397d50aff1d0b680a82ffe67040c"

NEOFORGE_IRIS_JAR_DIR="nilum-neoforge/libs"
NEOFORGE_IRIS_JAR_NAME="iris-neoforge-1.10.7+mc1.21.11.jar"
NEOFORGE_IRIS_JAR_URL="https://cdn.modrinth.com/data/YL57xq9U/versions/v6TgIIUM/iris-neoforge-1.10.7%2Bmc1.21.11.jar"
NEOFORGE_IRIS_JAR_SHA1="3483a33e4b4895473a23bd9f4b0268d0807c19ef"

fetch_vendored_jar() {
  local dir="$1" name="$2" url="$3" expected_sha1="$4"
  local dest="${dir}/${name}"
  if [ -f "${dest}" ]; then
    return
  fi

  mkdir -p "${dir}"
  curl -fsSL -o "${dest}" "${url}"

  local actual_sha1
  actual_sha1="$(sha1sum "${dest}" | cut -d' ' -f1)"
  if [ "${actual_sha1}" != "${expected_sha1}" ]; then
    echo "${name} sha1 mismatch: expected ${expected_sha1}, got ${actual_sha1}" >&2
    rm -f "${dest}"
    exit 1
  fi
}

fetch_vendored_libs() {
  fetch_vendored_jar "${FABRIC_IRIS_JAR_DIR}" "${FABRIC_IRIS_JAR_NAME}" "${FABRIC_IRIS_JAR_URL}" "${FABRIC_IRIS_JAR_SHA1}"
  fetch_vendored_jar "${NEOFORGE_IRIS_JAR_DIR}" "${NEOFORGE_IRIS_JAR_NAME}" "${NEOFORGE_IRIS_JAR_URL}" "${NEOFORGE_IRIS_JAR_SHA1}"
}

build_and_collect_jars() {
  local out_dir="$1"
  local version sha jar module suffix

  version="$(read_version)"
  sha="${COMMIT_SHA}"

  fetch_vendored_libs
  ./gradlew build mergeClientJars --console=plain

  while IFS= read -r jar; do
    module="$(basename "$(dirname "$(dirname "$(dirname "${jar}")")")")"
    suffix="${module#nilum-}"
    mkdir -p "${out_dir}/${module}"
    cp "${jar}" "${out_dir}/${module}/nilum-${version}-${sha}-${suffix^^}.jar"
  done < <(find . -path '*/build/libs/*.jar' ! -name '*-slim.jar' | grep -v '^\./build/libs/')

  if [ -f "build/libs/nilum-client-${version}.jar" ]; then
    mkdir -p "${out_dir}/nilum-client"
    cp "build/libs/nilum-client-${version}.jar" "${out_dir}/nilum-client/nilum-${version}-${sha}.jar"
  fi
}

# Rewrites README.md with the given commit as the last build. Every caller now
# corresponds to a real build (see build.yml's "workflow"/"build" tag gating),
# so there's no more distinct "last commit that wasn't built" to track.
update_readme() {
  local dir="$1" sha="$2" subject="$3"
  mkdir -p "${dir}/builds"
  printf '%s\n%s\n' "${sha}" "${subject}" > "${dir}/builds/.last-build"

  cat > "${dir}/README.md" <<EOF
---
<div align="center">

  # Nilum-Builds

Builds for https://github.com/RATR2/nilum

  Last Build: [\`${sha}\`](https://github.com/RATR2/nilum/commit/${sha}) ${subject}
</div>
EOF
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
