package cn.kuwo.intellij.plugin.actions;

import cn.kuwo.intellij.plugin.GitLabUtil;
import cn.kuwo.intellij.plugin.RMListObservable;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;

public final class CloseAction extends RequestDetailAction {

    public CloseAction() {
        super("Close", "Close the request.", IconLoader.getIcon("/icons/close.png"));
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        if (mergeRequest.getState().equals("closed") || mergeRequest.getState().equals("merged")) {
            Messages.showMessageDialog("The merge request has been closed or merged.", "Close Fail", AllIcons.Ide.Error);
        } else {
            GitLabUtil.getInstance(e.getProject()).closeRequest(mergeRequest);
            RMListObservable.getInstance().refreshList();
        }
    }
}