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
package org.edu_sharing.repository.server.jobs.quartz;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.quartz.*;

public abstract class AbstractJob implements Job,InterruptableJob {
	// add your job description
	static JobDescription JOB_DESCRIPTION;
	protected Log logger = LogFactory.getLog(AbstractJob.class);
	
	boolean isStarted = false;

	protected JobDataMap jobDataMap;

	public JobDataMap getJobDataMap() {
		return jobDataMap;
	}

	protected Class[] allJobs =  new Class[] { ImporterJob.class, RefreshCacheJob.class,RemoveDeletedImportsJob.class,RemoveImportedObjectsJob.class,GetAllDamagedObjects.class,RefreshPublisherListJob.class, TrackingJob.class, ExporterJob.class};
	protected boolean isInterrupted=false;

	//important for immediate executed Jobs so that we can give an user feedback if the job was vetoed
	//boolean vetoed = false;
	
	public boolean isStarted() {
		return isStarted;
	}
	
	public void setStarted(boolean isStarted) {
		this.isStarted = isStarted;
	}

	public Class[] getJobClasses() {
		// TODO Auto-generated method stub
		return allJobs;
	}
	protected synchronized void addJobClass(Class job) {
		if(!Arrays.asList(allJobs).contains(job)) {
			ArrayList<Class> list = new ArrayList<Class>(Arrays.asList(allJobs));
			list.add(job);
			allJobs = list.toArray(new Class[list.size()]);
		}
	}

	public boolean isInterrupted() {
		return isInterrupted;
	}

	@Override
	public void interrupt() throws UnableToInterruptJobException {
		isInterrupted = true;
	}

	@Override
	public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		this.jobDataMap=jobExecutionContext.getJobDetail().getJobDataMap();
	}
}
