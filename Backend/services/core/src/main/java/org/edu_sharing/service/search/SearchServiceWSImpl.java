package org.edu_sharing.service.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityType;
import org.edu_sharing.metadataset.v2.MetadataSetV2;
import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.search.model.SearchResult;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.search.model.SharedToMeType;
import org.edu_sharing.service.search.model.SortDefinition;

public class SearchServiceWSImpl extends SearchServiceAdapter {
	
	String appId;
	
	ApplicationInfo appInfo = null;
	
	public SearchServiceWSImpl(String appId) {
		this.appId = appId;
		appInfo = ApplicationInfoList.getRepositoryInfoById(this.appId);
	}
	
	@Override
	public SearchResult<EduGroup> getAllOrganizations(boolean scoped) throws Exception {
		return null;
	}
	
	@Override
	public SearchResultNodeRef getFilesSharedByMe(SortDefinition sortDefinition, ContentType contentType, int skipCount, int maxItems) throws Exception {
		return null;
	}
	
	@Override
	public SearchResultNodeRef getFilesSharedToMe(SharedToMeType type, SortDefinition sortDefinition, ContentType contentType, int skipCount, int maxItems) throws Exception {
		return null;
	}
	
	@Override
	public SearchResultNodeRef search(SearchToken searchToken, boolean scoped) {
		return null;
	}
	
	@Override
	public SearchResult<String> searchGroupMembers(String groupName, String pattern, String authorityType,
			int skipCount, int maxValues, SortDefinition sort) {
		return null;
	}
	
	@Override
	public SearchResult<EduGroup> searchOrganizations(String pattern, int skipCount, int maxValues, SortDefinition sort,boolean scoped, boolean onlyMemberships)
			throws Throwable {
		return null;
	}
	
	@Override
	public SearchResultNodeRef search(SearchToken _searchToken) {
		org.edu_sharing.repository.client.rpc.SearchToken searchToken = new org.edu_sharing.repository.client.rpc.SearchToken();
		searchToken.setCountProps(new ArrayList<String>(_searchToken.getFacettes()));
		searchToken.setCountPropsMinCount(_searchToken.getFacettesMinCount());
		searchToken.setNrOfResults(_searchToken.getMaxResult());
		searchToken.setRepositoryId(appId);
		return null;
	}

	@Override
	public List<NodeRef> getWorkflowReceive(String fullyAuthenticatedUser) {
		return null;
	}

	@Override
	public SearchResultNodeRef searchV2(MetadataSetV2 mds, String query, Map<String, String[]> criterias,
			SearchToken searchToken) throws Throwable {
		return null;
	}

	@Override
	public SearchResult<String> searchUsers(String _pattern, boolean globalSearch, int _skipCount, int _maxValues,
			SortDefinition sort, Map<String, String> customProperties) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SearchResult<String> findAuthorities(AuthorityType user, String searchWord, boolean globalContext, int from,
			int nrOfResults, SortDefinition sort, Map<String, String> customProperties)
			throws InsufficientPermissionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HashMap<ContentType,SearchToken> getLastSearchTokens() throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SearchResult<String> searchPersonGroups(String authorityName, String pattern, int skipCount, int maxValues,
			SortDefinition sort) {
		// TODO Auto-generated method stub
		return null;
	}

}
