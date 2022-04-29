package com.xioshe.only.java.base.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * NIO channel-channel copy, but not zero-copy
 *
 * @author xioshe 2022-04-29
 */
public class ChannelCopy {

    public static void copy() throws IOException {
        var source = Channels.newChannel(System.in);
        var dest = Channels.newChannel(System.out);
        try (source;dest){
            channelCopy(source, dest);
        }
    }

    private static void channelCopy(ReadableByteChannel source, WritableByteChannel dest) throws IOException {
        // direct buffer 要注意回收
        var buffer = ByteBuffer.allocate(16 * 1024);
        while (source.read(buffer) != -1) {
            buffer.flip();
            // write 不一定全部读完了
            dest.write(buffer);
            // 使用 compact 更高效，但是需要在循环外检查是否读完
            buffer.compact();
        }

        buffer.flip();
        while (buffer.hasRemaining()) {
            dest.write(buffer);
        }
    }
}
