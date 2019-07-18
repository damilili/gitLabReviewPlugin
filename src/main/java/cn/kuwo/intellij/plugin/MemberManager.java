package cn.kuwo.intellij.plugin;

import org.gitlab.api.models.GitlabProjectMember;
import org.gitlab.api.models.GitlabUser;

import java.util.*;

public class MemberManager {
    private static MemberManager ourInstance = new MemberManager();

    public static MemberManager getInstance() {
        return ourInstance;
    }

    private MemberManager() {
    }

    public  HashMap<String,GitlabUser> getMemberList() {
        return users;
    }

    public void setMemberList( HashMap<String,GitlabUser> users) {
        this.users = users;
    }

   private HashMap<String,GitlabUser> users = new HashMap<>();
}
