#!/usr/bin/env bash

# This scripts create a '.env' file that is used for docker & docker-compose as an input of environment variables.
# This script is called by gradle and get the current project version as an input param

if [ $# -lt 1 ]; then
  echo "USAGE: $0 <VERSION> <DEBUG> <SMOKE_TEST>"
  exit 1
fi

project_version=$1
# in cases where no params are supplied for debug or smoke tests, later we still need to log something
is_debug=$([ "$2" = "true" ] && echo "true" || echo "false")
is_smoke_test=$([ "$3" = "true" ] && echo "true" || echo "false")

echo "VERSION=$project_version" > .env
echo "REGISTRY_PREFIX=" >> .env
# Storage root path, this is temporary until we have a proper .properties file for all configs
echo "BLOCKNODE_STORAGE_ROOT_PATH=/app/storage" >> .env

if [ true = "$is_debug" ]; then
  # wait for debugger to attach
  echo "SERVER_OPTS='-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005'" >> .env
fi

if [ true = "$is_smoke_test" ]; then # TODO make sure every place this script is called knows about this change!
  # add smoke test variables
  echo "MEDIATOR_RING_BUFFER_SIZE=1024" >> .env
  echo "NOTIFIER_RING_BUFFER_SIZE=1024" >> .env
fi

# Output the values
echo ".env properties:"
echo "IS DEBUG: $is_debug"
echo "IS SMOKE_TEST: $is_smoke_test"
echo "VERSION/TAG UPDATED TO $1"
