package cn.kuwo.intellij.plugin.actions;

import cn.kuwo.intellij.plugin.GitLabUtil;
import cn.kuwo.intellij.plugin.RMListObservable;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;

public final class AcceptAction extends RequestDetailAction {

    public AcceptAction() {
        super("Accept", "Accept the request.", IconLoader.getIcon("/icons/accept.png"));
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        if (mergeRequest.getState().equals("closed") || mergeRequest.getState().equals("merged")) {
            Messages.showMessageDialog("The merge request has been closed or merged.", "Merge Fail", AllIcons.Ide.Error);
        } else {
            GitLabUtil.getInstance(e.getProject()).acceptRequest(mergeRequest);
            RMListObservable.getInstance().refreshList();
        }
    }

    @Override
    public void update(AnActionEvent e) {
        if (mergeRequest.getState().equals("closed") || mergeRequest.getState().equals("merged")) {
            e.getPresentation().setEnabled(false);
        } else {
            e.getPresentation().setEnabled(true);
        }
    }
}