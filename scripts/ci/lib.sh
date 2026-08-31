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

# Places each build's jars under the layout a downloader actually wants (what to run, grouped
# by role), not one folder per Gradle module. nilum-api is deliberately not collected here; it
# publishes as a GitHub Release asset instead (see publish_api_release), since it's a compile-time
# library for third-party developers, not something a server owner or player downloads from here.
build_and_collect_jars() {
  local out_dir="$1"
  local version sha

  version="$(read_version)"
  sha="${COMMIT_SHA}"

  fetch_vendored_libs
  ./gradlew build mergeUniversalJar mergeMultiloaderJar --console=plain

  mkdir -p \
    "${out_dir}/nilum-server/nilum-server-fabric" \
    "${out_dir}/nilum-server/nilum-server-neoforge" \
    "${out_dir}/nilum-server/nilum-server-paper" \
    "${out_dir}/nilum-universal" \
    "${out_dir}/nilum-client/nilum-multiloader" \
    "${out_dir}/nilum-source/nilum-common" \
    "${out_dir}/nilum-source/nilum-common-server" \
    "${out_dir}/nilum-source/nilum-common-client"

  cp "nilum-fabric/build/libs/nilum-fabric-${version}.jar" \
    "${out_dir}/nilum-server/nilum-server-fabric/nilum-${version}-${sha}-SERVER-FABRIC.jar"
  cp "nilum-neoforge/build/libs/nilum-neoforge-${version}.jar" \
    "${out_dir}/nilum-server/nilum-server-neoforge/nilum-${version}-${sha}-SERVER-NEOFORGE.jar"
  cp "nilum-paper/build/libs/nilum-paper-${version}.jar" \
    "${out_dir}/nilum-server/nilum-server-paper/nilum-${version}-${sha}-SERVER-PAPER.jar"
  cp "build/libs/nilum-universal-${version}.jar" \
    "${out_dir}/nilum-universal/nilum-${version}-${sha}-UNIVERSAL.jar"
  cp "build/libs/nilum-multiloader-${version}.jar" \
    "${out_dir}/nilum-client/nilum-multiloader/nilum-${version}-${sha}-MULTILOADER.jar"
  cp "nilum-common/build/libs/nilum-common-${version}.jar" \
    "${out_dir}/nilum-source/nilum-common/nilum-${version}-${sha}-COMMON.jar"
  cp "nilum-common-server/build/libs/nilum-common-server-${version}.jar" \
    "${out_dir}/nilum-source/nilum-common-server/nilum-${version}-${sha}-COMMON-SERVER.jar"
  cp "nilum-common-client/build/libs/nilum-common-client-${version}.jar" \
    "${out_dir}/nilum-source/nilum-common-client/nilum-${version}-${sha}-COMMON-CLIENT.jar"
}

# Publishes nilum-api as a GitHub Release on the source repo (not nilum-builds), since it's a
# public compile-time dependency for third-party developers rather than a runnable artifact.
# gh release create auto-creates the "v<version>" tag at target if it doesn't already exist.
publish_api_release() {
  local version="$1" sha="$2"
  local jar="nilum-api/build/libs/nilum-api-${version}.jar"

  gh release create "v${version}" "${jar}" \
    --repo RATR2/nilum \
    --target "${sha}" \
    --title "nilum-api ${version}" \
    --notes "Public API jar for third-party plugins and Skript/Denizen-style addons. See nilum-api/src/main/java/io/github/r4t2/nilum/api/NilumAPI.java."
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
