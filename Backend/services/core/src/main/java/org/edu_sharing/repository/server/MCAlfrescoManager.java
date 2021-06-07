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

import java.io.File;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.chemistry.opencmis.server.impl.CmisRepositoryContextListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.alfresco.policy.OnUpdatePersonPropertiesPolicy;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.MetadataReaderV2;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.quartz.JobHandler;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.UserEnvironmentTool;
import org.edu_sharing.repository.server.tools.cache.EduGroupCache;
import org.edu_sharing.repository.server.tracking.TrackingService;
import org.edu_sharing.repository.server.tracking.TrackingService.TrackingBufferFactory;
import org.edu_sharing.repository.server.tracking.buffer.FileRingBuffer;
import org.edu_sharing.repository.server.tracking.buffer.MemoryRingBuffer;
import org.edu_sharing.repository.server.tracking.buffer.TrackingBuffer;
import org.edu_sharing.repository.update.Edu_SharingAuthoritiesUpdate;
import org.edu_sharing.repository.update.KeyGenerator;
import org.edu_sharing.repository.update.Release_1_7_SubObjectsToFlatObjects;
import org.edu_sharing.repository.update.Release_1_7_UnmountGroupFolders;
import org.edu_sharing.repository.update.Release_3_2_DefaultScope;
import org.edu_sharing.repository.update.Release_3_2_FillOriginalId;
import org.edu_sharing.repository.update.Release_3_2_PermissionInheritFalse;
import org.edu_sharing.repository.update.Release_4_2_PersonStatusUpdater;
import org.edu_sharing.repository.update.SQLUpdater;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;
import org.springframework.context.ApplicationContext;

public class MCAlfrescoManager implements ServletContextListener {

	Log logger = LogFactory.getLog(MCAlfrescoManager.class);
	private ApplicationContext applicationContext;
	private ServiceRegistry serviceRegistry;

	// -- startup ---
	
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		try{
			applicationContext = AlfAppContextGate.getApplicationContext();
			serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
			//generate security keys if not there
			new KeyGenerator(null).execute();
			ApplicationInfoList.refresh();
			
			logger.info("load ApplicationInfos");			
			ApplicationInfo appInfo = ApplicationInfoList.getHomeRepository();

			logger.info("load Metadatasets");
			MetadataReaderV2.refresh();
			
			//do update this class checks if it is already done
			AuthenticationToolAPI authTool = new AuthenticationToolAPI();
			authTool.createNewSession(appInfo.getUsername(), appInfo.getPassword());
			
			logger.info("load edu groups");
			
			/**
			 * only refresh when size is null, to prevent that all clusternodes try to clear and fill again, so in best case only the first cluster node fill's this 
			 */
			if(EduGroupCache.getKeys().size() == 0){
				logger.info("starting filling edugroup cache");
				EduGroupCache.refresh();
			}else{
				logger.info("edugroup cache has "+EduGroupCache.getKeys().size() +" entries, getting(got) cache entries by another cluster node");
			}
			
			logger.info("check for updates");
			
			//remove lom subobjects
			new Release_1_7_SubObjectsToFlatObjects(null).execute();
			
			//unmount groupfolderes cause they are virtual mounted
			new Release_1_7_UnmountGroupFolders(null).execute();
			
			//make admin and other users get edu folders in userhome
			new Edu_SharingAuthoritiesUpdate(null).execute();
			
			new Release_3_2_DefaultScope(null).execute();
			
			//fill original property of all IO's
			new Release_3_2_FillOriginalId(null).execute();
			
			new Release_3_2_PermissionInheritFalse(null).execute();
			
			new SQLUpdater().execute();
			
			new Release_4_2_PersonStatusUpdater(null).execute();

			//init the system folders so that are created with a admin
			UserEnvironmentTool uet = new UserEnvironmentTool(appInfo.getUsername());
			uet.getEdu_SharingTemplateFolder();

			// init the esuid for admin
			createESUIDAdmin();
			
			//init ToolPermisssions
			ToolPermissionServiceFactory.getInstance().init();

			if (appInfo.getTrackingBufferSize() > 0) {
				
				int size = appInfo.getTrackingBufferSize();
				logger.info("startup TrackingBuffer (max=" + size + ")");
				
				File directory = (File) servletContextEvent.getServletContext().getAttribute("javax.servlet.context.tempdir");
				
				final TrackingBuffer trackingBuffer = 
						( directory != null 
						? new FileRingBuffer(directory, size)
						: new MemoryRingBuffer(size));
						
				TrackingService.registerBuffer(new TrackingBufferFactory() {
										
					public TrackingBuffer getTrackingBuffer() {
						return trackingBuffer;
					}
				});
			} else {
				logger.warn("no tracking!");				
			}
			
			logger.info("startup JobHandler");
			JobHandler.getInstance();
			
			//test setting cmis factory to use cmis in edu-sharing
			//ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
			Object factory = applicationContext.getBean("CMISServiceFactory");

			servletContextEvent.getServletContext().setAttribute(CmisRepositoryContextListener.SERVICES_FACTORY, factory);
			
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}

	private void createESUIDAdmin() {
		NodeRef admin = serviceRegistry.getPersonService().getPersonOrNull(ApplicationInfoList.getHomeRepository().getUsername());
		if (admin==null) {
			logger.warn("Creating an esuid for admin failed. Check that the username property in your homeapp is correct: " + ApplicationInfoList.getHomeRepository().getUsername());
		} else {
			if(OnUpdatePersonPropertiesPolicy.createESUIDIfNotExists(serviceRegistry.getNodeService(),admin)) {
				logger.info("Successfully created an esuid for the admin user");
			}
		}
	}

	// -- shutdown ---
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		try {
			
			logger.info("shutdown JobHandler");
			JobHandler.getInstance().shutDown();
			
			TrackingBufferFactory trackingBufferFactory = TrackingService.unregisterBuffer();			
			if (trackingBufferFactory != null) {
				
				logger.info("shutdown Tracking");
				TrackingBuffer trackingBuffer = trackingBufferFactory.getTrackingBuffer();
				
				if (! trackingBuffer.isEmpty()) {
					logger.warn("TrackingBuffer is not empty!");
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
