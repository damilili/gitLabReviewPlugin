package cn.kuwo.intellij.plugin.bean;

import org.gitlab.api.models.GitlabMergeRequest;
import org.gitlab.api.models.GitlabProject;

public class GitlabMergeRequestWrap {
    public GitlabProject srcLabProject;
    public GitlabProject targetLabProject;
    public GitlabMergeRequest gitlabMergeRequest;

    public GitlabMergeRequestWrap(GitlabMergeRequest mergeRequest) {
        this.gitlabMergeRequest = mergeRequest;
    }
}
