package cn.kuwo.intellij.plugin;

import cn.kuwo.intellij.plugin.bean.GitlabMergeRequestWrap;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRemote;
import org.gitlab.api.models.GitlabProject;

import java.util.ArrayList;
import java.util.HashMap;

public class GitlabProjectManager {
    private static GitlabProjectManager instance = null;
    private Project project;
    private HashMap<String, GitlabProject> projectHashMap = new HashMap<>();

    private GitlabProjectManager(Project project) {
        this.project = project;
    }

    public static GitlabProjectManager getInstance(Project project) {
        if (instance == null) {
            synchronized (GitlabProjectManager.class) {
                if (instance == null) {
                    instance = new GitlabProjectManager(project);
                }
            }
        }
        return instance;
    }

    public GitlabProject getGitlabProject(GitRemote remote) {
        return getGitlabProject(remote.getFirstUrl());
    }
    public GitlabProject getGitlabProject(String remoteHost, int projectId) {
        GitlabProject gitlabProject = projectHashMap.get(remoteHost + projectId);
        if (gitlabProject == null) {
            GitLabUtil instance = GitLabUtil.getInstance(project);
            for (GitlabProject labProject : instance.getLabProjects(remoteHost + projectId)) {
                projectHashMap.put(instance.getRemoteHost(remoteHost + projectId) + labProject.getId(), labProject);
                projectHashMap.put(labProject.getHttpUrl(), labProject);
                projectHashMap.put(labProject.getSshUrl(), labProject);
            }
            gitlabProject = projectHashMap.get(remoteHost + projectId);
        }
        return gitlabProject;
    }
    public GitlabProject getGitlabProject(String remoteUrl) {
        GitlabProject gitlabProject = projectHashMap.get(remoteUrl);
        if (gitlabProject == null) {
            GitLabUtil instance = GitLabUtil.getInstance(project);
            for (GitlabProject labProject : instance.getLabProjects(remoteUrl)) {
                projectHashMap.put(instance.getRemoteHost(remoteUrl) + labProject.getId(), labProject);
                projectHashMap.put(labProject.getHttpUrl(), labProject);
                projectHashMap.put(labProject.getSshUrl(), labProject);
            }
            gitlabProject = projectHashMap.get(remoteUrl);
        }
        return gitlabProject;
    }
}
