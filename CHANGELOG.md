# Version History

#### Version 3.5.0 (20-June-2019)

-   Added new feature to send metrics to StatsD collectors

#### Version 3.2 (13-Sept-2018)

-   don't include time spent blocked in total job time sent to influxdb
    (blocked time sent as a separate field).

#### Version 3.1.1 (05-Sept-2018)

-   update README, update center description, and wiki link

#### Version 3.1 (04-Sept-2018)

-   provide job name information for non-github jobs ([Github
    issue \#1](https://github.com/jenkinsci/github-autostatus-plugin/issues/12)6)

#### Version 3.0.1 (02-Sept-2018)

-   java.lang.NoClassDefFoundError in version 3.0, Jenkins
    2.121.3 ([Github
    issue \#12](https://github.com/jenkinsci/github-autostatus-plugin/issues/12))

-   Influx notifier needs to escape "=" and "," ([Github
    issue \#13](https://github.com/jenkinsci/github-autostatus-plugin/issues/13))

#### Version 3.0 (02-Aug-2018)

-   Move influxdb user/password to credentials ([Github
    issue \#9](https://github.com/jenkinsci/github-autostatus-plugin/issues/9))
-   Support secret text credentials in github status notification
    ([Github issue
    \#7](https://github.com/jenkinsci/github-autostatus-plugin/issues/7))

    Note: Version 3.0 is listed as a breaking change in the Jenkins plugin update center. This is because the optional user/password for notifying InfluxDB now uses the credentials store instead of separate strings.

    This is more secure, because otherwise the credentials are stored in plain text on the Jenkins controller.

    If you are using the InfluxDB feature (and using user/password), you will need to reconfigure the plugin to get them from a Jenkins credential after upgrading. If this does not apply to you, you don't need to do anything after upgrading.

#### Version 2.1 (23-Apr-2018)

-   Add support for all pipelines (previously only supported declarative
    pipeline jobs).
-   Added support to send stats to an influxdb server for build
    monitoring

#### Version 2.0 (06-Feb-2018)

-   Removed unnecessary serialization of GHRepository

#### Version 1.0.1 (18-Oct-2017)

-   Minor updates

#### Version 1.0 (16-Oct-2017)

-   Initial release, supporting sending stage status to github for
    declarative pipeline branches only