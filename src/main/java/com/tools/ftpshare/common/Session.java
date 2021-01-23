package com.tools.ftpshare.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.apache.commons.lang3.RandomStringUtils;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;

/**
 * 登录状态临时保存
 *
 * @author jpeng
 */
public class Session {

	/**
	 * 用户标识，每次链接产生一个
	 */
	private String userid;

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
	private Boolean login = false;
	/**
	 * 文件
	 */
	private File file;
	
	private RandomAccessFile raf;

	/**
	 * 文件偏移量
	 */
	private long offset;

//	private boolean transmission;

	public Session() {
		userid = RandomStringUtils.randomAlphanumeric(64);
	}
	

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

	public long getOffset() {
		return offset;
	}

	public void setOffset(long l) {
		this.offset = l;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
		
		try {
			raf = new RandomAccessFile(this.file, "rw");
		} catch (FileNotFoundException e) {
			this.file = null;
			throw new RuntimeException("创建文件失败");
		}
	}
	
	public void closefile(){
		if (null != raf) {
			try {
				raf.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		this.file = null;
	}
	
	public RandomAccessFile getRafile() {
		return this.raf;
	}


}
