package com.phpfmt.fmt;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.DocumentRunnable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.apache.log4j.Level;

import java.io.*;
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

    public void Format(final Document document, Settings settings) {
        for (final Project project : ProjectManager.getInstance().getOpenProjects()) {
            final PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
            LOGGER.debug("psiFile: " + psiFile.getText());
            if (isPsiFileEligible(project, psiFile)) {
                try {
                    final String formatted = fmt(document.getCharsSequence().toString(), settings);
                    LOGGER.debug("formatted: " + formatted);
                    ApplicationManager.getApplication().runWriteAction(new DocumentRunnable(document, null) {
                        @Override
                        public void run() {
                            CommandProcessor.getInstance().executeCommand(project, new Runnable() {
                                @Override
                                public void run() {
                                    String original = document.getText();
                                    String err = "";
                                    try {
                                        LOGGER.debug("_merge: " + err);
                                        this._merge(document, document.getTextLength(), formatted);
                                    } catch (MergeException e) {
                                        err = String.format("Could not merge changes into the buffer, edit aborted: %s", e.getMessage());
                                        LOGGER.debug("_merge: " + err);
                                        document.replaceString(0, original.length(), original);
                                    } catch (Exception e) {
                                        err = "error: " + e.getMessage();
                                    } finally {
                                        System.out.println(err);
                                        LOGGER.debug("err: " + err);
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
                                        String s = d.text;
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
                    LOGGER.debug("stack trace: " + sw.toString());
                    System.out.println("fmt error: " + e.getMessage());
                    LOGGER.debug("fmt error: " + e.getMessage());
                }
            }
        }
    }

    private boolean isPsiFileEligible(Project project, PsiFile psiFile) {
        return psiFile != null && isPsiFilePhysicallyInProject(project, psiFile);// &&
        //!isPsiFileExcluded(project, psiFile); //, settings.getExclusions()
    }

    private String fmt(String text, Settings settings) throws IllegalArgumentException, InterruptedException {

        List<String> list = new ArrayList<>();
        list.add(settings.getPhpExecutable());
        if (!settings.isDebug()) {
            list.add("-ddisplay_errors=stderr");
        }
        if (settings.isPsr1()) {
            list.add("-dshort_open_tag=On");
        }
        list.add(settings.getPharPath());

        if ("" != settings.getOptionsFile()) {
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

        if (settings.isIndentWithSpace()) {
            list.add("--indent_with_space");
        } else if (settings.getIndentWithSpace() > 0) {
            list.add("--indent_with_space=" + settings.getIndentWithSpace());
        }

        if (settings.isEnableAutoAlign()) {
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

        list.add("-o=-");
        list.add("-");
        System.out.println(list);
        LOGGER.debug("LIST: " + list.toString());
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
        StreamGobbler errorGobbler = new StreamGobbler(stderr, "ERROR", errous);
        ByteArrayOutputStream ous = new ByteArrayOutputStream();
        // any output?
        StreamGobbler outputGobbler = new StreamGobbler(stdout, "OUTPUT", ous);

        errorGobbler.start();
        outputGobbler.start();

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));

        try {
            writer.write(text);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new InterruptedException("error close process: " + e.getMessage() + ": text: " + text + ": list: " + list.toString() + ": settings: " + settings.toString());
        }
        int exitStatus = process.waitFor();
//

//       String fmtCode = Util.streamToString(stdout);
//       String err = Util.streamToString(stderr);

        String fmtCode = ous.toString();


        LOGGER.debug("fmtCode: " + fmtCode);
        if (0 != exitStatus) {
            String err = errous.toString();
            LOGGER.debug("stdErr: " + err);
            throw new InterruptedException(err);
        }
        return fmtCode;

    }

    public class MergeException extends Exception {
        public MergeException(String message) {
            super(message);
        }
    }

    class StreamGobbler extends Thread {
        InputStream is;
        String type;
        OutputStream os;

        StreamGobbler(InputStream is, String type) {
            this(is, type, null);
        }

        StreamGobbler(InputStream is, String type, OutputStream redirect) {
            this.is = is;
            this.type = type;
            this.os = redirect;
        }

        public void run() {
            try {
                PrintWriter pw = null;
                if (os != null)
                    pw = new PrintWriter(os);

                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line = null;
                while ((line = br.readLine()) != null) {
                    if (pw != null)
                        pw.println(line);
                    System.out.println(type + ">" + line);
                }
                if (pw != null)
                    pw.flush();
            } catch (IOException ioe) {
                LOGGER.debug("StreamGobbler: " + this.type);
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                ioe.printStackTrace(pw);
                LOGGER.debug("stack trace: " + sw.toString());
            }
        }
    }
}
