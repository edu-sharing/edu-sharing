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
package org.edu_sharing.repository.server;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import javax.transaction.UserTransaction;

import com.google.common.base.CharMatcher;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.query.PagingRequest;
import org.alfresco.query.PagingResults;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.content.filestore.FileContentReader;
import org.alfresco.repo.management.subsystems.SwitchableApplicationContextFactory;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.search.impl.lucene.SolrJSONResultSet;
import org.alfresco.repo.search.impl.solr.ESSearchParameters;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.security.authority.AuthorityInfo;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.repo.thumbnail.ThumbnailDefinition;
import org.alfresco.repo.thumbnail.ThumbnailRegistry;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.repo.version.VersionModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchParameters.FieldFacet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.MutableAuthenticationService;
import org.alfresco.service.cmr.security.NoSuchPersonException;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.security.PersonService.PersonInfo;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.cmr.version.VersionType;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.ISO8601DateFormat;
import org.alfresco.util.ISO9075;
import org.alfresco.util.Pair;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.queryParser.QueryParser;
import org.edu_sharing.alfresco.HasPermissionsWork;
import org.edu_sharing.alfresco.fixes.VirtualEduGroupFolderTool;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.MetadataKey;
import org.edu_sharing.metadataset.v2.MetadataSetV2;
import org.edu_sharing.metadataset.v2.MetadataWidget;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.repository.client.exception.CCException;
import org.edu_sharing.repository.client.rpc.ACE;
import org.edu_sharing.repository.client.rpc.ACL;
import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.repository.client.rpc.Group;
import org.edu_sharing.repository.client.rpc.SearchResult;
import org.edu_sharing.repository.client.rpc.Share;
import org.edu_sharing.repository.client.rpc.User;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.MimeTypes;
import org.edu_sharing.repository.client.tools.UrlTool;
import org.edu_sharing.repository.client.tools.metadata.ValueTool;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.repository.server.authentication.ContextManagementFilter;
import org.edu_sharing.repository.server.tools.ActionObserver;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.AuthenticatorRemoteAppResult;
import org.edu_sharing.repository.server.tools.AuthenticatorRemoteRepository;
import org.edu_sharing.repository.server.tools.DateTool;
import org.edu_sharing.repository.server.tools.EduGroupTool;
import org.edu_sharing.repository.server.tools.I18nServer;
import org.edu_sharing.repository.server.tools.PropertiesHelper;
import org.edu_sharing.repository.server.tools.ServerConstants;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.repository.server.tools.VCardConverter;
import org.edu_sharing.repository.server.tools.cache.Cache;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.edu_sharing.repository.server.tools.forms.DuplicateFinder;
import org.edu_sharing.service.authentication.ScopeUserHomeServiceFactory;
import org.edu_sharing.alfresco.service.connector.ConnectorService;
import org.edu_sharing.service.license.LicenseService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.nodeservice.model.GetPreviewResult;
import org.edu_sharing.service.share.ShareService;
import org.edu_sharing.service.share.ShareServiceImpl;
import org.edu_sharing.service.util.AlfrescoDaoHelper;
import org.springframework.context.ApplicationContext;
import org.springframework.extensions.surf.util.I18NUtil;

import net.sf.acegisecurity.AuthenticationCredentialsNotFoundException;

public class MCAlfrescoAPIClient extends MCAlfrescoBaseClient {

	private static Logger logger = Logger.getLogger(MCAlfrescoAPIClient.class);

	private static ApplicationContext applicationContext = null;

	private ServiceRegistry serviceRegistry = null;

	private NodeService nodeService = null;

	private ContentService contentService = null;

	private AuthorityService authorityService = null;

	private SearchService searchService = null;

	private NamespaceService namespaceService = null;

	private PersonService personService;

	private DictionaryService dictionaryService;

	org.edu_sharing.alfresco.service.AuthorityService eduAuthorityService;

	org.edu_sharing.alfresco.service.OrganisationService eduOrganisationService;

	public final static StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");

	public final static StoreRef userStoreRef = new StoreRef("user", "alfrescoUserStore");

	public final static StoreRef versionStoreRef = new StoreRef("versionStore", "version2Store");

	public final static StoreRef archiveStoreRef = new StoreRef("archive","SpacesStore");

	public static String propertyfile = CCConstants.REPOSITORY_FILE_HOME;

	/**
	 * when it's true and getChildren or getChild is called the properties of
	 * the referenced object will be returned by asking the remote Repository.
	 * sometimes this behavior is not what we want for example getting the real
	 * RemoteObject Properties so you can use this prop. see getPropertiesBridge
	 */
	boolean resolveRemoteObjects = true;

	protected String repId = null;

	protected ApplicationInfo appInfo = null;

	Repository repositoryHelper = null;

	private static String alfrescoSearchSubsystem = null;

	public static final String SEARCH_SUBSYSTEM_LUCENE = "lucene";
	public static final String SEARCH_SUBSYSTEM_SOLR = "solr";

	/**
	 * this constructor can be used when the authentication at alfresco services
	 * was already done The AuthenticationInfo is taken from the current thread.
	 *
	 * Pay attention when using it. Cause of the thread pool in tomcat this can
	 * lead to the problem that the user becomes someone else when the thread
	 * was used by another user before and no new authentication with alfresco
	 * authenticationservice was processed
	 */
	public MCAlfrescoAPIClient() {
		this(null);
	}

	public MCAlfrescoAPIClient(HashMap<String, String> _authenticationInfo) {
		this(ApplicationInfoList.getHomeRepository().getAppId(), _authenticationInfo);
	}

	/**
	 * TODO: change static methods to object methods, use class attributes
	 * repositoryFile and authenticationInfo
	 *
	 * @param _repositoryFile
	 * @param _authenticationInfo
	 */
	public MCAlfrescoAPIClient(String _repositoryFile, HashMap<String, String> _authenticationInfo) {

		appInfo = ApplicationInfoList.getHomeRepository();
		repId = appInfo.getAppId();

		applicationContext = AlfAppContextGate.getApplicationContext();

		serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

		repositoryHelper = (Repository) applicationContext.getBean("repositoryHelper");

		nodeService = serviceRegistry.getNodeService();

		contentService = serviceRegistry.getContentService();

		authorityService = serviceRegistry.getAuthorityService();

		searchService = (SearchService)applicationContext.getBean("scopedSearchService");//serviceRegistry.getSearchService();

		namespaceService = serviceRegistry.getNamespaceService();

		personService = serviceRegistry.getPersonService();

		dictionaryService = serviceRegistry.getDictionaryService();

		eduAuthorityService = (org.edu_sharing.alfresco.service.AuthorityService)applicationContext.getBean("eduAuthorityService");

		eduOrganisationService = (org.edu_sharing.alfresco.service.OrganisationService)applicationContext.getBean("eduOrganisationService");

		try {
			logger.debug("currentAuthInfo from authservice:" + serviceRegistry.getAuthenticationService().getCurrentUserName() + " "
					+ serviceRegistry.getAuthenticationService().getCurrentTicket());
		} catch (net.sf.acegisecurity.AuthenticationCredentialsNotFoundException e) {
			//logger.error(e.getMessage());
		}

		if (_authenticationInfo == null) {
			try{
				HashMap<String, String> authInfo = new HashMap<String, String>();
				authInfo.put(CCConstants.AUTH_USERNAME, serviceRegistry.getAuthenticationService().getCurrentUserName());
				authInfo.put(CCConstants.AUTH_TICKET, serviceRegistry.getAuthenticationService().getCurrentTicket());
				authenticationInfo = authInfo;
				logger.debug("authinfo init parameter is null, using " + " " + authenticationInfo.get(CCConstants.AUTH_USERNAME) + " " + authenticationInfo.get(CCConstants.AUTH_TICKET));
			}catch(AuthenticationCredentialsNotFoundException e){
				// if session/user is not initalized, some methods may not work
				// but still, we can initialize ApiClient
				logger.warn("authinfo init parameter is null and no user session found");
			}

			/**
			 * do not call serviceRegistry.getAuthenticationService().validate
			 * here to allow runAs code using this class validate would
			 * overwrite the runAsUser
			 *
			 * this safe cause ContextmanagementFilter calls
			 * authservice.clearCurrentSecurityContext() after every request
			 *
			 * @TODO check if it is better to put run as user in authInfo than
			 *       the fully authenticated user
			 */

		} else {
			authenticationInfo = _authenticationInfo;
			logger.debug("authinfo is not null" + " " + authenticationInfo.get(CCConstants.AUTH_USERNAME) + " "
					+ authenticationInfo.get(CCConstants.AUTH_TICKET));
			serviceRegistry.getAuthenticationService().validate(authenticationInfo.get(CCConstants.AUTH_TICKET));
		}

		if (alfrescoSearchSubsystem == null) {
			// SwitchableApplicationContextFactoryapp
			SwitchableApplicationContextFactory sACF = (SwitchableApplicationContextFactory) applicationContext.getBean("Search");
			alfrescoSearchSubsystem = sACF.getCurrentSourceBeanName();
		}

	}

	public SearchResult searchSolr(String query, int startIdx, int nrOfresults, List<String> facettes, int facettesMinCount, int facettesLimit)
			throws Throwable {

		SearchResultNodeRef srnr = searchSolrNodeRef(query, startIdx, nrOfresults, facettes, facettesMinCount, facettesLimit);
		SearchResult result = new SearchResult();
		result.setCountedProps(srnr.getCountedProps());
		result.setNodeCount(srnr.getNodeCount());
		result.setStartIDX(startIdx);

		HashMap<String, HashMap<String, Object>> returnVal = new LinkedHashMap<String, HashMap<String, Object>>();
		List<org.edu_sharing.service.model.NodeRef> resultNodeRefs = srnr.getData();
		for (int i = 0; i < resultNodeRefs.size(); i++) {

			org.edu_sharing.service.model.NodeRef nodeRefEdu = resultNodeRefs.get(i);
			NodeRef actNode = new NodeRef(new StoreRef(nodeRefEdu.getStoreProtocol(),nodeRefEdu.getStoreId()), nodeRefEdu.getNodeId());

			HashMap<String, Object> properties = getProperties(actNode);
			returnVal.put(actNode.getId(), properties);
		}

		result.setData(returnVal);
		return result;
	}

	public SearchResultNodeRef searchSolrNodeRef(String query, int startIdx, int nrOfresults, List<String> facettes, int facettesMinCount, int facettesLimit)
			throws Throwable {

		SearchResultNodeRef searchResult = new SearchResultNodeRef();

		SearchParameters searchParameters = new SearchParameters();
		searchParameters.addStore(storeRef);

		searchParameters.setLanguage(SearchService.LANGUAGE_LUCENE);
		searchParameters.setQuery(query);

		searchParameters.setSkipCount(startIdx);
		searchParameters.setMaxItems(nrOfresults);

		if (facettes != null && facettes.size() > 0) {
			for (String facetteProp : facettes) {
				String fieldFacette = "@" + facetteProp;
				FieldFacet fieldFacet = new FieldFacet(fieldFacette);
				fieldFacet.setLimit(facettesLimit);
				//fieldFacet.setMinCount(facettesMinCount);

				System.out.println("MONCOUT reset to one");
				fieldFacet.setMinCount(1);
				searchParameters.addFieldFacet(fieldFacet);
			}
		}

		ResultSet resultSet = searchService.query(searchParameters);

		long nrFound = ((SolrJSONResultSet) resultSet).getNumberFound();

		searchResult.setNodeCount((int) nrFound);

		int startIDX = startIdx;

		if (nrFound <= startIDX) {
			startIDX = 0;
		}
		searchResult.setStartIDX(startIDX);

		// do the facette
		if (facettes != null && facettes.size() > 0) {
			Map<String, Map<String, Integer>> newCountPropsMap = new HashMap<String, Map<String, Integer>>();
			for (String facetteProp : facettes) {
				Map<String, Integer> resultPairs = newCountPropsMap.get(facetteProp);
				if (resultPairs == null) {
					resultPairs = new HashMap<String, Integer>();
				}
				String fieldFacette = "@" + facetteProp;

				List<Pair<String, Integer>> facettPairs = resultSet.getFieldFacet(fieldFacette);
				Integer subStringCount = null;

				// plain solr
				logger.info("found " + facettPairs.size() + " facette pairs for" + fieldFacette);
				for (Pair<String, Integer> pair : facettPairs) {

					// value contains language information i.e. {de}
					String first = new String(pair.getFirst().replaceAll("\\{[a-z]*\\}", "").getBytes(), "UTF-8");
					// logger.info("pair.getFirst():"+first+" pair.getSecond():"+pair.getSecond());
					// why ever: no values will be counted to so filter them

					/**
					 *solr4 problem: delivers facetes that have count 0 and should not occur in the searchresult
					 *
					 * http://stackoverflow.com/questions/10069868/getting-facet-count-0-in-solr
					 * --> pair.getSecond() > 0
					 */
					if (first != null && !first.trim().equals("") && pair.getSecond() > 0) {
						resultPairs.put(first, pair.getSecond());
					}
				}

				if (resultPairs.size() > 0)
					newCountPropsMap.put(facetteProp, resultPairs);
			}
			searchResult.setCountedProps(newCountPropsMap);

		}

		searchResult.setData(AlfrescoDaoHelper.unmarshall(resultSet.getNodeRefs(), this.repId));
		logger.info("returns");
		return searchResult;

	}

	public HashMap<String, HashMap<String, Object>> search(String luceneString, String type) throws Exception {

		String queryString = "TYPE:\"" + type + "\"";
		if (luceneString != null && !luceneString.trim().equals("")) {
			queryString = queryString + " AND " + luceneString;
		}

		HashMap<String, HashMap<String, Object>> result = new HashMap<String, HashMap<String, Object>>();
		ResultSet resultSet = searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, queryString);

		List<NodeRef> nodeRefs = resultSet.getNodeRefs();
		for (NodeRef nodeRef : nodeRefs) {
			HashMap<String, Object> props = getPropertiesSimple(nodeRef.getId());
			result.put(nodeRef.getId(), props);
		}
		return result;
	}


	public HashMap<String, HashMap<String, Object>> search(String luceneString) throws Throwable {
		return this.search(luceneString, storeRef.getProtocol(), storeRef.getIdentifier(),0, 10000).getData();
	}

	public SearchResult search(String luceneString, String storeProtocol, String storeName, int from, int maxResult) throws Throwable {

		StoreRef storeRef = new StoreRef(storeProtocol, storeName);
		HashMap<String, HashMap<String, Object>> result = new LinkedHashMap<String, HashMap<String, Object>>();


		SearchParameters searchParameters = new SearchParameters();
		searchParameters.addStore(storeRef);

		searchParameters.setLanguage(SearchService.LANGUAGE_LUCENE);

		searchParameters.setQuery(luceneString);

		searchParameters.setSkipCount(from);
		searchParameters.setMaxItems(maxResult);

		ResultSet resultSet = searchService.query(searchParameters);

		List<NodeRef> nodeRefs = resultSet.getNodeRefs();
		for (NodeRef nodeRef : nodeRefs) {
			HashMap<String, Object> props = getProperties(new NodeRef(storeRef,nodeRef.getId()));
			result.put(nodeRef.getId(), props);
		}

		SearchResult sr = new SearchResult();
		sr.setData(result);
		sr.setStartIDX(from);
		sr.setNodeCount(maxResult);
		sr.setNodeCount((int)resultSet.getNumberFound());

		return sr;
	}

	@Override
	public HashMap<String, HashMap<String, Object>> search(String luceneString, ContextSearchMode mode)
			throws Throwable {
		HashMap<String, HashMap<String, Object>> result = new HashMap<String, HashMap<String, Object>>();
		SearchParameters token=new SearchParameters();
		token.setQuery(luceneString);
		List<NodeRef> nodeRefs = searchNodeRefs(token,mode);
		for (NodeRef nodeRef : nodeRefs) {
			try{
				HashMap<String, Object> props = getProperties(nodeRef.getId());
				result.put(nodeRef.getId(), props);
			}catch(AccessDeniedException e){
				logger.error("found node but can not access node properties:" + nodeRef.getId());
			}
		}
		return result;
	}

	public List<NodeRef> searchNodeRefs(SearchParameters token, ContextSearchMode mode){
        Set<String> authorities=null;
        if(mode.equals(ContextSearchMode.UserAndGroups)) {
            authorities = new HashSet<>(authorityService.getAuthorities());
            authorities.remove(CCConstants.AUTHORITY_GROUP_EVERYONE);
            // remove the admin role, otherwise may results in inconsistent results
            authorities.remove(CCConstants.AUTHORITY_ROLE_ADMINISTRATOR);
            authorities.add(AuthenticationUtil.getFullyAuthenticatedUser());
        }
        else if(mode.equals(ContextSearchMode.Public)){
            authorities=new HashSet<>();
            authorities.add(CCConstants.AUTHORITY_GROUP_EVERYONE);
        }
        SearchParameters essp = new SearchParameters();

        if(authorities!=null){
            essp = new ESSearchParameters();
            ((ESSearchParameters)essp).setAuthorities(authorities.toArray(new String[authorities.size()]));
        }
        essp.setQuery(token.getQuery());
        for(SearchParameters.SortDefinition sort : token.getSortDefinitions()){
            essp.addSort(sort);
        }
        essp.setLanguage(SearchService.LANGUAGE_LUCENE);
        essp.addStore(storeRef);
        for(SearchParameters.SortDefinition def : token.getSortDefinitions()) {
            essp.addSort(def);
        }
        return searchService.query(essp).getNodeRefs();
	}


	public String[] searchNodeIds(String luceneString) {
		return searchNodeIds(luceneString, -1);
	}

	public String[] searchNodeIds(String luceneString, int limit) {
		SearchParameters searchParameters = new SearchParameters();
		searchParameters.addStore(storeRef);
		searchParameters.setLanguage(SearchService.LANGUAGE_LUCENE);
		searchParameters.setQuery(luceneString);
		if (limit > -1) {
			searchParameters.setLimit(limit);
		}
		ResultSet resultSet = searchService.query(searchParameters);

		ArrayList<String> result = new ArrayList<String>();
		for (NodeRef nodeRef : resultSet.getNodeRefs()) {
			result.add(nodeRef.getId());
		}
		return result.toArray(new String[result.size()]);
	}

	public String formatData(String type, String key, Object value, String metadataSetId) {
		String returnValue = null;
		if (key != null && value != null) {
			boolean processed=false;
			// value is date than put a String with a long value so that it can
			// be formated with userInfo later
			if(value instanceof List){
				List<Object> list = (List<Object>) value;
				if(list.size()>0){
					if(list.get(0) instanceof Date) {
						returnValue = ValueTool.toMultivalue(
								list.stream().
										map((date) -> new Long(((Date)date).getTime()).toString()).
										collect(Collectors.toList()).toArray(new String[0])
						);
						processed=true;
					}
				}
			}
			if (value instanceof Date) {

				Date date = (Date) value;
				returnValue = new Long(date.getTime()).toString();
				processed=true;
			}
			if(!processed){
				returnValue = getValue(type, key, value, metadataSetId);
			}
			// !(value instanceof MLText || value instanceof List): prevent sth.
			// like de_DE=null in gui
			if (returnValue == null && value != null && !(value instanceof MLText || value instanceof List)) {
				returnValue = value.toString();
			}
		}
		return returnValue;
	}

	protected String getValue(String type, String prop, Object _value, String metadataSetId) {

		//MetadataSetModelProperty mdsmProp = getMetadataSetModelProperty(metadataSetId, type, prop);

		if (_value instanceof List && ((List) _value).size() > 0) {
			String result = null;
			for (Object value : (List) _value) {
				if (result != null)
					result += CCConstants.MULTIVALUE_SEPARATOR;
				if (value != null) {
					if (value instanceof MLText) {
						String tmpStr = getMLTextString(value);
						if (result != null)
							result += tmpStr;
						else
							result = tmpStr;
					} else {
						if (result != null)
							result += value.toString();
						else
							result = value.toString();
					}
				}
			}

			return result;
		} else if (_value instanceof List && ((List) _value).size() == 0) {
			// cause empty list toString returns "[]"
			return "";
		} else if (_value instanceof String) {
			return (String) _value;
		} else if (_value instanceof Number) {
			return _value.toString();
		} else if (_value instanceof MLText) {
			return getMLTextString(_value);
		} else {
			return _value.toString();
		}

	}
	/*
	MetadataSetModelProperty getMetadataSetModelProperty(String metadataSetId, String type, String prop) {
		MetadataSetModelProperty mdsmProp = null;

		// test take the deafault metadataset when metadatasetId is null
		if (metadataSetId == null)
			metadataSetId = CCConstants.metadatasetdefault_id;

		if (metadataSetId != null) {
			
			if (metadataSetsForRep == null) {
				metadataSetsForRep = RepoFactory.getMetadataSetsForRepository(repId);
			}

			MetadataSet mds = metadataSetsForRep.getMetadataSetById(metadataSetId);
			if (mds != null) {
				MetadataSetModelType mdsmt = mds.getMetadataSetModelType(type);
				if (mdsmt != null) {
					mdsmProp = mdsmt.getMetadataSetModelProperty(prop);
				}
			}
		}
		return mdsmProp;
	}
	*/
	protected String getMLTextString(Object _mlText) {

		if (_mlText instanceof MLText) {

			MLText mlText = (MLText) _mlText;

			// when description does not exist then return default value
			// when description exists bit there is no multilang the return value 
			if (true /*mdsmp == null || (mdsmp != null && !mdsmp.getMultilang())*/) {
				return mlText.getDefaultValue();
			}

			String mlValueString = null;

			for (Locale locale : mlText.getLocales()) {
				String mlValue = mlText.getValue(locale);

				String localeStr = (locale.toString().equals(".default")) ? CCConstants.defaultLocale : locale.toString();

				if (mlValueString == null) {
					// for props that are declared multilang in alfresco model
					// but not in cc metadataset then props are saved as default.
					if (mlText.getLocales().size() == 1 && localeStr.equals(CCConstants.defaultLocale)) {
						mlValueString = mlValue;
					} else {
						mlValueString = localeStr + "=" + mlValue;
					}
				} else {
					mlValueString += "[,]" + localeStr + "=" + mlValue;
				}
			}
			if (mlValueString != null && !mlValueString.trim().equals("") && !mlValueString.contains(CCConstants.defaultLocale)) {
				mlValueString += "[,]default=" + mlText.getDefaultValue();
			}

			return mlValueString;
		} else {
			return _mlText.toString();
		}
	}

	public HashMap<String, HashMap<String, Object>> getChildren(String parentID) throws Throwable {
		return getChildren(parentID, (String) null);
	}

	public HashMap<String, HashMap<String, Object>> getChildren(String parentID, String[] permissionsOnChild) throws Throwable {

		HashMap<String, HashMap<String, Object>> result = getChildren(parentID);

		ArrayList<String> toRemove = new ArrayList<String>();
		if (result != null) {
			for (String nodeId : result.keySet()) {
				if (!hasPermissions(nodeId, permissionsOnChild)) {
					toRemove.add(nodeId);
				}
			}
			for (String nodeId : toRemove) {
				result.remove(nodeId);
			}
		}

		return result;
	}

	public HashMap<String, HashMap<String, Object>> getChildrenRunAs(final String parentID, String runAs) throws Throwable {

		final String repoAdmin = ApplicationInfoList.getHomeRepository().getUsername();

		AuthenticationUtil.RunAsWork<HashMap<String, HashMap<String, Object>>> getChildrenWorker = new AuthenticationUtil.RunAsWork<HashMap<String, HashMap<String, Object>>>() {

			@Override
			public HashMap<String, HashMap<String, Object>> doWork() throws Exception {

				try {
					return new MCAlfrescoAPIClient().getChildren(parentID);
				} catch (Throwable e) {
					logger.error(e.getMessage(), e);
					return null;
				}

			}
		};

		return AuthenticationUtil.runAs(getChildrenWorker, repoAdmin);
	}

	public List<ChildAssociationRef> getChildrenChildAssociationRef(String parentID){
		if (parentID == null) {

			String startParentId = getRootNodeId();
			if (startParentId == null || startParentId.trim().equals("")) {
				parentID = nodeService.getRootNode(storeRef).getId();
			} else {
				parentID = startParentId;
			}
		}
		NodeRef parentNodeRef = new NodeRef(MCAlfrescoAPIClient.storeRef, parentID);
		List<ChildAssociationRef> childAssocList = nodeService.getChildAssocs(parentNodeRef);
		return childAssocList;
	}

	public HashMap<String, HashMap<String, Object>> getChildren(String parentID, String type) throws Throwable {

		HashMap<String, HashMap<String, Object>> returnVal = new HashMap<String, HashMap<String, Object>>();

		if (parentID == null) {
			String startParentId = getRootNodeId();
			if (startParentId == null || startParentId.trim().equals("")) {
				parentID = nodeService.getRootNode(storeRef).getId();
			} else {
				parentID = startParentId;
			}
		}

		NodeRef parentNodeRef = new NodeRef(MCAlfrescoAPIClient.storeRef, parentID);
		List<ChildAssociationRef> childAssocList = nodeService.getChildAssocs(parentNodeRef);
		for (ChildAssociationRef child : childAssocList) {

			/**
			 * Alfresco 4.0.e archiving on: - check if it's not the archive
			 * store (when a object was deleted and it was linked somwhere the
			 * link still exist and points to archive store)
			 */
			if (!child.getChildRef().getStoreRef().equals(MCAlfrescoAPIClient.storeRef))
				continue;

			if (type == null || type.equals(nodeService.getType(child.getChildRef()).toString())) {
				HashMap<String, Object> properties = getProperties(child.getChildRef());
				if (properties == null)
					continue;

				// to prevent performace issues in search we only put the
				// publish right here, it's only needed in workspace list
				String nodeId = properties.containsKey(CCConstants.VIRT_PROP_REMOTE_OBJECT_NODEID) ? (String) properties
						.get(CCConstants.VIRT_PROP_REMOTE_OBJECT_NODEID) : (String) properties.get(CCConstants.SYS_PROP_NODE_UID);

				boolean hasPublishPermission = this.hasPermissions(nodeId, new String[] { CCConstants.PERMISSION_CC_PUBLISH });
				properties.put(CCConstants.PERMISSION_CC_PUBLISH, new Boolean(hasPublishPermission).toString());

				// PrimaryParent?
				ChildAssociationRef childAssocRef = nodeService.getPrimaryParent(child.getChildRef());
				NodeRef tmpParentRef = childAssocRef.getParentRef();
				if (tmpParentRef.equals(parentNodeRef)) {
					properties.put(CCConstants.CCM_PROP_PRIMARY_PARENT, "true");
				} else {
					properties.put(CCConstants.CCM_PROP_PRIMARY_PARENT, "false");
				}

				// put ChildassociationName
				properties.put(CCConstants.CHILD_ASSOCIATION_NAME, child.getQName().getLocalName());

				// filter stupid mac files
				String name = (String) properties.get(CCConstants.CM_NAME);

				if (name != null && (name.startsWith("._") || name.startsWith(".DS_Store"))) {
					logger.debug("will not show system file " + name + " in webgui");
				} else {
					returnVal.put(child.getChildRef().getId(), properties);
				}
			}
		}

		return returnVal;
	}

	public HashMap<String, Object> getProperties(String nodeId) throws Throwable {
		return this.getProperties(new NodeRef(storeRef, nodeId));
	}


	public HashMap<String, Object> getProperties(String storeProtocol, String storeId, String nodeId) throws Throwable {
		return getProperties(new NodeRef(new StoreRef(storeProtocol,storeId),nodeId));
	}
    public String getDownloadUrl(String nodeId) throws Throwable {
        HashMap<String, Object> props = getProperties(nodeId);
        boolean downloadAllowed = downloadAllowed(nodeId);
        String redirectServletLink = this.getRedirectServletLink(repId, nodeId);
        if (props.get(CCConstants.ALFRESCO_MIMETYPE) != null && redirectServletLink != null && downloadAllowed) {
            String params = URLEncoder.encode("display=download");
            String downloadUrl = UrlTool.setParam(redirectServletLink, "params", params);
            return downloadUrl;
        }
        return null;
    }
	public boolean downloadAllowed(String nodeId){
		NodeRef ref=new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId);
		return downloadAllowed(nodeId,
				nodeService.getProperty(ref,QName.createQName(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY)),
				(String)nodeService.getProperty(ref,QName.createQName(CCConstants.CCM_PROP_EDITOR_TYPE))
				);
	}
	public boolean downloadAllowed(String nodeId,Serializable commonLicenseKey,String editorType){
		// when there is a signed request from the connector, the download (binary content delivery) is allowed
		if(ApplicationInfo.TYPE_CONNECTOR.equals(ContextManagementFilter.accessToolType.get())) {
			return true;
		}
		boolean downloadAllowed;
        // Array value
	    if(commonLicenseKey instanceof ArrayList)
		    downloadAllowed = (CCConstants.COMMON_LICENSE_EDU_P_NR_ND.equals(((ArrayList)commonLicenseKey).get(0))) ? false : true;
	    else
	        // string value
            downloadAllowed = (CCConstants.COMMON_LICENSE_EDU_P_NR_ND.equals(commonLicenseKey)) ? false : true;

        //allow download for owner, performance only check owner if download not allowed
		if(!downloadAllowed && isOwner(nodeId, authenticationInfo.get(CCConstants.AUTH_USERNAME))){
			downloadAllowed = true;
		}

		if(editorType != null && editorType.toLowerCase().equals(ConnectorService.ID_TINYMCE.toLowerCase())){
			downloadAllowed = false;
		}
		return downloadAllowed;
	}
	/**
	 * this method calls getPropertiesCached and makes a copy from the returned
	 * hashmap this hashmap will be modiefied with the data of the current
	 * user (i.e. ticket in contenturl, preview url and so on)
	 *
	 * @param nodeRef
	 * @return
	 * @throws Exception
	 */
	public HashMap<String, Object> getProperties(NodeRef nodeRef) throws Throwable {
		logger.debug("starting");

		// making a copy so that the cached map will not be influenced
		HashMap<String, Object> propsCopy = new HashMap<String, Object>(getPropertiesCached(nodeRef, true, true, false));

		logger.debug("starting extend several props with authentication and permission data");

		NodeServiceInterceptor.throwIfWrongScope(nodeService, nodeRef);

		String nodeType = (String) propsCopy.get(CCConstants.NODETYPE);

		// checking if it is form type content
		boolean isSubOfContent = serviceRegistry.getDictionaryService().isSubClass(QName.createQName(nodeType), QName.createQName(CCConstants.CM_TYPE_CONTENT));

		logger.debug("setting external URL");
		String contentUrl = URLTool.getNgRenderNodeUrl(nodeRef.getId(),null);

		contentUrl = URLTool.addOAuthAccessToken(contentUrl);
		propsCopy.put(CCConstants.CONTENTURL, contentUrl);

		// external URL
		if (isSubOfContent) {

			Serializable commonLicenseKey = (String)propsCopy.get(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY);
			boolean downloadAllowed = downloadAllowed(nodeRef.getId(),commonLicenseKey,(String)propsCopy.get(CCConstants.CCM_PROP_EDITOR_TYPE));

			if ((propsCopy.get(CCConstants.ALFRESCO_MIMETYPE) != null || propsCopy.get(CCConstants.LOM_PROP_TECHNICAL_LOCATION)!=null) && downloadAllowed) {
				propsCopy.put(CCConstants.DOWNLOADURL,URLTool.getDownloadServletUrl(nodeRef.getId(),null, true));
			}

			String commonLicensekey = (String)propsCopy.get(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY);
			if(commonLicensekey != null){
				if(Context.getCurrentInstance() != null){
					String ccversion = (String)propsCopy.get(CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_VERSION);
					String licenseUrl =  new LicenseService().getLicenseUrl(commonLicensekey, Context.getCurrentInstance().getLocale(),ccversion);
					if(licenseUrl != null){
						propsCopy.put(CCConstants.VIRT_PROP_LICENSE_URL, licenseUrl);
					}
				}
				String licenseIcon = new LicenseService().getIconUrl(commonLicensekey);
				if(licenseIcon != null) propsCopy.put(CCConstants.VIRT_PROP_LICENSE_ICON, licenseIcon);

			}
		}

		/** Add the image dimensions to the common CCM fields */
		if (nodeType.equals(CCConstants.CCM_TYPE_IO)){
			if(propsCopy.containsKey(CCConstants.EXIF_PROP_PIXELXDIMENSION)){
				propsCopy.put(CCConstants.CCM_PROP_IO_WIDTH,propsCopy.get(CCConstants.EXIF_PROP_PIXELXDIMENSION));
			}
			if(propsCopy.containsKey(CCConstants.EXIF_PROP_PIXELYDIMENSION)){
				propsCopy.put(CCConstants.CCM_PROP_IO_HEIGHT,propsCopy.get(CCConstants.EXIF_PROP_PIXELYDIMENSION));
			}

			//Preview Url not longer in cache
			String renderServiceUrlPreview = URLTool.getRenderServiceURL(nodeRef.getId(), true);
			if (renderServiceUrlPreview != null) {
				propsCopy.put(CCConstants.CM_ASSOC_THUMBNAILS, renderServiceUrlPreview);
			} else {
				propsCopy.put(CCConstants.CM_ASSOC_THUMBNAILS, NodeServiceHelper.getPreview(nodeRef, propsCopy).getUrl());
			}
		}

		/**
		 * run over all properties and format the date props with with current
		 * user locale
		 */
		if (nodeType.equals(CCConstants.CCM_TYPE_IO) || nodeType.equals(CCConstants.CCM_TYPE_COMMENT) || nodeType.equals(CCConstants.CCM_TYPE_COLLECTION_FEEDBACK) || nodeType.equals(CCConstants.CCM_TYPE_MAP) || nodeType.equals(CCConstants.CM_TYPE_FOLDER)) {
			String mdsId=CCConstants.metadatasetdefault_id;
			if(propsCopy.containsKey(CCConstants.CM_PROP_METADATASET_EDU_METADATASET)){
				mdsId=(String)propsCopy.get(CCConstants.CM_PROP_METADATASET_EDU_METADATASET);
			}
			MetadataSetV2 mds = MetadataHelper.getMetadataset(ApplicationInfoList.getHomeRepository(),mdsId);
			HashMap<String, Object> addAndOverwriteDateMap = new HashMap<String, Object>();
			for (Map.Entry<String, Object> entry : propsCopy.entrySet()) {

				PropertyDefinition propDef = dictionaryService.getProperty(QName.createQName(entry.getKey()));

				DataTypeDefinition dtd = null;
				if (propDef != null)
					dtd = propDef.getDataType();
				if (Context.getCurrentInstance() != null && dtd != null
						&& (dtd.getName().equals(DataTypeDefinition.DATE) || dtd.getName().equals(DataTypeDefinition.DATETIME))) {
					String[] values = ValueTool.getMultivalue((String) entry.getValue());
					String[] formattedValues=new String[values.length];
					int i=0;
					for(String value : values){
						formattedValues[i++]=new DateTool().formatDate(new Long(value));
					}
					// put time as long i.e. for sorting or formating in gui
					// this is basically just a copy of the real value for backward compatibility
					addAndOverwriteDateMap.put(entry.getKey() + CCConstants.LONG_DATE_SUFFIX, entry.getValue());
					// put formated
					addAndOverwriteDateMap.put(entry.getKey(), ValueTool.toMultivalue(formattedValues));
				}
				try{
					MetadataWidget widget = mds.findWidget(CCConstants.getValidLocalName(entry.getKey()));
					Map<String, MetadataKey> map = widget.getValuesAsMap();
					if(!map.isEmpty()){
						String[] keys=ValueTool.getMultivalue((String) entry.getValue());
						String[] values=new String[keys.length];
						for(int i=0;i<keys.length;i++)
							values[i]=map.containsKey(keys[i]) ? map.get(keys[i]).getCaption() : keys[i];
						addAndOverwriteDateMap.put(entry.getKey() + CCConstants.DISPLAYNAME_SUFFIX, StringUtils.join(values,CCConstants.MULTIVALUE_SEPARATOR));
					}

				}catch(Throwable t){

				}
			}

			for (Map.Entry<String, Object> entry : addAndOverwriteDateMap.entrySet()) {
				propsCopy.put(entry.getKey(), entry.getValue());
			}
		}
		// Preview this was done already in getPropertiesCached (the heavy
		// performance must be done in getPropertiesCached)
		// but we need to set the ticket when it's an alfresco generated preview
		// logger.info("setting Preview");
		if (nodeType.equals(CCConstants.CCM_TYPE_IO)) {
			String renderServiceUrlPreview = URLTool.getRenderServiceURL(nodeRef.getId(), true);
			if (renderServiceUrlPreview == null) {
				// prefer alfresco thumbnail
				String thumbnailUrl = (String) propsCopy.get(CCConstants.CM_ASSOC_THUMBNAILS);
				if (thumbnailUrl != null && !thumbnailUrl.trim().equals("")) {

					// prevent Browser Caching:
					thumbnailUrl = UrlTool.setParam(thumbnailUrl, "dontcache", new Long(System.currentTimeMillis()).toString());
					propsCopy.put(CCConstants.CM_ASSOC_THUMBNAILS, thumbnailUrl);
				}

			}

			/**
			 * for Collections Ref Objects return original nodeid
			 * @TODO its a association so it could be multivalue
			 */
			if(Arrays.asList(getAspects(nodeRef)).contains(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE)){
				AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {

					@Override
					public Void doWork() throws Exception{
						try {
							List<NodeRef> assocNode = getAssociationNodeIds(nodeRef, CCConstants.CM_ASSOC_ORIGINAL);
							if(assocNode.size() > 0){
								String originalNodeId = assocNode.get(0).getId();
								propsCopy.put(CCConstants.CM_ASSOC_ORIGINAL, originalNodeId);
							}
						} catch (Throwable t) {
							throw new Exception(t);
						}
						return null;
					}
				});
			}

		}

		// setting ticket for map icon url
		if (nodeType.equals(CCConstants.CCM_TYPE_MAP)) {
			String iconUrl = (String) propsCopy.get(CCConstants.CCM_PROP_MAP_ICON);
			if (iconUrl != null) {
				String paramToken = (iconUrl.contains("?")) ? "&" : "?";
				iconUrl = iconUrl + paramToken + "ticket=" + authenticationInfo.get(CCConstants.AUTH_TICKET);
				// prevent Browser Caching:
				iconUrl = UrlTool.setParam(iconUrl, "dontcache", new Long(System.currentTimeMillis()).toString());

				propsCopy.put(CCConstants.CCM_PROP_MAP_ICON, iconUrl);
			}
		}

		if (nodeType.equals(CCConstants.CCM_TYPE_MAP) || nodeType.equals(CCConstants.CM_TYPE_FOLDER)) {

			// Information if write is allowed (important for DragDropComponent)
			// and drawRelations
			HashMap<String, Boolean> permissions = hasAllPermissions(nodeRef.getId(), new String[] { PermissionService.WRITE, PermissionService.ADD_CHILDREN });
			for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
				propsCopy.put(entry.getKey(), entry.getValue().toString());
			}

			// for the system folder: these are created in german and english
			// language.
			// we can not cache it, cause cache mechanism is not able to handle
			// multiple lang props
			Object mlfolderTitleObject = nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CM_PROP_C_TITLE));
			String mlFolderTitle = formatData(nodeType, CCConstants.CM_PROP_C_TITLE, mlfolderTitleObject,
					(String) propsCopy.get(CCConstants.CM_PROP_METADATASET_EDU_METADATASET));
			propsCopy.put(CCConstants.CM_PROP_C_TITLE, mlFolderTitle);
		}

		// remote object
		if (nodeType.equals(CCConstants.CCM_TYPE_REMOTEOBJECT) && isResolveRemoteObjects()) {
			logger.info("BEGIN TYPE is REMOTEOBJECT");

			String remoteNodeId = (String) propsCopy.get(CCConstants.CCM_PROP_REMOTEOBJECT_NODEID);
			String remoteRepository = (String) propsCopy.get(CCConstants.CCM_PROP_REMOTEOBJECT_REPOSITORYID);
			// logger.info("THE MAGIC KEY"+CCConstants.CCM_PROP_REMOTEOBJECT_NODEID);
			logger.info("remoteRepository: " + remoteRepository + "  remoteNodeId:" + remoteNodeId);
			ApplicationInfo remoteRepInfo = ApplicationInfoList.getRepositoryInfoById(remoteRepository);
			if (remoteRepInfo == null) {
				logger.error("No ApplicationInfo found for Repository:" + remoteRepository + " and remoteNodeId:" + remoteNodeId);
				return null;
			}
			else if(remoteRepInfo.isRemoteAlfresco()){
				AuthenticatorRemoteRepository arr = new AuthenticatorRemoteRepository();
				// when repository got no Authentication
				HashMap<String, String> remoteAuthInfo = null;
				if (remoteRepInfo.getAuthenticationwebservice() != null && !remoteRepInfo.getAuthenticationwebservice().equals("")) {
					try {
                        AuthenticatorRemoteAppResult arar = arr.getAuthInfoForApp(authenticationInfo.get(CCConstants.AUTH_USERNAME), remoteRepInfo);
                        remoteAuthInfo = arar.getAuthenticationInfo();
					} catch (Throwable e) {
						logger.error("It seems that repository id:" + remoteRepInfo.getAppId() + " is not reachable:" + e.getMessage()+". Check the configured value of "+ApplicationInfo.KEY_AUTHENTICATIONWEBSERVICE);
						return null;
					}
				} else {
					// TODO check if that is right
					remoteAuthInfo = authenticationInfo;
				}

				HashMap<String, Object> result = null;
				try {
					MCBaseClient mcBaseClient = RepoFactory.getInstance(remoteRepInfo.getAppId(), authenticationInfo);

					// only when the user got remote permissions
					if (mcBaseClient instanceof MCAlfrescoBaseClient) {
						if (((MCAlfrescoBaseClient) mcBaseClient).hasPermissions(remoteNodeId, authenticationInfo.get(CCConstants.AUTH_USERNAME),
								new String[] { "Read" })) {

							result = mcBaseClient.getProperties(remoteNodeId);
						}
					} else {
						result = mcBaseClient.getProperties(remoteNodeId);
					}

				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}

				if (result != null) {
					result.put(CCConstants.VIRT_PROP_ISREMOTE_OBJECT, "true");
					result.put(CCConstants.VIRT_PROP_REMOTE_OBJECT_NODEID, nodeRef.getId());
				}

				// set AuthentificationInfo back to init values
				serviceRegistry.getAuthenticationService().validate(authenticationInfo.get(CCConstants.AUTH_TICKET));
				logger.debug("returning remote object");
				return result;
			}
		}

		logger.debug("returning");
		return propsCopy;
	}

	/**
	 * no user depended information like username and ticket will be set cause
	 * it will be cached so no (content url, icon url) will be set
	 * here (cause it contains the ticket info) also no user depended permission
	 * checks will be done
	 *
	 * @param nodeRef
	 * @return
	 * @throws Exception
	 */
	public HashMap<String, Object> getPropertiesCached(NodeRef nodeRef, boolean getFromCache, boolean checkModified, boolean ifNotInCacheReturnNull) throws Exception {
		return getPropertiesCached(nodeRef, getFromCache, checkModified, ifNotInCacheReturnNull, nodeService);
	}
	public HashMap<String, Object> getPropertiesCached(NodeRef nodeRef, boolean getFromCache, boolean checkModified, boolean ifNotInCacheReturnNull, NodeService service)
			throws Exception {

		Map<QName, Serializable> propMap = null;
		Cache repCache = new RepositoryCache();
		// only get object by cache for one storeRef cause we take only the
		// nodeId as key
		if (getFromCache && nodeRef.getStoreRef().equals(storeRef)) {

			HashMap<String, Object> propsFromCache = (HashMap<String, Object>) repCache.get(nodeRef.getId());

			if (propsFromCache != null) {

				// check if thumbnail generation was processing if true
				// occurs when i.e. when copying a node
				boolean refreshThumbnail = new Boolean((String) propsFromCache.get(CCConstants.KEY_PREVIEW_GENERATION_RUNS));

				if (checkModified) {
					
					Date mdate = (Date) nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CM_PROP_C_MODIFIED));

					Long orginalModTime = null;
					if (mdate != null) {
						orginalModTime = mdate.getTime();
					}

					String cacheModified = (String) propsFromCache.get(CCConstants.CC_CACHE_MILLISECONDS_KEY);
					Long cachedModTime = null;
					try {
						cachedModTime = new Long(cacheModified);
					} catch (Exception e) {

					}

					if (cachedModTime != null && orginalModTime != null && cachedModTime.longValue() == orginalModTime.longValue() && !refreshThumbnail) {
						return propsFromCache;
					} else {
						logger.debug("CACHE modified Date changed! refreshing:" + nodeRef.getId() + " cachedModTime:" + cachedModTime + " orginalModTime:"
								+ orginalModTime + " refreshThumbnail:" + refreshThumbnail);
					}
					
				} else {
					return propsFromCache;
				}
				
			} else if (ifNotInCacheReturnNull) {
				return null;
			}
			
		}

		if (propMap == null){
			propMap = service.getProperties(nodeRef);
		}
			
		HashMap<String, Object> properties = new HashMap<String, Object>();
		String nodeType = service.getType(nodeRef).toString();

		// Properties:
		for (QName qname : propMap.keySet()) {

			String propName = qname.toString();
			
			// Properties
			Serializable object = propMap.get(qname);

			// allow only Number and String Types other Types will be handeled
			// in the following code
			if (object instanceof String || object instanceof Date || object instanceof Number || object instanceof List || object instanceof Boolean
					|| object instanceof MLText || object instanceof NodeRef) {

				String metadataSetId = (String) propMap.get(QName.createQName(CCConstants.CM_PROP_METADATASET_EDU_METADATASET));
				String value = formatData(nodeType, propName, object, metadataSetId);

				// add formated replicationsourcetimestamp
				if (propName.equals(CCConstants.CCM_PROP_IO_REPLICATIONSOURCETIMESTAMP) && !value.equals("") && !value.trim().equals("0000-00-00T00:00:00Z")) {
					
					try {
						
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:sss");
						Date date = sdf.parse((String) value);
						DateFormat df = ServerConstants.DATEFORMAT_WITHOUT_TIME;
						String formatedDate = df.format(date);
						properties.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCETIMESTAMPFORMATED, formatedDate);

					} catch (ParseException e) {
						logger.error(value + " was no valid date of format " + "yyyy-MM-dd'T'HH:mm:sss");
					}

				}

				properties.put(propName, value);

				// put a ISO String when its a date value
				if (object instanceof Date) {
					properties.put(propName + CCConstants.ISODATE_SUFFIX, ISO8601DateFormat.format((Date) object));
				}

				// VCard
				HashMap<String, Object> vcard = VCardConverter.getVCardHashMap(nodeType, propName, value);
				if (vcard != null && vcard.size() > 0) properties.putAll(vcard);
				
			}

		}

		if (properties.containsKey(CCConstants.CM_PROP_VERSIONABLELABEL)) {
			properties.put(CCConstants.CM_ASPECT_VERSIONABLE, "true");
		}

		// MimeType
		// we run as system because the current user may not has enough permissions to access content
		properties.put(CCConstants.ALFRESCO_MIMETYPE, getAlfrescoMimetype(nodeRef));


		// MapRelations
		if (nodeType.equals(CCConstants.CCM_TYPE_MAPRELATION)) {
			List<AssociationRef> relSrcList = nodeService.getTargetAssocs(nodeRef, QName.createQName(CCConstants.CCM_ASSOC_RELSOURCE));
			List<AssociationRef> relTargetList = nodeService.getTargetAssocs(nodeRef, QName.createQName(CCConstants.CCM_ASSOC_RELTARGET));
			if ((relSrcList != null && relSrcList.size() > 0) && relTargetList != null && relTargetList.size() > 0) {
				logger.debug("relSrcList.get(0).getTargetRef().getId():" + relSrcList.get(0).getTargetRef().getId() + "  "
						+ nodeService.getType(relSrcList.get(0).getTargetRef()));
				logger.debug("relTargetList.get(0).getTargetRef().getId():" + relTargetList.get(0).getTargetRef().getId() + "  "
						+ nodeService.getType(relTargetList.get(0).getTargetRef()));
				properties.put(CCConstants.CCM_ASSOC_RELSOURCE, relSrcList.get(0).getTargetRef().getId());
				properties.put(CCConstants.CCM_ASSOC_RELTARGET, relTargetList.get(0).getTargetRef().getId());
			}
		}

		// Preview
		if (nodeType.equals(CCConstants.CCM_TYPE_IO)) {
			//@todo 5.1: check if this is needed since it only is used in the PreviewServlet
			/*
			GetPreviewResult prevResult = getPreviewUrl(nodeRef.getStoreRef(), nodeRef.getId());

			if (prevResult.getType().equals(GetPreviewResult.TYPE_USERDEFINED)) {
				properties.put(CCConstants.KEY_PREVIEWTYPE, GetPreviewResult.TYPE_USERDEFINED);
			}

			if (prevResult.getType().equals(GetPreviewResult.TYPE_GENERATED)) {
				properties.put(CCConstants.KEY_PREVIEWTYPE, GetPreviewResult.TYPE_GENERATED);
			}

			if (prevResult.isCreateActionRunning()) {
				properties.put(CCConstants.KEY_PREVIEW_GENERATION_RUNS, "true");
			} else {
				properties.remove(CCConstants.KEY_PREVIEW_GENERATION_RUNS);
			}
			*/

			List<NodeRef> usages = this.getChildrenByAssociationNodeIds(nodeRef.getStoreRef(),nodeRef.getId(), CCConstants.CCM_ASSOC_USAGEASPECT_USAGES);
			if (usages != null) {
				properties.put(CCConstants.VIRT_PROP_USAGECOUNT, "" + usages.size());
			}
			List<NodeRef> childs = this.getChildrenByAssociationNodeIds(nodeRef.getStoreRef(),nodeRef.getId(), CCConstants.CCM_ASSOC_CHILDIO);
			if (childs != null) {
				properties.put(CCConstants.VIRT_PROP_CHILDOBJECTCOUNT, "" + childs.size());
			}
			List<NodeRef> comments = this.getChildrenByAssociationNodeIds(nodeRef.getStoreRef(),nodeRef.getId(), CCConstants.CCM_ASSOC_COMMENT);
			if (comments != null) {
				properties.put(CCConstants.VIRT_PROP_COMMENTCOUNT,comments.size());
			}

			// add permalink
			String version = (String) properties.get(CCConstants.LOM_PROP_LIFECYCLE_VERSION);
			if (version == null)
				version = (String) properties.get(CCConstants.CM_PROP_VERSIONABLELABEL);

			//String permaLink = URLTool.getBaseUrl() + "/node/" + nodeRef.getId();
			String permaLink = URLTool.getNgComponentsUrl()+"render/" + nodeRef.getId();
			permaLink = (version != null) ? permaLink + "/" + version : permaLink;
			properties.put(CCConstants.VIRT_PROP_PERMALINK, permaLink);
		}

		if (nodeType.equals(CCConstants.CCM_TYPE_MAP)) {
			String iconUrl = URLTool.getBrowserURL(nodeRef, CCConstants.CCM_PROP_MAP_ICON);
			if (iconUrl != null) {
				properties.put(CCConstants.CCM_PROP_MAP_ICON, iconUrl);
			}
		}

		// we can cache primary parent here, instead of parentid which differs
		// from the content
		if (nodeType.equals(CCConstants.CCM_TYPE_IO) || nodeType.equals(CCConstants.CCM_TYPE_MAP) || nodeType.equals(CCConstants.CM_TYPE_FOLDER) || nodeType.equals(CCConstants.CCM_TYPE_TOOL_INSTANCE)) {
			ChildAssociationRef parentNodeRef = nodeService.getPrimaryParent(nodeRef);
			properties.put(CCConstants.VIRT_PROP_PRIMARYPARENT_NODEID, parentNodeRef.getParentRef().getId());
		}

		// NodeType
		properties.put(CCConstants.NODETYPE, nodeService.getType(nodeRef).toString());
		properties.put(CCConstants.NODEID, nodeRef.getId());

		// Repository Id is in API Mode always the home repository
		properties.put(CCConstants.REPOSITORY_ID, repId);
		properties.put(CCConstants.REPOSITORY_CAPTION, appInfo.getAppCaption());

		buildUpProperties(properties);

		// cache
		if (nodeRef.getStoreRef().equals(storeRef)) {
			Date mdate = (Date) propMap.get(QName.createQName(CCConstants.CM_PROP_C_MODIFIED));
			if (mdate != null) {
				properties.put(CCConstants.CC_CACHE_MILLISECONDS_KEY, new Long(mdate.getTime()).toString());
				repCache.put(nodeRef.getId(), properties);
			}
		}

		return properties;
	}

	public String getAlfrescoMimetype(NodeRef nodeRef) {
		return AuthenticationUtil.runAsSystem(() -> {
			ContentReader contentReader = contentService.getReader(nodeRef, QName.createQName(CCConstants.CM_PROP_CONTENT));
			if (contentReader != null) {
				return contentReader.getMimetype();
			}
			return null;
		});
	}

	public String getProperty(StoreRef store, String nodeId, String property) {
		return this.getProperty(store.getProtocol(), store.getIdentifier(), nodeId, property);
	}

	/**
	 * @TODO Same handling with List and MLText properties like in getProperties
	 *       ->getValue
	 */
	public String getProperty(String storeProtocol, String storeIdentifier, String nodeId, String property) {
		Serializable val = nodeService.getProperty(new NodeRef(new StoreRef(storeProtocol, storeIdentifier), nodeId), QName.createQName(property));
		if (val != null) {

			String result = null;
			if (val instanceof List && ((List) val).size() > 0) {

				for (Object value : (List) val) {
					if (result != null)
						result += CCConstants.MULTIVALUE_SEPARATOR;
					if (value != null) {
						if (result != null)
							result += value.toString(); //getMultiLangCleaned(value.toString());
						else
							result = value.toString(); //getMultiLangCleaned(value.toString());
					}
				}

			}else if(val instanceof NodeRef){
				result = ((NodeRef)val).toString();
			}else {
				result = val.toString(); //getMultiLangCleaned(val.toString());
			}

			return result;

		} else {
			return null;
		}
	}

	private String getMultiLangCleaned(String value) {

		String result = new String(value);

		// edu-sharing properties multilang = true {de_DE=Realschule}
		if (result != null && result.matches("\\{[a-z][a-z]_[A-Z][A-Z]=.*\\}")) {
			String[] splitted = result.split("=");
			result = splitted[1].replace("}", "");
		}

		if (result != null && result.matches("\\{default=.*\\}")) {
			String[] splitted = result.split("=");
			result = splitted[1].replace("}", "");
		}

		return result;
	}

	/**
	 * returns the simple alfresco properties without special handling
	 * 
	 * @param nodeId
	 * @return
	 */
	public HashMap<String, Object> getPropertiesSimple(StoreRef givenStoreRef, String nodeId) {

		NodeRef nodeRef = new NodeRef(givenStoreRef, nodeId);
		HashMap<String, Object> properties = new HashMap<String, Object>();
		Map<QName, Serializable> propMap = nodeService.getProperties(nodeRef);

		String nodeType = nodeService.getType(nodeRef).toString();
		
		// Properties
		for (QName qname : propMap.keySet()) {

			String propName = qname.toString();
			
			// Properties
			Serializable object = propMap.get(qname);

			if (object instanceof String || object instanceof Date || object instanceof Number || object instanceof List || object instanceof MLText) {

				String metadataSetId = (String) propMap.get(QName.createQName(CCConstants.CM_PROP_METADATASET_EDU_METADATASET));

				String value = formatData(nodeType, propName, object, metadataSetId);
				properties.put(propName, value);
			}
		}

		properties.put(CCConstants.NODETYPE, nodeType);
		properties.put(CCConstants.NODEID, nodeRef.getId());

		buildUpProperties(properties);

		return properties;
	}

	/**
	 * returns the simple alfresco Properties without special handling
	 * 
	 * @param nodeId
	 * @return
	 */
	public HashMap<String, Object> getPropertiesSimple(String nodeId) {
		return getPropertiesSimple(storeRef, nodeId);
	}

	public String getRootNode(StoreRef store) {
		return nodeService.getRootNode(store).getId();
	}
	
	public void removeNode(String storeProtocol, String storeId, String nodeId) {
		this.removeNode(new StoreRef(storeProtocol, storeId),nodeId);
	}
	
	public void removeNode(StoreRef store, String nodeId) {
		nodeService.deleteNode(new NodeRef(store, nodeId));
	}
	
	public String getRootNodeId() {

		String result = null;
		try {
			result = PropertiesHelper.getProperty("explorer-start-nodeid", MCAlfrescoAPIClient.propertyfile, PropertiesHelper.XML);

			if (result == null || result.trim().equals("")) {
				result = null;

				// access from API Client always is the HomeRepository
				ApplicationInfo appInfo = ApplicationInfoList.getHomeRepository();

				String adminUser = appInfo.getUsername();
				String tmpUser = authenticationInfo.get(CCConstants.AUTH_USERNAME);
				if (!adminUser.equals(tmpUser)) {
					result = getHomeFolderID(tmpUser);
				}else if ("admin".equals(tmpUser)) {
					result = getCompanyHomeNodeId();
				}

			}

		} catch (Exception e) {
			return null;
		}

		return result;
	}

	public String getRepositoryRoot() throws Exception {
		return nodeService.getRootNode(storeRef).getId();
	}

	public List<ChildAssociationRef> getChildAssociationByType(String storeProtocol,String storeId,String nodeId, String type){
		Set<QName> set = new HashSet<QName>();
		set.add(QName.createQName(type));
		return nodeService.getChildAssocs(new NodeRef(new StoreRef(storeProtocol,storeId),nodeId), set);
	}
	
	public HashMap<String, HashMap<String, Object>> getChildrenByType(String nodeId, String type) {
		return this.getChildrenByType(storeRef, nodeId, type);
	}

	public HashMap<String, HashMap<String, Object>> getChildrenByType(StoreRef store, String nodeId, String type) {
		
		HashMap<String, HashMap<String, Object>> result = new HashMap<String, HashMap<String, Object>>();
		List<ChildAssociationRef> childAssocList = nodeService.getChildAssocs(new NodeRef(store, nodeId));
		
		// nodeService.getc
		for (ChildAssociationRef child : childAssocList) {

			String childType = nodeService.getType(child.getChildRef()).toString();
			if (childType.equals(type)) {

				HashMap<String, Object> resultProps = getPropertiesWithoutChildren(child.getChildRef());
				String childNodeId = child.getChildRef().getId();
				result.put(childNodeId, resultProps);

			}
		}
		return result;
	}
	public List<NodeRef> getChildrenByAssociationNodeIds(StoreRef store, String nodeId, String association) {
		
		List<ChildAssociationRef> childAssocList = nodeService.getChildAssocs(new NodeRef(store, nodeId),QName.createQName(association),
				RegexQNamePattern.MATCH_ALL);		
		List<NodeRef> result=new ArrayList<>();
		for (ChildAssociationRef child : childAssocList) {
			result.add(child.getChildRef());
		}
		return result;
	}

	public HashMap<String, HashMap<String, Object>> getChildrenByAssociation(String nodeId, String association) {
		return this.getChildrenByAssociation(storeRef, nodeId, association);
	}

	public HashMap<String, HashMap<String, Object>> getChildrenByAssociation(String store, String nodeId, String association) {

		StoreRef storeRef = null;
		if (store == null) {
			storeRef = this.storeRef;
		} else {
			storeRef = new StoreRef(store);
		}

		return this.getChildrenByAssociation(storeRef, nodeId, association);
	}

	public HashMap<String, HashMap<String, Object>> getChildrenByAssociation(StoreRef store, String nodeId, String association) {
		HashMap<String, HashMap<String, Object>> result = new HashMap<String, HashMap<String, Object>>();
		List<ChildAssociationRef> childAssocList = nodeService.getChildAssocs(new NodeRef(store, nodeId), QName.createQName(association),
				RegexQNamePattern.MATCH_ALL);
		for (ChildAssociationRef child : childAssocList) {
			HashMap<String, Object> resultProps = getPropertiesWithoutChildren(child.getChildRef());
			String childNodeId = child.getChildRef().getId();
			result.put(childNodeId, resultProps);
		}
		return result;
	}

	private HashMap<String, Object> getPropertiesWithoutChildren(NodeRef nodeRef) {
		
		Map<QName, Serializable> childPropMap = nodeService.getProperties(nodeRef);
		HashMap<String, Object> resultProps = new HashMap<String, Object>();

		String nodeType = nodeService.getType(nodeRef).toString();

		for (QName qname : childPropMap.keySet()) {

			Serializable object = childPropMap.get(qname);

			String metadataSetId = (String) childPropMap.get(QName.createQName(CCConstants.CM_PROP_METADATASET_EDU_METADATASET));

			String value = formatData(nodeType, qname.toString(), object, metadataSetId);
			resultProps.put(qname.toString(), value);

			// VCard
			String type = nodeService.getType(nodeRef).toString();
			HashMap<String, Object> vcard = VCardConverter.getVCardHashMap(type, qname.toString(), value);
			if (vcard != null && vcard.size() > 0) resultProps.putAll(vcard);

		}

		resultProps.put(CCConstants.REPOSITORY_ID, repId);
		resultProps.put(CCConstants.REPOSITORY_CAPTION, appInfo.getAppCaption());

		buildUpProperties(resultProps);

		return resultProps;
	}

	public HashMap<String, Object> getChild(String parentId, String type, String property, String value) {
		return this.getChild(storeRef, parentId, type, property, value);
	}

	/**
	 * this method returns the first child that matches the prop value pair it
	 * 
	 * @param store
	 * @param parentId
	 * @param type
	 * @param property
	 * @param value
	 * @return
	 */
	public HashMap<String, Object> getChild(StoreRef store, String parentId, String type, String property, String value) {
		HashMap<String, HashMap<String, Object>> children = this.getChildrenByType(store, parentId, type);
		for (String childNodeId : children.keySet()) {
			HashMap<String, Object> childProps = children.get(childNodeId);
			String propValue = (String) childProps.get(property);
			if (propValue != null && propValue.equals(value))
				return childProps;
		}
		return null;
	}

	/**
	 * @param store
	 * @param parentId
	 * @param type
	 * @param props
	 * @return all nodes that got the same properties like props
	 */
	public HashMap<String, HashMap<String, Object>> getChilden(StoreRef store, String parentId, String type, HashMap props) {
		HashMap<String, HashMap<String, Object>> result = new HashMap<String, HashMap<String, Object>>();
		HashMap<String, HashMap<String, Object>> children = this.getChildrenByType(store, parentId, type);
		for (String childNodeId : children.keySet()) {
			HashMap<String, Object> childProps = children.get(childNodeId);
			boolean allPropsMatched = true;
			for (Object key : props.keySet()) {
				Object searchedPropVal = props.get(key);
				Object foundPropVal = childProps.get(key);
				if (searchedPropVal != null) {
					if (!searchedPropVal.equals(foundPropVal)) {
						allPropsMatched = false;
					}
				} else if (foundPropVal != null) {
					allPropsMatched = false;
				}
			}
			if (allPropsMatched) {
				result.put(childNodeId, childProps);
			}
		}
		return result;
	}

	/**
	 * @param store
	 * @param parentId
	 * @param type
	 * @param props
	 * @return
	 */
	public HashMap<String, Object> getChildRecursive(StoreRef store, String parentId, String type, HashMap props) throws Throwable {

		NodeRef parentNodeRef = new NodeRef(store, parentId);
		List<ChildAssociationRef> childAssocList = nodeService.getChildAssocs(parentNodeRef);
		for (ChildAssociationRef child : childAssocList) {
			boolean propertiesMatched = true;
			String childType = nodeService.getType(child.getChildRef()).toString();
			if (type != null) {
				if (type.equals(childType)) {

					// test with the cached getPops method
					// HashMap<String, Object> childProps =
					// getPropertiesWithoutChildren(child.getChildRef());
					HashMap<String, Object> childProps = getProperties(child.getChildRef());

					if (childProps.size() == 0)
						propertiesMatched = false;

					for (Object key : props.keySet()) {
						if (!childProps.containsKey(key)) {
							propertiesMatched = false;
							break;
						}
						Object childPropVal = childProps.get(key);
						Object searchPropVal = props.get(key);
						if (!searchPropVal.equals(childPropVal)) {
							propertiesMatched = false;
							break;
						}
					}
					if (propertiesMatched) {
						return childProps;
					}
				}
			}

			if (childType.equals(CCConstants.CCM_TYPE_MAP) || childType.equals(CCConstants.CM_TYPE_FOLDER)) {
				HashMap<String, Object> recursiveResult = getChildRecursive(store, child.getChildRef().getId(), type, props);
				if (recursiveResult != null)
					return recursiveResult;
			}

		}
		return null;
	}

	public HashMap<String, Object> getChildRecursive(String parentId, String type, HashMap props) throws Throwable {
		return this.getChildRecursive(storeRef, parentId, type, props);
	}

	/**
	 * uses the getPropertiesCached so no user information is in the result
	 * 
	 * @param store
	 * @param parentId
	 * @param type
	 * @param result
	 * @param cached
	 * @return
	 * @throws Throwable
	 */
	public HashMap<String, HashMap<String, Object>> getChildrenRecursive(StoreRef store, String parentId, String type,
			HashMap<String, HashMap<String, Object>> result, boolean cached) throws Throwable {

		if (result == null)
			result = new HashMap<String, HashMap<String, Object>>();
		NodeRef parentNodeRef = new NodeRef(store, parentId);
		List<ChildAssociationRef> childAssocList = nodeService.getChildAssocs(parentNodeRef);
		for (ChildAssociationRef child : childAssocList) {
			
			/**
			 * Alfresco 4.0.e archiving on: - check if it's not the archive
			 * store (when a object was deleted and it was linked somewhere the
			 * link still exists and points to archive store)
			 */
			if (child.getChildRef().getStoreRef().equals(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE))
				continue;
			
			String childType = nodeService.getType(child.getChildRef()).toString();
			if (type != null) {
				if (type.equals(childType)) {
					HashMap<String, Object> childProps = null;
					if (cached) {
						// don't return user specific info in props like URLs
						// with ticket and so
						childProps = getPropertiesCached(child.getChildRef(), true, true, false);
					} else {
						childProps = getPropertiesCached(child.getChildRef(), false, true, false);
					}
					result.put(child.getChildRef().getId(), childProps);
				}
			}

			if (childType.equals(CCConstants.CCM_TYPE_MAP) || childType.equals(CCConstants.CM_TYPE_FOLDER)) {

				HashMap folderprops = getProperties(child.getChildRef());
				String folderName = (String) folderprops.get(CCConstants.CM_NAME);
				String folderTitle = (String) folderprops.get(CCConstants.CM_PROP_C_TITLE);
				String lomTitle = (String) folderprops.get(CCConstants.LOM_PROP_GENERAL_TITLE);
				logger.info("getChildren of Folder:" + folderName + " folderTitle:" + folderTitle + " lomTitle:" + lomTitle);
				getChildrenRecursive(store, child.getChildRef().getId(), type, result, cached);
			}
		}
		
		if (result.size() > 0)
			return result;
		else
			return null;
	}

	public HashMap<String, HashMap<String, Object>> getChildrenRecursive(String parentId, String type) throws Throwable {
		return getChildrenRecursive(storeRef, parentId, type, null, true);
	}

	/**
	 * @param parentId
	 * @param type
	 * @param props
	 * @return all nodes that got the same properties like props
	 */
	public HashMap<String, HashMap<String, Object>> getChilden(String parentId, String type, HashMap props) {
		return getChilden(storeRef, parentId, type, props);
	}

	public String createNode(String parentID, String nodeTypeString, HashMap<String, Object> _props) {
		return this.createNode(storeRef, parentID, nodeTypeString, _props);
	}

	/**
	 * @param store
	 * @param parentID
	 * @param nodeType
	 * @param properties
	 * @return
	 */
	public String createNode(StoreRef store, String parentID, String nodeTypeString, HashMap<String, Object> _props) {

		return this.createNode(store, parentID, nodeTypeString, CCConstants.CM_ASSOC_FOLDER_CONTAINS, _props);
	}

	@Override
	public String createNode(String parentID, String nodeTypeString, String childAssociation, HashMap<String, Object> _props) {
		return this.createNode(storeRef, parentID, nodeTypeString, childAssociation, _props);
	}

	public String createNode(StoreRef store, String parentID, String nodeTypeString, String childAssociation, HashMap<String, Object> _props) {

		String name = (String)_props.get(CCConstants.CM_NAME);
		_props.put(CCConstants.CM_NAME,CharMatcher.JAVA_ISO_CONTROL.removeFrom(name));
		Map<QName, Serializable> properties = transformPropMap(_props);

		NodeRef parentNodeRef = new NodeRef(store, parentID);
		QName nodeType = QName.createQName(nodeTypeString);

      	String assocName = (String) _props.get(CCConstants.CM_NAME);
		if (assocName == null) {
			assocName = "defaultAssociationName";
		} else {

			// assco name must have be smaller than a maxlength
			// https://issues.alfresco.com/jira/browse/MNT-2417
			assocName = QName.createValidLocalName(assocName);
		}
		assocName = "{" + CCConstants.NAMESPACE_CCM + "}" + assocName;

		ChildAssociationRef childRef = nodeService.createNode(parentNodeRef, QName.createQName(childAssociation), QName.createQName(assocName), nodeType,
				properties);
		return childRef.getChildRef().getId();
	}

	public void addAspect(String nodeId, String aspect) {
		nodeService.addAspect(new NodeRef(storeRef, nodeId), QName.createQName(aspect), null);
	}

	public void updateNode(String nodeId, HashMap<String, Object> _props) {
		this.updateNode(storeRef, nodeId, _props);
	}

	public void updateNode(StoreRef store, String nodeId, HashMap<String, Object> _props) {

		try {
			String name = (String)_props.get(CCConstants.CM_NAME);
			_props.put(CCConstants.CM_NAME,CharMatcher.JAVA_ISO_CONTROL.removeFrom(name));
			Map<QName, Serializable> props = transformPropMap(_props);
			NodeRef nodeRef = new NodeRef(store, nodeId);

			// don't do this cause it's slow:
			/*
			 * for (Map.Entry<QName, Serializable> entry : props.entrySet()) {
			 * nodeService.setProperty(nodeRef, entry.getKey(),
			 * entry.getValue()); }
			 */

			// prevent overwriting of properties that don't come with param _props
			Set<QName> changedProps = props.keySet();
			Map<QName, Serializable> currentProps = nodeService.getProperties(nodeRef);
			for (Map.Entry<QName, Serializable> entry : currentProps.entrySet()) {
				if (!changedProps.contains(entry.getKey())) {
					props.put(entry.getKey(), entry.getValue());
				}
			}

			nodeService.setProperties(nodeRef, props);

		} catch (org.hibernate.StaleObjectStateException e) {
			// this occurs sometimes in workspace
			// it seems it is an alfresco bug:
			// https://issues.alfresco.com/jira/browse/ETHREEOH-2461
			logger.error("Thats maybe an alfreco bug: https://issues.alfresco.com/jira/browse/ETHREEOH-2461", e);
		} catch (org.springframework.orm.hibernate3.HibernateOptimisticLockingFailureException e) {
			// this occurs sometimes in workspace
			// it seems it is an alfresco bug:
			// https://issues.alfresco.com/jira/browse/ETHREEOH-2461
			logger.error("Thats maybe an alfreco bug: https://issues.alfresco.com/jira/browse/ETHREEOH-2461", e);
		}

	}

	public void createAssociation(String fromID, String toID, String association) {
		this.createAssociation(storeRef, fromID, toID, association);
	}

	public void createAssociation(StoreRef store, String fromID, String toID, String association) {
		nodeService.createAssociation(new NodeRef(store, fromID), new NodeRef(store, toID), QName.createQName(association));
	}

	public void createChildAssociation(String from, String to, String assocType, String assocName) {
		nodeService.addChild(new NodeRef(storeRef, from), new NodeRef(storeRef, to), QName.createQName(assocType), QName.createQName(assocName));
	}

	public void writeContent(String nodeID, byte[] content, String mimetype, String encoding, String property) throws Exception {
		this.writeContent(storeRef, nodeID, content, mimetype, encoding, property);
	}

	/**
	 * thats the bad version cause there are OutOfMemory Problems when uploading
	 * large files
	 * 
	 * so its better to use the method with InputStream or file as content
	 * 
	 * @param store
	 * @param nodeID
	 * @param content
	 * @param mimetype
	 * @param _encoding
	 * @param property
	 * @throws Exception
	 */
	public void writeContent(final StoreRef store, final String nodeID, final byte[] content, final String mimetype, String _encoding, final String property)
			throws Exception {
		ByteArrayInputStream is = new ByteArrayInputStream(content);
		this.writeContent(store, nodeID, is, mimetype, _encoding, property);
	}

	/**
	 * @param store
	 * @param nodeID
	 * @param content
	 * @param mimetype
	 * @param _encoding
	 * @param property
	 * @throws Exception
	 */
	public void writeContent(final StoreRef store, final String nodeID, final File content, final String mimetype, String _encoding, final String property)
			throws Exception {
		FileInputStream fis = new FileInputStream(content);
		this.writeContent(store, nodeID, fis, mimetype, _encoding, property);
	}
	
	/**
	 * Runs a transaction
	 * @param callback the callback to run
	 */
	public Object doInTransaction(RetryingTransactionCallback callback){
		TransactionService transactionService = serviceRegistry.getTransactionService();
		return transactionService.getRetryingTransactionHelper().doInTransaction(callback, false);
	}

	/**
	 * @param store
	 * @param nodeID
	 * @param content
	 * @param mimetype
	 * @param _encoding
	 * @param property
	 * @throws Exception
	 */
	public void writeContent(final StoreRef store, final String nodeID, final InputStream content, final String mimetype, String _encoding,
			final String property) throws Exception {

		final String encoding = (_encoding == null) ? "UTF-8" : _encoding;
		logger.debug("called nodeID:" + nodeID + " store:" + store + " mimetype:" + mimetype + " property:" + property);

		RetryingTransactionCallback callback = new RetryingTransactionCallback() {
			@Override
			public Object execute() throws Throwable {

				NodeRef nodeRef = new NodeRef(store, nodeID);
				final ContentWriter contentWriter = contentService.getWriter(nodeRef, QName.createQName(property), true);
				contentWriter.addListener(new ContentStreamListener() {
					@Override
					public void contentStreamClosed() throws ContentIOException {
						logger.debug("Content Stream was closed");
						logger.debug(" size:" + contentWriter.getContentData().getSize()+
									", URL:" + contentWriter.getContentData().getContentUrl()+
								 	", MimeType:" + contentWriter.getContentData().getMimetype()+"" +
									", ContentData ToString:" + contentWriter.getContentData().toString());
					}
				});
				
				String finalMimeType = mimetype;
				if(finalMimeType == null || finalMimeType.trim().equals("")) {
					finalMimeType = MCAlfrescoAPIClient.this.guessMimetype(MCAlfrescoAPIClient.this.getProperty(storeRef, nodeID, CCConstants.CM_NAME));
				}
				
				contentWriter.setMimetype(finalMimeType);
				contentWriter.setEncoding(encoding);

				InputStream is = content;
				contentWriter.putContent(is);

				return null;
			}
		};
		TransactionService transactionService = serviceRegistry.getTransactionService();
		transactionService.getRetryingTransactionHelper().doInTransaction(callback, false);
	}

	public void setUserDefinedPreview(String nodeId, byte[] content, String fileName) {
		
		String tmpDir = System.getProperty("java.io.tmpdir");
		String filePath = tmpDir + File.pathSeparator + "edu-sharing" + File.pathSeparator + "udpreview" + File.pathSeparator + System.currentTimeMillis()
				+ fileName;
		try {
			FileOutputStream fos = new FileOutputStream(filePath);
			fos.write(content);
			fos.close();

			this.setUserDefinedPreview(nodeId, new File(filePath), fileName);

			new File(filePath).delete();

		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * when userdefined preview is removed
	 * NodeCustomizationPolicies.onContentUpdate would be excecuted, cause it's
	 * not looking on which of the contentproperties is updated so we need to
	 * disable the policy behavior here.
	 * 
	 * to disable/enable the policy behavior there must be an transaction active
	 * 
	 * @param fileName
	 * @param file
	 * @param nodeId
	 */
	public void setUserDefinedPreview(String nodeId, File file, String fileName) {

		BehaviourFilter behaviourFilter = (BehaviourFilter) applicationContext.getBean("policyBehaviourFilter");
		TransactionService ts = serviceRegistry.getTransactionService();
		UserTransaction ut = ts.getNonPropagatingUserTransaction();
		try {
			ut.begin();

			NodeRef ioNodeRef = new NodeRef(MCAlfrescoAPIClient.storeRef, nodeId);
			try {
				
				behaviourFilter.disableBehaviour(ioNodeRef);
				ContentReader reader = new FileContentReader(file);

				if (fileName != null) {
					// BUG when IE: filename is the whole filepath
					int slash = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\')); 
					// check for Unix AND Win separator
					if (slash > -1) fileName = fileName.substring(slash + 1);
				}

				if (fileName != null) {
					String mimeType = serviceRegistry.getMimetypeService().guessMimetype(fileName);
					if (mimeType == null) {
						mimeType = MimeTypes.guessMimetype(fileName);
					}
					reader.setMimetype(mimeType);
				}

				ContentWriter writer = contentService.getWriter(ioNodeRef, QName.createQName(CCConstants.CCM_PROP_IO_USERDEFINED_PREVIEW), true);
				writer.setMimetype("image/png");

				ThumbnailRegistry thumbnailRegistry = (ThumbnailRegistry) applicationContext.getBean("thumbnailRegistry");
				ThumbnailDefinition thumbDef = thumbnailRegistry.getThumbnailDefinition("imgpreview");

				if (contentService.isTransformable(reader, writer, thumbDef.getTransformationOptions())) {
					contentService.transform(reader, writer, thumbDef.getTransformationOptions());
				} else {
					logger.error(reader.getMimetype() + " is not transformable to image/png");
				}
				
			} finally {
				behaviourFilter.enableBehaviour(ioNodeRef);
			}

			ut.commit();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return;
	}

	/**
	 * when user defined preview is removed
	 * NodeCustomizationPolicies.onContentUpdate would be excecuted, cause it's
	 * not looking on which of the contentproperties is updated so we need to
	 * disable the policy behavior here.
	 * 
	 * to disable/enable the policy behavior there must be an transaction active
	 * 
	 * @param nodeId
	 */
	public void removeUserDefinedPreview(String nodeId) {

		BehaviourFilter behaviourFilter = (BehaviourFilter) applicationContext.getBean("policyBehaviourFilter");
		TransactionService ts = serviceRegistry.getTransactionService();
		UserTransaction ut = ts.getNonPropagatingUserTransaction();
		try {
			ut.begin();

			NodeRef ioNodeRef = new NodeRef(MCAlfrescoAPIClient.storeRef, nodeId);
			try {
				behaviourFilter.disableBehaviour(ioNodeRef);
				nodeService.setProperty(new NodeRef(storeRef, nodeId), QName.createQName(CCConstants.CCM_PROP_IO_USERDEFINED_PREVIEW), null);
			} finally {
				behaviourFilter.enableBehaviour(ioNodeRef);
			}

			ut.commit();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	public void removeGlobalAspectFromGroup(String groupNodeId) throws Exception {
		
		UserTransaction userTransaction = serviceRegistry.getTransactionService().getNonPropagatingUserTransaction();
		
		userTransaction.begin();
		try{
			
			NodeRef nodeRef = new NodeRef(storeRef,groupNodeId );
			nodeService.removeAspect(nodeRef,QName.createQName( CCConstants.CCM_ASPECT_SCOPE));
			String authorityName = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_AUTHORITY_NAME);
			Set<String> userNames = authorityService.getContainedAuthorities(AuthorityType.USER,authorityName,false);
			
			//remove all shadow users from group
			for(String username : userNames){
				NodeRef personNodeRef = personService.getPerson(username);
				Map<QName, Serializable> personProps = nodeService.getProperties(personNodeRef);
				String repoId = (String)personProps.get(QName.createQName(CCConstants.PROP_USER_REPOSITORYID));
				if(repoId != null && !repoId.trim().equals("") && !appInfo.getAppId().equals(repoId)){
					authorityService.removeAuthority(authorityName, username);
				}
			}
			
			userTransaction.commit();
			
		} catch (Throwable e) {
			userTransaction.rollback();
		}

	}
	
	public Map<String, Serializable> transformQNameKeyToString(Map<QName, Serializable> props) {
		Map<String, Serializable> result = new HashMap<String, Serializable>();
		for (Map.Entry<QName, Serializable> entry : props.entrySet()) {
			result.put(entry.getKey().toString(), entry.getValue());
		}
		return result;
	}

	/**
	 * transform to Alfresco HashMap
	 * 
	 * @param map
	 * @return
	 */
	Map<QName, Serializable> transformPropMap(HashMap map) {
		Map<QName, Serializable> result = new HashMap<QName, Serializable>();
		for (Object key : map.keySet()) {

			try {
				Object value = map.get(key);
				if (value instanceof HashMap) {
					value = getMLText((HashMap) value);
				} else if (value instanceof List) {
					List transformedList = new ArrayList();
					for (Object valCol : (ArrayList) value) {
						if (valCol instanceof HashMap) {
							transformedList.add(getMLText((HashMap) valCol));
						} else {
							transformedList.add(valCol);
						}
					}
					value = transformedList;
				}
				result.put(QName.createQName((String) key), (Serializable) value);
			} catch (ClassCastException e) {
				logger.error("this prop has a wrong value:" + key + " val:" + map.get(key));
				logger.error(e.getMessage(), e);
			}
		}
		return result;
	}

	private MLText getMLText(HashMap i18nMap) {
		MLText mlText = new MLText();
		for (Object obj : i18nMap.keySet()) {
			String locale = (String) obj;
			mlText.addValue(new Locale(locale), (String) i18nMap.get(obj));
		}
		return mlText;
	}

	public Map<String, Serializable> transformPropMapToStringKeys(HashMap map) {
		Map<String, Serializable> result = new HashMap<String, Serializable>();
		for (Object key : map.keySet()) {
			result.put((String) key, (Serializable) map.get(key));
		}
		return result;
	}

	@Override
	public String getHomeFolderID(String username) throws Exception {
		
		if (NodeServiceInterceptor.getEduSharingScope() == null ||
				NodeServiceInterceptor.getEduSharingScope().trim().equals("")) {
			NodeRef person = serviceRegistry.getPersonService().getPerson(username,false);
			if (person != null) {
				NodeRef homfolder = (NodeRef) nodeService.getProperty(person, QName.createQName(CCConstants.CM_PROP_PERSON_HOME_FOLDER));
				return (homfolder != null) ? homfolder.getId() : null;
			} else {
				return null;
			}
			
		} else {
			
			NodeRef userHome = ScopeUserHomeServiceFactory.getScopeUserHomeService().getUserHome(
					username, 
					NodeServiceInterceptor.getEduSharingScope(),
					true);
			String userHomeNodeId = (userHome != null) ? userHome.getId() : null;
			return userHomeNodeId;
		}
	}

	public synchronized void createVersion(String nodeId) throws Exception {

		VersionService versionService = serviceRegistry.getVersionService();
		NodeRef nodeRef = new NodeRef(storeRef, nodeId);
		Map<String, Serializable> transFormedProps = transformQNameKeyToString(nodeService.getProperties(nodeRef));
		if (versionService.getVersionHistory(nodeRef) == null) {

			// see https://issues.alfresco.com/jira/browse/ALF-12815
			// alfresco-4.0.d fix version should start with 1.0 not with 0.1
			transFormedProps.put(VersionModel.PROP_VERSION_TYPE, VersionType.MAJOR);
		}
		versionService.createVersion(nodeRef, transFormedProps);


	}

	public String getPath(String nodeID) {
		return nodeService.getPath(new NodeRef(storeRef, nodeID)).toPrefixString(namespaceService);
	}

	public List<String> getAssociationNodeIds(String nodeID, String association) {
		NodeRef nodeRef = new NodeRef(storeRef, nodeID);
		List<String> result = new ArrayList<String>();
		for(NodeRef assocNodeRef : getAssociationNodeIds(nodeRef, association)){
			result.add(assocNodeRef.getId());
		}
		return result;
	}
	
	/**
	 * returns target Assocs NodeIds
	 * 
	 * @param nodeID
	 * @param association
	 * @return
	 */
	public List<NodeRef> getAssociationNodeIds(NodeRef nodeRef, String association) {

		List<NodeRef> result = new ArrayList<NodeRef>();

		
		QName assocQName = QName.createQName(association);

		List<AssociationRef> targetAssoc = nodeService.getTargetAssocs(nodeRef, assocQName);
		for (AssociationRef assocRef : targetAssoc)
			result.add(assocRef.getTargetRef());
		return result;
	}
	
	public HashMap<String, HashMap> getAssocNode(String nodeid, String association) throws Throwable {
		
		HashMap<String, HashMap> result = new HashMap<String, HashMap>();
		for(Map.Entry<NodeRef,HashMap> entry : getAssocNode(new NodeRef(storeRef,nodeid), association).entrySet()){
			result.put(entry.getKey().getId(), entry.getValue());
		}
		
		return result;
	}

	public HashMap<NodeRef, HashMap> getAssocNode(NodeRef nodeRef, String association) throws Throwable {
		HashMap<NodeRef, HashMap> result = new HashMap<NodeRef, HashMap>();
		List<NodeRef> nodeIds = this.getAssociationNodeIds(nodeRef, association);
		for (NodeRef nodeId : nodeIds) {
			result.put(nodeId, getProperties(nodeId));
		}
		return result;
	}

	public void removeAspect(String nodeId, String aspect) {
		nodeService.removeAspect(new NodeRef(storeRef, nodeId), QName.createQName(aspect));
	}

	public void removeAssociation(String fromID, String toID, String association) throws Exception {
		nodeService.removeAssociation(new NodeRef(storeRef, fromID), new NodeRef(storeRef, toID), QName.createQName(association));
	}
	
	/**
	 * Throws NoSuchPersonException when user not exists
	 */
	public HashMap<String, String> getUserInfo(String userName) throws Exception {
		
		return serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(
				
                new RetryingTransactionCallback<HashMap<String, String>>()
                {
                    public HashMap<String, String> execute() throws Throwable
                    {
                		NodeRef personRef = serviceRegistry.getPersonService().getPerson(userName, false);
                		if (personRef == null) {
                			return null;
                		}

                		Map<QName, Serializable> tmpProps = nodeService.getProperties(personRef);
                		HashMap<String, String> result = new HashMap<String, String>();
                		for (Map.Entry<QName, Serializable> entry : tmpProps.entrySet()) {
                			
                			Object value = entry.getValue();
                			
                			result.put(
                					entry.getKey().toString(), 
                					(value instanceof NodeRef) 
                						? ((NodeRef) value).getId() 
                						: (value != null)
                							? value.toString()
                							: null);

                		}

                		return result;

                    }
                }, true); 		
	}
	
	public String getGroupDisplayName(String groupName) throws Exception {
				
		AuthorityService authorityService = serviceRegistry.getAuthorityService();

		return serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(
				
                new RetryingTransactionCallback<String>()
                {
                    public String execute() throws Throwable
                    {
	                    try {
		                		String key = PermissionService.GROUP_PREFIX + groupName;
		                		
		                		return 	  authorityService.authorityExists(key)
		                				? authorityService.getAuthorityDisplayName(key)
		                				: null;
		                } catch(Throwable e) {
	                    	logger.error(e.getMessage(), e);
	                    	return null;
	                    }
                    }
                }, true); 
		
	}

	public String getGroupNodeId(String groupName) throws Exception {
		
		AuthorityService authorityService = serviceRegistry.getAuthorityService();

		return serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(
				
                new RetryingTransactionCallback<String>()
                {
                    public String execute() throws Throwable
                    {
                		String key = groupName.startsWith(PermissionService.GROUP_PREFIX) ? groupName : PermissionService.GROUP_PREFIX + groupName;
                		
                		return 	  authorityService.authorityExists(key)
                				? authorityService.getAuthorityNodeRef(key).getId()
                				: null;
                    }
                }, true); 
		
	}

	public String getEduGroupFolder(String groupName) throws Exception {
		
		return serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(
				
                new RetryingTransactionCallback<String>()
                {
                    public String execute() throws Throwable
                    {
						String key = groupName.startsWith(PermissionService.GROUP_PREFIX) ? groupName : PermissionService.GROUP_PREFIX + groupName;
                		
                    	NodeRef nodeRef = serviceRegistry.getAuthorityService().getAuthorityNodeRef(key);
                    	
                    	if (nodeRef == null) {
                    		return null;
                    	}
                    	
                    	
                    	NodeRef folderRef = (NodeRef) serviceRegistry.getNodeService().getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_EDUGROUP_EDU_HOMEDIR));

                    	return 	  folderRef != null
                				? folderRef.getId()
                				: null;
                    }
                }, true); 
		
	}
	
	public void createOrUpdateGroup(String groupName, String displayName) throws Exception {
		createOrUpdateGroup(groupName, displayName,null,false);
	}

	public String createOrUpdateGroup(String groupName, String displayName,String parentGroup,boolean preventDuplicate) throws Exception {
		
		if (parentGroup!=null) {
			if(getGroupNodeId(parentGroup)==null){
				throw new IllegalArgumentException("parent group "+parentGroup+" does not exists");
			}
		}
		
		return serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(
				
                new RetryingTransactionCallback<String>()
                {
                    public String execute() throws Throwable
                    {
                    	 return eduAuthorityService.createOrUpdateGroup(groupName, displayName, parentGroup, preventDuplicate);
                    }
                }, false); 

	}
	
	public String[] getUserNames() throws Exception {
	
		PersonService personService = serviceRegistry.getPersonService();
		
		return serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(
				
                new RetryingTransactionCallback<String[]>()
                {
                    public String[] execute() throws Throwable
                    {
                        PagingResults<PersonInfo> peopleReq = 
                        		personService.getPeople(
                        				null, 
                        				null, 
                        				null, 
                        				new PagingRequest(Integer.MAX_VALUE, null));
                        		
                        List<String> userNames = new ArrayList<String>();        
                        for (PersonInfo personInfo : peopleReq.getPage())
                        {
                            userNames.add(personInfo.getUserName());
                        }
                		
                		return userNames.toArray(new String[0]);
                    }
                }, true); 
	}

	public String[] searchUserNames(String pattern) throws Exception {
		
		PersonService personService = serviceRegistry.getPersonService();
		
		return serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(
				
                new RetryingTransactionCallback<String[]>() {
                    public String[] execute() throws Throwable {
                    	List<QName> filters = new ArrayList<QName>();
                        filters.add(ContentModel.PROP_FIRSTNAME);
                        filters.add(ContentModel.PROP_LASTNAME);
                        filters.add(ContentModel.PROP_EMAIL);

                        PagingResults<PersonInfo> peopleReq = 
                        		personService.getPeople(
                        				pattern, 
                        				filters, 
                        				null, 
                        				new PagingRequest(Integer.MAX_VALUE, null));
                        		
                        List<String> userNames = new ArrayList<String>();        
                        for (PersonInfo personInfo : peopleReq.getPage()) {
                            userNames.add(personInfo.getUserName());
                        }
                		
                		return userNames.toArray(new String[0]);
                    }
                }, true); 
		
	}

	public String[] getGroupNames() {
		
		AuthorityService authorityService = serviceRegistry.getAuthorityService();
		
		return serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(
				
                new RetryingTransactionCallback<String[]>()
                {
                    public String[] execute() throws Throwable
                    {
                        PagingResults<String> groupReq = 
                        		authorityService.getAuthorities(
                        				AuthorityType.GROUP,
                        				AuthorityService.ZONE_APP_DEFAULT, 
                        				null, 
                        				false,
                        				false,
                        				new PagingRequest(Integer.MAX_VALUE, null));
                        		
                        List<String> groupNames = new ArrayList<String>();        
                        for (String groupName : groupReq.getPage()) {
                        		if (groupName.startsWith(PermissionService.GROUP_PREFIX)) {
                        			groupName = groupName.substring(PermissionService.GROUP_PREFIX.length());
                        		}
                            groupNames.add(groupName);
                        }
                		
                		return groupNames.toArray(new String[0]);
                    }
                }, true); 
		
	}

	public String[] searchGroupNames(String pattern) throws Exception {
		
		AuthorityService authorityService = serviceRegistry.getAuthorityService();
		
		return serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(
				
                new RetryingTransactionCallback<String[]>()
                {
                    public String[] execute() throws Throwable
                    {
                        PagingResults<AuthorityInfo> groupReq = 
                        		authorityService.getAuthoritiesInfo(
                        				AuthorityType.GROUP, 
                        				null, 
                        				pattern, 
                        				null, 
                        				true,
                        				new PagingRequest(Integer.MAX_VALUE, null));
                        		
                        List<String> groupNames = new ArrayList<String>();        
                        for (AuthorityInfo groupInfo : groupReq.getPage()) {
                            groupNames.add(groupInfo.getAuthorityName());
                        }
                		
                		return groupNames.toArray(new String[0]);
                    }
                }, true); 
		
	}
	
	public void updateUser(HashMap<String, ?> userInfo) throws Exception {
		
		if(userInfo == null) {
			throw new PropertyRequiredException(CCConstants.CM_PROP_PERSON_USERNAME);
		}
		
		String userName = (String)userInfo.get(CCConstants.CM_PROP_PERSON_USERNAME);
		String currentUser = AuthenticationUtil.getRunAsUser();

		if (!currentUser.equals(userName) && !isAdmin()) {
			throw new AccessDeniedException("admin role required.");
		}
		
		PersonService personService = serviceRegistry.getPersonService();
		
		serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(
				
        new RetryingTransactionCallback<Void>() {
            public Void execute() throws Throwable {
        		Throwable runAs = AuthenticationUtil.runAs(
        				
    				new AuthenticationUtil.RunAsWork<Throwable>() {
    					
    					@Override
    					public Throwable doWork() throws Exception {
    						try{
    							addUserExtensionAspect(userName);
    							personService.setPersonProperties(userName, transformPropMap(userInfo));
    						} catch (Throwable e) {
    							logger.error(e.getMessage(), e);
    							return e;
    						}
    						return null;
    					}
    				}, 
    				ApplicationInfoList.getHomeRepository().getUsername());
        		
        		if (runAs != null) {
        			throw runAs;
        		}
        		return null;
            }
        }, 
        false); 
	}
	
	private void addUserExtensionAspect(String userName) {
		PersonService personService = serviceRegistry.getPersonService();
		if(!nodeService.hasAspect(personService.getPerson(userName),QName.createQName(CCConstants.CCM_ASPECT_USER_EXTENSION)))
				nodeService.addAspect(personService.getPerson(userName),QName.createQName(CCConstants.CCM_ASPECT_USER_EXTENSION),null);
	}
	
	public void createOrUpdateUser(HashMap<String, String> userInfo) throws Exception {
		
		String currentUser = AuthenticationUtil.getRunAsUser();
		
		if(userInfo == null){
			throw new PropertyRequiredException(CCConstants.CM_PROP_PERSON_USERNAME);
		}
		
		String userName = userInfo.get(CCConstants.CM_PROP_PERSON_USERNAME);
		String firstName = userInfo.get(CCConstants.CM_PROP_PERSON_FIRSTNAME);
		String lastName = userInfo.get(CCConstants.CM_PROP_PERSON_LASTNAME);
		String email = userInfo.get(CCConstants.CM_PROP_PERSON_EMAIL);
		
		if(userName == null || userName.trim().equals("")){
			throw new PropertyRequiredException(CCConstants.CM_PROP_PERSON_USERNAME);
		}
		
		if(firstName == null || firstName.trim().equals("")){
			throw new PropertyRequiredException(CCConstants.CM_PROP_PERSON_FIRSTNAME);
		}
		
		if(lastName == null || lastName.trim().equals("")){
			throw new PropertyRequiredException(CCConstants.CM_PROP_PERSON_LASTNAME);
		}
		
		if(email == null || email.trim().equals("")){
			throw new PropertyRequiredException(CCConstants.CM_PROP_PERSON_EMAIL);
		}
		
		if (!currentUser.equals(userName) && !isAdmin()) {
			throw new AccessDeniedException("admin role required.");
		}
		
		PersonService personService = serviceRegistry.getPersonService();
		
		serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(
				
            new RetryingTransactionCallback<Void>()
            {
                public Void execute() throws Throwable
                {
            		Throwable runAs = AuthenticationUtil.runAs(
            				
        				new AuthenticationUtil.RunAsWork<Throwable>() {
        					
        					@Override
        					public Throwable doWork() throws Exception {
        						
        						try {
        							
        	                    	if (personService.personExists(userName)) {
        	                			
        	                			personService.setPersonProperties(userName, transformPropMap(userInfo));
        	                			
        	                		} else {
        	                			
        	                			personService.createPerson(transformPropMap(userInfo));
        	                		}
        	                    	addUserExtensionAspect(userName);

        						} catch (Throwable e) {
        							logger.error(e.getMessage(), e);
        							return e;
        						}
        						
        						return null;
        					}
        				}, 
        				ApplicationInfoList.getHomeRepository().getUsername());
            		
            		if (runAs != null) {
            			throw runAs;
            		}
            		
            		return null;
                }
                
            }, 
            false); 
		
	}

	public void deleteUser(String userName) {
				
		PersonService personService = serviceRegistry.getPersonService();
		
		serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(
				
                new RetryingTransactionCallback<Void>()
                {
                    public Void execute() throws Throwable
                    {
                		personService.deletePerson(userName);

                		return null;
                    }
                }, false); 
		
	}
	
	public void deleteGroup(String groupName) {
		
		AuthorityService authorityService = serviceRegistry.getAuthorityService();
		
		serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(
				
                new RetryingTransactionCallback<Void>()
                {
                    public Void execute() throws Throwable
                    {
                		String key = PermissionService.GROUP_PREFIX + groupName;
                		
                		authorityService.deleteAuthority(key, true);

                		return null;
                    }
                }, false); 
	}

	public void removeAllMemberships(String groupName) {
		
		AuthorityService authorityService = serviceRegistry.getAuthorityService();
		
		serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(
				
                new RetryingTransactionCallback<Void>()
                {
                    public Void execute() throws Throwable
                    {
                		String key = PermissionService.GROUP_PREFIX + groupName;
                		
                		for (String containedAuthority : authorityService.getContainedAuthorities(null, key, true)) {
                			
                			authorityService.removeAuthority(key, containedAuthority);
                		}

                		return null;
                    }
                }, false); 


	}

	public void setUserPassword(String userName, String newPassword){
		
		MutableAuthenticationService authenticationService = serviceRegistry.getAuthenticationService();
		
		if (authenticationService.isAuthenticationMutable(userName)) {
			
			authenticationService.setAuthentication(userName, newPassword.toCharArray());
			
		} else {
			
			authenticationService.createAuthentication(userName, newPassword.toCharArray());
		}		
	}

	public void updateUserPassword(String userName, String oldPassword, String newPassword){
		
		MutableAuthenticationService authenticationService = serviceRegistry.getAuthenticationService();
		
		if (authenticationService.isAuthenticationMutable(userName)) {
			
			authenticationService.updateAuthentication(userName, oldPassword.toCharArray(), newPassword.toCharArray());
			
		} 		
	}
	
	public void removeNode(String nodeID, String fromID) {
		removeNode(nodeID,fromID,true);
	}
	
	public void removeNode(String nodeID, String fromID,boolean recycle) {
		// NodeService.removeChild will lead to an accessdeniedException when
		// the user got no DeleteChildren permission on the folder(fromId)
		// this appears i.e. when we have an linked GroupFolder and the group
		// gots the Collaborator right on it,
		// cause Collaborator brings no DeleteChildren permission with
		// so if fromID is the primary parent then we call deleteNode instead of
		// removeChild
		NodeRef nodeRef = new NodeRef(storeRef, nodeID);
		ChildAssociationRef childAssocRef = nodeService.getPrimaryParent(nodeRef);
		if(fromID == null){
			fromID = childAssocRef.getParentRef().getId();
		}
		if (childAssocRef.getParentRef().getId().equals(fromID)) {

			if(!recycle){
				nodeService.addAspect(nodeRef, ContentModel.ASPECT_TEMPORARY, null);
			}
			nodeService.deleteNode(nodeRef);

		} else {
			nodeService.removeChild(new NodeRef(storeRef, fromID), nodeRef);
		}

	}

	public void removeNodeAndRelations(String nodeID, String fromID) throws Throwable {
		this.removeNodeAndRelations(nodeID, fromID, true);
	}
	
	public void removeNodeAndRelations(String nodeID, String fromID, boolean recycle) throws Throwable {
		logger.info("called");
		this.removeRelationsForNode(nodeID, fromID);
		this.removeNode(nodeID, fromID,recycle);
		logger.info("return");
	}

	public boolean hasContent(String nodeId, String contentProp) throws Exception {
		ContentReader reader = serviceRegistry.getContentService().getReader(new NodeRef(storeRef, nodeId), QName.createQName(contentProp));
		if (reader != null && reader.getSize() > 0) {
			return true;
		} else {
			return false;
		}
	}

	public void executeAction(String nodeId, String actionName, String actionId, HashMap parameters, boolean async) {
		
		ActionService actionService = serviceRegistry.getActionService();
		Action action = actionService.createAction(actionName);
		action.setTrackStatus(true);

		NodeRef nodeRef = new NodeRef(storeRef, nodeId);

		if (async) {
			ActionObserver.getInstance().addAction(nodeRef, action);
		}
		if (parameters != null) {
			for (Object key : parameters.keySet()) {
				action.setParameterValue((String) key, (Serializable) parameters.get(key));
			}
		}
		actionService.executeAction(action, nodeRef, true, async);

	}

	public String getGroupFolderId() throws Throwable {
		return getGroupFolderId(this.authenticationInfo.get(CCConstants.AUTH_USERNAME));
	}

	public String getGroupFolderId(String userName)  {
		try {
			String homeFolder=getHomeFolderID(userName);
			if(homeFolder==null){
				logger.info("User "+userName+" has no home folder, will return no group folder for person");
				return null;
			}
			NodeRef child = NodeServiceFactory.getLocalService().getChild(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, homeFolder, CCConstants.CCM_TYPE_MAP, CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_EDUGROUP);
			return child.getId();
		}catch(Exception e){
            logger.info("Exception while fetching user "+userName+": "+e.getMessage()+", will return no group folder for person");
			return null;
		}
	}

	public HashMap getGroupFolders() throws Throwable {
		String folderId = getGroupFolderId();
		if (folderId == null) {
			logger.info("No GroupFolders ... returning empty map.");
			return new HashMap();
		}
		final HashMap map = getChildren(folderId);
		logger.debug("No GroupFolders ... returning map with size(" + map.size() + ").");
		return map;
	}

	public ArrayList<EduGroup> getEduGroups() throws Throwable {
		
		ArrayList<EduGroup> result = new ArrayList<EduGroup>();
		HashMap<String,HashMap<String,Object>> edugroups = search("@ccm\\:edu_homedir:\"workspace://*\"");
		for(Map.Entry<String,HashMap<String,Object>> entry : edugroups.entrySet()){
			String nodeRef = (String)entry.getValue().get(CCConstants.CCM_PROP_AUTHORITYCONTAINER_EDUHOMEDIR);
			//when a group folder relation is removed the noderef can be null cause of async solr refresh
			try{
				if(nodeRef != null){
					String nodeId = nodeRef.replace("workspace://SpacesStore/", "");
					HashMap<String, Object> folderProps = getProperties(nodeId);
					EduGroup eduGroup = new EduGroup();
					eduGroup.setFolderId((String)folderProps.get(CCConstants.SYS_PROP_NODE_UID));
					eduGroup.setFolderName((String)folderProps.get(CCConstants.CM_NAME));
					eduGroup.setGroupId((String)entry.getValue().get(CCConstants.SYS_PROP_NODE_UID));
					eduGroup.setGroupname((String)entry.getValue().get(CCConstants.CM_PROP_AUTHORITY_AUTHORITYNAME));
					eduGroup.setGroupDisplayName((String)entry.getValue().get(CCConstants.CM_PROP_AUTHORITY_AUTHORITYDISPLAYNAME));
					eduGroup.setFolderPath(getPath((String)folderProps.get(CCConstants.SYS_PROP_NODE_UID)));
					result.add(eduGroup);
				}
			}catch(AccessDeniedException e){}
		}
		return result;
	}

	public String getFavoritesFolder() throws Throwable {

		String userName = this.authenticationInfo.get(CCConstants.AUTH_USERNAME);

		String homefolderID = getHomeFolderID(userName);
		HashMap<String, HashMap<String, Object>> children = getChildren(homefolderID, CCConstants.CCM_TYPE_MAP);

		String basketsFolderID = null;

		for (String key : children.keySet()) {

			HashMap<String, Object> props = children.get(key);

			if (CCConstants.CCM_VALUE_MAP_TYPE_FAVORITE.equals(props.get(CCConstants.CCM_PROP_MAP_TYPE))) {
				basketsFolderID = key;
				break;
			}
		}

		if (basketsFolderID == null) {

			logger.info("creating Favorites Folder for:" + userName);

			HashMap<String, Object> properties = new HashMap<String, Object>();

			// get an alfresco installation locale corresponding name
			String userFavoritesFolderName = I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_USERFOLDER_FAVORITES);

			String folderName = new DuplicateFinder().getUniqueValue(getChildren(homefolderID), CCConstants.CM_NAME, userFavoritesFolderName);

			properties.put(CCConstants.CM_NAME, folderName);
			properties.put(CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_FAVORITE);

			basketsFolderID = createNode(homefolderID, CCConstants.CCM_TYPE_MAP, CCConstants.CM_ASSOC_FOLDER_CONTAINS, properties);
		}

		return basketsFolderID;
	}

	public HashMap getBaskets() throws Throwable {
		String favoritesFolderID = getFavoritesFolder();
		HashMap favoritesfolderChilds = getChildren(favoritesFolderID);
		Object[] keyarr = favoritesfolderChilds.keySet().toArray();
		HashMap basketHashMap = new HashMap();
		for (Object key : keyarr) {
			HashMap properties = (HashMap) favoritesfolderChilds.get(key);
			if (properties.get(CCConstants.NODETYPE).equals(CCConstants.CM_TYPE_FOLDER)
					|| properties.get(CCConstants.NODETYPE).equals(CCConstants.CCM_TYPE_MAP)) {
				HashMap basketContent = getChildren((String) key);
				properties.put(CCConstants.CCM_ASSOC_BASKETCONTENT, basketContent);
				basketHashMap.put(key, properties);
			}
		}
		return basketHashMap;
	}

	public HashMap<String, Boolean> hasAllPermissions(String nodeId, String[] permissions) {
		return hasAllPermissions(storeRef.getProtocol(), storeRef.getIdentifier(), nodeId, permissions);
	}
	
	public HashMap<String, Boolean> hasAllPermissions(String storeProtocol, String storeId, String nodeId, String[] permissions) {
		ApplicationInfo appInfo = ApplicationInfoList.getHomeRepository();
		String guestName = appInfo.getGuest_username();
		boolean guest=guestName!=null && guestName.equals(AuthenticationUtil.getFullyAuthenticatedUser());
		PermissionService permissionService = serviceRegistry.getPermissionService();
		HashMap<String, Boolean> result = new HashMap<String, Boolean>();
		NodeRef nodeRef = new NodeRef(new StoreRef(storeProtocol,storeId), nodeId);
		if (permissions != null && permissions.length > 0) {
			for (String permission : permissions) {
				AccessStatus accessStatus = permissionService.hasPermission(nodeRef, permission);
				// Guest only has read permissions, no modify permissions
				if(guest && !Arrays.asList(org.edu_sharing.service.permission.PermissionService.GUEST_PERMISSIONS).contains(permission)){
					accessStatus=AccessStatus.DENIED;
				}
				if (accessStatus.equals(AccessStatus.ALLOWED)) {
					result.put(permission, new Boolean(true));
				} else {
					result.put(permission, new Boolean(false));
				}
			}
		}
		return result;
	}

	/**
	 * with this method you got the opportunity to test if an authority has a
	 * set of permissions it's using the AuthenticationUtil.runAs Method of the
	 * Alfresco Foundation API
	 */
	public HashMap<String, Boolean> hasAllPermissions(String nodeId, String authority, String[] permissions) throws Exception {
		
		/**
		 * Strange behavior in alfresco: GROUP_EVERYONE is not a real group.
		 * When calling PermissionService.hasPermissions alfresco checks if the authority exists
		 * so alfresco-5.0.d raises an exception at AuthorityDAOImpl.listAuthorities(AuthorityDAOImpl.java:1086)
		 * 
		 * but this check is not done with users (without "GROUP_" prefix)
		 * 
		 * 
		 * so the workaround is to change the authority to a user that does not exist. 
		 * if GROUP_EVERYONE got's a permission on the node, the not existent user would have the permission, else not
		 * 
		 * so we change the authority name to EVERYONE 
		 * @TODO policy that prevents "EVERYONE" to become a real alfresco user
		 */
		if(authority.equals(PermissionService.ALL_AUTHORITIES)){
			authority = "EVERYONE";
		}
		
		OwnableService ownableService = serviceRegistry.getOwnableService();
		PermissionService permissionService = serviceRegistry.getPermissionService();
		if (authority.equals(PermissionService.OWNER_AUTHORITY)) {
			authority = ownableService.getOwner(new NodeRef(storeRef, nodeId));
			logger.info(PermissionService.OWNER_AUTHORITY + " mapping on userId:" + ownableService);
		}

		return AuthenticationUtil.runAs(new HasPermissionsWork(permissionService, authority, permissions, nodeId), authority);
	}

	public boolean hasPermissions(String nodeId, String authority, String[] permissions) throws Exception {
		HashMap<String, Boolean> hasAllPermResult = this.hasAllPermissions(nodeId, authority, permissions);
		for (String permission : permissions) {
			Boolean tmpBool = hasAllPermResult.get(permission);
			if (tmpBool == null || tmpBool.booleanValue() == false) {
				return false;
			}
		}
		return true;
	}

	public boolean hasPermissions(String nodeId, String[] permissions) {
		boolean result = true;
		HashMap<String, Boolean> hasAllPerm = hasAllPermissions(nodeId, permissions);
		for (Map.Entry<String, Boolean> entry : hasAllPerm.entrySet()) {
			if (entry.getValue() == false) {
				result = false;
			}
		}
		return result;
	}
	
	public User getOwner(String storeId,String storeProtocol,String nodeId){
		String owner = this.serviceRegistry.getOwnableService().getOwner(new NodeRef(new StoreRef(storeProtocol,storeId), nodeId));
		User user = new User();
		user.setUsername(owner);
		NodeRef persNoderef = null;
		try {
			persNoderef = personService.getPerson(owner,false);
		} catch(NoSuchPersonException e) {
			//ie the system user
		}
		if(persNoderef != null){
			Map<QName,Serializable> props = nodeService.getProperties(persNoderef);
			user.setEmail((String)props.get(QName.createQName(CCConstants.CM_PROP_PERSON_EMAIL)));
			user.setGivenName((String)props.get(QName.createQName(CCConstants.CM_PROP_PERSON_FIRSTNAME)));
			user.setSurname(((String)props.get(QName.createQName(CCConstants.CM_PROP_PERSON_LASTNAME))));
			user.setNodeId(persNoderef.getId());
		}
		return user;
	}

	/**
	 * @return the resolveRemoteObjects
	 */
	public boolean isResolveRemoteObjects() {
		return resolveRemoteObjects;
	}

	/**
	 * @param resolveRemoteObjects
	 *            the resolveRemoteObjects to set
	 */
	public void setResolveRemoteObjects(boolean resolveRemoteObjects) {
		this.resolveRemoteObjects = resolveRemoteObjects;
	}

	public void setProperty(String nodeId, String property, Serializable value) {
		this.nodeService.setProperty(new NodeRef(storeRef, nodeId), QName.createQName(property), value);
	}
	
	public void setProperty(String nodeId, String property, ArrayList<String> value) {
		
		if(CCConstants.CCM_PROP_EDUSCOPE_NAME.equals(property)){
			
			boolean isSystemUser = false;
			
			String user = (String)AuthenticationUtil.getFullyAuthenticatedUser();
			if("admin".equals(user)) {
				isSystemUser = true;
			}
			
			if(AuthenticationUtil.isRunAsUserTheSystemUser()){
				isSystemUser = true;
			}
			
			if(!isSystemUser){
				throw new RuntimeException("it's not allowed to change the scope");
			}
		}
		
		this.nodeService.setProperty(new NodeRef(storeRef, nodeId), QName.createQName(property), value);
	}

	@Override
	public MCBaseClient getInstance(HashMap<String, String> authenticationInfo) {
		return new MCAlfrescoAPIClient(authenticationInfo);
	}

	@Override
	public MCBaseClient getInstance(String repositoryFile, HashMap<String, String> authenticationInfo) {
		return new MCAlfrescoAPIClient(repositoryFile, authenticationInfo);
	}

	public String newBasket(String _basketName) throws Throwable {
		String basketsFolderID = this.getFavoritesFolder();
		HashMap properties = new HashMap();
		properties.put(CCConstants.CM_PROP_C_TITLE, _basketName);
		properties.put(CCConstants.CM_NAME, _basketName);
		return this.createNode(basketsFolderID, CCConstants.CCM_TYPE_MAP, properties);
	}

	/**
	 * was a basket not created within a favorite folder and just created thru 
	 * createChildAssociation so the favorite folder is not the primary parent
	 * and the basket will just get removed from the favorite folder
	 * 
	 * @param basketID
	 * @param authenticationInfo
	 * @return
	 * @throws org.apache.axis.AxisFault
	 * @throws RemoteException
	 */
	public boolean removeBasket(String basketID) throws Throwable {
		String fromID = getFavoritesFolder();
		/**
		 * just to remove node is not enough, because the children get deleted in cascade 
		 */
		removeNodeAndRelations(basketID, fromID);
		return true;
	}

	public boolean createChildAssociation(String folderId, String nodeId) {
		nodeService.addChild(new NodeRef(storeRef, folderId), new NodeRef(storeRef, nodeId), ContentModel.ASSOC_CONTAINS, ContentModel.ASSOC_CONTAINS);
		return true;
	}

	@Override
	public void createShare(String nodeId, String[] emails, long expiryDate) throws Exception {
		ShareService shareService = new ShareServiceImpl();
		String locale = (String) Context.getCurrentInstance().getRequest().getSession().getAttribute(CCConstants.AUTH_LOCALE);
		shareService.createShare(nodeId, emails, expiryDate, null, locale);
	}

	public Share[] getShares(String nodeId) {
		ShareService shareService = new ShareServiceImpl();
		return shareService.getShares(nodeId);
	}

	public boolean removeChildAssociation(String folderId, String nodeId) {
		nodeService.removeChild(new NodeRef(storeRef, folderId), new NodeRef(storeRef, nodeId));
		return true;
	}

	public HashMap<String, HashMap> getParents(String nodeID, boolean primary) throws Throwable {
		// nodeService.getP
		HashMap<String, HashMap> result = new HashMap<String, HashMap>();
		NodeRef nodeRef = new NodeRef(storeRef, nodeID);
		if (primary) {
			ChildAssociationRef cAR = nodeService.getPrimaryParent(nodeRef);
			
			logger.info("cAR:" + cAR);
			logger.info("cAR getChildRef:" + cAR.getChildRef().getId());
			logger.info("cAR getParentRef:" + cAR.getParentRef().getId());

			result.put(cAR.getParentRef().getId(), this.getProperties(cAR.getParentRef()));
		} else {
			List<ChildAssociationRef> parents = nodeService.getParentAssocs(nodeRef);
			try {
				for (ChildAssociationRef parent : parents) {
					String parentNodeId = parent.getParentRef().getId();
					HashMap parentProps = getProperties(parent.getParentRef());
					result.put(parentNodeId, parentProps);
				}
			} catch (AccessDeniedException e) {
				logger.info("access denied error while getting parents for:" + nodeID + " will continue with the next one");
			}
		}
		return result;
	}
	
	public ChildAssociationRef getParent(NodeRef nodeRef){
		return nodeService.getPrimaryParent(nodeRef);
	}

	public ACL getPermissions(String nodeId) throws Exception {

			return serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(
				
                new RetryingTransactionCallback<ACL>()
                {
                    public ACL execute() throws Throwable
                    {
					PermissionService permissionsService = serviceRegistry.getPermissionService();
			
					NodeRef nodeRef = new NodeRef(storeRef, nodeId);
					Set<AccessPermission> permSet = permissionsService.getAllSetPermissions(nodeRef);
					Iterator<AccessPermission> iter = permSet.iterator();
			
					boolean isInherited = false;
			
					ACL result = new ACL();
					ArrayList<ACE> aces = new ArrayList<ACE>();
					
			
					while (iter.hasNext()) {
						AccessPermission ace = iter.next();
						String alfAuthority = ace.getAuthority();
						
						String authority = alfAuthority;
						
						authority = (authority == null) ? authority = ace.getAuthority() : authority;
						String permission = ace.getPermission();
						ACE aceResult = new ACE();
						aceResult.setAuthority(authority);
						aceResult.setPermission(permission);
			
						aceResult.setInherited(ace.isInherited());
			
						// to be compatible with WS API where positiv access status is called "acepted"
						// in GUI we compare with "acepted"
						String accessStatus = ace.getAccessStatus().name();
						if (accessStatus.trim().equals("ALLOWED")) {
							accessStatus = "acepted";
						}
			
						aceResult.setAccessStatus(accessStatus);
						aceResult.setAuthorityType(AuthorityType.getAuthorityType(alfAuthority).name());
			
						if (AuthorityType.getAuthorityType(alfAuthority).equals(AuthorityType.USER) || 
								AuthorityType.getAuthorityType(alfAuthority).equals(AuthorityType.OWNER)) {
							
							NodeRef personNodeRef = null;
							if(AuthorityType.getAuthorityType(alfAuthority).equals(AuthorityType.OWNER)){
								personNodeRef = personService.getPerson(serviceRegistry.getOwnableService().getOwner(nodeRef));
							}else{
								personNodeRef = personService.getPerson(alfAuthority);
							}
							
							Map<QName, Serializable> personProps = nodeService.getProperties(personNodeRef);
							User user = new User();
							user.setNodeId(personNodeRef.getId());
							user.setEmail((String) personProps.get(ContentModel.PROP_EMAIL));
							user.setGivenName((String) personProps.get(ContentModel.PROP_FIRSTNAME));
							user.setSurname((String) personProps.get(ContentModel.PROP_LASTNAME));
							
							String repository = (String)personProps.get(QName.createQName(CCConstants.PROP_USER_REPOSITORYID));
							if(repository == null || repository.trim().equals("")) repository = appInfo.getAppId();
							user.setRepositoryId(repository);
							user.setUsername((String) personProps.get(ContentModel.PROP_USERNAME));
							aceResult.setUser(user);
						}
						
			
						if (AuthorityType.getAuthorityType(alfAuthority).equals(AuthorityType.GROUP)) {
							NodeRef groupNodeRef = serviceRegistry.getAuthorityService().getAuthorityNodeRef(alfAuthority);
							if(groupNodeRef == null) {
								logger.warn("authority " + alfAuthority + " does not exist." + " will continue");
								continue;
							}
							
							Map<QName, Serializable> groupProps = nodeService.getProperties(groupNodeRef);
							Group group = new Group();
							group.setName(alfAuthority);
							group.setDisplayName((String) groupProps.get(ContentModel.PROP_AUTHORITY_DISPLAY_NAME));
							group.setNodeId(groupNodeRef.getId());
							group.setRepositoryId(appInfo.getAppId());
							group.setAuthorityType(AuthorityType.getAuthorityType(alfAuthority).name());
							group.setScope((String)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_SCOPE_TYPE)));
							
							NodeRef authorityNodeRef = authorityService.getAuthorityNodeRef(alfAuthority);
							if(authorityNodeRef != null) {
								String groupType = (String)nodeService.getProperty(authorityNodeRef,  QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE));
								if(groupType != null) {
									group.setGroupType(groupType);
									
									if(CCConstants.ADMINISTRATORS_GROUP_TYPE.equals(groupType) 
											&& permission.equals(PermissionService.COORDINATOR)) {
										
										if(isSharedNode(nodeId)) {
											group.setEditable(false);
										}else{
											group.setEditable(true);
										}
										
									}
								}
							}
							aceResult.setGroup(group);
						}
			
						logger.debug("authority" + authority + " Permission:" + permission + " ACCESSSTATUS:" + aceResult.getAccessStatus() + "isInherited:"
								+ ace.isInherited() + " getInheritParentPermissions(nodeRef):" + permissionsService.getInheritParentPermissions(nodeRef));
			
						aces.add(aceResult);
					}
					
					result.setAces(aces.toArray(new ACE[aces.size()]));
			
					logger.debug("permissionsService.getInheritParentPermissions(nodeRef):" + permissionsService.getInheritParentPermissions(nodeRef));
					isInherited = permissionsService.getInheritParentPermissions(nodeRef);
			
					result.setInherited(isInherited);
		     		return result;
                    	
				    }
				}, false); 
	}
	
	/** true if this node is in a shared context ("My shared files"), false if it's in users home
	 * 
	 * @param nodeId
	 * @return
	 * @throws Throwable 
	 */
	private boolean isSharedNode(String nodeId) {
		try {
			
			String groupFolderId = getGroupFolderId(AuthenticationUtil.getFullyAuthenticatedUser());
			List<String> sharedFolderIds=new ArrayList<>();
	
			if (groupFolderId != null) {
				HashMap<String, HashMap<String, Object>> children = getChildren(groupFolderId);
				for (Object key : children.keySet()) {
					sharedFolderIds.add(key.toString());
				}				
			}
			if(sharedFolderIds.size()==0) return false;
			
			NodeRef last=new NodeRef(storeRef,nodeId);
			while(true) {
	    			if(last==null) break;
	    			if(sharedFolderIds.contains(last.getId())) return true;
	    			last=getParent(last).getParentRef();
	    		}
			
		} catch(Throwable t){
			logger.warn(t.getMessage());
		}
		return false;
	}

	/**
	 * returns admin authority if context is an edugroup
	 * @param nodeRef
	 * @return
	 */
	String getAdminAuthority(NodeRef nodeRef) {
		String authorityAdministrator = null;
		if(isSharedNode(nodeRef.getId())){
			Set<AccessPermission> allSetPermissions = serviceRegistry.getPermissionService().getAllSetPermissions(nodeRef);
			for(AccessPermission ap : allSetPermissions) {
				NodeRef authorityNodeRef = authorityService.getAuthorityNodeRef(ap.getAuthority());
				if(authorityNodeRef != null) {
					String groupType = (String)nodeService.getProperty(authorityNodeRef, QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE));
					if(groupType != null && CCConstants.ADMINISTRATORS_GROUP_TYPE.equals(groupType) 
							&& ap.getPermission().equals(PermissionService.COORDINATOR)) {
						authorityAdministrator = ap.getAuthority();
					}
				}
			}
		}
		return authorityAdministrator;
	}
	
	public void removeRelations(String parentID) throws Exception {
		NodeRef parentNodeRef = new NodeRef(storeRef, parentID);
		List<ChildAssociationRef> childAssocList = nodeService.getChildAssocs(parentNodeRef);
		for (ChildAssociationRef child : childAssocList) {
			String childType = nodeService.getType(child.getChildRef()).toString();
			if (childType.equals(CCConstants.CCM_TYPE_MAPRELATION)) {
				this.removeNode(child.getChildRef().getId(), parentID);
			}
		}
	}

	public void removeRelationsForNode(String nodeId, String nodeParentId) throws Throwable {
		
		HashMap<String, HashMap<String, Object>> childrenRelation = getChildren(nodeParentId, CCConstants.CCM_TYPE_MAPRELATION);

		for (String relationNodeId : childrenRelation.keySet()) {
			HashMap<String, Object> props = childrenRelation.get(relationNodeId);
			for (String propKey : props.keySet()) {

				if (propKey.equals(CCConstants.CCM_ASSOC_RELSOURCE) || propKey.equals(CCConstants.CCM_ASSOC_RELTARGET)) {

					String relToNodeId = (String) props.get(propKey);
					if (relToNodeId.equals(nodeId)) {
						removeNode(relationNodeId, nodeParentId);
					}
				}
			}
		}
	}
	/**
	 * 
	 * @param parentID
	 * @param childID
	 * @param association (is ignored)
	 *       removes all associations between two nodes
	 */
	public void removeChild(String parentID, String childID, String association) {
		nodeService.removeChild(new NodeRef(storeRef, parentID), new NodeRef(storeRef, childID));
	}

	public String dropToBasketRemoteNode(String basketId, HashMap<String, String> params) throws Exception {
		String result = createRemoteNode(basketId, params);
		return result;
	}

	public String createRemoteNode(String parentId, HashMap<String, String> params) throws Exception {
		
		String result = null;
		String remoteNodeId = params.get(CCConstants.NODEID);
		String remoteRepository = params.get(CCConstants.REPOSITORY_ID);
		String remoteRepositoryType = params.get(CCConstants.REPOSITORY_TYPE);
		
		if (remoteNodeId != null && remoteNodeId.trim().length() > 0 && remoteRepository != null && remoteRepository.trim().length() > 0) {
			HashMap properties = new HashMap();
			properties.put(CCConstants.CCM_PROP_REMOTEOBJECT_NODEID, remoteNodeId);
			properties.put(CCConstants.CCM_PROP_REMOTEOBJECT_REPOSITORYID, remoteRepository);

			if (remoteRepositoryType == null || remoteRepositoryType.trim().equals("")) {
				ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(remoteRepository);
				remoteRepositoryType = appInfo.getRepositoryType();
			}

			properties.put(CCConstants.CCM_PROP_REMOTEOBJECT_REPOSITORY_TYPE, remoteRepositoryType);

			result = this.createNode(parentId, CCConstants.CCM_TYPE_REMOTEOBJECT, properties);

		} else {
			logger.error("missing remote NodeId or remoteRepository");
		}
		return result;
	}

	public void moveNode(String newParentId, String childAssocType, String nodeId) throws Exception {
		String originalName =
				(String) nodeService.getProperty(
						new NodeRef(storeRef, nodeId),
						QName.createQName(CCConstants.CM_NAME));
		for(int i=0;;i++) {
			String name=originalName;
			if(i>0) {
				name = NodeServiceHelper.renameNode(name, i);
				nodeService.setProperty(new NodeRef(storeRef, nodeId),
						QName.createQName(CCConstants.CM_NAME),name);
			}
			try {
				nodeService.moveNode(
						new NodeRef(storeRef, nodeId),
						new NodeRef(storeRef, newParentId),
						QName.createQName(childAssocType),
						QName.createQName(CCConstants.NAMESPACE_CCM, name));

				// remove from cache so that the new primary parent will be refreshed
				Cache repCache = new RepositoryCache();
				repCache.remove(nodeId);
				break;
			}catch(DuplicateChildNodeNameException e){
				// let the loop run
			}
		}
	}

	/**
	 * @param nodeId : the id of the node to copy
	 * @param toNodeId : the id of the target folder
	 * @param copyChildren
	 */
	public String copyNode(String nodeId, String toNodeId, boolean copyChildren) throws Exception {
		NodeRef nodeRef = new NodeRef(MCAlfrescoAPIClient.storeRef, nodeId);

		CopyService copyService = serviceRegistry.getCopyService();

		String name = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
		NodeRef copyNodeRef = copyService.copyAndRename(nodeRef, new NodeRef(MCAlfrescoAPIClient.storeRef, toNodeId), QName.createQName(CCConstants.CM_ASSOC_FOLDER_CONTAINS),
				QName.createQName(name), copyChildren);
	
		return copyNodeRef.getId();
	}
	
	public String copyNode(String nodeId, String toNodeId, String assocType, String assocName, boolean copyChildren) throws Exception {
		NodeRef nodeRef = new NodeRef(MCAlfrescoAPIClient.storeRef, nodeId);

		CopyService copyService = serviceRegistry.getCopyService();

		NodeRef copyNodeRef = copyService.copy(nodeRef, new NodeRef(MCAlfrescoAPIClient.storeRef, toNodeId),QName.createQName(assocType),
				QName.createQName(assocName), copyChildren);
		
		return copyNodeRef.getId();
	}
	
	/**
	 * walk through all parents until you find a folder that is used as
	 * edugrouphomedir of a edugroup return the Group
	 * 
	 * @param nodeId
	 * @return
	 * @throws CCException
	 */
	public Group getEduGroupContextOfNode(String nodeId) {

		NodeRef result = null;

		NodeRef nodeRef = new NodeRef(storeRef, nodeId);
		QName nodeType = null;
		QName mapType = QName.createQName(CCConstants.CCM_TYPE_MAP);

		Collection<NodeRef> eduGroupNodeRefs = new VirtualEduGroupFolderTool(serviceRegistry, nodeService).getEduGroupNodeRefs();

		// nodeRefEduGroupFolder , noderefEduGroup
		Map<NodeRef, NodeRef> eduGroupEduGroupFolderMap = new HashMap<NodeRef, NodeRef>();
		for (NodeRef eduGroupNodeRef : eduGroupNodeRefs) {
			eduGroupEduGroupFolderMap.put((NodeRef) nodeService.getProperty(eduGroupNodeRef, QName.createQName(CCConstants.CCM_PROP_EDUGROUP_EDU_HOMEDIR)),
					eduGroupNodeRef);
		}

		Group group = null;
		try {
			do {
				ChildAssociationRef parentAssocRef = nodeService.getPrimaryParent(nodeRef);
				nodeRef = (parentAssocRef == null) ? null : parentAssocRef.getParentRef();
				if (nodeRef != null) {
					nodeType = nodeService.getType(nodeRef);
				}

				NodeRef groupNodeRef = eduGroupEduGroupFolderMap.get(nodeRef);
				if ((groupNodeRef != null)) {
					result = groupNodeRef;

				}

			} while (nodeRef != null && mapType.equals(nodeType) && result == null);

			if (result != null) {
				group = new Group();
				String authorityName = (String) nodeService.getProperty(result, ContentModel.PROP_AUTHORITY_NAME);
				group.setName(authorityName);
				group.setDisplayName((String) nodeService.getProperty(result, ContentModel.PROP_AUTHORITY_DISPLAY_NAME));
				group.setRepositoryId(appInfo.getAppId());
				group.setNodeId(result.getId());
				group.setAuthorityType(AuthorityType.getAuthorityType(group.getName()).name());
				group.setScope((String)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_SCOPE_TYPE)));
				NodeRef authorityNodeRef = authorityService.getAuthorityNodeRef(authorityName);
				if(authorityNodeRef != null) {
					String groupType = (String)nodeService.getProperty(authorityNodeRef,  QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE));
					if(groupType != null) {
						group.setGroupType(groupType);
					}
				}
			}
		} catch (org.alfresco.repo.security.permissions.AccessDeniedException e) {
			// maybe while doing nodeService.getPrimaryParent(nodeRef); and
			// landing in an folder where i have no read permissions
			logger.debug(e.getMessage());
		}
		
		return group;
	}

	public HashMap<String, String> checkAndCreateShadowUser(String username, String email, String repId) throws Exception {
		throw new Exception("checkAndCreateShadowUser is not implemented!");
	}

	public HashMap<String, HashMap<String,Object>> getVersionHistory(String nodeId) throws Throwable {
		VersionService versionService = serviceRegistry.getVersionService();
		VersionHistory versionHistory = versionService.getVersionHistory(new NodeRef(storeRef, nodeId));
		HashMap<String, HashMap<String,Object>> result = null;
		if (versionHistory != null && versionHistory.getAllVersions() != null && versionHistory.getAllVersions().size() > 0) {
			result = new HashMap<String, HashMap<String,Object>>();
			Collection<Version> versions = versionHistory.getAllVersions();

			for (Version version : versions) {

				HashMap<String,Object> props = getPropertiesSimple(version.getFrozenStateNodeRef().getStoreRef(), version.getFrozenStateNodeRef().getId());

				logger.debug(" version prop UID:" + props.get(CCConstants.SYS_PROP_NODE_UID));
				logger.debug(" version NodeID:" + props.get(CCConstants.NODEID));

				props.put(CCConstants.ALFRESCO_MIMETYPE, getAlfrescoMimetype(version.getFrozenStateNodeRef()));
				// contenturl
				String contentUrl = URLTool.getNgRenderNodeUrl(nodeId,version.getVersionLabel());
				contentUrl = URLTool.addOAuthAccessToken(contentUrl);
				
				props.put(CCConstants.CONTENTURL, contentUrl);
				if (props.get(CCConstants.ALFRESCO_MIMETYPE) != null && contentUrl != null) {
					props.put(CCConstants.DOWNLOADURL, URLTool.getDownloadServletUrl(nodeId,version.getVersionLabel(), true));
				}

				// thumbnail take the current thumbnail cause subobjects
				// (thumbnail will be removed by versioning)
				setPreviewUrlWithoutTicket(storeRef, nodeId, props);
				String thumbnailUrl = (String) props.get(CCConstants.CM_ASSOC_THUMBNAILS);
				if (thumbnailUrl != null && !thumbnailUrl.trim().equals("")) {
					// prevent Browser Caching
					thumbnailUrl = UrlTool.setParam(thumbnailUrl, "dontcache", new Long(System.currentTimeMillis()).toString());
					props.put(CCConstants.CM_ASSOC_THUMBNAILS, thumbnailUrl);
				}

				// version store NodeId
				props.put(CCConstants.VERSION_STORE_NODEID, version.getFrozenStateNodeRef().getId());

				// versionLabel
				props.put(CCConstants.CM_PROP_VERSIONABLELABEL, version.getVersionLabel());

				/* add permalink */
				String v = version.getVersionLabel();
				String permaLink = URLTool.getBaseUrl() + "/node/" + nodeId;
				permaLink = (v != null) ? permaLink + "/" + v : permaLink;
				props.put(CCConstants.VIRT_PROP_PERMALINK, permaLink);

				result.put(version.getFrozenStateNodeRef().getId(), props);
			}
		}
		return result;
	}

	public void revertVersion(String nodeId, String verLbl) throws Exception {
		VersionService versionService = serviceRegistry.getVersionService();
		VersionHistory versionHistory = versionService.getVersionHistory(new NodeRef(storeRef, nodeId));
		if (versionHistory != null && versionHistory.getAllVersions() != null && versionHistory.getAllVersions().size() > 0) {
			NodeRef ioNodeRef = new NodeRef(storeRef, nodeId);
			BehaviourFilter behaviourFilter = (BehaviourFilter) applicationContext.getBean("policyBehaviourFilter");

			UserTransaction userTransaction = serviceRegistry.getTransactionService().getNonPropagatingUserTransaction();

			try {
				
				userTransaction.begin();
				
				try {

					behaviourFilter.disableBehaviour(ioNodeRef, QName.createQName(CCConstants.CCM_TYPE_IO));
					Version version = versionHistory.getVersion(verLbl);
					versionService.revert(ioNodeRef, version, true);

				} finally {
					behaviourFilter.enableBehaviour(ioNodeRef, QName.createQName(CCConstants.CCM_TYPE_IO));
				}
				
				userTransaction.commit();
				
			} catch (Throwable e) {
				
				logger.error(e.getMessage()+" rolling back",e);
				userTransaction.rollback();
				throw new Exception("revert version failed cause of" + e.getMessage());			}

		}
	}

	public boolean isAdmin(String username) throws Exception {
		try {
			Set<String> testUsetAuthorities = serviceRegistry.getAuthorityService().getAuthoritiesForUser(username);
			for (String testAuth : testUsetAuthorities) {

				if (testAuth.equals("GROUP_ALFRESCO_ADMINISTRATORS")) {
					return true;
				}
			}
		} catch (org.alfresco.repo.security.permissions.AccessDeniedException e) {
			logger.debug(username + " is no admin!!!");
		}
		return false;
	}

	/**
	 * take the current runAs alfresco user and check if it is an admin normally
	 * runas = the fully authenticated user only when
	 * AuthenticationUtil.RunAsWork<Result> it differs
	 * 
	 * @return
	 * @throws Exception
	 */
	public boolean isAdmin() throws Exception {

		String username = AuthenticationUtil.getRunAsUser();
		return isAdmin(username);
	}

	public String getAlfrescoContentUrl(String nodeId) throws Exception {
		return URLTool.getBrowserURL(new NodeRef(storeRef, nodeId));
	}

	public void setPreviewUrlWithoutTicket(StoreRef storeRef, String nodeId, HashMap<String, Object> properties) {
		try {
			properties.put(CCConstants.CM_ASSOC_THUMBNAILS, NodeServiceHelper.getPreview(new NodeRef(storeRef,nodeId)).getUrl());
			// @todo 5.1: Check if this is needed in the client
			/*
			GetPreviewResult prevResult = getPreviewUrl(storeRef, nodeId);

			if (prevResult.getType().equals(GetPreviewResult.TYPE_USERDEFINED)) {
				properties.put(CCConstants.CM_ASSOC_THUMBNAILS, prevResult.getUrl());
				properties.put(CCConstants.KEY_PREVIEWTYPE, GetPreviewResult.TYPE_USERDEFINED);
			}

			if (prevResult.getType().equals(GetPreviewResult.TYPE_GENERATED)) {
				properties.put(CCConstants.CM_ASSOC_THUMBNAILS, prevResult.getUrl());
				properties.put(CCConstants.KEY_PREVIEWTYPE, GetPreviewResult.TYPE_GENERATED);
			}

			if (prevResult.isCreateActionRunning()) {
				properties.put(CCConstants.KEY_PREVIEW_GENERATION_RUNS, "true");
			} else {
				properties.remove(CCConstants.KEY_PREVIEW_GENERATION_RUNS);
			}
			*/
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	public String checkSystemFolderAndReturn(String foldername) throws Exception {
		String systemFolderRootId = getCompanyHomeNodeId();

		HashMap<String, Object> systemFolderProps = getChild(systemFolderRootId, CCConstants.CM_TYPE_FOLDER, CCConstants.CM_NAME, foldername);
		if (systemFolderProps == null || systemFolderProps.size() == 0) {
			HashMap newSystemFolderProps = new HashMap();
			newSystemFolderProps.put(CCConstants.CM_NAME, foldername);
			newSystemFolderProps.put(CCConstants.CM_PROP_C_TITLE, foldername);
			String newRemoteFolderId = createNode(systemFolderRootId, CCConstants.CM_TYPE_FOLDER, newSystemFolderProps);
			return newRemoteFolderId;
		} else {
			return (String) systemFolderProps.get(CCConstants.SYS_PROP_NODE_UID);
		}
	}

	public String getCompanyHomeNodeId() {
		return repositoryHelper.getCompanyHome().getId();
	}

	public void endSession(String user, String ticket) {
		serviceRegistry.getAuthenticationService().invalidateTicket(ticket);
	}

	public void endSession() {
		serviceRegistry.getAuthenticationService().invalidateTicket(this.authenticationInfo.get(CCConstants.AUTH_TICKET));
		serviceRegistry.getAuthenticationService().clearCurrentSecurityContext();
	}

	@Override
	public boolean isSubOf(String type, String parentType) throws Throwable {
		boolean isSubOf = serviceRegistry.getDictionaryService().isSubClass(QName.createQName(type), QName.createQName(parentType));
		return isSubOf;
	}

	public String getType(String nodeId) {
		return nodeService.getType(new NodeRef(storeRef, nodeId)).toString();
	}

	private NodeRef getAuthority(String name) {
		SearchParameters sp = new SearchParameters();
		
		sp.addStore(storeRef);
		sp.setLanguage("lucene");
		sp.setQuery("+TYPE:\""
				+ ContentModel.TYPE_AUTHORITY_CONTAINER
				+ "\""
				+ " +@"
				+ QueryParser.escape("{" + ContentModel.PROP_AUTHORITY_NAME.getNamespaceURI() + "}"
						+ ISO9075.encode(ContentModel.PROP_AUTHORITY_NAME.getLocalName())) + ":\"" + name + "\"");
		ResultSet rs = null;
		try {
			rs = searchService.query(sp);
			if (rs.length() == 0) {
				return null;
			} else {
				for (ResultSetRow row : rs) {
					String test = DefaultTypeConverter.INSTANCE.convert(String.class,
							nodeService.getProperty(row.getNodeRef(), ContentModel.PROP_AUTHORITY_NAME));
					if (test.equals(name)) {
						return row.getNodeRef();
					}
				}
			}
			return null;
		} finally {
			if (rs != null) {
				rs.close();
			}
		}
	}

	private String getAuthorityName(String groupNodeId) {
		return DefaultTypeConverter.INSTANCE.convert(String.class,
				nodeService.getProperty(new NodeRef(storeRef, groupNodeId), ContentModel.PROP_AUTHORITY_NAME));
	}

	private void buildUpProperties(HashMap<String, Object> properties) {

		// Creators
		String creator = (String) properties.get(CCConstants.CM_PROP_C_CREATOR);
		if (creator != null) {
			NodeRef ref = null;

			try {
				ref = personService.getPerson(creator, false);
			} catch (org.alfresco.service.cmr.security.NoSuchPersonException e) {
				logger.debug("person " + creator + " does not longer exsists");
			}

			if (ref != null) {

				properties.put(CCConstants.NODECREATOR_FIRSTNAME, nodeService.getProperty(ref, ContentModel.PROP_FIRSTNAME));
				properties.put(CCConstants.NODECREATOR_LASTNAME, nodeService.getProperty(ref, ContentModel.PROP_LASTNAME));
				properties.put(CCConstants.NODECREATOR_EMAIL, nodeService.getProperty(ref, ContentModel.PROP_EMAIL));
			} else {
				properties.put(CCConstants.NODECREATOR_FIRSTNAME, "unknown");
				properties.put(CCConstants.NODECREATOR_LASTNAME, "unknown");
				properties.put(CCConstants.NODECREATOR_EMAIL, "unknown");
			}
		}

		// Modfifier
		String modifier = (String) properties.get(CCConstants.CM_PROP_C_MODIFIER);
		if (modifier != null) {
			NodeRef ref = null;

			try {
				ref = personService.getPerson(modifier, false);
			} catch (org.alfresco.service.cmr.security.NoSuchPersonException e) {
				logger.debug("person " + modifier + " does not longer exsists");
			}

			if (ref != null) {

				properties.put(CCConstants.NODEMODIFIER_FIRSTNAME, nodeService.getProperty(ref, ContentModel.PROP_FIRSTNAME));
				properties.put(CCConstants.NODEMODIFIER_LASTNAME, nodeService.getProperty(ref, ContentModel.PROP_LASTNAME));
				properties.put(CCConstants.NODEMODIFIER_EMAIL, nodeService.getProperty(ref, ContentModel.PROP_EMAIL));
			} else {
				properties.put(CCConstants.NODEMODIFIER_FIRSTNAME, "unknown");
				properties.put(CCConstants.NODEMODIFIER_LASTNAME, "unknown");
				properties.put(CCConstants.NODEMODIFIER_EMAIL, "unknown");
			}
		}

	}

	public String getDetailsHtmlSnippet(String nodeId) throws Exception {
		throw new Exception("notImplementedYet");
	}

	public void setLocale(String localeStr) {
		Locale locale = I18NUtil.parseLocale(localeStr);
		I18NUtil.setLocale(locale);
	}

	@Override
	public String getNodeType(String nodeId) {
		return this.nodeService.getType(new NodeRef(storeRef, nodeId)).toString();
	}

	public GetPreviewResult getPreviewUrl(StoreRef storeRef, String nodeId) {
		return getPreviewUrl(storeRef.getProtocol(), storeRef.getIdentifier(), nodeId);
	}
	
	public String getUrl(){
		ApplicationInfo homeRep = ApplicationInfoList.getHomeRepository();

		String server = homeRep.getDomain();
		server = (server == null) ? homeRep.getHost() : server;
		return homeRep.getClientprotocol() + "://" + server + ":" + homeRep.getClientport() + "/" + homeRep.getWebappname();	
	}
	
	public GetPreviewResult getPreviewUrl(String storeProtocol, String storeIdentifier, String nodeId){
		return NodeServiceHelper.getPreview(new NodeRef(storeRef, nodeId));
	}

	@Override
	public boolean isOwner(String nodeId, String user) {

		String owner = serviceRegistry.getOwnableService().getOwner(new NodeRef(MCAlfrescoAPIClient.storeRef, nodeId));
		if (owner.equals(user)) {
			return true;
		} else {
			return false;
		}
	}

	public String[] getMetadataSets() {
		try {

			File mdsDir = new File(MCAlfrescoAPIClient.class.getClassLoader().getResource("org/edu_sharing/metadataset").toURI());

			final FilenameFilter filter = new FilenameFilter() {

				public boolean accept(final File dir, final String name) {

					if (name.matches("metadataset_[a-zA-Z]*.xml")) {
						return true;
					} else {
						return false;
					}
				}
			};
			String[] filesFound = mdsDir.list(filter);

			List<String> mdsNames = new ArrayList<String>();
			for (String mdsFile : filesFound) {
				String name = mdsFile.replace("metadataset_", "");
				name = mdsFile.replace(".xml", "");
				mdsNames.add(name);
			}

			return mdsNames.toArray(new String[mdsNames.size()]);
		} catch (URISyntaxException e) {
			logger.error(e.getMessage(), e);
		}

		return null;

	}

	@Override
	public void setOwner(String nodeId, String username) {
		serviceRegistry.getOwnableService().setOwner(new NodeRef(storeRef, nodeId), username);
	}

	@Override
	public String guessMimetype(String filename) {
		return serviceRegistry.getMimetypeService().guessMimetype(filename);
	}

	public boolean exists(String nodeId) {
		return nodeService.exists(new NodeRef(storeRef, nodeId));
	}

	/**
	 * returns hash of content, if node has no content -1
	 * 
	 * @param nodeId
	 * @param property
	 * @return
	 */
	public int getContentHash(String nodeId, String property) {
		return getContentHash(nodeId, property, storeRef.getProtocol(),storeRef.getIdentifier());
	}
		
	public int getContentHash(String nodeId, String property, String storeProtocol,String storeIdentifier) {
		ContentReader reader = this.contentService.getReader(new NodeRef(new StoreRef(storeProtocol,storeIdentifier), nodeId), QName.createQName(property));
		if (reader == null) {
			return -1;
		} else {
			return reader.getContentData().hashCode();
		}
	}

	
	public String[] getAspects(String nodeId){
		return getAspects(new NodeRef(storeRef,nodeId));
	}
	
	public String[] getAspects(String storeProtocol, String storeId, String nodeId){
		return getAspects(new NodeRef(new StoreRef(storeProtocol,storeId),nodeId));
	}
	
	public String[] getAspects(NodeRef nodeRef){
		Set<QName> set = nodeService.getAspects(nodeRef);
		ArrayList<String> result = new ArrayList<String>();
		for(QName qname : set){
			result.add(qname.toString());
		}
		return result.toArray(new String[result.size()]);
	}
	
	public String findNodeByPath(String path) {
		
		return serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(
				
                new RetryingTransactionCallback<String>()
                {
                    public String execute() throws Throwable
                    {
            	        List<String> paths = null;
            	        if (path == null || path.length() == 0)
            	        {
            	            paths = Collections.emptyList();
            	            
            	        } else {

            	        	paths = new ArrayList<String>();
            	        	
	            	        StringTokenizer token = new StringTokenizer(path, "/");
	            	        while (token.hasMoreTokens())
	            	        {
	            	        	String s = token.nextToken().replaceAll("\\{[^}]*\\}", "");
	            	        	
	            	        	String[] t = s.split(":");
	            	        	if (t.length == 2) {
	            	        		s = t[1];
	            	        	}

	            	            paths.add(s);
	            	        }
            	        }
            	        
            	        NodeRef companyHome = repositoryHelper.getCompanyHome();
            	        
            	        if (0 < paths.size() && "company_home".equals(paths.get(0))) {
            	        	
            	        	paths.remove(0);
            	        }
            	        
            			return serviceRegistry.getFileFolderService().
            					resolveNamePath(companyHome, paths, true).getNodeRef().getId();
                    }
                }, true); 
		
	}
	
	public void bindEduGroupFolder(String groupName, String folderId) throws Exception {

		try{
		serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(
				
                new RetryingTransactionCallback<Void>()
                {
                    public Void execute() throws Throwable
                    {
                		if (isAdmin()) {
                			
                			eduOrganisationService.bindEduGroupFolder(groupName,new NodeRef(storeRef,folderId));
                		}else{
                			throw new Exception("No Permissions to bind edugroup");
                		}
                		
                		return null;
                    }
                }, false); 

		}catch(AlfrescoRuntimeException e){
			throw (Exception)e.getCause();
		}
	}
	
	public void unbindEduGroupFolder(String groupName, String folderId) throws Exception {

		serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(
				
                new RetryingTransactionCallback<Void>()
                {
                    public Void execute() throws Throwable
                    {
                		if (isAdmin()) {
                			
                			AuthorityService authorityService = serviceRegistry.getAuthorityService();
                			NodeRef authorityNodeRef = authorityService.getAuthorityNodeRef(PermissionService.GROUP_PREFIX + groupName);
                			
                			if (authorityNodeRef == null) {
                				return null;
                			}

                			NodeService nodeService = serviceRegistry.getNodeService();
                			NodeRef folderNodeRef = new NodeRef(storeRef, folderId);

                			if (! nodeService.exists(folderNodeRef)) {
                				return null;
                			}
                			
                			EduGroupTool.processEduGroupMicroCommand("COMMAND REMOVE " + authorityNodeRef.toString() + " " + folderNodeRef.toString() );
                		}
                		
                		return null;
                    }
                }, false); 

	}
	
	public InputStream getContent(String nodeId){
		return getContent(nodeId, CCConstants.CM_PROP_CONTENT);
	}
	
	public InputStream getContent(String nodeId, String contentProp){
		ContentReader reader = serviceRegistry.getContentService().getReader(new NodeRef(storeRef,nodeId), QName.createQName(contentProp));
		if(reader != null) return reader.getContentInputStream();
		else return null;
	}
 
	/**
	 * https://community.alfresco.com/thread/176342-read-document-content-doc-docx-odt
	 * @param nodeId
	 * @param mimetype e.g. MimetypeMap.MIMETYPE_TEXT_PLAIN
	 * @return
	 */
	public String getNodeTextContent(String nodeId,String mimetype) {
		ContentReader reader = contentService.getReader(new NodeRef(storeRef,nodeId), QName.createQName(CCConstants.CM_PROP_CONTENT));
        if (reader != null && reader.exists())
        {
                // get the transformer
                org.alfresco.repo.content.transform.ContentTransformer transformer = contentService.getTransformer(reader.getMimetype(), MimetypeMap.MIMETYPE_TEXT_PLAIN);
                
                // is this transformer good enough?
                if (transformer != null)
                {
                    // We have a transformer that is fast enough
                    ContentWriter writer = contentService.getTempWriter();
                    writer.setMimetype(mimetype);

                    try
                    {
                        transformer.transform(reader, writer);
                        // point the reader to the new-written content
                        reader = writer.getReader();
                        // Check that the reader is a view onto something concrete
                        if (!reader.exists())
                        {
                            throw new ContentIOException("The transformation did not write any content, yet: \n"
                                    + "   transformer:     " + transformer + "\n" + "   temp writer:     " + writer);
                        }
                        return reader.getContentString();
                    }
                    catch (ContentIOException e)
                    {
                    	throw e;
                    }
                }
                throw new ContentIOException("No transformer found for mimetype "+reader.getMimetype());

            }
        throw new ContentIOException("No reader found");
	}

	public NodeRef findFolderNodeRef(StoreRef storeRef, String folderXPath)
    {
        NodeRef storeRootNodeRef = nodeService.getRootNode(storeRef);
        
        List<NodeRef> nodeRefs = searchService.selectNodes(storeRootNodeRef, folderXPath, null, namespaceService, false);
        
        NodeRef folderNodeRef = null;
        if (nodeRefs.size() != 1)
        {
            throw new AlfrescoRuntimeException("Cannot find folder location: " + folderXPath);
        }
        else
        {
            folderNodeRef = nodeRefs.get(0);
        }
        return folderNodeRef;
    }
	
	 public NodeRef getUserHomesNodeRef(StoreRef storeRef)
	 {
	        // get the "User Homes" location
	        return findFolderNodeRef(storeRef, "/app:company_home/app:user_homes");
	 }
	
}
