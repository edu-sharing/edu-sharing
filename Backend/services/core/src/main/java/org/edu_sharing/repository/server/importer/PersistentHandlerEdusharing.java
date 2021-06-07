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
package org.edu_sharing.repository.server.importer;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.DuplicateChildNodeNameException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.tools.EduSharingNodeHelper;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.jobs.quartz.AbstractJob;
import org.edu_sharing.repository.server.jobs.quartz.OAIConst;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.nodeservice.RecurseMode;
import org.springframework.context.ApplicationContext;

public class PersistentHandlerEdusharing implements PersistentHandlerInterface {

	private final AbstractJob job;
	private final boolean hasTimestampMap;

	MCAlfrescoBaseClient mcAlfrescoBaseClient = null;

	// for checking if node already excists
	List<NodeRef> allNodesInImportfolder = null;

	//
	HashMap<String, String> replIdTimestampMap = null;
	HashMap<String, NodeRef> replIdMap = null;

	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:sss");
	
	
	static ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	static ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	private String importFolderId;
	private Importer importer;

	public PersistentHandlerEdusharing(AbstractJob job,Importer importer,boolean buildCaches) throws Throwable {
		mcAlfrescoBaseClient = new MCAlfrescoAPIClient();
		this.job = job;
		this.importFolderId=prepareImportFolder();
		this.importer=importer;
		this.hasTimestampMap=buildCaches;
		// prepare cache
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
		serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(() -> {
			getAllNodesInImportfolder();
			getReplicationIdTimestampMap();
			return null;
		});
	}
	public Logger getLogger(){
		if(job != null) {
			return Logger.getLogger(job.getClass());
		}else{
			return Logger.getLogger(PersistentHandlerEdusharing.class);
		}
    }
	public static Logger getLogger(AbstractJob job){
		return Logger.getLogger(job.getClass());
	}

	public static void removeAllImportedObjects(AbstractJob job) throws Throwable {
		MCAlfrescoAPIClient mcAlfrescoBaseClient = new MCAlfrescoAPIClient();
		String importFolder = prepareImportFolder();
		if (importFolder != null) {
			HashMap children = (HashMap) mcAlfrescoBaseClient.getChildren(importFolder);
			if (children != null) {
				for (Object setKey : children.keySet()) {
					if(job!=null && job.isInterrupted()){
						getLogger(job).info("Job is aborted");
						return;
					}
					String name = (String) ((HashMap) children.get(setKey)).get(CCConstants.CM_NAME);
					getLogger(job).info("removing set:" + name);
					HashMap setCursorFolders = (HashMap) mcAlfrescoBaseClient.getChildren((String) setKey);
					TransactionService transactionService = serviceRegistry.getTransactionService();
					transactionService.getRetryingTransactionHelper().doInTransaction(()-> {
								for (Object setCursorfolderId : setCursorFolders.keySet()) {
									HashMap setCursorFolderProps = (HashMap) setCursorFolders.get(setCursorfolderId);
									String setCursorFolderName = (String) setCursorFolderProps.get(CCConstants.CM_NAME);
									getLogger(job).info("removing cursor folder:" + setCursorFolderName + " (set:" + name + ")");
									mcAlfrescoBaseClient.removeNode((String) setCursorfolderId, (String) setKey, false);
								}
								return null;
							},false);
					// mcAlfrescoBaseClient.removeNode( (String)setKey,importFolderNodeId);
				}
			} else {
				getLogger(job).info("importFolder has no children");
			}
		} else {
			getLogger(job).info("no importFolder available");
		}
	}

	public String safe(RecordHandlerInterfaceBase recordHandler, String cursor, String set) throws Throwable {
		HashMap<String, Object> newNodeProps = recordHandler.getProperties();
		String replicationId = (String) newNodeProps.get(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID);
		String lomCatalogId = (String) newNodeProps.get(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE);

		if (replicationId == null || replicationId.trim().equals("")) {
			getLogger().error("no replicationId in newNodeProps provided. will not safe/modify record.");
			return null;
		}

		if (lomCatalogId == null || lomCatalogId.trim().equals("")) {
			getLogger().error("no lomCatalogId in newNodeProps provided. will not safe/modify record.");
			return null;
		}
		String targetFolder = createFolderStructure(cursor, set);


		// watch out if object was already imported
		// HashMap<String, HashMap<String, Object>> alfResult =
		// mcAlfrescoBaseClient.search("@ccm\\:replicationsourceid:"+replicationId+" AND @ccm\\:replicationsource:"+lomCatalogId,
		// CCConstants.CCM_TYPE_IO);
		// HashMap searchProps = new HashMap();
		// searchProps.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE, lomCatalogId);
		// searchProps.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID, replicationId);
		// HashMap<String,Object> childProps = mcAlfrescoBaseClient.getChildRecursive(oaiImportBasefolder, CCConstants.CCM_TYPE_IO, searchProps);
		String nodeReplId=lomCatalogId+":"+replicationId;
		NodeRef childId = getNodeIfExists(nodeReplId,targetFolder);
		getLogger().debug("child id "+nodeReplId+": "+childId);

		String newTimeStamp = (String) newNodeProps.get(CCConstants.CCM_PROP_IO_REPLICATIONSOURCETIMESTAMP);
		if (childId != null) {

			// update
			/*
			getLogger().info("found one local Object for: Id:" + replicationId + " catalog:" + lomCatalogId + " childProps:"
					+ childProps.get(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE) + " " + childProps.get(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID)
					+ " " + childProps.get(CCConstants.SYS_PROP_NODE_UID));
					*/
			String oldTimeStamp = NodeServiceHelper.getProperty(childId,CCConstants.CCM_PROP_IO_REPLICATIONSOURCETIMESTAMP);

			String oldLicenseValid = NodeServiceHelper.getProperty(childId,CCConstants.CCM_PROP_IO_LICENSE_VALID);
			String newLicenseValid = (String) newNodeProps.get(CCConstants.CCM_PROP_IO_LICENSE_VALID);
			boolean licenseValidChanged = false;
			if ((oldLicenseValid == null && newLicenseValid != null) || (oldLicenseValid != null && !oldLicenseValid.equals(newLicenseValid))) {
				licenseValidChanged = true;
			}

			if (mustBePersisted(childId, replicationId,newTimeStamp)) {
				getLogger().info(" newTimeStamp "+newTimeStamp+" is after oldTimeStamp "+oldTimeStamp+" have to update object id:" + replicationId);
                updateNode((String) childId.getId(), newNodeProps, recordHandler.getPropertiesToRemove());
                setModifiedDate((String) childId.getId(), newNodeProps);
            } else if (licenseValidChanged) {
				getLogger().info(" license valid changed. have to update object. oldLicenseValid:" + oldLicenseValid + " newLicenseValid:"
						+ newLicenseValid);
                updateNode((String) childId.getId(), newNodeProps, recordHandler.getPropertiesToRemove());
                setModifiedDate((String) childId.getId(), newNodeProps);
            } else {
				getLogger().debug(" newTimeStamp.equals(oldTimeStamp) I'll do nothing");
			}
			return (String) childId.getId();

		} else {
			// insert
			String nodeId;
			getLogger().debug("found no local Object for: Id:" + replicationId + " catalog:" + lomCatalogId + " creating new one");
			nodeId=createNode(targetFolder, CCConstants.CCM_TYPE_IO, CCConstants.CM_ASSOC_FOLDER_CONTAINS, newNodeProps);
			setModifiedDate(nodeId,newNodeProps);
			// add it to the replication id map (full catalog + repl id)
			replIdMap.put(nodeReplId,new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId));
			// and the timestamp map (only replication id)
			replIdTimestampMap.put(replicationId,newTimeStamp);
			return nodeId;
			
		}
		
	}
    private Map<String,String> importFolderCursorIds=new HashMap<>();
	private synchronized String createFolderStructure(String cursor, String set) throws Throwable {
        if (set == null || set.trim().equals("")) {
            set = "unknownset";
        }

        // replace evil chars
        set = set.replace(":", "_");

        if (cursor == null || cursor.trim().equals("")) {
            cursor = "last";
        }
	    String searchId=set+":"+cursor;
	    // use cache to prevent calling alfresco getChild multiple times
	    if(importFolderCursorIds.containsKey(searchId))
	        return importFolderCursorIds.get(searchId);

		String folderId=importFolderId;
        HashMap<String, Object> setChild = mcAlfrescoBaseClient.getChild(folderId, CCConstants.CCM_TYPE_MAP, CCConstants.CM_NAME, set);
        if (setChild == null) {
            HashMap newimportFolderProps = new HashMap();
            newimportFolderProps.put(CCConstants.CM_NAME, set);
            newimportFolderProps.put(CCConstants.CM_PROP_C_TITLE, set);
            getLogger().info("Folder for set "+set+" does not yet exists, will be created");
            folderId = mcAlfrescoBaseClient.createNode(folderId, CCConstants.CCM_TYPE_MAP, newimportFolderProps);
        } else {
            folderId = (String) setChild.get(CCConstants.SYS_PROP_NODE_UID);
        }



		HashMap<String, Object> cursorChild = mcAlfrescoBaseClient.getChild(folderId, CCConstants.CCM_TYPE_MAP, CCConstants.CM_NAME, cursor);
		if (cursorChild == null) {
			HashMap newimportFolderProps = new HashMap();
			newimportFolderProps.put(CCConstants.CM_NAME, cursor);
			newimportFolderProps.put(CCConstants.CM_PROP_C_TITLE, cursor);
			getLogger().info("Folder for set "+set+" with cursor "+cursor+" does not yet exists, will be created");
			folderId = mcAlfrescoBaseClient.createNode(folderId, CCConstants.CCM_TYPE_MAP, newimportFolderProps);
		} else {
			folderId = (String) cursorChild.get(CCConstants.SYS_PROP_NODE_UID);
		}
		importFolderCursorIds.put(searchId,folderId);
		return folderId;
	}

	public static String prepareImportFolder() throws Throwable {
		MCAlfrescoAPIClient mcAlfrescoBaseClient = new MCAlfrescoAPIClient();
		String companyHomeId = mcAlfrescoBaseClient.getCompanyHomeNodeId();
		HashMap<String, Object> importFolderProps = mcAlfrescoBaseClient.getChild(companyHomeId, CCConstants.CCM_TYPE_MAP, CCConstants.CM_NAME,
				OAIPMHLOMImporter.FOLDER_NAME_IMPORTED_OBJECTS);
		String importFolderId;
		if (importFolderProps == null) {
			HashMap newimportFolderProps = new HashMap();
			newimportFolderProps.put(CCConstants.CM_NAME, OAIPMHLOMImporter.FOLDER_NAME_IMPORTED_OBJECTS);
			newimportFolderProps.put(CCConstants.CM_PROP_C_TITLE, OAIPMHLOMImporter.FOLDER_NAME_IMPORTED_OBJECTS);
			importFolderId = mcAlfrescoBaseClient.createNode(companyHomeId, CCConstants.CCM_TYPE_MAP, newimportFolderProps);
		} else {
			importFolderId = (String) importFolderProps.get(CCConstants.SYS_PROP_NODE_UID);
		}
		return importFolderId;
	}

	private void setModifiedDate(String nodeId,Map newNodeProps) throws NotSupportedException, SystemException, IllegalStateException, SecurityException, HeuristicMixedException, HeuristicRollbackException, RollbackException {
		if(newNodeProps.containsKey(CCConstants.CM_PROP_C_MODIFIED)){
			NodeRef nodeRef=new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId);
			BehaviourFilter filter=(BehaviourFilter)AlfAppContextGate.getApplicationContext().getBean("policyBehaviourFilter");
			ServiceRegistry serviceRegistry = (ServiceRegistry) AlfAppContextGate.getApplicationContext().getBean(ServiceRegistry.SERVICE_REGISTRY);
			synchronized (this) {
				UserTransaction tx = serviceRegistry.getTransactionService().getUserTransaction();
				tx.begin();
				filter.disableBehaviour(nodeRef, ContentModel.ASPECT_AUDITABLE);

				mcAlfrescoBaseClient.setProperty(nodeId, CCConstants.CM_PROP_C_MODIFIED, (String) newNodeProps.get(CCConstants.CM_PROP_C_MODIFIED));
				filter.enableBehaviour(nodeRef, ContentModel.ASPECT_AUDITABLE);
				tx.commit();
			}
		}
	}

	private NodeRef getNodeIfExists(String nodeReplId,String importFolderId) throws Throwable {
		return replIdMap.getOrDefault(nodeReplId,null);
	}
	

	public List<NodeRef> getAllNodesInImportfolder() throws Throwable {
		String importFolder = prepareImportFolder();
		if (importFolder != null) {
			return this.getAllNodesInImportfolder(importFolder);
		} else {
			getLogger().info("returns importFolderProps == null");
			return null;
		}

	}
	public static int estimateObjectSize(Object o){
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutput out = new ObjectOutputStream(bos);
			out.writeObject(o);
			out.flush();
			return bos.size();
		}catch(Throwable t){
			return -1;
		}
	}
	public List<NodeRef> getAllNodesInImportfolder(String importFolderId) throws Throwable {
		if(!hasTimestampMap){
			allNodesInImportfolder=new ArrayList<>();
		}
		if (allNodesInImportfolder == null) {
			getLogger().info("allNodesInImportfolder is null starting to initialize it");
			allNodesInImportfolder = NodeServiceFactory.getLocalService().getChildrenRecursive(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,importFolderId, Collections.singletonList(CCConstants.CCM_TYPE_IO), RecurseMode.Folders);

			getLogger().info("allNodesInImportfolder initialize finished! size:" + ((allNodesInImportfolder != null) ? allNodesInImportfolder.size() : 0));
			getLogger().info("allNodesInImportfolder initialize finished! calculated size:" + ((allNodesInImportfolder != null) ? (estimateObjectSize(allNodesInImportfolder)/1024)+" kb" : 0));
		}
		return allNodesInImportfolder;
	}

	public void updateNode(String nodeId, Map props, List<String> propertiesToRemove) throws Throwable {
		// idea first delete all childs and create them new
        synchronized (this) {
            HashMap children = mcAlfrescoBaseClient.getChildren(nodeId);
            for (Object key : children.keySet()) {
                mcAlfrescoBaseClient.removeNode((String) key, nodeId, false);
            }
        }
		HashMap<String, Object> simpleProps = new HashMap<String, Object>();
		HashMap<String, Object> nodeProps = new HashMap<String, Object>();
		for (Object key : props.keySet()) {
			String propKey = (String) key;
			if (propKey.startsWith("TYPE#")) {
				nodeProps.put(propKey, props.get(propKey));
			} else {
				simpleProps.put(propKey, props.get(propKey));
			}
		}
		try {
			for (String prop : propertiesToRemove){
				NodeServiceFactory.getLocalService().removeProperty(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),nodeId,prop);
			}
		}catch(Throwable t) {
			getLogger().warn("failed to remove props from node "+nodeId);
		}

		try {
			mcAlfrescoBaseClient.updateNode(nodeId, simpleProps);
		}catch(DuplicateChildNodeNameException e){
			simpleProps.put(CCConstants.CM_NAME, EduSharingNodeHelper.makeUniqueName((String) simpleProps.get(CCConstants.CM_NAME)));
			mcAlfrescoBaseClient.updateNode(nodeId, simpleProps);
		}
		createChildobjects(nodeId, nodeProps);
	}

	private void createChildobjects(String nodeId, HashMap<String, Object> nodeProps) throws Throwable {
		if(importer!=null && !importer.getRecordHandler().createSubobjects()){
			return;
		}
		for (Object key : nodeProps.keySet()) {
			String typekey = (String) key;
			String[] splitted = typekey.split("#");
			if (splitted.length == 3) {
				String subNodeType = splitted[1];
				String subNodeAssociation = splitted[2];
				if (nodeProps.get(typekey) instanceof List) {
					List list = (List) nodeProps.get(typekey);
					for (Object listentry : list) {
						Map subNodeProps = (Map) listentry;
						createNode(nodeId, subNodeType, subNodeAssociation, subNodeProps);
					}
				} else {
					// it must be a Map
					Map subNodeProps = (Map) nodeProps.get(typekey);
					createNode(nodeId, subNodeType, subNodeAssociation, subNodeProps);
				}
			}
		}
	}

	public String createNode(String parentId, String type, String association, Map props) throws Throwable {
		HashMap<String, Object> simpleProps = new HashMap<String, Object>();
		HashMap<String, Object> nodeProps = new HashMap<String, Object>();
		String[] aspects=null;
		for (Object key : props.keySet()) {
			String propKey = (String) key;
			if(propKey.equals("ASPECTS")){
				aspects=(String[])props.get(propKey);
			}
			else if (propKey.startsWith("TYPE#")) {
				nodeProps.put(propKey, props.get(propKey));
			} else {
				simpleProps.put(propKey, props.get(propKey));
			}
		}

		String newNodeId;
		// do not auto create versions (otherwise the node will get several versions e.g. during binary handler or preview)
		simpleProps.put(CCConstants.CCM_PROP_IO_CREATE_VERSION,false);
		try {
			newNodeId = mcAlfrescoBaseClient.createNode(parentId, type, association, simpleProps);
		} catch (DuplicateChildNodeNameException e) {
			simpleProps.put(CCConstants.CM_NAME, EduSharingNodeHelper.makeUniqueName((String)simpleProps.get(CCConstants.CM_NAME)));
			newNodeId = mcAlfrescoBaseClient.createNode(parentId, type, association, simpleProps);
		}
		if (aspects != null) {
			for (String aspect : aspects) {
				mcAlfrescoBaseClient.addAspect(newNodeId, aspect);
			}
		}
		createChildobjects(newNodeId,nodeProps);

		return newNodeId;
	}
	
	public HashMap<String, NodeRef> getReplIdMap() {
		return replIdMap;
	}

	public HashMap<String, String> getReplicationIdTimestampMap() {
		if(!hasTimestampMap) {
			replIdMap = new HashMap<>();
			replIdTimestampMap=new HashMap<>();
		}
		if (replIdTimestampMap == null) {
			try {
				String user=AuthenticationUtil.getFullyAuthenticatedUser();
				List<NodeRef> allNodes = getAllNodesInImportfolder();
				replIdMap = new HashMap<>();
				replIdTimestampMap = new HashMap<>();
				// fetch data parallel for faster build up
				allNodes.parallelStream().forEach((entry)->{
					AuthenticationUtil.runAs(()-> {
						String replSource = NodeServiceHelper.getProperty(entry, CCConstants.CCM_PROP_IO_REPLICATIONSOURCE);
						String replSourceId = NodeServiceHelper.getProperty(entry, CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID);
						String timestamp = NodeServiceHelper.getProperty(entry, CCConstants.CCM_PROP_IO_REPLICATIONSOURCETIMESTAMP);
						replIdMap.put(replSource + ":" + replSourceId, entry);
						if (replSource != null && !replSource.trim().equals("") && timestamp != null && !timestamp.trim().equals("")) {
							replIdTimestampMap.put(replSourceId, timestamp);
						} else {
							getLogger().error("cannot add nodeId " + entry.getId() + " to replIdTimestampMap replId:" + replSourceId + " timestamp:" + timestamp);
						}
						return null;
					},user);
				});
				getLogger().info("Build timestamp map finished, size: "+replIdTimestampMap.size());

			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return replIdTimestampMap;
	}
	public synchronized boolean mustBePersisted(String replId, String timeStamp) {
		return mustBePersisted(null, replId, timeStamp);
	}

	/**
	 * checks if an repl object must be created or updated
	 * 
	 * @param replId
	 * @param timeStamp
	 * @return
	 */
	public synchronized boolean mustBePersisted(NodeRef childId, String replId, String timeStamp) {

		// we will not safe without replId
		if (replId == null) {
			return false;
		}
		if(childId != null) {
			String blocked = NodeServiceHelper.getProperty(childId, CCConstants.CCM_PROP_IO_IMPORT_BLOCKED);
			if (Boolean.parseBoolean(blocked)) {
				getLogger().info("Object id " + replId + " is blocked for import, skipping");
				return false;
			}
		}
		if(job != null &&
				job.getJobDataMap() != null
				&& job.getJobDataMap().getBoolean(OAIConst.PARAM_FORCE_UPDATE)) {
			return true;
		}
		String oldTimeStamp = getReplicationIdTimestampMap().get(replId);

		// we will not safe without timestamp
		if (timeStamp == null || timeStamp.isEmpty()) {
			return false;
		}

		// does not exist
		if (oldTimeStamp == null || oldTimeStamp.isEmpty()) {
			getLogger().debug("oldTimeStamp is null or empty. returning true");
			return true;
		}

		Date newDate = null;
		try {
			newDate = sdf.parse(timeStamp);
			Date oldDate = sdf.parse(oldTimeStamp);
			if (newDate.after(oldDate)) {
				getLogger().debug("newDate.after(oldDate) newDate:" + newDate+ " oldDate:"+ oldDate+" returning true");
				return true;
			}

		} catch (ParseException e) {
			getLogger().error(e.getMessage() + " while comparing old and new timestamp for id:" + replId + " oldTimeStamp:" + oldTimeStamp
					+ " newTimeStamp:" + timeStamp);

			// if old date was damaged but new date is ok
			if (newDate != null) {
				getLogger().debug("old date was damaged but new date is ok. returning true");
				return true;
			}

		}
		catch(Throwable t){
			t.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean exists(String replId) {
		String oldTimeStamp = getReplicationIdTimestampMap().get(replId);

		// does not exist
		if (oldTimeStamp == null || oldTimeStamp.isEmpty()) {
			return false;
		}else {
			return true;
		}

	}
}
