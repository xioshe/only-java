package com.xioshe.only.java.base.nio.reactor;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

/**
 * 处理 IO 事件的 handler
 *
 * @author xioshe 2022-04-29
 */
public class IoHandler implements Runnable {

    enum Mode {READING, SENDING, CLOSED }

    final SocketChannel sc;
    SelectionKey sk;
    Mode mode;

    ByteBuffer input;
    ByteBuffer output;

    public IoHandler(SocketChannel sc) {
        this.sc = sc;
    }

    public void register(Selector selector) throws IOException {
        // 把提示发到界面
        sc.write(ByteBuffer.wrap("Single Reactor Mode, double Enter to exit.\r\nmsg>".getBytes()));
        mode = Mode.READING;
        input = ByteBuffer.allocate(1024);
        output = ByteBuffer.allocate(1024);

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
            switch (mode) {
                case READING -> read();
                case SENDING -> send();
            }
        } catch (IOException e) {
            try {
                sc.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private void read() throws IOException {
        input.clear(); // 清空接收缓冲区
        int n = sc.read(input);
        if (inputIsComplete(n)) {// 如果读取了完整的数据
            process();
            // 待发送的数据已经放入发送缓冲区中

            // 更改服务的逻辑状态以及要处理的事件类型
            sk.interestOps(SelectionKey.OP_WRITE);
        }
    }

    // 缓存每次读取的内容
    StringBuilder request = new StringBuilder();

    /**
     * 当读取到 \r\n 时表示结束
     *
     * @param bytes 读取的字节数，-1 通常是连接被关闭，0 非阻塞模式可能返回
     */
    protected boolean inputIsComplete(int bytes) throws IOException {
        if (bytes == -1) {
            // -1 客户端关闭了连接
            throw new EOFException();
        } else if (bytes > 0) {
            input.flip(); // 切换成读取模式
            int x = 0;
            while (input.hasRemaining()) {
                byte ch = input.get();

                if (ch == 3) { // ctrl+c 关闭连接
                    mode = Mode.CLOSED;
                    return true;
                } else if (ch == '\r' && x == 0) { // continue
                    x = 1;
                } else if (ch == '\n' && x == 1) {
                    // 读取到了 \r\n 读取结束
                    mode = Mode.SENDING;
                    return true;
                } else {
                    request.append((char)ch);
                }
            }
        }
        return false;
    }

    /**
     * 根据业务处理结果，判断如何响应
     * @throws EOFException 用户输入 ctrl+c 主动关闭
     */
    protected void process() throws EOFException {
        if (mode == Mode.CLOSED) {
            throw new EOFException();
        } else if (mode == Mode.SENDING) {
            String requestContent = request.toString(); // 请求内容
            byte[] response = requestContent.getBytes(StandardCharsets.UTF_8);
            // 向 output 写入数据
            output.put(response);
        }
    }

    private void send() throws IOException {
        output.flip();// 切换到读取模式，判断是否有数据要发送
        int written = -1;
        if (output.hasRemaining()) {
            written = sc.write(output);
        }

        // 检查连接是否处理完毕，是否断开连接
        if (outputIsComplete(written)) {
            sk.channel().close();
        } else {
            // 否则继续读取
            mode = Mode.READING;
            // 把提示发到界面
            sc.write(ByteBuffer.wrap("\r\nmsg> ".getBytes()));
            sk.interestOps(SelectionKey.OP_READ);
        }
        // 一次 tcp 连接还没有结束，为什么要关闭
//        sk.cancel();
    }

    /**
     * 当用户输入了一个空行，表示连接可以关闭了
     */
    protected boolean outputIsComplete(int written) {
        if (written <= 0) {
            // 用户只敲了个回车， 断开连接
            return true;
        }

        // 清空旧数据，接着处理后续的请求
        output.clear();
        request.delete(0, request.length());
        return false;
    }
}
