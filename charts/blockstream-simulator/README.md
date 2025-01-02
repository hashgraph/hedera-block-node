# BlockStream Simulator Helm Chart
Installs the BlockStream Simulator on a Kubernetes cluster.

## Prerequisites
- Helm 3+
- Kubernetes 1.29+

Set Release name and version to install.
```bash
export RELEASE="blockStreamSimulator"
export VERSION="0.3.0-SNAPSHOT"
```

## Template
To generate the K8 manifest files without installing the chart, you need to clone this repo and navigate to `/charts` folder.

```bash
helm template --name-template my-bs blockstream-simulator/ --dry-run --output-dir out
```

## Install using a published chart
To pull the packaged chart from the public repo:
```bash
helm pull oci://ghcr.io/hashgraph/hedera-block-node/charts/blockstream-simulator-chart--version "${VERSION}"
```

# Install the chart in Producer Mode
```bash
helm install "${RELEASE}" hedera-block-node/charts/blockstream-simulator-chart-$VERSION.tgz
```



