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

There are several ways to configure the Hedera Block Node. The following is the most common and recommended way to configure the Hedera Block Node.

1. Create an override values file, `values.yaml`:
2. Add the necessary environment configuration variables to the following section:
```yaml
blockNode:  
  config:
    # Add any additional env configuration here
    # key: value
    BLOCKNODE_STORAGE_ROOT_PATH: "/app/storage"
    
```
3. Secrets should be set at the following structure:
```yaml
blockNode:
  secret:
    PRIVATE_KEY: "<Secret>"    
```
or passed at the command line:
```bash
helm install "${RELEASE}" hedera-block-node/hedera-block-node --set blockNode.secret.PRIVATE_KEY="<Secret>"
```

## Using
Follow the `NOTES` instructions after installing the chart to perform `port-forward` to the Hedera Block Node and be able to use it.

## Uninstall
To uninstall the chart:
```bash
helm uninstall "${RELEASE}"
```

## Upgrade
To upgrade the chart:
```bash
# update the helm repo
helm repo update
# save current user supplied values
helm get values "${RELEASE}" > user-values.yaml
# upgrade the chart
helm upgrade "${RELEASE}" hedera-block-node/hedera-block-node -f user-values.yaml
```
