package org.edu_sharing.service.nodeservice;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang3.NotImplementedException;
import org.edu_sharing.repository.client.rpc.User;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.UrlTool;
import org.edu_sharing.repository.client.tools.metadata.ValueTool;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.service.nodeservice.model.GetPreviewResult;
import org.edu_sharing.service.permission.HandleMode;
import org.edu_sharing.service.search.model.SortDefinition;
import org.springframework.extensions.surf.util.URLEncoder;

public class NodeServiceAdapter implements NodeService {
	
	protected String appId;

	public NodeServiceAdapter(String appId) {
		this.appId=appId;
	}

	@Override
	public void updateNode(String nodeId, HashMap<String, String[]> props) throws Throwable {
	}

	@Override
	public void createAssoc(String parentId, String childId, String assocName) {

	}

	@Override
	public String createNode(String parentId, String nodeType, HashMap<String, String[]> props) throws Throwable {
		return null;
	}

	@Override
	public String createNodeBasic(String parentID, String nodeTypeString, HashMap<String, ?> _props) {
		return null;
	}

	@Override
	public String findNodeByName(String parentId, String name) {
		return null;
	}

	@Override
	public NodeRef copyNode(String sourceId, String nodeId, boolean withChildren) throws Throwable {
		return null;
	}

	@Override
	public String getCompanyHome() {
		return null;
	}

	@Override
	public HashMap<String, String[]> getNameProperty(String name) {
		return null;
	}

	@Override
	public List<NodeRef> getChildrenRecursive(StoreRef store, String nodeId, List<String> types, RecurseMode recurseMode) {
		return null;
	}

	@Override
    public NodeRef getChild(StoreRef store, String parentId, String type, String property, Serializable value) {
        return null;
    }

    @Override
	public void setOwner(String nodeId, String username) {
	}

	@Override
	public void setPermissions(String nodeId, String authority, String[] permissions, Boolean inheritPermission)
			throws Exception {
	}

	@Override
	public String getOrCreateUserInbox() {
		return null;
	}

	@Override
	public void createVersion(String nodeId) throws Exception {
	}

	@Override
	public void deleteVersionHistory(String nodeId) throws Exception {

	}

	@Override
	public void writeContent(StoreRef store, String nodeID, InputStream content, String mimetype, String _encoding,
			String property) throws Exception {
	}

	@Override
	public void removeNode(String nodeID, String fromID) {	
	}

	@Override
	public HashMap<String, Object> getProperties(String storeProtocol, String storeId, String nodeId) throws Throwable {
		return new HashMap<String,Object>();
	}

	@Override
	public HashMap<String, Object> getPropertiesDynamic(String storeProtocol, String storeId, String nodeId) throws Throwable {
		return new HashMap<String,Object>();
	}

	@Override
	public HashMap<String, Object> getPropertiesPersisting(String storeProtocol, String storeId, String nodeId) throws Throwable {
		return getProperties(storeProtocol, storeId, nodeId);
	}

	
	@Override
	public void addAspect(String nodeId, String aspect) {
	}
	
	@Override
	public String[] getAspects(String storeProtocol, String storeId, String nodeId) {
		return null;
	}
	
	@Override
	public void moveNode(String newParentId, String childAssocType, String nodeId) {
	}
	
	@Override
	public void revertVersion(String nodeId, String verLbl) throws Exception {
	}
	
	@Override
	public HashMap<String, HashMap<String, Object>> getVersionHistory(String nodeId) throws Throwable {
		return null;
	}
	
	private List<String> getPropertyValues(ValueTool vt,Object value) {
		List<String> values = new ArrayList<String>();
		if (value != null ){
			for (String mv : vt.getMultivalue(value.toString())) {
				values.add(mv);
			}
		}
		return values;
	}
	
	private HashMap<String,String[]> convertProperties(HashMap<String,Object> propsIn){
		ValueTool vt = new ValueTool();
		HashMap<String,String[]> properties = new HashMap<String,String[]>();
		for (Entry<String, Object> entry : propsIn.entrySet()) {
			List<String> values = getPropertyValues(vt, entry.getValue());
			properties.put(entry.getKey(), values.toArray(new String[values.size()]));
		}
		return properties;
	}
	
	// to be overwritten if necessary
	public InputStream getContent(String nodeId) throws Throwable{
		throw new Exception("getContent not implemented for this repository");
	}
	
	/**
	 * Import the node from a foreign repository to the local one
	 * @param localParent
	 * @return
	 * @throws Exception 
	 */
	@Override
	public String importNode(String nodeId,String localParent) throws Throwable {
		HashMap<String, Object> props = getProperties(null, null, nodeId);
		String mimetype=null;
		if(props.containsKey(CCConstants.LOM_PROP_TECHNICAL_FORMAT))
			mimetype= (String) props.get(CCConstants.LOM_PROP_TECHNICAL_FORMAT);
		InputStream content=getContent(nodeId);
		if(content==null)
			throw new IllegalArgumentException("The remote service did not provide any data to import");
		NodeService service=NodeServiceFactory.getLocalService();
		
		//fix name
		String name = (String) props.get(CCConstants.CM_NAME);
		name = NodeServiceHelper.cleanupCmName(name);
		props.put(CCConstants.CM_NAME, name);
		
		//preview
		String thumbnail = (String)props.get(CCConstants.CM_ASSOC_THUMBNAILS);
		if(thumbnail != null) {
			props.put(CCConstants.CCM_PROP_IO_THUMBNAILURL, thumbnail);
		}

		// Aspect ccm:imported_object properties
		props.put(CCConstants.CCM_PROP_IMPORTED_OBJECT_NODEID,props.get(CCConstants.SYS_PROP_NODE_UID));
		props.put(CCConstants.CCM_PROP_IMPORTED_OBJECT_APPID,appId);
		props.put(CCConstants.CCM_PROP_IMPORTED_OBJECT_APPNAME,ApplicationInfoList.getRepositoryInfoById(appId).getAppCaption());

		props.remove(CCConstants.SYS_PROP_NODE_UID);
		props.remove(CCConstants.CM_PROP_C_CREATED);
		props.remove(CCConstants.CM_PROP_C_MODIFIED);

		String localNode=service.createNodeBasic(localParent, CCConstants.CCM_TYPE_IO,props);
		if(content != null) {
			service.writeContent(new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore"), localNode, content,mimetype, null, CCConstants.CM_PROP_CONTENT);
		}
		return localNode;
	}
	
	@Override
	public User getOwner(String storeId, String storeProtocol, String nodeId) {
		return null;
	}

	@Override
	public String createNode(String parentId, String nodeType, HashMap<String, String[]> props, String childAssociation)
			throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream getContent(String storeProtocol, String storeId, String nodeId, String version, String contentProp) throws Throwable {
		return getContent(nodeId);
	}

	@Override
	public String getContentHash(String storeProtocol, String storeId, String nodeId, String version, String contentProp) {
		return null;
	}

	@Override
	public void removeNode(String nodeId, String parentId, boolean recycle) {
	}
	
	@Override
	public void removeNode(String potocol, String store, String nodeId) {	
	}

	@Override
	public String getOrCreateUserSavedSearch() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeAspect(String nodeId, String aspect) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateNodeNative(String nodeId, HashMap<String, ?> _props) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeProperty(String storeProtocol, String storeId, String nodeId, String property) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String createNodeBasic(StoreRef store, String parentID, String nodeTypeString, String childAssociation,
			HashMap<String, ?> _props) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getType(String storeProtocol,String storeId,String nodeId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean exists(String protocol, String store, String nodeId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getTemplateNode(String nodeId,boolean create) throws Throwable {
		return null;
	}

	@Override
	public void setTemplateProperties(String nodeId, HashMap<String, String[]> stringHashMap) throws Throwable {

	}

	@Override
	public void setTemplateStatus(String nodeId, Boolean enable) throws Throwable {

	}

	@Override
	public String getContentMimetype(String protocol, String storeId, String nodeId) {
		return null;
	}

	@Override
	public String getPrimaryParent(String nodeId) {
		return null;
	}

	@Override
	public List<ChildAssociationRef> getChildrenChildAssociationRefType(String parentID, String childType) {
		return null;
	}

	@Override
	public List<AssociationRef> getNodesByAssoc(String nodeId, AssocInfo assoc) {
		return null;
	}

	@Override
	public List<ChildAssociationRef> getChildrenChildAssociationRefAssoc(String parentID, String asoocName, List<String> filter, SortDefinition sortDefinition) {
		return new ArrayList<>();
	}

	@Override
	public void setProperty(String protocol, String storeId, String nodeId, String property, Serializable value) {
		// TODO Auto-generated method stub

	}

	@Override
	public GetPreviewResult getPreview(String storeProtocol, String storeIdentifier, String nodeId, HashMap<String, Object> nodeProps, String version) {
	    try {
			String previewURL = URLTool.getBaseUrl(true);
			previewURL += "/preview?nodeId="+URLEncoder.encodeUriComponent(nodeId)+"&repository="+
					URLEncoder.encodeUriComponent(appId)+
					"&storeProtocol="+storeProtocol+"&storeId="+storeIdentifier+"&dontcache="+System.currentTimeMillis();
			if(version!=null){
				previewURL +="&version="+version;
			}
			previewURL =  URLTool.addOAuthAccessToken(previewURL);
            return new GetPreviewResult(previewURL, false);
        }catch(Throwable t){
	        return null;
        }
	}

	@Override
	public List<NodeRef> getFrontpageNodes() throws Throwable {
		return null;
	}

	@Override
	public Serializable getPropertyNative(String storeProtocol, String storeId, String nodeId, String property) throws Throwable {
		return null;
	}

	@Override
	public String publishCopy(String nodeId, HandleMode handleMode) throws Throwable {
		throw new NotImplementedException("publishCopy");
	}

	@Override
	public List<String> getPublishedCopies(String nodeId) {
		throw new NotImplementedException("getPublishedCopies");
	}

	@Override
	public <T> List<T> sortNodeRefList(List<T> list, List<String> filter, SortDefinition sortDefinition) {
		return list;
	}
	@Override
	public String getPrimaryParent(String protocol, String store, String nodeId) {
		return null;
	}

	@Override
	public void removeNodeForce(String storeProtocol, String storeId, String nodeId, boolean recycle) {

	}
}
