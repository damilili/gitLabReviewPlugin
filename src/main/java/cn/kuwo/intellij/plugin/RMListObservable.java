package cn.kuwo.intellij.plugin;

import cn.kuwo.intellij.plugin.actions.StatusPopupAction;
import cn.kuwo.intellij.plugin.bean.FilterBean;
import org.gitlab.api.models.GitlabMergeRequest;

import java.util.List;
import java.util.Observable;

public class RMListObservable extends Observable {
    private static RMListObservable instance;

    private RMListObservable() {

    }

    public static RMListObservable getInstance() {
        if (instance == null) {
            instance = new RMListObservable();
        }
        return instance;
    }

    public void refreshList(List<GitlabMergeRequest> result) {
        setChanged();
        notifyObservers(result);
    }

    public void filterOwner(String owner) {
        FilterBean.getInstance().setOwner(owner);
        refreshFilter();
    }

    public void filterReviewer(String reviewer) {
        FilterBean.getInstance().setReviewer(reviewer);
        refreshFilter();
    }

    public void filterStatus(StatusPopupAction.Status status) {
        FilterBean.getInstance().setStatus(status==StatusPopupAction.Status.All?"":status.name());
        refreshFilter();
    }

    public void filterToBranch(String branch) {
        FilterBean.getInstance().setToBranch(branch);
        refreshFilter();
    }
    public void filterFromBranch(String branch) {
        FilterBean.getInstance().setFromBranch(branch);
        refreshFilter();
    }

    private void refreshFilter() {

    }

}
