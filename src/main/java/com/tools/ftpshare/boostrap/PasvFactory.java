package com.tools.ftpshare.boostrap;

import com.tools.ftpshare.common.Session;
import com.tools.ftpshare.handlers.FtpPasvHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * PASV工厂
 *
 * @author jpeng
 */
public enum PasvFactory {
    /**
     * 单例
     */
    INS;
    private ServerBootstrap pasvBootstrop = new ServerBootstrap()
            .option(ChannelOption.SO_BACKLOG, 1024)
//            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .group(new NioEventLoopGroup(), new NioEventLoopGroup())
            .channel(NioServerSocketChannel.class)
            .handler(new LoggingHandler(LogLevel.DEBUG));
//            .childHandler(new PasvInitializer());

    private final Logger logger = LoggerFactory.getLogger(PasvFactory.class);


    /**
     * 添加见监听端口
     *
     * @param port
     */
    public void addListen(int port, Session usersession) {
        try {

            pasvBootstrop.childHandler(new FtpPasvHandler(usersession));
            ChannelFuture future = pasvBootstrop.bind(port);

            usersession.setFuture(future);
//            ChannelPipeline pipeline = future.channel().pipeline();
//            pipeline.addLast("pasvhandler", new FtpPasvHandler(usersession));




            for (Map.Entry<String, ChannelHandler> entry : future.channel().pipeline()) {
                logger.debug(entry.getKey());
            }

            future.sync();
            logger.info("pasv 端口：{} 绑定完成", port);
        } catch (Exception e) {
            logger.error(e.toString(), e);
        }
    }

    public void delListen(Session usersession) {
//        ChannelFuture future = usersession.getFuture();
//        if (null != future) {
//            future.channel().close().addListener(ChannelFutureListener.CLOSE);
//            future.awaitUninterruptibly();
//            logger.info("关闭端口:{}", usersession.getUserid());
//        }

        try {
            usersession.getCtx().close().sync();
        } catch (InterruptedException e) {
           logger.error(e.toString(),e);
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

//                ctx.channel().writeAndFlush("ssssssssss".getBytes()).sync();
                ctx.channel().writeAndFlush(Unpooled.wrappedBuffer(o)).sync();
            }
        } catch (Exception e) {
            logger.error(e.toString(), e);
        }
    }


}
