package cn.kuwo.intellij.plugin.bean;

public class FilterBean {


    String searchKey;
    String toBranch;
    String status;
    String reviewer;
    String owner;
    String fromBranch;
    public String getToBranch() {
        return toBranch;
    }

    public void setToBranch(String toBranch) {
        this.toBranch = toBranch;
    }
    public String getFromBranch() {
        return fromBranch;
    }

    public void setFromBranch(String fromBranch) {
        this.fromBranch = fromBranch;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReviewer() {
        return reviewer;
    }

    public void setReviewer(String reviewer) {
        this.reviewer = reviewer;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    private static FilterBean ourInstance = new FilterBean();

    public static FilterBean getInstance() {
        return ourInstance;
    }

    public String getSearchKey() {
        return searchKey;
    }

    public void setSearchKey(String searchKey) {
        this.searchKey = searchKey;
    }
    private FilterBean() {
    }

    public void clearFilter() {
        FilterBean.getInstance().setOwner("");
        FilterBean.getInstance().setReviewer("");
        FilterBean.getInstance().setStatus("");
        FilterBean.getInstance().setFromBranch("");
        FilterBean.getInstance().setToBranch("");
    }
}
