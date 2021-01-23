package com.tools.ftpshare.handlers;

import com.tools.ftpshare.boostrap.FtpBoostrap;
import com.tools.ftpshare.boostrap.PasvFactory;
import com.tools.ftpshare.boostrap.PortFactory;
import com.tools.ftpshare.common.CmdCodeEnum;
import com.tools.ftpshare.common.CommandException;
import com.tools.ftpshare.common.Session;
import com.tools.ftpshare.common.TransportType;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.FileRegion;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.*;

public class FtpCmd {

    private final Logger logger = LoggerFactory.getLogger(FtpCmd.class);
    private String username;
    private String password;
    private TransportType transportType;
    private InetSocketAddress addressClient = null;
    private File renameFile;

    public String baseDir = System.getProperty("user.dir");
    public String currentDir = "/";

    public FtpCmd() {

    }

    private String resolvePath(String path) {
        if (path.charAt(0) != '/') {
            path = currentDir + "/" + path;
        }
        StringTokenizer pathSt = new StringTokenizer(path, "/");
        Stack<String> segments = new Stack<String>();
        while (pathSt.hasMoreTokens()) {
            String segment = pathSt.nextToken();
            if ("..".equals(segment)) {
                if (!segments.empty()) {
                    segments.pop();
                }
            } else if (".".equals(segment)) {
                // skip
            } else {
                segments.push(segment);
            }
        }

        StringBuilder pathBuf = new StringBuilder("/");
        Enumeration<String> segmentsEn = segments.elements();
        while (segmentsEn.hasMoreElements()) {
            pathBuf.append(segmentsEn.nextElement());
            if (segmentsEn.hasMoreElements()) {
                pathBuf.append("/");
            }
        }
        return pathBuf.toString();
    }

    private String listFile(File file) {
        Date date = new Date(file.lastModified());
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd hh:mm", new Locale("en", "US"));
        String dateStr = dateFormat.format(date);

        long size = file.length();
        String sizeStr = Long.toString(size);
        int sizePadLength = Math.max(8 - sizeStr.length(), 0);
        String sizeField = pad(sizePadLength) + sizeStr;

        if (file.isDirectory()) {
            return (String.format("drw-rw-rw- 1 ftp ftp     %s %s %s", sizeField, dateStr, file.getName()))
                    + CmdCodeEnum.EOL;
        } else {
            return (String.format("-rw-r--r-- 1 ftp ftp     %s %s %s", sizeField, dateStr, file.getName()))
                    + CmdCodeEnum.EOL;
        }
    }

    private String listFile2(File file) {
        Date date = new Date(file.lastModified());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String dateStr = dateFormat.format(date);
        if (file.isDirectory()) {
            return String.format("type=dir;modify=%s; %s", dateStr, file.getName()) + CmdCodeEnum.EOL;
        } else {
            return (String.format("type=file;modify=%s;size=%s; %s", dateStr, file.length(), file.getName()))
                    + CmdCodeEnum.EOL;
        }

    }

    private String pad(int length) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < length; i++) {
            buf.append(' ');
        }
        return buf.toString();
    }

    private String createNativePath(String ftpPath) {
        String path;
        if (ftpPath.charAt(0) == '/') {
            path = baseDir + ftpPath;
        } else {
            path = baseDir + currentDir +"/"+ ftpPath;
        }
        return path;
    }

    private InetSocketAddress parsePortArgs(String portArgs) {
        String[] strParts = portArgs.split(",");
        if (strParts.length != 6) {
            return null;
        }
        byte[] address = new byte[4];
        int[] parts = new int[6];
        for (int i = 0; i < 6; i++) {
            try {
                parts[i] = Integer.parseInt(strParts[i]);
            } catch (NumberFormatException e) {
                return null;
            }
            if (parts[i] < 0 || parts[i] > 255) {
                return null;
            }
        }
        for (int i = 0; i < 4; i++) {
            address[i] = (byte) parts[i];
        }
        int port = parts[4] << 8 | parts[5];
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByAddress(address);
        } catch (UnknownHostException e) {
            return null;
        }
        return new InetSocketAddress(inetAddress, port);
    }

    private void send(String code, String response, ChannelHandlerContext ctx) {
        logger.info("Code: {}, Text: {}", code, response);
        String line = code + " " + response + CmdCodeEnum.EOL;
        byte[] data = line.getBytes(CmdCodeEnum.ASCII);
        ctx.writeAndFlush(Unpooled.wrappedBuffer(data));
    }

    public void command_user(String line, StringTokenizer st, ChannelHandlerContext ctx, Session usersession)
            throws CommandException {
        username = st.nextToken();
        logger.info("Username: {},{}", username, usersession.getUserid());
        send(CmdCodeEnum.CODE_PASSWORD_NEEDED.getCode(),
                CmdCodeEnum.CODE_PASSWORD_NEEDED.getMsg().replace("{}", username), ctx);
    }

    public void command_pass(String line, StringTokenizer st, ChannelHandlerContext ctx, Session usersession)
            throws CommandException {
        if (username == null) {
            throw new CommandException(CmdCodeEnum.CODE_USERNAME_NEEDED.getCode(),
                    CmdCodeEnum.CODE_USERNAME_NEEDED.getMsg());
        }
        if (st.hasMoreTokens()) {
            password = st.nextToken();
        } else {
            password = "";
        }
        if (!"anonymous".equalsIgnoreCase(username)) {
            if (!("morf".equals(username) && "123".equals(password))) {
                throw new CommandException(CmdCodeEnum.CODE_NOT_LOGGED_IN.getCode(),
                        CmdCodeEnum.CODE_NOT_LOGGED_IN.getMsg());
            }
        }

        usersession.setLogin(true);

        send(CmdCodeEnum.CODE_LOGIN_SUCCESS.getCode(), CmdCodeEnum.CODE_LOGIN_SUCCESS.getMsg(), ctx);
    }

    public void command_syst(String line, StringTokenizer st, ChannelHandlerContext ctx, Session usersession)
            throws CommandException {
        usersession.checklogin();
        send(CmdCodeEnum.CODE_SYSTEM_TYPE.getCode(), CmdCodeEnum.CODE_SYSTEM_TYPE.getMsg(), ctx);
    }

    public void command_clnt(String line, StringTokenizer st, ChannelHandlerContext ctx, Session usersession)
            throws CommandException {
        usersession.checklogin();
        logger.debug("Client: {}", line);
        send(CmdCodeEnum.CODE_COMMAND_SUCCESS.getCode(),
                CmdCodeEnum.CODE_COMMAND_SUCCESS.getMsg().replace("{}", "CLNT"), ctx);
    }

    public void command_type(String line, StringTokenizer st, ChannelHandlerContext ctx, Session usersession)
            throws CommandException {
        usersession.checklogin();
        String arg = st.nextToken().toUpperCase();
        if (arg.length() != 1) {
            throw new CommandException(CmdCodeEnum.CODE_EXCEPTION_FAILED.getCode(),
                    "TYPE: invalid argument '" + arg + "'");
        }
        char code = arg.charAt(0);
        //FIXME：没搞明白A和I模式下数据的读取方式
        transportType = new TransportType(code);
        send(CmdCodeEnum.CODE_COMMAND_SUCCESS.getCode(), "Type set to " + code, ctx);
    }

    public void command_noop(String line, StringTokenizer st, ChannelHandlerContext ctx, Session usersession)
            throws CommandException {
        usersession.checklogin();
        send(CmdCodeEnum.CODE_COMMAND_SUCCESS.getCode(),
                CmdCodeEnum.CODE_COMMAND_SUCCESS.getMsg().replace("{}", "NOOP"), ctx);
    }

    public void command_port(String line, StringTokenizer st, ChannelHandlerContext ctx, Session usersession)
            throws CommandException {
        usersession.checklogin();
        usersession.setMode(CmdCodeEnum.PORT);
        addressClient = parsePortArgs(st.nextToken());
        if (null == addressClient) {
            throw new CommandException(CmdCodeEnum.CODE_SYNTAX_ERROR.getCode(),
                    "Syntax error in parameters or arguments");
        }
        logger.info("Client host: {}, port: {}", addressClient.getAddress(), addressClient.getPort());

        PortFactory.INS.addListen(20, usersession, addressClient);

        send(CmdCodeEnum.CODE_COMMAND_SUCCESS.getCode(),
                CmdCodeEnum.CODE_COMMAND_SUCCESS.getMsg().replace("{}", "PORT"), ctx);
    }

    public void command_cdup(String line, StringTokenizer st, ChannelHandlerContext ctx, Session usersession)
            throws CommandException {
        usersession.checklogin();
        currentDir = currentDir.substring(0, currentDir.lastIndexOf("/"));
        if (currentDir.length() == 0) {
            currentDir = "/";
        }
        send(CmdCodeEnum.CODE_COMMAND_SUCCESS.getCode(),
                CmdCodeEnum.CODE_COMMAND_SUCCESS.getMsg().replace("{}", "CDUP"), ctx);
    }

    public void command_list(String line, StringTokenizer st, ChannelHandlerContext ctx, Session usersession)
            throws CommandException {
        usersession.checklogin();
        String path;
        if (st.hasMoreTokens()) {
            path = st.nextToken();
        } else {
            path = currentDir;
        }

        logger.info("List Path: {}", path);
        path = createNativePath(path);

        try {
            File dir = new File(path);
            String[] fileNames = dir.list();
            int numFiles = fileNames != null ? fileNames.length : 0;

            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < numFiles; i++) {
                String fileName = fileNames[i];
                File file = new File(dir, fileName);
                buf.append(listFile(file));
            }
            buf.append(CmdCodeEnum.EOL);

            if (CmdCodeEnum.PASV.equalsIgnoreCase(usersession.getMode())) {
                PasvFactory.INS.writeandflush(usersession, buf.toString().getBytes(CmdCodeEnum.UTF8));
                PasvFactory.INS.delListen(usersession);
            } else {
                PortFactory.INS.writeandflush(usersession, buf.toString().getBytes(CmdCodeEnum.UTF8));
                PortFactory.INS.delListen(usersession);
            }
            send(CmdCodeEnum.CODE_TRANSFER_COMPLETE.getCode(), CmdCodeEnum.CODE_TRANSFER_COMPLETE.getMsg(), ctx);
        } catch (Exception exx) {
            throw new CommandException(CmdCodeEnum.CODE_FILE_LIST_ERROR.getCode(),
                    CmdCodeEnum.CODE_FILE_LIST_ERROR.getMsg());
        }
        
    }

    public void command_pwd(String line, StringTokenizer st, ChannelHandlerContext ctx, Session usersession)
            throws CommandException {
        usersession.checklogin();
        send(CmdCodeEnum.CODE_CURRENT_DIR.getCode(), CmdCodeEnum.CODE_CURRENT_DIR.getMsg().replace("{}", currentDir),
                ctx);

    }

    public void command_cwd(String line, StringTokenizer st, ChannelHandlerContext ctx, Session usersession)
            throws CommandException {
        usersession.checklogin();
        String newDir = currentDir;
        if (st.hasMoreElements()) {
            newDir = st.nextToken();
        }

        if (newDir.length() == 0) {
            newDir = "/";
        }

        newDir = resolvePath(newDir.replace("\\", "/"));

        File file = new File(createNativePath(newDir));
        if (!file.exists()) {
            throw new CommandException(CmdCodeEnum.CODE_CWD_FAIL.getCode(), newDir + ": no such directory");
        }
        if (!file.isDirectory()) {
            throw new CommandException(CmdCodeEnum.CODE_CWD_FAIL.getCode(), newDir + ": not a directory");
        }

        currentDir = newDir;
        logger.info("New current dir: {}", currentDir);
        send(CmdCodeEnum.CODE_CWD_SUCCESS.getCode(), CmdCodeEnum.CODE_CWD_SUCCESS.getMsg().replace("{}", currentDir),
                ctx);
    }

    public void command_retr(String line, StringTokenizer st, ChannelHandlerContext ctx, Session usersession)
            throws CommandException {
        usersession.checklogin();
        String path = null;
        try {
            path = line.substring(5);
        } catch (Exception e) {
            throw new NoSuchElementException(e.getMessage());
        }
        
        path = createNativePath(path);
        logger.info("Send try file: {}", path);

        try {
            File file = new File(path);
            if (!file.isFile()) {
                throw new CommandException(CmdCodeEnum.CODE_NOT_FOUND.getCode(), "Not a plain file.");
            }
            send(CmdCodeEnum.CODE_READ_DIR.getCode(), "Opening data connection.Mode:" + usersession.getMode(), ctx);

            //FIXME：方式都一样只是传送方式不一样
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            long fileLength = raf.length();
            final FileRegion region = new DefaultFileRegion(raf.getChannel(), 0, fileLength);

            if (CmdCodeEnum.PASV.equalsIgnoreCase(usersession.getMode())) {
                PasvFactory.INS.writeandflush(usersession, region);
            } else {
                PortFactory.INS.writeandflush(usersession, region);
            }

            raf.close();
            usersession.getCtx().close();
            send(CmdCodeEnum.CODE_TRANSFER_COMPLETE.getCode(), "Transfer complete.", ctx);
        } catch (FileNotFoundException e) {
            throw new CommandException(CmdCodeEnum.CODE_NOT_FOUND.getCode(), "No such file.");
        } catch (IOException e) {
            throw new CommandException(CmdCodeEnum.CODE_NOT_FOUND.getCode(), "IO exception");
        }
    }

    public void command_mkd(String line, StringTokenizer st, ChannelHandlerContext ctx, Session usersession)
            throws CommandException {
        usersession.checklogin();
        String arg = st.nextToken();
        String dirPath = resolvePath(arg);
        File dir = new File(createNativePath(dirPath));
        if (dir.exists()) {
            throw new CommandException(CmdCodeEnum.CODE_NOT_FOUND.getCode(), arg + ": file exists");
        }
        if (!dir.mkdir()) {
            throw new CommandException(CmdCodeEnum.CODE_EXCEPTION_FAILED.getCode(),
                    arg + ": directory could not be created");
        }
        send(CmdCodeEnum.CODE_CURRENT_DIR.getCode(), "\"" + dirPath + "\" directory created", ctx);
    }

    public void command_stor(String line, StringTokenizer st, ChannelHandlerContext ctx, Session usersession)
            throws CommandException {
        usersession.checklogin();
        String path = null;
        try {
            path = line.substring(5);
        } catch (Exception e) {
            throw new NoSuchElementException(e.getMessage());
        }
        path = createNativePath(path);
        logger.info("Upload file to: {}", path);

        /**
         * 主动与被动方式处理数据一样，不需要分开
         */
        try {
            File file = new File(path);

            usersession.setFile(file);
            usersession.setOffset(0); // 把指针移到启始位置。

            send(CmdCodeEnum.CODE_READ_DIR.getCode(), " Opening data connection.Mode:Pasv", ctx);
            // 使用锁同步，不使用循环方式等待。
            synchronized (usersession) {
                try {
                    usersession.wait(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            usersession.closefile();
            send(CmdCodeEnum.CODE_TRANSFER_COMPLETE.getCode(), "Transfer complete.", ctx);

        } catch (Exception ex) {
            throw new CommandException(CmdCodeEnum.CODE_NOT_FOUND.getCode(), "IO exception");
        }
    }

    public void command_dele(String line, StringTokenizer st, ChannelHandlerContext ctx, Session usersession)
            throws CommandException {
        usersession.checklogin();
        String arg = st.nextToken();
        String filePath = resolvePath(arg);
        File file = new File(createNativePath(filePath));
        if (!file.exists()) {
            throw new CommandException(CmdCodeEnum.CODE_NOT_FOUND.getCode(), arg + ": file does not exist");
        }
        if (!file.delete()) {
            throw new CommandException(CmdCodeEnum.CODE_NOT_FOUND.getCode(), arg + ": could not delete file");
        }
        send(CmdCodeEnum.CODE_ACTION_OK.getCode(), CmdCodeEnum.CODE_ACTION_OK.getMsg().replace("{}", "DELE"), ctx);
    }

    public void command_rnfr(String line, StringTokenizer st, ChannelHandlerContext ctx, Session usersession)
            throws CommandException {
        usersession.checklogin();
        String arg = st.nextToken();
        String filePath = resolvePath(arg);
        renameFile = new File(createNativePath(filePath));
        if (!renameFile.exists()) {
            throw new CommandException(CmdCodeEnum.CODE_NOT_FOUND.getCode(), arg + ": file does not exist");
        }
        send(CmdCodeEnum.CODE_ACTION_PENDING.getCode(), "Pending file", ctx);
    }

    public void command_rnto(String line, StringTokenizer st, ChannelHandlerContext ctx, Session usersession)
            throws CommandException {
        usersession.checklogin();
        String arg = st.nextToken();
        String filePath = resolvePath(arg);
        File newFile = new File(createNativePath(filePath));
        if (renameFile.renameTo(newFile)) {
            renameFile = null;
            send(CmdCodeEnum.CODE_ACTION_OK.getCode(), CmdCodeEnum.CODE_ACTION_OK.getMsg().replace("{}", "RNTO"), ctx);
        } else {
            throw new CommandException(CmdCodeEnum.CODE_NOT_FOUND.getCode(), arg + ": file does not exist");
        }
    }

    public void command_rmd(String line, StringTokenizer st, ChannelHandlerContext ctx, Session usersession)
            throws CommandException {
        usersession.checklogin();
        String arg = st.nextToken();
        String dirPath = resolvePath(arg);
        File dir = new File(createNativePath(dirPath));
        if (!dir.exists()) {
            throw new CommandException(CmdCodeEnum.CODE_NOT_FOUND.getCode(), arg + ": directory does not exist");
        }
        if (!dir.isDirectory()) {
            throw new CommandException(CmdCodeEnum.CODE_NOT_FOUND.getCode(), arg + ": not a directory");
        }
        if (!dir.delete()) {
            throw new CommandException(CmdCodeEnum.CODE_NOT_FOUND.getCode(), arg + ": could not remove directory");
        }
        send(CmdCodeEnum.CODE_ACTION_OK.getCode(), CmdCodeEnum.CODE_ACTION_OK.getMsg().replace("{}", "RMD"), ctx);
    }

    public void command_quit(String line, StringTokenizer st, ChannelHandlerContext ctx, Session usersession)
            throws CommandException {
        usersession = null;
        username = null;
        password = null;
        send(CmdCodeEnum.CODE_QUIT.getCode(), CmdCodeEnum.CODE_QUIT.getMsg(), ctx);
        ctx.channel().close();
    }

    public void command_size(String line, StringTokenizer st, ChannelHandlerContext ctx, Session usersession)
            throws CommandException {
        usersession.checklogin();
        String path = null;
        try {
            path = line.substring(5);
        } catch (Exception e) {
            throw new NoSuchElementException(e.getMessage());
        }
        if ("null".equalsIgnoreCase(path)) {
            send(CmdCodeEnum.CODE_NOT_FOUND.getCode(), "No such file or directory.", ctx);
            return;
        }
        path = createNativePath(path);
        File file = new File(path);
        if (!file.exists() || file.isDirectory()) {
            send(CmdCodeEnum.CODE_NOT_FOUND.getCode(), "No such file or directory.", ctx);
            return;

        }

        String size = String.valueOf(file.length());
        send(CmdCodeEnum.CODE_SIZE_ACCEPT.getCode(), CmdCodeEnum.CODE_SIZE_ACCEPT.getMsg().replace("{}", size), ctx);

    }

    public void command_feat(String line, StringTokenizer st, ChannelHandlerContext ctx, Session usersession)
            throws CommandException {
        send(CmdCodeEnum.CODE_FEATURE_LIST_BEGIN.getCode(), "", ctx);
        send("", "SIZE", ctx);
        send("", "CLNT", ctx);
        send("", "UTF8", ctx);
        send("", "MLST type*;size*;modify*;", ctx);
        send("", "MLSD", ctx);
        // send("", "MDTM", ctx);
        send("", "PASV", ctx);
        // send("", "MFMT", ctx);
        // send("", "REST STREAM", ctx);

        send(CmdCodeEnum.CODE_FEATURE_LIST_END.getCode(), CmdCodeEnum.CODE_FEATURE_LIST_END.getMsg(), ctx);
    }

    public void command_pasv(String line, StringTokenizer st, ChannelHandlerContext ctx, Session usersession)
            throws CommandException, UnknownHostException {
        if (null == FtpBoostrap.LocalIP) {
            send(CmdCodeEnum.CODE_PASV_FAIL.getCode(), CmdCodeEnum.CODE_PASV_FAIL.getMsg(), ctx);
            return;
        }

        usersession.setMode(CmdCodeEnum.PASV);
        int param1 = (int) (Math.random() * 49 + 185);
        int param2 = (int) (Math.random() * 100);
        String localhost = FtpBoostrap.LocalIP.replace(".", ",").concat(",").concat(param1 + "").concat(",")
                .concat(param2 + "");
        int localport = param1 * 256 + param2;
        PasvFactory.INS.addListen(localport, usersession);

        send(CmdCodeEnum.CODE_PASV_ENABLE.getCode(), CmdCodeEnum.CODE_PASV_ENABLE.getMsg().replace("{}", localhost),
                ctx);
    }

    public void command_rest(String line, StringTokenizer st, ChannelHandlerContext ctx, Session usersession)
            throws CommandException {
        send("501", "Syntax error.", ctx);
    }

    public void command_opts(String line, StringTokenizer st, ChannelHandlerContext ctx, Session usersession)
            throws CommandException {
        usersession.checklogin();
        String charset = "";
        String onoff = "";
        logger.debug("{}", st.countTokens());
        if (st.hasMoreTokens()) {
            charset = st.nextToken();
        }

        if (st.hasMoreTokens()) {
            onoff = st.nextToken();
        }

        if ("utf8".equalsIgnoreCase(charset) && "on".equalsIgnoreCase(onoff)) {
            ctx.pipeline().replace("StringDecoder", "StringDecoder", new StringDecoder(CmdCodeEnum.UTF8));
            ctx.pipeline().replace("StringEncoder", "StringEncoder", new StringEncoder(CmdCodeEnum.UTF8));
        }

        send(CmdCodeEnum.CODE_OPTS_SUCCESS.getCode(),
                CmdCodeEnum.CODE_OPTS_SUCCESS.getMsg().replace("{}", charset + " " + onoff), ctx);

    }

    public void command_mlst(String line, StringTokenizer st, ChannelHandlerContext ctx, Session usersession)
            throws CommandException {
        usersession.checklogin();
        String filepath;
        if (st.hasMoreTokens()) {
            filepath = st.nextToken();
        } else {
            filepath = currentDir;
        }

        logger.info("Path: {}", filepath);
        filepath = createNativePath(filepath);

        File file = new File(filepath);
        if (file.isFile()) {
            Date date = new Date(file.lastModified());
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            String dateStr = dateFormat.format(date);
            String content = String.format(" type=file;modify=%s;size=%s; %s\r\n", dateStr,
                    Long.toString(file.length()), filepath);

            send(CmdCodeEnum.CODE_MLST_BEGIN.getCode(), filepath, ctx);
            send("", content, ctx);
            send(CmdCodeEnum.CODE_MLST_END.getCode(), CmdCodeEnum.CODE_MLST_END.getMsg(), ctx);
        } else if (file.isDirectory()) {
            String fileNames[] = file.list();

            int numFiles = fileNames != null ? fileNames.length : 0;
            send(CmdCodeEnum.CODE_MLST_BEGIN.getCode(), filepath, ctx);

            for (int i = 0; i < numFiles; i++) {
                String fileName = fileNames[i];
                String content = listFile2(new File(file, fileName));
                send("", content, ctx);
            }

            send(CmdCodeEnum.CODE_MLST_END.getCode(), CmdCodeEnum.CODE_MLST_END.getMsg(), ctx);

        } else {
            send(CmdCodeEnum.CODE_MLST_FAIL.getCode(), CmdCodeEnum.CODE_MLST_FAIL.getMsg(), ctx);
        }

    }

    public void command_mlsd(String line, StringTokenizer st, ChannelHandlerContext ctx, Session usersession)
            throws CommandException {
        usersession.checklogin();
        String path;
        if (st.hasMoreTokens()) {
            path = st.nextToken();
        } else {
            path = currentDir;
        }

        logger.info("Path: {}", path);
        path = createNativePath(path);

        try {
            File dir = new File(path);
            String[] fileNames = dir.list();
            int numFiles = fileNames != null ? fileNames.length : 0;

            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < numFiles; i++) {
                String fileName = fileNames[i];
                File file = new File(dir, fileName);
                buf.append(listFile2(file));
            }

            if (CmdCodeEnum.PASV.equalsIgnoreCase(usersession.getMode())) {
                PasvFactory.INS.writeandflush(usersession, buf.toString().getBytes(CmdCodeEnum.UTF8));
                PasvFactory.INS.delListen(usersession);
            } else {
                PortFactory.INS.writeandflush(usersession, buf.toString().getBytes(CmdCodeEnum.UTF8));
                PortFactory.INS.delListen(usersession);
            }
            send(CmdCodeEnum.CODE_TRANSFER_COMPLETE.getCode(), CmdCodeEnum.CODE_TRANSFER_COMPLETE.getMsg(), ctx);
        } catch (Exception ex2) {
            logger.error(ex2.toString(), ex2);
        }
        
    }

}
