/**
 * 
 */
package com.skuview.common.controller;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.skuview.common.command.ExportFile;
import com.skuview.common.util.DownloadFileUtil;
import com.skuview.common.util.Importpath;
import com.skuview.controller.constants.ControllerConstants;
import com.skuview.exception.AttributeIdViolation;
import com.skuview.exception.CustomGenericException;
import com.skuview.exception.UniqueLibraryAttributeViolation;
import com.skuview.export.bo.ExportBO;
import com.skuview.fileimport.LibraryFileImport;
import com.skuview.libcategory.bo.LibPrdCategoryBO;
import com.skuview.libprodattr.LibProdAttrBO;
import com.skuview.service.bo.RoleService;
import com.skuview.user.model.Roles;
import com.skuview.user.model.UserDetails;

/**
 * @author 277232
 *
 */

@Controller
public class LibProdImportController {
	
	@Autowired
	public LibPrdCategoryBO libPrdCategoryBO;

	@Autowired
	public LibProdAttrBO libProdAttrBO; 
	
	@Autowired
	public RoleService roleBO;
	
	@Autowired
	public DownloadFileUtil downloadFileUtil;
	
	@Autowired
	public Importpath importPath;
	
	@Autowired
	ExportBO exportBO;
		
	String exportFilePath="";
	String fileName ="";
	
	@RequestMapping(value = "/libImport", method = RequestMethod.GET)
	public String createCategory(Map model,ExportFile exportFile,
			Map<String, Object> batchModel, HttpServletRequest request) {
		UserDetails userSess =(UserDetails) request.getSession().getAttribute("LoggedUser");
		setAccessMap(batchModel, userSess.getRole());
		model.put("exportFile", exportFile);
		List<String> batchNames = addLibrary();
		batchModel.put("batchNames", batchNames);
		//UserDetails user =(UserDetails) request.getSession().getAttribute("LoggedUser");
		return ControllerConstants.PAGE_LIBRARY_IMPORT;
	}
	
	@RequestMapping(value = "/libFileUpload", method = RequestMethod.POST)
	public ModelAndView imageUpload(@ModelAttribute("exportFile") ExportFile reportFile,
			@RequestParam("file") MultipartFile file,Map<String, Object> batchModel, HttpServletRequest request) {
		UserDetails user =(UserDetails) request.getSession().getAttribute("LoggedUser");
		setAccessMap(batchModel, user.getRole());
		boolean success = false;
		List<String> batchNames = addLibrary();
		batchModel.put("batchNames", batchNames);
		ModelAndView modelandview = new ModelAndView(ControllerConstants.PAGE_LIBRARY_IMPORT);
		String[] fileArray = file.getOriginalFilename().split("\\.");

		if (!file.isEmpty() && !ControllerConstants.SELECT.equalsIgnoreCase(reportFile.getBatchNames())
				&& null != fileArray[1] && fileArray[1].equalsIgnoreCase(ControllerConstants.FORMAT_XLSX)) {
			try{
				exportFilePath = exportBO.getExportFilePath("ExportFilePath");
				 
				fileName = fileArray[0] + "_" +new SimpleDateFormat("yyyyMMddhhmmss'.'").format(new Date()) + fileArray[1];
				
				LibraryFileImport libImport = new LibraryFileImport();
				List<Map<String, String>> fileMap = libImport.parseFile(file);
				if(ControllerConstants.PROD_ATTRIBUTE.equalsIgnoreCase(reportFile.getBatchNames())){
					success = libProdAttrBO.insertSystemProdAttribute(fileMap, file, exportFilePath, fileName);
				} else if (ControllerConstants.PROD_CATEGORY.equalsIgnoreCase(reportFile.getBatchNames())){
					success = libPrdCategoryBO.insertSystemCategory(fileMap, file, exportFilePath, fileName);
				} else {					
				}
				
				if(!success){
					modelandview.addObject("imgMsg", ControllerConstants.FILE_UPLOAD_FAILED);
				} else{
					modelandview.addObject("imgMsg", "success");
				}
				
			} catch (UniqueLibraryAttributeViolation e) {
				e.printStackTrace();
				modelandview.addObject("imgMsg", "error");
				//modelandview.addObject("imgMsg", ControllerConstants.UNIQUE_LIBRARY_ATTR_FAILED);
				return modelandview;
			}  catch (AttributeIdViolation e) {
				e.printStackTrace();
				modelandview.addObject("imgMsg", ControllerConstants.EXPORT_TEMPLATE_ATTRIBUTE_FAILED);
				return modelandview;
			} catch (IOException e) {
				e.printStackTrace();
				modelandview.addObject("imgMsg", ControllerConstants.FILE_UPLOAD_FAILED);
				return modelandview;
			} catch (Exception e) {
				e.printStackTrace();
				modelandview.addObject("imgMsg", ControllerConstants.FILE_UPLOAD_FAILED);
				return modelandview;
			}
		} else if(ControllerConstants.SELECT.equalsIgnoreCase(reportFile.getBatchNames())){
			modelandview.addObject("imgMsg", ControllerConstants.LIBRARY_REQUIRED);
		} else if(file.isEmpty()){
			modelandview.addObject("imgMsg", ControllerConstants.FILE_REQUIRED);
		} else if(null != fileArray[1] && !ControllerConstants.FORMAT_XLSX.equalsIgnoreCase(fileArray[1])){
			modelandview.addObject("imgMsg", ControllerConstants.FILE_INVALID);
		} 
		return modelandview;
	}
	
	@RequestMapping(value="/libtempdownload", method=RequestMethod.GET)
	public void downloadImportTemplate(@RequestParam(value = "name", required = true) String tempName,
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) {
		try {
						
			UserDetails userSess = (UserDetails) request.getSession().getAttribute(
					"LoggedUser");
			setAccessMap(model, userSess.getRole());			
			
			if(tempName.equalsIgnoreCase(ControllerConstants.LIB_IMP_ATTR)){
				tempName = importPath.getLibAttrTemp();
			}else if(tempName.equalsIgnoreCase(ControllerConstants.LIB_IMP_CAT)) {
				tempName = importPath.getLibCatTemp();
			}
			
			if (tempName != null) {
				downloadFileUtil
						.downloadFile(
								request,
								response,
								tempName);
			}
		} catch(Exception exception) {
			 throw new CustomGenericException(exception.getMessage());
		}
	}
	
	@RequestMapping(value="/libraryExport", method=RequestMethod.GET)
	public void downloadlibraryExport(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) {
		
		UserDetails userSess = (UserDetails) request.getSession().getAttribute(
				"LoggedUser");
		setAccessMap(model, userSess.getRole());
		
		if (null!=fileName && !fileName.isEmpty() && null!=exportFilePath && !exportFilePath.isEmpty()) {
			try {
				downloadFileUtil
						.downloadFile(
								request,
								response,
								(exportFilePath + "/" + fileName));
			} catch (Exception exce) {
				throw new CustomGenericException(exce.getMessage());
			}
		}
	}
	
	
	private List<String> addLibrary(){
		
		List<String> libraryNames = new ArrayList<String>();
		libraryNames.add(ControllerConstants.SELECT);
		libraryNames.add(ControllerConstants.PROD_ATTRIBUTE);
		libraryNames.add(ControllerConstants.PROD_CATEGORY);
	
		return libraryNames;
	}
	/**
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

}
