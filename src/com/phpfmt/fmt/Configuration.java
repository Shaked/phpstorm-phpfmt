package com.phpfmt.fmt;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by Shaked on 12/13/15.
 */
public class Configuration implements Configurable {

    private Settings settings = ServiceManager.getService(Settings.class);
    private JCheckBox activate;
    private JTextField phpExecutable;
    private JTextField optionsFile;
    private JCheckBox debugCheckBox;
    private JPanel mainPanel;
    private JTextArea passes;
    private JTextArea exclude;
    private JCheckBox psr1CheckBox;
    private JCheckBox psr1NamingCheckBox;
    private JCheckBox psr2CheckBox;
    private JCheckBox yodaCheckBox;
    private JTextField extensions;
    private JCheckBox formatOnSave;


    @Nls
    @Override
    public String getDisplayName() {
        return "PhpStorm-phpfmt";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        initActionListeners();
        return mainPanel;
    }

    @Override
    public boolean isModified() {
        boolean modified = settings.isActivate() != activate.isSelected();
        modified = modified || settings.getOptionsFile() != optionsFile.getText();
        modified = modified || debugCheckBox.isSelected();
        modified = modified || settings.getPhpExecutable() != phpExecutable.getText();
        modified = modified || settings.getPasses() != passes.getText();
        modified = modified || settings.getExclude() != exclude.getText();
        modified = modified || settings.isPsr1() != psr1CheckBox.isSelected();
        modified = modified || settings.isPsr1Naming() != psr1NamingCheckBox.isSelected();
        modified = modified || settings.isPsr2() != psr2CheckBox.isSelected();
        modified = modified || settings.isYoda() != yodaCheckBox.isSelected();
        modified = modified || !settings.getExtensions().contains(extensions.getText());
        modified = modified || settings.isFormatOnSave() != formatOnSave.isSelected();
        return modified;
    }

    @Override
    public void apply() throws ConfigurationException {
        settings.setActivated(activate.isSelected());
        settings.setOptionsFile(optionsFile.getText());
        settings.setDebug(debugCheckBox.isSelected());
        settings.setPhpExecutable(phpExecutable.getText());
        settings.setPasses(passes.getText());
        settings.setExclude(exclude.getText());
        settings.setPsr1(psr1CheckBox.isSelected());
        settings.setPsr1Naming(psr1NamingCheckBox.isSelected());
        settings.setPsr2(yodaCheckBox.isSelected());
        settings.setYoda(yodaCheckBox.isSelected());
        settings.setExtensions(extensions.getText());
        settings.setFormatOnSave(formatOnSave.isSelected());
    }

    @Override
    public void reset() {
        activate.setSelected(settings.isActivate());
        optionsFile.setText(settings.getOptionsFile());
        debugCheckBox.setSelected(settings.isDebug());
        phpExecutable.setText(settings.getPhpExecutable());
        passes.setText(settings.getPasses());
        exclude.setText(settings.getExclude());
        psr1CheckBox.setSelected(settings.isPsr1());
        psr1NamingCheckBox.setSelected(settings.isPsr1Naming());
        yodaCheckBox.setSelected(settings.isYoda());
        extensions.setText(StringUtil.join(settings.getExtensions(), ","));
        formatOnSave.setSelected(settings.isFormatOnSave());
    }

    @Override
    public void disposeUIResources() {
        activate = null;
        optionsFile = null;
        debugCheckBox = null;
        phpExecutable = null;
        passes = null;
        exclude = null;
        psr1CheckBox = null;
        psr1NamingCheckBox = null;
        yodaCheckBox = null;
        extensions = null;
        formatOnSave = null;
    }


    private void initActionListeners() {
        activate.addActionListener(getActionListener());
        optionsFile.addActionListener(getActionListener());
        debugCheckBox.addActionListener(getActionListener());
        phpExecutable.addActionListener(getActionListener());
        psr1CheckBox.addActionListener(getActionListener());
        psr1NamingCheckBox.addActionListener(getActionListener());
        yodaCheckBox.addActionListener(getActionListener());
        formatOnSave.addActionListener(getActionListener());
    }

    @NotNull
    private ActionListener getActionListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                optionsFile.setEnabled(activate.isSelected());

            }
        };
    }


}
