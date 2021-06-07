package org.edu_sharing.repository.server.authentication;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;

public class SameSiteCookieFilter implements javax.servlet.Filter {
	
	
	
	
	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if(request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            request = new ForceSameSiteNoneSessionCookieRequestWrapper((HttpServletRequest) request, (HttpServletResponse) response);
        }

        chain.doFilter(request, response);
		
	}
	
	public static class ForceSameSiteNoneSessionCookieRequestWrapper extends HttpServletRequestWrapper {
		
		Logger logger = Logger.getLogger(ForceSameSiteNoneSessionCookieRequestWrapper.class);
		
		HttpServletResponse response;

        public ForceSameSiteNoneSessionCookieRequestWrapper(HttpServletRequest request, HttpServletResponse response) {
            super(request);
            
            this.response = response;
        }

        @Override
        public HttpSession getSession(boolean create) {
            if(create) {
                HttpSession session = super.getSession(create);
                
                updateCookie(response.getHeaders("Set-Cookie"));

                return session;
            }
            return super.getSession(create);
        }

        @Override
        public HttpSession getSession() {
            HttpSession session = super.getSession();
            if(session != null) {
            	updateCookie(response.getHeaders("Set-Cookie"));
            }

            return session;
        }

        protected void updateCookie(Collection<String> cookiesAfterCreateSession) {
        	
        	String cookieAttributes = ApplicationInfoList.getHomeRepository().getCookieAttributes();
        	
        	boolean forcedByConfig = false;
        	//default handling
        	if(cookieAttributes == null) {
        		cookieAttributes = "SameSite=None";
        	}//plain tomcat JSESSIONID
        	else if(cookieAttributes.trim().equals("")) {
        		logger.debug("default JSESSIONID. No cookie attribute set.");
        		return;
        	}else {
        		forcedByConfig = true;
        		logger.debug("using cookieAttributes definition of config:" + cookieAttributes);
        	}
            if(cookiesAfterCreateSession != null && !response.isCommitted()) {
                // search if a cookie JSESSIONID Secure exists
            	final String fCookieAttributes = cookieAttributes;
            	
            	//dont send on when Secure is off
            	if(!forcedByConfig) {
            		Optional<String> cookieJSessionId = cookiesAfterCreateSession.stream()
                            .filter(cookie -> cookie.startsWith("JSESSIONID") && !cookie.contains("Secure"))
							.findAny();
            		if(cookieJSessionId.isPresent()) {
            			logger.debug("no Secure Attribut found. will not set SameSite cookie");
            			return;
            		}
            	}
            	
                Optional<String> cookieJSessionId = cookiesAfterCreateSession.stream()
                                                        .filter(cookie -> cookie.startsWith("JSESSIONID") && !cookie.contains(fCookieAttributes))
                										.findAny();
                if(cookieJSessionId.isPresent()) {
                    
                	for(String cookieEle : cookiesAfterCreateSession) {
                		logger.debug("cookieEle:" +cookieEle);
                	}
                	
                	// remove all Set-Cookie and add the unsecure version of the JSessionId Cookie
                    response.setHeader("Set-Cookie", cookieJSessionId.get() + "; " + fCookieAttributes);
                    

                    // re-add all other Cookies
                    cookiesAfterCreateSession.stream()
                            .filter(cookie -> !cookie.startsWith("JSESSIONID"))
                            .forEach(cookie -> response.addHeader("Set-Cookie", cookie));
                	
                }
            }
        }
    }
	
	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}
}
