package org.edu_sharing.restservices;

import java.io.InputStream;
import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.security.PermissionService;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.repository.server.tools.*;
import org.edu_sharing.service.permission.HandleMode;
import org.edu_sharing.alfresco.tools.EduSharingNodeHelper;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.MetadataQuery;
import org.edu_sharing.metadataset.v2.MetadataQueryParameter;
import org.edu_sharing.metadataset.v2.tools.MetadataSearchHelper;
import org.edu_sharing.metadataset.v2.MetadataReaderV2;
import org.edu_sharing.repository.client.rpc.Notify;
import org.edu_sharing.repository.client.rpc.Share;
import org.edu_sharing.repository.client.rpc.User;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.metadata.ValueTool;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.tools.cache.PreviewCache;
import org.edu_sharing.restservices.collection.v1.model.Collection;
import org.edu_sharing.restservices.collection.v1.model.CollectionReference;
import org.edu_sharing.restservices.node.v1.model.NodeEntries;
import org.edu_sharing.restservices.node.v1.model.NodeShare;
import org.edu_sharing.restservices.node.v1.model.NotifyEntry;
import org.edu_sharing.restservices.node.v1.model.WorkflowHistory;
import org.edu_sharing.restservices.shared.*;
import org.edu_sharing.restservices.shared.NodeRef;
import org.edu_sharing.restservices.shared.NodeSearch.Facette;
import org.edu_sharing.restservices.shared.NodeSearch.Facette.Value;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.comment.CommentService;
import org.edu_sharing.service.license.LicenseService;
import org.edu_sharing.service.mime.MimeTypesV2;
import org.edu_sharing.service.nodeservice.*;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.notification.NotificationServiceFactory;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.permission.PermissionServiceHelper;
import org.edu_sharing.service.rating.RatingDetails;
import org.edu_sharing.service.rating.RatingsCache;
import org.edu_sharing.service.rating.RatingServiceFactory;
import org.edu_sharing.service.remote.RemoteObjectService;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.search.model.SharedToMeType;
import org.edu_sharing.service.search.model.SortDefinition;
import org.edu_sharing.service.share.ShareService;
import org.edu_sharing.service.share.ShareServiceImpl;
import org.json.JSONArray;
import org.json.JSONObject;

import io.swagger.util.Json;
import org.springframework.context.ApplicationContext;

import static org.codehaus.groovy.runtime.DefaultGroovyMethods.collect;

public class NodeDao {
	private static Logger logger = Logger.getLogger(NodeDao.class);
	private static final StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
	private static final String[] DAO_PERMISSIONS = new String[]{
			org.alfresco.service.cmr.security.PermissionService.READ,
			org.alfresco.service.cmr.security.PermissionService.ADD_CHILDREN,
			org.alfresco.service.cmr.security.PermissionService.CHANGE_PERMISSIONS,
			org.alfresco.service.cmr.security.PermissionService.WRITE,
			org.alfresco.service.cmr.security.PermissionService.DELETE,
			CCConstants.PERMISSION_COMMENT,
			CCConstants.PERMISSION_FEEDBACK,
			CCConstants.PERMISSION_CC_PUBLISH,
			CCConstants.PERMISSION_READ_ALL
	};
	final List<String> access;
	private final org.edu_sharing.service.model.NodeRef.Preview previewData;
	/*
	whether this node dao is supposed to fetch collection counts (more expensive when true)
	 */
	boolean fetchCounts = true;
	// id of the object by the remote repository (null if not a remote object)
	private String remoteId;
	private RepositoryDao remoteRepository;

	private String version;

	public static NodeDao getNodeWithVersion(RepositoryDao repoDao, String nodeId,String versionLabel) throws DAOException {
		if(versionLabel!=null && versionLabel.equals("-1"))
			versionLabel = null;
		NodeDao nodeDao=getNode(repoDao,nodeId,Filter.createShowAllFilter());

		// published nodes don't have version histories!
		if(nodeDao.isPublishedCopy()){
			versionLabel = null;
		}
		if(versionLabel!=null) {
			nodeDao.version = versionLabel;
			nodeDao.nodeProps = nodeDao.getNodeHistory().get(nodeDao.version);
			if(nodeDao.nodeProps==null)
				throw new DAOMissingException(new Exception("Node "+nodeId+" does not have this version: "+versionLabel));
		}
		return nodeDao;
	}
	public static NodeDao getNode(RepositoryDao repoDao, String nodeId)
			throws DAOException {
		return getNode(repoDao, nodeId, new Filter());
	}
	public static NodeDao getNode(RepositoryDao repoDao, org.edu_sharing.service.model.NodeRef nodeRef)
			throws DAOException {
		return new NodeDao(repoDao, nodeRef, Filter.createShowAllFilter());
	}

	/** get node via shared link **/
	public static NodeDao getNode(RepositoryDao repoDao, String nodeId,String token)
			throws DAOException {
		return AuthenticationUtil.runAsSystem(new RunAsWork<NodeDao>() {			
			@Override
			public NodeDao doWork() throws Exception {		
				ShareServiceImpl service=new ShareServiceImpl();
				Share share=service.getShare(nodeId, token);
				if(share==null){
					throw new Exception("No share found for nodeId and token");
				}
				if (share.getExpiryDate() != ShareService.EXPIRY_DATE_UNLIMITED) {
					if (new Date(System.currentTimeMillis()).after(new Date(share.getExpiryDate()))) {
						throw new Exception("Share expired");
					}
				}
				share.setDownloadCount((share.getDownloadCount() + 1) );
				service.updateShare(share);
				
				return getNode(repoDao, nodeId, new Filter());
			}
		});
	}
	
	public static NodeDao getNode(RepositoryDao repoDao, String nodeId, Filter filter)
			throws DAOException {

		try {
			if(filter == null) filter = new Filter();
			return NodeDao.getNode(repoDao, null, null, nodeId, filter);
		} catch (Throwable t) {
			throw DAOException.mapping(t);
		}
	}
	
	public static NodeDao getNode(RepositoryDao repoDao, String storeProtocol, String storeId,  String nodeId, Filter filter)
			throws DAOException {
		try {
			
			return new NodeDao(repoDao,storeProtocol,storeId, nodeId, filter);
			
		} catch (Throwable t) {
			
			throw DAOException.mapping(t);
		}
	}
	
	public static NodeSearch search(RepositoryDao repoDao,
			org.edu_sharing.service.search.model.SearchToken searchToken) throws DAOException {
		SearchService searchService=SearchServiceFactory.getSearchService(repoDao.getId());
		return transform(repoDao,searchService.search(searchToken));
	}
	
	public static NodeSearch search(RepositoryDao repoDao,
			org.edu_sharing.service.search.model.SearchToken searchToken, boolean scoped) throws DAOException {
		SearchService searchService=SearchServiceFactory.getSearchService(repoDao.getId());
		return transform(repoDao,searchService.search(searchToken,scoped));
	}
	
	public static NodeSearch searchV2(RepositoryDao repoDao,MdsDaoV2 mdsDao,
			String query, List<MdsQueryCriteria> criterias,SearchToken token, Filter filter) throws DAOException {
		SearchService searchService=SearchServiceFactory.getSearchService(repoDao.getId());
		Map<String,String[]> criteriasMap = MetadataSearchHelper.convertCriterias(criterias);
		try {
			NodeSearch result = transform(repoDao,searchService.searchV2(mdsDao.getMds(),query,criteriasMap,token),filter);
			if(result.getCount()==0) {
				// try to search for ignorable properties to be null
				List<String> removed=slackCriteriasMap(criteriasMap,mdsDao.getMds().findQuery(query, MetadataReaderV2.QUERY_SYNTAX_LUCENE));
				result=transform(repoDao,searchService.searchV2(mdsDao.getMds(),query,criteriasMap,token),filter);
				result.setIgnored(removed);
				return result;
			}
			return result;
		} catch (Throwable e) {
			throw DAOException.mapping(e);
		}
	}
	
	private static List<String> slackCriteriasMap(Map<String, String[]> criteriasMap, MetadataQuery metadataQuery) {
		List<String> removed=new ArrayList<>();
		for(MetadataQueryParameter param : metadataQuery.getParameters()){
			if(param.getIgnorable()>0 && criteriasMap.containsKey(param.getName())) {
				criteriasMap.put(param.getName(),null);
				removed.add(param.getName());
			}
		}
		return removed;
	}

	public static NodeSearch searchFingerprint(RepositoryDao repoDao, String nodeId, Filter filter) throws DAOException {
		SearchService searchService=SearchServiceFactory.getSearchService(repoDao.getId());
		try {
			return transform(repoDao,searchService.searchFingerPrint(nodeId),filter);
		} catch (Throwable e) {
			throw DAOException.mapping(e);
		}
	}

	public static NodeSearch search(RepositoryDao repoDao, String query,
			int startIdx, int nrOfresults, List<String> facettes,
			int facettesMinCount, int facettesLimit) throws DAOException {

		try {
			
			SearchResultNodeRef search = ((MCAlfrescoAPIClient)repoDao.getBaseClient()).searchSolrNodeRef(query,
					startIdx, nrOfresults, facettes, facettesMinCount,
					facettesLimit);
	
			return transform(repoDao, search);
			
		} catch (Throwable t) {
			
			throw DAOException.mapping(t);
		}
	}
	
	public static NodeSearch transform(RepositoryDao repoDao, SearchResultNodeRef search){
		return transform(repoDao,search,null);
	}
	
	public static NodeSearch transform(RepositoryDao repoDao, SearchResultNodeRef search, Filter filter) {

		NodeSearch result = new NodeSearch();

		List<NodeRef> data = new ArrayList<NodeRef>();
		List<Node> nodes = new ArrayList<Node>();
		result.setResult(data);
		result.setNodes(nodes);
		
		if(search==null || search.getData()==null)
			return result;
		for (org.edu_sharing.service.model.NodeRef nodeRef : search.getData()) {
			NodeRef ref = new NodeRef();
			ref.setRepo(repoDao.getId());
			ref.setId(nodeRef.getNodeId());
			
			String storeProtocol = nodeRef.getStoreProtocol();
			if(StoreRef.PROTOCOL_ARCHIVE.equals(storeProtocol)){
				ref.setArchived(true);
			}
			
			data.add(ref);
			
			if(nodeRef.getProperties() != null){
				
				try{
					nodes.add(new NodeDao(repoDao, nodeRef, filter).asNode());
				}catch(DAOException e){
					
				}
			}
		}
		result.setCount(search.getNodeCount());
		result.setSkip(search.getStartIDX());
	
		Map<String, Map<String, Integer>> countedProps = search
				.getCountedProps();
		if (countedProps != null) {
			List<Facette> resultFacettes = new ArrayList<Facette>();
			for (Entry<String, Map<String, Integer>> entry : countedProps
					.entrySet()) {

				Facette facette = new Facette();
				facette.setProperty(entry.getKey());

				List<Value> values = new ArrayList<Value>();
				for (Entry<String, Integer> entryValue : entry.getValue()
						.entrySet()) {

					Value value = new Value();
					value.setValue(entryValue.getKey());
					value.setCount(entryValue.getValue());

					values.add(value);
				}

				Collections.sort(values,new Comparator<Value>(){

					@Override
					public int compare(Value o1, Value o2) {
						return o2.getCount().compareTo(o1.getCount());
					}

				});
				facette.setValues(values);

				resultFacettes.add(facette);
			}
			result.setFacettes(resultFacettes);
		}

		return result;
	}
	
	public static void delete(String protocol, String store, String nodeId) {
		NodeService nodeService = NodeServiceFactory.getNodeService(null);
		nodeService.removeNode(protocol, store, nodeId);
	}

	private final RepositoryDao repoDao;
	private final String nodeId;

	private HashMap<String, Object> nodeProps;
	private HashMap<String, HashMap<String, Object>> nodeHistory;

	private Map<String, Boolean> hasPermissions;

	private final String type;
	private final List<String> aspects;

	private final String storeProtocol;
	
	private final String storeId;
	
	NodeService nodeService;
	CommentService commentService;

	final Filter filter;

	private org.edu_sharing.service.permission.PermissionService permissionService;
	
	public static final String defaultStoreProtocol = "workspace";
	public static final String defaultStoreId = "SpacesStore";
	
	public static final String archiveStoreProtocol = "archive";
	public static final String archiveStoreId = "SpacesStore";
	
	private NodeDao(RepositoryDao repoDao, String nodeId) throws Throwable {
		this(repoDao,nodeId,new Filter());
	}
	/**
	 * return a node by a given name inside a parent folder
	 * @param repoDao
	 * @param parentId the folder to search
	 * @param type the NodeType to find
	 * @param name the CM_NAME to find
	 * @return
	 * @throws Throwable
	 */
	public static NodeDao getByParent(RepositoryDao repoDao, String parentId,String type,String name) throws Throwable {
		NodeService nodeService = NodeServiceFactory.getNodeService(repoDao.getId());
		org.alfresco.service.cmr.repository.NodeRef ref = nodeService.getChild(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, parentId,type,CCConstants.CM_NAME, name);
		return new NodeDao(repoDao,ref.getId());
	}
	private NodeDao(RepositoryDao repoDao, String nodeId, Filter filter) throws Throwable {

		this(repoDao,null,null,nodeId,filter);
	}
	
	private NodeDao(RepositoryDao repoDao, String storeProtocol, String storeId, String nodeId, Filter filter) throws DAOException {
		this(repoDao,new org.edu_sharing.service.model.NodeRefImpl(repoDao.getId(),
				storeProtocol!=null ? storeProtocol : defaultStoreProtocol,
				storeId!=null ? storeId : defaultStoreId,
				nodeId),
				filter);
	}

	public static String mapNodeConstants(RepositoryDao repoDao,String node) throws DAOException {
		try {
			if ("-userhome-".equals(node)) {
				node = repoDao.getUserHome();
			}
			if ("-inbox-".equals(node)) {
				node = repoDao.getUserInbox();
			}
			if ("-saved_search-".equals(node)) {
				node = repoDao.getUserSavedSearch();
			}
			return node;
		}catch (Exception e){
			throw DAOException.mapping(e);
		}
	}
	public SearchResult<Node> runSavedSearch(int skipCount, int maxItems, SearchService.ContentType contentType, SortDefinition sort, List<String> facettes) throws DAOException {
		try {
			if(!CCConstants.getValidLocalName(CCConstants.CCM_TYPE_SAVED_SEARCH).equals(getType())){
				throw new IllegalArgumentException("The given node must be of type "+CCConstants.CCM_TYPE_SAVED_SEARCH);
			}
			HashMap<String, Object> props = getNativeProperties();
			RepositoryDao repoDao = RepositoryDao
					.getRepository((String) props.get(CCConstants.CCM_PROP_SAVED_SEARCH_REPOSITORY));
			MdsDaoV2 mdsDao = MdsDaoV2.getMds(repoDao, (String) props.get(CCConstants.CCM_PROP_SAVED_SEARCH_MDS));

			SearchToken token = new SearchToken();
			token.setFacettes(facettes);
			token.setSortDefinition(sort);
			token.setFrom(skipCount);
			token.setMaxResult(maxItems);
			token.setContentType(contentType);

			ObjectMapper mapper = new ObjectMapper();
			List<MdsQueryCriteria> parameters = Arrays.asList(mapper.readValue(
					(String) props.get(CCConstants.CCM_PROP_SAVED_SEARCH_PARAMETERS), MdsQueryCriteria[].class));
			NodeSearch search = NodeDao.searchV2(repoDao, mdsDao,
					(String) props.get(CCConstants.CCM_PROP_SAVED_SEARCH_QUERY), parameters, token, filter);

			List<Node> data;
			if (search.getNodes().size() < search.getResult().size()) {
				//searched repo deliveres only nodeRefs by query time
				data = new ArrayList<>();
				for (NodeRef ref : search.getResult()) {
					data.add(NodeDao.getNode(repoDao, ref.getId(), filter).asNode());
				}
			} else {
				//searched repo delivered properties by query time
				data = search.getNodes();
			}


			Pagination pagination = new Pagination();
			pagination.setFrom(search.getSkip());
			pagination.setCount(data.size());
			pagination.setTotal(search.getCount());


			SearchResult<Node> response = new SearchResult<>();
			response.setNodes(data);
			response.setPagination(pagination);
			response.setFacettes(search.getFacettes());

			return response;
		}catch(Throwable t){
			throw DAOException.mapping(t);
		}
	}
	public static NodeSearch getRelevantNodes(RepositoryDao repoDao, int skipCount, int maxItems) throws DAOException {
		try {
			return transform(repoDao, SearchServiceFactory.getSearchService(repoDao.getId()).getRelevantNodes(skipCount,maxItems));
		}catch(Throwable t){
			throw DAOException.mapping(t);
		}
	}

	private int getCommentCount(){
		if(nodeProps.containsKey(CCConstants.VIRT_PROP_COMMENTCOUNT)){
			return (int) nodeProps.get(CCConstants.VIRT_PROP_COMMENTCOUNT);
		}
		return 0;
	}

	private NodeDao(RepositoryDao repoDao, org.edu_sharing.service.model.NodeRef nodeRef, Filter filter) throws DAOException {
		try{
	
			this.repoDao = repoDao;
			this.nodeId = nodeRef.getNodeId();
			
			this.storeProtocol = nodeRef.getStoreProtocol();
			this.storeId = nodeRef.getStoreId();
			
			this.nodeService = NodeServiceFactory.getNodeService(repoDao.getId());
			this.permissionService = PermissionServiceFactory.getPermissionService(repoDao.getId());
			/**
			 * call getProperties on demand
			 */
			if(nodeRef.getProperties() == null || nodeRef.getProperties().size() == 0){
				this.nodeProps = this.nodeService.getProperties(this.storeProtocol, this.storeId, this.nodeId);
				
			}else{
				this.nodeProps = nodeRef.getProperties();
			}
			this.previewData = nodeRef.getPreview();
			refreshPermissions(nodeRef);
			
			if(nodeProps.containsKey(CCConstants.NODETYPE)){
				this.type = (String) nodeProps.get(CCConstants.NODETYPE);
			}
			else{
				this.type = CCConstants.CCM_TYPE_IO;
			}
			if(nodeRef.getAspects() == null) {
				String[] aspects = nodeService.getAspects(this.storeProtocol, this.storeId, nodeId);
				this.aspects = (aspects != null) ? Arrays.asList(aspects) : new ArrayList<String>();
			} else {
				this.aspects = nodeRef.getAspects();
			}
			this.access = PermissionServiceHelper.getPermissionsAsString(hasPermissions);
			// replace all data if its an remote object
			if(this.type.equals(CCConstants.CCM_TYPE_REMOTEOBJECT)){
				this.remoteId=(String)this.nodeProps.get(CCConstants.CCM_PROP_REMOTEOBJECT_NODEID);
				this.remoteRepository=RepositoryDao.getRepository((String)this.nodeProps.get(CCConstants.CCM_PROP_REMOTEOBJECT_REPOSITORYID));
				this.nodeService=NodeServiceFactory.getNodeService(this.remoteRepository.getId());
				this.permissionService=PermissionServiceFactory.getPermissionService(this.remoteRepository.getId());
				this.nodeProps = this.nodeService.getProperties(null,null, this.remoteId);
			} else if (this.aspects.contains(CCConstants.CCM_ASPECT_REMOTEREPOSITORY)){
				// just fetch dynamic data which needs to be fetched, because the local io already has metadata
				try {
					NodeService nodeServiceRemote=NodeServiceFactory.getNodeService((String)this.nodeProps.get(CCConstants.CCM_PROP_REMOTEOBJECT_REPOSITORYID));
					HashMap<String, Object> nodePropsReplace = nodeServiceRemote.getPropertiesDynamic(
							null, null, (String) this.nodeProps.get(CCConstants.CCM_PROP_REMOTEOBJECT_NODEID));
					nodePropsReplace.remove(CCConstants.SYS_PROP_NODE_UID);
					nodePropsReplace.remove(CCConstants.CCM_PROP_REMOTEOBJECT_REPOSITORYID);
					nodePropsReplace.remove(CCConstants.CCM_PROP_REMOTEOBJECT_NODEID);
					this.nodeProps.putAll(nodePropsReplace);
				}catch(Throwable t){
					logger.warn("Error while fetching properties for node id "+getId()+": Node is a remote node and calling remote "+(String)this.nodeProps.get(CCConstants.CCM_PROP_REMOTEOBJECT_REPOSITORYID)+" failed",t);
				}
			}

			this.filter = filter;
			
		}catch(Throwable t){
			throw DAOException.mapping(t,nodeRef.getNodeId());
		}
	}
	public boolean isFromRemoteRepository(){
		return remoteId!=null || !this.repoDao.isHomeRepo() || this.aspects.contains(CCConstants.CCM_ASPECT_REMOTEREPOSITORY);
	}
	public void refreshPermissions(org.edu_sharing.service.model.NodeRef nodeRef) {
		if(nodeRef!=null && nodeRef.getPermissions()!=null && nodeRef.getPermissions().size() > 0){
			this.hasPermissions = nodeRef.getPermissions();
		} else {
			this.hasPermissions = permissionService.hasAllPermissions(storeProtocol, storeId, nodeId, DAO_PERMISSIONS);
		}
	}
	public static NodeEntries convertToRest(RepositoryDao repoDao,
											Filter propFilter,
											List<NodeRef> children,
											Integer skipCount,
											Integer maxItems) throws DAOException {
		return convertToRest(repoDao, propFilter, children, skipCount, maxItems, null);
	}
    public static NodeEntries convertToRest(RepositoryDao repoDao,
											Filter propFilter,
											List<NodeRef> children,
											Integer skipCount,
											Integer maxItems,
											Function<NodeDao, NodeDao> transform

	) throws DAOException {

        NodeEntries result=new NodeEntries();
        List<NodeRef> slice = new ArrayList<>();

		for(int i=skipCount;i<Math.min(children.size(),(long)skipCount+maxItems);i++){
			slice.add(children.get(i));
        }
		List<Node> nodes = convertToRest(repoDao, slice, propFilter, transform);
		Pagination pagination=new Pagination();
        pagination.setFrom(skipCount);
        pagination.setCount(nodes.size());
        pagination.setTotal(children.size());
        result.setPagination(pagination);
        result.setNodes(nodes);
        return result;
    }
    public static List<Node> convertToRest(RepositoryDao repoDao, List<NodeRef> list, Filter propFilter, Function<NodeDao, NodeDao> transform){
		final String user = AuthenticationUtil.getFullyAuthenticatedUser();
		final Context context = Context.getCurrentInstance();
		final String scope = NodeServiceInterceptor.getEduSharingScope();
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		List<Node> nodes = null;
		java.util.Collection<Callable<Node>> tasks = list.stream().map(
				(nodeRef) -> (Callable<Node>) () -> AuthenticationUtil.runAs(() -> {
						try {
							// apply thread variables to keep state of thread
							Context.setInstance(context);
							NodeServiceInterceptor.setEduSharingScope(scope);
							NodeDao nodeDao = NodeDao.getNode(repoDao, nodeRef.getId(), propFilter);
							if (transform != null) {
								nodeDao = transform.apply(nodeDao);
							}
							return nodeDao.asNode();
						} catch (DAOMissingException daoException) {
							logger.warn("Missing node " + nodeRef.getId() + " tried to fetch, skipping fetch", daoException);
							return null;
						} finally {
							Context.release();
						}
					}, user)
			).collect(Collectors.toList());
		try {
			nodes = executor.invokeAll(tasks).stream()
					.filter(Objects::nonNull)
					.map((f) -> {
						try {
							return f.get();
						} catch (InterruptedException|ExecutionException e) {
							throw new RuntimeException(e);
						}
					}).filter(Objects::nonNull).collect(Collectors.toList());
			executor.shutdown();
			return nodes;
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

    public static List<NodeRef> sortApiNodeRefs(RepositoryDao repoDao, List<NodeRef> refList, List<String> filter, SortDefinition sortDefinition) {
        return NodeDao.convertAlfrescoNodeRef(repoDao,NodeDao.sortAlfrescoRefs(NodeDao.convertApiNodeRef(refList), filter, sortDefinition));
    }

	public NodeDao createChild(String type, List<String> aspects,
			HashMap<String, String[]> properties,boolean renameIfExists) throws DAOException {
		return this.createChild(type, aspects, properties, renameIfExists, null);
	}
	
	public NodeDao createChild(String type, List<String> aspects,
			HashMap<String, String[]> properties,boolean renameIfExists, String childAssoc) throws DAOException {

		try {
			NameSpaceTool<String> nameSpaceTool = new NameSpaceTool<String>();
			type = nameSpaceTool.transformToLongQName(type);
			if(childAssoc!=null)
				childAssoc=CCConstants.getValidGlobalName(childAssoc);
			HashMap<String, String[]> props = transformProperties(properties);
			String childId;
			
			String originalNameArr[] = props.get(CCConstants.CM_NAME);
			String originalName= (originalNameArr != null && originalNameArr.length > 0) ? originalNameArr[0] : null;
			if(originalName == null) throw new Exception("missing name");

			// escape invalid characters of the name
			originalName = EduSharingNodeHelper.cleanupCmName(originalName);
			props.put(CCConstants.CM_NAME, new String[]{originalName});

			int i=2;
			while(true){
				try{
					childId = this.nodeService.createNode(nodeId, type, props, childAssoc);
					break;
				}catch(DuplicateChildNodeNameException e){
					if(renameIfExists){
						props.put(CCConstants.CM_NAME, new String[]{NodeServiceHelper.renameNode(originalName,i)});
						i++;
						continue;
					}
					throw e;
				}
			}
	
			if (aspects != null) {
				for (String aspect : aspects) {
					aspect = NameSpaceTool.transformToLongQName(aspect);
					nodeService.addAspect(childId, aspect);
				}
			}
	
			return new NodeDao(repoDao, childId, Filter.createShowAllFilter());
			
		} catch (Throwable t) {
			
			throw DAOException.mapping(t);
		}
		
	}

	public NodeDao createChildByMove(String sourceId) throws DAOException {

		try {
			
			nodeService.moveNode(nodeId, CCConstants.CM_ASSOC_FOLDER_CONTAINS,
					sourceId);
	
			return new NodeDao(repoDao, sourceId, Filter.createShowAllFilter());
			
		} catch (Throwable t) {
			
			throw DAOException.mapping(t);
		}
	}

	public NodeDao createChildByCopy(String sourceId, boolean withChildren)
			throws DAOException {

		try {
			org.alfresco.service.cmr.repository.NodeRef newNode = nodeService.copyNode(sourceId, nodeId, withChildren);
			permissionService.createNotifyObject(newNode.getId(), new AuthenticationToolAPI().getCurrentUser(), CCConstants.CCM_VALUE_NOTIFY_ACTION_PERMISSION_ADD);
			return new NodeDao(repoDao, newNode.getId(), Filter.createShowAllFilter());
			
		} catch (Throwable t) {
			
			throw DAOException.mapping(t);
		}
	}
	public List<NodeRef> getChildrenSubobjects() throws DAOException {
		SortDefinition sort=new SortDefinition();
		sort.addSortDefinitionEntry(new SortDefinition.SortDefinitionEntry(CCConstants.getValidLocalName(CCConstants.CCM_PROP_CHILDOBJECT_ORDER),true));
		sort.addSortDefinitionEntry(new SortDefinition.SortDefinitionEntry(CCConstants.getValidLocalName(CCConstants.CM_NAME),true));
		List<String> filter=new ArrayList<>();
		filter.add("files");
		return getChildren(null,filter,sort);
	}
	public List<NodeRef> getChildren() throws DAOException {
		return getChildren(null,null,new SortDefinition());
	}
	public List<NodeRef> getChildren(String assocName, List<String> filter, SortDefinition sortDefinition) throws DAOException {

		try {
			List<NodeRef> result = new ArrayList<NodeRef>();
	
			
			if(assocName!=null && !assocName.isEmpty()){
				assocName=CCConstants.getValidGlobalName(assocName);
			}
			for (ChildAssociationRef childRef : nodeService.getChildrenChildAssociationRefAssoc(getId(),assocName,filter,sortDefinition)) {
	
				NodeRef ref = new NodeRef();
				ref.setRepo(this.repoDao.getId());
				ref.setHomeRepo(this.repoDao.isHomeRepo());
				ref.setId(childRef.getChildRef().getId());
				
				String storeProtocol = childRef.getChildRef().getStoreRef().getProtocol();
				if(StoreRef.PROTOCOL_ARCHIVE.equals(storeProtocol)){
					ref.setArchived(true);
				}
			
				result.add(ref);
			}
	
			return result;
			
		} catch (Throwable t) {
			
			throw DAOException.mapping(t);
		}
	}
	public List<NodeRef> getAssocs(AssocInfo assoc,List<String> filter,SortDefinition sortDefinition) throws DAOException {

		try {
			List<NodeRef> result = new ArrayList<NodeRef>();
			assoc.setAssocName(CCConstants.getValidGlobalName(assoc.getAssocName()));
            List<AssociationRef> assocs = nodeService.getNodesByAssoc(getId(), assoc);
            assocs=((NodeServiceImpl)NodeServiceFactory.getLocalService()).sortNodeRefList(assocs,filter,sortDefinition);
			for (AssociationRef childRef : assocs) {

				NodeRef ref = new NodeRef();
				ref.setRepo(this.repoDao.getId());
				ref.setHomeRepo(this.repoDao.isHomeRepo());
				ref.setId(childRef.getTargetRef().getId());

				String storeProtocol = childRef.getTargetRef().getStoreRef().getProtocol();
				if(StoreRef.PROTOCOL_ARCHIVE.equals(storeProtocol)){
					ref.setArchived(true);
				}

				result.add(ref);
			}

			return result;

		} catch (Throwable t) {

			throw DAOException.mapping(t);
		}
	}
	public NodeDao changeProperties(HashMap<String,String[]> properties)
			throws DAOException {

		try {
			
			this.nodeService.updateNode(nodeId, transformProperties(properties));
	
			return new NodeDao(repoDao, nodeId, Filter.createShowAllFilter());
			
		} catch (Throwable t) {
			
			throw DAOException.mapping(t);
		}
	}

	public NodeDao changePropertiesWithVersioning(
			HashMap<String,String[]> properties, String comment) throws DAOException {

		try { 
			mergeVersionComment(properties, comment);
	
			// 1. update
			this.nodeService.updateNode(nodeId,transformProperties(properties));
	
			// 2. versioning
			this.nodeService.createVersion(nodeId);
	
			return new NodeDao(repoDao, nodeId, Filter.createShowAllFilter());
			
		} catch (Throwable t) {
			
			throw DAOException.mapping(t);
		}
	}
	
	public NodeDao changePreview(InputStream is,String mimetype) throws DAOException {

		try {
			is=ImageTool.autoRotateImage(is,ImageTool.MAX_THUMB_SIZE);
			nodeService.setProperty(storeProtocol,storeId,nodeId,
					CCConstants.CCM_PROP_IO_VERSION_COMMENT, CCConstants.VERSION_COMMENT_PREVIEW_CHANGED);
			nodeService.writeContent(storeRef, nodeId, is, mimetype, null,
					isDirectory() ? CCConstants.CCM_PROP_MAP_ICON : CCConstants.CCM_PROP_IO_USERDEFINED_PREVIEW);
			PreviewCache.purgeCache(nodeId);
			return new NodeDao(repoDao, nodeId);
			
		} catch (Throwable t) {

			throw DAOException.mapping(t);
		}
	}

	public NodeDao deletePreview() throws DAOException {

		try {
			nodeService.removeProperty(storeProtocol,storeId,nodeId,isDirectory() ? CCConstants.CCM_PROP_MAP_ICON : CCConstants.CCM_PROP_IO_USERDEFINED_PREVIEW);
			PreviewCache.purgeCache(nodeId);
			return new NodeDao(repoDao, nodeId);
		} catch (Throwable t) {
			throw DAOException.mapping(t);
		}
	}

	public NodeDao changeContent(InputStream is, String mimetype,
			String versionComment) throws DAOException {

		try {
			HashMap<String,String[]> props = new HashMap<>();
	
			boolean version=versionComment!=null && !versionComment.isEmpty();
			// 1. update
			if(version){
				props.put(CCConstants.CCM_PROP_IO_VERSION_COMMENT, new String[]{versionComment});
				//mergeVersionComment(props, versionComment);
			}
			props.put(CCConstants.CCM_PROP_IO_CREATE_VERSION,new String[]{new Boolean(version).toString()});
			nodeService.updateNode(nodeId, props);
			
	
			// 2. change content (automatic versioning)
			nodeService.writeContent(storeRef, nodeId, is, mimetype, null,
					CCConstants.CM_PROP_CONTENT);
	
			return new NodeDao(repoDao, nodeId);
			
		} catch (Throwable t) {
			
			throw DAOException.mapping(t);
		}
	}

	private void mergeVersionComment(HashMap<String,String[]> properties,
			String versionComment) {
		
		properties.remove(CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_VERSION_COMMENT));

		properties.put(CCConstants.CCM_PROP_IO_VERSION_COMMENT, new String[]{versionComment});
	}
	
	public void delete(boolean recycle) throws DAOException {
		try{
			nodeService.removeNode(nodeId, getParentId(),recycle);
		}catch(Exception e){
			throw DAOException.mapping(e);
		}
	}

	public List<NodeVersion> getHistory() throws DAOException {

		List<NodeVersion> history = new ArrayList<NodeVersion>();
		for (Entry<String, HashMap<String, Object>> version : getNodeHistory().entrySet()) {
			NodeVersion nodeVersion=convertVersionProps(version.getKey(),version.getValue());
			history.add(nodeVersion);
		}
		// Sort by version
		Collections.sort(history, (o1, o2) -> {
			if(o1.getVersion().getMajor()==o2.getVersion().getMajor())
				return o1.getVersion().getMinor()>o2.getVersion().getMinor() ? 1 : -1;
			return o1.getVersion().getMajor()>o2.getVersion().getMajor() ? 1 : -1;
		});

		return history;
	}

	public NodeDao revertHistory(int major, int minor) throws DAOException {

		try {
			
			String versionLabel = getVersionLabel(major, minor);
	
			HashMap<String, Object> versionProps = getNodeHistory().get(versionLabel);
	
			if (versionProps == null) {
				return null;
			}
	
			nodeService.revertVersion(nodeId, versionLabel);
	
			return new NodeDao(repoDao, nodeId);
			
		} catch (Throwable t) {
			
			throw DAOException.mapping(t);
		}
	}
	public List<String> getAspectsNative(){
		return this.aspects;
	}
	public Node asNode() throws DAOException {
		return asNode(true);
	}
		public Node asNode(boolean fetchReference) throws DAOException {
		if(this.version!=null){
			VersionedNode node=new VersionedNode();
			fillNodeObject(node);
			VersionedNode.Version version=new VersionedNode.Version();
			version.setComment((String)nodeProps.get(CCConstants.CCM_PROP_IO_VERSION_COMMENT));
			node.setVersion(version);
			return node;
		}
		Node data = new Node();
		if (isCollectionReference() && fetchReference) {
			data = new CollectionReference();
			fillNodeReference((CollectionReference) data);
		}
		fillNodeObject(data);
		return data;
	}

	private boolean isCollectionReference() {
		return aspects.contains(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE);
	}

	private boolean isPublishedCopy() {
		return getNativeProperties().get(CCConstants.CCM_PROP_IO_PUBLISHED_ORIGINAL) != null;
	}

	private void fillNodeReference(CollectionReference reference) throws DAOException {
		final String originalId = getReferenceOriginalId();
		reference.setOriginalId(originalId);
		// not supported and used by remote repositories
		if(isFromRemoteRepository()){
			return;
		}
		try {
			reference.setAccessOriginal(NodeDao.getNode(repoDao, originalId).asNode(false).getAccess());
		} catch (Throwable t) {
			// user may has no access to the original or it is deleted, this is okay
		}
		AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {

			@Override
			public Void doWork() throws Exception {
				try {
					NodeDao nodeDaoOriginal = NodeDao.getNode(repoDao, originalId);
					Serializable originalAccess = NodeServiceHelper.getPropertyNative(
							new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, originalId)
							, CCConstants.CCM_PROP_RESTRICTED_ACCESS);
					reference.setCreatedBy(nodeDaoOriginal.asNode(false).getCreatedBy());
					reference.setOriginalRestrictedAccess(originalAccess != null && (Boolean) originalAccess);
				} catch (InvalidNodeRefException t) {
					reference.setOriginalId(null);
					// original maybe deleted
				} catch (Throwable t){
					logger.warn("Could not fetch original node for id " + nodeId, t);
					reference.setOriginalId(null);
				}
				return null;
			}

		});
	}

	private String getReferenceOriginalId() throws DAOException {
		final String originalId = (String) getNativeProperties().get(CCConstants.CCM_PROP_IO_ORIGINAL);
		return originalId;
	}

	private void fillNodeObject(Node data) throws DAOException {
		data.setRef(getRef());
		data.setParent(getParentRef());
		data.setRemote(getRemote());

		data.setType(getType());
		data.setIsDirectory(isDirectory());
		data.setAspects(NameSpaceTool.transFormToShortQName(this.aspects));

		data.setName(getName());
		data.setTitle(getTitle());

		data.setCreatedAt(getCreatedAt());
		data.setCreatedBy(getCreatedBy());
		data.setOwner(getOwner());

		data.setModifiedAt(getModifiedAt());
		data.setModifiedBy(getModifiedBy());

		data.setContent(getContent(data));

		data.setDownloadUrl(getDownloadUrl());
		data.setMetadataset(getMetadataSet());

		data.setProperties(getProperties());

		data.setAccess(access);

		data.setMimetype(getMimetype());
		data.setMediatype(getMediatype());
		data.setIconURL(getIconURL());
		data.setCommentCount(getCommentCount());
		data.setLicense(getLicense());
		data.setSize(getSize(data));

		data.setRating(getRating());
		try {
			data.setPreview(getPreview());
		}catch(Exception e) {
			logger.warn(e.getMessage(),e);
		}
		data.setRepositoryType(getRepositoryType());

		if(isCollection()){
			Collection collection=new CollectionDao(repoDao, getRef().getId(),this,data).asCollection();
			data.setCollection(collection);
		}
	}
	public RepositoryDao getRepositoryDao(){
		return remoteRepository!=null ? remoteRepository : repoDao;
	}
	private Remote getRemote() throws DAOException {
		if(!isFromRemoteRepository())
			return null;
		Remote remote=new Remote();
		String remoteObjectRepositoryId = (String)this.nodeProps.get(CCConstants.CCM_PROP_REMOTEOBJECT_REPOSITORYID);
		if(aspects.contains(CCConstants.CCM_ASPECT_REMOTEREPOSITORY) && remoteObjectRepositoryId != null){
			remote.setId((String)this.nodeProps.get(CCConstants.CCM_PROP_REMOTEOBJECT_NODEID));
			try {
				remote.setRepository(RepositoryDao.getRepository(remoteObjectRepositoryId).asRepo());
			}catch(DAOMissingException e){
				logger.warn("Repository " + remoteObjectRepositoryId + " is not present anymore: " + e.getMessage());
			}
		} else if(remoteId!=null){
			remote.setId(remoteId);
			remote.setRepository(remoteRepository.asRepo());
		} else {
			// this is the case if NodeDao was already called via a remote ref (and not a shadow object)
			remote.setId(getId());
			remote.setRepository(repoDao.asRepo());
		}
		return remote;
	}

	private License getLicense() {
		if(isDirectory())
			return null;
		License license=new License();
		license.setIcon(new LicenseService().getIconUrl((String) nodeProps.get(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY)));
		license.setUrl(new LicenseService().getLicenseUrl(
				(String) nodeProps.get(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY),
				(String) nodeProps.get(CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_LOCALE),
				(String) nodeProps.get(CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_VERSION)
		));
		return license;
	}

	private Content getContent(Node data) throws DAOException {
		Content content=new Content();
		content.setVersion(getContentVersion(data));
		content.setUrl(getContentUrl());

		if(isFromRemoteRepository()){
			// @TODO: not available here
		} else if(data instanceof CollectionReference && ((CollectionReference) data).getOriginalId() != null){
			content.setHash(AuthenticationUtil.runAsSystem(() ->
					nodeService.getContentHash(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),
							((CollectionReference) data).getOriginalId(),this.version,org.alfresco.model.ContentModel.PROP_CONTENT.toString())
			));
		} else {
			content.setHash(nodeService.getContentHash(storeProtocol, storeId, nodeId, this.version, org.alfresco.model.ContentModel.PROP_CONTENT.toString()));
		}
		return content;
	}

	public String getType() {
		return NameSpaceTool.transformToShortQName(this.type);
	}

	private String getMetadataSet() {
		return (String)nodeProps.get(CCConstants.CM_PROP_METADATASET_EDU_METADATASET);
	}

	private String getIconURL() {
		return new MimeTypesV2().getIcon(type,nodeProps,aspects);
	}
	public List<String> getPermissions(String authority) throws DAOException {
		try{
			return permissionService.getPermissionsForAuthority(nodeId,authority);
		}catch(Exception e){
			throw DAOException.mapping(e);
		}
	}

	public NodePermissions getPermissions() throws DAOException {

		try {
			
			org.edu_sharing.repository.client.rpc.ACL permissions = null;
			try{
				
				permissions = permissionService.getPermissions(nodeId);
				
			}catch( org.alfresco.repo.security.permissions.AccessDeniedException accessDenied){
				//than you don't have the permission no ask for
				return null;
			}
			
			if (permissions == null) {
				return null;
			}
				
			NodePermissions result = new NodePermissions();
			
			ACL local = new ACL();
			local.setInherited(permissions.isInherited());
			local.setPermissions(new ArrayList<ACE>());

			result.setLocalPermissions(local);			
			result.setInheritedPermissions(new ArrayList<ACE>());

			org.edu_sharing.repository.client.rpc.ACE[] aces = permissions.getAces();
			
			if (aces != null) {
				
				HashMap<Authority,List<String>> authPerm = new HashMap<Authority,List<String>>();
				HashMap<Authority,List<String>> authPermInherited = new HashMap<Authority,List<String>>();
				for (org.edu_sharing.repository.client.rpc.ACE ace : aces) {
				
					if ("acepted".equals(ace.getAccessStatus())) {
					
						Authority authority = (Authority.Type.valueOf(ace.getAuthorityType()) == Authority.Type.GROUP )? new Group() : new Authority();
						if(authority instanceof Group) {
							Group g = (Group)authority;
							g.setProfile(new GroupProfile());
							g.getProfile().setGroupType(ace.getGroup().getGroupType());
							g.setEditable(ace.getGroup().isEditable());
						}
						
						authority.setAuthorityName(ace.getAuthority());
						authority.setAuthorityType(Authority.Type.valueOf(ace.getAuthorityType()));
						
						if (ace.isInherited()) {
							
							List<String> tmpPerms = authPermInherited.get(authority);
							if(tmpPerms == null){
								tmpPerms = new ArrayList<String>();
							}
							// do not duplicate existing permissions
							if(!tmpPerms.contains(ace.getPermission()))
								tmpPerms.add(ace.getPermission());
							authPermInherited.put(authority, tmpPerms);
								
						} else {
							List<String> tmpPerms = authPerm.get(authority);
							if(tmpPerms == null){
								tmpPerms = new ArrayList<String>();
							}
							// do not duplicate existing permissions
							if(!tmpPerms.contains(ace.getPermission()))
								tmpPerms.add(ace.getPermission());
							authPerm.put(authority, tmpPerms);
						}
					}
				}
				
				for(Map.Entry<Authority,List<String>> entry : authPerm.entrySet()){
					ACE ace = getACEAsSystem(entry.getKey());
					ace.setPermissions(entry.getValue());
					if(entry.getKey() instanceof Group) {
						ace.setEditable(((Group)entry.getKey()).isEditable());
					}
					result.getLocalPermissions().getPermissions().add(ace);
				}
				
				for(Map.Entry<Authority,List<String>> entry : authPermInherited.entrySet()){
					ACE ace = getACEAsSystem(entry.getKey());
					ace.setPermissions(entry.getValue());
					result.getInheritedPermissions().add(ace);
				}
				
			}
			
			return result;
			
		} catch (Throwable t) {
			
			throw DAOException.mapping(t);
		}
		
	}
	
	private ACE getACEAsSystem(Authority key){
		return AuthenticationUtil.runAsSystem(new RunAsWork<ACE>() {

			@Override
			public ACE doWork() throws Exception {
				try{
					if(key.getAuthorityType().name().equals("GROUP")){
						GroupProfile group=GroupDao.getGroup(repoDao, key.getAuthorityName()).asGroup().getProfile();
						return new ACE(key,null,group);
					}else if(key.getAuthorityType().name().equals("EVERYONE")){
						//@TODO check if authority must be changed
						GroupProfile group=GroupDao.getGroup(repoDao, key.getAuthorityName()).asGroup().getProfile();
						return new ACE(key,null,group);
					}else if(key.getAuthorityName().equals("ROLE_OWNER")) {
						return new ACE(key,null,null);
					}
					else{
						UserProfile user=PersonDao.getPerson(repoDao, key.getAuthorityName()).asPerson().getProfile();
						return new ACE(key,user,null);
					}
				}catch(DAOException e){
					// this may happens for a virtual user, e.g. GROUP_EVERYONE
					return new ACE(key,null,null);
				}
			}
		});
	}

	public void setPermissions(ACL permissions, String mailText, Boolean sendMail, Boolean sendCopy) throws DAOException {
		
		try {
			
			List<org.edu_sharing.repository.client.rpc.ACE> aces = new ArrayList<org.edu_sharing.repository.client.rpc.ACE>(); 

			for (ACE permission : permissions.getPermissions()) {
				
				for(String tmpPerm : permission.getPermissions()){
					org.edu_sharing.repository.client.rpc.ACE ace = new org.edu_sharing.repository.client.rpc.ACE();
					
					ace.setAccessStatus("acepted");
					
					ace.setAuthority(permission.getAuthority().getAuthorityName());
					ace.setAuthorityType(permission.getAuthority().getAuthorityType().name());
					
					ace.setPermission(tmpPerm);
					
					aces.add(ace);
				}
			}
			
			org.edu_sharing.service.permission.PermissionService permissionService = PermissionServiceFactory.getPermissionService(repoDao.getId());
			permissionService.setPermissions(
					nodeId, 
					aces,
					permissions.isInherited(), 
					mailText, sendMail, sendCopy
			);
			
			
		} catch (Throwable t) {
			
			throw DAOException.mapping(t);
		}
	}
	
	private HashMap<String,String[]> transformProperties(
			HashMap<String,String[]> properties) {
		return NodeServiceHelper.transformShortToLongProperties(properties);
	}

	private String getId() {

		return (String) nodeProps.get(CCConstants.SYS_PROP_NODE_UID);
	}

	public boolean isDirectory(){
		return MimeTypesV2.isDirectory(nodeProps);
	}
	
	public boolean isCollection(){
		return MimeTypesV2.isCollection(aspects, nodeProps);
	}
	
	public NodeRef getRef() {

		NodeRef nodeRef = createNodeRef(repoDao,nodeId);
		String storeProtocol = (String)this.nodeProps.get(CCConstants.SYS_PROP_STORE_PROTOCOL);
		if(StoreRef.PROTOCOL_ARCHIVE.equals(storeProtocol)){
			nodeRef.setArchived(true);
		}

		return nodeRef;
	}
	private NodeRef getParentRef() {
		return createNodeRef(repoDao,getParentId());
	}
	public static NodeRef createNodeRef(RepositoryDao repoDao,String nodeId) {
		if(repoDao.getRepositoryType().equals(ApplicationInfo.REPOSITORY_TYPE_LOCAL)){
			try {
				repoDao = RepositoryDao.getHomeRepository();
			} catch (DAOException e) {
				throw new RuntimeException(e);
			}
		}
		NodeRef nodeRef = new NodeRef();
		nodeRef.setRepo(repoDao.getId());
		nodeRef.setId(nodeId);
		nodeRef.setHomeRepo(repoDao.isHomeRepo());
		return nodeRef;
	}

	public String getStoreProtocol(){
		return (String)nodeProps.get(CCConstants.SYS_PROP_STORE_PROTOCOL);
	}
	
	public String getStoreIdentifier(){
		return (String)nodeProps.get(CCConstants.SYS_PROP_STORE_IDENTIFIER);
	}
	
	private String getParentId() {

		return (String) nodeProps
				.get(CCConstants.VIRT_PROP_PRIMARYPARENT_NODEID);
	}

	private String getName() {

		return (String) nodeProps.get(CCConstants.CM_NAME);
	}

	private String getTitle() {
		return (String) (nodeProps.get(CCConstants.LOM_PROP_GENERAL_TITLE) != null ?
						nodeProps.get(CCConstants.LOM_PROP_GENERAL_TITLE) :
						nodeProps.get(CCConstants.CM_PROP_TITLE));
	}

	public Date getCreatedAt() {

		String key = CCConstants.CM_PROP_C_CREATED
				+ CCConstants.LONG_DATE_SUFFIX;

		return nodeProps.containsKey(key) ? new Date(
				Long.parseLong((String) nodeProps.get(key))) : null;
	}

	public Person getCreatedBy() {

		Person ref = new Person();
		ref.setFirstName((String) nodeProps
				.get(CCConstants.NODECREATOR_FIRSTNAME));
		ref.setLastName((String) nodeProps
				.get(CCConstants.NODECREATOR_LASTNAME));
		if(this.checkUserHasPermissionToSeeMail((String) nodeProps.get(CCConstants.CM_PROP_C_CREATOR)))
			ref.setMailbox((String) nodeProps.get(CCConstants.NODECREATOR_EMAIL));

		return ref;
	}
	
	private Person getOwner() {
		User owner=nodeService.getOwner(storeId, storeProtocol, nodeId);
		if(owner==null)
			return null;
		Person ref = new Person();
		ref.setFirstName(owner.getGivenName());
		ref.setLastName(owner.getSurname());
		if(this.checkUserHasPermissionToSeeMail(owner.getUsername()))//only admin can see even if users have hide email
			ref.setMailbox(owner.getEmail());

		return ref;
	}


	/**
	 * Check if normal USER has permision to see email
	 * ADMIN can see the email even if the email is private for specific USER
	 *
	 * @param (String) userName  of Person,
	 * @return true || false
	 */
	private boolean checkUserHasPermissionToSeeMail(String userName) {
		try {
			if(LightbendConfigLoader.get().getBoolean("repository.privacy.filterMetadataEmail") &&
					(access== null || !access.contains(CCConstants.PERMISSION_WRITE))){
				return false;
			}
			if (this.isCurrentUserAdminOrSameUserAsUserName(userName)) // if is ADMIN or sameUser, don't need to countinue;
				return true;
			else {
				Map<String, Serializable> profileSettings = AuthorityServiceFactory.getLocalService().getProfileSettingsProperties(userName, CCConstants.CCM_PROP_PERSON_SHOW_EMAIL);
				boolean isEmailPublic = false;
				if (profileSettings.containsKey(CCConstants.CCM_PROP_PERSON_SHOW_EMAIL)) {
					isEmailPublic = (boolean) profileSettings.get(CCConstants.CCM_PROP_PERSON_SHOW_EMAIL);
				}
				return isEmailPublic;
			}

		} catch (Exception e) {
			logger.debug("Cannot check if current User has permision to see email or not  : " + e.getMessage(), e);
			return false;
		}
	}

	/**
	 * Method which check if:
	 * -  login User is ADMIN or simple USER,
	 * -  login User is similar with userName
	 *
	 * @param userName get a userName as parameter to controll
	 * @return TRUE if is ADMIN or Same USER || FALSE in other cases
	 */
	public boolean isCurrentUserAdminOrSameUserAsUserName(String userName) {
		if (AuthorityServiceFactory.getLocalService().isGlobalAdmin()) // if userLogin is ADMIN, don't need to countinue;
			return true;
		else {
			String currentUser = AuthenticationUtil.getFullyAuthenticatedUser();
			return currentUser.equals(userName);
		}
	}


	private Date getModifiedAt() {

		String key = CCConstants.CM_PROP_C_MODIFIED
				+ CCConstants.LONG_DATE_SUFFIX;

		return nodeProps.containsKey(key) ? new Date(
				Long.parseLong((String) nodeProps.get(key))) : null;
	}

	private Person getModifiedBy() {

		Person ref = new Person();

		ref.setFirstName((String) nodeProps
				.get(CCConstants.NODEMODIFIER_FIRSTNAME));
		ref.setLastName((String) nodeProps
				.get(CCConstants.NODEMODIFIER_LASTNAME));
		if(this.checkUserHasPermissionToSeeMail((String) nodeProps.get(CCConstants.CM_PROP_C_MODIFIER)))
			ref.setMailbox((String) nodeProps.get(CCConstants.NODEMODIFIER_EMAIL));

		return ref;
	}

	private String getContentVersion(Node data) throws DAOException {
		if(data instanceof CollectionReference && ((CollectionReference) data).getOriginalId() != null && !isFromRemoteRepository()){
			try {
				return AuthenticationUtil.runAsSystem(() ->
						nodeService.getProperty(StoreRef.PROTOCOL_WORKSPACE,
								StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),
								((CollectionReference) data).getOriginalId(), CCConstants.CM_PROP_VERSIONABLELABEL)
				);
			}catch(Throwable t){
				logger.warn("Error while fetching original node version from " + nodeId + ":" + t.getMessage());
			}
		}
		return (String) nodeProps.get(CCConstants.CM_PROP_VERSIONABLELABEL);
	}

	private String getContentUrl() {
		return (String) nodeProps.get(CCConstants.CONTENTURL);
	}
	private RatingDetails getRating(){
		try{
			return RatingServiceFactory.getRatingService(repoDao.getId()).getAccumulatedRatings(nodeId,null);
		}catch(Throwable t){
			logger.warn("Can not fetch ratings for node "+nodeId+": "+t.getMessage(),t);
			return null;
		}
	}
	private boolean isLink(){
		return nodeProps.containsKey(CCConstants.CCM_PROP_IO_WWWURL);
	}
	public String getDownloadUrl(){
		// the download servlet can may passthrough the TECHNICAL_LOCATION, so it depends on the actual state of the DOWNLOADURL
		//if(isLink())
		//	return null;

		// no download url if user can not access the content
		if(!access.contains(CCConstants.PERMISSION_READ_ALL))
			return null;
		return (String) nodeProps.get(CCConstants.DOWNLOADURL);
	}

	public NodeVersion getVersion(int major, int minor) throws DAOException {

		String versionLabel = getVersionLabel(major, minor);

		HashMap<String, Object> versionProps = getNodeHistory().get(versionLabel);

		return convertVersionProps(versionLabel, versionProps);
	}

	private NodeVersion convertVersionProps(String versionLabel, HashMap<String, Object> versionProps) throws DAOException {
		if (versionProps == null) {
			return null;
		}

		NodeVersion result = new NodeVersion();

		result.setVersion(transformVersion(versionLabel));

		result.setComment((String) versionProps
				.get(CCConstants.CCM_PROP_IO_VERSION_COMMENT));
		result.setContentUrl((String) versionProps.get(CCConstants.CONTENTURL));

		String keyCreated = CCConstants.CM_PROP_C_MODIFIED;
		result.setModifiedAt(versionProps.containsKey(keyCreated) ? ((String) versionProps.get(keyCreated)) : null);
		result.setProperties(convertProperties(filter,versionProps));
		Person ref = new Person();

		ref.setFirstName((String) versionProps
				.get(CCConstants.NODEMODIFIER_FIRSTNAME));
		ref.setLastName((String) versionProps
				.get(CCConstants.NODEMODIFIER_LASTNAME));
		ref.setMailbox((String) versionProps.get(CCConstants.NODEMODIFIER_EMAIL));

		result.setModifiedBy(ref);

		result.setProperties(getProperties(versionLabel));

		return result;
	}

	private static String getVersionLabel(int major, int minor) {
		return major + "." + minor;
	}

	private NodeVersionRef transformVersion(String versionLabel) {

		String[] versionTokens = versionLabel.split("\\.");

		NodeVersionRef version = new NodeVersionRef();
		version.setNode(getRef());
		version.setMajor(Integer.parseInt(versionTokens[0]));
		version.setMinor(Integer.parseInt(versionTokens[1]));
		version.setMinor(Integer.parseInt(versionTokens[1]));

		return version;
	}

	public HashMap<String,Object> getNativeProperties() {
		return nodeProps;
	}
	public void setNativeProperties(HashMap<String,Object> nodeProps) {
		this.nodeProps = nodeProps;
	}

	public HashMap<String,String[]> getAllProperties() throws DAOException {
		return getProperties(null,Filter.createShowAllFilter());
	}
	
	private HashMap<String,String[]> getProperties() throws DAOException {
		return getProperties(null);
	}
	
	private HashMap<String,String[]>  getProperties(String versionLabel) throws DAOException {
		return getProperties(versionLabel,filter);
	}
	
	public HashMap<String, Object> getNativeProperties(String versionLabel) throws DAOException{
		return versionLabel != null ? getNodeHistory()
				.get(versionLabel) : nodeProps;
	}
	
	public void addWorkflowHistory(WorkflowHistory history) throws DAOException{
		HashMap<String, String[]> properties = getAllProperties();
		String[] data=properties.get(CCConstants.getValidLocalName(CCConstants.CCM_PROP_WF_PROTOCOL));
		List<String> list=new ArrayList<>();
		if(data!=null)
			list=new ArrayList<>(Arrays.asList(data));
		try{
			String[] receivers=new String[history.getReceiver().length];
			for(int i=0;i<receivers.length;i++){
				receivers[i]=history.getReceiver()[i].getAuthorityName();
			}
			JSONObject json=new JSONObject();
			json.put("comment", history.getComment());
			json.put("editor", (history.getEditor()!=null && !history.getEditor().getAuthorityName().isEmpty()) ? history.getEditor().getAuthorityName() : AuthenticationUtil.getFullyAuthenticatedUser());
			json.put("receiver", receivers);
			json.put("status", history.getStatus());
			json.put("time", history.getTime()>0 ? history.getTime() : System.currentTimeMillis());
			list.add(0, json.toString());
			String[] result=new String[0];
			result=list.toArray(result);
			properties.put(CCConstants.getValidLocalName(CCConstants.CCM_PROP_WF_INSTRUCTIONS),new String[]{history.getComment()});
			properties.put(CCConstants.getValidLocalName(CCConstants.CCM_PROP_WF_RECEIVER),receivers);
			properties.put(CCConstants.getValidLocalName(CCConstants.CCM_PROP_WF_STATUS),new String[]{history.getStatus()});
			properties.put(CCConstants.getValidLocalName(CCConstants.CCM_PROP_WF_PROTOCOL),result);
			changeProperties(properties);
		}catch (Throwable t) {
			throw DAOException.mapping(t);
		}
	}

	public List<WorkflowHistory> getWorkflowHistory() throws DAOException{
		List<WorkflowHistory> workflow=new ArrayList<>();
		String[] data=getAllProperties().get(CCConstants.getValidLocalName(CCConstants.CCM_PROP_WF_PROTOCOL));
		if(data==null)
			return workflow;
		try{
			for(String entry : data){
				JSONObject json=new JSONObject(entry);
				WorkflowHistory history=new WorkflowHistory();
				if(json.has("comment"))
					history.setComment(json.getString("comment"));
				try{
					history.setEditor(new PersonDao(repoDao,json.getString("editor")).asPersonSimple());
				}catch(Throwable t){
					// The user may has no permission or entry deleted
					history.setEditor(new UserSimple());
					history.getEditor().setAuthorityName(json.getString("editor"));
				}
				JSONArray arr = json.getJSONArray("receiver");
				Authority[] list = new Authority[arr.length()];
				for(int i = 0; i < arr.length(); i++){
					try{
						String authority = arr.getString(i);
						if(authority.startsWith(PermissionService.GROUP_PREFIX)) {
							list[i]=new GroupDao(repoDao, authority).asGroup();
						}else {
							list[i]=new PersonDao(repoDao,authority).asPersonSimple();
						}			
					}catch(Throwable t){
						// The user may has no permission or entry deleted
						list[i] = new UserSimple();
						list[i].setAuthorityName(arr.getString(i));
					}
				}
				history.setReceiver(list);
				history.setStatus(json.getString("status"));
				history.setTime(Long.parseLong(json.getString("time")));
				
				workflow.add(history);
			}
			return workflow;

		}catch (Exception e) {
			throw DAOException.mapping(e);
		}
	}
	
	private HashMap<String,String[]>  getProperties(String versionLabel,Filter filter) throws DAOException {
		HashMap<String, Object> props = getNativeProperties(versionLabel);
		return convertProperties(filter, props);
	}

	private HashMap<String, String[]> convertProperties(Filter filter, HashMap<String, Object> props) {
		if (props == null) {
			return null;
		}

		if(filter.getProperties().size() == 0){
			return new HashMap<String, String[]>();
		}

		HashMap<String,String[]> properties = new HashMap<String,String[]>();
		if(LightbendConfigLoader.get().getBoolean("repository.privacy.filterVCardEmail")) {
			List<String> cleanup = new ArrayList<>();
			for (Entry<String, Object> entry : props.entrySet()) {
				if (CCConstants.getLifecycleContributerPropsMap().containsValue(entry.getKey()) || CCConstants.getMetadataContributerPropsMap().containsValue(entry.getKey())) {
					if (access == null || !access.contains(CCConstants.PERMISSION_WRITE)) {
						entry.setValue(VCardConverter.removeEMails(StringUtils.join(ValueTool.getMultivalue(entry.getValue().toString()), "\n")));
						cleanup.add(entry.getKey() + CCConstants.VCARD_EMAIL);
					}
				}
			}
			cleanup.forEach(props::remove);
		}
		for (Entry<String, Object> entry : props.entrySet()) {

			if(entry.getKey() == null) {
				logger.info(nodeId+" null property has value "+entry.getValue());
				continue;
			}
			String shortPropName = NameSpaceTool.transformToShortQName(entry.getKey());

			if(shortPropName != null){

				if(filter.getProperties().size() > 0 &&
						!filter.getProperties().contains(Filter.ALL)
						&& !filter.getProperties().contains(shortPropName)){
					continue;
				}
				List<String> values = getPropertyValues(entry.getValue());

				if(props.containsKey(entry.getKey()+ CCConstants.LONG_DATE_SUFFIX)){
					values = getPropertyValues(props.get(entry.getKey()+CCConstants.LONG_DATE_SUFFIX));
					properties.put(shortPropName, values.toArray(new String[values.size()]));
				}
				else{
					properties.put(shortPropName, values.toArray(new String[values.size()]));
				}
			}

		}

		return properties;
	}

	private List<String> getPropertyValues(Object value) {
		List<String> values = new ArrayList<String>();
		if (value != null ){
			if(value instanceof Date){
				values.add(String.valueOf(((Date) value).getTime()));
				return values;
			}
			for (String mv : ValueTool.getMultivalue(value.toString())) {
				values.add(mv);
			}
		}
		return values;
	}

	private String getMimetype() {
		return MimeTypesV2.getMimeType(nodeProps);
	}
	
	public String getMediatype() {
		return MimeTypesV2.getNodeType(type,nodeProps,aspects);
	}

	private String getSize(Node data) {
		if(data instanceof CollectionReference && ((CollectionReference) data).getOriginalId() != null && !isFromRemoteRepository()){
			return AuthenticationUtil.runAsSystem(() ->
					nodeService.getProperty(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),
					((CollectionReference) data).getOriginalId(), CCConstants.LOM_PROP_TECHNICAL_SIZE));
		}
		return nodeProps.containsKey(CCConstants.LOM_PROP_TECHNICAL_SIZE) ? (String) nodeProps
						.get(CCConstants.LOM_PROP_TECHNICAL_SIZE) : null;
	}
	
	private String getRepositoryType(){
		return repoDao.getApplicationInfo().getRepositoryType();
	}
	
	private Preview getPreview() {
		Preview result = new Preview(	nodeService,
										getStoreProtocol(),
										getStoreIdentifier(),
										remoteId!=null ? remoteId : getRef().getId(),
										this.version,
										nodeProps);
		if(previewData != null){
			result.setMimetype(previewData.getMimetype());
			result.setData(previewData.getData());
		}
		return result;
	}
	
	private String getPreviewImage() {
		return new MimeTypesV2(repoDao.getApplicationInfo()).getPreview(type,nodeProps,aspects);
	}

	public HashMap<String, HashMap<String, Object>> getNodeHistory() throws DAOException {
		try{
			if(nodeHistory == null){
				this.nodeHistory = new HashMap<String, HashMap<String, Object>>();
	
				HashMap<String, HashMap<String, Object>> versionHistory = nodeService
						.getVersionHistory(nodeId);
				
				if (versionHistory != null) {
	
					for (HashMap<String, Object> version : versionHistory.values()) {
						nodeHistory.put((String) version
								.get(CCConstants.CM_PROP_VERSIONABLELABEL), version);
					}
				}
			}
			return nodeHistory;
		}catch (Throwable t) {
			
			throw DAOException.mapping(t);
		}
	}

	public List<NotifyEntry> getNotifys() throws DAOException {
		try{
			throwIfPermissionIsMissing(CCConstants.PERMISSION_CHANGEPERMISSIONS);
			List<Notify> notifys = permissionService.getNotifyList(nodeId);
			List<NotifyEntry> result = new ArrayList<>(notifys.size());
			for(Notify notify : notifys){
				result.add(new NotifyEntry(notify));
			}
			return result;
		}
		catch(Throwable t){
			throw DAOException.mapping(t);
		}
	}

	private void throwIfPermissionIsMissing(String permission) throws DAOSecurityException {
		if(!permissionService.hasPermission(storeProtocol,storeId,nodeId,permission)){
			throw new DAOSecurityException(new SecurityException("Current user has no "+permission+" on node "+nodeId));
		}
	}

	public static SearchResult<NodeDao> getFilesSharedByMe(RepositoryDao repoDao, List<String> filter, Filter propertyFilter, SortDefinition sortDefinition, Integer skipCount, Integer maxItems) throws DAOException {
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
		return serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(()-> {
			SearchService searchService = SearchServiceFactory.getSearchService(repoDao.getApplicationInfo().getAppId());
			try {
				SearchResultNodeRef result = searchService.getFilesSharedByMe(sortDefinition, mapFilterToContentType(filter), skipCount.intValue(), maxItems == null ? RestConstants.DEFAULT_MAX_ITEMS : maxItems.intValue());
				return NodeDao.convertResultSet(repoDao,propertyFilter,result);
			} catch (Exception e) {
				throw DAOException.mapping(e);
			}
		});
	}

	private static SearchService.ContentType mapFilterToContentType(List<String> filter) {
		if(filter==null || filter.size()==0)
			return SearchService.ContentType.ALL;
		if(filter.contains("files") && filter.contains("folders")){
			return SearchService.ContentType.FILES_AND_FOLDERS;
		}
		if(filter.contains("files")){
			return SearchService.ContentType.FILES;
		}
		if(filter.contains("folders")){
			return SearchService.ContentType.FOLDERS;
		}
		logger.warn("Could not map the given filter value(s) "+filter.get(0)+" to a given ContentType, will use ContentType.ALL");
		return SearchService.ContentType.ALL;
	}

	public static List<org.alfresco.service.cmr.repository.NodeRef> sortAlfrescoRefs(List<org.alfresco.service.cmr.repository.NodeRef> refs, List<String> filter, SortDefinition sortDefinition) {
        return NodeServiceFactory.getLocalService().sortNodeRefList(refs,filter,sortDefinition);
    }

    /**
	 * All files the current user is a receiver of the workflow
	 * @param repoDao
	 * @return
	 * @throws DAOException
	 */
	public static List<NodeRef> getWorkflowReceive(RepositoryDao repoDao,List<String> filter, SortDefinition sortDefinition) throws DAOException {
		SearchService searchService = SearchServiceFactory.getSearchService(repoDao.getApplicationInfo().getAppId());
		try {
			List<org.alfresco.service.cmr.repository.NodeRef> refs = searchService.getWorkflowReceive(AuthenticationUtil.getFullyAuthenticatedUser());
			refs=NodeDao.sortAlfrescoRefs(refs,filter,sortDefinition);
			return convertAlfrescoNodeRef(repoDao,refs);
		} catch (Exception e) {
			throw DAOException.mapping(e);
		}
	}
	
	public static SearchResult<NodeDao> getFilesSharedToMe(RepositoryDao repoDao, SharedToMeType shareType, List<String> filter, Filter propertyFilter, SortDefinition sortDefinition, Integer skipCount, Integer maxItems) throws DAOException {
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
		return serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(()-> {
			SearchService searchService = SearchServiceFactory.getSearchService(repoDao.getApplicationInfo().getAppId());
			try {
				SearchResultNodeRef result = searchService.getFilesSharedToMe(shareType, sortDefinition,mapFilterToContentType(filter),skipCount.intValue(),maxItems==null ? RestConstants.DEFAULT_MAX_ITEMS : maxItems.intValue());
				return NodeDao.convertResultSet(repoDao, propertyFilter, result);
			} catch (Exception e) {
				throw DAOException.mapping(e);
			}
		});
	}

	private static SearchResult<NodeDao> convertResultSet(RepositoryDao repoDao, Filter propFilter, SearchResultNodeRef result) {
		SearchResult<NodeDao> converted=new SearchResult<>();
		Pagination pagination=new Pagination();
		pagination.setFrom(result.getStartIDX());
		pagination.setTotal(result.getNodeCount());
		pagination.setCount(result.getData().size());
		converted.setPagination(pagination);
		converted.setNodes(result.getData().stream().map((ref)-> {
			try {
				return new NodeDao(repoDao,ref,propFilter);
			} catch (DAOException e) {
				logger.warn(e.getMessage(),e);
			}
			return null;
		}).collect(Collectors.toList()));
		return converted;
	}

	public List<NodeShare> getShares(String email) throws DAOSecurityException {
		throwIfPermissionIsMissing(CCConstants.PERMISSION_CHANGEPERMISSIONS);
		ShareServiceImpl service = new ShareServiceImpl();
		List<NodeShare> entries=new ArrayList<>();
		for(Share share : service.getShares(this.nodeId)){
			if(email==null || email.equals(share.getEmail()))
				entries.add(new NodeShare(new org.alfresco.service.cmr.repository.NodeRef(NodeDao.storeRef,this.nodeId),share));
		}
		return entries;
	}

	public NodeShare createShare(long expiryDate,String password) throws DAOException {
		ShareServiceImpl service = new ShareServiceImpl();
		try {
			throwIfPermissionIsMissing(CCConstants.PERMISSION_CHANGEPERMISSIONS);
			return new NodeShare(new org.alfresco.service.cmr.repository.NodeRef(NodeDao.storeRef,this.nodeId),service.createShare(nodeId, expiryDate,password));
		} catch (Exception e) {
			throw DAOException.mapping(e);
		}
	}
	
	public void removeShare(String shareId) throws DAOException {
		throwIfPermissionIsMissing(CCConstants.PERMISSION_CHANGEPERMISSIONS);
		ShareServiceImpl service=new ShareServiceImpl();
    	for(Share share : service.getShares(this.nodeId)){
    		if(share.getNodeId().equals(shareId)){
    			service.removeShare(shareId);
    			return;
    		}
		}
    	throw DAOException.mapping(new Exception("share "+shareId+" was not found on node "+nodeId));
	}
	
	public NodeShare updateShare(String shareId, long expiryDate, String password) throws DAOException {
		throwIfPermissionIsMissing(CCConstants.PERMISSION_CHANGEPERMISSIONS);
		ShareServiceImpl service=new ShareServiceImpl();
    	for(Share share : service.getShares(this.nodeId)){
    		if(share.getNodeId().equals(shareId)){
    			share.setExpiryDate(expiryDate);
    			share.setPassword(password);
    			service.updateShare(share);
    			return new NodeShare(new org.alfresco.service.cmr.repository.NodeRef(NodeDao.storeRef,this.nodeId),share);
    		}
		}
    	throw DAOException.mapping(new Exception("share "+shareId+" was not found on node "+nodeId));
	}
	
	public NodeDao importNode(String parent) throws DAOException {
		try{
			String result=nodeService.importNode(this.nodeId,parent);
			return new NodeDao(RepositoryDao.getRepository(RepositoryDao.HOME),result);
		}catch(Throwable t){
			throw DAOException.mapping(t);
		}
	}

 	public void createVersion(String comment) throws DAOException, Exception {
		this.changePropertiesWithVersioning(getAllProperties(), comment);
	}

    public static List<org.alfresco.service.cmr.repository.NodeRef> convertApiNodeRef(List<NodeRef> refs) {
        List<org.alfresco.service.cmr.repository.NodeRef> converted=new ArrayList<>(refs.size());
        for(NodeRef ref : refs){
            converted.add(new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,ref.getId()));
        }
        return converted;
    }
 	public static List<NodeRef> convertAlfrescoNodeRef(RepositoryDao repoDao,List<org.alfresco.service.cmr.repository.NodeRef> refs){
 		List<NodeRef> converted=new ArrayList<>(refs.size());
 		for(org.alfresco.service.cmr.repository.NodeRef ref : refs){
 			converted.add(new NodeRef(repoDao.getId(),ref.getId()));
 		}
 		return converted;
 	}

    public void reportNode(String reason, String userEmail, String userComment) throws DAOException {
		try{
			NotificationServiceFactory.getNotificationService(repoDao.getApplicationInfo().getAppId())
			.notifyNodeIssue(nodeId, reason, userEmail, userComment);
		}catch(Throwable t){
			throw DAOException.mapping(t);
		}
		
	}
	/** store a new search node 
	 * @return */
	public static NodeDao saveSearch(String repoId, String mdsId, String query, String name,
			List<MdsQueryCriteria> parameters,boolean replace) throws DAOException {
		try{
    		String parent = RepositoryDao.getHomeRepository().getUserSavedSearch();
    		NodeDao parentDao = new NodeDao(RepositoryDao.getHomeRepository(), parent);
    		HashMap<String, String[]> props=new HashMap();
    		props.put(CCConstants.CM_NAME, new String[]{NodeServiceHelper.cleanupCmName(name)});
    		props.put(CCConstants.LOM_PROP_GENERAL_TITLE, new String[]{name});
    		props.put(CCConstants.CCM_PROP_SAVED_SEARCH_REPOSITORY, new String[]{repoId});
    		props.put(CCConstants.CCM_PROP_SAVED_SEARCH_MDS, new String[]{mdsId});
    		props.put(CCConstants.CCM_PROP_SAVED_SEARCH_QUERY, new String[]{query});
    		props.put(CCConstants.CCM_PROP_SAVED_SEARCH_PARAMETERS, new String[]{Json.pretty(parameters)});
    		props.put(CCConstants.CCM_PROP_IO_CREATE_VERSION, new String[]{"true"});
    		try{
    			return parentDao.createChild(CCConstants.CCM_TYPE_SAVED_SEARCH, null, props, false);
    		}catch(DAOException e){
    			if(e.getCause() instanceof DuplicateChildNodeNameException && replace){
	    			NodeDao old = NodeDao.getByParent(RepositoryDao.getHomeRepository(), parent, CCConstants.CCM_TYPE_SAVED_SEARCH, NodeServiceHelper.cleanupCmName(name));
	    			old.changeProperties(props);
	    			return old;
    			}
    			throw e;
    		}
		}catch(Throwable t){
			throw DAOException.mapping(t);
		}
		
	}
	
	public static NodeRemote prepareUsage(String repId, String nodeId) throws DAOException, Throwable{
 		
 		String tmpNodeId = new RemoteObjectService().getRemoteObject(repId, nodeId);
 		
 		NodeRemote nodeRemote = new NodeRemote();
		nodeRemote.setNode(new NodeDao(RepositoryDao.getRepository(repId),nodeId,Filter.createShowAllFilter()).asNode());
 		if(!tmpNodeId.equals(nodeId)) {
 			nodeRemote.setRemote(new NodeDao(RepositoryDao.getRepository(RepositoryDao.HOME),tmpNodeId,Filter.createShowAllFilter()).asNode());
 		}
 		
 		return nodeRemote;
 		
 	}

	public void addAspects(List<String> aspects) {
		for(String aspect : aspects) {
			nodeService.addAspect(nodeId, CCConstants.getValidGlobalName(aspect));
		}
	}
	public boolean getTemplateStatus() throws DAOException {
		Object value = this.getNativeProperties().getOrDefault(CCConstants.CCM_PROP_METADATA_PRESETTING_STATUS,false);
		if(value instanceof String){
			return Boolean.valueOf((String) value);
		}
		return (boolean)value;
	}
	public NodeDao getTemplateNode() throws DAOException {
		try {
			String template = nodeService.getTemplateNode(nodeId,false);
			if(template==null)
				return null;
			return NodeDao.getNode(repoDao, template,Filter.createShowAllFilter());
		}catch(Throwable t){
			throw DAOException.mapping(t);
		}
	}
	public NodeDao changeTemplateProperties(Boolean enable,HashMap<String, String[]> properties) throws DAOException {
		try {
			nodeService.setTemplateStatus(nodeId, enable);
			if(enable) {
				nodeService.setTemplateProperties(nodeId, transformProperties(properties));
			}
			return getTemplateNode();
		}
		catch(Throwable t){
			throw DAOException.mapping(t);
		}
	}

	public void setOwner(String username) {
		nodeService.setOwner( this.getId(), username);
	}

	public void setProperty(String property, Serializable value) {
		if(value==null){
			nodeService.removeProperty(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getProtocol(), StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), this.getId(), property);
		}else {
			nodeService.setProperty(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getProtocol(), StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), this.getId(), property, value);
		}
	}

	public NodeDao createFork(String sourceId) throws DAOException {
		try {
			NodeDao sourceDao = NodeDao.getNode(repoDao, sourceId);
			String[] source = new String[]{sourceId};
			RunAsWork<NodeDao> work = () -> {
				try {
					org.alfresco.service.cmr.repository.NodeRef newNode = nodeService.copyNode(source[0], nodeId, false);
					permissionService.createNotifyObject(newNode.getId(), new AuthenticationToolAPI().getCurrentUser(), CCConstants.CCM_VALUE_NOTIFY_ACTION_PERMISSION_ADD);
					nodeService.addAspect(newNode.getId(), CCConstants.CCM_ASPECT_FORKED);
					nodeService.setProperty(newNode.getStoreRef().getProtocol(), newNode.getStoreRef().getIdentifier(), newNode.getId(), CCConstants.CCM_PROP_FORKED_ORIGIN,
							new org.alfresco.service.cmr.repository.NodeRef(storeProtocol, storeId, source[0]));
					nodeService.setProperty(newNode.getStoreRef().getProtocol(), newNode.getStoreRef().getIdentifier(), newNode.getId(), CCConstants.CCM_PROP_FORKED_ORIGIN_VERSION,
					nodeService.getProperty(storeProtocol, storeId, source[0], CCConstants.LOM_PROP_LIFECYCLE_VERSION));
					permissionService.removeAllPermissions(newNode.getId());
					// re-activate inherition
					permissionService.setPermissions(newNode.getId(), null, true);
					return new NodeDao(repoDao, newNode.getId());
				} catch (Throwable throwable) {
					throw new RuntimeException(throwable);
				}
			};
			if(sourceDao.isCollectionReference()){
				// validate ccm:restricted_access
				NodeServiceHelper.validatePermissionRestrictedAccess(
						new org.alfresco.service.cmr.repository.NodeRef(new StoreRef(storeProtocol, storeId), sourceId),
						CCConstants.PERMISSION_READ_ALL);
				source[0] = sourceDao.getReferenceOriginalId();
				return AuthenticationUtil.runAsSystem(work);
			} else {
				return work.doWork();
			}

		}catch(Throwable t){
			throw DAOException.mapping(t);
		}
	}

	public String storeXApiData(String xApi) throws DAOException {
		try {
			JSONObject json = new JSONObject(xApi);
			return XApiTool.sendToXApi(nodeId,json);
		} catch (Throwable t) {
			throw DAOException.mapping(t);
		}
	}
	public static List<NodeRef> getFrontpageNodes(RepositoryDao repoDao) throws DAOException {
		try {
			return NodeServiceFactory.getNodeService(repoDao.getId()).
					getFrontpageNodes().stream().map((ref)->new NodeRef(repoDao,ref.getId())).collect(Collectors.toList());
		}catch(Throwable t){
			throw DAOException.mapping(t);
		}
	}

	/**
	 * If the NodeDao is a child, then all properties including inherited are returned
	 * otherwise, the own properties are returned
	 */
	public HashMap<String, Object> getInheritedPropertiesFromParent() throws Throwable {
		if(getAspectsNative().contains(CCConstants.CCM_ASPECT_IO_CHILDOBJECT)){
			Map<String, Object> propsChild = getNativeProperties();
			String parentRef = NodeServiceFactory.getLocalService().getPrimaryParent(getRef().getId());
			HashMap<String,Object> propsParent =
					NodeServiceHelper.getProperties(new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, parentRef));
			// ignore some technical properties, like mimetypes etc.
			for(String prop : CCConstants.CHILDOBJECT_IGNORED_PARENT_PROPERTIES)
				propsParent.remove(prop);
			// override it with the props from the child
			for(Map.Entry<String,Object> entry : propsChild.entrySet()){
				if(entry.getValue() != null && !entry.getValue().toString().isEmpty()) {
					propsParent.put(entry.getKey(), entry.getValue());
				}
			}
			return propsParent;
		} else {
			return getNativeProperties();
		}
	}

	public NodeDao publishCopy(HandleMode handleMode) throws DAOException {
		try {
			return NodeDao.getNode(repoDao, nodeService.publishCopy(nodeId, handleMode), Filter.createShowAllFilter());
		}catch(Throwable t){
			throw DAOException.mapping(t);
		}

	}

	public List<NodeDao> getPublishedCopies() throws DAOException {
		try {
			return nodeService.getPublishedCopies(nodeId).stream().map(
					(id) -> {
						try {
							return NodeDao.getNode(repoDao, id, Filter.createShowAllFilter());
						} catch (DAOException e) {
							throw new RuntimeException(e);
						}
					}
			).sorted((a,b) -> {
				try {
					String[] va=((String) a.getNativeProperties().get(CCConstants.LOM_PROP_LIFECYCLE_VERSION)).split("\\.");
					String[] vb=((String) b.getNativeProperties().get(CCConstants.LOM_PROP_LIFECYCLE_VERSION)).split("\\.");
					for(int i=0;i<va.length;i++){
						int c = Integer.compare(Integer.parseInt(va[i]), Integer.parseInt(vb[i]));
						if(c!=0) {
							return c;
						}
					}
					return 0;
				} catch (Throwable ignored) {
					return 0;
				}
			}).collect(Collectors.toList());
		}catch(RuntimeException e){
			throw DAOException.mapping(e.getCause());
		}
	}
}
