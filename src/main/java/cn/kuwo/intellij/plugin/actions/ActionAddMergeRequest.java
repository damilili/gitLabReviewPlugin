package cn.kuwo.intellij.plugin.actions;

import cn.kuwo.intellij.plugin.GitLabUtil;
import cn.kuwo.intellij.plugin.ui.CreateMergeRequestDialog.CreateMergeRequestDialog;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;

public class ActionAddMergeRequest extends DumbAwareAction {
    public ActionAddMergeRequest() {
        super("Create _Merge Request...", "Creates merge request from current branch", AllIcons.Vcs.Merge);
    }

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        CreateMergeRequestDialog createMergeRequestDialog=new CreateMergeRequestDialog(anActionEvent.getProject());
        createMergeRequestDialog.show();
        GitLabUtil instance = GitLabUtil.getInstance(anActionEvent.getProject());
    }
}
