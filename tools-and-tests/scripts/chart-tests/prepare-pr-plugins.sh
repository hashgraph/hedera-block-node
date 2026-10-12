#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
#
# Make the chart install tests use the PR's build of plugins that main has not published to Maven yet.
#
# The chart's resolve-plugins init container downloads plugins from Maven, so a plugin that is new in a PR
# (currently facility-app-state, which the PR's core requires) cannot be resolved until main publishes it,
# and the install fails. This script edits the chart in the CI workspace only (nothing is committed):
#   1. removes the plugin from `plugins.names` in values.yaml and the ci/ values, so Maven is not asked for
#      it, and
#   2. adds an init container to the default `blockNode.initContainers` in values.yaml that copies the plugin
#      jar from the PR's locally built solo-dev image (which bakes the PR's plugins) into the plugins volume.
#      resolve-plugins only removes the jars it resolved itself, so the copied jar is kept.
# The ci/ values do not set initContainers, so every test inherits the default. It also loads the solo-dev
# image into the kind cluster.
#
# Usage: prepare-pr-plugins.sh <version> [kind-cluster=chart-testing]
set -euo pipefail

VERSION="${1:?version required}"
KIND_CLUSTER="${2:-chart-testing}"
# Plugins taken from the PR build instead of Maven.
PR_PLUGINS=(facility-app-state)
SOLO_DEV_IMAGE="block-node-server-solo-dev:${VERSION}"
VALUES=charts/block-node-server/values.yaml

# 1. drop the PR-only plugins from the names Maven resolves
for plugin in "${PR_PLUGINS[@]}"; do
  find charts/block-node-server -maxdepth 3 -name '*.yaml' \( -name values.yaml -o -path '*/ci/*' \) \
    -exec perl -pi -e "s/\\b${plugin},//g; s/,${plugin}\\b//g" {} +
done

# 2. copy the PR-built jars in with an init container
COPY_CMDS=""
for plugin in "${PR_PLUGINS[@]}"; do
  COPY_CMDS+="cp /opt/hiero/block-node/app-${VERSION}/plugins/${plugin}-*.jar /plugins/ && "
done
python3 - "${VALUES}" "${SOLO_DEV_IMAGE}" "${COPY_CMDS}" <<'PY'
import sys

path, image, copy_cmds = sys.argv[1:4]
anchor = "        - name: logging-storage\n          mountPath: /logging-pvc\n"
block = f"""    - name: copy-pr-plugins
      image: {image}
      imagePullPolicy: Never
      command:
        - sh
        - -c
        - {copy_cmds}chmod 644 /plugins/*.jar
      volumeMounts:
        - name: plugins-storage
          mountPath: /plugins
"""
with open(path) as f:
    text = f.read()
if text.count(anchor) != 1:
    sys.exit(f"expected exactly one init-storage-dirs anchor in {path}, found {text.count(anchor)}")
with open(path, "w") as f:
    f.write(text.replace(anchor, anchor + block))
PY
echo "updated ${VALUES}"

kind load docker-image "${SOLO_DEV_IMAGE}" --name "${KIND_CLUSTER}"
