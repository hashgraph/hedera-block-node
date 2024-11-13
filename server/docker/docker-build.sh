#!/usr/bin/env bash

if [[ $# -lt 1 ]]; then
  echo "Usage: ${0} [version] [project_dir]"
  exit 1
fi

VERSION=$1

echo "Building image [block-node-server:${VERSION}]"
echo

# run docker build
docker buildx build --load -t "block-node-server:${VERSION}" --build-context distributions=../distributions --build-arg VERSION="${VERSION}" . || exit "${?}"

echo
echo "Image [block-node-server:${VERSION}] built successfully!"
