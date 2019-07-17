package cn.kuwo.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import org.gitlab.api.models.GitlabMergeRequest;

import javax.swing.*;

public abstract class RequestDetailAction extends AnAction {
     protected GitlabMergeRequest mergeRequest;

    public void setRequest(GitlabMergeRequest mergeRequest) {
        this.mergeRequest = mergeRequest;
    }

    public RequestDetailAction(String text, String des, Icon icon) {
        super(text, des, icon);
    }
}