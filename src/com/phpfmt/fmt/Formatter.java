package com.phpfmt.fmt;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.DocumentRunnable;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.encoding.EncodingManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.io.IOUtil;
import org.apache.log4j.Level;
import org.apache.sanselan.util.IOUtils;
import sun.security.pkcs.EncodingException;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static com.phpfmt.fmt.PsiFiles.isPsiFilePhysicallyInProject;

/**
 * Created by Shaked on 12/13/15.
 */
public class Formatter {
    public static final Logger LOGGER = Logger.getInstance(FormatterAction.class);

    static {
        LOGGER.setLevel(Level.DEBUG);
    }

    private boolean isValidExtension(FileType fileType, ArrayList<String> extensions) {
        String fileTypeName = fileType.getName().toLowerCase();
        return extensions.contains(fileTypeName);
    }

    private boolean blockedFileOrExtension(String fileName, String ignoreFileExtensions) {
        String[] p = ignoreFileExtensions.split(",");
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
        for (String item : p) {
            LOGGER.debug("checking item: " + item + " with extension: " + extension);
            if (fileName.lastIndexOf(item) > -1) {
                LOGGER.debug("blockedFileOrExtension::returns true::" + item);
                return true;
            }
        }
        LOGGER.debug("blockedFileOrExtension::returns false, filename: " + fileName + ", extensions: " + extension);
        return false;
    }

    public void Format(final Document document, final Settings settings) {
        for (final Project project : ProjectManager.getInstance().getOpenProjects()) {
            final PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
            Component.toEventLog(settings.isDebug(), "Format", "psiFile: " + psiFile.getText() + "filetype: " + psiFile.getFileType() + " name: " + psiFile.getOriginalFile().getName());
            if (!this.isValidExtension(psiFile.getFileType(), settings.getExtensions())) {
                Component.toEventLog(settings.isDebug(), "Format", "isValidExtension failed: " + psiFile.getFileType().getName().toLowerCase() + ": " + settings.getExtensions().toString());
                return;
            }

            if (!settings.getIgnoreFilesExtensions().isEmpty() && this.blockedFileOrExtension(psiFile.getName().toLowerCase(), settings.getIgnoreFilesExtensions())) {
                Component.toEventLog(settings.isDebug(), "Format", "blockedFileOrExtension passed: " + psiFile.getName().toLowerCase() + ": " + settings.getIgnoreFilesExtensions().toString());
                return;
            }

            if (isPsiFileEligible(project, psiFile)) {
                try {

                    Charset charset = EncodingManager.getInstance().getEncoding(FileDocumentManager.getInstance().getFile(document), true);
                    if (charset == null) charset = EncodingManager.getInstance().getDefaultCharset();
                    if (charset == null) charset = Charset.defaultCharset();
                    Charset myCharset = charset;
                    Component.toEventLog(settings.isDebug(), "Charset", "displayName: " + charset.displayName() + " name: " + charset.name());

                    final String formatted = fmt(document.getText(), settings, myCharset);
                    Component.toEventLog(settings.isDebug(), "Format", "formatted: " + formatted);
                    ApplicationManager.getApplication().runWriteAction(new DocumentRunnable(document, null) {
                        @Override
                        public void run() {
                            CommandProcessor.getInstance().executeCommand(project, new Runnable() {
                                @Override
                                public void run() {
                                    String original = document.getText();
                                    String err = "";
                                    try {
                                        Component.toEventLog(settings.isDebug(), "Format", "_merge: " + err);
                                        this._merge(document, document.getTextLength(), formatted);
                                    } catch (MergeException e) {
                                        err = String.format("Could not merge changes into the buffer, edit aborted: %s", e.getMessage());
                                        Component.toEventLog(settings.isDebug(), "Format", "_merge: " + err);
                                        document.replaceString(0, original.length(), original);
                                    } catch (Exception e) {
                                        err = "error: " + e.getMessage();
                                    } finally {
                                        System.out.println(err);
                                        Component.toEventLog(settings.isDebug(), "Format", "err: " + err);
                                    }
                                }

                                private String substr(int start, int end) {
                                    return document.getText(new TextRange(start, end)).toString();
                                }

                                private boolean _merge(Document document, int size, String text) throws MergeException {
                                    diff_match_patch dmp = new diff_match_patch();
                                    LinkedList<diff_match_patch.Diff> diffs = dmp.diff_main(this.substr(0, size), text, false);
                                    dmp.diff_cleanupEfficiency(diffs);
                                    int i = 0;
                                    boolean dirty = false;
                                    for (diff_match_patch.Diff d : diffs) {
                                        String s = d.text.replace("\r", "");
                                        int l = s.length();
                                        if (d.operation == diff_match_patch.Operation.EQUAL) {
                                            l = s.length();
                                            String subStr = this.substr(i, i + l);
                                            if (!s.equals(subStr)) {
                                                throw new MergeException("mismatch 1");
                                            }
                                            i += l;
                                        } else {
                                            dirty = true;
                                            if (d.operation == diff_match_patch.Operation.INSERT) {
                                                document.insertString(i, s);
                                                i += l;
                                            } else {
                                                if (!s.equals(this.substr(i, i + l))) {
                                                    throw new MergeException("mismatch 2");
                                                }
                                                document.deleteString(i, i + l);
                                            }
                                        }
                                    }
                                    return dirty;
                                }
                            }, formatted, document);
                        }
                    });

                } catch (Exception e) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    Component.toEventLog(settings.isDebug(), "Format", "stack trace: " + sw.toString());
                    Component.toEventLog(settings.isDebug(), "Format", "fmt error: " + e.getMessage());
                    Component.notify("Formatter", "Could not format code");
                }
            }
        }
    }

    private boolean isPsiFileEligible(Project project, PsiFile psiFile) {
        return psiFile != null && isPsiFilePhysicallyInProject(project, psiFile);// &&
        //!isPsiFileExcluded(project, psiFile); //, settings.getExclusions()
    }

    private String fmt(String text, Settings settings, Charset myCharset) throws IllegalArgumentException, InterruptedException {
        Component.toEventLog(settings.isDebug(), "fmt starts", "text: " + text);
        List<String> list = new ArrayList<>();
        list.add(settings.getPhpExecutable());
        if (!settings.isDebug()) {
            list.add("-ddisplay_errors=stderr");
        }
        if (settings.isPsr1()) {
            list.add("-dshort_open_tag=On");
        }
        String pharPath = settings.getPharPath();
        if (!settings.getCustomPharPath().isEmpty()) {
            pharPath = settings.getCustomPharPath();
        }
        list.add(pharPath);

        if (!settings.getOptionsFile().isEmpty()) {
            list.add(String.format("--config=%s", settings.getOptionsFile()));
        }

        if (settings.isPsr1()) {
            list.add("--psr1");
        }
        if (settings.isPsr1Naming()) {
            list.add("--psr1-naming");
        }


        if (settings.isPsr2()) {
            list.add("--psr2");
        }

        if (settings.getSpaceIndentationSize() > 0) {
            list.add("--indent_with_space");
        } else if (settings.isSpaceIndentation()) {
            list.add("--indent_with_space=" + settings.getSpaceIndentationSize());
        }

        if (settings.isAutoAlign()) {
            list.add("--enable_auto_align");
        }

        if (settings.isVisibilityOrder()) {
            list.add("--visibility_order");
        }

        if (settings.isSmartLinebreakAfterCurly()) {
            list.add("--smart_linebreak_after_curly");
        }

        if (settings.isYoda()) {
            list.add("--yoda");
        }

        if (!settings.getSettersGettersType().isEmpty()) {
            list.add("--setters_and_getters=" + settings.getSettersGettersType());
            list.add("--constructor=" + settings.getSettersGettersType());
        }

        if (settings.isAutoImport() && !settings.getOracleFileName().isEmpty()) {
            list.add("--oracleDB=" + settings.getOracleFileName());
        }

        if (!settings.getPasses().isEmpty()) {
            list.add("--passes=" + settings.getPasses());
        }

        if (!settings.getExclude().isEmpty()) {
            list.add("--exclude=" + settings.getExclude());
        }

        if (settings.isDebug()) {
            list.add("-v");
        }

        list.add("-o=-");
        list.add("-");
        Component.toEventLog(settings.isDebug(), "Format", "LIST: " + list.toString());

        ProcessBuilder pb = new ProcessBuilder(list);
        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new InterruptedException("error start process: " + e.getMessage() + ": text: " + text + ": list: " + list.toString() + ": settings: " + settings.toString());
        }

        OutputStream stdin = process.getOutputStream(); // <- Eh?
        InputStream stdout = process.getInputStream();
        InputStream stderr = process.getErrorStream();


        ByteArrayOutputStream errous = new ByteArrayOutputStream();
        StreamGobbler errorGobbler = new StreamGobbler(stderr, "ERROR", errous, myCharset, settings.isDebug());
        ByteArrayOutputStream ous = new ByteArrayOutputStream();
        // any output?
        StreamGobbler outputGobbler = new StreamGobbler(stdout, "OUTPUT", ous, myCharset, settings.isDebug());

        errorGobbler.start();
        outputGobbler.start();

        BufferedOutputStream writer = new BufferedOutputStream(stdin);

        try {
            byte[] buffer = new byte[1024];
            InputStream is = new ByteArrayInputStream(text.getBytes(myCharset));

            int read;
            while ((read = is.read(buffer)) != -1) {
                writer.write(buffer, 0, read);
            }
            String strFromBytes = new String(text.getBytes(myCharset), myCharset);
            Component.toEventLog(settings.isDebug(), "BeforeWriting", "text: " + text + " :: strFromBytes: " + strFromBytes);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new InterruptedException("error close process: " + e.getMessage() + ": text: " + text + ": list: " + list.toString() + ": settings: " + settings.toString());
        }
        int exitStatus = process.waitFor();

        String fmtCode = text;
        try {
            fmtCode = ous.toString(myCharset.name());
        } catch(UnsupportedEncodingException e) {
            Component.toEventLog(settings.isDebug(), "Format", "unsupported encoding exception:  " + e.getMessage());
        }

        Component.toEventLog(settings.isDebug(), "Format", "fmtCode: " + fmtCode + " :: ous: " + ous);
        if (0 != exitStatus) {
            String err = errous.toString();
            Component.toEventLog(settings.isDebug(), "Formatter", "stdErr: " + err);
            throw new InterruptedException(err);
        }
        return fmtCode;

    }

    public class MergeException extends Exception {
        public MergeException(String message) {
            super(message);
        }
    }

    static String readFile(String path, Charset encoding)
            throws IOException
    {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
}
