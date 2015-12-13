package com.phpfmt.fmt;

import com.intellij.AppTopics;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.richcopy.model.OutputInfoSerializer;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.fileEditor.FileDocumentManagerAdapter;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;

public class Component implements ApplicationComponent {

    private static final String COMPONENT_NAME = "Save Actions";
    private final Settings settings = ServiceManager.getService(Settings.class);

    public void initComponent() {
        install();
        final MessageBus bus = ApplicationManager.getApplication().getMessageBus();
        final MessageBusConnection connection = bus.connect();
        connection.subscribe(AppTopics.FILE_DOCUMENT_SYNC, new FileListener());
    }

    private void install() {
        String version = PluginManager.getPlugin(PluginId.getId("com.phpfmt.fmt.SaveAllAction.id")).getVersion();
        SaveAllAction.LOGGER.debug(this.getClass().toString() + ": version: " + version + ": isInstalled:" + settings.isInstalled(version) + " getPath: " + settings.getPharPath());
        if (!settings.isInstalled(version)) {
            File phar = new File("fmt.phar");
            if (phar != null) {
                phar.delete();
            }
            try {
                InputStream ddlStream = Component.class.getClassLoader().getResourceAsStream("/bin/fmt.phar");
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream("fmt.phar");
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
                String pharPath = new File("fmt.phar").getAbsolutePath();

                SaveAllAction.LOGGER.debug(this.getClass().toString() + ": pharPath:" + pharPath);
                settings.setPharPath(pharPath);
                settings.setVersion(version);
            } catch (Exception e) {
                SaveAllAction.LOGGER.debug(this.getClass().toString() + ": exception export resource" + e.getMessage());
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
            SaveAllAction.LOGGER.debug(Component.class.toString() + " jarPath: " + jarPath + " :: jarFolder: " + jarFolder + " outName: " + outName);
            while ((readBytes = stream.read(buffer)) > 0) {
                resStreamOut.write(buffer, 0, readBytes);
            }
        } catch (Exception ex) {
            throw ex;
        } finally {
            stream.close();
            resStreamOut.close();
        }

        SaveAllAction.LOGGER.debug(Component.class.toString() + " outName: " + outName);
        return outName;
    }

    public void disposeComponent() {
    }

    @NotNull
    public String getComponentName() {
        return COMPONENT_NAME;
    }

}
