package com.tools.ftpshare.boostrap;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Map;

/**
 * @author jpeng
 */
public class FtpBoostrap {
    private final static Logger logger = LoggerFactory.getLogger(FtpBoostrap.class);
    public  static String LocalIP;

    public static void main(String[] args) throws Exception {
        try {
            //FIXME：此方式获取本地IP会导致卡顿。网上方法是修改/etc/hosts 添加IPV4和IPV6两种格式的 本地IP和机器名对应关系。
            LocalIP = Inet4Address.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            logger.error("获取本地IP异常，无法使用PASV模式");
        }
        start();
    }

    private static void start() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bs = new ServerBootstrap()
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new LoggingHandler(LogLevel.DEBUG))
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ServerInitializer());

            Channel ch = bs.bind(21).sync().channel();
            logger.info("Start server port: {}", 21);

            ch.closeFuture().sync();

        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

/*	private void stop() {
		
		try {
			ch.close().sync();
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}*/

}
