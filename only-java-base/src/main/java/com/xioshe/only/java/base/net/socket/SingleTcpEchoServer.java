package com.xioshe.only.java.base.net.socket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 基于 Socket 的服务端
 * 启动一次仅支持一个连接
 *
 * @author xioshe 2021-02-12
 */
public class SingleTcpEchoServer implements SocketServer {


    public static void main(String[] args) {
        new SingleTcpEchoServer().startServer(8080);
    }


    @Override
    public void startServer(int port) {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Waiting for accept...");
            // 阻塞等待
            try (Socket socket = server.accept()) {
                System.out.println("Accepted a connection.");
                var handler = new EchoSocketHandler(socket, getAndIncreaseSeq());
                handler.echoThroughSocket();
            }
        } catch (IOException e) {
            // new ss
            e.printStackTrace();
        }
    }

    @Override
    public int getAndIncreaseSeq() {
        return 0;
    }

    @Override
    public int getSeq() {
        return 0;
    }
}
