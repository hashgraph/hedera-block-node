# Helm Chart
Installs the Hedera Block Node on a Kubernetes cluster.

## Prerequisites
- Helm 3+
- Kubernetes 1.29+

For Development and Test environment is recommended to use minikube with the following command (if want to deploy the kube-prometheus-stack for metrics visualization):
```bash
minikube delete && minikube start --kubernetes-version=v1.23.0 --memory=6g --bootstrapper=kubeadm --extra-config=kubelet.authentication-token-webhook=true --extra-config=kubelet.authorization-mode=Webhook --extra-config=scheduler.bind-address=0.0.0.0 --extra-config=controller-manager.bind-address=0.0.0.0
```

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
helm dependency update charts/hedera-block-node
helm install "${RELEASE}" charts/hedera-block-node -f <path-to-custom-values-file>
```
*Note:* If using the chart directly after cloning the github repo, there is no need to add the repo. and install can be directly.
Assuming you are at the root folder of the repo.
```bash
helm dependency build charts/hedera-block-node

helm install "${RELEASE}" charts/hedera-block-node -f <path-to-custom-values-file>
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

### Enable Prometheus + Grafana Stack
By default the stack includes a chart dependency that includes a prometheus + grafana + node-exporter stack, also adds 3 provisioned dashboards
- **Hedera Block Node Dashboard:** to monitor the Hedera Block Node metrics, this are the server application specific metrics. 
- **Node Exporter Full:** to monitor the node-exporter metrics, system metrics at the K8 cluster/node level.
- **Kubernetes View Pods:** to monitor the kubernetes pods metrics, system metrics at the container level.

If you prefer to use your own prometheus+grafana stack, you can disable the stack by setting the following values:
```yaml
kube-prometheus-stack:
  enabled: false
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
