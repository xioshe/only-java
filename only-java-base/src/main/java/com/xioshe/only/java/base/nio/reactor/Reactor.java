package com.xioshe.only.java.base.nio.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;

/**
 * 单线程 Reactor
 *
 * @author xioshe 2022-04-29
 */
public class Reactor implements Runnable, AutoCloseable {

    public static void main(String[] args) {
        try {
            Thread th = new Thread(new Reactor(9090));
            th.setName("Reactor");
            th.start();
            th.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    final Selector selector;
    final ServerSocketChannel ssc;

    public Reactor(int port) throws IOException {
        this.selector = Selector.open();
        this.ssc = ServerSocketChannel.open();
        ssc.socket().bind(new InetSocketAddress(port));
        // 注册 ServerSocketChannel
        new Acceptor(ssc).register(selector);
    }

    @Override
    public void run() {
        // 内部流程从此开始
        try {
            while (!Thread.currentThread().isInterrupted()) {
                // 监听事件
                selector.select();
                var keys = selector.selectedKeys();
                var it = keys.iterator();
                while (it.hasNext()) {
                    // 分发事件
                    dispatch(it.next());
                    it.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException {
        ssc.close();
        selector.close();
    }

    private void dispatch(SelectionKey sk) throws ClosedChannelException {
        Runnable handler = (Runnable) sk.attachment();
        if (handler != null) {
            handler.run();
        }
    }

    /**
     * 处理 accept 事件的 handler
     */
    static class Acceptor implements Runnable {

        final ServerSocketChannel ssc;

        SelectionKey acceptKey;

        public Acceptor(ServerSocketChannel ssc) {
            this.ssc = ssc;
        }

        public void register(Selector selector) throws IOException {
            ssc.configureBlocking(false);
            // 此时仅仅是注册新连接事件，事件还没有发生
            acceptKey = ssc.register(selector, SelectionKey.OP_ACCEPT, this);
        }

        @Override
        public void run() {
            // 此时新连接事件已经发生了
            try {
                var socketChannel = ssc.accept();
                if (socketChannel != null) {
                    System.out.println(socketChannel.getRemoteAddress());
                    // 注册 SocketChannel
                    new IoHandler(socketChannel).register(acceptKey.selector());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
