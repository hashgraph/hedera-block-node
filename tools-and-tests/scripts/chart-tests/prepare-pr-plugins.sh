#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
#
# Make the chart install tests use the PR's build of plugins that main has not published to Maven yet.
#
# The chart's resolve-plugins init container downloads plugins from Maven, so a plugin that is new in a PR
# (currently facility-app-state, which the PR's core requires) cannot be resolved until main publishes it,
# and the install fails. This script:
#   1. removes the plugin from `plugins.names` in the chart values the tests use (workspace edit only), so
#      Maven is not asked for it, and
#   2. writes a values overlay that adds an init container copying the plugin jar from the PR's locally built
#      solo-dev image (which bakes the PR's plugins) into the plugins volume. resolve-plugins only removes
#      the jars it resolved itself, so the copied jar is kept.
# It also loads the solo-dev image into the kind cluster.
#
# Pass the overlay to helm after the test's own values file, e.g. `--values "${OVERLAY}"`.
#
# Usage: prepare-pr-plugins.sh <version> [kind-cluster=chart-testing] [overlay=/tmp/pr-plugins-values.yaml]
set -euo pipefail

VERSION="${1:?version required}"
KIND_CLUSTER="${2:-chart-testing}"
OVERLAY="${3:-/tmp/pr-plugins-values.yaml}"
# Plugins taken from the PR build instead of Maven.
PR_PLUGINS=(facility-app-state)
SOLO_DEV_IMAGE="block-node-server-solo-dev:${VERSION}"

# 1. drop the PR-only plugins from the names Maven resolves
for plugin in "${PR_PLUGINS[@]}"; do
  find charts/block-node-server -maxdepth 3 -name '*.yaml' \( -name values.yaml -o -path '*/ci/*' \) \
    -exec perl -pi -e "s/\\b${plugin},//g; s/,${plugin}\\b//g" {} +
done

# 2. overlay with the copy init container. Helm replaces lists, so the chart's default
# init-storage-dirs container is restated unchanged.
COPY_CMDS=""
for plugin in "${PR_PLUGINS[@]}"; do
  COPY_CMDS+="cp /opt/hiero/block-node/app-${VERSION}/plugins/${plugin}-*.jar /plugins/ && "
done
cat > "${OVERLAY}" <<YAML
blockNode:
  initContainers:
    - name: init-storage-dirs
      image: busybox
      command:
        - sh
        - -c
        - |
          mkdir -p /application-state-pvc && \\
          chown 2000:2000 /application-state-pvc && \\
          chmod 700 /application-state-pvc && \\
          mkdir -p /archive-pvc/archive-data && \\
          chown 2000:2000 /archive-pvc/archive-data && \\
          chmod 700 /archive-pvc/archive-data && \\
          mkdir -p /live-pvc/live-data && \\
          chown 2000:2000 /live-pvc/live-data && \\
          chmod 700 /live-pvc/live-data
      volumeMounts:
        - name: application-state-storage
          mountPath: /application-state-pvc
        - name: archive-storage
          mountPath: /archive-pvc
        - name: live-storage
          mountPath: /live-pvc
        - name: logging-storage
          mountPath: /logging-pvc
    - name: copy-pr-plugins
      image: ${SOLO_DEV_IMAGE}
      imagePullPolicy: Never
      command:
        - sh
        - -c
        - ${COPY_CMDS}chmod 644 /plugins/*.jar
      volumeMounts:
        - name: plugins-storage
          mountPath: /plugins
YAML
echo "wrote ${OVERLAY}"

kind load docker-image "${SOLO_DEV_IMAGE}" --name "${KIND_CLUSTER}"
