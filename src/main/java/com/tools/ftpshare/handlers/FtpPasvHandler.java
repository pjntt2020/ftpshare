package com.tools.ftpshare.handlers;

import com.tools.ftpshare.common.Session;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 使用了ByteArray编解码器，返回类型是字节数组类型。
 * @author jpeng
 */
public class FtpPasvHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private final Logger logger = LoggerFactory.getLogger(FtpPasvHandler.class);
    private final Session usesession;

    public FtpPasvHandler(Session session){
        this.usesession = session;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf msg) throws Exception {
        logger.debug("{}",msg);
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        usesession.setCtx(ctx);
        logger.debug("{},{},{}",ctx.channel().remoteAddress(),"pasv socket connection",usesession.getUserid());

        for (Map.Entry<String, ChannelHandler> entry : ctx.channel().pipeline()) {
            logger.debug(entry.getKey());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("{},{},{}",ctx.channel().remoteAddress(),"pasv socket close",usesession.getUserid());
        super.channelInactive(ctx);
    }

}
