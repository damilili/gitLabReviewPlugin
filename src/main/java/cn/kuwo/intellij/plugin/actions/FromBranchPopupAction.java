package cn.kuwo.intellij.plugin.actions;

import cn.kuwo.intellij.plugin.RMListObservable;
import cn.kuwo.intellij.plugin.bean.FilterBean;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.util.Consumer;
import git4idea.GitRemoteBranch;
import git4idea.GitUtil;
import git4idea.branch.GitBranchesCollection;
import git4idea.repo.GitRepository;

import java.util.Collection;
import java.util.List;
import java.util.Observable;
import java.util.Observer;


public final class FromBranchPopupAction extends BasePopupAction {
    private final Project project;

    public FromBranchPopupAction(Project project, String filterName) {
        super(filterName);
        this.project = project;
        updateFilterValueLabel("All");
        RMListObservable.getInstance().addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                String branch = FilterBean.getInstance().getToBranch();
                if (branch != null && branch != "") {
                    updateFilterValueLabel(branch);
                } else {
                    updateFilterValueLabel("All");
                }
            }
        });
    }

    @Override
    protected void createActions(Consumer<AnAction> actionConsumer) {
        actionConsumer.consume(new DumbAwareAction("All") {
            @Override
            public void actionPerformed(AnActionEvent e) {
                RMListObservable.getInstance().filterFromBranch("");
            }
        });
        List<GitRepository> repositories = GitUtil.getRepositoryManager(project).getRepositories();
        for (GitRepository gitRepository : repositories) {
            GitBranchesCollection branches = gitRepository.getBranches();
            Collection<GitRemoteBranch> remoteBranches = branches.getRemoteBranches();
            for (GitRemoteBranch remoteBranch : remoteBranches) {
                actionConsumer.consume(new DumbAwareAction(remoteBranch.getName()) {
                    @Override
                    public void actionPerformed(AnActionEvent e) {
                        RMListObservable.getInstance().filterFromBranch(remoteBranch.getName());
                    }
                });
            }
        }
    }
}