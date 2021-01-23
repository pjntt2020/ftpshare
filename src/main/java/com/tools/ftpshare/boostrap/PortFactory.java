package com.tools.ftpshare.boostrap;

import com.tools.ftpshare.common.Session;
import com.tools.ftpshare.handlers.DataTransferHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;

public enum PortFactory {
	/**
	 * 单例
	 */
	INS;
    private Bootstrap portBootstrop = new Bootstrap()
            .option(ChannelOption.SO_BACKLOG, 1024)
			/**
			 * 设置端口重用
			 */
            .option(ChannelOption.SO_REUSEADDR,true)
            .group(new NioEventLoopGroup())
            .channel(NioSocketChannel.class);
	
    private final Logger logger = LoggerFactory.getLogger(PortFactory.class);
    
    
    public void addListen(int port, Session usersession,InetSocketAddress address) {
		try {
			portBootstrop.handler(new LoggingHandler(LogLevel.DEBUG)); 
			portBootstrop.handler(new DataTransferHandler(usersession)); 
			
			portBootstrop.remoteAddress(address);
			portBootstrop.localAddress(port);
			
			ChannelFuture future = portBootstrop.connect();

			for (Map.Entry<String, ChannelHandler> entry : future.channel().pipeline()) {
				logger.debug(entry.getKey());
			}

			logger.info("port 端口：{} 绑定完成", port);
			future.sync();
		} catch (Exception e) {
			logger.error(e.toString(), e);
		}
	}
    
    
    public void delListen(Session usersession) {
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
