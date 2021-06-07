package org.edu_sharing.repository.server;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;
import org.edu_sharing.service.tracking.TrackingService;
import org.edu_sharing.service.tracking.TrackingServiceFactory;

public abstract class AuthenticationToolAbstract implements AuthenticationTool {

	static Logger log = Logger.getLogger(AuthenticationToolAbstract.class);
	
	@Override
	public void storeAuthInfoInSession(String username, String ticket, String authType, HttpSession session) {
		String currentTicket = (String)session.getAttribute(CCConstants.AUTH_TICKET);
		//ivalidate old ticket when it's not the same
		if(currentTicket != null && !currentTicket.trim().equals("") && !currentTicket.equals(ticket)){
			try{
				this.logout(currentTicket);
			}catch(Throwable e){
				e.printStackTrace();
			}
		}
		if(username!=null && !username.equals(session.getAttribute(CCConstants.AUTH_USERNAME))) {
            session.setAttribute(CCConstants.AUTH_USERNAME, username);
            TrackingServiceFactory.getTrackingService().trackActivityOnUser(username,TrackingService.EventType.LOGIN_USER_SESSION);
        }

		session.setAttribute(CCConstants.AUTH_TICKET, ticket);
		session.setAttribute(CCConstants.AUTH_TYPE, authType);
	}
	
	@Override
	public String getTicketFromSession(HttpSession session) {
		String ticket = (String)session.getAttribute(CCConstants.AUTH_TICKET);
		return ticket;
	}
	
	@Override
	public HashMap<String, String> getAuthentication(HttpSession session) {
		HashMap<String,String> result = new HashMap<String,String>();
		String currentTicket = (String)session.getAttribute(CCConstants.AUTH_TICKET);
		String userName = (String)session.getAttribute(CCConstants.AUTH_USERNAME);
		
		result.put(CCConstants.AUTH_USERNAME, userName);
		result.put(CCConstants.AUTH_TICKET, currentTicket);
		return result;
	}
	
	public String getCurrentUser(){
		if(Context.getCurrentInstance() != null && Context.getCurrentInstance().getRequest() != null){
			HttpSession session = Context.getCurrentInstance().getRequest().getSession();
			if(session != null){
				return (String)session.getAttribute(CCConstants.AUTH_USERNAME);
			}
		}
		return null;
	}
	
	public String getCurrentLocale(){
		try {
			HttpSession session = Context.getCurrentInstance().getRequest().getSession();
			String currentLocale = (String) session.getAttribute(CCConstants.AUTH_LOCALE);
			if (currentLocale == null || currentLocale.trim().equals("")) currentLocale = getPrimaryLocale();
			return currentLocale;
		}catch(Throwable t){
			String primary=getPrimaryLocale();
			log.debug("error fetching current locale from session, will use primary "+primary);
			return primary;
		}
	}
	public String getCurrentLanguage(){
		return getCurrentLocale().substring(0,2);
	}

	/**
	 * returns the current primary locale (basically the first defined locale in the client.config
	 * If it's not defined, it will use de_DE
	 * @return
	 */
	public String getPrimaryLocale() {
		try {
			String language=ConfigServiceFactory.getCurrentConfig().getValue("supportedLanguages",new String[]{"de"})[0];
			return getLocaleFromLanguage(language);
		} catch (Throwable t) {
			log.warn("Error fetching the default language from config",t);
			return "de_DE";
		}
	}

	private String getLocaleFromLanguage(String language){
		Map<String,String> locales = new HashMap<>();
		locales.put("de","de_DE");
		locales.put("en","en_US");
		return locales.get(language);
	}
}
