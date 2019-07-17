package cn.kuwo.intellij.plugin.actions;

import cn.kuwo.intellij.plugin.GitLabUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.util.IconLoader;

public final class CommentAction extends RequestDetailAction {

    public CommentAction() {
        super("", "Add comments on the request.",IconLoader.getIcon("/icons/comment.png"));
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        GitLabUtil.getInstance(e.getProject()).commentRequest(mergeRequest,"abddddddded");
    }
}