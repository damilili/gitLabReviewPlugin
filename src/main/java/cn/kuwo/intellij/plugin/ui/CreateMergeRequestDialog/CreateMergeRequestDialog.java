package cn.kuwo.intellij.plugin.ui.CreateMergeRequestDialog;

import cn.kuwo.intellij.plugin.GitLabUtil;
import cn.kuwo.intellij.plugin.bean.Branch;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
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
import org.gitlab.api.models.GitlabMergeRequest;
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
    private ProgressIndicator showdiffProgress;
    private ProgressIndicator initProgressIndicator;
    private GitRepository[] repositories;
    private GitRemote[] gitRemotes;

    public CreateMergeRequestDialog(@Nullable Project project) {
        super(project);
        this.project = project;
        init();

    }

    @Override
    protected void init() {
        super.init();
        initView();
        setTitle("Create Merge Request");
        setVerticalStretch(1f);
        gitlabUtil = GitLabUtil.getInstance(project);
        new Task.Backgroundable(project, "Get Remote Repository Info") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                initProgressIndicator = indicator;
                indicator.setText("get repositories...");
                refreshRepoBoxData();
                if (initProgressIndicator.isCanceled()) {
                    Thread.interrupted();
                }
                indicator.setText("get remotes...");
                refreshRemoteBoxData();
                if (initProgressIndicator.isCanceled()) {
                    Thread.interrupted();
                }
                indicator.setText("get remote branches...");
                refreshBranchBoxData();
                if (initProgressIndicator.isCanceled()) {
                    Thread.interrupted();
                }
                indicator.setText("get remote group user...");
                refreshAssigeeBoxData();
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        refreshRepoBox();
                        refreshRemoteBox();
                    }
                }, ModalityState.any());
                initProgressIndicator = null;
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
                        showdiffProgress = indicator;
                        gitlabUtil.showDifBetweenBranchs(curRepository, curRemoteBranches.get(sourceBranch.getSelectedIndex()), curRemoteBranches.get(targetBranch.getSelectedIndex()));
                        showdiffProgress = null;
                    }
                }.queue();
            }
        });
        assigneeBox.setEditable(false);
        assigneeBox.setBounds(140, 170, 180, 20);
        mergeTitle.addFocusListener(new JTextFieldHintListener(mergeTitle));
    }

    private void initView() {
        //
        refreshRepoBoxData();
        refreshRepoBox();
        repositoryItemListener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    remoteBox.removeAllItems();
                    curRepository = repositories[repositoryBox.getSelectedIndex()];
                    new Task.Backgroundable(project, "Get Remote Repository Info") {
                        @Override
                        public void run(@NotNull ProgressIndicator indicator) {
                            System.out.println("refreshRepoBox");
                            indicator.setText("get remotes...");
                            refreshRemoteBoxData();
                            ApplicationManager.getApplication().invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    refreshRemoteBox();
                                }
                            });
                        }
                    }.queue();
                }
            }
        };
        repositoryBox.addItemListener(repositoryItemListener);
        if (repositories.length > 1) {
            labelRepository.setVisible(true);
            repositoryBox.setVisible(true);
        } else if (repositories.length == 1) {
            labelRepository.setVisible(false);
            repositoryBox.setVisible(false);
        }
//
        remoteItemListener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    curRemote = gitRemotes[remoteBox.getSelectedIndex()];
                    sourceBranch.removeAllItems();
                    targetBranch.removeAllItems();
                    refreshTitle();
                    new Task.Backgroundable(project, "Get Remote Repository Info") {
                        @Override
                        public void run(@NotNull ProgressIndicator indicator) {
                            System.out.println("refreshRemoteBox");
                            indicator.setText("get remote branches...");
                            refreshBranchBoxData();
                            indicator.setText("get remote group user...");
                            refreshAssigeeBoxData();
                            ApplicationManager.getApplication().invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    refreshBranchBox();
                                    refreshAssigeeBox();
                                }
                            });
                        }
                    }.queue();
                }
            }
        };
        remoteBox.addItemListener(remoteItemListener);
        //
        sourceBranch.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                refreshTitle();
            }
        });
        //
        targetBranch.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                refreshTitle();
            }
        });
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
        if (showdiffProgress != null) {
            if (showdiffProgress.isRunning()) {
                showdiffProgress.cancel();
            }
            showdiffProgress = null;
        }
        if (initProgressIndicator != null) {
            if (initProgressIndicator.isRunning()) {
                initProgressIndicator.cancel();
            }
            initProgressIndicator = null;
        }
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
        int finalUserId = userId;
        new Task.Backgroundable(project, "Compare the Two Branches...") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                List<GitCommit> diffBetweenBranchs = gitlabUtil.getDiffBetweenBranchs(curRepository, curRemoteBranches.get(sourceBranch.getSelectedIndex()), curRemoteBranches.get(targetBranch.getSelectedIndex()));
                if (diffBetweenBranchs != null && diffBetweenBranchs.size() > 0) {
                    GitLabUtil instance = GitLabUtil.getInstance(project);
                    GitlabMergeRequest gitlabMergeRequest = instance.addMergeRequest(curRemoteBranches.get(sourceBranch.getSelectedIndex()), curRemoteBranches.get(targetBranch.getSelectedIndex()), finalUserId, title);
                    if (gitlabMergeRequest != null) {
                        indicator.setText("Create Merge Request Success");
                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                CreateMergeRequestDialog.super.doOKAction();
                            }
                        }, ModalityState.any());
                    }
                    instance.getAllRequest();
                } else {
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            Messages.showMessageDialog("Nothing to merge from " + branchSource + "into " + branchTarget + ".", "Create Merge Request Fail", AllIcons.Ide.Error);
                        }
                    }, ModalityState.any());
                }

            }
        }.queue();

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

    private void refreshRepoBoxData() {
        Collection<GitRepository> repositories1 = GitUtil.getRepositories(project);
        repositories = repositories1.toArray(new GitRepository[repositories1.size()]);
    }

    private void refreshRepoBox() {
        if (repositories != null) {
            for (int i = 0; i < repositories.length; i++) {
                GitRepository repository = repositories[i];
                repositoryBox.addItem(repository.getRoot().getName());
            }
            curRepository = repositories[repositoryBox.getSelectedIndex()];
        }
    }

    private void refreshRemoteBoxData() {
        if (curRepository != null) {
            Collection<GitRemote> remotes = curRepository.getRemotes();
            gitRemotes = remotes.toArray(new GitRemote[remotes.size()]);
        }
    }

    private void refreshRemoteBox() {
        if (gitRemotes != null) {
            for (GitRemote gitRemote : gitRemotes) {
                remoteBox.addItem(gitRemote.getName());
            }
            curRemote = gitRemotes[remoteBox.getSelectedIndex()];
        }
    }

    private void refreshBranchBoxData() {
        if (curRepository != null) {
            curRemoteBranches = (ArrayList<Branch>) gitlabUtil.getRemoteBranches(curRepository);
        }
    }

    private void refreshBranchBox() {
        if (curRemoteBranches != null) {
            for (Branch branch : curRemoteBranches) {
                System.out.println("branch===" + branch.gitlabBranch.getName());
                sourceBranch.addItem(branch.gitlabBranch.getName());
                targetBranch.addItem(branch.gitlabBranch.getName());
            }
        }
        refreshTitle();
    }

    private void refreshAssigeeBoxData() {
        if (curRemote != null) {
            GitlabProject labProject = GitLabUtil.getInstance(project).getLabProject(curRemote.getFirstUrl());
            curGroupUser = gitlabUtil.getGroupUser(labProject);
        }
    }

    private void refreshAssigeeBox() {
        if (curGroupUser != null) {
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
