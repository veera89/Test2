package com.skuview.importsku.model;

import java.util.Date;

import javax.persistence.Id;

import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "TRACK_FILE_STATUS")
public class TrackFileStatus {

	@Id
	private long id;
	private long fileId;
	private long statusId;
	private Date timestamp;
	private long userId;
	private String comments;
	
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
	public long getStatusId() {
		return statusId;
	}
	public void setStatusId(long statusId) {
		this.statusId = statusId;
	}
	public Date getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	public long getUserId() {
		return userId;
	}
	public void setUserId(long userId) {
		this.userId = userId;
	}
	@Override
	public String toString() {
		return "TrackFileStatus [id=" + id + ", fileId=" + fileId
				+ ", statusId=" + statusId + ", timestamp=" + timestamp
				+ ", userId=" + userId + "]";
	}
	public String getComments() {
		return comments;
	}
	public void setComments(String comments) {
		this.comments = comments;
	}		
}
