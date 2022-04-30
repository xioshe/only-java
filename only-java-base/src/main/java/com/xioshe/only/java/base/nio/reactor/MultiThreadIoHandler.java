package com.xioshe.only.java.base.nio.reactor;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 用子线程来处理业务方法 process()
 *
 * @author xioshe 2022-04-30
 */
public class MultiThreadIoHandler extends IoHandler {

    static final int PROCESSING = 4;

    ExecutorService workerPool = Executors.newFixedThreadPool(5);

    public MultiThreadIoHandler(SocketChannel sc) {
        super(sc);
    }

    @Override
    synchronized void read() throws IOException {
        while (sc.read(input) > 0) {
            input.flip();
            output.put(input);
            input.clear();
        }
        mode = PROCESSING;
        workerPool.execute(new Processor());
        process();
    }

    private synchronized void processAndHandOff() {
        process();

        // 唤醒主线程的 select
        sk.selector().wakeup();
    }

    class Processor implements Runnable {
        @Override
        public void run() {
            processAndHandOff();
        }
    }
}
