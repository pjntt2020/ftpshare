package com.tools.ftpshare.common;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class TransportType {

	private final char type;

    public TransportType(char type) {
        this.type = type;
    }

    public InputStreamReader getStreamReader(Socket socket) throws IOException {
        if (type == 'I') {
            return new InputStreamReader(socket.getInputStream());
        } else {
            return new InputStreamReader(socket.getInputStream(), "ASCII");
        }
    }

    public OutputStreamWriter getStreamWriter(Socket socket) throws IOException {
        if (type == 'I') {
            return new OutputStreamWriter(socket.getOutputStream());
        } else {
            return new OutputStreamWriter(socket.getOutputStream(), "ASCII");
        }
    }

}
