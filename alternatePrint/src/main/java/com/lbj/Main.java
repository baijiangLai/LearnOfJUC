package com.lbj;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Main {
    public static void main(String[] args) {
//        Object lock = new Object();
//
//        Thread threadA = new Thread(new SynchronizedPrintThread(lock, "A", 0), "线程A");
//        Thread threadB = new Thread(new SynchronizedPrintThread(lock, "B",1), "线程B");
//        Thread threadC = new Thread(new SynchronizedPrintThread(lock, "C",2), "线程C");
//
//        threadA.start();
//        threadB.start();
//        threadC.start();

//        ReentrantLock lock = new ReentrantLock();
//
//
//        Condition conditionA = lock.newCondition();
//        Condition conditionB = lock.newCondition();
//        Condition conditionC = lock.newCondition();
//
//
//        Thread threadA = new Thread(new ReentrantLockPrintThread(lock, conditionA, conditionB, "A", 0));
//        Thread threadB = new Thread(new ReentrantLockPrintThread(lock, conditionB, conditionC, "B", 1));
//        Thread threadC = new Thread(new ReentrantLockPrintThread(lock, conditionC, conditionA, "C", 2));
//
//        threadA.start();
//        threadB.start();
//        threadC.start();


        Semaphore semaphoreA = new Semaphore(1);
        Semaphore semaphoreB = new Semaphore(0);
        Semaphore semaphoreC = new Semaphore(0);

        Thread threadA = new Thread(new SemaphorePrintThread(semaphoreA, semaphoreB, "A", 0));
        Thread threadB = new Thread(new SemaphorePrintThread(semaphoreB, semaphoreC, "B", 1));
        Thread threadC = new Thread(new SemaphorePrintThread(semaphoreC, semaphoreA, "C", 2));

        threadA.start();
        threadB.start();
        threadC.start();
    }
}

/**
 * 使用synchronized方式实现
 */
class SynchronizedPrintThread implements Runnable {
    private final Object lock;
    private final String message;
    private final int order;
    private static int currentOrder = 0;

    public SynchronizedPrintThread(Object lock, String message, int order) {
        this.lock = lock;
        this.message = message;
        this.order = order;
    }

    @Override
    public void run() {
        try {
            synchronized (lock) {
                for (int i = 0; i < 10; i++) {
                    // 打印顺序就是A-->B-->C
                    while (currentOrder % 3 != order) {
                        lock.wait(); // 当前线程等待，直到轮到自己打印
                    }
                    System.out.println(Thread.currentThread().getName() + "------" + message);
                    currentOrder++;
                    lock.notifyAll(); // 唤醒其他等待的线程
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}


/**
 * 使用ReentrantLock方式实现
 *
 *
 * Condition对象是与锁（ReentrantLock）关联的条件对象，用于线程间的等待和通知机制。
 * 每个Condition对象都与一个锁相关联，可以通过调用锁的newCondition()方法来创建。
 * Condition对象提供了await()、signal()和signalAll()等方法，用于线程的等待和通知操作。
 */

class ReentrantLockPrintThread implements Runnable {
    private final Lock lock;
    private final Condition current;
    private final Condition next;
    private final String message;
    private final int order;

    // 多个线程共享，如果没有static的话，那么会造成只有第一个线程执行了，其余线程还在等待第一个线程执行。
    private static int currentOrder = 0;

    public ReentrantLockPrintThread(Lock lock, Condition currentCondition, Condition nextCondition, String message, int order) {
        this.lock = lock;
        this.current = currentCondition; //当前线程信号量
        this.next = nextCondition;      //下一个线程信号量
        this.message = message;
        this.order = order;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < 10; i++) {
                // 某个线程持有锁，只有一个线程进入后续部分
                lock.lock();
                // 条件判断，如果不满足，当前线程等待，
                while (currentOrder % 3 != order) {
                    current.await();
                }
                System.out.println(Thread.currentThread().getName() + "------" + message);
                currentOrder++;
                // 使用signal()唤醒下一个线程
                next.signal();
                // 锁释放
                lock.unlock();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}


/**
 * 信号量的方式实现
 */
class SemaphorePrintThread implements Runnable {
    private final Semaphore current;
    private final Semaphore next;
    private static int currentOrder = 0;

    private final String message;
    private final int order;

    public SemaphorePrintThread(Semaphore current, Semaphore next, String message, int order) {
        this.current = current;
        this.next = next;
        this.message = message;
        this.order = order;
    }

    @Override
    public void run() {
        try {

            for (int i = 0; i < 10; i++) {
                /**
                 * 核心：通过获取当前线程的信号量来确定是否轮到自己执行。
                 * 在执行完打印操作后，释放下一个线程的信号量，从而实现线程的交叉打印。
                 */

                current.acquire();      // 获取当前线程的信号量
                System.out.println(Thread.currentThread().getName() + "------" + message);
                currentOrder++;
                next.release();         //释放下一个线程的信号量
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
