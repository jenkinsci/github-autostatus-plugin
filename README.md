# Autostatus Plugin

This is a job monitoring plugin, which watches for builds from multibranch projects and send job and stage level stats to various endpoints, including

- Github (to see pending/success/error status from your PR)
- InfluxDB (for building monitoring dashboards).

It doesn't require modifying your Jenkinsfiles - once installed, it automatically provides stats for all multibranch jobs ran on the Jenkins master.

## Github status

The plugin can be configured to send commit status for each stage for jobs build from a multibranch project. For example:

![](images/github-status.png)

## Grafana dashboards

The plugin can also be configured to send job and stage stats to in InfluxDB instance, which can be used to build dashboards in Grafana to monitor build time and success rate. For example:

![](images/grafana-dashboard.png)

For more information, please visit [the plugin's page on jenkins.io](https://plugins.jenkins.io/github-autostatus).
