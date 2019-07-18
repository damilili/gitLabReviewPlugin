package cn.kuwo.intellij.plugin.actions;

import cn.kuwo.intellij.plugin.MemberManager;
import cn.kuwo.intellij.plugin.RMListObservable;
import cn.kuwo.intellij.plugin.bean.FilterBean;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import com.intellij.vcs.log.VcsUser;
import git4idea.GitUserRegistry;
import git4idea.GitUtil;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.gitlab.api.models.GitlabUser;

import javax.swing.*;
import java.util.Observable;
import java.util.Observer;

public final class OwnerPopupAction extends BasePopupAction {
    private final Project project;

    public OwnerPopupAction(Project project, String filterName) {
        super(filterName);
        this.project = project;
        RMListObservable.getInstance().addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                String reviewer = FilterBean.getInstance().getOwner();
                if (reviewer != null && reviewer != "") {
                    updateFilterValueLabel(reviewer);
                } else {
                    updateFilterValueLabel("All");
                }
            }
        });
        updateFilterValueLabel("All");
    }

    @Override
    protected void createActions(Consumer<AnAction> actionConsumer) {
        actionConsumer.consume(new DumbAwareAction("All") {
            @Override
            public void actionPerformed(AnActionEvent e) {
                RMListObservable.getInstance().filterOwner("");
            }
        });
//        addMeItem(actionConsumer, project);
        for (GitlabUser user : MemberManager.getInstance().getMemberList().values()) {
            actionConsumer.consume(new DumbAwareAction(user.getName()) {
                @Override
                public void actionPerformed(AnActionEvent e) {
                    RMListObservable.getInstance().filterReviewer(user.getName());
                }
            });
        }
        selectUserTextArea = new JTextArea();
        selectOkAction = buildOkAction();
        addSelectItem(actionConsumer);
    }

    protected void addMeItem(Consumer<AnAction> actionConsumer,Project project) {
        actionConsumer.consume(new DumbAwareAction("Me") {
            @Override
            public void actionPerformed(AnActionEvent e) {
                GitRepositoryManager repositoryManager = GitUtil.getRepositoryManager(project);
                for (GitRepository gitRepository : repositoryManager.getRepositories()) {
                    GitUserRegistry gitUserRegistry = GitUserRegistry.getInstance(project);
                    VirtualFile virtualFile = gitRepository.getRoot();
                    VcsUser user = gitUserRegistry.getUser(virtualFile);
                    String name = user.getName();
                    FilterBean.getInstance().setOwner(name);
                    RMListObservable.getInstance().filterOwner(name);
                    break;
                }
            }
        });
    }

    protected AnAction buildOkAction() {
        return new AnAction() {
            public void actionPerformed(AnActionEvent e) {
                popup.closeOk(e.getInputEvent());
                String newText = selectUserTextArea.getText().trim();
                if (newText.isEmpty()) {
                    return;
                }
                if (!Comparing.equal(newText, getFilterValueLabel().getText())) {
                    RMListObservable.getInstance().filterOwner(newText);
                }
            }
        };
    }
}