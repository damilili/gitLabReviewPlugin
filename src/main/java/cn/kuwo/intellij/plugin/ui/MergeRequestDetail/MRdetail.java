package cn.kuwo.intellij.plugin.ui.MergeRequestDetail;

import cn.kuwo.intellij.plugin.CommonUtil;
import cn.kuwo.intellij.plugin.actions.*;
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

public class MRdetail {
    private Project project;
    private JLabel title;
    private JLabel webUrl;
    private JLabel state;
    private JLabel createdTime;
    private JLabel updated;
    private JLabel srcBranch;
    private JLabel description;
    private JLabel author;
    private JLabel assignee;
    private JLabel targetBranch;
    public JPanel base;
    private GitlabMergeRequest mergeRequest;
    private ActionToolbar actionToolbar;
    private SimpleToolWindowPanel basePan;
    private static MRdetail mRdetail;

    public MRdetail(Project project) {
        this.project = project;
    }

    public static MRdetail getMergeRequestDetail(Project project, GitlabMergeRequest mergeRequest) {
        if (mRdetail == null) {
            mRdetail = new MRdetail(project);
        }
        mRdetail.inflateView(mergeRequest);
        return mRdetail;
    }

    private void inflateView(GitlabMergeRequest mergeRequest) {
        this.mergeRequest = mergeRequest;
        String title = "<html>" + mergeRequest.getTitle() + "</html>";
        this.title.setText(title);
        this.assignee.setText(mergeRequest.getAssignee() == null ? "<html><font color=\"red\">Unspecified</font></html>" : mergeRequest.getAssignee().getName());
        author.setText(mergeRequest.getAuthor().getName());
        createdTime.setText(BaseMergeRequestCell.sdf.format(mergeRequest.getCreatedAt()));
        updated.setText(BaseMergeRequestCell.sdf.format(mergeRequest.getUpdatedAt()));
        srcBranch.setText(mergeRequest.getSourceBranch());
        targetBranch.setText(mergeRequest.getTargetBranch());
        description.setText(mergeRequest.getDescription());
        state.setText(mergeRequest.getState());
        setWebUrl();
    }

    public JPanel getBasePan() {
        if (basePan == null) {
            basePan = new SimpleToolWindowPanel(true, true);
            ActionToolbar actionToolbar = getDetailToolBar(project);
            actionToolbar.setTargetComponent(base);
            basePan.setToolbar(actionToolbar.getComponent());
            basePan.setContent(base);
        }
        for (AnAction anAction : actionToolbar.getActions()) {
            if (anAction instanceof RequestDetailAction) {
                ((RequestDetailAction) anAction).setRequest(mergeRequest);
            }
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
        return actionToolbar;
    }

    public void setWebUrl() {
        if (webUrl.getMouseListeners() == null || webUrl.getMouseListeners().length == 0) {
            webUrl.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    CommonUtil.openWebPage(mergeRequest.getWebUrl() + "/merge_requests/" + mergeRequest.getIid());
                }
            });
        }
    }

}
