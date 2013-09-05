/*******************************************************************************
 * Copyright (c) 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.cloudfoundry.ide.eclipse.internal.server.ui.IPartChangeListener;
import org.cloudfoundry.ide.eclipse.internal.server.ui.PartChangeEvent;
import org.cloudfoundry.ide.eclipse.internal.server.ui.UIPart;
import org.cloudfoundry.ide.eclipse.internal.server.ui.WizardPartChangeEvent;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;

/**
 * Wizard page that manages multiple UI parts, and handles errors from each
 * part. In terms of errors, UI parts are each treated as atomic units, meaning
 * that any error generated by any part is considered to be from the part as a
 * whole, rather from individual controls in that part.
 * 
 * <p/>
 * In order for the page to manage errors from parts, the page MUST be added as
 * a listener to each UI Part that is created.
 * 
 */
public abstract class PartsWizardPage extends WizardPage implements IPartChangeListener {

	private boolean canFinish;

	protected Map<UIPart, IStatus> partStatus = new HashMap<UIPart, IStatus>();

	protected PartsWizardPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);

	}

	public void handleChange(PartChangeEvent event) {
		IStatus status = event.getStatus();
		if (status == null) {
			status = Status.OK_STATUS;
		}

		// If the part indicates its OK, remove it from the list of tracked
		// parts, as any error it would have previously
		// generated has now been fixed.
		if (status.isOK()) {
			partStatus.remove(event.getSource());

			// Check if there are other errors that haven't yet been resolved
			for (Entry<UIPart, IStatus> entry : partStatus.entrySet()) {
				status = entry.getValue();
				break;
			}

		}
		else if (event.getSource() != null) {
			partStatus.put(event.getSource(), status);
		}

		boolean updateButtons = !(event instanceof WizardPartChangeEvent)
				|| ((WizardPartChangeEvent) event).updateWizardButtons();

		update(updateButtons, status);
	}

	@Override
	public boolean isPageComplete() {
		return canFinish;
	}

	/**
	 * This should be the ONLY way to notify the wizard page whether the page is
	 * complete or not, as well as display any error or warning messages.
	 * 
	 * <p/>
	 * 
	 * The wizard page will only be complete if it receives an OK status.
	 * 
	 * <p/>
	 * 
	 * It is up to the caller to correctly set the OK state of the page in case
	 * it sets a non-OK status, and the non-OK status gets resolved.
	 * 
	 * @param updateButtons true if force the wizard button states to be
	 * refreshed. NOTE that if true, it is up to the caller to ensure that the
	 * wizard page has been added to the wizard , and the wizard page is
	 * visible.
	 * @param status if status is OK, the wizard can complete. False otherwise.
	 */
	protected void update(boolean updateButtons, IStatus status) {

		canFinish = status.isOK();

		if (status.isOK()) {
			setErrorMessage(null);
		}
		else if (status.getSeverity() == IStatus.ERROR) {
			setErrorMessage(status.getMessage() != null ? status.getMessage()
					: "Unknown error. Unable to complete the operation.");
		}
		else if (status.getSeverity() == IStatus.INFO) {
			setMessage(status.getMessage(), DialogPage.INFORMATION);
		}
		else if (status.getSeverity() == IStatus.WARNING) {
			setMessage(status.getMessage(), DialogPage.WARNING);
		}

		if (updateButtons) {
			getWizard().getContainer().updateButtons();
		}
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {

			if (getPreviousPage() == null) {
				// delay until dialog is actually visible
				if (!getControl().isDisposed()) {
					performWhenPageVisible();
				}
			}
			else {
				performWhenPageVisible();
			}
		}
	}

	protected void performWhenPageVisible() {
		// Do nothing by default;
	}

}
