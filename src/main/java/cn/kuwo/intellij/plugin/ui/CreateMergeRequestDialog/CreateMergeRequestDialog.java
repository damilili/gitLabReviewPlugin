package cn.kuwo.intellij.plugin.ui.CreateMergeRequestDialog;

import cn.kuwo.intellij.plugin.GitLabUtil;
import cn.kuwo.intellij.plugin.bean.Branch;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import git4idea.GitRemoteBranch;
import org.gitlab.api.models.GitlabProjectMember;
import org.gitlab.api.models.GitlabUser;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
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
    private JCheckBox removeSourceBranch;
    private JCheckBox wip;
    private JComboBox sourceBranch;
    private ArrayList<Branch> remoteBranches;
    private GitLabUtil instance;
    private List<GitlabProjectMember> curGroupUser;
    public static final String key_lastAsssignee = "cn_kuwo_intellij_plugin_lastAssignee";
    public static final String key_lastSourceBranch = "cn_kuwo_intellij_plugin_lastSourceBranch";
    public static final String key_lastTargetBranch = "cn_kuwo_intellij_plugin_lastTargetBranch";

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
        instance = GitLabUtil.getInstance(project);
        remoteBranches = instance.getRemoteBranches();
        diffButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (sourceBranch.getSelectedItem().equals(targetBranch.getSelectedItem())) {
                    Messages.showMessageDialog("Target branch must be different from source branch.", "Create Merge Request Fail", AllIcons.Ide.Error);
                    return;
                }
                instance.getDifBetweenBranchs(remoteBranches.get(sourceBranch.getSelectedIndex()), remoteBranches.get(targetBranch.getSelectedIndex()));
            }
        });
        for (Branch branch : remoteBranches) {
            sourceBranch.addItem(branch.repoName + "/" + branch.gitlabBranch.getName());
            targetBranch.addItem(branch.repoName + "/" + branch.gitlabBranch.getName());
        }
        GitRemoteBranch curLocalTrackedRemoteBranch = instance.getCurLocalTrackedRemoteBranch();
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
                int selectedIndex = targetBranch.getSelectedIndex();
                Branch branch = remoteBranches.get(selectedIndex);
                refreshAssigeeBox(branch, null);
            }
        });
        PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(project);
        String lastAssigneeId = propertiesComponent.getValue(key_lastAsssignee);
        String lastSource = propertiesComponent.getValue(key_lastSourceBranch);
        String lastTaget = propertiesComponent.getValue(key_lastTargetBranch);
        //源分支
        if (curLocalTrackedRemoteBranch != null) {
            String nameForRemoteOperations = curLocalTrackedRemoteBranch.getName();
            sourceBranch.setSelectedItem(nameForRemoteOperations);
        } else {
            if (lastSource == null || lastSource.isEmpty()) {
                sourceBranch.setSelectedIndex(0);
            } else {
                sourceBranch.setSelectedItem(lastSource);
            }
        }
        //目标分支
        if (lastTaget != null && !lastTaget.isEmpty()) {
            if (targetBranch.getItemCount() > 0) {
                Branch branch = remoteBranches.get(targetBranch.getSelectedIndex());
                refreshAssigeeBox(branch, lastAssigneeId);
                targetBranch.setSelectedItem(lastTaget);
            }
        } else {
            if (targetBranch.getItemCount() > 0) {
                targetBranch.setSelectedIndex(0);
                Branch branch = remoteBranches.get(0);
                refreshAssigeeBox(branch, lastAssigneeId);
            }
        }
        assigneeBox.setEditable(false);
        assigneeBox.setBounds(140, 170, 180, 20);
        mergeTitle.addFocusListener(new JTextFieldHintListener(mergeTitle));
    }

    private void refreshAssigeeBox(Branch branch, String lastAssigneeId) {
        assigneeBox.removeAllItems();
        curGroupUser = instance.getGroupUser(branch.repoName);
        for (GitlabProjectMember gitlabProjectMember : curGroupUser) {
            assigneeBox.addItem(gitlabProjectMember.getName());
        }
        if (curGroupUser.size() > 0) {
            if (lastAssigneeId != null) {
                curGroupUser.forEach(member -> {
                    if (String.valueOf(member.getId()).equals(lastAssigneeId)) {
                        assigneeBox.setSelectedItem(member.getName());
                        return;
                    }
                });
            } else {
                assigneeBox.setSelectedIndex(0);
            }
        }
    }

    @Override
    protected void doOKAction() {
        if (sourceBranch.getSelectedItem().equals(targetBranch.getSelectedItem())) {
            Messages.showMessageDialog("Target branch must be different from source branch.", "Create Merge Request Fail", AllIcons.Ide.Error);
            return;
        }
        String title = mergeTitle.getText();
        if (wip.isSelected()) {
            title = "WIP:" + title;
        }
        int userId = -1;
        if (curGroupUser != null) {
            userId = curGroupUser.get(assigneeBox.getSelectedIndex()).getId();
        }
        String branchSource = remoteBranches.get(sourceBranch.getSelectedIndex()).gitlabBranch.getName();
        String branchTarget = remoteBranches.get(targetBranch.getSelectedIndex()).gitlabBranch.getName();
        if (userId == -1) {
            Messages.showMessageDialog("Select a assignee..", "Create Merge Request Fail", AllIcons.Ide.Error);
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
        PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(project);
        propertiesComponent.setValue(key_lastAsssignee, String.valueOf(userId));
        propertiesComponent.setValue(key_lastSourceBranch, branchSource);
        propertiesComponent.setValue(key_lastTargetBranch, branchTarget);
        GitLabUtil.getInstance(project).addMergeRequest(remoteBranches.get(sourceBranch.getSelectedIndex()), remoteBranches.get(targetBranch.getSelectedIndex()), userId, title);
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
