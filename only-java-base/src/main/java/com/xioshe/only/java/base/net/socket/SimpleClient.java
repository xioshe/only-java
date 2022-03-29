package com.xioshe.only.java.base.net.socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

/**
 * 简易客户端，读取命令行输入，发送至服务端，并输出服务端返回消息
 *
 * @author xioshe 2021-02-12
 */
public record SimpleClient(String serverHost, int serverPort) {

    public static void main(String[] args) {
        new SimpleClient("localhost", 8080).startClient();
    }

    /**
     * <b>优化思路：</b>
     * <ul>
     *     <li>获取输入的方法分离出去</li>
     *     <li>考虑对连接进行延迟初始化，确定获取用户输入后</li>
     * </ul>
     *
     */
    private void startClient() {
        try (Socket socket = new Socket(serverHost, serverPort);
             PrintWriter sw = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader sr = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner scanner = new Scanner(System.in)) {
            System.out.println("Server connected.");
            for (String line; (line = sr.readLine()) != null; ) {
                System.out.println("Server: " + line);
                if ("BYE".equals(line)) {
                    System.out.println("Connection will be closed...");
                    break;
                }

                System.out.println("Please input something:");
                // hasNextLine 会阻塞
                if (scanner.hasNextLine()) {
                    String input = scanner.nextLine();
                    // Send to server
                    sw.println(input);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
