package com.xioshe.only.java.base.net;

import java.io.*;
import java.net.Socket;

/**
 * 从 Socket 中读取消息，并回复相同消息
 *
 * @author xioshe 2022-03-27
 */
record EchoSocketHandler(Socket incoming, int number) implements Runnable {

    @Override
    public void run() {
        try {
            echoThroughSocket();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 将请求的消息直接回复回去
     *
     * @throws IOException 读输入流写输入流
     */
    void echoThroughSocket() throws IOException {
        // Closing this socket will also close the socket's InputStream and OutputStream.
        InputStream in = incoming.getInputStream();
        OutputStream out = incoming.getOutputStream();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            try (PrintWriter pw = new PrintWriter(out, true)) {
                pw.println("Hello! Enter BYE to exit.");
                for (String line; (line = reader.readLine()) != null; ) {
                    System.out.println("No." + number + " client: " + line);
                    // 回复请求
                    pw.println(line);
                }
            }
        }
    }
}
