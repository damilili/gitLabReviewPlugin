package cn.kuwo.intellij.plugin.ui.MergeRequestDetail;

import cn.kuwo.intellij.plugin.GitLabUtil;
import cn.kuwo.intellij.plugin.ui.BaseMergeRequestCell.BaseMergeRequestCell;
import com.intellij.openapi.project.Project;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabMergeRequest;
import org.gitlab.api.models.GitlabProject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;

public class MergeRequestDetail {
    private Project project;
    private GitlabMergeRequest mergeRequest;

    public static MergeRequestDetail getMergeRequestDetail(Project project, GitlabMergeRequest mergeRequest) {
        MergeRequestDetail mergeRequestCell = new MergeRequestDetail(project);
        if (mergeRequest != null) {
            mergeRequestCell.inflateView(mergeRequest);
        } else {
            mergeRequestCell.getBasePan().setVisible(false);
        }
        return mergeRequestCell;
    }

    private void inflateView(GitlabMergeRequest mergeRequest) {
        this.mergeRequest = mergeRequest;
        String title = "<html>" + mergeRequest.getTitle() + "</html>";
        getName().setText(title);
        getAssignee().setText(mergeRequest.getAssignee().getName());
        getAuthor().setText(mergeRequest.getAuthor().getName());
        getCreatedTime().setText(BaseMergeRequestCell.sdf.format(mergeRequest.getCreatedAt()));
        getUpdatedTime().setText(BaseMergeRequestCell.sdf.format(mergeRequest.getUpdatedAt()));
        getSourceBranch().setText(mergeRequest.getSourceBranch());
        getTargetBranch().setText(mergeRequest.getTargetBranch());
        getDescription().setText(mergeRequest.getDescription());
        getState().setText(mergeRequest.getState());
        getBasePan().setVisible(true);
        setWebUrl();
    }

    public MergeRequestDetail(Project project) {
        this.project = project;
        getBasePan().setVisible(false);
    }

    public void refresh(GitlabMergeRequest mergeRequest) {
        inflateView(mergeRequest);
    }

    public JPanel getBasePan() {
//        panel1.setBackground(new Color(0, 0x2b, 0x2b, 0x2b));
        return panel1;
    }

    public void setPanel1(JPanel panel1) {
        this.panel1 = panel1;
    }

    public JLabel getName() {
//        title.setBackground(new Color(0, 0x2b, 0x2b, 0x2b));
        return title;
    }

    public void setName(JLabel name) {
        this.title = name;
    }

    private JPanel panel1;
    private JLabel title;

    public JLabel getTitle() {
        return title;
    }

    public void setTitle(JLabel title) {
        this.title = title;
    }

    public JLabel getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(JLabel createdTime) {
        this.createdTime = createdTime;
    }

    public JLabel getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(JLabel updatedTime) {
        this.updatedTime = updatedTime;
    }

    public JLabel getState() {
        return state;
    }

    public void setState(JLabel state) {
        this.state = state;
    }

    public JLabel getSourceBranch() {
        return sourceBranch;
    }

    public void setSourceBranch(JLabel sourceBranch) {
        this.sourceBranch = sourceBranch;
    }

    public JLabel getTargetBranch() {
        return targetBranch;
    }

    public void setTargetBranch(JLabel targetBranch) {
        this.targetBranch = targetBranch;
    }

    public JLabel getAuthor() {
        return author;
    }

    public void setAuthor(JLabel author) {
        this.author = author;
    }

    public JLabel getAssignee() {
        JTextField jTextField;
        JTextArea jTextArea;
        return assignee;
    }

    public void setAssignee(JLabel assignee) {
        this.assignee = assignee;
    }

    public JLabel getDescription() {
        return description;
    }

    public void setDescription(JLabel description) {
        this.description = description;
    }

    private JLabel createdTime;
    private JLabel updatedTime;
    private JLabel state;
    private JLabel sourceBranch;
    private JLabel targetBranch;
    private JLabel author;
    private JLabel assignee;
    private JLabel description;

    public JLabel setWebUrl() {
        webUrl.addMouseListener(new MouseAdapter() {
            private void openWebPage(String uri) {
                Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
                if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
                    try {
                        desktop.browse(new URI(uri));
                    } catch (Exception ignored) {
                    }
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                openWebPage(mergeRequest.getWebUrl() + "/merge_requests/" + mergeRequest.getIid());
            }
        });
        return webUrl;
    }

    private JLabel webUrl;
}
