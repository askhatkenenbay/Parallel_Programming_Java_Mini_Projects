package edu.coursera.parallel;

import java.util.concurrent.Phaser;

/**
 * Wrapper class for implementing one-dimensional iterative averaging using
 * phasers.
 */
public final class OneDimAveragingPhaser {
    /**
     * Default constructor.
     */
    private OneDimAveragingPhaser() {
    }

    /**
     * Sequential implementation of one-dimensional iterative averaging.
     *
     * @param iterations The number of iterations to run
     * @param myNew      A double array that starts as the output array
     * @param myVal      A double array that contains the initial input to the
     *                   iterative averaging problem
     * @param n          The size of this problem
     */
    public static void runSequential(final int iterations, final double[] myNew,
                                     final double[] myVal, final int n) {
        double[] next = myNew;
        double[] curr = myVal;

        for (int iter = 0; iter < iterations; iter++) {
            for (int j = 1; j <= n; j++) {
                next[j] = (curr[j - 1] + curr[j + 1]) / 2.0;
            }
            double[] tmp = curr;
            curr = next;
            next = tmp;
        }
    }

    /**
     * An example parallel implementation of one-dimensional iterative averaging
     * that uses phasers as a simple barrier (arriveAndAwaitAdvance).
     *
     * @param iterations The number of iterations to run
     * @param myNew      A double array that starts as the output array
     * @param myVal      A double array that contains the initial input to the
     *                   iterative averaging problem
     * @param n          The size of this problem
     * @param tasks      The number of threads/tasks to use to compute the solution
     */
    public static void runParallelBarrier(final int iterations,
                                          final double[] myNew, final double[] myVal, final int n,
                                          final int tasks) {
        Phaser ph = new Phaser(0);
        ph.bulkRegister(tasks);

        Thread[] threads = new Thread[tasks];

        for (int ii = 0; ii < tasks; ii++) {
            final int i = ii;

            threads[ii] = new Thread(() -> {
                double[] threadPrivateMyVal = myVal;
                double[] threadPrivateMyNew = myNew;

                final int chunkSize = (n + tasks - 1) / tasks;
                final int left = (i * chunkSize) + 1;
                int right = (left + chunkSize) - 1;
                if (right > n) right = n;

                for (int iter = 0; iter < iterations; iter++) {
                    for (int j = left; j <= right; j++) {
                        threadPrivateMyNew[j] = (threadPrivateMyVal[j - 1]
                                + threadPrivateMyVal[j + 1]) / 2.0;
                    }
                    ph.arriveAndAwaitAdvance();

                    double[] temp = threadPrivateMyNew;
                    threadPrivateMyNew = threadPrivateMyVal;
                    threadPrivateMyVal = temp;
                }
            });
            threads[ii].start();
        }

        for (int ii = 0; ii < tasks; ii++) {
            try {
                threads[ii].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * A parallel implementation of one-dimensional iterative averaging that
     * uses the Phaser.arrive and Phaser.awaitAdvance APIs to overlap
     * computation with barrier completion.
     * <p>
     * TODO Complete this method based on the provided runSequential and
     * runParallelBarrier methods.
     *
     * @param iterations The number of iterations to run
     * @param myNew      A double array that starts as the output array
     * @param myVal      A double array that contains the initial input to the
     *                   iterative averaging problem
     * @param n          The size of this problem
     * @param tasks      The number of threads/tasks to use to compute the solution
     */
    public static void runParallelFuzzyBarrier(final int iterations,
                                               final double[] myNew, final double[] myVal, final int n,
                                               final int tasks) {
        Phaser[] phasers = new Phaser[tasks];
        for (int i = 0; i < phasers.length; i++) {
            phasers[i] = new Phaser(1);
        }
        Thread[] threads = new Thread[tasks];
        for (int k = 0; k < tasks; k++) {
            final int i = k;
            threads[k] = new Thread(() -> {
                double[] myVal1 = myVal;
                double[] myNew1 = myNew;
                for (int iter = 0; iter < iterations; iter++) {
                    final int chunkSize = (n + tasks - 1) / tasks;
                    final int left = (i * chunkSize) + 1;
                    int right = (left + chunkSize) - 1;
                    if (right > n) right = n;
                    for (int j = left; j <= right; j++) {
                        myNew1[j] = (myVal1[j - 1]
                                + myVal1[j + 1]) / 2.0;
                    }
                    int currPhase = phasers[i].arrive();
                    if (i - 1 >= 0) {
                        phasers[i - 1].awaitAdvance(currPhase);
                    }
                    if (i + 1 < tasks) {
                        phasers[i + 1].awaitAdvance(currPhase);
                    }
                    double[] temp = myNew1;
                    myNew1 = myVal1;
                    myVal1 = temp;
                }
            });
            threads[k].start();
        }
        for (int k = 0; k < tasks; k++) {
            try {
                threads[k].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
