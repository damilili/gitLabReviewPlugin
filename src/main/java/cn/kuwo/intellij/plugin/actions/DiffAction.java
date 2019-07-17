package cn.kuwo.intellij.plugin.actions;

import cn.kuwo.intellij.plugin.GitLabUtil;
import cn.kuwo.intellij.plugin.bean.Branch;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import git4idea.GitUtil;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabProject;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public final class DiffAction extends RequestDetailAction {

    public DiffAction() {
        super("", "Show differnet between the two branches.", AllIcons.Diff.Diff);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        GitLabUtil instance = GitLabUtil.getInstance(e.getProject());
        Branch branchSrc = new Branch();
        Branch branchTraget = new Branch();
        GitlabProject srcLabProject = null, targetProject = null;
        GitlabAPI gitlabAPI = instance.getGitlabAPI(mergeRequest.getWebUrl());
        try {
            srcLabProject = gitlabAPI.getProject(mergeRequest.getSourceProjectId());
            if (mergeRequest.getSourceProjectId().intValue() != mergeRequest.getTargetProjectId()) {
                targetProject = gitlabAPI.getProject(mergeRequest.getTargetProjectId());
            } else {
                targetProject = srcLabProject;
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        GitRepository gitRepository = null;
        out:
        for (GitRepository temGitRepository : GitUtil.getRepositories(e.getProject())) {
            for (GitRemote gitRemote : temGitRepository.getRemotes()) {
                List<String> urls = gitRemote.getUrls();
                for (String url : urls) {
                    if (url.contains(mergeRequest.getWebUrl())) {
                        branchSrc.repoName = gitRemote.getName();
                        branchTraget.repoName = gitRemote.getName();
                        gitRepository = temGitRepository;
                        break out;
                    }
                }
            }
        }
        if (gitRepository != null && branchSrc.repoName != null && !branchSrc.repoName.isEmpty()) {
            branchSrc.gitlabProject = srcLabProject;
            branchTraget.gitlabProject = targetProject;
            branchSrc.gitlabBranch = new GitlabBranch();
            branchTraget.gitlabBranch = new GitlabBranch();
            branchSrc.gitlabBranch.setName(mergeRequest.getSourceBranch());
            branchTraget.gitlabBranch.setName(mergeRequest.getTargetBranch());
            GitRepository finalGitRepository = gitRepository;
            new Task.Backgroundable(e.getProject(), "Get Different Between Branchs...") {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    instance.showDifBetweenBranchs(finalGitRepository, branchSrc, branchTraget);
                }
            }.queue();
        }
    }
}