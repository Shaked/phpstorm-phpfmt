package com.phpfmt.fmt;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.fileEditor.FileDocumentManagerAdapter;
import org.jetbrains.annotations.NotNull;

import static com.phpfmt.fmt.Documents.isDocumentActive;

//https://github.com/JetBrains/intellij-community/blob/master/platform/lang-impl/src/com/intellij/execution/console/ConsoleHistoryController.java

public class FileListener extends FileDocumentManagerAdapter {
    private final Settings settings = ServiceManager.getService(Settings.class);

    @Override
    public void beforeDocumentSaving(@NotNull Document document) {
        FormatterAction.LOGGER.debug("isActivate: " + settings.isActivate());
        FormatterAction.LOGGER.debug("isActionPerformed: " + " :: isDocumentActive: " + isDocumentActive(document));
        if (!settings.isActivate() || !settings.isFormatOnSave()) {
            FormatterAction.LOGGER.debug("isActivate: " + settings.isActivate() + "isFormatOnSave" + settings.isFormatOnSave());
            return;
        }

        Formatter formatter = new Formatter();
        formatter.Format(document, settings);
    }
}
