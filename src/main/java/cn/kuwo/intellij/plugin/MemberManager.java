package cn.kuwo.intellij.plugin;

import org.gitlab.api.models.GitlabProjectMember;

import java.util.ArrayList;
import java.util.List;

public class MemberManager {
    private static MemberManager ourInstance = new MemberManager();

    public static MemberManager getInstance() {
        return ourInstance;
    }

    private MemberManager() {
    }

    public List<GitlabProjectMember> getMemberList() {
        return users;
    }

    public void setMemberList(List<GitlabProjectMember> users) {
        this.users = users;
    }

   private   List<GitlabProjectMember> users = new ArrayList<>();
}
