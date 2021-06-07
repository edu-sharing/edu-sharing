package org.edu_sharing.service.tracking;

import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.codec.digest.DigestUtils;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.springframework.context.ApplicationContext;

import javax.servlet.http.HttpSession;
import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class TrackingServiceDefault implements TrackingService{
    private final org.edu_sharing.service.nodeservice.NodeService nodeService;
    public static Map<EventType,String> EVENT_PROPERTY_MAPPING=new HashMap<>();

    BehaviourFilter policyBehaviourFilter = null;

    TransactionService transactionService = null;

    static{
        EVENT_PROPERTY_MAPPING.put(EventType.DOWNLOAD_MATERIAL,CCConstants.CCM_PROP_TRACKING_DOWNLOADS);
        EVENT_PROPERTY_MAPPING.put(EventType.VIEW_MATERIAL,CCConstants.CCM_PROP_TRACKING_VIEWS);
        EVENT_PROPERTY_MAPPING.put(EventType.VIEW_MATERIAL_EMBEDDED,CCConstants.CCM_PROP_TRACKING_VIEWS);
    }

    public TrackingServiceDefault() {

        ApplicationContext appContext = AlfAppContextGate.getApplicationContext();
        ServiceRegistry serviceRegistry = (ServiceRegistry) appContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
        nodeService=NodeServiceFactory.getLocalService();
        transactionService = serviceRegistry.getTransactionService();
        policyBehaviourFilter = (BehaviourFilter)appContext.getBean("policyBehaviourFilter");
    }

    @Override
    public boolean trackActivityOnUser(String authorityName, EventType type) {
        return false;
    }

    @Override
    public boolean trackActivityOnNode(NodeRef nodeRef,NodeTrackingDetails details, EventType type) {
        String qname = EVENT_PROPERTY_MAPPING.get(type);
        if(qname == null){
            return false;
        }
        String value= nodeService.getProperty(nodeRef.getStoreRef().getProtocol(),nodeRef.getStoreRef().getIdentifier(),nodeRef.getId(),qname);
        if(value==null)
            value="0";

        long valueLong=Long.parseLong(value);
        valueLong++;
        final String finalValue=""+valueLong;
        AuthenticationUtil.runAsSystem(()->{
        	RetryingTransactionHelper rth = transactionService.getRetryingTransactionHelper();
    		rth.doInTransaction((RetryingTransactionCallback<Void>) () -> {
                policyBehaviourFilter.disableBehaviour(nodeRef);
                nodeService.setProperty(nodeRef.getStoreRef().getProtocol(),nodeRef.getStoreRef().getIdentifier(),nodeRef.getId(),qname,finalValue);
                policyBehaviourFilter.enableBehaviour(nodeRef);
                // change the value in cache
                Map<String, Object> cache = new RepositoryCache().get(nodeRef.getId());
                if(cache!=null) {
                    cache.put(qname, finalValue);
                }
                return null;
            });
            return null;
        });
        return true;
    }

    /**
     * remove / annonymize / print the username for tracking
     * @return
     */
    protected String getTrackedUsername(String username) {
        if(username==null)
            username=AuthenticationUtil.getFullyAuthenticatedUser();

        UserTrackingMode mode=getUserTrackingMode();
        if (mode.equals(UserTrackingMode.obfuscate)) {
            return DigestUtils.sha1Hex(username);
        } else if (mode.equals(UserTrackingMode.full)) {
            return username;
        } else if (mode.equals(UserTrackingMode.session)) {
            HttpSession session = Context.getCurrentInstance() == null ? null :
                    Context.getCurrentInstance().getRequest().getSession(false);
            if(session != null){
                return DigestUtils.sha1Hex(session.getId() + username);
            }
        }

        // we need any kind of stable id for tracking, so we'll generate a random, hopefully unique UUID
        return UUID.randomUUID().toString();
    }
    protected UserTrackingMode getUserTrackingMode(){
        String mode= LightbendConfigLoader.get().getString("repository.tracking.userMode");
        if(mode==null)
            return UserTrackingMode.none;
        return UserTrackingMode.valueOf(mode);
    }
}
