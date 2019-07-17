package cn.kuwo.intellij.plugin.actions;

import cn.kuwo.intellij.plugin.GitLabUtil;
import cn.kuwo.intellij.plugin.RMListObservable;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.util.messages.impl.Message;
import git4idea.GitUtil;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;

public final class AcceptAction extends RequestDetailAction {

    public AcceptAction() {
        super("Accept", "Accept the request.", IconLoader.getIcon("/icons/accept.png"));
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
//        for (GitRepository gitRepository : GitUtil.getRepositories(e.getProject())) {
//            for (GitRemote gitRemote : gitRepository.getRemotes()) {
//                gitRemote.get
//            }
//        }
        String sourceBranch = mergeRequest.getSourceBranch();
        String targetBranch = mergeRequest.getTargetBranch();
        if (mergeRequest.getState().equals("closed") || mergeRequest.getState().equals("merged")) {
            Messages.showMessageDialog("The merge request has been closed or merged.", "Merge Fail",AllIcons.Ide.Error);
        } else {
            GitLabUtil.getInstance(e.getProject()).acceptRequest(mergeRequest);
            RMListObservable.getInstance().refreshList();
        }
    }
}