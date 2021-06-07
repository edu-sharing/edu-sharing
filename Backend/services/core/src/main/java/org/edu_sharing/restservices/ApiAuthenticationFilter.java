package org.edu_sharing.restservices;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.authentication.subsystems.SubsystemChainingAuthenticationService;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.authentication.AuthenticationFilter;
import org.edu_sharing.service.authentication.EduAuthentication;
import org.edu_sharing.service.authentication.oauth2.TokenService;
import org.edu_sharing.service.authentication.oauth2.TokenService.Token;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;
import org.springframework.context.ApplicationContext;

public class ApiAuthenticationFilter implements javax.servlet.Filter {

	Logger logger = Logger.getLogger(ApiAuthenticationFilter.class);
	
	private TokenService tokenService;
	private EduAuthentication eduAuthenticationService;
	
	private AuthenticationComponent authenticationComponent;
	
	@Override
	public void destroy() {
	}
	
	@Override
	public void doFilter(ServletRequest req, ServletResponse resp,
			FilterChain chain) throws IOException, ServletException {
		
		HttpServletRequest httpReq = (HttpServletRequest) req;
		HttpServletResponse httpResp = (HttpServletResponse)resp;

		if("OPTIONS".equals(httpReq.getMethod())){
			chain.doFilter(req, resp);
			return;
		}
				
		HttpSession session = httpReq.getSession(true);
		//session.setMaxInactiveInterval(30);
		AuthenticationToolAPI authTool = new AuthenticationToolAPI();
		HashMap<String, String> validatedAuth = authTool.validateAuthentication(session);

		AuthenticationFilter.handleLocale(true, httpReq.getHeader("locale"), httpReq, httpResp);

		String authHdr = httpReq.getHeader("Authorization");

		// always take the header so we can auth when a guest is activated
		if (authHdr != null) {			
				
				if (authHdr.length() > 5 && authHdr.substring(0, 5).equalsIgnoreCase("BASIC")) {
					logger.debug("auth is BASIC");
					validatedAuth = httpBasicAuth(authHdr);
					if(validatedAuth != null) {
						String succsessfullAuthMethod = SubsystemChainingAuthenticationService.getSuccessFullAuthenticationMethod();
						String authMethod = ("alfrescoNtlm1".equals(succsessfullAuthMethod) || "alfinst".equals(succsessfullAuthMethod)) ? CCConstants.AUTH_TYPE_DEFAULT : CCConstants.AUTH_TYPE + succsessfullAuthMethod;
						String username = validatedAuth.get(CCConstants.AUTH_USERNAME);
						authTool.storeAuthInfoInSession(username, validatedAuth.get(CCConstants.AUTH_TICKET), authMethod, session);
					}
				} else if (authHdr.length() > 6 && authHdr.substring(0, 6).equalsIgnoreCase("Bearer")) {
					
					logger.info("auth is OAuth");
					
					String accessToken = authHdr.substring(6).trim();
					
					try {
						Token token = tokenService.getToken(accessToken);
						
						if (token != null) {
							logger.info("oAuthToken:"+ token.getAccessToken() +" alfresco ticket:"+ token.getTicket());
							
							//validate and set current user
							authTool.storeAuthInfoInSession(
									token.getUsername(), 
									token.getTicket(),
									CCConstants.AUTH_TYPE_OAUTH,
									session);
							
							session.setAttribute(CCConstants.AUTH_ACCESS_TOKEN, token.getAccessToken());
							
							validatedAuth = authTool.validateAuthentication(session);							
						}	
					} catch (Exception ex) {
						
						logger.error(ex.getMessage(), ex);
					}				
				}else if (authHdr.length() > 10 && authHdr.substring(0, 10).equalsIgnoreCase(CCConstants.AUTH_HEADER_EDU_TICKET)) {
					String ticket = authHdr.substring(10).trim();
					if(ticket != null){
						if(authTool.validateTicket(ticket)){
							// Force a renew of all toolpermissions since they might have now changed!
							ToolPermissionServiceFactory.getInstance().getAllAvailableToolPermissions(true);
		  					//if its APIClient username is ignored and is figured out with authentication service
		  					authTool.storeAuthInfoInSession(authTool.getCurrentUser(), ticket, CCConstants.AUTH_TYPE_TICKET, httpReq.getSession());
		  					validatedAuth = authTool.validateAuthentication(session);
		  				}
					}
				}
			
		}

		List<String> AUTHLESS_ENDPOINTS=Arrays.asList(new String[]{"/authentication","/_about","/config","/register","/sharing"});
		List<String> ADMIN_ENDPOINTS=Arrays.asList(new String[]{"/admin", "/bulk"});
		List<String> DISABLED_ENDPOINTS=new ArrayList<>();

		try {
			if(!ConfigServiceFactory.getCurrentConfig(req).getValue("register.local",true)){
				DISABLED_ENDPOINTS.add("/register");
			}
		} catch (Exception e) {}

		boolean noAuthenticationNeeded=false;
		for(String endpoint : AUTHLESS_ENDPOINTS){
			if(httpReq.getPathInfo().startsWith(endpoint)){
				noAuthenticationNeeded=true;
				break;
			}
		}
		boolean adminRequired=false;
		for(String endpoint : ADMIN_ENDPOINTS){
			if(httpReq.getPathInfo().startsWith(endpoint)){
				adminRequired=true;
				break;
			}
		}

		for(String endpoint : DISABLED_ENDPOINTS){
			if(httpReq.getPathInfo().startsWith(endpoint)){
				httpResp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				httpResp.flushBuffer();
				httpResp.getWriter().print("This endpoint is disabled via config");
				return;
			}
		}
		
		if(adminRequired && !AuthorityServiceFactory.getLocalService().isGlobalAdmin()){
			httpResp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			httpResp.flushBuffer();
			httpResp.getWriter().print("Admin rights are required for this endpoint");
			return;
		}
		// ignore the auth for the login
		if(validatedAuth == null && !noAuthenticationNeeded){
			if(httpReq.getPathInfo().equals("/swagger.json"))
				httpResp.setHeader("WWW-Authenticate", "BASIC realm=\""+ "Edu-Sharing Rest API" +"\"");
			httpResp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			httpResp.flushBuffer();
			return;
		}
		
		// Chain other filters
		chain.doFilter(req, resp);
	}

	public static HashMap<String, String> httpBasicAuth(String authHdr) {
		HashMap<String, String> validatedAuth = null;
		AuthenticationToolAPI authTool = new AuthenticationToolAPI();

		// Basic authentication details present

		String basicAuth = new String(java.util.Base64.getDecoder().decode(authHdr.substring(6)), StandardCharsets.ISO_8859_1);

		// Split the username and password

		String username = null;
		String password = null;

		int pos = basicAuth.indexOf(":");
		if (pos != -1) {
			username = basicAuth.substring(0, pos);
			password = basicAuth.substring(pos + 1);
		} else {
			username = basicAuth;
			password = "";
		}

		try {

			// Authenticate the user
			validatedAuth = authTool.createNewSession(username, password);
		} catch (Exception ex) {
			Logger.getLogger(ApiAuthenticationFilter.class).error(ex.getMessage(), ex);
		}
		return validatedAuth;
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		
		ApplicationContext eduApplicationContext = 
				org.edu_sharing.spring.ApplicationContextFactory.getApplicationContext();

		tokenService = (TokenService) eduApplicationContext.getBean("oauthTokenService");		
		eduAuthenticationService = (EduAuthentication) eduApplicationContext.getBean("authenticationService");
		
		ApplicationContext alfApplicationContext = AlfAppContextGate.getApplicationContext();
		
		authenticationComponent = (AuthenticationComponent) alfApplicationContext.getBean("authenticationComponent");

	}

}
