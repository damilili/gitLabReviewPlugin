package cn.kuwo.intellij.plugin.actions;

import cn.kuwo.intellij.plugin.RMListObservable;
import cn.kuwo.intellij.plugin.bean.FilterBean;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.util.Consumer;

import java.util.Observable;
import java.util.Observer;

public final class StatusPopupAction extends BasePopupAction {
    private final Project project;

    public static enum Status {
        All, Opened, Closed, Merged
    }

    public StatusPopupAction(Project project, String filterName) {
        super(filterName);
        this.project = project;
        updateFilterValueLabel(Status.All.name());
        RMListObservable.getInstance().addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                String status = FilterBean.getInstance().getStatus();
                if (status != null && status != "") {
                    updateFilterValueLabel(status);
                } else {
                    updateFilterValueLabel("All");
                }
            }
        });
    }

    @Override
    protected void createActions(Consumer<AnAction> actionConsumer) {
        for (Status status : Status.values()) {
            actionConsumer.consume(new DumbAwareAction(status.name()) {
                @Override
                public void actionPerformed(AnActionEvent e) {
                    RMListObservable.getInstance().filterStatus(status);
                }
            });
        }
    }
}