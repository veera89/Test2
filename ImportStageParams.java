package com.skuview.importsku.model;

public class ImportStageParams {

	private String newFileName;
	private FileSummaryModel fileSummaryModel;
	private TrackFileStatus trackFileStatusModel;
	
	public String getNewFileName() {
		return newFileName;
	}
	public void setNewFileName(String newFileName) {
		this.newFileName = newFileName;
	}
	public FileSummaryModel getFileSummaryModel() {
		return fileSummaryModel;
	}
	public void setFileSummaryModel(FileSummaryModel fileSummaryModel) {
		this.fileSummaryModel = fileSummaryModel;
	}
	public TrackFileStatus getTrackFileStatusModel() {
		return trackFileStatusModel;
	}
	public void setTrackFileStatusModel(TrackFileStatus trackFileStatusModel) {
		this.trackFileStatusModel = trackFileStatusModel;
	}
	@Override
	public String toString() {
		return "ImportStageParams [newFileName=" + newFileName
				+ ", fileSummaryModel=" + fileSummaryModel
				+ ", trackFileStatusModel=" + trackFileStatusModel + "]";
	}
		
}
