package com.skuview.importsku.model;

import java.util.Map;

import javax.persistence.Id;

import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "STAGING_SKU_INFO")
public class StagingSKUInfo {

	@Id
	private long id;
	private long fileId;
	private long sourceId;
	private Map<String,String> attributeValues;
	private int status;
	private Map<String,String> attributeErrors;

	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public long getFileId() {
		return fileId;
	}
	public void setFileId(long fileId) {
		this.fileId = fileId;
	}
	public Map<String, String> getAttributeValues() {
		return attributeValues;
	}
	public void setAttributeValues(Map<String, String> attributeValues) {
		this.attributeValues = attributeValues;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	
	public Map<String, String> getAttributeErrors() {
		return attributeErrors;
	}
	
	public void setAttributeErrors(Map<String, String> attributeErrors) {
		this.attributeErrors = attributeErrors;
	}
	
	public long getSourceId() {
		return sourceId;
	}
	public void setSourceId(long sourceId) {
		this.sourceId = sourceId;
	}
	@Override
	public String toString() {
		return "StagingSKUInfo [id=" + id + ", fileId=" + fileId
				+ ", attributeValues=" + attributeValues + ", status=" + status
				+ "]";
	}	
	
}
