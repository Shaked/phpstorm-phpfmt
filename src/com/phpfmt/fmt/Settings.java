package com.phpfmt.fmt;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Shaked on 12/13/15.
 */
@State(name = "phpfmtSettings",
        storages = {
                @Storage(file = StoragePathMacros.APP_CONFIG + "/phpfmt_settings.xml")})
public class Settings implements PersistentStateComponent<Settings> {
    @Nullable
    @Override
    public Settings getState() {
        return this;
    }

    @Override
    public void loadState(Settings settings) {
        XmlSerializerUtil.copyBean(settings, this);
    }

    private String pharPath = "";
    private String version = "";

    private String optionsFile = "";

    private String phpExecutable = "/usr/local/bin/php";

    private boolean activated = true;
    private boolean debug = false;
    private boolean psr1 = false;
    private boolean psr1Naming = false;
    private boolean psr2 = false;
    private int indentWithSpace = 0;
    private boolean enableAutoAlign = false;
    private boolean visibilityOrder = false;
    private boolean smartLinebreakAfterCurly = false;
    private boolean yoda = false;
    private boolean sgter = false;
    private boolean autoImport = false;

    public boolean isInstalled(String version) {
        return pharPath != "" && this.version == version;
    }

    public String getPharPath() {
        return pharPath;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setPharPath(String pharPath) {
        this.pharPath = pharPath;
    }

    private String passes = "";
    private String exclude = "";

    public boolean isActivated() {
        return activated;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isPsr1() {
        return psr1;
    }

    public void setPsr1(boolean psr1) {
        this.psr1 = psr1;
    }

    public boolean isPsr1Naming() {
        return psr1Naming;
    }

    public void setPsr1Naming(boolean psr1Naming) {
        this.psr1Naming = psr1Naming;
    }

    public boolean isPsr2() {
        return psr2;
    }

    public void setPsr2(boolean psr2) {
        this.psr2 = psr2;
    }

    public boolean isIndentWithSpace() {
        return (indentWithSpace == -1);
    }
    public int getIndentWithSpace() {
        return indentWithSpace;
    }

    public void setIndentWithSpace(int indentWithSpace) {
        this.indentWithSpace = indentWithSpace;
    }

    public String getPasses() {
        return passes;
    }

    public void setPasses(String passes) {
        this.passes = passes;
    }

    public String getExclude() {
        return exclude;
    }

    public void setExclude(String exclude) {
        this.exclude = exclude;
    }

    public boolean isActivate() {
        return this.activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public boolean isEnableAutoAlign() {
        return enableAutoAlign;
    }

    public void setEnableAutoAlign(boolean enableAutoAlign) {
        this.enableAutoAlign = enableAutoAlign;
    }

    public boolean isVisibilityOrder() {
        return visibilityOrder;
    }

    public void setVisibilityOrder(boolean visibilityOrder) {
        this.visibilityOrder = visibilityOrder;
    }

    public boolean isSmartLinebreakAfterCurly() {
        return smartLinebreakAfterCurly;
    }

    public void setSmartLinebreakAfterCurly(boolean smartLinebreakAfterCurly) {
        this.smartLinebreakAfterCurly = smartLinebreakAfterCurly;
    }

    public boolean isYoda() {
        return yoda;
    }

    public void setYoda(boolean yoda) {
        this.yoda = yoda;
    }

    public boolean isSgter() {
        return sgter;
    }

    public void setSgter(boolean sgter) {
        this.sgter = sgter;
    }

    public boolean isAutoImport() {
        return autoImport;
    }

    public void setAutoImport(boolean autoImport) {
        this.autoImport = autoImport;
    }

    public String getOptionsFile() {
        return optionsFile;
    }

    public void setOptionsFile(String optionsFile) {
        this.optionsFile = optionsFile;
    }

    public String getPhpExecutable() {
        return phpExecutable;
    }

    public void setPhpExecutable(String phpExecutable) {
        this.phpExecutable = phpExecutable;
    }


}
