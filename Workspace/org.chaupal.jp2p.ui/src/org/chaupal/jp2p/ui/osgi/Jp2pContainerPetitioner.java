/*******************************************************************************
 * Copyright (c) 2014 Chaupal.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, Version 2.0
 * which accompanies this distribution, and is available at
 * http://www.apache.org/licenses/LICENSE-2.0.html
 *******************************************************************************/

package org.chaupal.jp2p.ui.osgi;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import net.jp2p.container.Jp2pContainer;
import net.jp2p.container.IJp2pContainer;
import net.jp2p.container.IJp2pDSComponent;
import net.jp2p.container.Jp2pContainer.ServiceChange;
import net.jp2p.container.component.ComponentChangedEvent;
import net.jp2p.container.component.IComponentChangedListener;
import net.jp2p.container.component.IJp2pComponent;
import net.jp2p.container.component.IJp2pComponentNode;
import net.jp2p.container.dispatcher.ServiceChangedEvent;
import net.jp2p.container.dispatcher.ServiceEventDispatcher;
import net.jp2p.container.properties.IJp2pProperties;
import net.jp2p.container.properties.IJp2pPropertySource;
import net.jp2p.container.utils.Utils;

import org.chaupal.jp2p.ui.comparator.Jp2pServiceComparator;
import org.chaupal.jp2p.ui.log.Jp2pLog;
import org.chaupal.jp2p.ui.osgi.RootPropertySource.PetitionerProperties;
import org.eclipselabs.osgi.ds.broker.service.AbstractPalaver;
import org.eclipselabs.osgi.ds.broker.service.AbstractPetitioner;
import org.eclipselabs.osgi.ds.broker.service.ParlezEvent;

public class Jp2pContainerPetitioner extends AbstractPetitioner<String, String, IJp2pContainer<?>>
	implements IJp2pComponentNode<Collection<IJp2pContainer<?>>>
{
	public static final String S_WRN_THREAD_INTERRUPTED = "The thread is interrupted. Probably stopping service";
	
	private static Jp2pContainerPetitioner attendee = new Jp2pContainerPetitioner();
	
	private List<IJp2pComponent<?>> children;

	private ServiceEventDispatcher dispatcher = ServiceEventDispatcher.getInstance();	
	private IComponentChangedListener<IJp2pComponent<?>> listener;
	private RefreshRunnable refresher;
	private RootPropertySource source;
	
	private Logger logger = Logger.getLogger( Jp2pContainerPetitioner.class.getName() );
	
	private Jp2pContainerPetitioner() {
		super( new ResourcePalaver());
		children = new ArrayList<IJp2pComponent<?>>();
		source = new RootPropertySource();
		refresher = new RefreshRunnable( source );
		this.listener = new IComponentChangedListener<IJp2pComponent<?>>() {
			
			@Override
			public void notifyServiceChanged(ComponentChangedEvent<IJp2pComponent<?>> event) {
				dispatcher.serviceChanged( new ServiceChangedEvent( this, ServiceChange.COMPONENT_EVENT ));
				refresher.start();
			}
		};
	}
	
	public static Jp2pContainerPetitioner getInstance(){
		return attendee;
	}

	@Override
	public IJp2pPropertySource<IJp2pProperties> getPropertySource() {
		return source;
	}

	public IJp2pContainer<?> getJp2pContainer( String identifier ) {
		for( IJp2pContainer<?> container: super.getCollection() )
			if( container.getIdentifier().equals( identifier ))
				return container;
		return null;
	}

	
	@Override
	public void petition(String data) {
	    logger.info("Petitioning Containers for UI");
		super.petition(data);
	}

	@Override
	protected void onDataReceived( ParlezEvent<IJp2pContainer<?>> event ) {
		  super.onDataReceived( event );
		  if(!( event.getData() instanceof IJp2pComponent ))
			  return;
		  this.addChild( event.getData());
		  logger.info("Container added: " + event.getData().getIdentifier( ));
	}

	@Override
	public String getId() {
		return this.getClass().getPackage().getName() + ".petitioner";
	}

	/**
	 * Get a String label for this component. This can be used for display options and 
	 * is not meant to identify the component;
	 * @return
	 */
	@Override
	public String getComponentLabel(){
		return this.source.getComponentName();
	}

	@Override
	public boolean isRoot() {
		return true;
	}

	@Override
	public Collection<IJp2pContainer<?>> getModule() {
		return super.getCollection();
	}

	@Override
	public IJp2pComponent<?>[] getChildren() {
		return children.toArray( new IJp2pComponent<?>[ this.children.size()]);
	}

	@Override
	public boolean addChild(IJp2pComponent<?> child) {
		if( children.contains( child ))
			return false;
		children.add( child );
		IJp2pContainer<?> container = (IJp2pContainer<?>) child;
		container.getDispatcher().addServiceChangeListener( listener );
		Collections.sort( children, new Jp2pServiceComparator<Object>());
		dispatcher.serviceChanged( new ServiceChangedEvent( this, ServiceChange.CHILD_ADDED ));
		dispatcher.serviceChanged( new ServiceChangedEvent( this, ServiceChange.REFRESH ));
		return true;
	}

	@Override
	public void removeChild(IJp2pComponent<?> child) {
		children.remove( child );
		IJp2pContainer<?> container = (IJp2pContainer<?>) child;
		container.getDispatcher().removeServiceChangeListener( listener );
		dispatcher.serviceChanged( new ServiceChangedEvent( this, ServiceChange.CHILD_REMOVED ));
		dispatcher.serviceChanged( new ServiceChangedEvent( this, ServiceChange.REFRESH ));
	}

	@Override
	public boolean hasChildren() {
		return !this.children.isEmpty();
	}

	public void finalise(){
		for( IJp2pComponent<?> container: this.children ){
			((Jp2pContainer<?>) container).getDispatcher().removeServiceChangeListener( listener );
		}
		this.refresher.stop();
	}

	/**
	 * The palaver contains the conditions for attendees to create an assembly. In this case, the attendees must
	 * pass a string identifier (the package id) and provide a token that is equal
	 * @author Kees
	 *
	 */
	private static class ResourcePalaver extends AbstractPalaver<String>{

		private static final String S_JP2P_INF = "/JP2P-INF/token.txt";
		
		private String providedToken;

		protected ResourcePalaver() {
			super( getProvidedInfo()[0]);
			this.providedToken = getProvidedInfo()[1];
		}

		private static final String[] getProvidedInfo(){
			Class<?> clss = ResourcePalaver.class;
			String[] info = { IJp2pDSComponent.S_IJP2P_CONTAINER_PACKAGE_ID, IJp2pDSComponent.S_IP2P_TOKEN} ;
			URL url = clss.getResource(S_JP2P_INF );
			if( url == null )
				return info;
			Scanner scanner = null;
			try{
				scanner = new Scanner( clss.getResourceAsStream( S_JP2P_INF ));
				String str = scanner.nextLine();
				if( !Utils.isNull(str))
					info[0] = str;
				str = scanner.nextLine();
				if( !Utils.isNull(str))
					info[1] = str;
				return info;
			} catch (Exception e) {
				e.printStackTrace();
			}
			finally{
				if( scanner != null )
					scanner.close();
			}
			return null;
			
		}
		
		@Override
		public String giveToken() {
			if( this.providedToken == null )
				return IJp2pDSComponent.S_IP2P_TOKEN;
			return this.providedToken;	
		}

		@Override
		public boolean confirm(Object token) {
			if( token == null )
				return false;
			boolean retval = token.equals( IJp2pDSComponent.S_IP2P_TOKEN ); 
			if( retval )
				return ( retval );
			return token.equals(this.providedToken );
		}	
	}

	private class RefreshRunnable implements Runnable{

		private ExecutorService service;
		private RootPropertySource source;
		
		public RefreshRunnable( RootPropertySource source) {
			super();
			this.source = source;
		}

		/**
		 * Start the runnable thread
		 */
		synchronized void start(){
			if( service != null )
				return;
			service = Executors.newCachedThreadPool();
			service.execute(this);	
		}
		
		/**
		 * Stop the service
		 */
		public void stop(){
			Thread.currentThread().interrupt();
		}
		
		@Override
		public void run() {
			ServiceEventDispatcher dispatcher = ServiceEventDispatcher.getInstance();
			dispatcher.serviceChanged( new ServiceChangedEvent(this, ServiceChange.REFRESH ));
			try{
				Thread.sleep((long) this.source.getProperty( PetitionerProperties.REFRESH_TIME ));
			}
			catch( InterruptedException ex ){
				Jp2pLog.logWarning( Jp2pContainerPetitioner.S_WRN_THREAD_INTERRUPTED );
			}
			service = null;
		}
	}
}