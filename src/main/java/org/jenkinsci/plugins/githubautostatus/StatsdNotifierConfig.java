/*
 * The MIT License
 *
 * Copyright 2018 jxpearce.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.githubautostatus;

import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;

/**
 * Class for StatsD configuration notifier.
 *
 * @author Shane Gearon (shane.gearon@hootsuite.com)
 */
public class StatsdNotifierConfig {

    private String externalizedID;
    private String statsdHost;
    private int statsdPort;
    private String statsdBucket;
    private String statsdMaxSize;
    private static final Logger LOGGER = Logger.getLogger(StatsdWrapper.class.getName());

    /**
     * Gets the externalized id.
     *
     * @return the externalized id
     */
    public String getExternalizedID() {
        return externalizedID;
    }

    /**
     * Gets the StatsD URL.
     *
     * @return the StatsD URL
     */
    public String getStatsdHost() {
        return statsdHost;
    }

    /**
     * Gets the StatsD port.
     *
     * @return the StatsD port
     */
    public int getStatsdPort() {
        return statsdPort;
    }

    /**
     * Gets the StatsD bucket.
     *
     * @return the StatsD bucket
     */
    public String getStatsdBucket() {
        return statsdBucket;
    }

    /**
     * Gets the StatsD maximum packet size.
     *
     * @return the StatsD maximum packet size
     */
    public String getStatsdMaxSize() {
        return statsdMaxSize;
    }

    /**
     * Creates a StatsD notification config based on the global settings.
     *
     * @param externalizedID externalized id
     * @return the config
     */
    public static StatsdNotifierConfig fromGlobalConfig(String externalizedID) {
        BuildStatusConfig config = BuildStatusConfig.get();
        if (!config.getEnableStatsd()) {
            return null;
        }
        StatsdNotifierConfig statsdNotifierConfig = new StatsdNotifierConfig();

        System.out.println(config.getStatsdHost());
        if (StringUtils.isEmpty(config.getStatsdHost())) {
            config.setEnableStatsd(false);
            return null;
        }

        if (config.getEnableStatsd()) {
            statsdNotifierConfig.externalizedID = externalizedID;

            statsdNotifierConfig.statsdHost = config.getStatsdHost();
            int port = 8125;
            String configPort = config.getStatsdPort() == null ? "" : config.getStatsdPort();
            if (!configPort.equals("")) {
                try {
                    port = Integer.parseInt(config.getStatsdPort());
                } catch (NumberFormatException e) {
                    LOGGER.warning("Could not parse port '" + config.getStatsdPort() + "', using 8125 (default)");
                }
            }
            statsdNotifierConfig.statsdPort = port;
            statsdNotifierConfig.statsdBucket = config.getStatsdBucket();
            statsdNotifierConfig.statsdMaxSize = config.getStatsdMaxSize();
        }

        return statsdNotifierConfig;
    }
}
