/*******************************************************************************
 * Copyright (c) 2014 Chaupal.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, Version 2.0
 * which accompanies this distribution, and is available at
 * http://www.apache.org/licenses/LICENSE-2.0.html
 *******************************************************************************/
/**
 * 
 */
package org.chaupal.jp2p.ui.template.config;

import java.util.Map;
import java.util.logging.Logger;

import org.chaupal.jp2p.ui.template.project.AbstractJp2pTemplateSection;
import org.chaupal.jp2p.ui.template.project.ContextWizardOption;
import org.chaupal.jp2p.ui.template.project.ContextWizardOption.TemplateOptions;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;

/**
 * @author Marine
 *
 */
public class JxseConfigurationTemplateSection extends AbstractJp2pTemplateSection {

	public static final String TEMPLATE_ROOT = "jxse";

	private static final String S_MSG_JXSE_CONTEXT_PROPS = "JXSE Context Properties";
	private static final String S_MSG_SET_JXSE_CONTEXT_PROPS = "Set JXSE Context Properties";

	private static final String S_JXSE_CONFIGURATION_PAGE = "JXSE Configuration Page";
	private static final String S_JXSE_CONFIGURATION_DESC_1 = "Configure the global properties of the JXSE bundle";
	private static final String S_JXSE_CONFIGURATION_DESC_2 = "The network configuration of the JXSE bundle";
	
	private ContextWizardOption view;
	
	public JxseConfigurationTemplateSection() {
		super( TEMPLATE_ROOT );
		this.setPageCount(2);
		this.createOptions();
	}

	/**
	 * Get the template that is requested
	 * @return
	 */
	public TemplateOptions getTemplate(){
		return view.getTemplate();
	}
	
	public void createOptions() {
		view = new ContextWizardOption( this, S_MSG_JXSE_CONTEXT_PROPS, S_MSG_SET_JXSE_CONTEXT_PROPS );
		this.registerOption(view, null,  0 );
	}

	
	/**
	 * Set the template option
	 * @param option
	 */
	public void setTemplateOption( TemplateOptions option ){
		this.view.setTemplateOption( option );
	}

	@Override
	public void update() throws Exception{
		view.complete();
	}

	
	@Override
	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		Logger logger = Logger.getLogger(this.getClass().getName());
		logger.info("Updating model");
		try {
			view.complete();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		super.updateModel(monitor);
	}

	@Override
	public void addPages(Wizard wizard) {
		WizardPage page = this.createPage(0, "jxse_bundle_context_id_2");
		page.setTitle( S_JXSE_CONFIGURATION_PAGE + " (2)");
		page.setDescription( S_JXSE_CONFIGURATION_DESC_1 );
		wizard.addPage(page);
		page = new NetworkConfiguratorWizardPage("Set up network configuration");
		page.setTitle( S_JXSE_CONFIGURATION_PAGE +" (3)");
		page.setDescription( S_JXSE_CONFIGURATION_DESC_2);
		wizard.addPage(page);
		super.addPages(wizard);
	}

	@Override
	protected void onFillProperties(Map<String, String> attributes) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected String getResourceLocation() {
		// TODO Auto-generated method stub
		return null;
	}
}
