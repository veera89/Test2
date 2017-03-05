package com.skuview.importsku.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import com.skuview.category.dao.constants.Constants;
import com.skuview.common.dao.ProductKeyUtilDAO;
import com.skuview.common.model.ImportSKUModel;
import com.skuview.common.model.ProcessingParamSourceModel;
import com.skuview.common.model.ProdAttributeModel;
import com.skuview.common.model.SKUHistory;
import com.skuview.common.model.TemplateInfo;
import com.skuview.common.util.DataReader;
import com.skuview.common.util.Importpath;
import com.skuview.common.util.ReaderFactory;
import com.skuview.common.vo.DraftVersionVO;
import com.skuview.exceptionreport.dao.ExceptionReportDAO;
import com.skuview.importsku.ImportSKU;
import com.skuview.importsku.dao.ImportDAO;
import com.skuview.importsku.dao.StagingImportDAO;
import com.skuview.importsku.model.FileLocationInfo;
import com.skuview.importsku.model.FileSummaryModel;
import com.skuview.importsku.model.ImportStageParams;
import com.skuview.importsku.model.SKUInfo;
import com.skuview.importsku.model.StagingSKUInfo;
import com.skuview.importsku.model.StatusReference;
import com.skuview.importsku.model.TrackFileStatus;
import com.skuview.merge.dao.MergeDAO;
import com.skuview.processingparameter.dao.ProcessingParameterDAO;
import com.skuview.productattribute.ProdAttrBO;
import com.skuview.productimportreport.dao.ProductImportReportDAO;
import com.skuview.rules.bo.RulesBO;
import com.skuview.rules.model.TransformationRule;
import com.skuview.sequence.dao.SequenceGeneratorDAO;
import com.skuview.template.dao.TemplateDAO;
import com.skuview.template.model.TemplateModel;
import com.skuview.template.model.TemplateRecordTypeModel;
import com.skuview.user.model.UserDetails;
import com.skuview.validation.AttributeValidator;

/**
 * This is the Business layer to import the SKU's into our system
 * 
 * @author 372153
 *
 */
public class ImportSKUImpl implements ImportSKU {

	private static final Logger logger = Logger.getLogger(ImportSKUImpl.class);
	
	@Autowired
	public ReaderFactory readerFactory;
	
	//@Autowired
	private FileSummaryModel fileSummary;
	
	//@Autowired
	private TrackFileStatus trackFileStatus;
	
	@Autowired
	private StagingSKUInfo stageSkuInfo;
	
	@Autowired
	private StagingImportDAO stageImportDAO;
	
	@Autowired
	private ImportDAO importDAO;
	
	@Autowired 
	private SequenceGeneratorDAO sequenceGenDAO;
	
	@Autowired
	private TemplateDAO templateDAO;
	
	@Autowired
	private SKUHistory skuHistory;
	
	@Autowired
	private MergeDAO curationDAO;
	
	@Autowired
	public FileLocationInfo fileLocationInfo;
	
	@Autowired
	private ProductImportReportDAO productImportReportDAO;
	
	@Autowired
	private AttributeValidator attributeValidator;
	
	@Autowired
	private ProdAttrBO prodAttrBO;
	
	private String upcAttrIdStr = null;
	
	@Autowired
	private ExceptionReportDAO exceptionReportDAO;
	
	@Autowired
	private ProcessingParameterDAO processingParameterDAO;
	
	@Autowired
	private Importpath importPath;
	
	@Autowired
	private ProductKeyUtilDAO productKeyUtilDAO;
	
	@Autowired
	private RulesBO rulesBO;
	
	private int importBatchSize = 0;
	
	private String transformationPropPath;
	
	//DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	
	/* (non-Javadoc)
	 * @see com.skuview.importsku.ImportSKU#importDataToStaging(com.skuview.common.model.ImportSKUModel, com.skuview.common.util.ReaderFactory)
	 */
	@Override
	public FileSummaryModel importDataToStaging(String newFileName, ImportSKUModel importSKUModel, ReaderFactory readerFactory,long userId) throws Exception {
		
		MultipartFile file = importSKUModel.getSkuFile();
		long fileSummaryId = getFileSummaryId();
		
		if (!file.isEmpty()) {
			 logger.debug(file.getOriginalFilename());
             Date requestedDate = new Timestamp(new Date().getTime());
             InputStream inStream = null;
             
         	Map<String, String> settingsMap = curationDAO.getSettingsColletion();
			importBatchSize = Integer.parseInt(settingsMap.get(Constants.IMPORT_BATCHSIZE));
			
             	System.out.println("ImportPath ===="+ importPath.getImportPath());
	            File outputFile =new File(importPath.getImportPath()+newFileName);
	         	inStream = new FileInputStream(outputFile);
	         	
	             
	         	DataReader dataReader = readerFactory.getReaderInstance(FilenameUtils
	     				.getExtension(file.getOriginalFilename()));
	         	dataReader.open(inStream);
	     		
	     		//Get Source ID
	     		TemplateModel tempModel=importDAO.getTemplateModel(importSKUModel.getTemplateId());
	     		
        		fileSummary.setId(fileSummaryId);
        		fileSummary.setBatchId(getStageBatchId());
        		fileSummary.setFileName(newFileName);
        		fileSummary.setTemplateId(importSKUModel.getTemplateId());
        			if(tempModel!=null)
        		fileSummary.setSourceId(tempModel.getSourceId());
        		fileSummary.setTotalNoOfRecordsInStage(0);
        		fileSummary.setTotalNoOfRecords(0);
        		insertFileSummary(fileSummary);
        		
        		trackFileStatus.setStatusId(Constants.TRACKFILESTATUS_REQSUBMITTED);           
        		trackFileStatus.setFileId(fileSummaryId);
        		trackFileStatus.setUserId(userId);             // 1 - Admin - ref from User collection
        		trackFileStatus.setTimestamp(requestedDate);
        		trackFileStatus.setId(getTrackFileStatusId());
        		insertTrackFileStatus(trackFileStatus);
        		
        		List<String> colNames = dataReader.getColumns();
        		List<StagingSKUInfo> stageSKUInfoList = new ArrayList<StagingSKUInfo>();
        		long totalNoOfRecordsInStage = 0;
        		while (true) {
        			Map<String, String> data = dataReader.readRow();
        			if (data == null) {
        				break;
        			}
        			logger.debug(data.toString());
        			
        			stageSkuInfo = new StagingSKUInfo();
        			stageSkuInfo.setFileId(fileSummaryId);
        				if(tempModel!=null)
        			stageSkuInfo.setSourceId(tempModel.getSourceId());
        			stageSkuInfo.setAttributeValues(data);
        			stageSkuInfo.setStatus(1); // 1 - New; 2 - Completed; 3 - Error
        			stageSkuInfo.setId(sequenceGenDAO.getNextSequenceId(Constants.STAGE_SKU_INFO_SEQID));
        //			insertStageSKUInfo(stageSkuInfo);
        			totalNoOfRecordsInStage++;
        			stageSKUInfoList.add(stageSkuInfo);
        			if(stageSKUInfoList.size() == importBatchSize) {
        				insertStageSKUInfoAsBatch(stageSKUInfoList);
        				stageSKUInfoList = new ArrayList<StagingSKUInfo>();
        			}
        		}
        		
        		if(stageSKUInfoList!=null && stageSKUInfoList.size()>0) {
        			insertStageSKUInfoAsBatch(stageSKUInfoList);
        			stageSKUInfoList = null;
        		}
        		
        		dataReader.close();     
        		
        		Date stagedDate = new Timestamp(new Date().getTime());
        		fileSummary.setTotalNoOfRecordsInStage(totalNoOfRecordsInStage);
        		updateFileSummary(fileSummary);	
        		
        		logger.debug("Import - Total number of records imported to staging table : "+totalNoOfRecordsInStage);
        		
        		trackFileStatus.setStatusId(Constants.TRACKFILESTATUS_STAGED);           
        		trackFileStatus.setTimestamp(stagedDate);
        		trackFileStatus.setId(getTrackFileStatusId());
        		insertTrackFileStatus(trackFileStatus);
           
	        }
		 
		 return fileSummary;
		
	}
	
	/* (non-Javadoc)
	 * @see com.skuview.importsku.ImportSKU#trackFileSummary(java.lang.String, long)
	 */
	@Override
	public FileSummaryModel trackFileSummary(String newFileName, long templateId) throws Exception {
		long fileSummaryId = getFileSummaryId();
		if (newFileName != null) {
			String fileName = FilenameUtils.getBaseName(newFileName);
			long tempId = 0;
			long sourceId = 0;
			TemplateModel tempModel = null;
			
			if (templateId == 0) {
				String sourceName = fileName.split("_")[0];
				logger.debug("sourceName :" + sourceName);
				sourceId = importDAO.getSourceId(sourceName);
				tempId = getTemplateId(sourceName);
			} else {
				tempModel = importDAO.getTemplateModel(templateId);
				sourceId = tempModel.getSourceId();
				tempId = templateId;
			}
			logger.debug("tempId :"+tempId);
			
			fileSummary = new FileSummaryModel();
			fileSummary.setId(fileSummaryId);
			fileSummary.setBatchId(getStageBatchId());
			fileSummary.setFileName(fileName+"."+FilenameUtils.getExtension(newFileName));
			fileSummary.setTemplateId(tempId);
			fileSummary.setSourceId(sourceId);
			fileSummary.setTotalNoOfRecordsInStage(0);
			fileSummary.setTotalNoOfRecords(0);
			fileSummary.setTimeStamp(new Timestamp(new Date().getTime()));
			insertFileSummary(fileSummary);
			logger.debug(fileSummary);
		}
		return fileSummary;
	}
	
	/* (non-Javadoc)
	 * @see com.skuview.importsku.ImportSKU#trackFileStutusModel(long, long)
	 */
	@Override
	public TrackFileStatus trackFileStutusModel(long fileSummaryId, long userId) throws Exception {
		Date requestedDate = new Timestamp(new Date().getTime());
		
		trackFileStatus = new TrackFileStatus();
		trackFileStatus.setStatusId(Constants.TRACKFILESTATUS_REQSUBMITTED);           
		trackFileStatus.setFileId(fileSummaryId);
		trackFileStatus.setUserId(userId);             
		trackFileStatus.setTimestamp(requestedDate);
		trackFileStatus.setId(getTrackFileStatusId());
		insertTrackFileStatus(trackFileStatus);
		return trackFileStatus;
	}
	
	/* (non-Javadoc)
	 * @see com.skuview.importsku.ImportSKU#importDataToStagingByJob(com.skuview.importsku.model.ImportStageParams)
	 */
	@Override
	public ImportStageParams importDataToStagingByJob(ImportStageParams importStageParams) throws Exception {
		
		String newFileName = importStageParams.getNewFileName();
		FileSummaryModel fileSummaryModel = importStageParams.getFileSummaryModel();
		TrackFileStatus trackFileStatusModel = importStageParams.getTrackFileStatusModel();
				
		if (newFileName != null) {
			logger.debug(newFileName);
			InputStream inStream = null;
			Map<String, String> settingsMap = curationDAO.getSettingsColletion();
			importBatchSize = Integer.parseInt(settingsMap.get(Constants.IMPORT_BATCHSIZE));
			
			if(fileSummaryModel != null) {
            	
            	File outputFile =new File(newFileName);
	         	inStream = new FileInputStream(outputFile);
	             
	         	DataReader dataReader = readerFactory.getReaderInstance(FilenameUtils
	     				.getExtension(newFileName));
	         	dataReader.open(inStream);
	     		
        		List<String> colNames = dataReader.getColumns();
        		/* Mandatory, null and dot check */
        		boolean hasMandField=columnHasMandField(fileSummaryModel.getTemplateId(), colNames, trackFileStatusModel);
        		if(!hasMandField) {
        			
        			logger.debug("Mandatory Feild is missing in the file : ");
        			trackFileStatusModel.setStatusId(Constants.TRACKFILESTATUS_REJECTED);           
        			trackFileStatusModel.setTimestamp(new Timestamp(new Date().getTime()));
        			trackFileStatusModel.setId(getTrackFileStatusId());
        			insertTrackFileStatus(trackFileStatusModel);
        			
        			importStageParams.setFileSummaryModel(fileSummaryModel);
            		importStageParams.setTrackFileStatusModel(trackFileStatusModel);
        			
            		return importStageParams;
        		}
        		
        		trackFileStatusModel.setStatusId(Constants.TRACKFILESTATUS_STAGING);           
        		trackFileStatusModel.setTimestamp(new Timestamp(new Date().getTime()));
        		trackFileStatusModel.setId(getTrackFileStatusId());
        		insertTrackFileStatus(trackFileStatusModel);
        		
        		List<StagingSKUInfo> stageSKUInfoList = new ArrayList<StagingSKUInfo>();
        		long totalNoOfRecordsInStage = 0;
        		long skuViewStageId = sequenceGenDAO.getNextSequenceId(Constants.STAGE_SKU_INFO_SEQID);
                sequenceGenDAO.updateSequence((skuViewStageId+importBatchSize), Constants.STAGE_SKU_INFO_SEQID);
        		while (true) {
        			Map<String, String> data = dataReader.readRow();
        			if (data == null) {
        				break;
        			}
        			//logger.debug(data.toString());
        			
        			stageSkuInfo = new StagingSKUInfo();
        			stageSkuInfo.setFileId(fileSummaryModel.getId());
        			stageSkuInfo.setSourceId(fileSummaryModel.getSourceId());
        			stageSkuInfo.setAttributeValues(data);
        			stageSkuInfo.setStatus(1); // 1 - New; 2 - Completed; 3 - Error
        			stageSkuInfo.setId(skuViewStageId);
        			totalNoOfRecordsInStage++;
        			stageSKUInfoList.add(stageSkuInfo);
        			skuViewStageId++;
        			if(stageSKUInfoList.size() == importBatchSize) {
        				insertStageSKUInfoAsBatch(stageSKUInfoList);
        				stageSKUInfoList = new ArrayList<StagingSKUInfo>();
        				skuViewStageId = sequenceGenDAO.getNextSequenceId(Constants.STAGE_SKU_INFO_SEQID);
                        sequenceGenDAO.updateSequence((skuViewStageId+importBatchSize), Constants.STAGE_SKU_INFO_SEQID);
        			}
        		}
        		
        		if(stageSKUInfoList!=null && stageSKUInfoList.size()>0) {
        			insertStageSKUInfoAsBatch(stageSKUInfoList);
        			stageSKUInfoList = null;
        			skuViewStageId = 0;
        		}
        		
        		dataReader.close();     
        		
        		Date stagedDate = new Timestamp(new Date().getTime());
        		fileSummaryModel.setTotalNoOfRecordsInStage(totalNoOfRecordsInStage);
        		updateFileSummary(fileSummaryModel);	
        		
        		logger.debug("Import - Total number of records imported to staging table : "+totalNoOfRecordsInStage);
        		
        		trackFileStatusModel.setStatusId(Constants.TRACKFILESTATUS_STAGED);           
        		trackFileStatusModel.setTimestamp(stagedDate);
        		trackFileStatusModel.setId(getTrackFileStatusId());
        		insertTrackFileStatus(trackFileStatusModel);
        		
        		importStageParams.setFileSummaryModel(fileSummaryModel);
        		importStageParams.setTrackFileStatusModel(trackFileStatusModel);
           
            } else {
            	logger.debug("Source not found");
            }
	     }
		 return importStageParams;
	}
	
	private boolean columnHasMandField(long tempId, List<String> colNames, TrackFileStatus trackFileStatusModel ) throws Exception { 
		
		boolean hasMandField = false;
		
		if(null!=colNames && colNames.contains(null)){
			hasMandField = false;
			trackFileStatusModel.setComments(Constants.REJECT_NULL_DOT);
			return hasMandField;
		}
		for(String column : colNames) {
			if(column.contains(Constants.STRING_DOT)) { 
				hasMandField = false;
				trackFileStatusModel.setComments(Constants.REJECT_NULL_DOT);
				return hasMandField;				
			}
		}
		
		TemplateRecordTypeModel tempRecTypeModel;
		Map<String, Long> tempAttMap=null;
		List<String> productKeysList = new ArrayList<String>();
		List<String> listTOComapare = new ArrayList<String>();
		productKeysList = productKeyUtilDAO.getProductKeys();
		tempRecTypeModel = templateDAO.getTemplateRecordTypeById(tempId);
		tempAttMap = tempRecTypeModel.getAttributeMap();		
		for(String mandFeild:productKeysList) {
			for(String col:colNames) {
				if(null!=tempAttMap && null!=tempAttMap.get(col) && mandFeild.equals(tempAttMap.get(col).toString())) {
				
					//if(mandFeild.equals(tempAttMap.get(col))){
						listTOComapare.add(mandFeild);
						break;
					//}
				}
			}
		}
		if(productKeysList.size() == listTOComapare.size()) {
			hasMandField = true;
		} else{
			trackFileStatusModel.setComments(Constants.REJECT_MANDATORY_HEADER);
		}
		
		return hasMandField;
	}

	/* (non-Javadoc)
	 * @see com.skuview.importsku.ImportSKU#importDataToProd(com.skuview.importsku.model.FileSummaryModel)
	 */
	@Override
	public String importDataToProd(ImportStageParams importStageParams,UserDetails userSess) throws Exception {

		FileSummaryModel fileSummary = importStageParams.getFileSummaryModel();
		TrackFileStatus trackFileStatusModel = importStageParams.getTrackFileStatusModel();
		
		Map<Long, Properties> transformMap = new HashMap<Long, Properties>();
		Map<Long, Boolean> replaceValueMap = new HashMap<Long, Boolean>();
		getTransformationRules(transformMap, replaceValueMap);
		
		trackFileStatusModel.setStatusId(Constants.TRACKFILESTATUS_MOVINGTOPROD);           
		trackFileStatusModel.setTimestamp(new Timestamp(new Date().getTime()));
		trackFileStatusModel.setId(getTrackFileStatusId());
		insertTrackFileStatus(trackFileStatusModel);
		
		//List<FileSummaryModel> fileSummaryModels = stageImportDAO.getStagingFileSummaryByStatus(2);
		List<StagingSKUInfo> stageSkuInfoList = null;
		SKUInfo skuInfo = null;
		TemplateRecordTypeModel tempRecTypeModel;
		Map<String, Long> tempAttMap=null;
		String tempAttrKey = null;
		long tempAttrValue;
		String attributeValue = "";
		Map<Long,String> attributeValues;
		Map<ProdAttributeModel,String> attributeDatatypeValues=new HashMap<ProdAttributeModel,String>();
		String importFeedback = "";
		Map<String, String> stageAttrMap = null;
		String productId = null;
		List<String> upcList = new ArrayList<String>();;
		String upcItemKey = null;
		
		boolean sortedFlag = false; // variable added for sorting, if the user wants to sort Length/width/Height then this flag will be enabled
		
		
		Map<Long, Set<String>> sourceUPCMap = new HashMap<Long, Set<String>>();
		Set<String> upcSet = new HashSet<String>();
		
		stageSkuInfoList = new ArrayList<StagingSKUInfo>();
		 
		Map<String, String> settingsMap = curationDAO.getSettingsColletion();
		upcAttrIdStr = settingsMap.get(Constants.UPC);
		long upcAttrId = Long.parseLong(upcAttrIdStr);
		//long itemNoAttrId = Long.parseLong(settingsMap.get(Constants.ITEM_NUMBER));
		long compPrdKeyAttrId =  Long.parseLong(settingsMap.get(Constants.COMP_PRDKEY));
		String compPrdKey;
		/*String upcValue;
		String itemNumber;*/
		
		/*String mandtryValStr=settingsMap.get(Constants.IMPORT_MANDATORY_ATTR);
		//List<String> mandAttrList = prodAttrBO.getAllMandatoryAttrId();
		List<String> mandatoryAttrList = new ArrayList<String>();
		if(null!=mandtryValStr) {
			mandatoryAttrList = Arrays.asList(mandtryValStr.split(";"));
		}*/
		
		List<String> productKeysList = new ArrayList<String>();
		productKeysList = productKeyUtilDAO.getProductKeys();
		//System.err.println("upcAttrId-"+upcAttrId);
		
		//stageSkuInfoList = stageImportDAO.getStagingSKUInfoByFileId(fileSummary.getId());
		logger.debug("File id from importskuimpl.java :"+fileSummary.getId());
		
		tempRecTypeModel = templateDAO.getTemplateRecordTypeById(fileSummary.getTemplateId());
		tempAttMap = tempRecTypeModel.getAttributeMap();
		Set<Map.Entry<String, Long>> tempAttrSet = tempAttMap.entrySet();
		long totalNoOfRecords = 0;
		
		
		boolean isValidRecord;
		
		Map<Long,ProdAttributeModel> attrMap=new HashMap<Long,ProdAttributeModel>();
		int skipValue = 0;
		int limitConstant = importBatchSize;
		
		long noOfRecords = stageImportDAO.getCountOfStagingSKUInfoByFileId(fileSummary.getId());
		try{
			if(noOfRecords!=0) {
				
				long noOfIterations = noOfRecords/limitConstant;
				List<SKUInfo> skuInfoList = new ArrayList<SKUInfo>();
				long skuInfoId = sequenceGenDAO.getNextSequenceId(Constants.SKU_INFO_SEQID);
	            sequenceGenDAO.updateSequence((skuInfoId+importBatchSize), Constants.SKU_INFO_SEQID);
				for(int iteration = 0 ; iteration <= noOfIterations; iteration++) {
					
					stageSkuInfoList = stageImportDAO.getStagingSKUInfoByFileIdWithLimit(fileSummary.getId(), skipValue, limitConstant);
					for(StagingSKUInfo stagingSKUInfo : stageSkuInfoList) {
						
						isValidRecord=true;
						attributeValues = new LinkedHashMap<Long, String>();
						attributeDatatypeValues=new HashMap<ProdAttributeModel,String>();
						stageAttrMap = stagingSKUInfo.getAttributeValues();
						Map<String,String> stagingAttributeErrors=new HashMap<String,String>();
						Map<Long,String> skuInfoAttributeErrors=new HashMap<Long,String>();
						Map<Long,String> attrNmRecordMap=new HashMap<Long,String>();
						
						
						for(Iterator<Map.Entry<String, Long>> itr = tempAttrSet.iterator(); itr.hasNext();) {
							Map.Entry<String, Long> tempAttrEntry = (Map.Entry<String, Long>) itr.next();
							tempAttrKey = tempAttrEntry.getKey();       											// attribute name from template
							if(tempAttrEntry.getValue() == null){
								continue;
							}
							tempAttrValue = tempAttrEntry.getValue();   											// attribute id from template
							attributeValue = stageAttrMap.get(tempAttrKey)!=null?stageAttrMap.get(tempAttrKey):"";
							
							//transformationRules
							if(transformMap.size() > 0 && transformMap.containsKey(tempAttrValue) && attributeValue != "" && 
									((transformMap.get(tempAttrValue)).getProperty(attributeValue) != null)){
								attributeValues.put(tempAttrValue, (transformMap.get(tempAttrValue)).getProperty(attributeValue));
							}else if(transformMap.size() > 0 && transformMap.containsKey(tempAttrValue) && attributeValue != "" && 
									((transformMap.get(tempAttrValue)).getProperty(attributeValue) == null) && (replaceValueMap.get(tempAttrValue))){
								attributeValues.put(tempAttrValue, "");
							}else{
								attributeValues.put(tempAttrValue, attributeValue);
							}
							
							attrNmRecordMap.put(tempAttrValue, tempAttrKey);
						
							if(tempAttrValue == upcAttrId && attributeValue.replaceAll("\\s+", "").trim().length()!=0){
								productId = attributeValue;
							}
							
							// Staging - Mandatory block check 
							
							if(productKeysList.contains(String.valueOf(tempAttrValue))){
							if(attributeValue.replaceAll("\\s+", "").trim().length()==0) {
								isValidRecord=false;
								stagingAttributeErrors.put(tempAttrKey, Constants.MANDATORY_VALUE);
								}
							}
							
							// SKUInfo DB Validation
							
							if(isValidRecord){
										// Get Attribute List for Data type check
	
							if(attrMap.get(tempAttrValue)==null){
							ProdAttributeModel attrModel=importDAO.getAttributeModel(tempAttrValue);  
							attrMap.put(tempAttrValue, attrModel);
							}
							if(attrMap.get(tempAttrValue)!=null && attributeValue.replaceAll("\\s+", "").trim().length()!=0 )
							attributeDatatypeValues.put(attrMap.get(tempAttrValue), attributeValue.replaceAll("\\s+", "").trim());  	//  Pre-Prod Attribute validation 
							
										// Attribute Mandatory Check 
							if(Arrays.asList(attrMap.get(tempAttrValue).getQualityClass()).contains(Constants.REQUIRED)){
								if(attributeValue.replaceAll("\\s+", "").trim().length()==0) {
									skuInfoAttributeErrors.put(tempAttrValue, Constants.MANDATORY_VALUE);
								}
							}
							
							
							}
						}
						compPrdKey = "";
						for (String attId : productKeysList) {
							String tempKey = (String) (attributeValues.get(Long.parseLong(attId))!=null?attributeValues.get(Long.parseLong(attId)):"");
							if(!compPrdKey.isEmpty())
								compPrdKey = compPrdKey + "_" + tempKey;
							else
								compPrdKey = tempKey;
						}
						
						//upcValue = (String) (attributeValues.get(upcAttrId)!=null?attributeValues.get(upcAttrId):"");
						//itemNumber = (String) (attributeValues.get(itemNoAttrId)!=null?attributeValues.get(itemNoAttrId):"");
						//compPrdKey = upcValue +"_"+ itemNumber;
						attributeValues.put(compPrdKeyAttrId, compPrdKey);
						// sorting implemented for Length/Width/Height
						if(settingsMap.get(Constants.SORT_BEFORE).equalsIgnoreCase(Constants.YES)){
							
							String length = null;
							String width = null;
							String height = null;
	
							if(attributeValues.get(Long.parseLong(settingsMap.get("Length"))) != null && 
									!Constants.EMPTY_STRING.equalsIgnoreCase((String) attributeValues.get(Long.parseLong(settingsMap.get("Length"))))){
								length = (String) attributeValues.get(Long.parseLong(settingsMap.get("Length")));
							} else {
								length = "0";
							}
							if(attributeValues.get(Long.parseLong(settingsMap.get("Width"))) != null && 
									!Constants.EMPTY_STRING.equalsIgnoreCase((String) attributeValues.get(Long.parseLong(settingsMap.get("Width"))))){
								width = (String) attributeValues.get(Long.parseLong(settingsMap.get("Width")));
							} else {
								width = "0";
							}
							if(attributeValues.get(Long.parseLong(settingsMap.get("Height"))) != null && 
									!Constants.EMPTY_STRING.equalsIgnoreCase((String) attributeValues.get(Long.parseLong(settingsMap.get("Height"))))){
								height = (String) attributeValues.get(Long.parseLong(settingsMap.get("Height")));
							} else {
								height = "0";
							}
							//String length = attributeValues.get(Long.parseLong(settingsMap.get("Length"))) != null ?attributeValues.get(Long.parseLong(settingsMap.get("Length"))) : "0";
							//String width = attributeValues.get(Long.parseLong(settingsMap.get("Width"))) != null ?attributeValues.get(Long.parseLong(settingsMap.get("Width"))) : "0";
							//String height = attributeValues.get(Long.parseLong(settingsMap.get("Height"))) != null ?attributeValues.get(Long.parseLong(settingsMap.get("Height"))) : "0";
							if((NumberUtils.isNumber(length)) && (NumberUtils.isNumber(width)) && (NumberUtils.isNumber(height))) {
								double[] sortArray = {Double.parseDouble(length), Double.parseDouble(width), Double.parseDouble(height)};
								Arrays.sort(sortArray);
								attributeValues.put(Long.parseLong(settingsMap.get("Length")), String.valueOf(sortArray[2]));
								attributeValues.put(Long.parseLong(settingsMap.get("Width")), String.valueOf(sortArray[1]));
								attributeValues.put(Long.parseLong(settingsMap.get("Height")), String.valueOf(sortArray[0]));
							} 
							sortedFlag = true;
						} 
						
						/* This else block added overwrite function, if the same UPC, item number and Source are same, 
						 then existing record will be deleted for the same*/
						
						// :TODO - Re look is required to check the logic to find out the existing UPC's
						
						if(settingsMap.get(Constants.NEWVERSION_FROMSRC).equalsIgnoreCase(Constants.OVER_WRITE)){
							upcItemKey = settingsMap.get(Constants.COMP_PRDKEY);
							String UpcItemId = (String) attributeValues.get(Long.parseLong(upcItemKey));
							//boolean recordExist = importDAO.isSameItemandUPExist(fileSummary.getSourceId(), UpcItemId, upcItemKey);
							upcList.add(UpcItemId);
							//if(importDAO.isSameItemandUPExist(fileSummary.getSourceId(), UpcItemId, upcItemKey)){
							if(!sourceUPCMap.isEmpty() &&  sourceUPCMap.get(fileSummary.getSourceId()).contains(UpcItemId)){
								isValidRecord = false;
								stagingAttributeErrors.put(upcItemKey, Constants.DUBLICATE);
							}
							//boolean isDeleted = importDAO.deleteSameItemandUPExist(fileSummary.getSourceId(), UpcItemId, upcItemKey);
							//System.out.println("Record deleted is "+ isDeleted);
							//}
						} /*else if(settingsMap.get(Constants.NEWVERSION_FROMSRC).equalsIgnoreCase(Constants.DISCARD)){
							String upcItemKey = settingsMap.get(Constants.COMP_PRDKEY);
							String UpcItemId = attributeValues.get(Long.parseLong(upcItemKey));
							//boolean recordExist = importDAO.isSameItemandUPExist(fileSummary.getSourceId(), UpcItemId, upcItemKey);
							if((importDAO.isSameItemandUPExist(fileSummary.getSourceId(), UpcItemId, upcItemKey))
									|| (!sourceUPCMap.isEmpty() &&  sourceUPCMap.get(fileSummary.getSourceId()).contains(UpcItemId))){
								stagingAttributeErrors.put(upcItemKey, Constants.DUBLICATE);
								isValidRecord = false;
							}
						}*/
						//  Attribute data type validation - starts
							
							List<DraftVersionVO> ValidationErrorList=attributeValidator.getValidAttribute(attributeDatatypeValues);
											
							for(DraftVersionVO draftErr: ValidationErrorList){
								if(draftErr.isError()){
									skuInfoAttributeErrors.put(draftErr.getAttributeId(), Constants.DATATYPE_ISNOT +draftErr.getRuleApplied()[0]);
									//Set value as empty to avoid date type format exception
									attributeValues.put(Long.valueOf(draftErr.getAttributeId()), "");
								}
								
							}	
							
							// This code is added to find the duplicate version in the same UPC, source and Item number.
							
							if(sourceUPCMap.containsKey(fileSummary.getSourceId())){
								Set<String> valueSet = sourceUPCMap.get(fileSummary.getSourceId());
								valueSet.add((String) attributeValues.get(Long.parseLong(settingsMap.get(Constants.COMP_PRDKEY))));
								upcSet = valueSet;
							} else {
								upcSet.add((String) attributeValues.get(Long.parseLong(settingsMap.get(Constants.COMP_PRDKEY))));
							}
							sourceUPCMap.put(fileSummary.getSourceId(), upcSet);
							
						//  Attribute data type validation - ends
						
						if(isValidRecord) {
								skuInfo = new SKUInfo();
								skuInfo.setFileId(fileSummary.getId());
								skuInfo.setBatchId(fileSummary.getBatchId());
								skuInfo.setSourceId(fileSummary.getSourceId());
								//skuInfo.setId(getSkuInfoId());
								skuInfo.setId(skuInfoId);
								skuInfo.setProcessingType(Constants.PROCESSING_TYPE_VERSIONS);
								skuInfo.setMergeRecType(Constants.MERGE_REC_TOBEPROCESSED);
								skuInfo.setMergeStatus(Constants.MERGE_STATUS_TOBEMERGED);
								  
								//skuInfo.setMergeStatus(Constants.NO_MERGE_STATUS);
								skuInfo.setCurateStatus(Constants.CURATE_STATUS_TOBECURATED);
								//skuInfo.setMergeStatus(Constants.NO_CURATE_STATUS);
								
								skuInfo.setCreatedDate(new Timestamp(new Date().getTime()));
								skuInfo.setLasUpdatedDate(new Timestamp(new Date().getTime()));
								skuInfo.setCurateLastUpdatedDate(new Timestamp(new Date().getTime()));
								skuInfo.setAttributeValues(attributeValues);
								skuInfo.setSorted(sortedFlag);
								if(skuInfoAttributeErrors.size()>0){
									skuInfo.setAttrValidationErr(skuInfoAttributeErrors);
									skuInfo.setErroredSKU(true);
								}
								//krishna
								
								//insertSKUInfo(skuInfo);
								skuInfoList.add(skuInfo);
								skuInfoId++;
		        			if(skuInfoList.size() == importBatchSize) {
		        				if(null != upcList && !upcList.isEmpty()){
		        					importDAO.deleteSameItemandUPExist(fileSummary.getSourceId(), upcList, upcItemKey);
		        					upcList.clear();
		        				}
		        				insertSKUInfoAsBatch(skuInfoList);
		        				skuInfoList = new ArrayList<SKUInfo>();
		        				sourceUPCMap.clear();
		        				skuInfoId = sequenceGenDAO.getNextSequenceId(Constants.SKU_INFO_SEQID);
	                            sequenceGenDAO.updateSequence((skuInfoId+importBatchSize), Constants.SKU_INFO_SEQID);
		        			}
							
		        			totalNoOfRecords++;
							
							/* Commenting the code as we are not using Sku history any where and to tune the performance*/
							
							/*skuHistory.setId(getSkuHistoryId());
							skuHistory.setProductId(productId);
							skuHistory.setOperation(Constants.SKUHISTORY_OPERATION_IMPORT);
							skuHistory.setStatus(Constants.SKUHISTORY_STATUS_SUCCESS);
							skuHistory.setUpdatedDate(new Timestamp(new Date().getTime()));
							skuHistory.setUserName(userSess.getFirstName()+" "+userSess.getLastName());
							
							insertSKUHistory(skuHistory);*/
							updateStagingStatus(Constants.STAGING_STATUS_IMPORTED,stagingSKUInfo.getId(),stagingAttributeErrors);
											
						} else {
							updateStagingStatus(Constants.STAGING_STATUS_FAILED,stagingSKUInfo.getId(),stagingAttributeErrors);
							
						}
						/*if(stagingAttributeErrors.size()>0 || skuInfoAttributeErrors.size()>0){
							skuInfoAttributeErrors.putAll(stagingAttributeErrors);
						
						List<ExceptionDetails> exceptionList=new ArrayList<ExceptionDetails>();
						
						for (Map.Entry<String, String> entry : skuInfoAttributeErrors.entrySet()){
							ExceptionDetails errMsgObj=new ExceptionDetails();
							errMsgObj.setId(sequenceGenDAO.getNextSequenceId(Constants.EXCEPTION_DETAILS_SEQID));
							errMsgObj.setBatchId(fileSummary.getBatchId());
							errMsgObj.setProductId(productId==null?"":productId);
							errMsgObj.setSourceId(fileSummary.getSourceId());
							errMsgObj.setAttributeName(entry.getKey());
							errMsgObj.setErrorMessage(entry.getValue());
							errMsgObj.setErrorType(Constants.VALIDATION_ERROR);
							errMsgObj.setAttributeValue(attributeValue);
							errMsgObj.setFileId(fileSummary.getId());
							errMsgObj.setCreatedTimeStamp(new Timestamp(new Date().getTime()));
							errMsgObj.setCreatedUserId(userSess.getId());
							exceptionList.add(errMsgObj);		
						}
						
						exceptionReportDAO.insertTrackErrorMessage(exceptionList);
						}*/
					}
					
					skipValue = skipValue + limitConstant;
				}
				
				if(skuInfoList!=null && skuInfoList.size()>0) {
					if(null != upcList && !upcList.isEmpty()){
						importDAO.deleteSameItemandUPExist(fileSummary.getSourceId(), upcList, upcItemKey);
						upcList.clear();
					}
					insertSKUInfoAsBatch(skuInfoList);
					skuInfoList = null;
					skuInfoId = 0;
	    		}
				
			}
		} catch(Exception exception){
			exception.printStackTrace();
		} /*finally{
			logger.debug("updateMergeandCurateStatus : "+ fileSummary.getId());
			importDAO.updateMergeandCurateStatus(fileSummary.getId());
			logger.debug("updateMergeandCurateStatus executed successfully");
		}*/
		
		Date importedOn = new Timestamp(new Date().getTime());
		fileSummary.setTotalNoOfRecords(totalNoOfRecords);
		updateFileSummary(fileSummary);
		
		logger.debug("Import - Total number of records moved to product table : "+totalNoOfRecords);
		
		trackFileStatusModel.setStatusId(Constants.TRACKFILESTATUS_MOVEDTOPROD);           
		trackFileStatusModel.setTimestamp(importedOn);
		trackFileStatusModel.setId(getTrackFileStatusId());
		insertTrackFileStatus(trackFileStatusModel);
		
		//importFeedback = fileSummary.getFileName()+" is imported successfully";
		importFeedback = Constants.IMPORT_PROD_SUCCESS;
		return importFeedback;
	}
	
	private void getTransformationRules(Map<Long, Properties> transformMap, Map<Long, Boolean> replaceValue) throws Exception{
		List<TransformationRule> transformationRules = rulesBO.getTransformProperties();
		
		if(transformationRules.size() > 0){
			for(TransformationRule rule : transformationRules){
				replaceValue.put(rule.getAttrId(), rule.isReplaceValue());
				Properties properties = new Properties();
				String[] files = rule.getFiles();
				for(String file : files){
					InputStream inputStream = new FileInputStream(getTransformationPropPath()+file);
					properties.load(inputStream);
				}
				transformMap.put(rule.getAttrId(), properties);
			}
			transformationRules = null;
		}	
	}

	public String getTransformationPropPath() {
		return transformationPropPath;
	}

	public void setTransformationPropPath(String transformationPropPath) {
		this.transformationPropPath = transformationPropPath;
	}
	
	/* (non-Javadoc)
	 * @see com.skuview.importsku.ImportSKU#getAllTemplateIdAndName()
	 */
	@Override
	public Map<Long, String> getAllTemplateIdAndName() throws Exception {
		Map<Long, String> templateMap = new LinkedHashMap<Long, String>();
		templateMap = templateDAO.getAllTemplateNames();
		return templateMap;
	}
	
	/* (non-Javadoc)
	 * @see com.skuview.importsku.ImportSKU#insertFileSummary(com.skuview.importsku.model.FileSummaryModel)
	 */
	@Override
	public void insertFileSummary(FileSummaryModel fileSummaryModel) throws Exception {
		stageImportDAO.insertFileSummary(fileSummaryModel);
	}
	
	/* (non-Javadoc)
	 * @see com.skuview.importsku.ImportSKU#updateFileSummary(com.skuview.importsku.model.FileSummaryModel)
	 */
	@Override
	public void updateFileSummary(FileSummaryModel fileSummaryModel) throws Exception {
		stageImportDAO.insertFileSummary(fileSummaryModel);
	}
	
	/* (non-Javadoc)
	 * @see com.skuview.importsku.ImportSKU#insertTrackFileStatus(com.skuview.importsku.model.TrackFileStatus)
	 */
	@Override
	public void insertTrackFileStatus(TrackFileStatus trackFileStatus) throws Exception {
		stageImportDAO.insertTrackFileStatus(trackFileStatus);
	}
	
	/* (non-Javadoc)
	 * @see com.skuview.importsku.ImportSKU#updateTrackFileStatus(com.skuview.importsku.model.TrackFileStatus)
	 */
	@Override
	public void updateTrackFileStatus(TrackFileStatus trackFileStatus) throws Exception {
		stageImportDAO.insertTrackFileStatus(trackFileStatus);
	}
	
	/* (non-Javadoc)
	 * @see com.skuview.importsku.ImportSKU#insertStageSKUInfo(com.skuview.importsku.model.StagingSKUInfo)
	 */
	@Override
	public void insertStageSKUInfo(StagingSKUInfo stageSkuInfo) throws Exception {
		stageImportDAO.insertSKU(stageSkuInfo);
	}
	
	/* (non-Javadoc)
	 * @see com.skuview.importsku.ImportSKU#insertStageSKUInfo(com.skuview.importsku.model.StagingSKUInfo)
	 */
	 
	@Override
	public void insertStageSKUInfoAsBatch(List<StagingSKUInfo> stageSkuInfoList) throws Exception {
		stageImportDAO.insertSKUs(stageSkuInfoList);
	}
	
	/* (non-Javadoc)
	 * @see com.skuview.importsku.ImportSKU#insertSKUInfo(com.skuview.importsku.model.SKUInfo)
	 */
	@Override
	public void insertSKUInfo(SKUInfo skuInfo) throws Exception {
		importDAO.insertSKU(skuInfo);
	}
	
	/* (non-Javadoc)
	 * @see com.skuview.importsku.ImportSKU#insertSKUInfoAsBatch(com.skuview.importsku.model.SKUInfo)
	 */
	@Override
	public void insertSKUInfoAsBatch(List<SKUInfo> skuInfoList) throws Exception {
		importDAO.insertSKUs(skuInfoList);
	}
	
	/**
	 * @param skuHistory
	 * @throws Exception
	 */
	private void insertSKUHistory(SKUHistory skuHistory) throws Exception {
		importDAO.insertSKUHistory(skuHistory);		
	}
	

	/**
	 * @param status,stagingId
	 * @throws Exception
	 */
	private void updateStagingStatus(int status,long stagingId,Map<String,String> attributeErrors) throws Exception {
		importDAO.updateStagingStatus(status,stagingId,attributeErrors);
	}
	
	/* (non-Javadoc)
	 * @see com.skuview.importsku.ImportSKU#getFileSummaryId()
	 */
	@Override
	public long getFileSummaryId() throws Exception {
		return sequenceGenDAO.getNextSequenceId(Constants.FILE_SUMMARY_SEQID);
	}
	
	/* (non-Javadoc)
	 * @see com.skuview.importsku.ImportSKU#getStageBatchId()
	 */
	@Override
	public long getStageBatchId() throws Exception {
		return sequenceGenDAO.getNextSequenceId(Constants.STAGE_BATCH_SEQID);
	}
	
	/* (non-Javadoc)
	 * @see com.skuview.importsku.ImportSKU#getSkuInfoId()
	 */
	@Override
	public long getSkuInfoId() throws Exception {
		return sequenceGenDAO.getNextSequenceId(Constants.SKU_INFO_SEQID);
	}
	
	/* (non-Javadoc)
	 * @see com.skuview.importsku.ImportSKU#getTrackFileStatusId()
	 */
	@Override
	public long getTrackFileStatusId() throws Exception {
		return sequenceGenDAO.getNextSequenceId(Constants.TRACK_FILE_STATUS_SEQID);
	}
	
	/* (non-Javadoc)
	 * @see com.skuview.importsku.ImportSKU#getSkuHistoryId()
	 */
	@Override
	public long getSkuHistoryId() throws Exception {
		return sequenceGenDAO.getNextSequenceId(Constants.SKU_HISTORY_SEQID);
	}
	
	/* (non-Javadoc)
	 * @see com.skuview.importsku.ImportSKU#getSkuHistoryId()
	 */
	@Override
	public long getFileLocationPathId() throws Exception {
		return sequenceGenDAO.getNextSequenceId(Constants.FILE_LOCATION_INFO_SEQID);
	}
	
	/* (non-Javadoc)
	 * @see com.skuview.importsku.ImportSKU#getAllRecordsToExport(java.lang.Integer, java.util.List, java.util.List, java.util.List)
	 */
	/*@Override
	public List<SKUInfo> getAllRecordsToExport(Integer productStatus,List<String> searchBy, List<String> filterByNum, List<String> filterByNon, List<String> keyword, List<String> dataTypeVal) throws Exception {
		return importDAO.getAllRecordsToExport(productStatus, searchBy, filterByNum, filterByNon, keyword, dataTypeVal);
	}*/
	
	@Override
	public List<StatusReference> loadFileStatus(long fileId) throws Exception {
		long currentStatus=0;
		
		
		List<TrackFileStatus> trackList=productImportReportDAO.getFileStatus(fileId);
		
		for(TrackFileStatus fileStatus:trackList){
			
			
			if(currentStatus<fileStatus.getStatusId()){
				currentStatus=fileStatus.getStatusId();
			}
		}
		 
		List<StatusReference> statusList=productImportReportDAO.getStatusDesc(currentStatus);
		
		
		
		return statusList;
		
	}
	
	/* (non-Javadoc)
	 * @see com.skuview.importsku.ImportSKU#getTemplateInfo(long)
	 */
	@Override
	public TemplateInfo getTemplateInfo(long templateId) throws Exception {
		TemplateModel tempModel = templateDAO.getTemplate(templateId);
		ProcessingParamSourceModel srcModel = processingParameterDAO.getProcessParamSource(tempModel.getSourceId());
		TemplateInfo templateInfo = new TemplateInfo();
		templateInfo.setSourceName(srcModel.getSourceName());
		templateInfo.setFileExtension(tempModel.getFileType());
		return templateInfo;
	}

	/* (non-Javadoc)
	 * @see com.skuview.importsku.ImportSKU#getTemplateId(java.lang.String)
	 */
	@Override
	public long getTemplateId(String sourceName) throws Exception{
		long sourceId = importDAO.getSourceId(sourceName);
		long templateId = 0;
		if(sourceId != 0)
			templateId = importDAO.getTemplateId(sourceId);
		return templateId;
	}
	
	/* (non-Javadoc)
	 * @see com.skuview.importsku.ImportSKU#createFileLocationInfo(com.skuview.importsku.model.FileLocationInfo)
	 */
	@Override
	public void createFileLocationInfo(FileLocationInfo fileLocationInfoObject) throws Exception {
		fileLocationInfoObject.setId(getFileLocationPathId());
		importDAO.insertFileLocationInfo(fileLocationInfoObject);
	}

}