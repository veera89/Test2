package com.skuview.fileimport;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.json.JSONArray;
import org.springframework.web.multipart.MultipartFile;

 
public class LibraryFileImport {
	
	private String colHeaders[] = {};
	private List<Map<String, String>> finalMap= new ArrayList<Map<String, String>>();

	
	public List<Map<String, String>> parseFile (MultipartFile fileName) throws FileNotFoundException {
		try {
			byte [] byteArr=fileName.getBytes();
			InputStream finput = new ByteArrayInputStream(byteArr);
			Workbook workbook = WorkbookFactory.create(finput);
			Sheet sheet = workbook.getSheetAt(0);
			for (Iterator<Row> sheetRows =  sheet.rowIterator(); sheetRows.hasNext();) {
				Row row =  sheetRows.next();
				JSONArray cellArray = new JSONArray();
				for( Iterator<Cell> rowCells = row.cellIterator(); rowCells.hasNext();) {
					Cell cell = rowCells.next();
					if (cell.getCellType() ==  3 || cell.getCellType() == 1) {
						cellArray.put(cell.getStringCellValue());
					} else if(cell.getCellType() == 0) {
						cellArray.put(cell.getNumericCellValue());
					} else if(cell.getCellType() ==  4) {
						cellArray.put(cell.getBooleanCellValue());							
					} else if(cell.getCellType() ==  5) {
						cellArray.put(cell.getErrorCellValue());
					} else if(cell.getCellType() == 2) {
						cellArray.put(cell.getCellFormula());
					}
				}
				if(colHeaders.length <= 0){
					headerAttribute(cellArray);
				} else{
					assembleFinalData(cellArray);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
	return finalMap;
	}
	
	public void headerAttribute(JSONArray cellArray){
		try{
			List<String> headerList = new ArrayList<String>();
			for(int i=0; i <= cellArray.length()-1; i++){
				headerList.add(cellArray.get(i).toString());
			}
			colHeaders = headerList.toArray(new String[headerList.size()]);
			System.out.println("colHeaders=="+ colHeaders.length);
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
  public void assembleFinalData(JSONArray cellArray){
	 Map <String, String> map = new HashMap<String, String>();
	 try{
			//List<String> headerList = new ArrayList<String>();
			for(int i=0; i <= cellArray.length()-1; i++){
				map.put(colHeaders[i], cellArray.get(i).toString());
			}
			finalMap.add(map);
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*public void loadProdAttribute(List<Map<String, String>> finalMap, List<ProductAttributeCO> prodAttrList){
		try{
			if(null != finalMap && !finalMap.isEmpty()){
				for(Map<String, String> attList : finalMap){
					ProductAttributeCO attModel = new ProductAttributeCO();
					for(Map.Entry<String, String> mapEntry : attList.entrySet()){
						if(!mapEntry.getKey().isEmpty() && mapEntry.getKey().equalsIgnoreCase("name")){
							attModel.setName(mapEntry.getValue());
						 } else if(!mapEntry.getKey().isEmpty() && mapEntry.getKey().equalsIgnoreCase("shortDescription")){
							 attModel.setShortDescription(mapEntry.getValue());
						 } else if(!mapEntry.getKey().isEmpty() && mapEntry.getKey().equalsIgnoreCase("longDescription")){
							 attModel.setLongDescription(mapEntry.getValue());
						 } else if(!mapEntry.getKey().isEmpty() && mapEntry.getKey().equalsIgnoreCase("dataType")){
							 attModel.setDataType(mapEntry.getValue());
						 } else if(!mapEntry.getKey().isEmpty() && mapEntry.getKey().equalsIgnoreCase("uom")){
							 attModel.setUom(mapEntry.getValue());
						 } else if(!mapEntry.getKey().isEmpty() && mapEntry.getKey().equalsIgnoreCase("defaultExportName")){
							 attModel.setDefaultExportName(mapEntry.getValue());
						 } else if(!mapEntry.getKey().isEmpty() && mapEntry.getKey().equalsIgnoreCase("defaultDisplayName")){
							 attModel.setDefaultDisplayName(mapEntry.getValue());
						 } else if(!mapEntry.getKey().isEmpty() && mapEntry.getKey().equalsIgnoreCase("displayFormat")){
							 attModel.setDisplayFormat(mapEntry.getValue());
						 } else if(!mapEntry.getKey().isEmpty() && mapEntry.getKey().equalsIgnoreCase("typeOfData")){
							 attModel.setTypeOfData(mapEntry.getValue());
						 } else if(!mapEntry.getKey().isEmpty() && mapEntry.getKey().equalsIgnoreCase("hasMulitpleValue")){
							 attModel.setHasMulitpleValue(mapEntry.getValue());
						 } else if(!mapEntry.getKey().isEmpty() && mapEntry.getKey().equalsIgnoreCase("functionalClass")){
							 attModel.setFunctionalClass(new String[]{mapEntry.getValue()});
						 } else if(!mapEntry.getKey().isEmpty() && mapEntry.getKey().equalsIgnoreCase("qualityClass")){
							 attModel.setQualityClass(new String[]{mapEntry.getValue()});
						 } else if(!mapEntry.getKey().isEmpty() && mapEntry.getKey().equalsIgnoreCase("sourceClass")){
							 attModel.setSourceClass(new String[]{mapEntry.getValue()});
						 } else if(!mapEntry.getKey().isEmpty() && mapEntry.getKey().equalsIgnoreCase("relatedCategory")){
							 attModel.setRelatedCategory(new String[]{mapEntry.getValue()});
						 } else if(!mapEntry.getKey().isEmpty() && mapEntry.getKey().equalsIgnoreCase("isSkuLibrary")){
							 attModel.setSkuLibrary(Boolean.parseBoolean(mapEntry.getValue()));
						 }
					}
					prodAttrList.add(attModel);
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void loadCategory(List<Map<String, String>> finalMap, List<CategoryCO> categoryList){
		try{
			if(null != finalMap && !finalMap.isEmpty()){
				for(Map<String, String> catList : finalMap){
					CategoryCO catModel = new CategoryCO();
					for(Map.Entry<String, String> mapEntry : catList.entrySet()){
						if(!mapEntry.getKey().isEmpty() && mapEntry.getKey().equalsIgnoreCase("name")){
							catModel.setName(mapEntry.getValue());
						 } else if(!mapEntry.getKey().isEmpty() && mapEntry.getKey().equalsIgnoreCase("parentCategory")){
							 catModel.setParentCategory(mapEntry.getValue());
						 } else if(!mapEntry.getKey().isEmpty() && mapEntry.getKey().equalsIgnoreCase("skuViewId")){
							 catModel.setSkuViewId(Long.parseLong(mapEntry.getValue()));
						 } else if(!mapEntry.getKey().isEmpty() && mapEntry.getKey().equalsIgnoreCase("hasValue")){
							 catModel.setHasValue(mapEntry.getValue());
						 } else if(!mapEntry.getKey().isEmpty() && mapEntry.getKey().equalsIgnoreCase("isRoot")){
							 catModel.setIsRoot(mapEntry.getValue());
						 } else if(!mapEntry.getKey().isEmpty() && mapEntry.getKey().equalsIgnoreCase("isMapped")){
							 catModel.setIsMapped(mapEntry.getValue());
						 } else if(!mapEntry.getKey().isEmpty() && mapEntry.getKey().equalsIgnoreCase("isActvie")){
							 catModel.setIsActive(mapEntry.getValue());
						 } else if(!mapEntry.getKey().isEmpty() && mapEntry.getKey().equalsIgnoreCase("isClient")){
							 catModel.setIsClient(mapEntry.getValue());
						 }
					}
					categoryList.add(catModel);
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
	}*/
}
