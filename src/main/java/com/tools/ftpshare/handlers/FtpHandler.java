package com.tools.ftpshare.handlers;

import com.tools.ftpshare.common.CmdCodeEnum;
import com.tools.ftpshare.common.CommandException;
import com.tools.ftpshare.common.Session;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.StringTokenizer;

/**
 * @author jpeng
 */
public class FtpHandler extends SimpleChannelInboundHandler<String> {
	private final Logger logger = LoggerFactory.getLogger(FtpHandler.class);
	/**
	 * 定义反射方式获取函数的变量类型
	 */

	private final Class[] commandHandlerParamTypes = {String.class, StringTokenizer.class, ChannelHandlerContext.class,Session.class};

	/**
	 * 通过反射将方法与FTP操作字进行关联。
	 */
	private final Class<FtpCmd> ftpcmd = FtpCmd.class;
	private Object ftpcmdInstance = null;
	private Session usersession;

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		if (cause instanceof IOException) {
			ctx.channel().close();
			return;
		}
		logger.error(cause.getMessage(), cause.getCause());
		if (ctx.channel().isOpen()) {
			ctx.channel().close();
		}
	}

	private void send(String code, String response, ChannelHandlerContext ctx) {
		logger.debug("Code: {}, Text: {}", code, response);
		String line = code + " " + response + "\r\n";
		byte[] data = line.getBytes(CmdCodeEnum.ASCII);
		ctx.writeAndFlush(Unpooled.wrappedBuffer(data));

	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		String version = "0.0.1";
		send(CmdCodeEnum.CODE_CONNECT_SUCCESS.getCode(),CmdCodeEnum.CODE_CONNECT_SUCCESS.getMsg().replace("{}", version), ctx);
		ftpcmdInstance = ftpcmd.newInstance();
		usersession = new Session();
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		logger.debug("main socket close");
		ftpcmdInstance = null;
		usersession = null;
		super.channelInactive(ctx);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String msg) {
		StringTokenizer st = new StringTokenizer(msg);
		String command = st.nextToken().toLowerCase();
		logger.debug("Line: {},Command: {}", msg,command);
		
		Object[] args = { msg, st, ctx, usersession};
		try {
			Method commandHandler = ftpcmd.getMethod("command_" + command, commandHandlerParamTypes);
			commandHandler.invoke(ftpcmdInstance, args);
		} catch (InvocationTargetException e) {
			CommandException ce = (CommandException) e.getTargetException();
			send(ce.getCode(), ce.getText(), ctx);
		} catch (NoSuchMethodException e) {
			logger.error(e.toString(),e);
			send(CmdCodeEnum.CODE_EXCEPTION_FAILED.getCode() , CmdCodeEnum.CODE_EXCEPTION_FAILED.getMsg().replace("{}", msg), ctx);
		} catch (Exception e) {
			logger.error(e.toString(),e);
		}
	}

}
