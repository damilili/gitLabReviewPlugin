package cn.kuwo.intellij.plugin;

import cn.kuwo.intellij.plugin.actions.StatusPopupAction;
import cn.kuwo.intellij.plugin.bean.FilterBean;
import cn.kuwo.intellij.plugin.bean.GitlabMergeRequestWrap;
import org.gitlab.api.models.GitlabMergeRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

public class RMListObservable extends Observable {
    private static RMListObservable instance;
    private ArrayList<GitlabMergeRequestWrap> gitlabMergeRequests;

    private RMListObservable() {

    }

    public static RMListObservable getInstance() {
        if (instance == null) {
            instance = new RMListObservable();
        }
        return instance;
    }

    GitlabMergeRequest currentRequest;

    public void setCurrentRequest(GitlabMergeRequest currentRequest) {
        this.currentRequest = currentRequest;
    }

    public void refreshList() {
        List<GitlabMergeRequestWrap> result = new ArrayList<>();
        if (gitlabMergeRequests != null) {
            for (GitlabMergeRequestWrap gitlabMergeRequest : gitlabMergeRequests) {
                if (currentRequest != null) {
                    if (currentRequest.getProjectId().intValue() == gitlabMergeRequest.gitlabMergeRequest.getProjectId() &&
                            currentRequest.getId().intValue() == gitlabMergeRequest.gitlabMergeRequest.getId() &&
                            currentRequest.getIid().intValue() == gitlabMergeRequest.gitlabMergeRequest.getIid()) {
                        RMCommentsObservable.getInstance().refreshRequest(gitlabMergeRequest);
                    }
                }
                String reviewer = FilterBean.getInstance().getReviewer();
                if (reviewer != null && !reviewer.trim().isEmpty() && (gitlabMergeRequest.gitlabMergeRequest.getAssignee() == null || !gitlabMergeRequest.gitlabMergeRequest.getAssignee().getName().toLowerCase().contains(reviewer.toLowerCase()))) {
                    continue;
                }
                String owner = FilterBean.getInstance().getOwner();
                if (owner != null && !owner.trim().isEmpty() && !gitlabMergeRequest.gitlabMergeRequest.getAuthor().getName().toLowerCase().contains(owner.toLowerCase())) {
                    continue;
                }
                String fromBranch = FilterBean.getInstance().getFromBranch();
                if (fromBranch != null && !fromBranch.trim().isEmpty() && !(gitlabMergeRequest.srcLocalProName + "/" + gitlabMergeRequest.gitlabMergeRequest.getSourceBranch()).toLowerCase().contains(fromBranch.toLowerCase())) {
                    continue;
                }
                String toBranch = FilterBean.getInstance().getToBranch();
                if (toBranch != null && !toBranch.trim().isEmpty() && !(gitlabMergeRequest.targetLocalProName + "/" + gitlabMergeRequest.gitlabMergeRequest.getTargetBranch()).toLowerCase().contains(toBranch.toLowerCase())) {
                    continue;
                }
                String status = FilterBean.getInstance().getStatus();
                if (status != null && !status.trim().isEmpty() && !gitlabMergeRequest.gitlabMergeRequest.getState().toLowerCase().contains(status.toLowerCase())) {
                    continue;
                }
                String searchKey = FilterBean.getInstance().getSearchKey();
                if (searchKey != null) {
                    boolean titleContain = gitlabMergeRequest.gitlabMergeRequest.getTitle() != null && gitlabMergeRequest.gitlabMergeRequest.getTitle().toLowerCase().contains(searchKey.trim().toLowerCase());
                    boolean discriptionContain = gitlabMergeRequest.gitlabMergeRequest.getDescription() != null && gitlabMergeRequest.gitlabMergeRequest.getDescription().toLowerCase().contains(searchKey.trim().toLowerCase());
                    if (!searchKey.trim().isEmpty() && !titleContain && !discriptionContain) {
                        continue;
                    }
                }
                result.add(gitlabMergeRequest);
            }
        }
        setChanged();
        notifyObservers(result);
    }

    public void filterSearchKey(String key) {
        FilterBean.getInstance().setSearchKey(key);
        refreshList();
    }

    public void filterOwner(String owner) {
        FilterBean.getInstance().setOwner(owner);
        refreshList();
    }

    public void filterReviewer(String reviewer) {
        FilterBean.getInstance().setReviewer(reviewer);
        refreshList();
    }

    public void filterStatus(StatusPopupAction.Status status) {
        FilterBean.getInstance().setStatus(status == StatusPopupAction.Status.All ? "" : status.name());
        refreshList();
    }

    public void filterToBranch(String branch) {
        FilterBean.getInstance().setToBranch(branch);
        refreshList();
    }

    public void filterFromBranch(String branch) {
        FilterBean.getInstance().setFromBranch(branch);
        refreshList();
    }

    public void resetList(ArrayList<GitlabMergeRequestWrap> gitlabMergeRequests) {
        this.gitlabMergeRequests = gitlabMergeRequests;
        refreshList();
    }
}
