package com.tools.ftpshare.common;

public class CommandException extends RuntimeException {
	private static final long serialVersionUID = -603611298981511389L;

	private final String code;
	private final String text;

	public CommandException(String code, String text) {
		super(code + " " + text);
		this.code = code;
		this.text = text;
	}

	public String getCode() {
		return code;
	}

	public String getText() {
		return text;
	}

	@Override
	public String getMessage() {
		return code + " " + text;
	}

}
