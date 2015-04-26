package de.codecentric.eclipse.tutorial.app.handler;

import java.util.Optional;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.fx.core.ProgressReporter;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.operation.CancelableOperation;
import org.eclipse.fx.core.update.UpdateService;
import org.eclipse.fx.core.update.UpdateService.UpdatePlan;
import org.eclipse.fx.core.update.UpdateService.UpdateResult;
import org.eclipse.jface.dialogs.MessageDialog;

import at.bestsolution.e4.extensions.core.services.RestartService;

public class FXUpdateHandler {

	@Execute
	public void execute(
			UpdateService updateService, 
			ThreadSynchronize sync, 
			RestartService restartService) {
		CancelableOperation<Optional<UpdatePlan>> check = updateService.checkUpdate(ProgressReporter.NULLPROGRESS_REPORTER);
		check.onCancel(() -> showMessage(sync, "Operation cancelled"));
		check.onException(t -> {
			String message = t.getStatus().getMessage();
			showError(sync, message);
		});
		check.onComplete((updatePlan) -> {
			if (!updatePlan.isPresent()) {
				showMessage(sync, "Nothing to update");
			}
			else {
				if (showConfirmation(
						sync, 
						"Updates available", 
						"There are updates available. Do you want to install them now?")) {
					
					CancelableOperation<UpdateResult> result = updatePlan.get().runUpdate(ProgressReporter.NULLPROGRESS_REPORTER);
					result.onCancel(() -> showMessage(sync, "Operation cancelled"));
					result.onException(t -> showError(sync, t.getLocalizedMessage()));
					result.onComplete((r) -> {
						if (showConfirmation(
								sync, 
								"Updates installed, restart?", 
								"Updates have been installed successfully, do you want to restart?")) {
					
							sync.syncExec(() -> restartService.restart(true));
						}
					});
				}
			}
		});
	}

	private void showMessage(ThreadSynchronize sync, final String message) {
		// as the provision needs to be executed in a background thread
		// we need to ensure that the message dialog is executed in
		// the UI thread
		sync.syncExec(() -> {
            MessageDialog.openInformation(null, "Information", message);
		});
	}

	private void showError(ThreadSynchronize sync, final String message) {
		// as the provision needs to be executed in a background thread
		// we need to ensure that the message dialog is executed in
		// the UI thread
		sync.syncExec(new Runnable() {

			@Override
			public void run() {
                MessageDialog.openError(null, "Error", message);
			}
		});
	}
	
	private boolean showConfirmation(ThreadSynchronize sync, final String title, final String message) {
		return sync.syncExec(() -> {
			return MessageDialog.openConfirm(null, title, message);
		}, false);
	}
}
