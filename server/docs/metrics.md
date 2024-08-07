# Metrics
This document describes the metrics that are available in the system, its purpose, and how to use them.

## Overview
We are using Prometheus to collect metrics from the system, through the use of [Hedera Platform SDK - Metrics](https://github.com/hashgraph/hedera-services/blob/develop/platform-sdk/docs/base/metrics/metrics.md).


## Purpose

The purpose of metrics is to provide a way to measure the performance of the system. Metrics are used to monitor the system and to detect any issues that may arise. Metrics can be used to identify bottlenecks, track the performance of the system over time, and to help diagnose problems.

## Configuration

### Prometheus

Prefix: prometheus, ie. `prometheus.configKey`

| ConfigKey                  | Description                                                                           | Default     |
|----------------------------|---------------------------------------------------------------------------------------|-------------|
| enableEndpoint             | either `true` or `false`. Enables or disables the endpoint for metrics                | true        |
| endpointPortNumber         | Port of the Prometheus endpoint                                                       | 9999        |
| endpointMaxBacklogAllowed  | The maximum number of incoming TCP connections which the system will queue internally | 1           |

## Usage
All classes that need to observe metrics can get them from the BlockNodeContext.

If a new metric is needed, it can be added to MetricsService, and then used in the classes that need it. This is to avoid having to define the metrics in several places.
MetricsService centralizes the creation of metrics and provides a way to access them from any other class.


