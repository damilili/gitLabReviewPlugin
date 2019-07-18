package cn.kuwo.intellij.plugin;

import com.intellij.openapi.project.Project;
import git4idea.GitUtil;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class LocalRepositoryManager {
    private static LocalRepositoryManager instance = null;
    private Project project;

    private LocalRepositoryManager(Project project) {
        this.project = project;
    }

    public static LocalRepositoryManager getInstance(Project project) {
        if (instance == null) {
            synchronized (LocalRepositoryManager.class) {
                if (instance == null) {
                    instance = new LocalRepositoryManager(project);
                }
            }
        }
        return instance;
    }

    public List<GitRepository> getRepositorys() {
        Collection<GitRepository> repositories = GitUtil.getRepositories(project);
        GitRepository[] gitRepositories = repositories.toArray(new GitRepository[repositories.size()]);
        return Arrays.asList(gitRepositories);
    }

    public String getRemoteName(String url) {
        for (GitRepository gitRepository : getRepositorys()) {
            Collection<GitRemote> remotes = gitRepository.getRemotes();
            for (GitRemote remote : remotes) {
                if (CommonUtil.urlMatch(remote.getFirstUrl(), url)) {
                    return remote.getName();
                }
            }
        }
        return null;
    }

}
