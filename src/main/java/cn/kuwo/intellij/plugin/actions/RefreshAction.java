package cn.kuwo.intellij.plugin.actions;

import cn.kuwo.intellij.plugin.GitLabUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class RefreshAction extends AnAction implements DumbAware {
    private Project project;
    private JPanel myPanel;

    public RefreshAction(Project project) {
        super("Refresh", "Refresh merge request list", AllIcons.Actions.Refresh);
        this.project = project;
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        new Task.Backgroundable(project, "Get merge requests...") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                GitLabUtil.getInstance(project).getAllRequest();
            }
        }.queue();
    }
}
