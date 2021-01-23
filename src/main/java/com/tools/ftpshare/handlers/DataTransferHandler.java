package com.tools.ftpshare.handlers;

import java.io.RandomAccessFile;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tools.ftpshare.common.Session;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * 
 * @author jpeng
 */
public class DataTransferHandler extends SimpleChannelInboundHandler<ByteBuf> {
	private final Logger logger = LoggerFactory.getLogger(DataTransferHandler.class);
	private final Session usesession;

	public DataTransferHandler(Session session) {
		this.usesession = session;

	}

	@Override
	protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf msg) throws Exception {
		logger.debug("{},{},Mode:{}", "channelRead0", msg.readableBytes(),usesession.getMode());
		RandomAccessFile raf = usesession.getRafile();
		if (null != raf) {
			long start = usesession.getOffset();
			raf.seek(start);
			raf.getChannel().write(msg.nioBuffer());
			usesession.setOffset(start + msg.readableBytes());
		}
	}


	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		usesession.setCtx(ctx);
		logger.debug("{},{},{},Mode:{}", ctx.channel().remoteAddress(), "socket connection", usesession.getUserid(),usesession.getMode());

		try {
			if (null != usesession.getFile()) {
				logger.debug("创建文件,{}");
			} else {
				logger.debug("不需创建文件,{}");
			}
		} catch (Exception e) {
			logger.error(e.toString(), e);
		}

		for (Map.Entry<String, ChannelHandler> entry : ctx.channel().pipeline()) {
			logger.debug(entry.getKey());
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		logger.debug("{},{},{},Mode:{}", ctx.channel().remoteAddress(), "完成数据传输操作，已关闭连接通道！", usesession.getUserid(),usesession.getMode());
		RandomAccessFile raf = usesession.getRafile();
		if (null != raf) {
			//使用锁传递状态变化
			synchronized (usesession) {
				raf.close();
				usesession.notify();
			}
			logger.debug("关闭文件");
		} else {
			logger.debug("不需关闭文件");
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.debug("{},{},Mode:{}", cause.getMessage(), cause,usesession.getMode());
		ctx.close();
	}

}
