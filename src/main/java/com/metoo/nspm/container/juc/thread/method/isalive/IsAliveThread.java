package com.metoo.nspm.container.juc.thread.method.isalive;

public class IsAliveThread extends Thread {

    @Override
    public void run() {
        System.out.println("子线程运行状态：" + this.isAlive());
    }

}
