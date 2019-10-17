/*
 * The MIT License
 *
 * Copyright 2019 Tom Hadlaw.
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

import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import com.timgroup.statsd.StatsDClientException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

/**
 * Wraps regular UDP based StatsD client with concurrent hostname refreshing logic.
 *
 * @author Tom Hadlaw (thomas.hadlaw@hootsuite.com)
 */
public class StatsdClient implements StatsdWrapper {

    private final int CLIENT_TTL = 300;
    private static final Logger LOGGER = Logger.getLogger(StatsdClient.class.getName());

    private StatsDClient client;
    private String hostname = "";
    private String prefix = "";
    private int port = 8125;
    private ReentrantReadWriteLock lock;

    private static volatile StatsdClient statsDClient;

    /**
     * Attempts to create a new StatsD client instance, if successful then
     * the active client is safely swapped out.
     *
     * @throws StatsDClientException if unable to refresh client
     */
    public void newClient() throws StatsDClientException {
        Lock wl = lock.writeLock();
        StatsDClient newClient = null;
        try {
            newClient = new NonBlockingStatsDClient(prefix, hostname, port);
            LOGGER.info("New StatsD client created. " + newClient.hashCode());
        } catch (StatsDClientException e) {
            LOGGER.warning("Could not refresh client, will continue to use old instance");

            if (this.client == null) {
                throw e;
            }
            return;
        }

        // only acquire write lock if hostname resolution succeeded.
        wl.lock();
        try {
            if (this.client != null) {
                // this will flush remaining messages out of queue.
                this.client.stop();
            }
            this.client = newClient;
        } catch (Exception e) {
            LOGGER.warning("Could not refresh client, will continue to use old instance");
            if (this.client == null) {
                throw e;
            }
        } finally {
            wl.unlock();
        }
    }

    /**
     * Constructs a new StatsD client.
     *
     * @param prefix   StatsD prefix
     * @param hostname StatsD collector hostname (default localhost)
     * @param port     StatsD collector listener port (default 8125)
     */
    public StatsdClient(String prefix, String hostname, int port) throws StatsDClientException {
        this.hostname = hostname;
        this.prefix = prefix;
        this.port = port;

        this.lock = new ReentrantReadWriteLock();
        ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1);
        exec.setRemoveOnCancelPolicy(true);

        Runnable refreshClient = new Runnable() {
            @Override
            public void run() {
                newClient();
            }
        };
        exec.scheduleAtFixedRate(refreshClient, CLIENT_TTL, CLIENT_TTL, TimeUnit.SECONDS);

        this.newClient();
        LOGGER.info("StatsdClient wrapper created. " + this.hashCode());
    }

    public static StatsdClient getInstance(String prefix, String hostname, int port) {
        if (statsDClient == null) {
            synchronized (StatsdClient.class) {
                // double check locking method to make singleton thread safe
                if (statsDClient == null) {
                    statsDClient = new StatsdClient(prefix, hostname, port);
                }
            }
        }

        return statsDClient;
    }

    /**
     * Execute Invokes runnable surrounded in the objects readlock.
     *
     * @param l Runnable to be invoked.
     */
    private void execLocked(Runnable l) {
        Lock rl = lock.readLock();
        rl.lock();
        try {
            l.run();
        } finally {
            rl.unlock();
        }
    }

    /**
     * Runs a StatsD increment in a safe way.
     *
     * @param key    the bucket key
     * @param amount amount to increment
     */
    @Override
    public void increment(String key, int amount) {
        execLocked(new Runnable() {
            @Override
            public void run() {
                LOGGER.info("Logging value " + amount + " for key " + key);
                client.increment(key, amount);
            }
        });
    }

    /**
     * Runs a StatsD timer state in a safe way.
     *
     * @param key the bucket key
     * @param duration the duration
     */
    @Override
    public void time(String key, long duration) {
        execLocked(new Runnable() {
            @Override
            public void run() {
                LOGGER.info("Logging duration " + duration + " for key " + key);
                client.time(key, duration);
            }
        });
    }
}
