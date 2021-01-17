package com.tools.ftpshare.common;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import org.apache.commons.lang3.RandomStringUtils;


/**
 * 登录状态临时保存
 *
 * @author jpeng
 */
public class Session {
    /**
     * 用户标识，每次链接产生一个
     */
    private final String userid = RandomStringUtils.randomAlphanumeric(64);

    /**
     * 当前链接
     */
    private ChannelFuture future;

    /**
     * 当前使用链接模式
     */
    private String mode;

    /**
     * 用户登录状态
     */
    private boolean login = false;

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public void setCtx(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    private ChannelHandlerContext ctx;

    public boolean isLogin() {
        return login;
    }

    public void setLogin(boolean login) {
        this.login = login;
    }

    public String getUserid() {
        return userid;
    }

    public ChannelFuture getFuture() {
        return future;
    }

    public void setFuture(ChannelFuture future) {
        this.future = future;
    }


    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }


    public void checklogin() {
        if (!login) {
            throw new CommandException(CmdCodeEnum.CODE_NOT_LOGGED_IN.getCode(),
                    CmdCodeEnum.CODE_NOT_LOGGED_IN.getMsg());
        }
    }

}
