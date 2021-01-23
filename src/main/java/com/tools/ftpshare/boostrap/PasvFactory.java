package com.tools.ftpshare.boostrap;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tools.ftpshare.common.Session;
import com.tools.ftpshare.handlers.DataTransferHandler;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.FileRegion;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * PASV服务端口工厂
 *
 * @author jpeng
 */
public enum PasvFactory {
	/**
	 * 单例
	 */
	INS;
//	private ServerBootstrap pasvBootstrop = new ServerBootstrap().option(ChannelOption.SO_BACKLOG, 1024)
//			.childOption(ChannelOption.SO_KEEPALIVE, true).group(new NioEventLoopGroup(), new NioEventLoopGroup())
//			.channel(NioServerSocketChannel.class).handler(new LoggingHandler(LogLevel.DEBUG));
	
	
	private ServerBootstrap pasvBootstrop = new ServerBootstrap().option(ChannelOption.SO_BACKLOG, 1024)
			.childOption(ChannelOption.SO_KEEPALIVE, true).group(new NioEventLoopGroup(), new NioEventLoopGroup())
			.channel(NioServerSocketChannel.class).handler(new LoggingHandler(LogLevel.DEBUG));

	// .childHandler(new PasvInitializer());

	private final Logger logger = LoggerFactory.getLogger(PasvFactory.class);

	/**
	 * 添加监听端口
	 *
	 * @param port
	 */
	public void addListen(int port, Session usersession) {
		try {
			// pasvBootstrop.childHandler(new ByteArrayDecoder());
			// pasvBootstrop.childHandler(new ByteArrayEncoder());
			pasvBootstrop.childHandler(new DataTransferHandler(usersession));
			//FIXME:未明白为什么通过以下方式添加处理 器会不成功。
			// pasvBootstrop.childHandler(new
			// ChannelInitializer<SocketChannel>() {
			// @Override
			// protected void initChannel(SocketChannel ch) throws Exception {
			// ChannelPipeline p = ch.pipeline();
			// p.addLast("framer", new DelimiterBasedFrameDecoder(256,
			// Delimiters.lineDelimiter()));
			// p.addLast("decoder", new ByteArrayDecoder());
			// p.addLast("encoder", new ByteArrayEncoder());
			// }
			// });

			ChannelFuture future = pasvBootstrop.bind(port);

			// usersession.setFuture(future);
			// ChannelPipeline pipeline = future.channel().pipeline();
			// pipeline.addLast("pasvhandler", new FtpPasvHandler(usersession));

			for (Map.Entry<String, ChannelHandler> entry : future.channel().pipeline()) {
				logger.debug(entry.getKey());
			}

			logger.info("pasv 端口：{} 绑定完成", port);
			future.sync();
		} catch (Exception e) {
			logger.error(e.toString(), e);
		}
	}

	public void delListen(Session usersession) {
		// ChannelFuture future = usersession.getFuture();
		// if (null != future) {
		// future.channel().close().addListener(ChannelFutureListener.CLOSE);
		// future.awaitUninterruptibly();
		// logger.info("关闭端口:{}", usersession.getUserid());
		// }

		try {
			usersession.getCtx().close().sync();
		} catch (InterruptedException e) {
			logger.error(e.toString(), e);
		}

	}

	public void writeandflush(Session usersession, byte[] o) {
		try {
			ChannelHandlerContext ctx = usersession.getCtx();
			if (null != ctx) {
				/**
				 * 使用了ByteArray编解码器，只需要写入字符烽组就可以。
				 */

				for (Map.Entry<String, ChannelHandler> entry : ctx.channel().pipeline()) {
					logger.debug(entry.getKey());
				}

				// ctx.channel().writeAndFlush(o).sync();
				ctx.channel().writeAndFlush(Unpooled.wrappedBuffer(o)).sync();
			}
		} catch (Exception e) {
			logger.error(e.toString(), e);
		}
	}

	public void writeandflush(Session usersession, FileRegion region) {
		try {
			ChannelHandlerContext ctx = usersession.getCtx();
			if (null != ctx) {
				
				for (Map.Entry<String, ChannelHandler> entry : ctx.channel().pipeline()) {
					logger.debug(entry.getKey());
				}
				//使用0拷贝方式发送文件数据
				ctx.channel().writeAndFlush(region).sync();
			}
		} catch (Exception e) {
			logger.error(e.toString(), e);
		}

	}

}
