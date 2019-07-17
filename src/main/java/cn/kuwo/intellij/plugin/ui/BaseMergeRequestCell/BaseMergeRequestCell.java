package cn.kuwo.intellij.plugin.ui.BaseMergeRequestCell;

import com.intellij.icons.AllIcons;
import org.gitlab.api.models.GitlabMergeRequest;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;

public class BaseMergeRequestCell {
    public JLabel getMrTitle() {
        return mrTitle;
    }

    public JLabel getMrState() {
        return mrState;
    }

    public JLabel getMrAuthor() {
        return mrAuthor;
    }

    public JLabel getUpdateTime() {
        return updateTime;
    }

    private JLabel mrTitle;
    private JLabel mrState;
    private JLabel mrAuthor;
    private JLabel updateTime;

    public JPanel getBasePan() {
        return basePan;
    }

    private JPanel basePan;

    public JLabel getReviewer() {
        return reviewer;
    }

    public void setReviewer(JLabel reviewer) {
        this.reviewer = reviewer;
    }

    public JLabel getFromBranch() {
        return fromBranch;
    }

    public void setFromBranch(JLabel fromBranch) {
        this.fromBranch = fromBranch;
    }

    public JLabel getToBranch() {
        return toBranch;
    }

    public void setToBranch(JLabel toBranch) {
        this.toBranch = toBranch;
    }

    private JLabel reviewer;
    private JLabel fromBranch;
    private JLabel toBranch;

    public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    public static BaseMergeRequestCell getMergeRequestCell(GitlabMergeRequest mergeRequest) {
        BaseMergeRequestCell baseMergeRequestCell = new BaseMergeRequestCell();
        baseMergeRequestCell.getMrTitle().setText(mergeRequest.getTitle());
        baseMergeRequestCell.getMrAuthor().setText(mergeRequest.getAuthor().getName());
        baseMergeRequestCell.getUpdateTime().setText(sdf.format(mergeRequest.getCreatedAt()));
        baseMergeRequestCell.getMrState().setText(mergeRequest.getState());
        baseMergeRequestCell.getReviewer().setIcon(AllIcons.Vcs.Arrow_right);
        baseMergeRequestCell.getReviewer().setText(mergeRequest.getAssignee().getName());
        baseMergeRequestCell.getFromBranch().setText("| " + mergeRequest.getSourceBranch());
        baseMergeRequestCell.getToBranch().setText(mergeRequest.getTargetBranch());
        baseMergeRequestCell.getToBranch().setIcon(AllIcons.Vcs.Arrow_right);
        return baseMergeRequestCell;
    }

    public void setBackGround(int color) {
        basePan.setBackground(Color.decode(color + ""));
    }
}
