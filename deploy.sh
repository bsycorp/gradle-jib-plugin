#!/bin/bash
set -e

if [ -z "$CI_BRANCH" ]; then
  CI_BRANCH="${GITHUB_REF##*/}"
fi
./gradlew build publish -i -PappVersion="$CI_BRANCH" --stacktrace
