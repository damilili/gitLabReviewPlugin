package cn.kuwo.intellij.plugin;

import cn.kuwo.intellij.plugin.bean.Branch;
import com.google.gson.Gson;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitLabUtil {
    private static GitLabUtil instance;
    private Project project;
    private Map<String, GitlabAPI> gitlabAPIs = new HashMap<>();

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
        Collection<GitRepository> repositories = GitUtil.getRepositories(project);
        repositories.forEach(repo -> repo.getRemotes().forEach(remote -> {
            String firstUrl = remote.getFirstUrl();
            String remoteHost = getRemoteHost(firstUrl);
            GitlabAPI gitlabAPI = GitlabAPI.connect("https://" + remoteHost, getToken(remoteHost), TokenType.PRIVATE_TOKEN, AuthMethod.URL_PARAMETER);
            gitlabAPIs.put(remoteHost, gitlabAPI);
        }));
    }

    public void refreshToken(String remoteHost) {
        GitlabAPI gitlabAPI = GitlabAPI.connect("https://" + remoteHost, getToken(remoteHost), TokenType.PRIVATE_TOKEN, AuthMethod.URL_PARAMETER);
        gitlabAPIs.put(remoteHost, gitlabAPI);
    }

    public GitlabAPI getGitlabAPI(String urlOrHost) {
        String host = getRemoteHost(urlOrHost);
        GitlabAPI gitlabAPI = gitlabAPIs.get(host);
        if (gitlabAPI == null) {
            gitlabAPI = GitlabAPI.connect("https://" + urlOrHost, getToken(urlOrHost), TokenType.PRIVATE_TOKEN, AuthMethod.URL_PARAMETER);
            gitlabAPIs.put(host, gitlabAPI);
        }
        return gitlabAPI;
    }

    public GitlabProject getLabProject(String remote) {
        GitlabAPI gitlabAPI = getGitlabAPI(remote);
        try {
            List<GitlabProject> memberProjects = gitlabAPI.getProjects();
            for (GitlabProject memberProject : memberProjects) {
                if (urlMatch(remote, memberProject.getSshUrl()) || urlMatch(remote, memberProject.getHttpUrl())) {
                    return memberProject;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Error error) {
            Throwable cause = error.getCause();
            String message = cause.getMessage();
            if (message != null && message.contains("401")) {
                Messages.showMessageDialog("Unauthorized", "Create Merge Request Fail", AllIcons.Ide.Error);
            } else {
                Messages.showMessageDialog(message, "Create Merge Request Fail", AllIcons.Ide.Error);
            }
        }
        return null;
    }

    @Nullable
    private String getRemoteHost(String remote) {
        Pattern compile = Pattern.compile("(?<=(http(s)?://))[\\w|\\.]*");
        Matcher matcher = compile.matcher(remote);
        String remoteHost = remote;
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
                        gitlabMergeRequests.addAll(getGitlabAPI(remote.getFirstUrl()).getMergeRequests(labProject));
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
        GitlabAPI gitlabAPI = getGitlabAPI(from.gitlabProject.getHttpUrl());
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
                    List<GitlabBranch> branches = getGitlabAPI(remote.getFirstUrl()).getBranches(labProject.getId());
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

    public List<Branch> getRemoteBranches(GitRepository gitRepository) {
        ArrayList<Branch> result = new ArrayList<>();
        HashSet<String> tem = new HashSet<>();
        Collection<GitRemote> remotes1 = gitRepository.getRemotes();
        GitRemote[] remoteArr = remotes1.toArray(new GitRemote[remotes1.size()]);
        for (int i = remoteArr.length - 1; i >= 0; i--) {
            GitRemote gitRemote = remoteArr[i];
            if (tem.contains(gitRemote.getFirstUrl())) {
                continue;
            } else {
                tem.add(gitRemote.getFirstUrl());
            }
            GitlabAPI gitlabAPI = getGitlabAPI(gitRemote.getFirstUrl());
            GitlabProject labProject = getLabProject(gitRemote.getFirstUrl());
            String name = gitRemote.getName();
            if (labProject != null) {
                try {
                    List<GitlabBranch> branches = gitlabAPI.getBranches(labProject);
                    for (GitlabBranch branch : branches) {
                        Branch branch1 = new Branch();
                        branch1.gitlabBranch = branch;
                        branch1.repoName = name;
                        branch1.gitlabProject = labProject;
                        result.add(branch1);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    public GitRemoteBranch getCurLocalTrackedRemoteBranch() {
        for (GitRepository repository : GitUtil.getRepositories(project)) {
            GitRemoteBranch trackedBranch = repository.getCurrentBranch().findTrackedBranch(repository);
            return trackedBranch;
        }
        return null;
    }

    public List<GitlabProjectMember> getGroupUser(GitlabProject labProject) {
        try {
            return getGitlabAPI(labProject.getHttpUrl()).getProjectMembers(labProject);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void showDifBetweenBranchs(GitRepository gitRepository, Branch srcBranch, Branch targetBranch) {
        try {
            String oldRevision = srcBranch.repoName + "/" + srcBranch.gitlabBranch.getName();
            String newRevision = targetBranch.repoName + "/" + targetBranch.gitlabBranch.getName();
            List<GitCommit> commits1 = GitHistoryUtils.history(project, gitRepository.getRoot(), newRevision + ".." + oldRevision);
            List<GitCommit> commits2 = GitHistoryUtils.history(project, gitRepository.getRoot(), oldRevision + ".." + newRevision);
            Collection<Change> diff = GitChangeUtils.getDiff(project, gitRepository.getRoot(), oldRevision, newRevision, null);
            GitCommitCompareInfo info = new GitCommitCompareInfo(GitCommitCompareInfo.InfoType.BOTH);
            info.put(gitRepository, diff);
            info.put(gitRepository, Couple.of(commits1, commits2));
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                @Override
                public void run() {
                    GitCompareBranchesDialog dialog = new GitCompareBranchesDialog(project, oldRevision, newRevision, info, gitRepository, true);
                    dialog.show();
                }
            });
        } catch (VcsException e) {
            e.printStackTrace();
        }
    }

    public List<GitCommit> getDiffBetweenBranchs(GitRepository gitRepository, Branch srcBranch, Branch targetBranch) {
        String oldRevision = srcBranch.repoName + "/" + srcBranch.gitlabBranch.getName();
        String newRevision = targetBranch.repoName + "/" + targetBranch.gitlabBranch.getName();
        List<GitCommit> diffCommits = null;
        try {
            diffCommits = GitHistoryUtils.history(project, gitRepository.getRoot(),  newRevision+ ".." + oldRevision);
        } catch (VcsException e) {
            e.printStackTrace();
        }
        return diffCommits;
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
