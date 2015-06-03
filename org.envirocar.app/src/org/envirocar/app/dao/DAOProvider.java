/* 
 * enviroCar 2013
 * Copyright (C) 2013  
 * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 * 
 */
package org.envirocar.app.dao;

import org.envirocar.app.Injector;
import org.envirocar.app.application.ContextInternetAccessProvider;
import org.envirocar.app.application.TermsOfUseManager;
import org.envirocar.app.application.UserManager;
import org.envirocar.app.dao.cache.CacheAnnouncementsDAO;
import org.envirocar.app.dao.cache.CacheFuelingDAO;
import org.envirocar.app.dao.cache.CacheSensorDAO;
import org.envirocar.app.dao.cache.CacheTermsOfUseDAO;
import org.envirocar.app.dao.cache.CacheTrackDAO;
import org.envirocar.app.dao.cache.CacheUserDAO;
import org.envirocar.app.dao.exception.DAOException;
import org.envirocar.app.dao.remote.RemoteAnnouncementsDAO;
import org.envirocar.app.dao.remote.RemoteFuelingDAO;
import org.envirocar.app.dao.remote.RemoteSensorDAO;
import org.envirocar.app.dao.remote.RemoteTermsOfUseDAO;
import org.envirocar.app.dao.remote.RemoteTrackDAO;
import org.envirocar.app.dao.remote.RemoteUserDAO;
import org.envirocar.app.util.Util;

import android.content.Context;
import android.os.AsyncTask;

import java.io.File;

import javax.inject.Inject;

/**
 * the {@link DAOProvider} consists a set of methods
 * to access specific DAOs. It checks the internet connection
 * and decides whether it should use a Cache DAO or a Remote one.
 * 
 * @author matthes rieke
 *
 */
public class DAOProvider {

	private Injector mInjector;
	private InternetAccessProvider mInternetAccessProvider;
	private CacheDirectoryProvider mCacheDirectoryProvider;

	/**
	 * Constructor.
	 *
	 * @param context
	 */
	public DAOProvider(final Context context){
		((Injector) context).injectObjects(this);
		this.mInternetAccessProvider = new ContextInternetAccessProvider(context);
		this.mCacheDirectoryProvider = new CacheDirectoryProvider() {
					@Override
					public File getBaseFolder() {
						return Util.resolveCacheFolder(context);
					}
				};
	}

	/**
	 * @return the {@link SensorDAO}
	 */
	public SensorDAO getSensorDAO() {
		if (this.mInternetAccessProvider.isConnected()) {
			return new RemoteSensorDAO(new CacheSensorDAO(this.mCacheDirectoryProvider));
		}
		return new CacheSensorDAO(this.mCacheDirectoryProvider);
	}
	
	
	/**
	 * @return the {@link TrackDAO}
	 */
	public TrackDAO getTrackDAO() {
		if (this.mInternetAccessProvider.isConnected()) {
			return new RemoteTrackDAO();
		}
		return new CacheTrackDAO();
	}
	
	/**
	 * @return the {@link UserDAO}
	 */
	public UserDAO getUserDAO() {
		if (this.mInternetAccessProvider.isConnected()) {
			return new RemoteUserDAO();
		}
		return new CacheUserDAO();
	}
	
	/**
	 * @return the {@link FuelingDAO}
	 */
	public FuelingDAO getFuelingDAO() {
		if (this.mInternetAccessProvider.isConnected()) {
			return new RemoteFuelingDAO(new CacheFuelingDAO(this.mCacheDirectoryProvider));
		}
		return new CacheFuelingDAO(this.mCacheDirectoryProvider);
	}
	
	/**
	 * @return the {@link TermsOfUseDAO}
	 */
	public TermsOfUseDAO getTermsOfUseDAO() {
		if (this.mInternetAccessProvider.isConnected()) {
			return new RemoteTermsOfUseDAO(new CacheTermsOfUseDAO(this.mCacheDirectoryProvider));
		}
		return new CacheTermsOfUseDAO(this.mCacheDirectoryProvider);
	}


	public AnnouncementsDAO getAnnouncementsDAO() {
		if (this.mInternetAccessProvider.isConnected()) {
			return new RemoteAnnouncementsDAO(new CacheAnnouncementsDAO(this.mCacheDirectoryProvider));
		}
		return new CacheAnnouncementsDAO(this.mCacheDirectoryProvider);
	}
	
	public static <T> void async(final AsyncExecutionWithCallback<T> callback) {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				boolean fail = true;
				T result = null;
				Exception ex = null;
				try {
					result = callback.execute();
					fail = false;
				} catch (RuntimeException e) {
					ex = e;
				} catch (DAOException e) {
					ex = e;
				}
				callback.onResult(result, fail, ex);
				return null;
			}
		}.execute();
	}
	
	public static interface AsyncExecutionWithCallback<T> {
		
		public T execute() throws DAOException;
		
		public T onResult(T result, boolean fail, Exception exception);
		
	}

}
