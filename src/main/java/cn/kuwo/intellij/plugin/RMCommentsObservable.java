package cn.kuwo.intellij.plugin;

import cn.kuwo.intellij.plugin.bean.GitlabMergeRequestWrap;

import java.util.ArrayList;
import java.util.Observable;

public class RMCommentsObservable extends Observable {
    private static RMCommentsObservable instance;
    private ArrayList<GitlabMergeRequestWrap> gitlabMergeRequests;

    private RMCommentsObservable() {

    }

    public static RMCommentsObservable getInstance() {
        if (instance == null) {
            instance = new RMCommentsObservable();
        }
        return instance;
    }

    public void refreshComment() {
          setChanged();
          notifyObservers();
    }

    public void refreshRequest(GitlabMergeRequestWrap gitlabMergeRequest) {
        setChanged();
        notifyObservers(gitlabMergeRequest);
    }
}
