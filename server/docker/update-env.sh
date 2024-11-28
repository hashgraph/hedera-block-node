#!/usr/bin/env bash

# This scripts create a '.env' file that is used for docker & docker-compose as an input of environment variables.
# This script is called by gradle and get the current project version as an input param

if [ $# -lt 1 ]; then
  # <VERSION> is required!
  echo "USAGE: $0 <VERSION> [DEBUG] [SMOKE_TEST]"
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

if [ true = "$is_debug" ]; then
  # The server will wait for the debugger to attach on port 5005
  # JProfiler can attach on port 8849
  echo "SERVER_OPTS='-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005' -agentpath:/path/to/libjprofilerti.so=port=8849 " >> .env
fi

if [ true = "$is_smoke_test" ]; then
  # add smoke test variables
  # @todo(#372) - default persistence type should be BLOCK_AS_LOCAL_FILE
  echo "PERSISTENCE_STORAGE_TYPE=BLOCK_AS_LOCAL_DIRECTORY" >> .env
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
echo