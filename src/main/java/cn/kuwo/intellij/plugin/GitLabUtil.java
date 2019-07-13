package cn.kuwo.intellij.plugin;

import cn.kuwo.intellij.plugin.bean.Branch;
import com.google.gson.Gson;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import git4idea.GitCommit;
import git4idea.GitRemoteBranch;
import git4idea.GitUtil;
import git4idea.changes.GitChangeUtils;
import git4idea.history.GitHistoryUtils;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import git4idea.ui.branch.GitCompareBranchesDialog;
import git4idea.util.GitCommitCompareInfo;
import org.apache.commons.lang.StringUtils;
import org.gitlab.api.AuthMethod;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.GitlabAPIException;
import org.gitlab.api.TokenType;
import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabMergeRequest;
import org.gitlab.api.models.GitlabProject;
import org.gitlab.api.models.GitlabProjectMember;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitLabUtil {
    private static GitLabUtil instance;
    private Project project;
    private GitlabAPI gitlabAPI;

    private GitLabUtil(Project project) {
        this.project = project;
        init();
    }

    public static GitLabUtil getInstance(Project project) {
        if (instance == null) {
            synchronized (GitLabUtil.class) {
                if (instance == null) {
                    instance = new GitLabUtil(project);
                }
            }
        }
        return instance;
    }

    private void init() {
    }

    private GitlabProject getLabProject(String remote) {
        String remoteHost = getRemoteHost(remote);
        if (remoteHost != null) {
            if (gitlabAPI == null) {
                gitlabAPI = GitlabAPI.connect("https://" + remoteHost, getToken(remote), TokenType.PRIVATE_TOKEN, AuthMethod.URL_PARAMETER);
            }
            GitRepositoryManager repositoryManager = GitUtil.getRepositoryManager(project);
            repositoryManager.getRepositoryForFile(project.getBaseDir());
            try {
                List<GitlabProject> memberProjects = gitlabAPI.getProjects();
                for (GitlabProject memberProject : memberProjects) {
                    if (urlMatch(remote, memberProject.getSshUrl()) || urlMatch(remote, memberProject.getHttpUrl())) {
                        return memberProject;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Nullable
    private String getRemoteHost(String remote) {
        Pattern compile = Pattern.compile("(?<=(http(s)?://))[\\w|\\.]*");
        Matcher matcher = compile.matcher(remote);
        String remoteHost = null;
        if (matcher.find()) {
            remoteHost = matcher.group();
        } else {
            compile = Pattern.compile("(?<=(git@))[\\w|\\.|\\d]*");
            matcher = compile.matcher(remote);
            if (matcher.find()) {
                remoteHost = matcher.group();
            }
        }
        return remoteHost;
    }

    public List getMergedMergeRequests() {
        return null;
    }

    public List getAllRequest() {
        ArrayList<GitlabMergeRequest> gitlabMergeRequests = new ArrayList<>();
        try {
            GitRepositoryManager manager = GitUtil.getRepositoryManager(project);
            List<GitRepository> repositories = manager.getRepositories();
            for (GitRepository repository : repositories) {
                Collection<GitRemote> remotes = repository.getRemotes();
                for (GitRemote remote : remotes) {
                    String token = getToken(remote.getFirstUrl());
                    if (token != null && !token.isEmpty()) {
                        GitlabProject labProject = getLabProject(remote.getFirstUrl());
                        if (labProject != null) {
                            List<GitlabMergeRequest> mergedMergeRequests = gitlabAPI.getMergeRequests(labProject);
                            gitlabMergeRequests.addAll(mergedMergeRequests);
                        }
                    }
                }
            }
            System.out.println("size==" + gitlabMergeRequests.size());
            RMListObservable.getInstance().refreshList(gitlabMergeRequests);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return gitlabMergeRequests;
    }

    private String getToken(String remoteUrl) {
        PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(project);
        return propertiesComponent.getValue(getRemoteHost(remoteUrl));
    }

    /**
     * @param from     从哪个分支合并
     * @param to       合并到哪个分支
     * @param assignee 指定谁去审核
     * @param title    标题
     * @return 是否成功
     */
    public boolean addMergeRequest(Branch from, Branch to, int assignee, String title) {
        if (gitlabAPI == null) {
            Collection<GitRepository> repositories = GitUtil.getRepositories(project);
            for (GitRepository repository : repositories) {
                for (GitRemote gitRemote : repository.getRemotes()) {
                    if (gitRemote.getName() != null && gitRemote.getName().equals(from.repoName)) {
                        String remoteHost = getRemoteHost(gitRemote.getFirstUrl());
                        gitlabAPI = GitlabAPI.connect("https://" + remoteHost, getToken(gitRemote.getFirstUrl()), TokenType.PRIVATE_TOKEN, AuthMethod.URL_PARAMETER);
                        break;
                    }
                }
            }
        }
        if (gitlabAPI != null) {
            if (from != null && from.gitlabProject != null) {
                Integer id = from.gitlabProject.getId();
                try {
                    GitlabMergeRequest mergeRequest = gitlabAPI.createMergeRequest(id, from.gitlabBranch.getName(), to.gitlabBranch.getName(), assignee, title);
                    if (mergeRequest.getId() != 0) {
                        return true;
                    }
                } catch (IOException e) {
                    String message = "Create Merge Request Fail";
                    e.printStackTrace();
                    if (e instanceof GitlabAPIException) {
                        Gson gson = new Gson();
                        ErrMessage errMessage = gson.fromJson(e.getMessage(), ErrMessage.class);
                        if (errMessage != null && errMessage.message != null && errMessage.message.size() > 0) {
                            message = errMessage.message.get(0);
                        }
                    }
                    Messages.showMessageDialog(message, "Create Merge Request Fail", AllIcons.Ide.Error);
                }
            }
        }
        return false;
    }

    private boolean urlMatch(String remoteUrl, String apiUrl) {
        String formattedRemoteUrl = remoteUrl.trim();
        String formattedApiUrl = apiUrl.trim();
        formattedRemoteUrl = formattedRemoteUrl.replace("https://", "");
        formattedRemoteUrl = formattedRemoteUrl.replace("http://", "");
        return StringUtils.isNotBlank(formattedApiUrl) && StringUtils.isNotBlank(formattedRemoteUrl) && formattedApiUrl.toLowerCase().contains(formattedRemoteUrl.toLowerCase());
    }

    public ArrayList<Branch> getRemoteBranches() {
        ArrayList<Branch> result = new ArrayList<>();
        GitRepositoryManager manager = GitUtil.getRepositoryManager(project);
        List<GitRepository> repositories = manager.getRepositories();
        for (GitRepository repository : repositories) {
            Collection<GitRemote> remotes = repository.getRemotes();
            for (GitRemote remote : remotes) {
                String name = remote.getName();
                GitlabProject labProject = getLabProject(remote.getFirstUrl());
                try {
                    List<GitlabBranch> branches = gitlabAPI.getBranches(labProject.getId());
                    for (GitlabBranch branch : branches) {
                        Branch tem = new Branch();
                        tem.gitlabProject = labProject;
                        tem.gitlabBranch = branch;
                        tem.repoName = name;
                        result.add(tem);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("result==" + result.size());
        return result;
    }

    public GitRemoteBranch getCurLocalTrackedRemoteBranch() {
        for (GitRepository repository : GitUtil.getRepositories(project)) {
            GitRemoteBranch trackedBranch = repository.getCurrentBranch().findTrackedBranch(repository);
            return trackedBranch;
        }
        return null;
    }

    public List<GitlabProjectMember> getGroupUser(String remoteRepoName) {
        for (GitRepository gitRepository : GitUtil.getRepositories(project)) {
            for (GitRemote gitRemote : gitRepository.getRemotes()) {
                if (gitRemote.getName().equals(remoteRepoName)) {
                    GitlabProject labProject = getLabProject(gitRemote.getFirstUrl());
                    try {
                        return gitlabAPI.getProjectMembers(labProject);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }

    public void getDifBetweenBranchs(Branch srcBranch, Branch targetBranch) {
        Collection<GitRepository> repositories = GitUtil.getRepositories(project);
        for (GitRepository repository : repositories) {
            try {
                String oldRevision = srcBranch.repoName + "/" + srcBranch.gitlabBranch.getName();
                String newRevision = targetBranch.repoName + "/" + targetBranch.gitlabBranch.getName();
                Collection<Change> diff = GitChangeUtils.getDiff(project, repository.getRoot(), oldRevision, newRevision, null);
                GitCommitCompareInfo info = new GitCommitCompareInfo(GitCommitCompareInfo.InfoType.BOTH);
                info.put(repository, diff);
                List<GitCommit> commits1 = GitHistoryUtils.history(project, repository.getRoot(), ".." + oldRevision);
                List<GitCommit> commits2 = GitHistoryUtils.history(project, repository.getRoot(), newRevision + "..");
                info.put(repository, Couple.of(commits2, commits1));
                GitCompareBranchesDialog dialog = new GitCompareBranchesDialog(project, oldRevision, newRevision, info, repository, true);
                dialog.show();
            } catch (VcsException e) {
                e.printStackTrace();
            }
        }
    }

    public String getLabProById(int id) {
        GitRepositoryManager repositoryManager = GitUtil.getRepositoryManager(project);
        List<GitRepository> repositories = repositoryManager.getRepositories();
        for (GitRepository repository : repositories) {
            Collection<GitRemote> remotes = repository.getRemotes();
            for (GitRemote remote : remotes) {
                GitlabProject labProject = getLabProject(remote.getFirstUrl());
            }
        }
        return null;
    }

    class ErrMessage {
        ArrayList<String> message;
    }
}
