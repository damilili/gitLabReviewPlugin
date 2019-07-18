package cn.kuwo.intellij.plugin.ui;

import cn.kuwo.intellij.plugin.CommonUtil;
import cn.kuwo.intellij.plugin.LocalRepositoryManager;
import cn.kuwo.intellij.plugin.RMListObservable;
import cn.kuwo.intellij.plugin.actions.*;
import cn.kuwo.intellij.plugin.bean.Branch;
import cn.kuwo.intellij.plugin.bean.GitlabMergeRequestWrap;
import cn.kuwo.intellij.plugin.ui.BaseMergeRequestCell.BaseMergeRequestCell;
import cn.kuwo.intellij.plugin.ui.MergeRequestDetail.MergeRequestDetail;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.changes.ui.ChangesViewContentProvider;
import com.intellij.openapi.vcs.ui.SearchFieldAction;
import com.intellij.ui.JBSplitter;
import com.intellij.util.NotNullFunction;
import git4idea.GitVcs;
import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabMergeRequest;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class MergeRequestContent implements ChangesViewContentProvider {

    private Project project;
    private JList requestList;
    private JPanel panel1;
    private DataModel dataModel;
    private Observer requestListObserver = new Observer() {
        @Override
        public void update(Observable o, Object arg) {
            if (requestList != null) {
                if (arg != null && arg instanceof List) {
                    dataModel.arrayList = (List<GitlabMergeRequestWrap>) arg;
                } else {
                    dataModel.arrayList = null;
                }
                requestList.clearSelection();
                requestList.updateUI();
            }
        }
    };
    private JBSplitter horizontalSplitter;

    public MergeRequestContent(Project project) {
        this.project = project;
    }

    @Override
    public JComponent initContent() {
        horizontalSplitter = new JBSplitter(false, 0.7f);
        SimpleToolWindowPanel basePan = new SimpleToolWindowPanel(true, true);
        ActionToolbar actionToolbar = getToolBar(project);
        actionToolbar.setTargetComponent(requestList);
        basePan.setToolbar(actionToolbar.getComponent());
        basePan.setContent(panel1);
        horizontalSplitter.setFirstComponent(basePan);
        dataModel = new DataModel();
        requestList.setModel(dataModel);
        requestList.setCellRenderer(new MRCommentCellRender());
        requestList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    GitlabMergeRequestWrap gitlabMergeRequestWrap = dataModel.getElementAt(requestList.getSelectedIndex());
                    GitlabMergeRequest gitlabMergeRequest = gitlabMergeRequestWrap.gitlabMergeRequest;
                    Branch srcBranch = new Branch();
                    if (gitlabMergeRequestWrap.srcLabProject != null) {
                        srcBranch.repoName = gitlabMergeRequestWrap.srcLocalProName;
                        srcBranch.gitlabBranch = new GitlabBranch();
                        srcBranch.gitlabBranch.setName(gitlabMergeRequest.getSourceBranch());
                    } else {
                        if (Messages.YES == Messages.showYesNoDialog("The request comes from an unknown branch  and the\nbrowser is about to be opened to view the details.", "Message", "Yes", "No", AllIcons.Ide.Warning_notifications)) {
                            CommonUtil.openWebPage(gitlabMergeRequest.getWebUrl() + "/merge_requests/" + gitlabMergeRequest.getIid());
                        }
                        return;
                    }
                    Branch targetBranch = new Branch();
                    if (gitlabMergeRequestWrap.targetLabProject != null) {
                        targetBranch.repoName = gitlabMergeRequestWrap.targetLocalProName;
                        targetBranch.gitlabBranch = new GitlabBranch();
                        targetBranch.gitlabBranch.setName(gitlabMergeRequest.getTargetBranch());
                    } else {
                        CommonUtil.openWebPage(gitlabMergeRequest.getWebUrl() + "/merge_requests/" + gitlabMergeRequest.getIid());
                        return;
                    }
                    MergeRequestDetail mergeRequestDetail = MergeRequestDetail.getMergeRequestDetail(project, gitlabMergeRequest);
                    horizontalSplitter.setSecondComponent(mergeRequestDetail.getBasePan());
                }
            }
        });
        requestList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {

            }
        });
        RMListObservable.getInstance().addObserver(requestListObserver);
        return horizontalSplitter;
    }

    @Override
    public void disposeContent() {
        RMListObservable.getInstance().deleteObserver(requestListObserver);
    }

    private ActionToolbar getToolBar(Project project) {
        DefaultActionGroup toolBarActionGroup = (DefaultActionGroup) ActionManager.getInstance().getAction("GitMergeRequest.Toolbar");
        SearchFieldAction searchFieldAction = new SearchFieldAction("") {
            @Override
            public void actionPerformed(AnActionEvent event) {
                RMListObservable.getInstance().filterSearchKey(getText().trim());
            }
        };
        toolBarActionGroup.add(searchFieldAction);
//        状态
        StatusPopupAction branchPopupAction = new StatusPopupAction(project, "Status");
        toolBarActionGroup.add(branchPopupAction);
//        原始分支
        FromBranchPopupAction fromBranchPopupAction = new FromBranchPopupAction(project, "FromBranch");
        toolBarActionGroup.add(fromBranchPopupAction);
//        目标分支
        ToBranchPopupAction toBranchPopupAction = new ToBranchPopupAction(project, "ToBranch");
        toolBarActionGroup.add(toBranchPopupAction);
//        检查者
        ReviewerPopupAction reviewerPopupAction = new ReviewerPopupAction(project, "Reviewer");
        toolBarActionGroup.add(reviewerPopupAction);
//        发起者
        OwnerPopupAction ownerPopupAction = new OwnerPopupAction(project, "Owner");
        toolBarActionGroup.add(ownerPopupAction);
        //分割线
        toolBarActionGroup.addSeparator();
//        刷新
        RefreshAction refreshAction = new RefreshAction(project);
        toolBarActionGroup.add(refreshAction);
        return ActionManager.getInstance().createActionToolbar("GitMergeRequest.Toolbar", toolBarActionGroup, true);
    }


    public static class VisibilityPredicate implements NotNullFunction<Project, Boolean> {
        @NotNull
        @Override
        public Boolean fun(@NotNull Project project) {
            return ProjectLevelVcsManager.getInstance(project).checkVcsIsActive(GitVcs.NAME);
        }
    }

    public class DataModel extends AbstractListModel {
        private List<GitlabMergeRequestWrap> arrayList;

        @Override
        public GitlabMergeRequestWrap getElementAt(int index) {
            if (arrayList == null) {
                return null;
            }
            return arrayList.get(index);
        }

        @Override
        public int getSize() {
            return arrayList == null ? 0 : arrayList.size();
        }
    }

    public class MRCommentCellRender extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            GitlabMergeRequestWrap mergeRequest = value instanceof GitlabMergeRequestWrap ? ((GitlabMergeRequestWrap) value) : null;
            if (mergeRequest != null) {
                BaseMergeRequestCell mergeRequestCell = BaseMergeRequestCell.getMergeRequestCell(mergeRequest);
                if (requestList.getSelectedIndex() == index) {
                    mergeRequestCell.setBackGround(0xff4B6EAF);
                } else {
                    mergeRequestCell.setBackGround(0xff3C3F41);
                }
                return mergeRequestCell.getBasePan();
            }
            return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        }
    }

}
