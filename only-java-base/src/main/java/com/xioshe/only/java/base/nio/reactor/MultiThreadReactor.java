package com.xioshe.only.java.base.nio.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 多 Reactor 多线程
 *
 * @author xioshe 2022-05-01
 */
public class MultiThreadReactor {

    public static void main(String[] args) throws IOException {
        new MultiThreadReactor(8989).start();
    }

    private final ExecutorService reactorPool = Executors.newFixedThreadPool(5);

    private final Reactor mainReactor;
    private final Reactor[] subReactors = new Reactor[4];
    final int port;

    public MultiThreadReactor(int port) {
        this.port = port;
        try {
            mainReactor = new Reactor();
            for (int i = 0; i < subReactors.length; i++) {
                // 每个 reactor 会创建一个 selector
                subReactors[i] = new Reactor();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    void start() throws IOException {
        // 先注册 Acceptor 与 MainReactor
        new Acceptor().register(mainReactor.getSelector(), port);
        reactorPool.execute(mainReactor);

        for (Reactor subReactor : subReactors) {
            // 在子线程运行 Reactor
            reactorPool.execute(subReactor);
        }
    }

    class Acceptor implements Runnable {
        ServerSocketChannel ssc;
        int next = 0;

        public void register(final Selector selector, int port) throws IOException {
            this.ssc = ServerSocketChannel.open();
            ssc.socket().bind(new InetSocketAddress(port));
            ssc.configureBlocking(false);
            ssc.register(selector, SelectionKey.OP_ACCEPT, this);
        }

        @Override
        public synchronized void run() {
            System.out.println("Acceptor start.");
            try {
                var sc = ssc.accept();
                if (sc != null) {
                    var sr = subReactors[next++];
                    sr.register(new IoHandler(sc));
                    // 多线程版 handler
//                    sr.register(new MultiThreadIoHandler(sc));
                    next %= subReactors.length;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    static class Reactor implements Runnable {
        final Selector selector;
        public Reactor() throws IOException {
            this.selector = Selector.open();
        }
        public Selector getSelector() {
            return selector;
        }

        @Override
        public void run() {
            System.out.println("Reactor start in thread-" + Thread.currentThread().getId());
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    selector.select();
                    Set<SelectionKey> selected = selector.selectedKeys();
                    Iterator<SelectionKey> it = selected.iterator();
                    while (it.hasNext()) {
                        SelectionKey sk = it.next();
                        dispatch(sk);
                        it.remove();
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        void dispatch(SelectionKey k) {
            Runnable r = (Runnable) (k.attachment()); // 拿到通道注册时附加的对象
            if (r != null) r.run();
        }

        /**
         * 向 Reactor 中注册 IoHandler，所以这个方法只适用于 SubReactor
         * <br/>所以注册事件发生在调用线程中，也就是 Acceptor 所在线程上。
         * <br/>如果想提升性能，可以使用一个同步队列来延迟注册
         *
         * @param basicHandler {@link IoHandler}
         */
        void register(IoHandler basicHandler) throws IOException {
            basicHandler.register(selector);
        }
    }

}
