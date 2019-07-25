package cn.kuwo.intellij.plugin.actions;

import cn.kuwo.intellij.plugin.ui.SettingUi;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.DumbAware;

public class SettingAction extends AnAction implements DumbAware {
    public SettingAction( ) {
        super("Settings", "Open GitReviewMerge Plugin ", AllIcons.General.Settings);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        ShowSettingsUtil.getInstance().showSettingsDialog(e.getProject(),SettingUi.SETTINGNAME );
    }
}
