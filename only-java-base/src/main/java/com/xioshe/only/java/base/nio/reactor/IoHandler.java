package com.xioshe.only.java.base.nio.reactor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * 处理 IO 事件的 handler
 *
 * @author xioshe 2022-04-29
 */
public class IoHandler implements Runnable {

    static int READING = 0, SENDING = 1;

    final SocketChannel sc;
    SelectionKey sk;
    int mode;
    ByteBuffer input;
    ByteBuffer output;

    public IoHandler(SocketChannel sc) {
        this.sc = sc;
    }

    public void register(Selector selector) throws IOException {
        input = ByteBuffer.allocate(1024);
        output = ByteBuffer.allocate(1024);
        mode = READING;

        // 注册 SocketChannel
        sc.configureBlocking(false);
        sk = sc.register(selector, 0);
        sk.interestOps(SelectionKey.OP_READ);
        sk.attach(this);
        // 注册了新事件，唤醒一下 selector.select()
        selector.wakeup();
    }

    @Override
    public void run() {
        try {
            if (mode == READING) {
                read();
            } else if (mode == SENDING) {
                send();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void read() throws IOException {
        while (sc.read(input) > 0) {
            input.flip();
            output.put(input);
            input.clear();
        }
        process();
    }

    void process() {
        startSendMode();
    }

    void send() throws IOException {
        output.flip();
        while (output.hasRemaining()) {
            sc.write(output);
        }
        output.clear();
        startReadMode();
    }

    void startSendMode() {
        mode = SENDING;
        sk.interestOps(SelectionKey.OP_WRITE);
    }

    void startReadMode() {
        mode = READING;
        sk.interestOps(SelectionKey.OP_READ);
    }
}
