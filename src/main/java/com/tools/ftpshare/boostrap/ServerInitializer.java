package com.tools.ftpshare.boostrap;

import com.tools.ftpshare.common.CmdCodeEnum;
import com.tools.ftpshare.handlers.FtpHandler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

/**
 * @author jpeng
 */
public class ServerInitializer extends ChannelInitializer<SocketChannel> {

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline p = ch.pipeline();
		p.addLast("framer", new DelimiterBasedFrameDecoder(256, Delimiters.lineDelimiter()));
		p.addLast("StringDecoder", new StringDecoder(CmdCodeEnum.ASCII));
		p.addLast("StringEncoder", new StringEncoder(CmdCodeEnum.ASCII));
		p.addLast("Serverhandler", new FtpHandler());

	}

}
