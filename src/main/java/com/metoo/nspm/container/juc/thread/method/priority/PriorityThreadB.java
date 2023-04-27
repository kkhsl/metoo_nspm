package com.metoo.nspm.container.juc.thread.method.priority;

import lombok.SneakyThrows;

public class PriorityThreadB extends Thread {

    @SneakyThrows
    @Override
    public void run() {
        long begin = System.currentTimeMillis();
        int sum = 0;
        for (int i = 0; i <= 10; i ++){
            sum += i;
            System.out.println("B: " + Thread.currentThread().getName() + " num: " + i);
            Thread.sleep(1000);
        }
        long end = System.currentTimeMillis();
        System.out.println("Thread B " + (end - begin));
    }
}
