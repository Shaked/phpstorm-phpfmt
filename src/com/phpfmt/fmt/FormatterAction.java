package com.phpfmt.fmt;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.sun.javadoc.Doc;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;

import static com.phpfmt.fmt.Documents.isDocumentActive;

public class FormatterAction extends AnAction implements DumbAware {
    final public static String id = "phpfmt";
    private Settings settings = ServiceManager.getService(Settings.class);
    public static final Logger LOGGER = Logger.getInstance(FormatterAction.class);

    static {
        LOGGER.setLevel(Level.DEBUG);
    }

    private static Editor getEditor(AnActionEvent e) {
        return PlatformDataKeys.EDITOR.getData(e.getDataContext());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Document document = getEditor(e).getDocument();
        FormatterAction.LOGGER.debug("isActivate: " + settings.isActivate());
        FormatterAction.LOGGER.debug("isActionPerformed: " + " :: isDocumentActive: " + isDocumentActive(document));
        if (!settings.isActivate()) {
            FormatterAction.LOGGER.debug("isActivate: " + settings.isActivate() + "isFormatOnSave" + settings.isFormatOnSave());
            return;
        }
        Formatter formatter = new Formatter();
        formatter.Format(document, settings);
    }
}
