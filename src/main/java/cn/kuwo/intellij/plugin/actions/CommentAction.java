package cn.kuwo.intellij.plugin.actions;

import cn.kuwo.intellij.plugin.GitLabUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public final class CommentAction extends RequestDetailAction {

    public CommentAction() {
        super("", "Add comments on the request.", IconLoader.getIcon("/icons/comment.png"));
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        JTextField jTextField = new JTextField();
        Messages.showTextAreaDialog(jTextField, "Input Comment", "cc");
        String comment = jTextField.getText();
        if (comment != null && !comment.isEmpty()) {
            if (comment.length() > 200) {
                Messages.showMessageDialog(e.getProject(),
                        "The length of the message must be less than  than 200.", "message", AllIcons.Ide.Error);
                return;
            }
            GitLabUtil.getInstance(e.getProject()).commentRequest(mergeRequest, comment);
        }
    }
}