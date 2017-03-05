package com.skuview.importsku.model;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.Id;

import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "SKU_INFO")
public class SKUInfo {

	@Id
	private long id;
	private long fileId;
	private long batchId;
	private long sourceId;
	private Map<Long,String> attributeValues;
	private int processingType;
	private int mergeRecType;
	private int mergeStatus;
	private int curateStatus;
	private boolean isDeleted = false;
	private boolean isLocked = false;
	private Date lasUpdatedDate;
	private Date curateLastUpdatedDate;
	private Date createdDate;
	private Map<Long,String> attrValidationErr;
	private boolean isSorted = false;
	private Map<Long,String> attrMergeErr;
	private String curationComments;
	private long mergeId;
	private boolean isErroredSKU;	//To check whether the sku is clean or errored during import
	private Map<Long,List<String>> attrCurateErr;		//To update curation error in SKUInfo
	
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
	public long getBatchId() {
		return batchId;
	}
	public void setBatchId(long batchId) {
		this.batchId = batchId;
	}
	public Map<Long, String> getAttributeValues() {
		return attributeValues;
	}
	public void setAttributeValues(Map<Long, String> attributeValues) {
		this.attributeValues = attributeValues;
	}
	public int getProcessingType() {
		return processingType;
	}
	public void setProcessingType(int processingType) {
		this.processingType = processingType;
	}
	public int getMergeRecType() {
		return mergeRecType;
	}
	public void setMergeRecType(int mergeRecType) {
		this.mergeRecType = mergeRecType;
	}
	public int getMergeStatus() {
		return mergeStatus;
	}
	public void setMergeStatus(int mergeStatus) {
		this.mergeStatus = mergeStatus;
	}
	public boolean isDeleted() {
		return isDeleted;
	}
	public void setDeleted(boolean isDeleted) {
		this.isDeleted = isDeleted;
	}
	public Date getLasUpdatedDate() {
		return lasUpdatedDate;
	}
	public void setLasUpdatedDate(Date lasUpdatedDate) {
		this.lasUpdatedDate = lasUpdatedDate;
	}
	public boolean isLocked() {
		return isLocked;
	}
	public void setLocked(boolean isLocked) {
		this.isLocked = isLocked;
	}
	
	public long getSourceId() {
		return sourceId;
	}
	public void setSourceId(long sourceId) {
		this.sourceId = sourceId;
	}

	public Map<Long, String> getAttrValidationErr() {
		return attrValidationErr;
	}
	
	public void setAttrValidationErr(Map<Long, String> attrValidationErr) {
		this.attrValidationErr = attrValidationErr;
	}

	public Map<Long, String> getAttrMergeErr() {
		return attrMergeErr;
	}

	public void setAttrMergeErr(Map<Long, String> attrMergeErr) {
		this.attrMergeErr = attrMergeErr;
	}
	@Override
	public String toString() {
		return "SKUInfo [id=" + id + ", fileId=" + fileId + ", batchId="
				+ batchId + ", sourceId=" + sourceId + ", attributeValues="
				+ attributeValues + ", processingType=" + processingType
				+ ", mergeRecType=" + mergeRecType + ", mergeStatus="
				+ mergeStatus + ", isDeleted=" + isDeleted + ", isLocked="
				+ isLocked + ", lasUpdatedDate=" + lasUpdatedDate
				+ ", createdDate=" + createdDate + ", attributeErrors="
				+ attrValidationErr + "]";
	}
	/**
	 * @return the createdDate
	 */
	public Date getCreatedDate() {
		return createdDate;
	}
	/**
	 * @param createdDate the createdDate to set
	 */
	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}
	/**
	 * @return the isSorted
	 */
	public boolean isSorted() {
		return isSorted;
	}
	/**
	 * @param isSorted the isSorted to set
	 */
	public void setSorted(boolean isSorted) {
		this.isSorted = isSorted;
	}
	/**
	 * @return the curationComments
	 */
	public String getCurationComments() {
		return curationComments;
	}
	/**
	 * @param curationComments the curationComments to set
	 */
	public void setCurationComments(String curationComments) {
		this.curationComments = curationComments;
	}	
	public int getCurateStatus() {
		return curateStatus;
	}
	public void setCurateStatus(int curateStatus) {
		this.curateStatus = curateStatus;
	}
	public long getMergeId() {
		return mergeId;
	}
	public void setMergeId(long mergeId) {
		this.mergeId = mergeId;
	}
	public Date getCurateLastUpdatedDate() {
		return curateLastUpdatedDate;
	}
	public void setCurateLastUpdatedDate(Date curateLastUpdatedDate) {
		this.curateLastUpdatedDate = curateLastUpdatedDate;
	}
	public boolean isErroredSKU() {
		return isErroredSKU;
	}
	public void setErroredSKU(boolean isErroredSKU) {
		this.isErroredSKU = isErroredSKU;
	}
	public Map<Long,List<String>> getAttrCurateErr() {
		return attrCurateErr;
	}
	public void setAttrCurateErr(Map<Long,List<String>> attrCurateErr) {
		this.attrCurateErr = attrCurateErr;
	}
	
	
}
