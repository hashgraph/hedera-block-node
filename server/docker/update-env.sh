#!/usr/bin/env bash

# This scripts create a '.env' file that is used for docker & docker-compose as an input of environment variables.
# This script is called by gradle and get the current project version as an input param

if [ $# -lt 1 ]; then
  echo "USAGE: $0 <VERSION> <DEBUG>"
  exit 1
fi

echo "VERSION=$1" > .env
echo "REGISTRY_PREFIX=" >> .env
# Storage root path, this is temporary until we have a proper .properties file for all configs
echo "BLOCKNODE_STORAGE_ROOT_PATH=/app/storage" >> .env

if [ $# -eq 2 ]; then
  echo "SERVER_OPTS='-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005'" >> .env
fi

echo "DEBUG $2"
echo "VERSION/TAG UPDATED TO $1"
