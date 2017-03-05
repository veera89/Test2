package com.skuview.importsku.model;

import java.util.Date;

import javax.persistence.Id;

import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "FILE_LOCATION_INFO")
public class FileLocationInfo {

	@Id
	private long id;
	private String filePath;
	private String fileName;
	private long templateId;
	private int status;
	private long userId;
	private Date requestedDate;
	private long fileSummaryId;
	private long trackFileStatusId;
	private int importType;                  // 1 - Online; 2 - Offline;
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public long getTemplateId() {
		return templateId;
	}
	public void setTemplateId(long templateId) {
		this.templateId = templateId;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}	
	public long getUserId() {
		return userId;
	}
	public void setUserId(long userId) {
		this.userId = userId;
	}
	public Date getRequestedDate() {
		return requestedDate;
	}
	public void setRequestedDate(Date requestedDate) {
		this.requestedDate = requestedDate;
	}
	public long getFileSummaryId() {
		return fileSummaryId;
	}
	public void setFileSummaryId(long fileSummaryId) {
		this.fileSummaryId = fileSummaryId;
	}
	public long getTrackFileStatusId() {
		return trackFileStatusId;
	}
	public void setTrackFileStatusId(long trackFileStatusId) {
		this.trackFileStatusId = trackFileStatusId;
	}
	public int getImportType() {
		return importType;
	}
	public void setImportType(int importType) {
		this.importType = importType;
	}
	@Override
	public String toString() {
		return "FileLocationInfo [id=" + id + ", filePath=" + filePath
				+ ", fileName=" + fileName + ", templateId=" + templateId
				+ ", status=" + status + ", userId=" + userId
				+ ", requestedDate=" + requestedDate + ", fileSummaryId="
				+ fileSummaryId + ", trackFileStatusId=" + trackFileStatusId
				+ ", importType=" + importType + "]";
	}
		
}
