package com.phpfmt.fmt;

import java.io.*;
import java.nio.charset.Charset;

/**
 * Created by klein on 05/01/16.
 */
public class StreamGobbler extends Thread {
    InputStream is;
    String type;
    OutputStream os;
    Charset myCharset;
    boolean isDebug;

    StreamGobbler(InputStream is, String type, OutputStream redirect, Charset myCharset) {
        this(is, type, redirect, myCharset, false);
    }

    StreamGobbler(InputStream is, String type, OutputStream redirect, Charset myCharset, boolean isDebug) {
        this.is = is;
        this.type = type;
        this.os = redirect;
        this.myCharset = myCharset;
        this.isDebug = isDebug;
    }

    public void run() {
        try {
            byte[] buffer = new byte[1024];

            InputStream reader = new BufferedInputStream(is);
            OutputStream writer = new BufferedOutputStream(os);
            int read;
            while ((read = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, read);
            }

            writer.flush();
            writer.close();
            Component.toEventLog(isDebug, "StreamGobbler", "type: " + type);
        } catch (IOException ioe) {
            Component.toEventLog(isDebug, "StreamGobbler", "type: " + type + " @@ ioe" + ioe.getMessage());
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ioe.printStackTrace(pw);
            Component.toEventLog(isDebug, "StreamGobbler", "type: " + type + "stack trace: " + sw.toString());
        }
    }
}
