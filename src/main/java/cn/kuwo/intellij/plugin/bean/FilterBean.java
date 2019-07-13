package cn.kuwo.intellij.plugin.bean;

public class FilterBean {
    public String getToBranch() {
        return toBranch;
    }

    public void setToBranch(String toBranch) {
        this.toBranch = toBranch;
    }

    String toBranch;

    public String getFromBranch() {
        return fromBranch;
    }

    public void setFromBranch(String fromBranch) {
        this.fromBranch = fromBranch;
    }

    String fromBranch;

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

    String status;
    String reviewer;
    String owner;
    private static FilterBean ourInstance = new FilterBean();

    public static FilterBean getInstance() {
        return ourInstance;
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
