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

class StatsdWrapper {
    private static com.timgroup.statsd.StatsDClient client;
    private static final Logger LOGGER = Logger.getLogger(Wrapper.class.getName());
    private String hostname = "";
    private String prefix = "";
    private int port = 8125;
    private ReentrantReadWriteLock lock;
    private final int CLIENT_TTL = 300;

    /**
     * 
     * @throws StatsDClientException
     */
    private void newClient() throws StatsDClientException {
        Lock wl = lock.writeLock();
        try {
            StatsDClient newClient = new NonBlockingStatsDClient(prefix, hostname, port);
            // only aquire write lock if hostname resolution succeeded. Otherwise the
            // method will throw an exception and immediately release the lock. This is
            wl.lock();
            if (client instanceof StatsDClient) {
                // this will flush remaining messages out of queue.
                client.stop();
            }
            System.out.println("Created new client!!!");
            client = newClient;
        } catch (StatsDClientException e) {
            LOGGER.warning("Could not refresh client, will continue to use old instance");

            if (!(client instanceof StatsDClient)) {
                throw e;
            }
        } finally {
            if (lock.isWriteLocked()) {
                wl.unlock();
            }
        }
    }

    /**
     * Constructs a new StatsdWrapper.
     * 
     * @param _prefix   Statsd prefix
     * @param _hostname Statsd collector hostname (default localhost)
     * @param _port     Statsd collector listener port (default 8125)
     */
    public StatsdWrapper(String _prefix, String _hostname, int _port) throws StatsDClientException {
        hostname = _hostname;
        prefix = _prefix;
        port = _port;

        lock = new ReentrantReadWriteLock();
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();

        // TODO: Look into using https://javadoc.jenkins.io/jenkins/util/Timer.html.
        // Actually the correct one is:
        // https://javadoc.jenkins.io/hudson/model/PeriodicWork.html

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

    public void increment(String key, int amount) {
        execLocked(new Runnable() {
            @Override
            public void run() {
                System.out.println("ok ... doing increment");
                client.increment(key, amount);
            }
        });
    }
}
