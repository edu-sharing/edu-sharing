package org.edu_sharing.restservices;

import java.util.Map;

import org.apache.log4j.Logger;
import org.edu_sharing.service.rendering.RenderingTool;

public class RenderingDao {

	RepositoryDao repoDao;
	
	public RenderingDao(RepositoryDao repoDao) {
		this.repoDao = repoDao;
	}
	
	public String getDetails(String nodeId,String nodeVersion, String displayMode,Map<String,String> parameters) throws DAOException{
		try{
			return repoDao.getRenderingServiceClient().getDetails(nodeId,nodeVersion,
					displayMode==null || displayMode.isEmpty() ? RenderingTool.DISPLAY_DYNAMIC : displayMode,
					parameters);
		}catch(Exception e){
			throw DAOException.mapping(e);
		}
	}
}
