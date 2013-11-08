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

import org.envirocar.app.R;
import org.envirocar.app.activity.DialogUtil;
import org.envirocar.app.activity.DialogUtil.PositiveNegativeCallback;
import org.envirocar.app.dao.DAOException;
import org.envirocar.app.dao.DAOProvider;
import org.envirocar.app.dao.DAOProvider.AsyncExecutionWithCallback;
import org.envirocar.app.dao.TermsOfUseRetrievalException;
import org.envirocar.app.exception.ServerException;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.TermsOfUse;
import org.envirocar.app.model.TermsOfUseInstance;

import android.app.Activity;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

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
				TermsOfUse response;
				try {
					response = DAOProvider.instance().getTermsOfUseDAO().getTermsOfUse();
					setList(response);
					retrieveLatestInstance();
				} catch (TermsOfUseRetrievalException e) {
					logger.warn(e.getMessage(), e);
				}
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
			try {
				TermsOfUseInstance inst = DAOProvider.instance().getTermsOfUseDAO().getTermsOfUseInstance(id);
				setCurrent(inst);
			} catch (TermsOfUseRetrievalException e) {
				logger.warn(e.getMessage(), e);
			}
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

	private void setList(TermsOfUse termsOfUse) {
		logger.info("List of TermsOfUse size: "+termsOfUse.getInstances().size());
		list = termsOfUse;
	}

	public void userAcceptedTermsOfUse(final User user, final String issuedDate) {
		DAOProvider.async(new AsyncExecutionWithCallback<Void>() {

			@Override
			public Void execute() throws DAOException {
				user.setTouVersion(issuedDate);
				DAOProvider.instance().getUserDAO().updateUser(user);
				return null;
			}

			@Override
			public Void onResult(Void result, boolean fail, Exception exception) {
				if (!fail) {
					user.setTouVersion(issuedDate);
					UserManager.instance().setUser(user);
					logger.info("User successfully updated.");
				}
				else {
					logger.warn(exception.getMessage(), exception);
				}
				return null;
			}
		});
	}

	
	/**
	 * verify the user's accepted terms of use version
	 * against the latest from the server
	 * 
	 * @param acceptedTermsOfUseVersion the accepted version of the current user
	 * @return true, if the provided version is the latest
	 * @throws ServerException if the server did not respond (as expected)
	 */
	public static boolean verifyTermsUseOfVersion(
			String acceptedTermsOfUseVersion) throws ServerException {
		if (acceptedTermsOfUseVersion == null) return false;
		
		TermsOfUseInstance current = instance().getCurrentTermsOfUse();
		
		return current.getIssuedDate().equals(acceptedTermsOfUseVersion);
	}
	
	/**
	 * Checks if the Terms are accepted. If not, open Dialog. On positive
	 * feedback, update the User.
	 * 
	 * @param user
	 * @param activity
	 * @param callback
	 */
	public static void askForTermsOfUseAcceptance(final User user, final Activity activity,
			final PositiveNegativeCallback callback) {
		boolean verified = false;
		try {
			verified = TermsOfUseManager.verifyTermsUseOfVersion(user.getTouVersion());
		} catch (ServerException e) {
			logger.warn(e.getMessage(), e);
			return;
		}
		if (!verified) {
			
			final TermsOfUseInstance current;
			try {
				current = TermsOfUseManager.instance().getCurrentTermsOfUse();
			} catch (ServerException e) {
				logger.warn("This should never happen!", e);
				return;
			}
			
			DialogUtil.createTermsOfUseDialog(current,
					user.getTouVersion() == null, new DialogUtil.PositiveNegativeCallback() {

				@Override
				public void negative() {
					logger.info("User did not accept the ToU.");
					Crouton.makeText(activity, activity.getString(R.string.terms_of_use_cant_continue), Style.ALERT).show();
					if (callback != null) {
						callback.negative();
					}
				}

				@Override
				public void positive() {
					TermsOfUseManager.instance().userAcceptedTermsOfUse(user, current.getIssuedDate());
					Crouton.makeText(activity, activity.getString(R.string.terms_of_use_updating_server), Style.INFO).show();
					if (callback != null) {
						callback.positive();
					}
				}
						
			}, activity);
		}
		else {
			logger.info("User has accpeted ToU in current version.");
		}
	}

}
