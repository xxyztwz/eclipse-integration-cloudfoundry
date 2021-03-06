/*******************************************************************************
 * Copyright (c) 2012, 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.DeploymentInfo;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationAction;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationInfo;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;

public class CloudFoundryApplicationWizard extends Wizard {

	protected final CloudFoundryApplicationModule module;

	protected final CloudFoundryServer server;

	protected IApplicationWizardDelegate wizardDelegate;

	protected final ApplicationWizardDescriptor applicationDescriptor;

	public CloudFoundryApplicationWizard(CloudFoundryServer server, CloudFoundryApplicationModule module,
			IApplicationWizardDelegate wizardDelegate) {
		Assert.isNotNull(server);
		Assert.isNotNull(module);
		this.server = server;
		this.module = module;
		this.wizardDelegate = wizardDelegate;

		applicationDescriptor = new ApplicationWizardDescriptor();
		setNeedsProgressMonitor(true);
		setWindowTitle("Application");
	}

	@Override
	public boolean canFinish() {
		boolean canFinish = super.canFinish();
		if (canFinish && wizardDelegate instanceof ApplicationWizardDelegate) {
			canFinish = ((ApplicationWizardDelegate) wizardDelegate).isValid(applicationDescriptor);
		}

		return canFinish;
	}

	@Override
	public void addPages() {

		// if a wizard provider exists, see if it contributes pages to the
		// wizard
		List<IWizardPage> applicationDeploymentPages = null;

		if (wizardDelegate == null) {
			// Use the default Java Web pages
			wizardDelegate = ApplicationWizardRegistry.getDefaultJavaWebWizardDelegate();
		}

		if (wizardDelegate != null) {
			// Note that the descriptor should be initialised FIRST before
			// requesting pages from the wizard delegate.
			wizardDelegate.initialiseWizardDescriptor(applicationDescriptor, server, module);
			applicationDeploymentPages = wizardDelegate.getWizardPages(applicationDescriptor, server, module);
		}

		if (applicationDeploymentPages != null && !applicationDeploymentPages.isEmpty()) {
			for (IWizardPage updatedPage : applicationDeploymentPages) {
				addPage(updatedPage);
			}
		}
		else {

			String moduleID = module != null && module.getModuleType() != null ? module.getModuleType().getId()
					: "Unknown module type.";

			CloudFoundryPlugin.logError("Unable to load application deployment wizard pages for application type: "
					+ moduleID);
		}

	}

	public ApplicationInfo getApplicationInfo() {
		return applicationDescriptor.getApplicationInfo();
	}

	public DeploymentInfo getDeploymentInfo() {
		return applicationDescriptor.getDeploymentInfo();
	}

	public ApplicationAction getDeploymentMode() {
		return applicationDescriptor.getStartDeploymentMode();
	}

	public Staging getStaging() {
		return applicationDescriptor.getStaging();
	}

	/**
	 * @return
	 */
	public List<CloudService> getCreatedCloudServices() {
		return applicationDescriptor.getCreatedCloudServices();
	}

	/**
	 * May be empty if nothing selected, but never null
	 * @return
	 */
	public List<String> getSelectedServicesForBinding() {
		return applicationDescriptor.getSelectedServicesForBinding();
	}

	@Override
	public boolean performFinish() {
		return true;
	}

}
