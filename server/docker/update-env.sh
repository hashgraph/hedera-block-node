#!/usr/bin/env bash

# This scripts create a '.env' file that is used for docker & docker-compose as an input of environment variables.
# This script is called by gradle and get the current project version as an input param

if [ $# -lt 1 ]; then
  echo "USAGE: $0 <VERSION> <DEBUG> <SMOKE_TEST>"
  exit 1
fi

project_version=$1
# determine if we should include debug opts
[ "$2" = true ] && is_debug=true || is_debug=false
# determine if we should include smoke test env variables
[ "$3" = true ] && is_smoke_test=true || is_smoke_test=false

echo "VERSION=$project_version" > .env
echo "REGISTRY_PREFIX=" >> .env
# Storage root path, this is temporary until we have a proper .properties file for all configs
echo "BLOCKNODE_STORAGE_ROOT_PATH=/app/storage" >> .env
echo "JAVA_OPTS='-Xms8G -Xmx16G'" >> .env

if [ true = "$is_debug" ]; then
  # wait for debugger to attach
  echo "SERVER_OPTS='-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005'" >> .env
fi

if [ true = "$is_smoke_test" ]; then
  # add smoke test variables
  echo "MEDIATOR_RING_BUFFER_SIZE=1024" >> .env
  echo "NOTIFIER_RING_BUFFER_SIZE=1024" >> .env
fi

# Output the values
echo ".env properties:"
cat .env
