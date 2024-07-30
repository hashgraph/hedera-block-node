# Helm Chart
Installs the Hedera Block Node on a Kubernetes cluster.

## Prerequisites
- Helm 3+
- Kubernetes 1.29+

Set environment variables that will be used for the remainder of the document:

```bash
export RELEASE="bn1"
```

## Template
To generate the K8 manifest files without installing the chart:
```bash
helm template --name-template my-bn hedera-block-node/ --dry-run --output-dir out
```

## Install
To install the chart:
```bash
helm repo add hedera-block-node https://hashgraph.github.io/hedera-block-node/charts
helm upgrade --install "${RELEASE}" hedera-block-node/hedera-block-node
```


## Configure

## Testing

## Using

## Uninstall

## Troubleshooting

