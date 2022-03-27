package com.xioshe.only.java.base.net;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 用多线程支持并发请求
 *
 * @author xioshe 2021-02-18
 */
public class ParallelTcpEchoServer implements SocketServer {

    private final AtomicInteger number = new AtomicInteger(0);

    public static void main(String[] args) {
        new ParallelTcpEchoServer().startServer(8080);
    }

    @Override
    public void startServer(int port) {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Waiting for accept...");
            while (true) {
                // 不能用 try-with-resource，否则子线程执行时 Socket 已经关闭了
                Socket incoming = server.accept();
                System.out.println("Accepted a connection, number is " + getSeq());
                EchoSocketHandler handler = new EchoSocketHandler(incoming, getAndIncreaseSeq());
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getAndIncreaseSeq() {
        return number.getAndIncrement();
    }

    @Override
    public int getSeq() {
        return number.intValue();
    }
}
