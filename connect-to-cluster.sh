#!/usr/bin/env bash

# This script is used to connect to a cluster using the kubectl command line tool.
# It assumes that the cluster is already created and running.

# These parameters should match the cluster you want to connect to
#CLUSTER_NAME="akslypprivatedev"
#SUBSCRIPTION=lyp-felles-aks-dev
#RESOURCE_GROUP=rg-lyp-weu-dev-aks

CLUSTER_NAME="akslypfellesdev"
SUBSCRIPTION=lyp-felles-aks-dev
RESOURCE_GROUP=rg-lyp-weu-aks-dev

# The subscription

DEFAULT_NAMESPACE=reference-implementation


if [ -z "$(which az)" ]; then
    echo "Azure CLI is not installed. Please install it first."
    exit 1
fi

if [ -z "$(which kubectl)" ]; then
    echo "kubectl is not installed. Please install it first."
    exit 1
fi

az login

echo "Set subscription"
az account set --subscription $SUBSCRIPTION

echo "Download cluster credentials"
az aks get-credentials --resource-group $RESOURCE_GROUP --name $CLUSTER_NAME

kubelogin convert-kubeconfig -l azurecli

echo "Set default namespace to $DEFAULT_NAMESPACE"
kubectl config set-context --current --namespace=$DEFAULT_NAMESPACE
