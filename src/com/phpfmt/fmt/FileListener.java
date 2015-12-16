package com.phpfmt.fmt;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.fileEditor.FileDocumentManagerAdapter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.URL;
import java.util.*;

import static com.phpfmt.fmt.Documents.isDocumentActive;

//https://github.com/JetBrains/intellij-community/blob/master/platform/lang-impl/src/com/intellij/execution/console/ConsoleHistoryController.java

public class FileListener extends FileDocumentManagerAdapter {
    private final Settings settings = ServiceManager.getService(Settings.class);

    @Override
    public void beforeDocumentSaving(@NotNull Document document) {
        SaveAllAction.LOGGER.debug("isActivate: " + settings.isActivate());
        SaveAllAction.LOGGER.debug("isActionPerformed: " + " :: isDocumentActive: " + isDocumentActive(document));
        if (!settings.isActivate()) {
            SaveAllAction.LOGGER.debug("Document " + document + " is still active, do not execute");
            return;
        }

        Formatter formatter = new Formatter();
        formatter.Format(document, settings);
    }
}
