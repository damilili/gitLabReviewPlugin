package cn.kuwo.intellij.plugin.actions;

import cn.kuwo.intellij.plugin.Constants;
import cn.kuwo.intellij.plugin.ui.CreateMergeRequestDialog.CreateMergeRequestDialog;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import git4idea.GitUtil;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;

import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActionAddMergeRequest extends DumbAwareAction {
    public ActionAddMergeRequest() {
        super("Create _Merge Request...", "Creates merge request from current branch", AllIcons.Vcs.Merge);
    }

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        if (checkToken(anActionEvent.getProject())) {
            CreateMergeRequestDialog createMergeRequestDialog = new CreateMergeRequestDialog(anActionEvent.getProject());
            createMergeRequestDialog.show();
        }else {
            Messages.showMessageDialog("Some tokens are not configured", "Create Merge Request Fail", AllIcons.Ide.Error);
        }
    }

    private boolean checkToken(Project project) {
        GitRepositoryManager manager = GitUtil.getRepositoryManager(project);
        List<GitRepository> repositories = manager.getRepositories();
        HashSet<String> remoteHosts = new HashSet<>();
        for (GitRepository repository : repositories) {
            for (GitRemote gitRemote : repository.getRemotes()) {
                Pattern compile = Pattern.compile("(?<=(http(s)?://))[\\w|\\.]*");
                Matcher matcher = compile.matcher(gitRemote.getFirstUrl());
                if (matcher.find()) {
                    remoteHosts.add(matcher.group());
                    continue;
                }
                compile = Pattern.compile("(?<=(git@))[\\w|\\.|\\d]*");
                matcher = compile.matcher(gitRemote.getFirstUrl());
                if (matcher.find()) {
                    remoteHosts.add(matcher.group());
                }
            }
        }
        PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(project);
        for (String remoteHost : remoteHosts) {
            String value = propertiesComponent.getValue(Constants.PROPERTIEPRE+remoteHost);
            if (value == null || value.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
