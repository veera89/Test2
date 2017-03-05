package com.skuview.importsku.model;

import java.util.Date;

import javax.persistence.Id;

import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "FILE_SUMMARY")
public class FileSummaryModel {

	@Id
	private long id;
	private long batchId;
	private long templateId;
	private String fileName;
	private long totalNoOfRecordsInStage;
	private long totalNoOfRecords;
	private boolean isDeleted=false;
	private long totalNoOfDeletedRecords=0;
	private long totalNoOfProcessedRecords;
	private long sourceId;
	private Date timeStamp;
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public long getBatchId() {
		return batchId;
	}
	public void setBatchId(long batchId) {
		this.batchId = batchId;
	}
	public long getTemplateId() {
		return templateId;
	}
	public void setTemplateId(long templateId) {
		this.templateId = templateId;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	public long getTotalNoOfRecords() {
		return totalNoOfRecords;
	}
	public void setTotalNoOfRecords(long totalNoOfRecords) {
		this.totalNoOfRecords = totalNoOfRecords;
	}	
	public long getTotalNoOfRecordsInStage() {
		return totalNoOfRecordsInStage;
	}
	public void setTotalNoOfRecordsInStage(long totalNoOfRecordsInStage) {
		this.totalNoOfRecordsInStage = totalNoOfRecordsInStage;
	}
	public boolean isDeleted() {
		return isDeleted;
	}
	public void setDeleted(boolean isDeleted) {
		this.isDeleted = isDeleted;
	}
	public long getTotalNoOfProcessedRecords() {
		return totalNoOfProcessedRecords;
	}

	public void setTotalNoOfProcessedRecords(long totalNoOfProcessedRecords) {
		this.totalNoOfProcessedRecords = totalNoOfProcessedRecords;
	}

	public long getTotalNoOfDeletedRecords() {
		return totalNoOfDeletedRecords;
	}

	public void setTotalNoOfDeletedRecords(long totalNoOfDeletedRecords) {
		this.totalNoOfDeletedRecords = totalNoOfDeletedRecords;
	}
	
	/**
	 * @return the sourceId
	 */
	public long getSourceId() {
		return sourceId;
	}
	/**
	 * @param sourceId the sourceId to set
	 */
	public void setSourceId(long sourceId) {
		this.sourceId = sourceId;
	}
	@Override
	public String toString() {
		return "FileSummaryModel [id=" + id + ", batchId=" + batchId
				+ ", templateId=" + templateId + ", fileName=" + fileName
				+ ", totalNoOfRecordsInStage=" + totalNoOfRecordsInStage
				+ ", totalNoOfRecords=" + totalNoOfRecords + " timeStamp="+timeStamp+"]";
	}
	public Date getTimeStamp() {
		return timeStamp;
	}
	public void setTimeStamp(Date timeStamp) {
		this.timeStamp = timeStamp;
	}
}
