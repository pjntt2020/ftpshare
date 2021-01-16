package com.tools.ftpshare.boostrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * @author jpeng
 */
public class FtpBoostrap {
	private final static Logger logger = LoggerFactory.getLogger(FtpBoostrap.class);
	public static void main(String[] args) throws Exception {
		start();
	}

	private static void start() throws Exception {
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap bs = new ServerBootstrap();
			bs.option(ChannelOption.SO_BACKLOG, 1024);
			bs.option(ChannelOption.SO_KEEPALIVE, true);
			
			bs.group(bossGroup, workerGroup)
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
