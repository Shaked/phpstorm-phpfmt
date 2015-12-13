package com.phpfmt.fmt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Scanner;

/**
 * Created by Shaked on 12/13/15.
 */
public class Util {
    public static String streamToString(InputStream is) {
        Scanner scanner = new Scanner(is);
        String s = "";
        while (scanner.hasNextLine()) {
            String line = String.format("%s\n", scanner.nextLine());
            s += line;
        }
        return s;
    }

}
