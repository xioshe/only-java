package com.xioshe.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Netty 版 Echo Server
 *
 * @author xioshe 2022-05-03
 */
public class EchoServer {

    public static void main(String[] args) throws InterruptedException {
        if (args.length != 1) {
            System.out.println("Usage: " + EchoServer.class.getSimpleName() + " <port>");
            return;
        }
        var port = Integer.parseInt(args[0]);
        new EchoServer(port).start();
    }

    private final int port;

    public EchoServer(int port) {
        this.port = port;
    }

    public void start() throws InterruptedException {
        var echoHandler = new EchoServerHandler();
        EventLoopGroup elGroup = new NioEventLoopGroup();
        try {
            var serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(elGroup)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(port))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(echoHandler);
                        }
                    });
            // 阻塞绑定
            var bindFuture = serverBootstrap.bind().sync();
            // 阻塞等待通道关闭时的通知，主线程一直阻塞
            bindFuture.channel().closeFuture().sync();
        } finally {
            // 阻塞停止事件循环
            elGroup.shutdownGracefully().sync();
        }
    }

    @ChannelHandler.Sharable
    private static class EchoServerHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ByteBuf in = (ByteBuf) msg;
            System.out.println("Server received: " + in.toString(StandardCharsets.UTF_8));
            // next channel handler
            ctx.write(in);
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            // 写入一个空 buffer，目的是触发 flush
            ctx.writeAndFlush(Unpooled.EMPTY_BUFFER)
                    // 监听 writeAndFlush 异步操作，完成后关闭该 Channel
                    .addListener(ChannelFutureListener.CLOSE);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            // 关闭 Channel
            ctx.close();
        }
    }

}
