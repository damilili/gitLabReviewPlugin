package cn.kuwo.intellij.plugin.ui.MergeRequestDetail;

import cn.kuwo.intellij.plugin.CommonUtil;
import cn.kuwo.intellij.plugin.GitLabUtil;
import cn.kuwo.intellij.plugin.actions.*;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import org.gitlab.api.models.GitlabMergeRequest;
import org.gitlab.api.models.GitlabNote;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

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
    private JEditorPane comments;
    private JLabel commentCount;
    private GitlabMergeRequest mergeRequest;
    private ActionToolbar actionToolbar;
    private SimpleToolWindowPanel basePan;
    private static MRdetail mRdetail;
    private Thread thread;

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
        createdTime.setText(CommonUtil.sdf.format(mergeRequest.getCreatedAt()));
        updated.setText(CommonUtil.sdf.format(mergeRequest.getUpdatedAt()));
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
        refreshComments();

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

    public void refreshComments() {
        if (thread != null && (thread.isAlive() || !thread.isInterrupted())) {
            thread.interrupt();
        }
        new Task.Backgroundable(project, "Get Merge Request Comments") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                thread = Thread.currentThread();
                List<GitlabNote> discussions = GitLabUtil.getInstance(project).getDiscussions(mergeRequest);
                if (indicator.isCanceled()) {
                    thread.interrupt();
                }
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        commentCount.setText("comments(" + discussions.size() + "):");
                        StringBuffer conmmentInfo = new StringBuffer();
                        conmmentInfo.append("<html><head></head> <body>");
                        for (int i = discussions.size() - 1; i >= 0; i--) {
                            GitlabNote discussion = discussions.get(i);
                            conmmentInfo.append("<b>");
                            conmmentInfo.append(discussion.getAuthor().getName() + ": ");
                            conmmentInfo.append("</b>");
                            conmmentInfo.append(CommonUtil.sdf.format(discussion.getCreatedAt()));
                            conmmentInfo.append("<br/>");
                            conmmentInfo.append(discussion.getBody());
                            conmmentInfo.append("<br/>");
                        }
                        conmmentInfo.append("</body></html>");
                        comments.setContentType("text/html");
                        String t = conmmentInfo.toString();
                        comments.setText(t);
                    }
                }, ModalityState.any());
                thread = null;
            }
        }.queue();
    }
}
