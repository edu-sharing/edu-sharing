package org.edu_sharing.service.remote;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.nodeservice.NodeServiceImpl;
import org.edu_sharing.service.permission.PermissionServiceFactory;

public class RemoteObjectService {

	Logger logger = Logger.getLogger(RemoteObjectService.class);

	/**
	 * creates and returns remoteObjectId when it's a 3dParty repo
	 * returns nodeId if its homeRepo
	 * @param repositoryId
	 * @param nodeId
	 * @return
	 */
	public String getRemoteObject(String repositoryId, String nodeId) {

		return AuthenticationUtil.runAsSystem(new RunAsWork<String>() {

			@Override
			public String doWork() throws Exception {
				try {
					ApplicationInfo repInfo = ApplicationInfoList.getRepositoryInfoById(repositoryId);
					if (is3dPartyRepository(repInfo)) {
						logger.info("repository " + repInfo.getAppId() + " is not HomeNode and No Alfresco");

						MCAlfrescoBaseClient mcAlfrescoBaseClient = new MCAlfrescoAPIClient();

						org.edu_sharing.service.permission.PermissionService permissionService = PermissionServiceFactory
								.getPermissionService(ApplicationInfoList.getHomeRepository().getAppId());
						String remoteObjectFolderId = null;
						remoteObjectFolderId = getRemoteObjectsFolder();
		
						// schau nach ob remote object schon existiert
						mcAlfrescoBaseClient.setResolveRemoteObjects(false);
						HashMap<String, Object> remoteObjectProps = mcAlfrescoBaseClient.getChild(remoteObjectFolderId,
								CCConstants.CCM_TYPE_REMOTEOBJECT, CCConstants.CCM_PROP_REMOTEOBJECT_NODEID, nodeId);

						String remoteObjectNodeId = null;
						if (remoteObjectProps == null) {
							logger.info("found no remote object for remote node id:" + nodeId + " creating new one");
							// create RemoteObject as system
							remoteObjectProps = new HashMap<>();
							remoteObjectProps.put(CCConstants.CCM_PROP_REMOTEOBJECT_NODEID, nodeId);
							remoteObjectProps.put(CCConstants.CCM_PROP_REMOTEOBJECT_REPOSITORY_TYPE,
									repInfo.getRepositoryType());
							remoteObjectProps.put(CCConstants.CCM_PROP_REMOTEOBJECT_REPOSITORYID, repInfo.getAppId());

							remoteObjectNodeId = NodeServiceFactory.getLocalService().createNodeBasic(remoteObjectFolderId,
									CCConstants.CCM_TYPE_REMOTEOBJECT, remoteObjectProps);
							NodeService nodeService = NodeServiceFactory.getNodeService(repInfo.getAppId());
							InputStream content = nodeService.getContent(StoreRef.PROTOCOL_WORKSPACE,
									StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId,
									null,CCConstants.CM_PROP_CONTENT);
							HashMap<String, Object> properties = nodeService.getPropertiesPersisting(StoreRef.PROTOCOL_WORKSPACE,
									StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId);
							if (content != null) {
								// Store content from remote repo in node
								NodeServiceFactory.getLocalService().writeContent(
										StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, remoteObjectNodeId, content,
										(String) properties.get(CCConstants.LOM_PROP_TECHNICAL_FORMAT), "UTF-8",
										CCConstants.CM_PROP_CONTENT);
							}
						} else {
							remoteObjectNodeId = (String) remoteObjectProps.get(CCConstants.SYS_PROP_NODE_UID);
						}

						// CLEANUP english?
						// read rechte für den eigentlichen user auf das remote Object
						// setzen, damit render service das checken kann

						String userName = (String) Context.getCurrentInstance().getRequest().getSession()
								.getAttribute(CCConstants.AUTH_USERNAME);
						permissionService.setPermissions(remoteObjectNodeId, userName,
								new String[] { CCConstants.PERMISSION_ALL, CCConstants.PERMISSION_CC_PUBLISH }, true);

						return remoteObjectNodeId;

					} else {
						return nodeId;
					}

				} catch (Throwable t) {
					throw new Exception(t);
				}

			}
		});

	}
	
	public HashMap<String,Object> getRemoteObjectProperties(String repositoryId, String nodeId){
		try {
			String tmpNodeId = getRemoteObject(repositoryId, nodeId);
			if(nodeId.equals(tmpNodeId)) {
				return NodeServiceFactory.getNodeService(repositoryId).getProperties(MCAlfrescoAPIClient.storeRef.getProtocol(), MCAlfrescoAPIClient.storeRef.getIdentifier(), tmpNodeId);	
			}else {
				HashMap<String,Object> props = new NodeServiceImpl(repositoryId).getProperties(MCAlfrescoAPIClient.storeRef.getProtocol(), MCAlfrescoAPIClient.storeRef.getIdentifier(), tmpNodeId);
				props.putAll(NodeServiceFactory.getNodeService(repositoryId).getProperties(MCAlfrescoAPIClient.storeRef.getProtocol(), MCAlfrescoAPIClient.storeRef.getIdentifier(), tmpNodeId));
				return props;
			}
		}catch(Throwable e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}

	private String getRemoteObjectsFolder() throws Throwable, Exception {

		MCAlfrescoAPIClient mcAlfrescoBaseClient = new MCAlfrescoAPIClient();

		String remoteObjectFolderId = null;
		String companyHomeId = mcAlfrescoBaseClient.getCompanyHomeNodeId();
		HashMap<String, Object> defaultRemoteFolderProps = mcAlfrescoBaseClient.getChild(companyHomeId,
				CCConstants.CCM_TYPE_MAP, CCConstants.CM_NAME, CCConstants.CC_DEFAULT_REMOTEOBJECT_FOLDER_NAME);
		if (defaultRemoteFolderProps == null) {

			HashMap newDefaultRemoteFolderProps = new HashMap();
			newDefaultRemoteFolderProps.put(CCConstants.CM_NAME, CCConstants.CC_DEFAULT_REMOTEOBJECT_FOLDER_NAME);
			newDefaultRemoteFolderProps.put(CCConstants.CM_PROP_C_TITLE, CCConstants.CC_DEFAULT_REMOTEOBJECT_FOLDER_NAME);
			String newRemoteFolderId = mcAlfrescoBaseClient.createNode(companyHomeId, CCConstants.CCM_TYPE_MAP, newDefaultRemoteFolderProps);

			if (newRemoteFolderId != null) {
				remoteObjectFolderId = newRemoteFolderId;
			} else {
				logger.error("Could not create default Data Folder");
			}
		} else {
			remoteObjectFolderId = (String) defaultRemoteFolderProps.get(CCConstants.SYS_PROP_NODE_UID);
		}
		return remoteObjectFolderId;

	}

	private boolean is3dPartyRepository(ApplicationInfo repInfo) {
		return !repInfo.ishomeNode() && !repInfo.isRemoteAlfresco();
	}

	public String getOrCreateRemoteMetadataObject(String sourceRepositoryId, String originalNodeId) throws Throwable {
		String ROOT_PATH = "/app:company_home/ccm:remote_ios";
		ApplicationInfo repInfo = ApplicationInfoList.getRepositoryInfoById(sourceRepositoryId);
		NodeService nsSourceRepo = NodeServiceFactory.getNodeService(sourceRepositoryId);
		HashMap<String, Object> propsIn = nsSourceRepo.getPropertiesPersisting(null, null, originalNodeId);
		if(propsIn == null || propsIn.size() == 0) {
			throw new Exception("no properties found for source nodeId:" + originalNodeId + ", appId: " + sourceRepositoryId);
		}
		if(propsIn.containsKey(CCConstants.CM_NAME)) {
			propsIn.put(CCConstants.CM_NAME, NodeServiceHelper.cleanupCmName((String) propsIn.get(CCConstants.CM_NAME)));
		}
		// set the wwwurl so that the rendering will redirect to the source
		// @TODO: Check behaviour for each connected repository type
		// propsIn.put(CCConstants.CCM_PROP_IO_WWWURL, propsIn.get(CCConstants.LOM_PROP_TECHNICAL_LOCATION));

		// set the metadataset to keep the rendering of metadata consistent
		propsIn.put(CCConstants.CM_PROP_METADATASET_EDU_METADATASET, repInfo.getMetadatsetsV2()[0]);
		// We also need to store repository information for remote edu-sharing objects
		propsIn.put(CCConstants.CCM_PROP_REMOTEOBJECT_NODEID, originalNodeId);
		propsIn.put(CCConstants.CCM_PROP_REMOTEOBJECT_REPOSITORY_TYPE, repInfo.getRepositoryType());
		propsIn.put(CCConstants.CCM_PROP_REMOTEOBJECT_REPOSITORYID, repInfo.getAppId());
		// remove illegal data
		HashMap<String, Object> props = cleanupRemoteProperties(propsIn);
		return AuthenticationUtil.runAsSystem(() -> {
			try {
				Map<String, Object> searchProps = new HashMap<>();
				searchProps.put(CCConstants.CCM_PROP_REMOTEOBJECT_NODEID, originalNodeId);
				searchProps.put(CCConstants.CCM_PROP_REMOTEOBJECT_REPOSITORYID, repInfo.getAppId());
				NodeService nodeService = NodeServiceFactory.getLocalService();
				String root = NodeServiceHelper.getContainerRootPath(ROOT_PATH);
				// allow everyone to cc publish from this folder
				PermissionServiceFactory.getLocalService().setPermissions(root, CCConstants.AUTHORITY_GROUP_EVERYONE,
						new String[]{CCConstants.PERMISSION_CONSUMER, CCConstants.PERMISSION_CC_PUBLISH}, false);
				List<NodeRef> nodes = NodeServiceHelper.findNodeByPropertiesRecursive(
								new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,
								root),
								Collections.singletonList(CCConstants.CCM_TYPE_IO),
								searchProps);
				if(nodes.size()>1){
					throw new Exception("For remote node "+originalNodeId+" where found "+nodes.size()+" local objects, invalid state!");
				}
				if(nodes.size()==0) {
					// create
					String containerId = NodeServiceHelper.getContainerIdByPath(ROOT_PATH, "yyyy/MM/dd");
					return nodeService.createNodeBasic(containerId, CCConstants.CCM_TYPE_IO, props);
				}
				else{
					// update in case metadata of remote source have changed
					nodeService.updateNodeNative(nodes.get(0).getId(), props);
					return nodes.get(0).getId();
				}
			}catch(Throwable t){
				throw new RuntimeException(t);
			}
		});
	}

	public static HashMap<String, Object> cleanupRemoteProperties(HashMap<String, Object> propsIn) {
		HashMap<String, Object> props=new HashMap<>(propsIn);
		props.remove(CCConstants.SYS_PROP_NODE_UID);
		props.remove(CCConstants.NODECREATOR_EMAIL);
		props.remove(CCConstants.NODECREATOR_FIRSTNAME);
		props.remove(CCConstants.NODECREATOR_LASTNAME);
		props.remove(CCConstants.NODEMODIFIER_EMAIL);
		props.remove(CCConstants.NODEMODIFIER_FIRSTNAME);
		props.remove(CCConstants.NODEMODIFIER_LASTNAME);
		props.remove(CCConstants.CONTENTURL);
		for(Map.Entry<String, Object> prop: propsIn.entrySet()){
			if(prop.getKey().startsWith("{virtualproperty}")) {
				props.remove(prop.getKey());
			}
		}
		return props;
	}
}
