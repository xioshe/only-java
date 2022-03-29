package com.xioshe.only.java.base.net.socket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * 对 {@link EchoSocketHandler#echoThroughSocket()} 方法的测试
 *
 * @author xioshe 2022-03-27
 */
@ExtendWith(MockitoExtension.class)
class EchoSocketHandlerTests {

    EchoSocketHandler handler;

    @Mock
    Socket mockSocket;

    ByteArrayInputStream inputStream;

    ByteArrayOutputStream outputStream;

    @BeforeEach
    void setup() throws IOException {
        outputStream = new ByteArrayOutputStream();
        when(mockSocket.getOutputStream()).thenReturn(outputStream);
        handler = new EchoSocketHandler(mockSocket, 0);
    }

    @Test
    void echo() throws IOException {
        var inputMsg = "Test Java Socket Echo.";
        var expectMsg = "Hello! Enter BYE to exit.\n" + inputMsg + "\n";
        inputStream = new ByteArrayInputStream(inputMsg.getBytes(StandardCharsets.UTF_8));
        when(mockSocket.getInputStream()).thenReturn(inputStream);

        handler.echoThroughSocket();
        assertThat(outputStream.toByteArray()).as("can correct echo")
                .isEqualTo(expectMsg.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void stop_echo_when_receive_BYE() throws IOException {
        var bytes = """
                First line
                BYE
                last line
                """.getBytes(StandardCharsets.UTF_8);
        inputStream = new ByteArrayInputStream(bytes);
        when(mockSocket.getInputStream()).thenReturn(inputStream);

        handler.echoThroughSocket();
        assertThat(outputStream.toString()).as("收到结束信号后停止读输入流")
                .doesNotContain("last line")
                .endsWith("BYE\n");
    }
}