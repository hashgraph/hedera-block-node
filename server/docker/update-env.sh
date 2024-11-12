#!/usr/bin/env bash

# This scripts create a '.env' file that is used for docker & docker-compose as an input of environment variables.
# This script is called by gradle and get the current project version as an input param

if [ $# -lt 2 ]; then
  # <PROJECT_BUILD_ROOT> and <VERSION> are required!
  echo "USAGE: $0 <PROJECT_BUILD_ROOT> <VERSION> [DEBUG] [SMOKE_TEST]"
  exit 1
fi

project_build_root=$1
project_version=$2
# determine if we should include debug opts
[ "$3" = true ] && is_debug=true || is_debug=false
# determine if we should include smoke test env variables
[ "$4" = true ] && is_smoke_test=true || is_smoke_test=false

# work only inside the scope of the project build root
cd "$project_build_root" || exit 1

echo "VERSION=$project_version" > .env
echo "REGISTRY_PREFIX=" >> .env
# Storage root path, this is temporary until we have a proper .properties file for all configs
echo "BLOCKNODE_STORAGE_ROOT_PATH=/app/storage" >> .env

if [ true = "$is_debug" ]; then
  # wait for debugger to attach
  echo "SERVER_OPTS='-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005'" >> .env
fi

if [ true = "$is_smoke_test" ]; then
  # add smoke test variables
  echo "MEDIATOR_RING_BUFFER_SIZE=1024" >> .env
  echo "NOTIFIER_RING_BUFFER_SIZE=1024" >> .env
  echo "JAVA_OPTS='-Xms4G -Xmx4G'" >> .env
else
  # Set the production default values
  echo "JAVA_OPTS='-Xms16G -Xmx16G'" >> .env
fi

# Output the values
echo ".env properties:"
cat .env
