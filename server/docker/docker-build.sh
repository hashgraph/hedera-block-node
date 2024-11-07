#!/usr/bin/env bash

if [[ $# -lt 1 ]]; then
  echo "Usage: ${0} [version] [project_dir]"
  exit 1
fi

VERSION=$1

echo "CREATING CONTAINER FOR VERSION ${VERSION}"
echo "Using project directory: ${2}"
echo

# run docker build
echo "Building container:"
docker buildx build --load -t "block-node-server:${VERSION}" --build-context distributions=../build/distributions --build-arg VERSION="${VERSION}" . || exit "${?}"
