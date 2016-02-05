package com.phpfmt.fmt;

import com.intellij.AppTopics;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.notification.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class Component implements ApplicationComponent {

    private static final String COMPONENT_NAME = "Save Actions";
    private final Settings settings = ServiceManager.getService(Settings.class);

    public void initComponent() {
        install();
        toEventLog(settings.isDebug(), "initComponent", "settings.isAutoUpdatePhar? " + settings.isAutoUpdatePhar());
        if (settings.isAutoUpdatePhar()) {
            updatePhar();
        }
        final MessageBus bus = ApplicationManager.getApplication().getMessageBus();
        final MessageBusConnection connection = bus.connect();
        connection.subscribe(AppTopics.FILE_DOCUMENT_SYNC, new FileListener());
    }

    private void updatePhar() {
        List<String> list = new ArrayList<>();
        list.add(settings.getPhpExecutable());
        if (!settings.isDebug()) {
            list.add("-ddisplay_errors=stderr");
        }
        list.add(settings.getPharPath());
        list.add("--selfupdate");
        toEventLog(settings.isDebug(), "updatePhar", "LIST: " + list.toString());
        ProcessBuilder pb = new ProcessBuilder(list);
        Process process;
        try {
            process = pb.start();
            InputStream stdout = process.getInputStream();
            InputStream stderr = process.getErrorStream();


            ByteArrayOutputStream errous = new ByteArrayOutputStream();
            StreamGobbler errorGobbler = new StreamGobbler(stderr, "ERROR", errous, Charset.defaultCharset());
            ByteArrayOutputStream ous = new ByteArrayOutputStream();
            // any output?
            StreamGobbler outputGobbler = new StreamGobbler(stdout, "OUTPUT", ous, Charset.defaultCharset());

            errorGobbler.start();
            outputGobbler.start();

            int exitStatus = -1;
            try {
                exitStatus = process.waitFor();
            } catch (InterruptedException e) {
                toEventLog(settings.isDebug(), "updatePhar", "waiting for process failed: " + e.getMessage());
            }
            String retOutput = ous.toString();


            toEventLog(settings.isDebug(), "updatePhar", "retOutput: " + retOutput);
            if (0 != exitStatus) {
                String err = errous.toString();
                toEventLog(settings.isDebug(), "updatePhar", "stdErr: " + err);
            }
        } catch (IOException e) {
            notify("Update fmt.phar", "Could not automatically update fmt.phar. For more details, use `debug mode`");
            toEventLog(settings.isDebug(), "Update fmt.phar", "error start process: " + e.getMessage() + ": list: " + list.toString() + ": settings: " + settings.toString());
        }
    }

    private String path() {
        URL url1 = Component.class.getResource("/bin/fmt.phar");
        String ur = url1.toString();
        ur = ur.substring(9);
        String truepath[] = ur.split("phpstorm-phpfmt.jar!");
        truepath[0] = truepath[0].replaceAll("%20", " ");
        String x = truepath[0];
        toEventLog(settings.isDebug(), "path    ", "Path is: " + x + " UR is: " + ur);
        return x;
    }

    final static NotificationGroup ng = new NotificationGroup("phpfmt", NotificationDisplayType.NONE, true);
    public static void notify(String title, String msg) {
        Notification notification =  ng.createNotification("[" + title + "]" +msg, NotificationType.INFORMATION);
        Notifications.Bus.notify(notification);
        notification.hideBalloon();
    }

    public static void toEventLog(boolean isDebug, String title, String msg) {
        if (isDebug) {
            FormatterAction.LOGGER.debug(title + ": " + msg);
            notify(title, msg);
        }
    }

    private void install() {
        String version = PluginManager.getPlugin(PluginId.getId("phpfmt")).getVersion();
        toEventLog(settings.isDebug(), "install", this.getClass().toString() + ": version: " + version + ": isInstalled:" + settings.isInstalled(version) + " getPath: " + settings.getPharPath());
        if (!settings.isInstalled(version)) {
            String pharFile = path() + "/fmt.phar";
            File phar = new File(pharFile);
            if (phar != null) {
                phar.delete();
            }
            try {
                InputStream ddlStream = Component.class.getClassLoader().getResourceAsStream("/bin/fmt.phar");
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(pharFile);
                    byte[] buf = new byte[2048];
                    int r = ddlStream.read(buf);
                    while (r != -1) {
                        fos.write(buf, 0, r);
                        r = ddlStream.read(buf);
                    }

                } finally {
                    if (fos != null) {
                        fos.close();
                    }
                }
                String pharPath = new File(pharFile).getAbsolutePath();

                toEventLog(settings.isDebug(), "install", this.getClass().toString() + ": pharPath:" + pharPath);
                settings.setPharPath(pharPath);
                settings.setVersion(version);
            } catch (Exception e) {
                toEventLog(settings.isDebug(), "install-exception", this.getClass().toString() + ": exception export resource" + e.getMessage());
            }
        }
    }

    public String ExportResource(String resourceName, String saveName) throws Exception {
        InputStream stream = null;
        OutputStream resStreamOut = null;
        String jarFolder;
        String outName;
        try {
            stream = Component.class.getResourceAsStream(resourceName);//note that each / is a directory down in the "jar tree" been the jar the root of the tree
            if (stream == null) {
                throw new Exception("Cannot get resource \"" + resourceName + "\" from Jar file.");
            }

            int readBytes;
            byte[] buffer = new byte[4096];
            String jarPath = Component.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            jarFolder = new File(jarPath).getParentFile().getPath().replace('\\', '/');
            outName = jarFolder + saveName;
            resStreamOut = new FileOutputStream(outName);
            FormatterAction.LOGGER.debug(Component.class.toString() + " jarPath: " + jarPath + " :: jarFolder: " + jarFolder + " outName: " + outName);
            while ((readBytes = stream.read(buffer)) > 0) {
                resStreamOut.write(buffer, 0, readBytes);
            }
        } catch (Exception ex) {
            throw ex;
        } finally {
            stream.close();
            resStreamOut.close();
        }

        FormatterAction.LOGGER.debug(Component.class.toString() + " outName: " + outName);
        return outName;
    }

    public void disposeComponent() {
    }

    @NotNull
    public String getComponentName() {
        return COMPONENT_NAME;
    }

}
