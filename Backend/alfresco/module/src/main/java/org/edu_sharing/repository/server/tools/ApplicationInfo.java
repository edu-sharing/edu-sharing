/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */
package org.edu_sharing.repository.server.tools;

import java.io.Serializable;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

public class ApplicationInfo implements Comparable<ApplicationInfo>, Serializable{

	public static final long DEFAULT_OFFSET_MS = 10000;

	/**
	 * property file keys
	 * 
	 */
	public static final String KEY_HOST = "host";
	
	public static final String KEY_DOMAIN = "domain";
	
	public static final String KEY_PORT = "port";
	
	public static final String KEY_PROTOCOL = "protocol";
	
	public static final String KEY_CLIENTPORT = "clientport";
	
	public static final String KEY_CLIENTPROTOCOL = "clientprotocol";
	
	public static final String KEY_TYPE = "type";
	
	public static final String KEY_REPOSITORY_TYPE = "repositorytype";
	
	public static final String KEY_TRUSTEDCLIENT = "trustedclient";
	
	public static final String KEY_SEARCHABLE = "searchable";
	
	public static final String KEY_APPCAPTION = "appcaption";
	
	public static final String KEY_APPID = "appid";
	
	public static final String KEY_USERNAME = "username";
	
	public static final String KEY_PASSWORD = "password";
	
	public static final String KEY_LOGO = "logo";
	
	public static final String KEY_ICON = "icon";
	
	public static final String KEY_AUTHENTICATIONWEBSERVICE = "authenticationwebservice";
	
	public static final String KEY_WEBAPPNAME = "webappname";
	
	public static final String KEY_WSPATH = "wspath";
	
	public static final String KEY_WSHOTPATH = "wshotpath";
	
	public static final String KEY_ALFRESCOCONTEXT = "alfrescocontext";
	
	public static final String KEY_SEARCHCLASS = "searchclass";

	public static final String KEY_REMOTE_PROVIDER = "remote_provider";

	public static final String KEY_AUTHENTICATIONTOOLCLASS = "authenticationtoolclass";
	
	public static final String KEY_CONTENTURL = "contenturl";

	public static final String KEY_URL_DYNAMIC = "url_dynamic";

	public static final String KEY_PREVIEWURL = "previewurl";
	
	public static final String KEY_IS_HOME_NODE = "is_home_node";
	
	public static final String KEY_CUSTOM_HTML_HEADERS = "custom_html_headers";

	public static final String KEY_METADATASETS_V2 = "metadatasetsV2";
	
	public static final String KEY_PUBLIC_KEY = "public_key";
	public static final String KEY_PRIVATE_KEY = "private_key";
	
	public static final String KEY_MESSAGE_OFFSET_MILLISECONDS = "message_offset_ms";
	public static final String KEY_MESSAGE_SEND_OFFSET_MILLISECONDS = "message_send_offset_ms";
	
	public static final String KEY_LOGOUT_URL = "logouturl";
	
	public static final String KEY_API_KEY = "apikey";
	public static final String KEY_API_URL = "apiurl";

	public static final String KEY_ORDER = "order";

	public static final String KEY_ALLOW_ORIGIN = "allow_origin";

	public static final String KEY_COOKIE_ATTRIBUTES = "cookie_attributes";

	/**
	 * property file vals
	 */
	public static final String TYPE_REPOSITORY = "REPOSITORY";
	public static final String TYPE_LMS = "LMS";
	public static final String TYPE_CMS = "CMS";

	/**
	 * SERVICE = renderservice -> reserved, no other system can use this
	 */
	public static final String TYPE_RENDERSERVICE = "SERVICE";

	/**
	 * LEARNING_LOCKER = reserved for a learning locker system
	 */
	public static final String TYPE_LEARNING_LOCKER = "LEARNING_LOCKER";

	public static final String TYPE_CONNECTOR = "CONNECTOR";

	/**
	 * ROCKETCHAT = reserved for a rocketchat system
	 */
	public static final String TYPE_ROCKETCHAT = "ROCKETCHAT";


	public static final String REPOSITORY_TYPE_ALFRESCO = "ALFRESCO"; // a remote alfresco repository
	
	public static final String REPOSITORY_TYPE_LOCAL = "LOCAL"; // the local alfreso repository
	
	public static final String REPOSITORY_TYPE_EDUNEX = "EDUNEX";
	
	public static final String REPOSITORY_TYPE_YOUTUBE = "YOUTUBE";
	
	public static final String REPOSITORY_TYPE_DDB = "DDB";
	
	public static final String REPOSITORY_TYPE_WIKIMEDIA = "WIKIMEDIA";

	public static final String AUTHBYAPP_USERNAME_PROP_MAIL = "MAIL";
	
	public static final String AUTHBYAPP_USERNAME_PROP_USERNAME = "USERNAME";

	public static final String AUTHBYAPP_USER_WHITELIST = "auth_by_app_user_whitelist";
	
	public static final String WEBSITEPREVIEWRENDERSERVICE = "websitepreviewrenderservice";

	public static final String NOTIFY_FETCH_LIMIT = "notify_fetch_limit";

	public static final String REPOSITORY_TYPE_MEMUCHO = "MEMUCHO";

	/**
	 * Remote sso userid mapping. Only for remote alfresco repos
	 * If it is not set, the one from the edu-sharing-sso-context.xml will be used
	 * @TODO make the sso config not influcene remote logins!
	 */
	public static final String REMOTE_USERID = "remote_userid";
	public static final String FORCED_USER = "forced_user";

	public static final String PROPERTY_VALIDATOR_REGEX_CM_NAME = "property_validator_regex_cm_name";

	private final Properties properties;

	private String host = null;
	
	/**
	 * an , separated List of Hosts, maybe ip ranges in future
	 */
	private String hostAliases = null;
	
	/**
	 * used for previewurls or content urls, usefull when we got an proxy
	 */
	private String domain = null;
	
	private String alfrescocontext = null;
	
	private String port = null;
	
	private String protocol = null;
	
	private String clientport = null;
	
	private String clientprotocol = null;
	
	private String wspath = null;
	
	private String wshotpath = null;
	
	private String webappname = null;
	
	private String path = null;

	private String appFile = null;
	
	private String ishomeNode = null;
	
	private String username = null;
	
	private String password = null;
	
	private String logo = null;
	
	private String icon = null;

	private String guest_username = null;
	
	private String guest_password = null;
	
	private String appCaption = null;
	
	private String appId = null;
	
	private String trustedclient = null;
	
	private String type = null;
	
	private String authenticationwebservice = null;
	
	private String permissionwebservice = null;
	
	private String subtype = null;

	private String repositoryType = null;
	
	/**
	 * this is a property used to redirect to content deliverd by repositories renderservice
	 */
	private String contentUrl = null;

	/**
	 * this is a property used redirect to preview deliverd by repositories renderservice
	 */
	private String previewUrl = null;
	
	private String customHtmlHeaders = null;

	private String logoutUrl = null;

	private String websitepreviewrenderservice;
	
	private String searchable = "true";
	
	//file that contains metadatasets for the repository
	private String metadatsetsV2 = null;
	
	//devmode metadatasets will be parsed every time and not cached in RepoFactory
	private String devmode = null;
	
	//recommend objects search
	private String recommend_objects_query = null;
	
	
	//for lms: if this mail is configured and the user authenticates with this mail than we authenticate without sending an confirmation mail
	private String trustedEmailAddress = null;
	
	//allow to switch from email as username to username like the app. the static vars AUTHBYAPP_USERNAME_PROP_MAIL, AUTHBYAPP_USERNAME_PROP_USERNAME are valid values 
	private String authByAppUsernameProp = null;
	
	//allow login from applications without sending an confirmation mail
	private boolean authByAppSendMail = false;

	private String authByAppUserWhitelist = null;
	
	private String publicKey = null;
	
	private String privateKey = null;
	
	private String messageOffsetMs = null;
	
	private String messageSendOffsetMs = null;
	
	
	/**
	 * allow the mapping from the username send as an authByApp param to an userDirectory Attribute
	 * the mapped value will be taken as the repositoryusername
	 * 
	 * authByAppUsernameMappingDirectoryUsername: the property which is used as a key to ask the userDirectory
	 * 
	 * auth_by_app_usernamemapping_dir_username
	 * 
	 * authByAppUsernameMappingRepositoryUsername: the userdirectory property which value is used as respository username 
	 * 
	 * auth_by_app_usernamemapping_rep_username
	 */
	private String authByAppUsernameMappingDirectoryUsername = null;
	
	private String authByAppUsernameMappingRepositoryUsername = null;
	
	//an "," separated String with authentication Types that need to be explicit activated. i.e. valid value is "shibboleth"
	private String allowedAuthenticationTypes = null;
	
	//for lms or other connected systems: if they call the gwt app with the css_appid param then the custom css file is included in the html
	private String customCss = null;
	
	private int trackingBufferSize = 0;
	
	private String apiKey;
	
	private int order;

	private String xml;

	/**
	 * der Anfangsteil des alfresco Intergity Pattern:
	 * (.*[\"\*\\\>\<\?\/\:\|]+.*)|(.*[\.]?.*[\.]+$)|(.*[ ]+$)
	 * so das nur die kritischen Zeichen matchen und nicht der ganze string
	 */
	//default value ([\"\*\\\\\>\<\?\/\:\|'\r\n])
	private String validatorRegexCMName = "([\\\"\\*\\\\\\\\\\>\\<\\?\\/\\:\\|'\\r\\n])";

	private String cookieAttributes;
	private Map<CacheKey, Serializable> cache = new HashMap<>();

	public ApplicationInfo(String _appFile) throws Exception{
		if(_appFile == null) throw new Exception("Application Filename was null!");
		appFile = _appFile;
		
		ClassLoader classLoader = Thread.currentThread()
				.getContextClassLoader();
		URL url = classLoader.getResource(appFile);
		xml = new String(Files.readAllBytes(Paths.get(url.toURI())));
		
		//test if file exists if not exception is thrown
		//properties.getProperty("is_home_node");
		properties = PropertiesHelper.getProperties(appFile,PropertiesHelper.XML);
		
		host = properties.getProperty(KEY_HOST);
				
		hostAliases = properties.getProperty("host_aliases");
		
		domain = properties.getProperty(KEY_DOMAIN);
		
		port = properties.getProperty(KEY_PORT);
		if(port == null || port.trim().equals("")){
			port="80";
		}
		
		protocol = properties.getProperty(KEY_PROTOCOL);
		if(protocol == null || protocol.trim().equals("")){
			protocol = "http";
		}
		
		clientport =  properties.getProperty(KEY_CLIENTPORT);
		clientprotocol =  properties.getProperty(KEY_CLIENTPROTOCOL);
		
		wspath = properties.getProperty(KEY_WSPATH);
		ishomeNode = properties.getProperty(KEY_IS_HOME_NODE);
		if(ishomeNode == null){
			ishomeNode = "false";
		}
		wshotpath = properties.getProperty(KEY_WSHOTPATH);
		
		webappname = properties.getProperty(KEY_WEBAPPNAME);
		if(webappname == null) webappname = "edu-sharing";
		
		username = properties.getProperty(KEY_USERNAME);
		
		password = properties.getProperty(KEY_PASSWORD);
		
		
		
		guest_username = properties.getProperty("guest_username");
		
		guest_password = properties.getProperty("guest_password");
		
		logo = properties.getProperty(KEY_LOGO);
		
		icon = properties.getProperty(KEY_ICON);

		appCaption = properties.getProperty(KEY_APPCAPTION);
		
		appId = properties.getProperty(KEY_APPID);
		
		trustedclient = properties.getProperty(KEY_TRUSTEDCLIENT);
		
		type = properties.getProperty(KEY_TYPE);
		
		authenticationwebservice = properties.getProperty("authenticationwebservice");
		
		permissionwebservice = properties.getProperty("permissionwebservice");

		subtype  = properties.getProperty("subtype");

		repositoryType = properties.getProperty(KEY_REPOSITORY_TYPE);
		
		contentUrl = properties.getProperty(KEY_CONTENTURL);

		previewUrl = properties.getProperty(KEY_PREVIEWURL);
		
		customHtmlHeaders = properties.getProperty(KEY_CUSTOM_HTML_HEADERS);

		logoutUrl = properties.getProperty(KEY_LOGOUT_URL);

		searchable = properties.getProperty(KEY_SEARCHABLE);

		if(searchable == null) searchable = "true";
		
		path = properties.getProperty("path");

		metadatsetsV2 = properties.getProperty(KEY_METADATASETS_V2);
		
		devmode = properties.getProperty("devmode");

		devmode = (devmode == null)? "false": devmode;
		
		alfrescocontext = properties.getProperty(KEY_ALFRESCOCONTEXT);
		
		recommend_objects_query = properties.getProperty("recommend_objects_query");
		
		trustedEmailAddress = properties.getProperty("trusted_emailaddress");
		
		authByAppUsernameProp = properties.getProperty("auth_by_app_username_prop");
		authByAppUsernameProp = (authByAppUsernameProp == null)? authByAppUsernameProp = ApplicationInfo.AUTHBYAPP_USERNAME_PROP_USERNAME : authByAppUsernameProp;
		
		String tmpAuthByAppSendmail = properties.getProperty("auth_by_app_sendmail");
		authByAppSendMail = (tmpAuthByAppSendmail == null)? true : new Boolean(tmpAuthByAppSendmail);
		
		authByAppUsernameMappingDirectoryUsername = properties.getProperty("auth_by_app_usernamemapping_dir_username");
		
		authByAppUsernameMappingRepositoryUsername = properties.getProperty("auth_by_app_usernamemapping_rep_username");

		authByAppUserWhitelist = properties.getProperty(AUTHBYAPP_USER_WHITELIST);

		allowedAuthenticationTypes = properties.getProperty("allowed_authentication_types");
		
		customCss = properties.getProperty("custom_css");
		
		String tmpTrackingBufferSize = properties.getProperty("trackingBufferSize");
		trackingBufferSize = (tmpTrackingBufferSize != null ? Integer.parseInt(tmpTrackingBufferSize) : 0);
		
		publicKey = properties.getProperty(KEY_PUBLIC_KEY);
		
		privateKey = properties.getProperty(KEY_PRIVATE_KEY);
		
		messageOffsetMs = properties.getProperty(KEY_MESSAGE_OFFSET_MILLISECONDS);
		
		messageSendOffsetMs = properties.getProperty(KEY_MESSAGE_SEND_OFFSET_MILLISECONDS);
		
		apiKey = properties.getProperty(KEY_API_KEY);

		websitepreviewrenderservice = properties.getProperty(WEBSITEPREVIEWRENDERSERVICE);

		String orderString = properties.getProperty(KEY_ORDER);
		order = orderString==null ? (ishomeNode() ? 0 : 1) : Integer.parseInt(orderString);

		cookieAttributes = properties.getProperty(KEY_COOKIE_ATTRIBUTES);

		getWebServiceUrl();
		getWebServerUrl();
		
		String cmNameRegex = properties.getProperty(PROPERTY_VALIDATOR_REGEX_CM_NAME);
		if(cmNameRegex != null && !cmNameRegex.trim().equals("")) {
			validatorRegexCMName = cmNameRegex;
		}

	}
	
	public String getXml() {
		return xml;
	}

	public int getOrder() {
		return order;
	}

	public void setXml(String xml) {
		this.xml = xml;
	}

	public String getHost() {
		return host;
	}

	public String getDomain() {

		if(domain == null) {
			return host;
		}
		
		return domain;
	}

	public String getPort() {
		return port;
	}

	public String getWspath() {
		return wspath;
	}
	public String getBaseUrl(){
		String port = "";
		if(!this.getPort().equals("80") && !this.getPort().equals("443")){
			port = ":" + this.getPort();
		}

		return this.getProtocol()+"://" + this.getHost() + port;
	}
	public String getWebServiceUrl(){
		return this.getBaseUrl() + this.getWspath();
	}
	
	public String getWebServiceHotUrl(){
		return getWebServiceHotUrl(false);
	}
	
	public String getWebServiceHotUrl(boolean external){
		String port = ""; 
		if(external){
			if(!this.getClientport().equals("80") && !this.getClientport().equals("443")){
				port = ":" + this.getClientport();
			}
			return this.getClientprotocol()+"://" + this.getDomain() + port + this.getWshotpath();
		}else{
			if(!this.getPort().equals("80") && !this.getPort().equals("443")){
				port = ":" + this.getPort();
			}
			return this.getProtocol()+"://" + this.getHost() + port + this.getWshotpath();
		}
	}

	public String getString(String key,String defaultValue){
		return properties.getProperty(key,defaultValue);
	}

	public int getInteger(String key,int defaultValue){
		String property = properties.getProperty(key);
		if(property==null || property.isEmpty())
			return defaultValue;
		return Integer.parseInt(property);
	}

	public boolean getBoolean(String key,boolean defaultValue){
		String property = properties.getProperty(key);
		if(property==null || property.isEmpty())
			return defaultValue;
		return property.equalsIgnoreCase("true");
	}
	public String getWebServerUrl(){
		String port = ""; 
		if(!this.getPort().equals("80") && !this.getPort().equals("443")){
			port = ":" + this.getPort();
		}
		
		return this.getProtocol()+"://"+this.getHost()+port;
	}
	
	public String getClientBaseUrl(){
		String result = this.getClientprotocol() + "://"+ ((this.getDomain() == null) ? this.getHost() : this.getDomain()) +":"+ this.getClientport() + "/"+getWebappname();
		return result;
	}

	public boolean ishomeNode() {
		if(ishomeNode.equals("true")){
			return true;
		}else{
			return false;
		}
	}


	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getGuest_username() {
		return guest_username;
	}

	public String getGuest_password() {
		return guest_password;
	}

	public String getAppCaption() {
		return appCaption;
	}

	public String getAppFile() {
		return appFile;
	}

	public String getAppId() {
		return appId;
	}

	public String getTrustedclient() {
		return trustedclient;
	}

	public String getType() {
		return type;
	}

	public String getAuthenticationwebservice() {
		if(authenticationwebservice==null || authenticationwebservice.isEmpty())
			return getWebServiceHotUrl()+"/authentication";

		return authenticationwebservice;
	}

	public String getPermissionwebservice() {
		
		return permissionwebservice;
	}

	public String getSubtype() {
		return subtype;
	}

	/**
	 * @return the repositoryType
	 */
	public String getRepositoryType() {
		
		return repositoryType;
	}
	
	
	/**
	 *this is a property used redirect to content deliverd by repositories renderservice
	 * 
	 * @return the renderServiceUrl
	 */
	public String getContentUrl() {
		
		return contentUrl;
	}

	/**
	 * this is a property used redirect to preview deliverd by repositories renderservice
	 * 
	 * @return the previewUrl
	 */
	public String getPreviewUrl() {
		
		return previewUrl;
	}
	
	public String getCustomHtmlHeaders() {
		return customHtmlHeaders;
	}

	public String getLogoutUrl() {
		
		if(logoutUrl == null || logoutUrl.trim().equals("")){
			return "/edu-sharing/logout";
		}
		
		return logoutUrl;
	}

	/**
	 * @return the searchable
	 */
	public boolean getSearchable() {
		
		
		if(searchable != null && searchable.trim().equals("true")){
			return true;
		}else{
			return false;
		}
		
	}
	
	/**
	 * @return the path
	 */
	public String getPath() {
		
		
		return path;
	}

	/**
	 * @return the metadatsetsV2
	 */
	public String[] getMetadatsetsV2() {
		if(metadatsetsV2==null)
			return new String[]{"mds"};
		return metadatsetsV2.split(",");
	}

	/**
	 * @return the devmode
	 */
	public String getDevmode() {
		
		return devmode;
	}

	public String getWshotpath() {
		if(wshotpath==null || wshotpath.isEmpty()){
			return "/edu-sharing/services/";
		}
		return wshotpath;
	}

	public String getAlfrescocontext() {
		
		return alfrescocontext;
	}

	public String getRecommend_objects_query() {
		
		return recommend_objects_query;
	}

	public String getTrustedEmailAddress() {
		
		return trustedEmailAddress;
	}
	

	public String getAuthByAppUsernameProp() {
		return authByAppUsernameProp;
	}

	public boolean isAuthByAppSendMail() {
		return authByAppSendMail;
	}
	
	
	/**
	 * allow the mapping from the username send as an authByApp param to an userDirectory Attribute
	 * the mapped value will be taken as the repositoryusername
	 * 
	 * in Application File: auth_by_app_usernamemapping_dir_username
	 * 
	 * to use this both auth_by_app_usernamemapping_rep_username and auth_by_app_usernamemapping_dir_username must be defined
	 * and the userdataService property must be configured at CCAuthMethodTrustedRepository
	 * 
	 * @return the property which is used as a key to ask the userDirectory
	 */
	public String getAuthByAppUsernameMappingDirectoryUsername() {
		return authByAppUsernameMappingDirectoryUsername;
	}

	/**
	 * allow the mapping from the username send as an authByApp param to an userDirectory Attribute
	 * the mapped value will be taken as the repositoryusername
	 * 
	 * in Application File: auth_by_app_usernamemapping_rep_username
	 * 
	 * to use this both auth_by_app_usernamemapping_rep_username and auth_by_app_usernamemapping_dir_username must be defined
	 * and the userdataService property must be configured at CCAuthMethodTrustedRepository
	 * 
	 * @return the userdirectory property which value is used as respository username 
	 */
	public String getAuthByAppUsernameMappingRepositoryUsername() {
		return authByAppUsernameMappingRepositoryUsername;
	}

	public String getAllowedAuthenticationTypes() {
		
		return allowedAuthenticationTypes;
		
		
	}

	public String getCustomCss() {
		return customCss;
	}

	public String getHostAliases() {
		return hostAliases;
	}
	
	/**
	 * checks it hostName is configured host or host alias
	 * @param hostName
	 * @return
	 */
	public boolean isTrustedHost(String hostName){
		List<String> hostList = new ArrayList<String>();
		hostList.add(host);
		if(this.getDomain() != null && !this.getDomain().equals("")){
			hostList.add(this.getDomain());
		}
		
		if(this.getHostAliases() != null && !this.getHostAliases().trim().equals("")){
			String[] splitted = this.getHostAliases().split(",");
			// add all and trim to fix stuff like "ip1, ip2"
			hostList.addAll(Arrays.stream(splitted).map(String::trim).collect(Collectors.toList()));
		}

		return hostList.contains("*") || hostList.contains(hostName);
	}

	public String getProtocol() {
		return protocol;
	}

	/**
	 * or building client urls like preview url, if not set port value is returned
	 * @return
	 */
	public String getClientport() {
		String result = clientport;
		
		if(result != null && !result.trim().equals("")){
			return result;
		}else{
			result = getPort();
			if(result != null && !result.trim().equals("")){
				return result;
			}else{
				return "80";
			}
		}
	}
	
	/**
	 * or building client urls like preview url, if not set protocal value is returned
	 * @return
	 */
	public String getClientprotocol() {
		String result = clientprotocol;
		if(result != null && !result.trim().equals("")){
			return result;
		}else{
			result = getProtocol();
			if(result != null && !result.trim().equals("")){
				return result;
			}else{
				return "http";
			}
		}
		
	}

	public String getWebappname() {
		return webappname;
	}
	
	public int getTrackingBufferSize() {
		return trackingBufferSize;
	}
	
	public String getPublicKey() {
		return publicKey;
	}
	
	public String getPrivateKey() {
		return privateKey;
	}
	
	public long getMessageOffsetMs() {
		if(messageOffsetMs!=null && !messageOffsetMs.isEmpty())
			return new Long(messageOffsetMs);
		return DEFAULT_OFFSET_MS;
	}
	
	public long getMessageSendOffsetMs() {
		if(messageSendOffsetMs!=null && !messageSendOffsetMs.isEmpty())
			return new Long(messageSendOffsetMs);
		return DEFAULT_OFFSET_MS;
	}
	
	public String getLogo() {
		return logo;
	}
	
	public String getIcon() {
		return icon;
	}

	public String getApiKey() {
		return apiKey;
	}
	
	public String getValidatorRegexCMName() {
		return validatorRegexCMName;
	}

	public String getWebsitepreviewrenderservice() {
		return websitepreviewrenderservice;
	}	
	
	@Override
	public int compareTo(ApplicationInfo o) {
		return Integer.compare(getOrder(), o.getOrder());
	}

	/**
	 * returns true if the given app is describing a remote alfresco (edu-sharing) instance
	 */
	public boolean isRemoteAlfresco() {
		return REPOSITORY_TYPE_ALFRESCO.equals(getRepositoryType()) && TYPE_REPOSITORY.equals(getType());
	}

	public String getCookieAttributes() {
		return cookieAttributes;
	}

	public void setAuthByAppUserWhitelist(String authByAppUserWhitelist) {
		this.authByAppUserWhitelist = authByAppUserWhitelist;
	}

	public String getAuthByAppUserWhitelist() {
		return authByAppUserWhitelist;
	}

	public Map<CacheKey, Serializable> getCache() {
		return cache;
	}
	public enum CacheKey{
		RemoteAlfrescoVersion
	}
}
