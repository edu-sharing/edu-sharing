package org.edu_sharing.service.permission;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.search.impl.lucene.SolrJSONResultSet;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.security.permissions.impl.acegi.FilteringResultSet;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ISO9075;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.log4j.Logger;
import org.apache.lucene.queryParser.QueryParser;
import org.edu_sharing.alfresco.service.OrganisationService;
import org.edu_sharing.alfresco.service.handleservice.HandleService;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.repository.client.rpc.ACE;
import org.edu_sharing.repository.client.rpc.ACL;
import org.edu_sharing.repository.client.rpc.Authority;
import org.edu_sharing.repository.client.rpc.Group;
import org.edu_sharing.repository.client.rpc.Notify;
import org.edu_sharing.repository.client.rpc.Result;
import org.edu_sharing.repository.client.rpc.User;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.*;
import org.edu_sharing.repository.server.tools.mailtemplates.MailTemplate;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.edu_sharing.service.collection.CollectionServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.oai.OAIExporterService;
import org.edu_sharing.alfresco.service.toolpermission.ToolPermissionException;
import org.edu_sharing.service.toolpermission.ToolPermissionService;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;
import org.edu_sharing.service.version.VersionTool;
import org.springframework.context.ApplicationContext;

import com.google.gson.Gson;

public class PermissionServiceImpl implements org.edu_sharing.service.permission.PermissionService {


	public static final String NODE_PUBLISHED = "NODE_PUBLISHED";
	// the maximal number of "notify" entries in the PH_HISTORY field that are serialized
	private static final int MAX_NOTIFY_HISTORY_LENGTH = 100;
	private NodeService nodeService = null;
	private PersonService personService;
	private ApplicationInfo appInfo;
	private ToolPermissionService toolPermission;

	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	OrganisationService organisationService = (OrganisationService)applicationContext.getBean("eduOrganisationService");
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	AuthorityService authorityService = serviceRegistry.getAuthorityService();
	MCAlfrescoAPIClient repoClient = new MCAlfrescoAPIClient();
	Logger logger = Logger.getLogger(PermissionServiceImpl.class);
	private PermissionService permissionService;

	public PermissionServiceImpl(String appId) {
		appInfo = ApplicationInfoList.getHomeRepository();
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext
				.getBean(ServiceRegistry.SERVICE_REGISTRY);

		nodeService = serviceRegistry.getNodeService();
		permissionService = serviceRegistry.getPermissionService();

		personService = serviceRegistry.getPersonService();

	}

	public PermissionServiceImpl() {
		this(ApplicationInfoList.getHomeRepository().getAppId());
	}
	
	public PermissionServiceImpl(boolean test) {
		
	}

	/**
	 * @TODO Thread safe / blocking for multiple users
	 * 
	 * @param nodeId
	 * @param aces
	 * @param inheritPermissions
	 * @param mailText
	 * @param sendMail
	 * @param sendCopy
	 */
	public void setPermissions(String nodeId, List<ACE> aces, Boolean inheritPermissions, String mailText, Boolean sendMail,
			Boolean sendCopy) throws Throwable {

		NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
		ACL currentACL = repoClient.getPermissions(nodeId);

		/**
		 * remove the inherited from the old and new
		 */
		List<ACE> acesNew = new ArrayList<ACE>(aces);
		acesNew=addCollectionCoordinatorPermission(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId),acesNew);
		Iterator<ACE> acesNewIter = acesNew.iterator();
		while (acesNewIter.hasNext()) {
			ACE ace = acesNewIter.next();
			if (ace.isInherited()) {
				acesNewIter.remove();
			}
		}

		List<ACE> acesOld = new ArrayList<ACE>(Arrays.asList(currentACL.getAces()));
		Iterator<ACE> acesOldIter = acesOld.iterator();
		while (acesOldIter.hasNext()) {
			ACE ace = acesOldIter.next();
			if (ace.isInherited()) {
				acesOldIter.remove();
			}
		}

		List<ACE> acesToAdd = new ArrayList<ACE>();
		List<ACE> acesToUpdate = new ArrayList<ACE>();
		List<ACE> acesToRemove = new ArrayList<ACE>();
		List<ACE> acesNotChanged = new ArrayList<ACE>();

		/**
		 * remove the ones that are already set (didn't change)
		 */
		Iterator<ACE> iteratorNew = acesNew.iterator();
		while (iteratorNew.hasNext()) {
			ACE ace = iteratorNew.next();
			if (acesOld.contains(ace)) {
				acesNotChanged.add(ace);
				iteratorNew.remove();
			}
		}

		List<String> aceOldAuthorityList = new ArrayList<String>();
		for (ACE aceOld : acesOld) {
			aceOldAuthorityList.add(aceOld.getAuthority());
		}
		for (ACE aceNew : acesNew) {
			if (aceOldAuthorityList.contains(aceNew.getAuthority())) {
				acesToUpdate.add(aceNew);
			} else {
				acesToAdd.add(aceNew);
			}
		}

		for (ACE aceOld : acesOld) {
			if (!acesToUpdate.contains(aceOld) && !acesNotChanged.contains(aceOld)) {
				acesToRemove.add(aceOld);
			}
		}

		boolean createNotify = false;
		if (acesToAdd.size() > 0) {
			HashMap<String, String[]> authPermissions = new HashMap<String, String[]>();
			for (ACE toAdd : acesToAdd) {
				String[] permissions = authPermissions.get(toAdd.getAuthority());
				if (permissions == null) {
					permissions = new String[] { toAdd.getPermission() };
				} else {
					ArrayList<String> plist = new ArrayList<String>(Arrays.asList(permissions));
					plist.add(toAdd.getPermission());
					permissions = plist.toArray(new String[plist.size()]);
				}
				authPermissions.put(toAdd.getAuthority(), permissions);
			}
			addPermissions(nodeId, authPermissions, inheritPermissions, mailText,
					sendMail, sendCopy);
		}

		if (acesToUpdate.size() > 0) {
			for (ACE toUpdate : acesToUpdate) {
				setPermissions(nodeId, toUpdate.getAuthority(), new String[] { toUpdate.getPermission() }, null);
			}
			createNotify = true;
		}

		if (acesToRemove.size() > 0) {
			for (ACE toRemove : acesToRemove) {
				removePermissions(nodeId, toRemove.getAuthority(), new String[] { toRemove.getPermission() });
			}
			createNotify = true;
		}

		if (inheritPermissions != null
				&& inheritPermissions.booleanValue() != repoClient.getPermissions(nodeId).isInherited()) {
			setPermissions(nodeId, null, null, inheritPermissions);
			createNotify = true;
		}

		if (createNotify) {
			createNotifyObject(nodeId, new AuthenticationToolAPI().getCurrentUser(),
					CCConstants.CCM_VALUE_NOTIFY_ACTION_PERMISSION_CHANGE);
		}

		if(nodeService.hasAspect(nodeRef,QName.createQName(CCConstants.CCM_ASPECT_COLLECTION))){
			CollectionServiceFactory.getCollectionService(appInfo.getAppId()).updateScope(nodeRef, aces);
		}


		OAIExporterService service = new OAIExporterService();
		if(service.available()) {
			boolean publishToOAI = false;

			List<String> licenseList = (List<String>)serviceRegistry.getNodeService().getProperty(new NodeRef(MCAlfrescoAPIClient.storeRef,nodeId), QName.createQName(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY));

			if(licenseList != null) {
				for(String license : licenseList) {
					if(license != null && license.startsWith("CC_")) {
						for(ACE ace : acesToAdd) {
							if(ace.getAuthorityType().equals(AuthorityType.EVERYONE.toString())) {
								publishToOAI = true;
							}
						}

						for(ACE ace : acesToUpdate) {
							if(ace.getAuthorityType().equals(AuthorityType.EVERYONE.toString())) {
								publishToOAI = true;
							}
						}

						for(ACE ace : acesNotChanged) {
							if(ace.getAuthorityType().equals(AuthorityType.EVERYONE.toString())) {
								publishToOAI = true;
							}
						}
					}
				}
			}
			if(publishToOAI) {
				service.export(nodeId);
			}
		}


	}

	@Override
	public void addPermissions(String _nodeId, HashMap<String, String[]> _authPerm, Boolean _inheritPermissions,
							   String _mailText, Boolean _sendMail, Boolean _sendCopy) throws Throwable {

		EmailValidator mailValidator = EmailValidator.getInstance(true, true);

		String currentLocale = new AuthenticationToolAPI().getCurrentLocale();

		// used for sending copy to user
		String copyMailText = "";

		String senderName = null;
		String senderFirstName = null, senderLastName = null;

		String user = new AuthenticationToolAPI().getCurrentUser();
		HashMap<String, String> senderInfo = repoClient.getUserInfo(user);
		if (senderInfo != null) {
			senderFirstName = senderInfo.get(CCConstants.CM_PROP_PERSON_FIRSTNAME);
			senderLastName = senderInfo.get(CCConstants.CM_PROP_PERSON_LASTNAME);
			if (senderFirstName != null && senderLastName != null) {
				senderName = senderFirstName + " " + senderLastName;
			} else {
				senderName = user;
			}
		}

		for (String authority : _authPerm.keySet()) {
			String[] permissions = _authPerm.get(authority);
			setPermissions(_nodeId, authority, permissions, _inheritPermissions);

			String emailaddress = null;
			String receiverFirstName = null, receiverLastName = null;

			AuthorityType authorityType = AuthorityType.getAuthorityType(authority);


			if (AuthorityType.USER.equals(authorityType)) {
				HashMap<String, String> personInfo = repoClient.getUserInfo(authority);

				if (personInfo != null) {
					receiverFirstName = personInfo.get(CCConstants.CM_PROP_PERSON_FIRSTNAME);
					receiverLastName = personInfo.get(CCConstants.CM_PROP_PERSON_LASTNAME);
					emailaddress = personInfo.get(CCConstants.CM_PROP_PERSON_EMAIL);
				}
				addToRecent(personService.getPerson(authority));
			}
			// send group email notifications
			if(AuthorityType.GROUP.equals(authorityType)){
				receiverLastName="";
				receiverFirstName= (String) nodeService.getProperty(authorityService.getAuthorityNodeRef(authority),QName.createQName(CCConstants.CM_PROP_AUTHORITY_AUTHORITYDISPLAYNAME));
				emailaddress= (String) nodeService.getProperty(authorityService.getAuthorityNodeRef(authority),QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPEMAIL));
				addToRecent(authorityService.getAuthorityNodeRef(authority));
			}


			if (mailValidator.isValid(emailaddress) && _sendMail) {
				Mail mail = new Mail();
				HashMap<String, Object> props = repoClient.getProperties(_nodeId);
				String nodeType = (String) props.get(CCConstants.NODETYPE);

				String name = null;
				if (nodeType.equals(CCConstants.CCM_TYPE_IO)) {
					name = (String) props.get(CCConstants.LOM_PROP_GENERAL_TITLE);
					name = (name == null || name.trim().isEmpty()) ? (String) props.get(CCConstants.CM_NAME) : name;
				} else {
					name = (String) props.get(CCConstants.CM_PROP_C_TITLE);
					name = (name == null || name.trim().isEmpty()) ? (String) props.get(CCConstants.CM_NAME) : name;
				}

				String permText = "";
				for (String perm : permissions) {
					if(CCConstants.CCM_VALUE_SCOPE_SAFE.equals(NodeServiceInterceptor.getEduSharingScope())){
						// do not show some permission infos in safe invitations since they don't make sense
						if(Arrays.asList(CCConstants.PERMISSION_CC_PUBLISH).contains(perm)){
							continue;
						}
					}
					String i18nPerm = I18nServer
							.getTranslationDefaultResourcebundle(I18nServer.getPermissionCaption(perm), "en_EN");
					String i18nPermDesc = I18nServer.getTranslationDefaultResourcebundle(
							I18nServer.getPermissionDescription(perm), currentLocale);

					if (i18nPermDesc != null) {
						if (!permText.isEmpty())
							permText += "\n";
						permText += i18nPermDesc;
					}

				}

				String linkText = I18nServer.getTranslationDefaultResourcebundle("dialog_inviteusers_mailtext_link",
						currentLocale);
				String localeStr = currentLocale;
				if (localeStr == null || localeStr.equals("default")) {
					localeStr = "de_DE";
				}

				ServletContext context = Context.getCurrentInstance().getRequest().getSession().getServletContext();
				Map<String, String> replace = new HashMap<>();
				replace.put("inviterFirstName", senderFirstName.trim());
				replace.put("inviterLastName", senderLastName.trim());
				replace.put("firstName", receiverFirstName.trim());
				replace.put("lastName", receiverLastName.trim());
				replace.put("name", name.trim());
				replace.put("message", _mailText.trim());
				replace.put("permissions", permText.trim());
				replace.put("link", MailTemplate.generateContentLink(appInfo, _nodeId));
				String template="invited";
				boolean send=true;
				org.edu_sharing.service.nodeservice.NodeService nodeServiceApp = NodeServiceFactory.getNodeService(appInfo.getAppId());
				if(nodeType.equals(CCConstants.CCM_TYPE_MAP) &&
						nodeServiceApp.hasAspect(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),_nodeId,CCConstants.CCM_ASPECT_COLLECTION)){
					template="invited_collection";
					// if the receiver is the creator itself, skip it (because it is automatically added)
					if(authority.equals(nodeServiceApp.getProperty(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),_nodeId,CCConstants.CM_PROP_C_CREATOR))){
						send=false;
					}
				}
				if(CCConstants.CCM_VALUE_SCOPE_SAFE.equals(NodeServiceInterceptor.getEduSharingScope())){
					template = "invited_safe";
				}
				if(send) {
					mail.sendMailHtml(context, senderName, senderInfo.get(CCConstants.CM_PROP_PERSON_EMAIL), emailaddress, MailTemplate.getSubject(template, currentLocale),
							MailTemplate.getContent(template, currentLocale, true), replace);
				}

			} else {
				logger.info("username/authority: " + authority + " has no valid emailaddress:" + emailaddress);
			}

		}

		org.edu_sharing.service.permission.PermissionService permissionService = PermissionServiceFactory
				.getPermissionService(ApplicationInfoList.getHomeRepository().getAppId());

		permissionService.createNotifyObject(_nodeId, user, CCConstants.CCM_VALUE_NOTIFY_ACTION_PERMISSION_ADD);
	}


	/**
	 * Add the authority into the recent list of the users authorities
	 * @param authority
	 */
	private void addToRecent(NodeRef authority) {
		addToRecentProperty(CCConstants.CCM_PROP_PERSON_RECENTLY_INVITED, authority);
	}

	/**
	 * add nodeRef to recent elements list for a property with "NodeRef" list type
	 * Use also @getRecentProperty to get the current list
	 * @param property
	 * @param elementAdd
	 */
	@Override
	public void addToRecentProperty(String property, NodeRef elementAdd){
		nodeService.setProperty(personService.getPerson(AuthenticationUtil.getFullyAuthenticatedUser()),
				QName.createQName(property),
				PropertiesHelper.addToRecentProperty(elementAdd, getRecentProperty(property), 10));
	}

	@Override
	public List<String> getRecentlyInvited() {
		return getRecentProperty(CCConstants.CCM_PROP_PERSON_RECENTLY_INVITED).stream().map((n) -> {
			if(nodeService.getType(n).equals(QName.createQName(CCConstants.CM_TYPE_PERSON))) {
				return (String)nodeService.getProperty(n, QName.createQName(CCConstants.CM_PROP_PERSON_USERNAME));
			} else {
				return (String)nodeService.getProperty(n, QName.createQName(CCConstants.CM_PROP_AUTHORITY_NAME));
			}
		}).collect(Collectors.toList());
	}
	@Override
	public ArrayList<NodeRef> getRecentProperty(String property) {
		List<NodeRef> data= (List<NodeRef>) nodeService.getProperty(personService.getPerson(AuthenticationUtil.getFullyAuthenticatedUser()),
				QName.createQName(property));
		if(data == null){
			return new ArrayList<>();
		}
		return new ArrayList<>(data);

	}

	@Override
	public List<Notify> getNotifyList(final String nodeId) throws Throwable {
		if (!toolPermission.hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_HISTORY)) {
			throw new ToolPermissionException(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_HISTORY);
		}

		Comparator c = new Comparator<Notify>() {
			@Override
			public int compare(Notify o1, Notify o2) {

				if (o1.getCreated().getTime() == o2.getCreated().getTime()) {
					return 0;
				} else if (o1.getCreated().getTime() > o2.getCreated().getTime()) {
					return -1;
				} else if (o1.getCreated().getTime() < o2.getCreated().getTime()) {
					return 1;
				}

				return 0;
			}

		};

		Gson gson = new Gson();
		List<String> jsonHistory = (List<String>)nodeService.getProperty(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId),QName.createQName(CCConstants.CCM_PROP_PH_HISTORY));

		List<Notify> notifyList = new ArrayList<Notify>();
		if(jsonHistory != null) {
			for(String json : jsonHistory) {
				Notify notify = gson.fromJson(json, Notify.class);

				NodeRef personRef = personService.getPerson(notify.getUser().getAuthorityName(),false);
				Map<QName, Serializable> personProps = nodeService.getProperties(personRef);
				notify.getUser().setGivenName((String)personProps.get(QName.createQName(CCConstants.CM_PROP_PERSON_FIRSTNAME)));
				notify.getUser().setSurname((String)personProps.get(QName.createQName(CCConstants.CM_PROP_PERSON_LASTNAME)));
				notify.getUser().setEmail((String)personProps.get(QName.createQName(CCConstants.CM_PROP_PERSON_EMAIL)));

				/**
				 * @todo overwrite acl user firstname, lastname, email
				 */

				notifyList.add(notify);
			}

			Collections.sort(notifyList, c);
		}

		System.out.println("NOTIFYLIST:" + notifyList.size());
		return notifyList;
	}

	public void setPermissions(String nodeId, List<ACE> aces) throws Exception {
		setPermissions(nodeId, aces, null);
	}

	/**
	 * set's all local permissions contained in the aces array, removes all
	 * permissions that are not in the ace array
	 *
	 * @param nodeId
	 * @param aces
	 * @param inheritPermission
	 * @throws Exception
	 */
	public void setPermissions(String nodeId, List<ACE> aces, Boolean inheritPermission) throws Exception {

		if (inheritPermission != null) {
			if (!toolPermission.hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE) && !isSharedNode(nodeId)) {
				throw new ToolPermissionException(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE);
			}
			if (!toolPermission.hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_SHARE) && isSharedNode(nodeId)) {
				throw new ToolPermissionException(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_SHARE);
			}
		}

		checkCanManagePermissions(nodeId, aces);

		NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);

		String authorityAdministrator = getAdminAuthority(nodeRef);

		PermissionService permissionsService = this.serviceRegistry.getPermissionService();
		aces=addCollectionCoordinatorPermission(nodeRef,aces);
		if (aces != null) {
			for (ACE ace : aces) {

				if (!this.serviceRegistry.getAuthorityService().authorityExists(ace.getAuthority())
						&& !"GROUP_EVERYONE".equals(ace.getAuthority())) {
					throw new Exception("authority " + ace.getAuthority() + " does not exist!");
				}
				String permission = ace.getPermission();
				// prevent authorityAdministrator ace is changed
				if (!ace.isInherited()
						&& (authorityAdministrator == null || !authorityAdministrator.equals(ace.getAuthority()))) {
					permissionsService.setPermission(nodeRef, ace.getAuthority(), permission, true);
				}
			}
		}

		ArrayList<AccessPermission> toRemove = new ArrayList<AccessPermission>();
		Set<AccessPermission> allSetPerm = permissionsService.getAllSetPermissions(nodeRef);

		for (AccessPermission accessPerm : allSetPerm) {
			if (accessPerm.isInherited()) {
				continue;
			}
			if (!containslocalPerm(aces, accessPerm.getAuthority(), accessPerm.getPermission())) {
				if (authorityAdministrator == null || !(authorityAdministrator.equals(accessPerm.getAuthority())
						&& PermissionService.COORDINATOR.equals(accessPerm.getPermission()))) {
					toRemove.add(accessPerm);
				}
			}
		}

		for (AccessPermission accessPerm : toRemove) {
			permissionsService.deletePermission(nodeRef, accessPerm.getAuthority(), accessPerm.getPermission());
		}

		if (inheritPermission != null) {
			logger.info("setInheritParentPermissions " + inheritPermission);
			permissionsService.setInheritParentPermissions(nodeRef, inheritPermission);
		}
	}
	@Override
	public void setPermissionInherit(String nodeId,boolean inheritPermission) throws Exception {
		NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
		permissionService.setInheritParentPermissions(nodeRef,inheritPermission);
	}

	private List<ACE> addCollectionCoordinatorPermission(NodeRef nodeRef, List<ACE> aces) {
		if(!nodeService.hasAspect(nodeRef,QName.createQName(CCConstants.CCM_ASPECT_COLLECTION)))
			return aces;

		org.edu_sharing.repository.client.rpc.ACE coordinator=new org.edu_sharing.repository.client.rpc.ACE();
		coordinator.setAuthority(AuthenticationUtil.getFullyAuthenticatedUser());
		coordinator.setAuthorityType(org.edu_sharing.restservices.shared.Authority.Type.USER.name());
		coordinator.setPermission(CCConstants.PERMISSION_COORDINATOR);
		if(aces!=null && aces.contains(coordinator))
			return aces;
		List<ACE> newAces = new ArrayList<>(aces);
		newAces.add(coordinator);
		return newAces;
	}

	/**
	 * returns admin authority if context is an edugroup
	 *
	 * @param nodeRef
	 * @return
	 */
	String getAdminAuthority(NodeRef nodeRef) {
		String authorityAdministrator = null;
		if (isSharedNode(nodeRef.getId())) {
			Set<AccessPermission> allSetPermissions = serviceRegistry.getPermissionService()
					.getAllSetPermissions(nodeRef);
			for (AccessPermission ap : allSetPermissions) {
				NodeRef authorityNodeRef = authorityService.getAuthorityNodeRef(ap.getAuthority());
				if (authorityNodeRef != null) {
					String groupType = (String) nodeService.getProperty(authorityNodeRef,
							QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE));
					if (groupType != null && CCConstants.ADMINISTRATORS_GROUP_TYPE.equals(groupType)
							&& ap.getPermission().equals(PermissionService.COORDINATOR)) {
						authorityAdministrator = ap.getAuthority();
					}
				}
			}
		}
		return authorityAdministrator;
	}

	private boolean containslocalPerm(List<ACE> aces, String eduAuthority, String eduPermission) {
		logger.info("eduAuthority:" + eduAuthority + " eduPermission:" + eduPermission);
		if (aces == null)
			return false;
		for (ACE ace : aces) {
			if (ace.isInherited()) {
				continue;
			}
			logger.info("ace.getAuthority():" + ace.getAuthority() + " ace.getPermission():" + ace.getPermission());
			if (ace.getAuthority().equals(eduAuthority) && ace.getPermission().equals(eduPermission)) {
				return true;
			}
		}
		return false;
	}

	private void checkCanManagePermissions(String node, String authority) throws Exception {
		ACE ace = new ACE();
		ace.setAuthority(authority);
		checkCanManagePermissions(node, Arrays.asList(new ACE[] { ace }));
	}

	private void checkCanManagePermissions(String nodeId, List<ACE> aces) throws Exception {
		boolean hasUsers = false, hasAll = false;
		if (aces != null) {
			for (ACE ace : aces) {

				if (ace.getAuthority() != null && ace.getAuthority().equals("GROUP_EVERYONE")) {
					hasAll = true;
				} else {
					hasUsers = true;
				}
			}
		}

		// not required anymore, also private files can be shared in scope
		/*
		 * if(!shared && NodeServiceInterceptor.getEduSharingScope()!=null){
		 * if(QName.createQName(CCConstants.CCM_TYPE_NOTIFY).equals(nodeService.getType(
		 * new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId)))){ // allow
		 * notify objects to share } else { throw new
		 * Exception("Setting Permissions for private files in scope is not allowed"); }
		 * }
		 */

		if (!toolPermission.hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_SAFE)
				&& NodeServiceInterceptor.getEduSharingScope() != null) {
			throw new ToolPermissionException(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_SAFE);
		}
		if (!toolPermission.hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_ALLAUTHORITIES) && hasAll) {
			throw new ToolPermissionException(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_ALLAUTHORITIES);
		}
		if (NodeServiceInterceptor.getEduSharingScope()!=null && hasAll) {
			throw new SecurityException("Inviting of "+CCConstants.AUTHORITY_GROUP_EVERYONE+" is not allowed in scope "+NodeServiceInterceptor.getEduSharingScope());
		}
		if (!toolPermission.hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE) && hasUsers && !isSharedNode(nodeId)) {
			throw new ToolPermissionException(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE);
		}
		if (!toolPermission.hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_SHARE) && hasUsers
				&& isSharedNode(nodeId)) {
			throw new ToolPermissionException(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_SHARE);
		}
	}

	/**
	 * true if this node is in a shared context ("My shared files"), false if it's
	 * in users home
	 * 
	 * @param nodeId
	 * @return
	 * @throws Throwable
	 */
	private boolean isSharedNode(String nodeId) {
		try {
			String groupFolderId = repoClient.getGroupFolderId(AuthenticationUtil.getFullyAuthenticatedUser());
			List<String> sharedFolderIds = new ArrayList<>();

			if (groupFolderId != null) {
				List<ChildAssociationRef> children = NodeServiceFactory.getLocalService().getChildrenChildAssociationRef(groupFolderId);
				for (ChildAssociationRef key : children) {
					sharedFolderIds.add(key.getChildRef().getId());
				}
			}
			if (sharedFolderIds.size() == 0)
				return false;

			NodeRef last = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
			while (true) {
				if (last == null)
					break;
				if (sharedFolderIds.contains(last.getId()))
					return true;
				last = repoClient.getParent(last).getParentRef();
			}
		} catch (Throwable t) {
			logger.warn(t.getMessage());
		}
		return false;
	}

	public void addPermissions(String nodeId, ACE[] aces) throws Exception {

		serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(

				new RetryingTransactionCallback<Void>() {

					public Void execute() throws Throwable {

						checkCanManagePermissions(nodeId, Arrays.asList(aces));
						NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
						PermissionService permissionsService = serviceRegistry.getPermissionService();

						for (ACE ace : aces) {

							if (ace == null) {
								continue;
							}

							if (!serviceRegistry.getAuthorityService().authorityExists(ace.getAuthority())
									&& !"GROUP_EVERYONE".equals(ace.getAuthority())) {
								throw new Exception("authority " + ace.getAuthority() + " does not exist!");
							}

							String permission = ace.getPermission();

							if (!ace.isInherited()) {
								permissionsService.setPermission(nodeRef, ace.getAuthority(), permission, true);
							}
						}

						return null;
					}

				}, false);

	}

	public void removePermissions(String nodeId, ACE[] aces) throws Exception {

		serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(

				new RetryingTransactionCallback<Void>() {
					public Void execute() throws Throwable {

						checkCanManagePermissions(nodeId, Arrays.asList(aces));

						NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
						PermissionService permissionsService = serviceRegistry.getPermissionService();

						String adminAuthority = getAdminAuthority(nodeRef);

						for (ACE ace : aces) {

							if (ace == null) {
								continue;
							}

							if (!authorityService.authorityExists(ace.getAuthority())
									&& !"GROUP_EVERYONE".equals(ace.getAuthority())) {
								throw new Exception("authority " + ace.getAuthority() + " does not exist!");
							}

							if (adminAuthority != null && !adminAuthority.trim().equals("")
									&& adminAuthority.equals(ace.getAuthority())
									&& PermissionService.COORDINATOR.equals(ace.getPermission())) {
								continue;
							}

							String permission = ace.getPermission();

							if (!ace.isInherited()) {
								permissionsService.deletePermission(nodeRef, ace.getAuthority(), permission);
							}
						}

						return null;
					}

				}, false);
	}

	/**
	 * set's permission for one authority, leaves permissions already set for the
	 * authority
	 */
	public void setPermissions(String nodeId, String authority, String[] permissions, Boolean inheritPermission)
			throws Exception {
		checkCanManagePermissions(nodeId, authority);

		PermissionService permissionsService = this.serviceRegistry.getPermissionService();
		NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
		if (inheritPermission != null) {
			logger.info("setInheritParentPermissions " + inheritPermission);
			permissionsService.setInheritParentPermissions(nodeRef, inheritPermission);
		}

		String adminAuthority = getAdminAuthority(nodeRef);

		if (permissions != null) {
			for (String permission : permissions) {

				if (adminAuthority != null && !adminAuthority.trim().equals("") && adminAuthority.equals(authority)
						&& PermissionService.COORDINATOR.equals(permission)) {
					continue;
				}

				permissionsService.setPermission(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId), authority, permission, true);

			}
		}

	}

	@Override
	public void removeAllPermissions(String nodeId) throws Exception {
		permissionService.deletePermissions(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId));
	}

	public void removePermissions(String nodeId, String authority, String[] _permissions) throws Exception {

		checkCanManagePermissions(nodeId, authority);

		NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
		PermissionService permissionsService = this.serviceRegistry.getPermissionService();

		String adminAuthority = getAdminAuthority(nodeRef);

		if (_permissions != null && _permissions.length > 0) {
			Set<AccessPermission> permSet = permissionsService.getAllSetPermissions(nodeRef);

			// logger.info("getAllSetPermissions size:"+permSet.size());
			for (String permission : _permissions) {
				Iterator<AccessPermission> iter = permSet.iterator();
				// only if Permission exists and authority is the same
				while (iter.hasNext()) {
					AccessPermission ace = iter.next();

					if (adminAuthority != null && !adminAuthority.trim().equals("")
							&& adminAuthority.equals(ace.getAuthority())
							&& PermissionService.COORDINATOR.equals(ace.getPermission())) {
						continue;
					}

					// logger.info("ace.getAuthority():"+ace.getAuthority()+"
					// ace.getPermission():"+ace.getPermission());
					if (ace.getAuthority().equals(authority) && ace.getPermission().equals(permission)) {
						permissionsService.deletePermission(nodeRef, authority, permission);
					}
				}
			}
		}
	}

	private void addGlobalAuthoritySearchQuery(StringBuffer searchQuery) {
		if (NodeServiceInterceptor.getEduSharingScope() == null)
			return;
		try {
			// fetch all groups which are allowed to acces confidential and
			String nodeId = toolPermission.getToolPermissionNodeId(CCConstants.CCM_VALUE_TOOLPERMISSION_CONFIDENTAL);
			StringBuffer groupPathQuery = new StringBuffer();
			// user may not has ReadPermissions on ToolPermission, so fetch as admin
			ACL permissions = AuthenticationUtil.runAsSystem(new RunAsWork<ACL>() {
				@Override
				public ACL doWork() throws Exception {
					return getPermissions(nodeId);
				}
			});
			for (ACE ace : permissions.getAces()) {
				if (groupPathQuery.length() != 0) {
					groupPathQuery.append(" OR ");
				}
				groupPathQuery.append("PATH:\"").append("/").append("sys\\:system").append("/")
						.append("sys\\:authorities").append("/").append("cm\\:")
						.append(ISO9075.encode(ace.getAuthority())).append("//.").append("\"");
			}
			if(groupPathQuery.toString().equals("")) {
				throw new IllegalArgumentException("Global search failed for scope, there were no groups found on the toolpermission "+CCConstants.CCM_VALUE_TOOLPERMISSION_CONFIDENTAL);
			}
			searchQuery.append(" AND (" + groupPathQuery + ")");
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}

	@Override
	public StringBuffer getFindUsersSearchString(String query,List<String> searchFields, boolean globalContext) {

		boolean fuzzyUserSearch = !globalContext || ToolPermissionServiceFactory.getInstance()
				.hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_FUZZY);

		StringBuffer searchQuery = new StringBuffer("TYPE:cm\\:person");
		StringBuffer subQuery=new StringBuffer();

		if (fuzzyUserSearch) {
				if (query != null) {
					for (String token : StringTool.getPhrases(query)) {

						boolean isPhrase = token.startsWith("\"") && token.endsWith("\"");

						if (isPhrase) {

							token = (token.length() > 2) ? token.substring(1, token.length() - 1) : "";

						} else {

							if (!(token.startsWith("*") || token.startsWith("?"))) {
								token = "*" + token;
							}

							if (!(token.endsWith("*") || token.endsWith("?"))) {
								token = token + "*";
							}
						}
						StringBuffer fieldQuery=new StringBuffer();
						for(String field : searchFields) {
							if(fieldQuery.length()>0) {
								fieldQuery.append(" OR ");
							}
							fieldQuery.append("@cm\\:").append(field).append(":").append("\"").append(token).append("\"");
						}
						subQuery.append(subQuery.length() > 0 ? " AND " : "").append("(").append(fieldQuery).append(")");
					}
				}
		} else {

			// when no fuzzy search remove "*" from searchstring and remove all params
			// except email

			String emailValue = query;

			// remove wildcards (*,?)
			if (emailValue != null) {
				emailValue = emailValue.replaceAll("[\\*\\?]", "");
			}

			for (String token : StringTool.getPhrases(emailValue)) {

				boolean isPhrase = token.startsWith("\"") && token.endsWith("\"");

				if (isPhrase) {
					token = (token.length() > 2) ? token.substring(1, token.length() - 1) : "";
				}

				if (token.length() > 0) {
					subQuery.append("=@cm:email:").append("\"").append(token).append("\"");
				}
			}

			// if not fuzzy and no value for email return empty result
			if (subQuery.length() == 0) {
				return null;
			}
		}

		/**
		 * global / groupcontext search
		 */
		boolean hasToolPermission = toolPermission
				.hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH);
		if (globalContext) {

			if (!hasToolPermission) {
				return null;
			}
			addGlobalAuthoritySearchQuery(searchQuery);

		}else{

			List<String> eduGroupAuthorityNames = organisationService.getMyOrganisations(true);

			/**
			 * if there are no edugroups you you are not allowed to search global return
			 * nothing
			 */
			if (eduGroupAuthorityNames.size() == 0) {
				if (!hasToolPermission) {
					return null;
				}
				return getFindUsersSearchString(query, searchFields, true);
			}

			StringBuffer groupPathQuery = new StringBuffer();
			for (String eduGroup : eduGroupAuthorityNames) {
				if (groupPathQuery.length() == 0) {
					groupPathQuery.append("PATH:\"").append("/").append("sys\\:system").append("/")
							.append("sys\\:authorities").append("/").append("cm\\:").append(ISO9075.encode(eduGroup))
							.append("//.").append("\"");
				} else {
					groupPathQuery.append(" OR ").append("PATH:\"").append("/").append("sys\\:system").append("/")
							.append("sys\\:authorities").append("/").append("cm\\:").append(ISO9075.encode(eduGroup))
							.append("//.").append("\"");
				}
			}

			if (groupPathQuery.length() > 0) {
				searchQuery.append(" AND (").append(groupPathQuery).append(")");
			}
		}
		if(!AuthorityServiceHelper.isAdmin()) {
			// allow the access to the guest user for admin
			filterGuestAuthority(searchQuery);
		}

		if (subQuery.length() > 0) {
			searchQuery.append(" AND (").append(subQuery).append(")");
		}

		//cm:espersonstatus
		if(!LightbendConfigLoader.get().getIsNull("repository.personActiveStatus")) {
			String personActiveStatus = LightbendConfigLoader.get().getString("repository.personActiveStatus");
			searchQuery.append(" AND @cm\\:espersonstatus:\"" + personActiveStatus + "\"");
		}

		/**
		 * filter out remote users
		 */
		String homeRepo = ApplicationInfoList.getHomeRepository().getAppId();
		searchQuery.append(" AND (ISUNSET:\"cm:repositoryid\" OR ISNULL:\"cm:repositoryid\" OR @cm\\:repositoryId:\"" + homeRepo + "\")");

		logger.info("findUsers: " + searchQuery);

		return searchQuery;
	}

	private void filterGuestAuthority(StringBuffer searchQuery) {
		String guest=ApplicationInfoList.getHomeRepository().getGuest_username();
		if(guest!=null && !guest.trim().isEmpty()){
			searchQuery.append(" AND NOT @cm\\:userName:\""+ QueryParser.escape(guest)+"\""
							 + " AND NOT @cm\\:userName:\"guest\"");
		}
	}

	@Override
	public StringBuffer getFindGroupsSearchString(String searchWord, boolean globalContext, boolean skipTpCheck) {
		boolean fuzzyGroupSearch = skipTpCheck || !globalContext || ToolPermissionServiceFactory.getInstance()
				.hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_FUZZY);
		
		StringBuffer searchQuery = new StringBuffer("TYPE:cm\\:authorityContainer AND NOT @ccm\\:scopetype:system");

		searchWord = searchWord != null ? searchWord.trim() : "";
	
		StringBuffer subQuery = new StringBuffer();
		
		if(fuzzyGroupSearch) {
			if (("*").equals(searchWord)) {
				searchWord = "";
			}
			if (searchWord.length() > 0) {
	
	
				for (String token : StringTool.getPhrases(searchWord)) {
	
					boolean isPhrase = token.startsWith("\"") && token.endsWith("\"");
	
					if (isPhrase) {
	
						token = (token.length() > 2) ? token.substring(1, token.length() - 1) : "";
	
					} else {
	
						if (!(token.startsWith("*") || token.startsWith("?"))) {
							token = "*" + token;
						}
	
						if (!(token.endsWith("*") || token.endsWith("?"))) {
							token = token + "*";
						}
					}
	
					if (token.length() > 0) {
	
						boolean furtherToken = (subQuery.length() > 0);
						//subQuery.append((furtherToken ? " AND( " : "(")).append("@cm\\:authorityName:").append("\"")
						//		.append(token).append("\"").append(" OR @cm\\:authorityDisplayName:").append("\"")
						subQuery.append((furtherToken ? " AND( " : "(")).append("@cm\\:authorityDisplayName:").append("\"")
								.append(token).append("\"");
						subQuery.append(")");
	
					}
				}	
			}
		}
		else {

			// remove wildcards (*,?)
			if (searchWord != null) {
				searchWord = searchWord.replaceAll("[\\*\\?]", "");
			}
			
			for (String token : StringTool.getPhrases(searchWord)) {

				boolean isPhrase = token.startsWith("\"") && token.endsWith("\"");

				if (isPhrase) {
					token = (token.length() > 2) ? token.substring(1, token.length() - 1) : "";
				}

				if (token.length() > 0) {
					subQuery.append("=@cm:authorityDisplayName:").append("\"").append(token).append("\"");
				}
			}

			// if not fuzzy and no value for email return empty result
			if (subQuery.length() == 0) {
				return null;
			}
		}
		if (subQuery.length() > 0) {
			searchQuery.append(" AND (").append(subQuery).append(")");
		}
		boolean hasToolPermission = skipTpCheck || toolPermission
				.hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH);

		if (globalContext) {
			if (!hasToolPermission) {
				return null;
			}
			addGlobalAuthoritySearchQuery(searchQuery);
		} else {

			List<String> eduGroupAuthorityNames = organisationService.getMyOrganisations(true);

			/**
			 * if there are no edugroups you you are not allowed to search global return
			 * nothing
			 */
			if (eduGroupAuthorityNames.size() == 0) {
				if (!hasToolPermission) {
					return null;
				}
			}

			StringBuffer groupPathQuery = new StringBuffer();
			for (String eduGroup : eduGroupAuthorityNames) {
				if (groupPathQuery.length() == 0) {
					groupPathQuery.append("PATH:\"").append("/").append("sys\\:system").append("/")
							.append("sys\\:authorities").append("/").append("cm\\:").append(ISO9075.encode(eduGroup))
							.append("//.").append("\"");
				} else {
					groupPathQuery.append(" OR ").append("PATH:\"").append("/").append("sys\\:system").append("/")
							.append("sys\\:authorities").append("/").append("cm\\:").append(ISO9075.encode(eduGroup))
							.append("//.").append("\"");
				}
			}

			if (groupPathQuery.length() > 0) {
				searchQuery.append(" AND (").append(groupPathQuery).append(")");
			}
		}

		searchQuery.append(" AND NOT (@cm\\:authorityName:" + CCConstants.AUTHORITY_GROUP_ALFRESCO_ADMINISTRATORS
				+ " or @cm\\:authorityName:" + CCConstants.AUTHORITY_GROUP_EMAIL_CONTRIBUTORS + ")");

		logger.info("findGroups: " + searchQuery);

		return searchQuery;
	}

	@Override
	public Result<List<User>> findUsers(String query,List<String> searchFields, boolean globalContext, int from, int nrOfResults) {

		StringBuffer searchQuery = null;
		searchQuery = getFindUsersSearchString(query, searchFields, globalContext);

		if (searchQuery == null) {
			return new Result<List<User>>();
		}

		SearchService searchService = serviceRegistry.getSearchService();
		SearchParameters searchParameters = new SearchParameters();
		searchParameters.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
		searchParameters.setLanguage(SearchService.LANGUAGE_LUCENE);
		searchParameters.setQuery(searchQuery.toString());
		searchParameters.setSkipCount(from);
		searchParameters.setMaxItems(nrOfResults);
		searchParameters.addSort("@" + CCConstants.PROP_USER_EMAIL, true);
		ResultSet resultSet = searchService.query(searchParameters);

		List<User> data = new ArrayList<User>();
		for (NodeRef nodeRef : resultSet.getNodeRefs()) {
			User user = new User();
			user.setEmail((String) nodeService.getProperty(nodeRef, ContentModel.PROP_EMAIL));
			user.setGivenName((String) nodeService.getProperty(nodeRef, ContentModel.PROP_FIRSTNAME));

			String repository = (String) nodeService.getProperty(nodeRef,
					QName.createQName(CCConstants.PROP_USER_REPOSITORYID));
			if (repository == null || repository.trim().equals(""))
				repository = appInfo.getAppId();
			user.setRepositoryId(repository);

			user.setSurname((String) nodeService.getProperty(nodeRef, ContentModel.PROP_LASTNAME));
			user.setNodeId(nodeRef.getId());
			user.setUsername((String) nodeService.getProperty(nodeRef, ContentModel.PROP_USERNAME));
			data.add(user);
		}

		Result<List<User>> result = new Result<List<User>>();
		result.setData(data);

		if (resultSet instanceof SolrJSONResultSet) {
			result.setNodeCount((int) ((SolrJSONResultSet) resultSet).getNumberFound());
			result.setStartIDX(((SolrJSONResultSet) resultSet).getStart());
		} else if (resultSet instanceof FilteringResultSet) {
			result.setNodeCount(resultSet.length());
			// alf4.2.f FilteringResultSet throws java.lang.UnsupportedOperationException
			// when calling getStart
			// so we take the from param
			result.setStartIDX(from);
		} else {
			result.setNodeCount(resultSet.length());
			result.setStartIDX(resultSet.getStart());
		}

		logger.info("nodecount:" + result.getNodeCount() + " startidx:" + result.getStartIDX() + " count:"
				+ result.getData().size());

		return result;
	}

	@Override
	public Result<List<Authority>> findAuthorities(String searchWord, boolean globalContext, int from,
			int nrOfResults) {

		// fields to search in - not using username
		List<String> searchFields = new ArrayList<>();
		searchFields.add("email");
		searchFields.add("firstName");
		searchFields.add("lastName");

		StringBuffer findUsersQuery = getFindUsersSearchString(searchWord,searchFields, globalContext);
		StringBuffer findGroupsQuery = getFindGroupsSearchString(searchWord, globalContext, false);

		/**
		 * don't find groups of scopes when no scope is provided
		 */
		if (NodeServiceInterceptor.getEduSharingScope() == null) {

			/**
			 * groups arent initialized with eduscope aspect and eduscopename null
			 */
			findGroupsQuery.append(" AND NOT @ccm\\:eduscopename:\"*\"");
		}

		StringBuffer finalQuery = findUsersQuery.insert(0, "(").append(") OR (").append(findGroupsQuery).append(")");

		System.out.println("finalQuery:" + finalQuery);

		List<Authority> data = new ArrayList<Authority>();

		SearchParameters searchParameters = new SearchParameters();
		searchParameters.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
		searchParameters.setLanguage(SearchService.LANGUAGE_LUCENE);
		searchParameters.setQuery(finalQuery.toString());
		searchParameters.setSkipCount(from);
		searchParameters.setMaxItems(nrOfResults);

		searchParameters.addSort("@" + CCConstants.CM_PROP_AUTHORITY_AUTHORITYDISPLAYNAME, true);
		searchParameters.addSort("@" + CCConstants.PROP_USER_FIRSTNAME, true);

		// dont use scopeed search service
		SearchService searchService = serviceRegistry.getSearchService();
		ResultSet resultSet = searchService.query(searchParameters);

		for (NodeRef nodeRef : resultSet.getNodeRefs()) {

			String authorityName = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_AUTHORITY_NAME);
			if (authorityName != null) {
				Group group = new Group();
				group.setName(authorityName);
				group.setDisplayName(
						(String) nodeService.getProperty(nodeRef, ContentModel.PROP_AUTHORITY_DISPLAY_NAME));
				group.setRepositoryId(appInfo.getAppId());
				group.setNodeId(nodeRef.getId());
				group.setAuthorityType(AuthorityType.getAuthorityType(group.getName()).name());
				group.setScope(
						(String) nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_SCOPE_TYPE)));
				data.add(group);
			} else {
				User user = new User();
				user.setEmail((String) nodeService.getProperty(nodeRef, ContentModel.PROP_EMAIL));
				user.setGivenName((String) nodeService.getProperty(nodeRef, ContentModel.PROP_FIRSTNAME));

				String repository = (String) nodeService.getProperty(nodeRef,
						QName.createQName(CCConstants.PROP_USER_REPOSITORYID));
				if (repository == null || repository.trim().equals(""))
					repository = appInfo.getAppId();
				user.setRepositoryId(repository);

				user.setSurname((String) nodeService.getProperty(nodeRef, ContentModel.PROP_LASTNAME));
				user.setNodeId(nodeRef.getId());
				user.setUsername((String) nodeService.getProperty(nodeRef, ContentModel.PROP_USERNAME));
				data.add(user);
			}
		}

		Result<List<Authority>> result = new Result<List<Authority>>();
		result.setData(data);

		if (resultSet instanceof SolrJSONResultSet) {
			result.setNodeCount((int) ((SolrJSONResultSet) resultSet).getNumberFound());
			result.setStartIDX(((SolrJSONResultSet) resultSet).getStart());
		} else if (resultSet instanceof FilteringResultSet) {
			result.setNodeCount(resultSet.length());
			// alf4.2.f FilteringResultSet throws java.lang.UnsupportedOperationException
			// when calling getStart
			// so we take the from param
			result.setStartIDX(from);
		} else {
			result.setNodeCount(resultSet.length());
			result.setStartIDX(resultSet.getStart());
		}

		logger.info("nodecount:" + result.getNodeCount() + " startidx:" + result.getStartIDX() + " count:"
				+ result.getData().size());
		return result;
	}

	@Override
	public Result<List<Group>> findGroups(String searchWord, boolean globalContext, int from, int nrOfResults) {

		StringBuffer searchQuery = getFindGroupsSearchString(searchWord, globalContext, false);

		if (searchQuery == null) {
			return new Result<List<Group>>();
		}

		List<Group> data = new ArrayList<Group>();

		SearchParameters searchParameters = new SearchParameters();
		searchParameters.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
		searchParameters.setLanguage(SearchService.LANGUAGE_LUCENE);
		searchParameters.setQuery(searchQuery.toString());
		searchParameters.setSkipCount(from);
		searchParameters.setMaxItems(nrOfResults);
		searchParameters.addSort("@" + CCConstants.CM_PROP_AUTHORITY_AUTHORITYDISPLAYNAME, true);

		SearchService searchService = serviceRegistry.getSearchService();
		ResultSet resultSet = searchService.query(searchParameters);

		for (NodeRef nodeRef : resultSet.getNodeRefs()) {
			String authorityName = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_AUTHORITY_NAME);
			Group group = new Group();
			group.setName(authorityName);
			group.setDisplayName((String) nodeService.getProperty(nodeRef, ContentModel.PROP_AUTHORITY_DISPLAY_NAME));
			group.setRepositoryId(appInfo.getAppId());
			group.setNodeId(nodeRef.getId());
			group.setAuthorityType(AuthorityType.getAuthorityType(group.getName()).name());
			group.setScope(
					(String) nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_SCOPE_TYPE)));
			data.add(group);
		}

		Result<List<Group>> result = new Result<List<Group>>();
		result.setData(data);

		if (resultSet instanceof SolrJSONResultSet) {
			result.setNodeCount((int) ((SolrJSONResultSet) resultSet).getNumberFound());
			result.setStartIDX(((SolrJSONResultSet) resultSet).getStart());
		} else if (resultSet instanceof FilteringResultSet) {
			result.setNodeCount(resultSet.length());
			// alf4.2.f FilteringResultSet throws java.lang.UnsupportedOperationException
			// when calling getStart
			// so we take the from param
			result.setStartIDX(from);
		} else {
			result.setNodeCount(resultSet.length());
			result.setStartIDX(resultSet.getStart());
		}

		logger.info("nodecount:" + result.getNodeCount() + " startidx:" + result.getStartIDX() + " count:"
				+ result.getData().size());
		return result;
	}

	public void createNotifyObject(final String nodeId, final String user, final String action) {

		NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);

		if(!nodeService.hasAspect(nodeRef, QName.createQName(CCConstants.CCM_ASPECT_PERMISSION_HISTORY))) {
			nodeService.addAspect(nodeRef, QName.createQName(CCConstants.CCM_ASPECT_PERMISSION_HISTORY), null);
		}

		nodeService.setProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_ACTION), action);

		ArrayList<String> phUsers = (ArrayList<String>)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_USERS));
		if(phUsers == null) phUsers = new ArrayList<String>();
		if(!phUsers.contains(user)) phUsers.add(user);
		nodeService.setProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_USERS), phUsers);
		Date created = new Date();
		nodeService.setProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_MODIFIED), created);


		//ObjectMapper jsonMapper = new ObjectMapper();
		Gson gson = new Gson();
		Notify n = new Notify();
		try {
			ACL acl = repoClient.getPermissions(nodeId);
			// set of all authority names that are not inherited, but explicitly set
			nodeService.setProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_INVITED), PermissionServiceHelper.getExplicitAuthoritiesFromACL(acl));

			acl.setAces(acl.getAces());


			n.setAcl(acl);
			n.setCreated(created);
			n.setNotifyAction(action);
			n.setNotifyUser(user);
			User u = new User();
			u.setAuthorityName(user);
			u.setUsername(user);
			n.setUser(u);


			String jsonStringACL = gson.toJson(n);
			List<String> history = (List<String>)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_HISTORY));
			history = (history == null)? new ArrayList<String>() : history;
			while(history.size()>MAX_NOTIFY_HISTORY_LENGTH){
					history.remove(0);
			}
			history.add(jsonStringACL);
			nodeService.setProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_HISTORY), new ArrayList(history));
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}

	@Override
	public boolean hasPermission(String storeProtocol, String storeId, String nodeId, String permission) {
		return hasAllPermissions(storeProtocol,storeId,nodeId,new String[]{permission}).get(permission);
	}
	@Override
	public boolean hasPermission(String storeProtocol, String storeId, String nodeId, String authority, String permission) {
		return hasAllPermissions(storeProtocol,storeId,nodeId,authority,new String[]{permission}).get(permission);
	}
	@Override
	public HashMap<String, Boolean> hasAllPermissions(String storeProtocol, String storeId, String nodeId, String authority,
													  String[] permissions) {
		return AuthenticationUtil.runAs(()->hasAllPermissions(storeProtocol,storeId,nodeId,permissions),authority);
	}
	@Override
	public HashMap<String, Boolean> hasAllPermissions(String storeProtocol, String storeId, String nodeId,
			String[] permissions) {
		ApplicationInfo appInfo = ApplicationInfoList.getHomeRepository();
		String guestName = appInfo.getGuest_username();
		boolean guest = guestName != null && guestName.equals(AuthenticationUtil.getFullyAuthenticatedUser());
		PermissionService permissionService = serviceRegistry.getPermissionService();
		HashMap<String, Boolean> result = new HashMap<String, Boolean>();
		NodeRef nodeRef = new NodeRef(new StoreRef(storeProtocol, storeId), nodeId);
		if (permissions != null && permissions.length > 0) {
			for (String permission : permissions) {
					AccessStatus accessStatus = permissionService.hasPermission(nodeRef, permission);
				// Guest only has read permissions, no modify permissions
				if(guest && !Arrays.asList(GUEST_PERMISSIONS).contains(permission)){
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

	@Override
	public ACL getPermissions(String nodeId) throws Exception {
		return repoClient.getPermissions(nodeId);
	}
	private boolean isAdminOrSystem(){
		return Arrays.asList(AuthenticationUtil.SYSTEM_USER_NAME,ApplicationInfoList.getHomeRepository().getUsername()).contains(AuthenticationUtil.getFullyAuthenticatedUser());
	}

	@Override
	public List<String> getPermissionsForAuthority(String nodeId, String authorityId) throws Exception {
		if(!authorityId.equals(AuthenticationUtil.getFullyAuthenticatedUser())){
			if(!isAdminOrSystem()) {
				if(!hasPermission(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),nodeId,PermissionService.READ_PERMISSIONS)) {
					throw new InsufficientPermissionException("Current user is missing "+PermissionService.READ_PERMISSIONS+" for this node");
				}
			}
		}

		if(!CCConstants.AUTHORITY_GROUP_EVERYONE.equals(authorityId) && !"System".equals(authorityId) && !authorityService.authorityExists(authorityId)){
			throw new IllegalArgumentException("Authority "+authorityId+" does not exist");
		}
		return AuthenticationUtil.runAs(() -> {
			List<String> result = new ArrayList<>();
			NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);

			for (String permission : CCConstants.getPermissionList()) {
				if (permissionService.hasPermission(nodeRef, permission).equals(AccessStatus.ALLOWED)) {
					result.add(permission);
				}
			}
			return result;
		}, CCConstants.AUTHORITY_GROUP_EVERYONE.equals(authorityId) ? AuthenticationUtil.getGuestUserName() : authorityId);
	}
	/**
	 * return explicitly set permissions for this node
	 * Inherited or permissions from groups are ignored
	 * @param nodeId
	 * @param authorityId
	 * @return
	 * @throws Exception
	 */
	@Override
	public List<String> getExplicitPermissionsForAuthority(String nodeId, String authorityId) throws Exception {
		if(!authorityId.equals(AuthenticationUtil.getFullyAuthenticatedUser()) && !isAdminOrSystem()){
			if(!getPermissionsForAuthority(nodeId, AuthenticationUtil.getFullyAuthenticatedUser())
					.contains(PermissionService.READ_PERMISSIONS)) {
				throw new InsufficientPermissionException("Current user is missing "+PermissionService.READ_PERMISSIONS+" for this node");
			}
		}

		if(!CCConstants.AUTHORITY_GROUP_EVERYONE.equals(authorityId) && !authorityService.authorityExists(authorityId)){
			throw new IllegalArgumentException("Authority "+authorityId+" does not exist");
		}
		NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId);
		List<String> result=new ArrayList<>();
		Set<AccessPermission> permissions = permissionService.getAllSetPermissions(nodeRef);
		for(AccessPermission permission:permissions) {
			if(permission.getAuthority().equals(authorityId) &&
					CCConstants.getPermissionList().contains(permission.getPermission())){
				result.add(permission.getPermission());
			}
		}
		return result;
	}
	@Override
	public void setPermission(String nodeId, String authority, String permission) {
		permissionService.setPermission(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId), authority, permission, true);
	}

	public void setToolPermission(ToolPermissionService toolPermission) {
		this.toolPermission = toolPermission;
	}
	
}
