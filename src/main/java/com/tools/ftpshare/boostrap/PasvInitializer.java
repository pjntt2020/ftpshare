package com.tools.ftpshare.boostrap;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;

/**
 * @author jpeng
 */
public class PasvInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();
        p.addLast("framer", new DelimiterBasedFrameDecoder(256, Delimiters.lineDelimiter()));
        p.addLast("decoder", new ByteArrayDecoder());
        p.addLast("encoder", new ByteArrayEncoder());
    }
}
