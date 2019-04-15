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

import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import com.timgroup.statsd.StatsDClientException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

/**
 * Wraps regular UDP based Statd client with concurrent hostname refreshing logic.
 * 
 * @author Tom Hadlaw (thomas.hadlaw@hootsuite.com)
 */
public class StatsdWrapper {
    private static com.timgroup.statsd.StatsDClient client;
    private static final Logger LOGGER = Logger.getLogger(StatsdWrapper.class.getName());
    private String hostname = "";
    private String prefix = "";
    private int port = 8125;
    private ReentrantReadWriteLock lock;
    private final int CLIENT_TTL = 300;

    /**
     * newClient attempts to create a new statsd client instance, if succesful then
     * the active client is safely swapped out.
     * 
     * @throws StatsDClientException
     */
    private void newClient() throws StatsDClientException {
        Lock wl = lock.writeLock();
        StatsDClient newClient = null;
        try {
            newClient = new NonBlockingStatsDClient(prefix, hostname, port);
        } catch (StatsDClientException e) {
            LOGGER.warning("Could not refresh client, will continue to use old instance");

            if (client == null) {
                throw e;
            }
            return;
        }

        // only aquire write lock if hostname resolution succeeded.
        wl.lock();
        try {
            if (client != null) {
                // this will flush remaining messages out of queue.
                client.stop();
            }
            client = newClient;
        } catch (Exception e) {
            LOGGER.warning("Could not refresh client, will continue to use old instance");
            if (client == null) {
                throw e;
            }
        } finally {
            wl.unlock();
        }
    }

    /**
     * Constructs a new StatsdWrapper.
     * 
     * @param prefix   Statsd prefix
     * @param hostname Statsd collector hostname (default localhost)
     * @param port     Statsd collector listener port (default 8125)
     */
    public StatsdWrapper(String prefix, String hostname, int port) throws StatsDClientException {
        this.hostname = hostname;
        this.prefix = prefix;
        this.port = port;

        this.lock = new ReentrantReadWriteLock();
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();

        exec.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                System.out.println("Refreshing Client");
                newClient();
            }
        }, CLIENT_TTL, CLIENT_TTL, TimeUnit.SECONDS);

        this.newClient();
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
        } catch (Exception e) {
            throw e;
        } finally {
            rl.unlock();
        }
    }

    /**
     * Runs a Statsd increment in a safe way.
     * 
     * @param key    the bucket key
     * @param amount amount to increment
     */
    public void increment(String key, int amount) {
        execLocked(new Runnable() {
            @Override
            public void run() {
                System.out.println("ok ... doing increment");
                client.increment(key, amount);
            }
        });
    }

    /**
     * Run a Statsd timer state in a safe way.
     * 
     * @param key the bucket key
     * @param duration the duration
     */
    public void time(String key, long duration) {
        execLocked(new Runnable() {
            @Override
            public void run() {
                client.time(key, duration);
            }
        });
    }
}
