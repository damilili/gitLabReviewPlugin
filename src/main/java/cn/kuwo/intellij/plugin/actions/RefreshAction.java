package cn.kuwo.intellij.plugin.actions;

import cn.kuwo.intellij.plugin.GitLabUtil;
import cn.kuwo.intellij.plugin.ui.SettingUi;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

public class RefreshAction extends AnAction implements DumbAware {

    public RefreshAction() {
        super("Refresh", "Refresh merge request list", AllIcons.Actions.Refresh);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        if (GitLabUtil.getInstance(e.getProject()).checkTokens() == 0) {
            ShowSettingsUtil.getInstance().showSettingsDialog(e.getProject(),SettingUi.SETTINGNAME );
            return;
        }
        new Task.Backgroundable(e.getProject(), "Get merge requests...") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                GitLabUtil.getInstance(e.getProject()).getAllRequest();
            }
        }.queue();
    }
}
