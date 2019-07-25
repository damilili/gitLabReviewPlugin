package cn.kuwo.intellij.plugin;

import cn.kuwo.intellij.plugin.bean.Branch;
import cn.kuwo.intellij.plugin.bean.GitlabMergeRequestWrap;
import com.google.gson.Gson;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.vcs.log.VcsUser;
import git4idea.GitCommit;
import git4idea.GitUserRegistry;
import git4idea.GitUtil;
import git4idea.changes.GitChangeUtils;
import git4idea.history.GitHistoryUtils;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import git4idea.ui.branch.GitCompareBranchesDialog;
import git4idea.update.GitFetchResult;
import git4idea.update.GitFetcher;
import git4idea.util.GitCommitCompareInfo;
import org.gitlab.api.AuthMethod;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.GitlabAPIException;
import org.gitlab.api.TokenType;
import org.gitlab.api.models.*;
import org.jetbrains.annotations.NotNull;
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
        for (GitlabProject gitlabProject : getLabProjects(remote)) {
            if (CommonUtil.urlMatch(remote, gitlabProject.getSshUrl()) || CommonUtil.urlMatch(remote, gitlabProject.getHttpUrl())) {
                return gitlabProject;
            }
        }
        return null;
    }

    public List<GitlabProject> getLabProjects(String remote) {
        GitlabAPI gitlabAPI = getGitlabAPI(remote);
        try {
            return gitlabAPI.getProjects();
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
    public String getRemoteHost(String remote) {
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


    public void getAllRequest() {
        //顺便更新下用户列表
        HashMap<String, GitlabUser> memberMap = MemberManager.getInstance().getMemberList();
        ArrayList<GitlabMergeRequestWrap> gitlabMergeRequestWraps = new ArrayList<>();
        GitlabProjectManager gitlabProjectManager = GitlabProjectManager.getInstance(project);
        new Task.Backgroundable(project, "Get All Requests...") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    GitRepositoryManager manager = GitUtil.getRepositoryManager(project);
                    List<GitRepository> repositories = manager.getRepositories();
                    for (GitRepository repository : repositories) {
                        Collection<GitRemote> remotes = repository.getRemotes();
                        for (GitRemote remote : remotes) {
                            indicator.setText("get gitlabProject :" + remote.getFirstUrl());
                            if (indicator.isCanceled()) {
                                return;
                            }
                            GitlabProject gitlabProject = gitlabProjectManager.getGitlabProject(remote);
                            String remoteHost = getRemoteHost(remote.getFirstUrl());
                            String token = getToken(remote.getFirstUrl());
                            if (token != null && !token.isEmpty()) {
                                GitlabAPI gitlabAPI = getGitlabAPI(remoteHost);
                                indicator.setText("get mergeRequests :" + remoteHost);
                                if (indicator.isCanceled()) {
                                    return;
                                }
                                List<GitlabMergeRequest> mergeRequests = gitlabAPI.getMergeRequests(gitlabProject);
                                for (GitlabMergeRequest mergeRequest : mergeRequests) {
                                    if (mergeRequest.getTargetProjectId().intValue() != mergeRequest.getSourceProjectId() && mergeRequest.getProjectId().intValue() != mergeRequest.getTargetProjectId()) {
                                        continue;
                                    }
                                    GitlabMergeRequestWrap gitlabMergeRequestWrap = new GitlabMergeRequestWrap(mergeRequest);
                                    if (mergeRequest.getAssignee() != null) {
                                        memberMap.put(mergeRequest.getAssignee().getName(), mergeRequest.getAssignee());
                                    }
                                    memberMap.put(mergeRequest.getAuthor().getName(), mergeRequest.getAuthor());
                                    indicator.setText("get gitlabProject :" + remoteHost + ":" + mergeRequest.getSourceProjectId());
                                    if (indicator.isCanceled()) {
                                        return;
                                    }
                                    gitlabMergeRequestWrap.srcLabProject = gitlabProjectManager.getGitlabProject(remoteHost, mergeRequest.getSourceProjectId());
                                    String remoteLocalName = null;
                                    if (gitlabMergeRequestWrap.srcLabProject != null) {
                                        remoteLocalName = LocalRepositoryManager.getInstance(project).getRemoteName(gitlabMergeRequestWrap.srcLabProject.getHttpUrl());
                                        if (remoteLocalName == null || remoteLocalName.isEmpty()) {
                                            remoteLocalName = LocalRepositoryManager.getInstance(project).getRemoteName(gitlabMergeRequestWrap.srcLabProject.getSshUrl());
                                        }
                                    }
                                    gitlabMergeRequestWrap.srcLocalProName = remoteLocalName;
                                    indicator.setText("get gitlabProject :" + remoteHost + ":" + mergeRequest.getTargetProjectId());
                                    if (indicator.isCanceled()) {
                                        return;
                                    }
                                    if (mergeRequest.getTargetProjectId().intValue() != mergeRequest.getSourceProjectId()) {
                                        gitlabMergeRequestWrap.targetLabProject = gitlabProjectManager.getGitlabProject(remoteHost, mergeRequest.getTargetProjectId());
                                        remoteLocalName = null;
                                        if (gitlabMergeRequestWrap.targetLabProject != null) {
                                            remoteLocalName = LocalRepositoryManager.getInstance(project).getRemoteName(gitlabMergeRequestWrap.targetLabProject.getHttpUrl());
                                            if (remoteLocalName == null || remoteLocalName.isEmpty()) {
                                                remoteLocalName = LocalRepositoryManager.getInstance(project).getRemoteName(gitlabMergeRequestWrap.targetLabProject.getSshUrl());
                                            }
                                        }
                                        gitlabMergeRequestWrap.targetLocalProName = remoteLocalName;
                                    } else {
                                        gitlabMergeRequestWrap.targetLabProject = gitlabProject;
                                        gitlabMergeRequestWrap.targetLocalProName = remote.getName();
                                    }
                                    mergeRequest.setWebUrl(gitlabProject.getWebUrl());
                                    gitlabMergeRequestWraps.add(gitlabMergeRequestWrap);
                                }
                            }
                        }
                    }
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            RMListObservable.getInstance().resetList(gitlabMergeRequestWraps);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.queue();
    }

    /**
     * @return 检查是否所有的主机都配有token 0：部分未配置 -1： 全部都没配置 1：全部配置了
     */
    public int checkTokens() {
        HashSet<String> remoteSet = new HashSet();
        Collection<GitRepository> repositories = GitUtil.getRepositories(project);
        for (GitRepository repository : repositories) {
            for (GitRemote gitRemote : repository.getRemotes()) {
                remoteSet.add(getRemoteHost(gitRemote.getFirstUrl()));
            }
        }
        PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(project);
//        remoteSet.size()
        int noSetSize=0;
        for (String remote : remoteSet) {
            String value = propertiesComponent.getValue(getRemoteHost(remote));
            if (value == null || value.isEmpty()) {
                noSetSize++;
            }
        }
        if (noSetSize==0) {
            return -1;
        }
        if (remoteSet.size()>noSetSize) {
            return 0;
        }
        return 1;
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
    public GitlabMergeRequest addMergeRequest(Branch from, Branch to, int assignee, String title) {
        GitlabAPI gitlabAPI = getGitlabAPI(from.gitlabProject.getHttpUrl());
        if (gitlabAPI != null) {
            if (from != null && from.gitlabProject != null) {
                Integer id = from.gitlabProject.getId();
                try {
                    GitlabMergeRequest mergeRequest = gitlabAPI.createMergeRequest(id, from.gitlabBranch.getName(), to.gitlabBranch.getName(), assignee, title);
                    return mergeRequest;
                } catch (IOException e) {
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
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
                    }, ModalityState.any());
                }
            }
        }
        return null;
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
            GitFetchResult result = new GitFetcher(project, new EmptyProgressIndicator(), false).fetch(gitRepository.getRoot(), srcBranch.repoName, null);
            if (!result.isSuccess()) {
                GitFetcher.displayFetchResult(project, result, null, result.getErrors());
                return;
            }
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

    public void commentRequest(GitlabMergeRequest mergeRequest, String des) {
        GitlabAPI gitlabAPI = getGitlabAPI(mergeRequest.getWebUrl());
        try {
            GitlabNote note = gitlabAPI.createNote(mergeRequest, des);
            if (note != null) {
                RMCommentsObservable.getInstance().refreshComment();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<GitlabNote> getDiscussions(GitlabMergeRequest mergeRequest) {
        GitlabAPI gitlabAPI = getGitlabAPI(mergeRequest.getWebUrl());
        try {
            return gitlabAPI.getNotes(mergeRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean closeRequest(GitlabMergeRequest mergeRequest) {
        GitlabAPI gitlabAPI = getGitlabAPI(mergeRequest.getWebUrl());
        try {
            for (GitRepository gitRepository : GitUtil.getRepositories(project)) {
                for (GitRemote gitRemote : gitRepository.getRemotes()) {
                    if (CommonUtil.urlMatch(mergeRequest.getWebUrl(), gitRemote.getFirstUrl())) {
                        GitUserRegistry gitUserRegistry = GitUserRegistry.getInstance(project);
                        VirtualFile virtualFile = gitRepository.getRoot();
                        VcsUser user = gitUserRegistry.getUser(virtualFile);
                        String assigneeName = null;
                        if (mergeRequest.getAssignee() != null) {
                            assigneeName = mergeRequest.getAssignee().getName();
                        }
                        if (!mergeRequest.getAuthor().getName().equals(user.getName()) && (assigneeName == null || user == null || !assigneeName.equals(user.getName()))) {
                            Messages.showMessageDialog("You is not the assignee or the owner, please change yourself to be the assignee in the browser first.", "Message", AllIcons.Ide.Error);
                            return false;
                        }
                    }
                }
            }
            GitlabMergeRequest gitlabMergeRequest = gitlabAPI.updateMergeRequest(getLabProject(mergeRequest.getWebUrl()).getId(), mergeRequest.getId(), mergeRequest.getTargetBranch(), gitlabAPI.getUser().getId(), null, null, "close", null);
            if (gitlabMergeRequest != null) {
                StatusBar.Info.set("Close Merge Request Success", project);
            }
            instance.getAllRequest();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean acceptRequest(GitlabMergeRequest mergeRequest) {
        for (GitRepository gitRepository : GitUtil.getRepositories(project)) {
            for (GitRemote gitRemote : gitRepository.getRemotes()) {
                if (CommonUtil.urlMatch(mergeRequest.getWebUrl(), gitRemote.getFirstUrl())) {
                    GitUserRegistry gitUserRegistry = GitUserRegistry.getInstance(project);
                    VirtualFile virtualFile = gitRepository.getRoot();
                    VcsUser user = gitUserRegistry.getUser(virtualFile);
                    if (mergeRequest.getAssignee() == null || mergeRequest.getAssignee().getName() == null || user == null || !mergeRequest.getAssignee().getName().equals(user.getName())) {
                        Messages.showMessageDialog("The assignee is not you, please change you to be the assignee in the browser first.", "Message", AllIcons.Ide.Error);
                        return false;
                    }
                }
            }
        }

        GitlabAPI gitlabAPI = getGitlabAPI(mergeRequest.getWebUrl());
        try {
            GitlabMergeRequest gitlabMergeRequest = gitlabAPI.acceptMergeRequest(getLabProject(mergeRequest.getWebUrl()), mergeRequest.getId(), "Merge branch " + mergeRequest.getSourceBranch() + " into " + mergeRequest.getTargetBranch());
            if (gitlabMergeRequest != null) {
                StatusBar.Info.set("Merge Request Success", project);
            }
            instance.getAllRequest();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<GitCommit> getDiffBetweenBranchs(GitRepository gitRepository, Branch srcBranch, Branch targetBranch) {
        GitFetchResult result = new GitFetcher(project, new EmptyProgressIndicator(), false).fetch(gitRepository.getRoot(), srcBranch.repoName, null);
        if (!result.isSuccess()) {
            GitFetcher.displayFetchResult(project, result, null, result.getErrors());
            return null;
        }
        String oldRevision = srcBranch.repoName + "/" + srcBranch.gitlabBranch.getName();
        String newRevision = targetBranch.repoName + "/" + targetBranch.gitlabBranch.getName();
        List<GitCommit> diffCommits = null;
        try {
            diffCommits = GitHistoryUtils.history(project, gitRepository.getRoot(), newRevision + ".." + oldRevision);
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

    public String getMergeReqestDetailUrl(GitlabMergeRequest mergeRequest) {
        try {
            getGitlabAPI(mergeRequest.getWebUrl()).getMergeRequest(getLabProject(mergeRequest.getWebUrl()), mergeRequest.getId());
        } catch (IOException e) {
            e.printStackTrace();
        }

        String remoteHost = getRemoteHost(mergeRequest.getWebUrl());
        return "http://" + remoteHost + GitlabProject.URL + "/" + getLabProject(mergeRequest.getWebUrl()).getId() + "/merge_request/" + mergeRequest.getId();
    }

    class ErrMessage {
        ArrayList<String> message;
    }
}
