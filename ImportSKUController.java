package com.skuview.common.controller;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.support.ByteArrayMultipartFileEditor;

import com.skuview.category.dao.constants.Constants;
import com.skuview.common.command.DraftVersion;
import com.skuview.common.dao.ProductKeyUtilDAO;
import com.skuview.common.model.AttributeType;
import com.skuview.common.model.ImportSKUModel;
import com.skuview.common.model.ProdAttributeModel;
import com.skuview.common.model.Settings;
import com.skuview.common.model.TemplateInfo;
import com.skuview.common.util.Importpath;
import com.skuview.common.util.ReaderFactory;
import com.skuview.controller.constants.ControllerConstants;
import com.skuview.exception.CustomGenericException;
import com.skuview.importsku.ImportSKU;
import com.skuview.importsku.model.FileLocationInfo;
import com.skuview.importsku.model.FileSummaryModel;
import com.skuview.importsku.model.ImportStageParams;
import com.skuview.importsku.model.StatusReference;
import com.skuview.importsku.model.TrackFileStatus;
import com.skuview.jobs.bo.JobsBO;
import com.skuview.manualcuration.bo.ManualCurationBO;
import com.skuview.productattribute.ProdAttrBO;
import com.skuview.sequence.dao.SequenceGeneratorDAO;
import com.skuview.service.bo.RoleService;
import com.skuview.template.bo.TemplateBO;
import com.skuview.uniqueidentifier.bo.UniqueIdentifierBO;
import com.skuview.user.model.Roles;
import com.skuview.user.model.UserDetails;

/**
 * This controller class is used to import the SKU's into the system.
 * 
 * @author 372153
 *
 */
/**
 * @author 458912
 *
 */
@Controller
public class ImportSKUController {

	private static final Logger logger = Logger.getLogger(ImportSKUController.class);
	
	
	Map<Long, String> templateMap = null;
	String importFeedback = "";
	long fileId=0;
	String fileName="";
	
	@Autowired
	public ReaderFactory readerFactory;
	
	@Autowired
	public ImportSKU importSKU;
	
	@Autowired
	private FileSummaryModel fileSummary;
	
	@Autowired
	private TrackFileStatus trackFileStatus;
	
	@Autowired
	private ImportStageParams importStageParams;
	
	@Autowired
	public RoleService roleBO;
	
	@Autowired
	public FileLocationInfo fileLocationInfo;

	@Autowired
	public JobsBO jobsBO;
	
	@Autowired
    @Qualifier("importSKUValidator")
    private Validator importSKUValidator;
	
	@Autowired
	private  Importpath importPath;
	
	@Autowired
	private CategoryController categoryController;
	
	@Autowired
	private ManualCurationBO manualCurationBO;
	
	@Autowired
	private ProductKeyUtilDAO productKeyUtilDAO;
	
	@Autowired
	private TemplateBO templateBO;
	
	@Autowired
	private SequenceGeneratorDAO sequenceGenDAO;
	
	@Autowired
	private UniqueIdentifierBO uniqueIdentifierBO;
	
	@Autowired
	public ProdAttrBO prodAttrBO;
	
 
    @InitBinder("importSKUModel")
    private void initBinder(WebDataBinder binder) {
        binder.setValidator(importSKUValidator);
    }
    
    @InitBinder
    protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder){
    	 binder.registerCustomEditor(byte[].class,new ByteArrayMultipartFileEditor());
   }
    
    Thread thread;
    
    private Map<String, String> infoMap = null;
	
	private Map<String, LinkedHashMap<String, AttributeType>> curateMap = null;

	private boolean isCurateFailed = false;
	
	private boolean isSkuValidated = false;
	
	private boolean isSkuSaved = false;
	
	private boolean readOnly = false;
	
    //private static String LOCAL_IMPORT_PATH = "C:/Program Files/Skuview_ImportedFiles/";
	
	/**
	 * This method is used to initialize the form object and load all the dropdown values in the form
	 * 
	 * @param model		Model object to populate the values in jsp
	 * @param request	Request object
	 * @return			Returns the control to the Import jsp
	 */
	@RequestMapping(value = "/importSKU", method = RequestMethod.GET)
	public String productSearch(Map<String, Object> model, HttpServletRequest request) {
		try {
			UserDetails userSess =(UserDetails) request.getSession().getAttribute("LoggedUser");
			setAccessMap(model, userSess.getRole());
			initializeFormObject(model);
		} catch(Exception exception) {
			 throw new CustomGenericException(exception.getMessage());
		}
		return Constants.IMPORTSKU;
	}
	
	/**
	 * This method is used to import the SKU's from the input file
	 * 
	 * @param importModel		Model object holds the inputs from jsp file
	 * @param bindingResult		Binds the validation results
	 * @param model				Model object to populate the values to the jsp
	 * @param request			Request object
	 * @return					Returns the control to Import jsp
	 */
	@RequestMapping(value = "/importSKUSubmit", method = RequestMethod.POST)
    public String importSKUHandler(@ModelAttribute("importSKUModel")  @Validated final ImportSKUModel importModel, 
    		BindingResult bindingResult, Map<String, Object> model, HttpServletRequest request) {
		
		try {
		
			final UserDetails userSess =(UserDetails) request.getSession().getAttribute("LoggedUser");
			setAccessMap(model, userSess.getRole());
			retrieveFormObject(model, importModel);
			
			if (bindingResult.hasErrors()) {
	            logger.info("Returning ImportSKU.jsp page - validation failure");
	            return Constants.IMPORTSKU;
	        }
		
			
			logger.debug("temp id : "+importModel.getTemplateId());
			thread = new Thread(new Runnable() {
				public void run(){
					try{
						Date requestedDate = new Timestamp(new Date().getTime());
						logger.info("Initiating file transfer from UI to server 'Import' folder" );
						String fileName = FilenameUtils.getBaseName(importModel.getSkuFile().getOriginalFilename());
						String fileExt = FilenameUtils.getExtension(importModel.getSkuFile().getOriginalFilename());
						StringBuffer newFileName = new StringBuffer();
						newFileName.append(fileName);
						newFileName.append('_');
						newFileName.append(new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date()));
						newFileName.append(".");
						newFileName.append(fileExt);
						byte[] bytes = importModel.getSkuFile().getBytes();
		                BufferedOutputStream stream =
		                        new BufferedOutputStream(new FileOutputStream(new File(importPath.getUiImportPath()+newFileName)));
		                stream.write(bytes);
		                stream.close();
		             
						logger.debug("File "+newFileName+" is Successfully transferred to server" );
						Integer tempId = new Integer(importModel.getTemplateId());
						
						fileSummary = importSKU.trackFileSummary(newFileName.toString(), tempId.longValue());
						if(fileSummary != null) {
							trackFileStatus = importSKU.trackFileStutusModel(fileSummary.getId(), userSess.getId());
							if(trackFileStatus != null) {
								
								fileLocationInfo.setFilePath(importPath.getUiImportPath());
								fileLocationInfo.setFileName(newFileName.toString());
								fileLocationInfo.setStatus(Constants.FILE_LOC_NEW);
								fileLocationInfo.setTemplateId(tempId.longValue());
								fileLocationInfo.setUserId(userSess.getId());
								fileLocationInfo.setRequestedDate(requestedDate);
								fileLocationInfo.setFileSummaryId(fileSummary.getId());
								fileLocationInfo.setTrackFileStatusId(trackFileStatus.getId());
								fileLocationInfo.setImportType(Constants.IMPORT_ONLINE);
								
								importSKU.createFileLocationInfo(fileLocationInfo);
							}
						}
					}catch(Exception exception) {
						exception.printStackTrace();
						 throw new CustomGenericException(exception.getMessage());
					}
				}
			});
			
			thread.start();
			
		} catch(Exception exception) {
			 throw new CustomGenericException(exception.getMessage());
		}
			
		logger.debug(importFeedback);
		model.put("fileId", fileId);
		model.put("importFeedback", "success");
        return Constants.IMPORTSKU;
    }
	
	/**
	 * This method is used to import the input file - Backup for online import
	 * 
	 * @param importModel
	 * @param bindingResult
	 * @param model
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/importSKUSubmit_bk", method = RequestMethod.POST)
    public String importSKUHandler_bk(@ModelAttribute("importSKUModel")  @Validated final ImportSKUModel importModel, 
    		BindingResult bindingResult, Map<String, Object> model, HttpServletRequest request) {
		
		try {
		
			final UserDetails userSess =(UserDetails) request.getSession().getAttribute("LoggedUser");
			setAccessMap(model, userSess.getRole());
			retrieveFormObject(model, importModel);
			
			if (bindingResult.hasErrors()) {
	            logger.info("Returning ImportSKU.jsp page - validation failure");
	            return Constants.IMPORTSKU;
	        }
		
			
			logger.debug("temp id : "+importModel.getTemplateId());
			thread = new Thread(new Runnable() {
				public void run(){
					try{
						//importSKU = new ImportSKUImpl();
						fileSummary = new FileSummaryModel();
						long startTime = System.currentTimeMillis()/1000;
						logger.debug("Calling transfer Method" );
						//importModel.getSkuFile().transferTo(new File(Constants.LOCAL_IMPORT_PATH+importModel.getSkuFile().getOriginalFilename()));
						String fileName = FilenameUtils.getBaseName(importModel.getSkuFile().getOriginalFilename());
						String fileExt = FilenameUtils.getExtension(importModel.getSkuFile().getOriginalFilename());
						StringBuffer newFileName = new StringBuffer();
     					//newFileName.append(Constants.LOCAL_IMPORT_PATH);
						newFileName.append(fileName);
						newFileName.append('_');
						newFileName.append(System.currentTimeMillis());
						newFileName.append(".");
						newFileName.append(fileExt);
						//final String newFileName = importModel.getSkuFile().getName()+startTime+".csv";
						byte[] bytes = importModel.getSkuFile().getBytes();
						System.out.println("Imported Path==" + importPath.getImportPath());
		                BufferedOutputStream stream =
		                        new BufferedOutputStream(new FileOutputStream(new File(importPath.getImportPath()+newFileName)));
		                stream.write(bytes);
		                stream.close();
		             
						logger.debug("Successfully copied" );

						long endTime = System.currentTimeMillis();
						logger.debug("Time taken for upload the file " +((endTime-startTime)/1000) + " Sec(s).");
						fileSummary = importSKU.importDataToStaging(newFileName.toString(), importModel, readerFactory,userSess.getId());
						startTime = System.currentTimeMillis();
						logger.debug("Time taken for import to Staging " +((startTime-endTime)/1000) + " Sec(s).");
						fileId=fileSummary.getId();
						fileName=fileSummary.getFileName();
						//importFeedback = importSKU.importDataToProd(fileSummary,userSess);
						endTime = System.currentTimeMillis();
						logger.debug("Time taken for import to production Table  " +((endTime-startTime)/1000) + " Sec(s).");
			        	
					}catch(Exception exception) {
						exception.printStackTrace();
						 throw new CustomGenericException(exception.getMessage());
					}
				}
			});
			
			thread.start();
			
		} catch(Exception exception) {
			 throw new CustomGenericException(exception.getMessage());
		}
			
		logger.debug(importFeedback);
		model.put("fileId", fileId);
		model.put("importFeedback", "success");
        return Constants.IMPORTSKU;
    }
	
	/**
	 * Method to initialize the form object 
	 * 
	 * @param model			Model holds the values to be populated in screen
	 * @throws Exception	Throws the generic exception
	 */
	public void initializeFormObject(Map<String, Object> model) throws Exception {
				
		ImportSKUModel importSKUModel = new ImportSKUModel();
		model.put("importSKUModel", importSKUModel);
		
		templateMap = getAllTemplatesMap();
		model.put("templateMap", templateMap);		
	}
	
	/**
	 * Method to retrieve the form object
	 * 
	 * @param model			Model holds the values to be populated in screen
	 * @param importModel	Model holds the values selected
	 * @throws Exception	Throws the generic exception
	 */
	public void retrieveFormObject(Map<String, Object> model, ImportSKUModel importModel) throws Exception {
		fileId=0;
		fileName="";
		ImportSKUModel importSKUModel = importModel;
		model.put("importSKUModel", importSKUModel);
		
		templateMap = getAllTemplatesMap();
		model.put("templateMap", templateMap);		
	}
	
	/**
	 * Get all the templates object
	 * 
	 * @return Map<Long, String>	Returns the template id and name
	 * @throws Exception			Throws generic exception
	 */
	private Map<Long, String> getAllTemplatesMap() throws Exception  {
		templateMap = new LinkedHashMap<Long, String>();
		templateMap.put(new Long(0), "-- Select --");
		templateMap.putAll(importSKU.getAllTemplateIdAndName());
		return templateMap;
	}
	/**
	 * Set the access to the functionalities in the screen
	 * 
	 * @param model
	 * @param roleId
	 * to get role based menu view
	 */
	private void setAccessMap(Map<String, Object> model, long roleId) {
		
		try{
		
			Roles role = roleBO.getRole(roleId);
			String[] accessLevel =  role.getAccessLevel();
			
			for(String access : accessLevel){
				String accessName = roleBO.getAccessName(Long.parseLong(access));
				model.put(accessName, accessName);
			}
		}catch(Exception e) {
			e.printStackTrace();
			
		}
	}
	/**
	 * Method to retrieve status of the file thru ajax call
	 * 
	 * @param 
	 * @return String (Status message)
	 * @throws Exception	Throws the generic exception
	 */
	
	@RequestMapping(value = "/loadFileStatus", method = RequestMethod.POST, produces = MediaType.ALL_VALUE)
	public @ResponseBody String loadFileStatus(
			 HttpServletResponse response) {
		
		List<StatusReference> statusList=new ArrayList<StatusReference>();
		
		String returnMsg="";
		
		/*System.out.println(fileId);*/

		
		try {
			if(fileId!=0)
				statusList=importSKU.loadFileStatus(fileId);
			
			
			if(statusList.size()>0 && statusList.get(0).getStatusId()==Constants.TRACKFILESTATUS_MOVEDTOPROD){
				returnMsg="Import file "+fileName+" is processed";
			}
			
		} catch(Exception exception) {
			 throw new CustomGenericException(exception.getMessage());
		}
		
		
		return returnMsg;
	}
	
	@RequestMapping(value = "/getSrcInfo", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody TemplateInfo getSrcInfo(@RequestBody Long tempId, HttpServletRequest request, HttpServletResponse response){
		TemplateInfo templateInfo = null;
		try{
			if(tempId!=null) {
				templateInfo = importSKU.getTemplateInfo(tempId);
			}
		}catch(Exception e){
			throw new CustomGenericException(e.getMessage());
		}
		return templateInfo;
	}
	
	
	/**
	 * This method is used to load the New product import screen
	 * 
	 * @param model			Model object to populate the values to the jsp
	 * @param request		Request object
	 * @return
	 */
	@RequestMapping(value = "/importProduct", method = RequestMethod.GET)
	public String manualCuartionView(Map<String, Object> model, HttpServletRequest request) {
		curateMap = new LinkedHashMap<String, LinkedHashMap<String,AttributeType>>();
		infoMap = new HashMap<String, String>();
		Map<Long, String> attrList = new HashMap<Long, String>();
		Map<Long,String> headerMap = new HashMap<Long, String>();
		Map<String,String> headMap = new HashMap<String, String>();
		ProdAttributeModel prodAttrModel = new ProdAttributeModel();
		String prod = "";
		try{
			UserDetails user =(UserDetails) request.getSession().getAttribute("LoggedUser");
			setAccessMap(model, user.getRole());
			readOnly = false;
			List<String> setMap = productKeyUtilDAO.getProductKeys();
			attrList = templateBO.getAllClientAttrIdMap();	
			prod = getProductConfigValue();
			prodAttrModel = prodAttrBO.findAttribute(Integer.parseInt(prod));
			prod = getAttrDispName(prodAttrModel,prod);
			curateMap = manualCurationBO.fetchImportSkuData(infoMap);
			isCurateFailed = Boolean.parseBoolean(infoMap.get("curateFlag"));
			infoMap.put("isCurateFailed", String.valueOf(isCurateFailed));
			loadHeaderMap(headerMap,attrList,setMap,headMap);
			model.put("productName", prod);
			model.put("headerMap", headerMap);
			model.put("headMap", headMap);
			model.put("readOnly",false);
			loadImportModelObject(model, curateMap,infoMap);
		} catch(Exception exception){
			logger.debug("Exception occured in manualCuartionView method");	
		}
		return Constants.NEW_PRODUCT;
	}
	

	@RequestMapping(value = "/submitImportedData", method = RequestMethod.POST,params="cancel")
	public String cancelImportedData(@ModelAttribute("submitCuration") DraftVersion drafAttributes,
			Map<String, Object> model, WebRequest webRequest, HttpServletRequest request) {
		return "redirect:/importProduct";
		
	}
	
	
	private void loadImportModelObject(Map<String, Object> model,
			Map<String, LinkedHashMap<String, AttributeType>> curateMap,Map<String, String> infoMap) {
		Map<String, String> urlMap = new HashMap<String, String>();
		try {
			setUrlMap(urlMap);
			model.put("selectedUrl", urlMap);
			model.put("skuData", curateMap);
			categoryController.setCategoryTreeData(model);
			model.put(ControllerConstants.VAL_UPC,
					infoMap.get(ControllerConstants.SETTINGS_UPC));
			model.put(ControllerConstants.VAL_ITEMNUMBER,
					infoMap.get(ControllerConstants.SETTINGS_ITEM_NUMBER));
			model.put(ControllerConstants.VAL_PRODNAME,
					infoMap.get(ControllerConstants.SETTINGS_PRODUCT_NAME));

			if (null != infoMap.get("skuSaved")
					&& infoMap.get("skuSaved").equalsIgnoreCase("true")) {
				model.put(ControllerConstants.SUCCESS_CURATION,
						ControllerConstants.CURATION_SUCCESS_MSG);
			} else if (isSkuValidated) {
				if (null != infoMap.get("curateFlag")
						&& infoMap.get("curateFlag").equalsIgnoreCase("true")) {
					model.put(ControllerConstants.FAILED_CURATION,
							ControllerConstants.CURATION_FAILED_MSG);
				} else if (null != infoMap.get("skuSaved")
						&& infoMap.get("skuSaved").equalsIgnoreCase("false")) {
					model.put(ControllerConstants.FAILED_CURATION,
							ControllerConstants.ERROR_MSG);
				} else {
					model.put(ControllerConstants.SUCCESS_CURATION,
							ControllerConstants.CURATION_VALIDATE_MSG);
				}
			} else if (null != infoMap.get("skuSaved")
					&& infoMap.get("skuSaved").equalsIgnoreCase("false")) {
				model.put(ControllerConstants.FAILED_CURATION,
						ControllerConstants.ERROR_MSG);
			}
			model.put("comments", infoMap.get("comments"));
		} catch (Exception exception) {
			throw new CustomGenericException(exception.getMessage());
		}
	}	
	
	/**
	 * This method is used to add new product
	 * 
	 * @param drafAttributes
	 * @param webRequest
	 * @param model				Model object to populate the values to the jsp
	 * @param request			Request object
	 * @return					Returns control to NewProduct jsp
	 */
	@RequestMapping(value = "/submitImportedData", method = RequestMethod.POST,params="save")
	public String submitImportedData(@ModelAttribute("submitCuration") DraftVersion drafAttributes,
			Map<String, Object> model, WebRequest webRequest, HttpServletRequest request) {
		curateMap = new LinkedHashMap<String, LinkedHashMap<String,AttributeType>>();
		infoMap = new HashMap<String, String>();	
		boolean flag=false;
		Map<Long, String> attrList = new HashMap<Long, String>();
		Map<String,String> headerMap = new LinkedHashMap<String, String>();
		Map<String,String> headMap = new LinkedHashMap<String, String>();
		ProdAttributeModel prodAttrModel = new ProdAttributeModel();
		String prod = "";
		String strConcat = "";
		String itemName="";
		try{
			UserDetails user =(UserDetails) request.getSession().getAttribute("LoggedUser");
			setAccessMap(model, user.getRole());
			List<String> setMap = productKeyUtilDAO.getProductKeys();
			prod = getProductConfigValue();
			prodAttrModel = prodAttrBO.findAttribute(Integer.parseInt(prod));
			prod = getAttrDispName(prodAttrModel,prod);
			attrList = templateBO.getAllAttrIdMap();
			Map webMap = webRequest.getParameterMap();
			Map<String, String> skuMap = new TreeMap<String, String>();
			Set<Map.Entry<String, String[]>> entrySet = webMap.entrySet();
			Iterator<Map.Entry<String, String[]>> mergeValue = entrySet.iterator();
			while (mergeValue.hasNext()) {
				Map.Entry<String, String[]> value = mergeValue.next(); 
				String returnValuevalue[] = value.getValue();
				if(!value.getKey().startsWith(Constants.PROD_IDENTIFIER))
					skuMap.put(value.getKey(), returnValuevalue[0]);
				else
					headMap.put(value.getKey(), returnValuevalue[0]);
			}
			
			for (Map.Entry<String, String> entry : skuMap.entrySet()) {
				if(entry.getKey().equals("AutogenerateKey"))
					flag=true;
			}
			
			if(flag==true)
			{
				for (Map.Entry<String, String> entry : headMap.entrySet()) 	
						skuMap.put(entry.getKey().substring(Constants.PROD_IDENTIFIER.length(),entry.getKey().length()), String.valueOf(sequenceGenDAO.getNextSequenceId(Constants.SEQ_PRODIDENTIFIER)));	
			}
			else
			{
					for (Map.Entry<String, String> entry : headMap.entrySet())
						//if(entry.getValue().length()==0||skuMap.get("Item Name").length()==0)
						//{
							//model.put(ControllerConstants.MANDATORY_FIELD,ControllerConstants.MANDATORY_FIELD_MSG);
							//skuMap.put(entry.getKey().substring(Constants.PROD_IDENTIFIER.length(),entry.getKey().length()),  entry.getValue());
						//}
						//else
							skuMap.put(entry.getKey().substring(Constants.PROD_IDENTIFIER.length(),entry.getKey().length()),  entry.getValue());
			}
		
			for (Map.Entry<String, String> entry : headMap.entrySet()) 
				headerMap.put(entry.getKey().substring(Constants.PROD_IDENTIFIER.length(),entry.getKey().length()), skuMap.get(entry.getKey().substring(3,entry.getKey().length())));
			for (Map.Entry<String, String> entry : headMap.entrySet()) 
				strConcat+=skuMap.get(entry.getKey().substring(Constants.PROD_IDENTIFIER.length(),entry.getKey().length()))+"_";
			
			String conCat = strConcat.substring(0, strConcat.length() - 1);
			for (Long id : attrList.keySet()) {
				if (id == Constants.COMPOSITE_KEY)
					skuMap.put(attrList.get(id), conCat);
			}

			infoMap.put("isSkuValidated", String.valueOf(isSkuValidated));
			curateMap = manualCurationBO.saveImportedData(skuMap, infoMap, isCurateFailed, readOnly);
			isSkuSaved = Boolean.parseBoolean(infoMap.get("skuSaved"));
			String err = errMsg(headerMap,skuMap,prod);
			itemName = skuMap.get(prod);
			
			model.put(ControllerConstants.MANDATORY_FIELD, err);
			model.put("headMap",headerMap);
			for(String s:infoMap.keySet()){
				if(s.equalsIgnoreCase("readonly") && infoMap.get(s).equalsIgnoreCase("true"))
					readOnly=true;
			}
			if(readOnly == true)
				model.put("readOnly",true);
			else
				model.put("readOnly",false);
			loadImportModelObject(model, curateMap,infoMap);
			model.put("productName", prod);
			model.put("productNameVal", itemName);
			isSkuValidated = false;
			readOnly = false;
			
		} catch(Exception exception){
			logger.debug("Exception occured in submitCuratedData method");	
		} 
		return Constants.NEW_PRODUCT;
	}
	
	/** This Method is used to loadHeaderMap
	 * @param headerMap
	 * @param attrList
	 * @param setMap
	 * @param headMap
	 */
	private void loadHeaderMap(Map<Long, String> headerMap,
			Map<Long, String> attrList, List<String> setMap, Map<String, String> headMap) {
		for(Long id : attrList.keySet()){
			for(String s : setMap){
				if(id == Long.parseLong(s))
				{
					headerMap.put(id, attrList.get(id));
				}
			}
		}
		for (Long id : headerMap.keySet()) {
			headMap.put(headerMap.get(id),"");
		}
		
	}


	private void setUrlMap(Map<String, String> urlMap){
		Map<String, String> prodURL =  new HashMap<String, String>();;
		try{
			
			String[] prodUrl = importPath.getProductUrl().split(";");
			for(String str : prodUrl){
				String tempStr = str.toString();
				String[] tempStrArray = tempStr.split("#&");
				if(null != tempStrArray){
					prodURL.put(tempStrArray[0], tempStrArray[1]);
					
				}
			}
			String domainName = importPath.getSelectedUrl();
			String[] name = domainName.split(Constants.COMMA);
			for(String domain : name){
				if(prodURL.containsKey(domain)){
					urlMap.put(domain, prodURL.get(domain));
				}
			}
			
		}catch(Exception exception) {
			throw new CustomGenericException(exception.getMessage());
		}
		
	}
	
	private String errMsg(Map<String, String> headerMap, Map<String, String> skuMap, String prod) {
		String msg="";
		for(Entry<String, String> entry : headerMap.entrySet() ){
			if(entry.getValue().isEmpty()){
				msg = ControllerConstants.MANDATORY_MSG;
			}
		}
		
		if(skuMap.get(prod).isEmpty()&& skuMap.get(prod).length()==0){
			msg = ControllerConstants.MANDATORY_MSG;		
		}
		
		return msg;
		
	}
	private String getProductConfigValue() throws Exception {
		Map<String, Settings> settingsMap = new HashMap<String, Settings>();
		settingsMap = uniqueIdentifierBO.getSettingsMap();
		Settings set = null;
		String id = "";
		for(Entry<String, Settings> entry:settingsMap.entrySet()){
			if(entry.getKey().equals(Constants.PRODUCT_NAME)){
				set=entry.getValue();
			}
		}
		id=set.getConfigValue();		
		return id;
	}
	
	private String getAttrDispName(ProdAttributeModel prodAttrModel, String prod) {
		prod="";
		prod = prodAttrModel.getDefaultDisplayName();
		return prod;		
	}
}
