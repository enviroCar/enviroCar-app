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
package org.envirocar.app.application;

import java.util.concurrent.TimeoutException;

import org.apache.http.Header;
import org.envirocar.app.exception.ServerException;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.TermsOfUse;
import org.envirocar.app.model.TermsOfUseInstance;
import org.envirocar.app.network.RestClient;
import org.json.JSONException;
import org.json.JSONObject;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;

public class TermsOfUseManager {

	private static final Logger logger = Logger.getLogger(TermsOfUseManager.class);
	private static TermsOfUseManager instance;
	private TermsOfUseInstance current;
	protected TermsOfUse list;
	private Object downloadMutex = new Object();

	private TermsOfUseManager() {
		try {
			retrieveTermsOfUse();
		} catch (ServerException e) {
			logger.warn(e.getMessage(), e);
		}
	}
	
	public static TermsOfUseManager instance() {
		if (instance == null) {
			instance = new TermsOfUseManager();
		}
		return instance;
	}
	
	public TermsOfUseInstance getCurrentTermsOfUse() throws ServerException {
		if (this.current == null) {
			retrieveTermsOfUse();
		}
		
		return current;
	}

	public TermsOfUse getInstancesReferences() {
		return list;
	}
	
	private void retrieveTermsOfUse() throws ServerException {
		new Thread(new Runnable() {
			@Override
			public void run() {
				RestClient.downloadTermsOfUse(new JsonHttpResponseHandler() {
					@Override
					public void onSuccess(int statusCode, Header[] headers,
							JSONObject response) {
						try {
							setList(TermsOfUse.fromJson(response));
							retrieveLatestInstance();
						} catch (JSONException e) {
							logger.warn(e.getMessage(), e);
						}
					}
					
					@Override
					public void onFailure(Throwable error, String content) {
						logger.warn("Server error: '"+error+"'; "+content);
					}
					
				});				
			}
		}).start();
		
		synchronized (downloadMutex) {
			while (current == null) {
				try {
					downloadMutex.wait(5000);
					
					if (current == null) {
						throw new ServerException(new TimeoutException("Waiting to long for a response."));
					}
				} catch (InterruptedException e) {
					throw new ServerException(e);
				}
			}
		}
	}

	private void retrieveLatestInstance() {
		if (list != null && list.getInstances() != null && list.getInstances().size() > 0) {
			String id = list.getInstances().get(0).getId();
			RestClient.downloadTermsOfUseInstance(id, new JsonHttpResponseHandler() {
				@Override
				public void onSuccess(int statusCode, Header[] headers,
						JSONObject response) {
					try {
						setCurrent(TermsOfUseInstance.fromJson(response));
					} catch (JSONException e) {
						logger.warn(e.getMessage(), e);
					}
				}	
				
				@Override
				public void onFailure(Throwable error, String content) {
					logger.warn("Server error: '"+error+"'; "+content);
				}
			});
		}
		else {
			logger.warn("Could not retrieve latest instance as their is no list available!");
		}
	}

	private void setCurrent(TermsOfUseInstance t) {
		logger.info("Current Terms Of Use: "+ t.getIssuedDate());
		current = t;
		
		synchronized (downloadMutex) {
			downloadMutex.notifyAll();
		}
	}

	private void setList(TermsOfUse termsOfUse) throws JSONException {
		logger.info("List of TermsOfUse size: "+termsOfUse.getInstances().size());
		list = termsOfUse;
	}

	public void userAcceptedTermsOfUse(final User user, final String issuedDate) {
		RestClient.updateAcceptedTermsOfUseVersion(user, issuedDate, new AsyncHttpResponseHandler() {
			@Override
			public void onSuccess(int statusCode, String content) {
				user.setAcceptedTermsOfUseVersion(issuedDate);
				//TODO verify the user resource on the server
				UserManager.instance().setUser(user);
				logger.info("User successfully updated.");
			}
			
			@Override
			public void onFailure(Throwable error, String content) {
				logger.warn(content, error);
			}
		});
	}
	

}
