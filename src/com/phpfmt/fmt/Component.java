package com.phpfmt.fmt;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class Component implements ApplicationComponent {
    public static final Logger LOGGER = Logger.getInstance(FormatterAction.class);

    static {
        LOGGER.setLevel(Level.DEBUG);
    }
    private static final String COMPONENT_NAME = "Save Actions";
    private final Settings settings = ServiceManager.getService(Settings.class);

    public void initComponent() {
        LOGGER.debug("phpfmt welcome!", String.format("Debug mode is: %s", settings.isDebug()? "On": "Off"));
        toEventLog(settings.isDebug(), "phpfmt updating phpfmt...", String.format("engine: %s, version: %s.", settings.getEngineChannel(), settings.getEngineVersion()));
        selfUpdate();
        final MessageBus bus = ApplicationManager.getApplication().getMessageBus();
        final MessageBusConnection connection = bus.connect();
        connection.subscribe(AppTopics.FILE_DOCUMENT_SYNC, new FileListener());
    }

    private void selfUpdate() {
        String engineVersion = settings.getEngineVersion();
        String engineChannel = settings.getEngineChannel();
        if (engineVersion.isEmpty()) {
            String releasesUrl = "https://raw.githubusercontent.com/phpfmt/releases/master/releases.json";
            toEventLog(settings.isDebug(), "phpfmt fetching phpfmt version", String.format("using releases url: %s, with engine: %s.", releasesUrl, settings.getEngineChannel()));
            InputStream responseInputStream = null;
            try {
                URL url = new URL(releasesUrl);
                HttpURLConnection request  = (HttpURLConnection) url.openConnection();
                request.connect();
                if (request.getResponseCode() > 400) {
                    responseInputStream = request.getErrorStream();
                } else {
                    responseInputStream = request.getInputStream();
                }

                // Convert to a JSON object to print data
                JsonParser jp = new JsonParser(); //from gson
                JsonElement root = jp.parse(new InputStreamReader((InputStream) request.getContent())); //Convert the input stream to a json element
                JsonObject rootobj = root.getAsJsonObject(); //May be an array, may be an o
                engineVersion = rootobj.get(engineChannel).getAsString();
                settings.setEngineVersion(engineVersion);
                toEventLog(settings.isDebug(), "phpfmt found version", String.format("engine: %s, version: %s.", settings.getEngineChannel(), settings.getEngineVersion()));
            } catch (Exception e) {
                toEventLog(settings.isDebug(), "phpfmt cannot get releases data", e.getMessage());
            } finally {
                if (responseInputStream != null) {
                    try {
                        responseInputStream.close();
                    } catch (IOException e) {
                    }
                }
            }
        }

        if(engineVersion.isEmpty()) {
            return;
        }

        String downloadUrl = "https://github.com/phpfmt/releases/raw/master/releases/" + engineChannel + "/" + engineVersion + "/fmt.phar";
        FileOutputStream fos = null;
        String output = path() + "fmt.phar";
        try {
            toEventLog(settings.isDebug(), "phpfmt downloading new release", "");
            URL website = new URL(downloadUrl);
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            fos = new FileOutputStream(output);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.close();
        } catch (Exception e) {
            toEventLog(settings.isDebug(), "phpfmt cannot download fmt.phar", e.getMessage());
            if (fos != null){
                try {
                    fos.close();
                } catch(IOException fosEx) {

                }
            }
            return;
        }
        settings.setPharPath(output);
        toEventLog(settings.isDebug(), "phpfmt was updated successfully.", "");
    }

    private String path() {
        URL url1 = Component.class.getResource("/META-INF/plugin.xml");
        String ur = url1.toString();
        ur = ur.substring(9);
        String truepath[] = ur.split("phpstorm-phpfmt.jar!");
        truepath[0] = truepath[0].replaceAll("%20", " ");
        String x = truepath[0];
        toEventLog(settings.isDebug(), "phpfmt phar's path extracted", String.format("path is: %s, ur: %s", x, ur));
        return x;
    }

    final static NotificationGroup ng = new NotificationGroup("phpfmt", NotificationDisplayType.NONE, true);

    public static void notify(String title, String msg) {
        Notification notification = ng.createNotification("[" + title + "]" + msg, NotificationType.INFORMATION);
        EventLog.formatForLog(notification,"");
        Notifications.Bus.notify(notification);
        notification.hideBalloon();
    }
    public static void toEventLog(boolean isDebug, String title) {
        toEventLog(isDebug, title, "");
    }

    public static void toEventLog(boolean isDebug, String title, String msg) {
        if (isDebug) {
            LOGGER.debug(title + ": " + msg);
            notify(title, msg);
        }
    }

    public void disposeComponent() {
    }

    @NotNull
    public String getComponentName() {
        return COMPONENT_NAME;
    }

}
