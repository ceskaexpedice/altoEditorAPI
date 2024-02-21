/*
 * Copyright (C) 2023 Lukas Sykora
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.inovatika.altoEditor.process;

import cz.inovatika.altoEditor.utils.Const;
import java.lang.Thread.UncaughtExceptionHandler;
import java.sql.Timestamp;
import java.util.Comparator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dispatcher controls scheduling of {@link FileGeneratorProcess}.
 *
 * For now it runs processes in single thread to preserve memory resources.
 *
 * @author Lukas Sykora
 */
public final class ProcessDispatcher {

    private static final Logger LOGGER = Logger.getLogger(ProcessDispatcher.class.getName());
    private static ProcessDispatcher INSTANCE = new ProcessDispatcher();

    private ExecutorService pool;
    private final int threadCount;

    public ProcessDispatcher() {
        this(1);
    }

    ProcessDispatcher(int threadCount) {
        if (threadCount < 1) {
            throw new IllegalArgumentException("threadCount: " + threadCount);
        }
        this.threadCount = threadCount;
    }

    public static ProcessDispatcher getDefault() {
        return INSTANCE;
    }

    public static void setDefault(ProcessDispatcher dispatcher) {
        INSTANCE = dispatcher;
    }

    public void init() {
        pool = newThreadPool();
    }

    public void stop() {
        stop(60, TimeUnit.SECONDS);
    }

    public void stopNow() {
        stop(5, TimeUnit.SECONDS);
    }

    public void stop(long timeout, TimeUnit unit) {
        if (pool == null) {
            return ;
        }
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(timeout, unit)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(timeout, unit)) {
                    LOGGER.severe("ImportDispatcher thread pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    public Future<FileGeneratorProcess> addPeroProcess(FileGeneratorProcess task) {
        return addTask(task);
    }

    <T extends Runnable> Future<T> addTask(T task) {
        checkRunning();
        return pool.submit(new ExceptionHandlingTask(task), task);
    }

    private void checkRunning() {
        if (pool == null) {
            throw new IllegalStateException("needs init");
        }
        if (pool.isShutdown()) {
            restart();
        }
    }

    public void restart() {
        if (pool != null && pool.isShutdown()) {
            pool = newThreadPool();
        }
    }

    private ExecutorService newThreadPool() {
        ExecutorService executorService = new ThreadPoolExecutor(threadCount, threadCount, 0L,
                TimeUnit.MILLISECONDS, new PriorityBlockingQueue<Runnable>(11, new PriorityFutureComparator ()), new ProcessDispatcherThreadFactory()) {

            @Override
            protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
                RunnableFuture<T> newTaskFor = super.newTaskFor(runnable, value);
                return new PriorityFuture<>(newTaskFor, ((FileGeneratorProcess) value).getBatch().getPriority(), ((FileGeneratorProcess) value).getBatch().getCreateDate());
            }

        };
        return executorService;
    }

    private static final class ExceptionHandlingTask implements Runnable {

        private final Runnable delegate;

        public ExceptionHandlingTask(Runnable delegate) {
            this.delegate = delegate;
        }

        @Override
        public void run() {
            try {
                delegate.run();
            } catch (Throwable t) {
                Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), t);
            }
        }

    }

    private static final class ProcessDispatcherThreadFactory implements ThreadFactory {

        private final ThreadFactory factory;

        public ProcessDispatcherThreadFactory() {
            factory = Executors.defaultThreadFactory();
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = factory.newThread(r);
            String name = ProcessDispatcher.class.getSimpleName() + '-' + thread.getName();
            thread.setName(name);
            UncaughtExceptionHandler uncaughtExceptionHandler = thread.getUncaughtExceptionHandler();
            thread.setUncaughtExceptionHandler(new ProcessDispatcherExceptionHandler(uncaughtExceptionHandler));
            return thread;
        }

    }

    private static final class ProcessDispatcherExceptionHandler implements UncaughtExceptionHandler {

        private final UncaughtExceptionHandler delegate;

        public ProcessDispatcherExceptionHandler(UncaughtExceptionHandler delegate) {
            this.delegate = delegate;
        }

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            LOGGER.log(Level.SEVERE, t.getName(), e);
        }

    }

    private class PriorityFutureComparator  implements Comparator<Runnable> {

        @Override
        public int compare(Runnable o1, Runnable o2) {
            if (o1 == null && o2 == null) {
                return 0;
            } else if (o1 == null) {
                return -1;
            } else if (o2 ==null) {
                return 1;
            } else {
                int priorityO1 = transform(((PriorityFuture) o1).getPriority());
                int priorityO2 = transform(((PriorityFuture) o2).getPriority());

                // -1 pro to, co ma bezet nejdriv
                // 0 pokud maji stejnou prioritu --> pote rozhoduje cas vzniku
                // 1 pro to, co ma bezet naposled
                return priorityO1 > priorityO2 ? -1 : (priorityO1 == priorityO2 ? compareTimestamp(o1, o2) : 1);
            }
        }

        private int compareTimestamp(Runnable o1, Runnable o2) {
            Timestamp timestampO1 = ((PriorityFuture) o1).getCreatedDate();
            Timestamp timestampO2 = ((PriorityFuture) o2).getCreatedDate();

            return timestampO1.compareTo(timestampO2);
        }

        private int transform(String priority) {
            if (priority == null) {
                return 0;
            } else if (Const.BATCH_PRIORITY_HIGH.equals(priority)) {
                return 1;
            } else if (Const.BATCH_PRIORITY_MEDIUM.equals(priority)) {
                return 0;
            } else if (Const.BATCH_PRIORITY_LOW.equals(priority)) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    private class PriorityFuture<T> implements RunnableFuture<T> {

        private RunnableFuture<T> src;
        private String priority;
        private Timestamp createdDate;

        public PriorityFuture(RunnableFuture<T> newTaskFor, String priority, Timestamp createdDate) {
            this.src = newTaskFor;
            this.priority = priority;
            this.createdDate = createdDate;
        }

        public String getPriority() {
            return priority;
        }

        public Timestamp getCreatedDate() {
            return createdDate;
        }

        @Override
        public void run() {
            src.run();
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return src.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return src.isCancelled();
        }

        @Override
        public boolean isDone() {
            return src.isDone();
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            return src.get();
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return src.get(timeout, unit);
        }
    }
}
