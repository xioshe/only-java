package com.xioshe.only.java.base.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 支持多个连接的 Echo 服务器
 * 但一次只能处理一个 TCP 连接，处理完一个 TCP 连接后才能接受下一个连接
 *
 * @author xioshe 2022-03-03
 */
public class SerialTcpEchoServer implements SocketServer {

    private int number = 0;

    public static void main(String[] args) {
        new SerialTcpEchoServer().startServer(8080);
    }

    @Override
    public void startServer(int port) {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Waiting for accept...");

            while (true) {
                // 阻塞等待
                try (Socket socket = server.accept()) {
                    System.out.println("Accepted a connection, sequence number is " + getSeq());
                    var handler = new EchoSocketHandler(socket, getAndIncreaseSeq());
                    handler.echoThroughSocket();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getAndIncreaseSeq() {
        return number++;
    }

    @Override
    public int getSeq() {
        return number;
    }
}
