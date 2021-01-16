package com.tools.ftpshare.common;

import java.nio.charset.Charset;

/**
 * @author jpeng
 */

public enum CmdCodeEnum {
	CODE_CONNECT_SUCCESS("220","FTP server ({}) ready."),
	CODE_NOT_LOGGED_IN("530","Login incorrect."),
	CODE_USERNAME_NEEDED("503","Login with username first."),
	CODE_PASSWORD_NEEDED("331","Password required for {}."),
	CODE_EXCEPTION_FAILED("500","Command '{}' Execution failed"),
	CODE_LOGIN_SUCCESS("230","Authorization has been successfully."),
	CODE_SYSTEM_TYPE("215", "UNIX emulated by File_Zilla"),
	CODE_COMMAND_SUCCESS("200","{} command successful."),
	CODE_SYNTAX_ERROR("501","Syntax error."),
	CODE_READ_DIR("150","Opening data channel for directory list."),
	CODE_TRANSFER_COMPLETE("226","Transfer OK"),
	CODE_CONNECTION_ERROR("425","Cannot open data connection."),
	CODE_FILE_LIST_ERROR("551","File listing failed."),
	CODE_CURRENT_DIR("257","\"{}\" is current directory."),
	CODE_NOT_FOUND("550","{}: No such directory."),
	CODE_QUIT("200","Goodbye..."),
	CODE_ACTION_PENDING("350","Requested file action pending further information."),
	CODE_ACTION_OK("250","{} Command okay."),
	CODE_SIZE_ACCEPT("213","{}"),
	CODE_FEATURE_LIST_BEGIN("211-Features:","{}"),
	CODE_FEATURE_LIST_END("211","End"),
	
	CODE_CWD_SUCCESS("250","CWD successful. \"{}\" is current directory."),
	CODE_CWD_FAIL("550","\"{}\" No such directory."),
	
	CODE_PASV_FAIL("425","Cannot open data connection."),
	CODE_PASV_ENABLE("227",""),

	
	CODE_MLST_BEGIN("250-Listing","{}"),
	CODE_MLST_END("250","End"),
	CODE_MLST_FAIL("550","File or directory not found."),
	
	
	CODE_OPTS_SUCCESS("200","OPTS {} command success")
	
	;
	
	public static final Charset ASCII = Charset.forName("ASCII");
	public static final Charset UTF8 = Charset.forName("UTF-8");

	private String code;
	private String msg;

	CmdCodeEnum(String code, String msg) {
		this.code = code;
		this.msg = msg;
	}

	public String getCode() {
		return code;
	}

	public String getMsg() {
		return msg;
	}

	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(code);
		builder.append(" ");
		builder.append(msg);
		return builder.toString();
	}
}
