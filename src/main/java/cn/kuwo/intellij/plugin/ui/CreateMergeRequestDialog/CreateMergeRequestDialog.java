package cn.kuwo.intellij.plugin.ui.CreateMergeRequestDialog;

import cn.kuwo.intellij.plugin.GitLabUtil;
import cn.kuwo.intellij.plugin.bean.Branch;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.wm.StatusBar;
import git4idea.GitCommit;
import git4idea.GitUtil;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import org.gitlab.api.models.GitlabProject;
import org.gitlab.api.models.GitlabProjectMember;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Dialog fore creating merge requests
 *
 * @author ppolivka
 * @since 30.10.2015
 */
public class CreateMergeRequestDialog extends DialogWrapper {

    private Project project;

    private JPanel mainView;
    private JComboBox targetBranch;
    private JTextField mergeTitle;
    private JButton diffButton;
    private JComboBox assigneeBox;
    private JComboBox sourceBranch;
    private JComboBox repositoryBox;
    private JLabel labelRepository;
    private JComboBox remoteBox;
    private GitLabUtil gitlabUtil;

    private GitRepository curRepository;
    private GitRemote curRemote;
    private ArrayList<Branch> curRemoteBranches;
    private List<GitlabProjectMember> curGroupUser;
    private ItemListener remoteItemListener;
    private ItemListener repositoryItemListener;

    public CreateMergeRequestDialog(@Nullable Project project) {
        super(project);
        this.project = project;
        init();

    }

    @Override
    protected void init() {
        super.init();
        setTitle("Create Merge Request");
        setVerticalStretch(1f);
        gitlabUtil = GitLabUtil.getInstance(project);
        new Task.Backgroundable(project, "Get Remote Repository Info") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("get repositories...");
                refreshRepoBox();
                indicator.setText("get remotes...");
                refreshRemoteBox();
                indicator.setText("get remote branches...");
                refreshBranchBox();
                indicator.setText("get remote group user...");
                refreshAssigeeBox();
            }
        }.queue();

        diffButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (sourceBranch.getSelectedItem().equals(targetBranch.getSelectedItem())) {
                    Messages.showMessageDialog("Target branch must be different from source branch.", "Create Merge Request Fail", AllIcons.Ide.Error);
                    return;
                }
                new Task.Backgroundable(project, "Get Different Between Branchs...") {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        gitlabUtil.showDifBetweenBranchs(curRepository, curRemoteBranches.get(sourceBranch.getSelectedIndex()), curRemoteBranches.get(targetBranch.getSelectedIndex()));
                    }
                }.queue();
            }
        });
        assigneeBox.setEditable(false);
        assigneeBox.setBounds(140, 170, 180, 20);
        mergeTitle.addFocusListener(new JTextFieldHintListener(mergeTitle));
    }


    @Override
    protected void doOKAction() {
        if (sourceBranch.getSelectedItem().equals(targetBranch.getSelectedItem())) {
            Messages.showMessageDialog("Target branch must be different from source branch.", "Create Merge Request Fail", AllIcons.Ide.Error);
            return;
        }
        String title = mergeTitle.getText();
        int userId = -1;
        if (curGroupUser != null) {
            userId = curGroupUser.get(assigneeBox.getSelectedIndex()).getId();
        }
        String branchSource = curRemoteBranches.get(sourceBranch.getSelectedIndex()).gitlabBranch.getName();
        String branchTarget = curRemoteBranches.get(targetBranch.getSelectedIndex()).gitlabBranch.getName();
        if (userId == -1) {
            Messages.showMessageDialog("Select a assignee...", "Create Merge Request Fail", AllIcons.Ide.Error);
            return;
        }
        if (branchSource == null || branchTarget == null) {
            Messages.showMessageDialog("The source branch and the target branch cannot be empty.", "Create Merge Request Fail", AllIcons.Ide.Error);
            return;
        }
        if (title == null || title.isEmpty()) {
            Messages.showMessageDialog("Merge title cannot be empty.", "Create Merge Request Fail", AllIcons.Ide.Error);
            return;
        }
        List<GitCommit> diffBetweenBranchs = gitlabUtil.getDiffBetweenBranchs(curRepository, curRemoteBranches.get(sourceBranch.getSelectedIndex()), curRemoteBranches.get(targetBranch.getSelectedIndex()));
        if (diffBetweenBranchs != null && diffBetweenBranchs.size() > 0) {
            boolean succ = GitLabUtil.getInstance(project).addMergeRequest(curRemoteBranches.get(sourceBranch.getSelectedIndex()), curRemoteBranches.get(targetBranch.getSelectedIndex()), userId, title);
            if (succ) {
                StatusBar.Info.set("Create Merge Request Success", project);
            }
        }else {
            Messages.showMessageDialog("The source branch has been merged to target branch.", "Create Merge Request Fail", AllIcons.Ide.Error);
        }
        super.doOKAction();
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        return null;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return mainView;
    }

    private void refreshRepoBox() {
        Collection<GitRepository> repositories1 = GitUtil.getRepositories(project);
        GitRepository[] repositories = repositories1.toArray(new GitRepository[repositories1.size()]);
        if (repositoryItemListener == null) {
            repositoryItemListener = new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        curRepository = repositories[repositoryBox.getSelectedIndex()];
                        new Task.Backgroundable(project, "Get Remote Repository Info") {
                            @Override
                            public void run(@NotNull ProgressIndicator indicator) {
                                System.out.println("refreshRepoBox");
                                indicator.setText("get remotes...");
                                refreshRemoteBox();

                            }
                        }.queue();
                    }
                }
            };
            repositoryBox.addItemListener(repositoryItemListener);
        }

        if (repositories.length > 1) {
            labelRepository.setVisible(true);
            repositoryBox.setVisible(true);
        } else if (repositories.length == 1) {
            labelRepository.setVisible(false);
            repositoryBox.setVisible(false);
        }
        for (int i = 0; i < repositories.length; i++) {
            GitRepository repository = repositories[i];
            repositoryBox.addItem(repository.getRoot().getName());
        }
        curRepository = repositories[repositoryBox.getSelectedIndex()];
    }

    private void refreshRemoteBox() {
        Collection<GitRemote> remotes = curRepository.getRemotes();
        GitRemote[] gitRemotes = remotes.toArray(new GitRemote[remotes.size()]);
        if (remoteItemListener == null) {
            remoteItemListener = new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        curRemote = gitRemotes[remoteBox.getSelectedIndex()];
                        new Task.Backgroundable(project, "Get Remote Repository Info") {
                            @Override
                            public void run(@NotNull ProgressIndicator indicator) {
                                System.out.println("refreshRemoteBox");
                                indicator.setText("get remote branches...");
                                refreshBranchBox();
                                indicator.setText("get remote group user...");
                                refreshAssigeeBox();
                            }
                        }.queue();
                    }
                }
            };
            remoteBox.addItemListener(remoteItemListener);
        }
        remoteBox.removeAllItems();
        for (GitRemote gitRemote : gitRemotes) {
            remoteBox.addItem(gitRemote.getName());
        }
        curRemote = gitRemotes[remoteBox.getSelectedIndex()];
    }

    private void refreshBranchBox() {
        sourceBranch.removeAllItems();
        targetBranch.removeAllItems();
        curRemoteBranches = (ArrayList<Branch>) gitlabUtil.getRemoteBranches(curRepository);
        for (Branch branch : curRemoteBranches) {
            System.out.println("branch===" + branch.gitlabBranch.getName());
            sourceBranch.addItem(branch.gitlabBranch.getName());
            targetBranch.addItem(branch.gitlabBranch.getName());
        }
        sourceBranch.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                refreshTitle();
            }
        });
        targetBranch.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                refreshTitle();
            }
        });
        refreshTitle();
    }

    private void refreshAssigeeBox() {
        assigneeBox.removeAllItems();
        GitlabProject labProject = GitLabUtil.getInstance(project).getLabProject(curRemote.getFirstUrl());
        if (labProject != null) {
            curGroupUser = gitlabUtil.getGroupUser(labProject);
            for (GitlabProjectMember gitlabProjectMember : curGroupUser) {
                assigneeBox.addItem(gitlabProjectMember.getName());
            }
        }
    }

    public class JTextFieldHintListener implements FocusListener {
        private Color foreground;
        private JTextField textField;

        public JTextFieldHintListener(JTextField jTextField) {
            this.textField = jTextField;
            jTextField.setText(getHint());  //默认直接显示
            foreground = jTextField.getForeground();
            jTextField.setForeground(Color.GRAY);
        }

        @Override
        public void focusGained(FocusEvent e) {
            //获取焦点时，清空提示内容
            String temp = textField.getText();
            if (temp.equals(getHint())) {
                textField.setText("");
                textField.setForeground(foreground);
            }
        }

        @Override
        public void focusLost(FocusEvent e) {
            String temp = textField.getText();
            if (temp.equals("")) {
                textField.setForeground(Color.GRAY);
                textField.setText(getHint());
            }
        }
    }

    private String getHint() {
        if (sourceBranch != null && targetBranch != null && sourceBranch.getSelectedItem() != null && targetBranch.getSelectedItem() != null) {
            return "Merge of " + sourceBranch.getSelectedItem() + " to " + targetBranch.getSelectedItem();
        }
        return "";
    }

    private void refreshTitle() {
        if (mergeTitle.getForeground() == Color.GRAY) {
            mergeTitle.setText(getHint());
        }
    }
}
