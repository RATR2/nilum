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

# Rewrites README.md with the last actually-built commit and the given commit.
# $4 (is_build) records $2/$3 as the new "last build" first when "true" - when
# "false" (an ignored commit), the existing last-build record is left alone, so
# "Last Build" and "Last Commit" can legitimately point at different commits.
update_readme() {
  local dir="$1" sha="$2" subject="$3" is_build="$4"
  local state_file="${dir}/builds/.last-build"

  if [ "${is_build}" = "true" ]; then
    mkdir -p "${dir}/builds"
    printf '%s\n%s\n' "${sha}" "${subject}" > "${state_file}"
  fi

  local build_sha="" build_subject=""
  if [ -f "${state_file}" ]; then
    build_sha="$(sed -n '1p' "${state_file}")"
    build_subject="$(sed -n '2p' "${state_file}")"
  fi

  cat > "${dir}/README.md" <<EOF
---
<div align="center">

  # Nilum-Builds

Builds for https://github.com/RATR2/nilum

  Last Build: [${build_subject}](https://github.com/RATR2/nilum/commit/${build_sha})

  Last Commit: [${subject}](https://github.com/RATR2/nilum/commit/${sha})
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
