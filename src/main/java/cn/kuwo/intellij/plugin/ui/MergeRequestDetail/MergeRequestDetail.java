package cn.kuwo.intellij.plugin.ui.MergeRequestDetail;

import cn.kuwo.intellij.plugin.CommonUtil;
import cn.kuwo.intellij.plugin.RMListObservable;
import cn.kuwo.intellij.plugin.actions.*;
import cn.kuwo.intellij.plugin.bean.FilterBean;
import cn.kuwo.intellij.plugin.ui.BaseMergeRequestCell.BaseMergeRequestCell;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import org.gitlab.api.models.GitlabMergeRequest;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.Observable;
import java.util.Observer;

public class MergeRequestDetail {
    private Project project;
    private GitlabMergeRequest mergeRequest;
    private SimpleToolWindowPanel basePan;
    private static ActionToolbar actionToolbar;

    public static MergeRequestDetail getMergeRequestDetail(Project project, GitlabMergeRequest mergeRequest) {
        MergeRequestDetail mergeRequestCell = new MergeRequestDetail(project);
        if (mergeRequest != null) {
            mergeRequestCell.inflateView(mergeRequest);
        } else {
            mergeRequestCell.panel1.setVisible(false);
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
        panel1.setVisible(true);
        setWebUrl();
    }

    public MergeRequestDetail(Project project) {
        this.project = project;
        panel1.setVisible(false);
        RMListObservable.getInstance().addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                inflateView(mergeRequest);
            }
        });
    }

    public void refresh(GitlabMergeRequest mergeRequest) {
        inflateView(mergeRequest);
    }

    public JPanel getBasePan() {
//        panel1.setBackground(new Color(0, 0x2b, 0x2b, 0x2b));
        if (basePan == null) {
            basePan = new SimpleToolWindowPanel(true, true);
            ActionToolbar actionToolbar = getDetailToolBar(project);
            actionToolbar.setTargetComponent(panel1);
            basePan.setToolbar(actionToolbar.getComponent());
            basePan.setContent(panel1);
        }
        return basePan;
    }

    private ActionToolbar getDetailToolBar(Project project) {
        if (actionToolbar == null) {
            DefaultActionGroup toolBarActionGroup = (DefaultActionGroup) ActionManager.getInstance().getAction("GitMergeRequest.Detail");
            //diff
            DiffAction diffAction = new DiffAction();
            toolBarActionGroup.add(diffAction);
            // comment
            CommentAction commentAction = new CommentAction();
            toolBarActionGroup.add(commentAction);
            //close
            CloseAction closeAction = new CloseAction();
            toolBarActionGroup.add(closeAction);
            //accept
            AcceptAction acceptAction = new AcceptAction();
            toolBarActionGroup.add(acceptAction);
            actionToolbar = ActionManager.getInstance().createActionToolbar("GitMergeRequest.Detail", toolBarActionGroup, true);
        }
        for (AnAction anAction : actionToolbar.getActions()) {
            if (anAction instanceof RequestDetailAction) {
                ((RequestDetailAction) anAction).setRequest(mergeRequest);
            }
        }
        return actionToolbar;
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
            @Override
            public void mouseClicked(MouseEvent e) {
                CommonUtil.openWebPage(mergeRequest.getWebUrl() + "/merge_requests/" + mergeRequest.getIid());
            }
        });
        return webUrl;
    }

    private JLabel webUrl;
}
