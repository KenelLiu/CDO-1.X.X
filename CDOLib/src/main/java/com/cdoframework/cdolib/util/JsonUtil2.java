package com.cdoframework.cdolib.util;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cdoframework.cdolib.base.DataType;
import com.cdoframework.cdolib.base.ObjectExt;
import com.cdoframework.cdolib.base.Utility;
import com.cdoframework.cdolib.data.cdo.CDO;
/**
 * 升级版用来替代JsonUtil
 * JSON 与CDO 互相转换工具类
 * @author KenelLiu 
 */
public class JsonUtil2 {
	public final static String Class_Byte="BYTE";
	public final static String Class_Short="SHORT";
	public final static String Class_Int="INT";
	public final static String Class_Integer="INTEGER";
	public final static String Class_Long="LONG";
	public final static String Class_Float="FLOAT";
	public final static String Class_Double="DOUBLE";
	
	public final static String Class_Boolean="BOOLEAN";
	public final static String Class_String="STRING";
	public final static String Class_Time="TIME";
	public final static String Class_Date="DATE";
	public final static String Class_DateTime="DATETIME";
	final static String Class_File="FILE";
	final static String Class_CDO="CDO";
	
	static class Empty{}
	/**
	 * 
	 * @param strJSON json字符串格式
	 * @return 将字符串json格式转换成CDO对象,会将strJSON中key对应value都转换成String
	 * @throws JSONException
	 */
	public static CDO json2CDO(String strJSON) throws JSONException{		
		return json2CDO(strJSON,Empty.class,Class_String);
	}
	/**
	 * 
	 * @param strJSON json字符串格式
	 * @param cls  定义 了strJSON中key的类型,数据转换时,会将key对应value转换成key的类型数据.
	 *   如 : strJSON={"key1":"value1","key2":20} 在cls中   定义了 String key1, int key2 变量,
	 *   则    转换时 将strJSON中key1对应value1转成CDO中string对象,key2对应value转成CDO中int对象
	 * @return CDO 对象
	 * @throws JSONException
	 */
	public static CDO json2CDO(String strJSON,Class<?> cls) throws JSONException{
		return json2CDO(strJSON,cls,null);
	}
	/**
	 * 
	 * @param strJSON json字符串格式
	 * @param cls  定义 了strJSON中key的类型,数据转换时,会将key对应value转换成key的类型数据.
	 *   如 : strJSON={"key1":"value1","key2":20} 在cls中   定义了 String key1, int key2 变量,
	 *   则    转换时 将strJSON中key1对应value1转成CDO中string对象,key2对应value转成CDO中int对象
	 * @param defaultType  当cls中未定义 strJSON中key的类型时,默认使用此类型．
	 * @return
	 * @throws JSONException
	 */
	public static CDO json2CDO(String strJSON,Class<?> cls,String defaultType) throws JSONException{
		if(strJSON==null)
			return null;
		if(!strJSON.startsWith("{") && !strJSON.startsWith("\r{") && !strJSON.startsWith("\r\n{")){
			throw new JSONException(strJSON+" is not jsonObject");
		} 
		JSONObject jsonObject = String2Json(strJSON); // 转换成JSONObject
		return JSONObject2CDO(jsonObject, cls, defaultType);
	}
	/**
	 * @param JSONObj JSON对象
	 * @return 将JSON对象转换成CDO对象,会将JSON对象中key对应value都转换成String
	 * @throws JSONException
	 */
	public static CDO JSONObject2CDO(JSONObject JSONObj)throws JSONException{
		return JSONObject2CDO(JSONObj,Empty.class,Class_String);
	}
	/**
	 * 
	 * @param JSONObj JSON对象
	 * @param cls  定义 了JSONObj中key的类型,数据转换时,会将key对应value转换成key的类型数据.
	 *   如 : JSONObj={"key1":"value1","key2":20} 在cls中   定义了 String key1, int key2 变量,
	 *   则    转换时 将JSONObj中key1对应value1转成CDO中string类型,key2对应value转成CDO中int类型
	 * @return
	 * @throws JSONException
	 */
	public static CDO JSONObject2CDO(JSONObject JSONObj,Class<?> cls) throws JSONException{
		return JSONObject2CDO(JSONObj,cls,null);
	}	
	/**
	 * @param JSONObj JSON对象
	 * @param cls  定义 了jsonObject中key的类型,数据转换时,会将key对应value转换成key的类型数据.
	 *   如 : JSONObj={"key1":"value1","key2":20} 在cls中   定义了 String key1, int key2 变量,
	 *   则    转换时 将JSONObj中key1对应value1转成CDO中string类型,key2对应value转成CDO中int类型
	 * @param defaultType  当cls中未定义 jsonObject中key的类型时,默认使用此类型．
	 * @return
	 * @throws JSONException
	 */
	public static CDO JSONObject2CDO(JSONObject JSONObj,Class<?> cls,String defaultType) throws JSONException{
		if(JSONObj==null)
			return null;
		if(cls==null)
			 throw new NullPointerException("JSONObject2CDO Dependent conversion type class is null");				
		
		CDO cdoRequest = new CDO();	
		Set<String> clsFieldsName=new HashSet<String>();
		try{			
			Field[] fields=cls.getDeclaredFields();
			for(int i=0;i<fields.length;i++){
				clsFieldsName.add(fields[i].getName());
			}	
			setCDOFromJson(JSONObj, cdoRequest,cls,clsFieldsName,defaultType);
		}finally{
			clsFieldsName.clear();
			clsFieldsName=null;
		}
		return cdoRequest;
	}
	/**
	 * 将CDO转换成JSONObject
	 * @param cdoData
	 * @return
	 * @throws JSONException 
	 */
	public static JSONObject CDO2JSONObject(CDO cdoData) throws JSONException{		
		JSONObject json=new JSONObject();
		if(cdoData==null)
			return json;
		for(Iterator<Map.Entry<String,ObjectExt>> it=cdoData.iterator();it.hasNext();){
			String field=it.next().getKey();
			CDO2JSON(field, json, cdoData);
	    }
		return  json;
	}	
	/**
	 * 将CDO转换成JSONObject
	 * @param cdoData
	 * @param fields 转换数组指定的字段
	 * @return
	 * @throws JSONException 
	 */
	public static JSONObject CDO2JSONObject(CDO cdoData,String[] fields) throws JSONException{
		JSONObject json=new JSONObject();
		if(cdoData==null || fields==null)
			return json;
		for(int i=0;i<fields.length;i++){
			if(fields[i]==null) continue;
			if(cdoData.exists(fields[i])){
				CDO2JSON(fields[i], json, cdoData);
			}
		}
		return  json;
	}
	/**
	 * 
	 * @param strJSONArray JSONArray字符串格式
	 * @return 将字符串JSONArray格式转换成List<CDO>对象,会将strJSONArray中key对应value都转换成String
	 * @throws JSONException
	 */
	public static List<CDO> jsonArray2CDOArray(String strJSONArray) throws JSONException   {
		return jsonArray2CDOArray(strJSONArray, Empty.class, Class_String);
	}
	
	/**
	 * 
	 * @param strJSONArray JSONArray字符串格式
	 * @param cls  定义 了JSONArray中key的类型,数据转换时,会将key对应value转换成key的类型数据.
	 *   如 : strJSONArray=[{"key1":"value1","key2":20}] 在cls中   定义了 String key1, int key2 变量,
	 *   则    转换时 将strJSON中key1对应value1转成CDO中string类型,key2对应value转成CDO中int类型
	 * @return
	 * @throws JSONException 
	 */
	public static List<CDO> jsonArray2CDOArray(String strJSONArray,Class<?>  cls) throws JSONException {
		return jsonArray2CDOArray(strJSONArray,cls,null);
	}
	/**
	 * 
	 * @param strJSONArray JSONArray字符串格式
	 * @param cls 定义 了JSONArray中key的类型,数据转换时,会将key对应value转换成key的类型数据.
	 *   如 : strJSONArray=[{"key1":"value1","key2":20}] 在cls中   定义了 String key1, int key2 变量,
	 *   则    转换时 将strJSON中key1对应value1转成CDO中string对象,key2对应value转成CDO中int对象	   
	 * @param defaultType 当cls中未定义 strJSONArray中key的类型时,默认使用此类型．
	 * @return
	 * @throws JSONException 
	 * @throws Exception
	 */
	public static List<CDO> jsonArray2CDOArray(String strJSONArray,Class<?>  cls,String defaultType) throws JSONException {
		if(strJSONArray==null)
			return null;
		JSONArray JSONArr=new JSONArray(strJSONArray);
		return JSONArray2CDOArray(JSONArr, cls, defaultType);
	}
	
	/**
	 * 
	 * @param JSONArr 
	 * @return 将JSONArr对象转换成CDO[]对象,会将JSONArr中key对应value都转换成String
	 * @throws JSONException
	 */
	public static List<CDO> JSONArray2CDOArray(JSONArray JSONArr) throws JSONException   {
		return JSONArray2CDOArray(JSONArr, Empty.class, Class_String);
	}
	
	/**
	 * @param JSONArr
	 * @param cls  定义 了JSONArr中key的类型,数据转换时,会将key对应value转换成key的类型数据.
	 *   如 : JSONArr=[{"key1":"value1","key2":20}] 在cls中   定义了 String key1, int key2 变量,
	 *   则    转换时 将JSONArr中key1对应value1转成CDO中string类型,key2对应value转成CDO中int类型
	 * @return
	 * @throws JSONException 
	 */
	public static List<CDO> JSONArray2CDOArray(JSONArray JSONArr,Class<?>  cls) throws JSONException {
		return JSONArray2CDOArray(JSONArr,cls,null);
	}
	/**
	 * 
	 * @param JSONArr
	 * @param cls 定义 了JSONArray中key的类型,数据转换时,会将key对应value转换成key的类型数据.
	 *   如 : JSONArr=[{"key1":"value1","key2":20}] 在cls中   定义了 String key1, int key2 变量,
	 *   则    转换时 将strJSON中key1对应value1转成CDO中string对象,key2对应value转成CDO中int对象	   
	 * @param defaultType 当cls中未定义 strJSONArray中key的类型时,默认使用此类型．
	 * @return
	 * @throws JSONException 
	 * @throws Exception
	 */
	public static List<CDO> JSONArray2CDOArray(JSONArray JSONArr,Class<?>  cls,String defaultType) throws JSONException {
		if(JSONArr==null)
			return null;
		
		List<CDO> cdoList=new ArrayList<CDO>(JSONArr.length());
		//============保存目标类型的字段名称====//
		Set<String> clsFieldsName=new HashSet<String>();
		try{			
			Field[] fields=cls.getDeclaredFields();
			for(int i=0;i<fields.length;i++){
				clsFieldsName.add(fields[i].getName());
			}	
			for(int i=0;i<JSONArr.length();i++){			
				CDO cdoData=new CDO();
				setCDOFromJson(JSONArr.getJSONObject(i),cdoData,cls,clsFieldsName,defaultType);
				cdoList.add(cdoData);
			}						
		}finally{
			clsFieldsName.clear();
			clsFieldsName=null;
		}		
		return cdoList;
	}
		
	/**
	 * CDO数组转换成JSON 数组字符
	 * @param cdos
	 * @return
	 */
	public static String CDOArray2JSONArrayString(List<CDO> cdoList) {
		if (cdoList == null) {
			return null;
		}
		StringBuffer buf = new StringBuffer(100);
		buf.append("[");
		for (int i = 0; i <cdoList.size(); i++) {
			if (i > 0) {
				buf.append(",");
			}
			buf.append(cdoList.get(i).toJSON());
		}
		buf.append("]");
		return buf.toString();
	}	
	
	/**
	 * CDO数组转换成JSONArray数组
	 * @param dataList
	 * @return
	 * @throws JSONException 
	 */
	public static JSONArray CDOArray2JSONArray(List<CDO> cdoList) throws JSONException{		
		if(cdoList==null)
			return null;
		JSONArray arr=new JSONArray();
		for(int i=0;i<cdoList.size();i++){
			CDO cdoData=cdoList.get(i);	
			JSONObject json=new JSONObject();
			for(Iterator<Map.Entry<String,ObjectExt>> it=cdoData.iterator();it.hasNext();){
					String field=it.next().getKey();
					CDO2JSON(field, json, cdoData);
			 }
			 arr.put(json);
		}
		return  arr;
	}	
	/**
	 * 把CDO数组转换成JSONArray数组
	 * @param fields 转换数组指定的字段
	 * @param dataList
	 * @return
	 * @throws JSONException 
	 */
	public static JSONArray CDOArray2JSONArray(List<CDO> cdoList,String[] fields) throws JSONException{		
		if(cdoList==null)
			return null;	
		JSONArray arr=new JSONArray();
		
		for(int i=0;i<cdoList.size();i++){
			CDO cdoData=cdoList.get(i);				
			JSONObject json=new JSONObject();
			for(int k=0;k<fields.length;k++){
				String field=fields[k];
				if(field==null)
					continue;
				if(cdoData.exists(field)){
					CDO2JSON(field, json, cdoData);
				}
			}			
			arr.put(json);
		}
		return  arr;
	}
	

	
	static void CDO2JSON(String field,JSONObject json,CDO data) throws JSONException{
		int type=data.getField(field).getType();
		switch(type){
			case DataType.BOOLEAN_TYPE: 
				json.put(field, data.getBooleanValue(field));
				break;
			case DataType.BYTE_TYPE:	
			case DataType.SHORT_TYPE:
			case DataType.INTEGER_TYPE:
			case DataType.LONG_TYPE:	
				json.put(field, data.getLongValue(field));
				break;
			case DataType.FLOAT_TYPE: 
				json.put(field, data.getFloatValue(field));
				break;		
			case DataType.DOUBLE_TYPE:
				json.put(field, data.getDoubleValue(field));
				break;		
			case DataType.STRING_TYPE: 
				json.put(field, data.getStringValue(field));
				break;	
			case DataType.DATE_TYPE: 
				json.put(field, data.getDateValue(field));
				break;	
			case DataType.TIME_TYPE: 
				json.put(field, data.getTimeValue(field));
				break;	
			case DataType.DATETIME_TYPE: 
				json.put(field, data.getDateTimeValue(field));
				break;								
			case DataType.BOOLEAN_ARRAY_TYPE: 
				JSONArray arr=new JSONArray();
				boolean[] vals1=data.getBooleanArrayValue(field);
				for(int m=0;m<vals1.length;m++){
					arr.put(vals1[m]);
				}
				json.put(field,arr);
				break;
			case DataType.BYTE_ARRAY_TYPE:
				arr=new JSONArray();
				byte[] vals2=data.getByteArrayValue(field);
				for(int m=0;m<vals2.length;m++){
					arr.put(vals2[m]);
				}
				json.put(field,arr);							
				break;
			case DataType.SHORT_ARRAY_TYPE:
				arr=new JSONArray();
				short[] vals3=data.getShortArrayValue(field);
				for(int m=0;m<vals3.length;m++){
					arr.put(vals3[m]);
				}
				json.put(field,arr);							
				break;
			case DataType.INTEGER_ARRAY_TYPE:
				arr=new JSONArray();
				int[] vals4=data.getIntegerArrayValue(field);
				for(int m=0;m<vals4.length;m++){
					arr.put(vals4[m]);
				}
				json.put(field,arr);							
				break;							
			case DataType.LONG_ARRAY_TYPE:	
				arr=new JSONArray();
				long[] vals5=data.getLongArrayValue(field);
				for(int m=0;m<vals5.length;m++){
					arr.put(vals5[m]);
				}
				json.put(field,arr);
			case DataType.FLOAT_ARRAY_TYPE: 
				arr=new JSONArray();
				float[] vals6=data.getFloatArrayValue(field);
				for(int m=0;m<vals6.length;m++){
					arr.put(vals6[m]);
				}
				json.put(field,arr);
				break;
			case DataType.DOUBLE_ARRAY_TYPE: 
				arr=new JSONArray();
				double[] vals7=data.getDoubleArrayValue(field);
				for(int m=0;m<vals7.length;m++){
					arr.put(vals7[m]);
				}
				json.put(field,arr);
				break;							
			case DataType.STRING_ARRAY_TYPE: 
				arr=new JSONArray();
				String[] vals8=data.getStringArrayValue(field);
				for(int m=0;m<vals8.length;m++){
					arr.put(vals8[m]);
				}
				json.put(field,arr);
				break;	
			case DataType.DATE_ARRAY_TYPE: 
				arr=new JSONArray();
				String[] vals9=data.getDateArrayValue(field);
				for(int m=0;m<vals9.length;m++){
					arr.put(vals9[m]);
				}
				json.put(field,arr);
				break;	
			case DataType.TIME_ARRAY_TYPE: 
				arr=new JSONArray();
				String[] vals10=data.getTimeArrayValue(field);
				for(int m=0;m<vals10.length;m++){
					arr.put(vals10[m]);
				}
				json.put(field,arr);
				break;	
			case DataType.DATETIME_ARRAY_TYPE: 
				arr=new JSONArray();
				String[] vals11=data.getDateTimeArrayValue(field);
				for(int m=0;m<vals11.length;m++){
					arr.put(vals11[m]);
				}
				json.put(field,arr);
				break;	
			case DataType.CDO_TYPE:
				CDO tmpCDO=data.getCDOValue(field);
				JSONObject tmpJSON=new JSONObject();
				for(Iterator<Map.Entry<String,ObjectExt>> it=tmpCDO.iterator();it.hasNext();){
					String tmpField=it.next().getKey();
					CDO2JSON(tmpField, tmpJSON, tmpCDO);
				}
				json.put(field,tmpJSON);
				break;
			case DataType.CDO_ARRAY_TYPE:	
				CDO[] cdoList=data.getCDOArrayValue(field);
				JSONArray tmpArr=new JSONArray();
				for(int k=0;k<cdoList.length;k++){
					 tmpCDO=cdoList[k];
					 tmpJSON=new JSONObject();
					 for(Iterator<Map.Entry<String,ObjectExt>> it=tmpCDO.iterator();it.hasNext();){
							String tmpField=it.next().getKey();
							CDO2JSON(tmpField, tmpJSON, tmpCDO);
					 }
					 tmpArr.put(tmpJSON);					 
				}
				json.put(field,tmpArr);
				break;
			case DataType.FILE_TYPE:
				json.put(field, data.getFileValue(field));
				break;
		}
	 }
	/**
	 * 
	 * @param strJSON
	 * @return
	 * @throws JSONException
	 */
	private static JSONObject String2Json(String strJSON) throws JSONException {
		JSONObject jsonObj = null;
		try {
			jsonObj = new JSONObject(strJSON); // 偶数个的这里会转换成功
		} catch (org.json.JSONException jex) {
			String newStr=Function.FormatTextForJson(strJSON);
			jsonObj = new JSONObject(newStr);
		}
		return jsonObj;
	}
	/**
	 * 根据 Json数据格式，得到一个具有构造一个等级结构
	 * 
	 * @throws JSONException
	 */
	private static void setCDOFromJson(JSONObject JSONObj,CDO cdoRequest,Class<?>  cls,Set<String> clsFieldsName,String defaultType) throws JSONException {
		try {
			String key = "";
			Object obj = null;
			CDO subCDO=null;
			for (Iterator<?> it = JSONObj.keys(); it.hasNext();) {
				key = (String) it.next();
				obj = JSONObj.get(key);
				if (obj instanceof JSONObject) {
					subCDO = new CDO();
					cdoRequest.setCDOValue(key, subCDO);
					setCDOFromJson((JSONObject) obj, subCDO,cls,clsFieldsName,defaultType);
				} else if (obj instanceof JSONArray) {
					//数组 里的数据 1为普通数据类型,2 json对象类型,3 不支持数组嵌套数组 ,混合数据
					JSONArray jsonArr=(JSONArray) obj;
					if(jsonArr.length()==0){	
						setEmptyArray(cdoRequest,key,cls,clsFieldsName,defaultType);
						continue;
					}					
				   setCDOListFromJson((JSONArray)obj,cdoRequest,key,cls,clsFieldsName,defaultType);
				} else {
					//设置普通数据类型
					setCommonField(cdoRequest, key, obj,cls,clsFieldsName,defaultType);
				}
			}
		} catch (Exception e) {
			throw new JSONException(e);
		}
	}
	
	
	private static void setCDOListFromJson(JSONArray jsonObj,CDO cdoParent,String key,Class<?>  cls,Set<String> clsFieldsName,String defaultType) throws Exception {
		Object obj = null;
		List<String> commonList=null;
		List<CDO> cdoList=null;
		for (int i = 0; i < jsonObj.length(); i++) {
			obj = jsonObj.get(i);
			if (obj instanceof JSONObject) {
				if(cdoList==null){
					cdoList=new ArrayList<CDO>();
					//=======保存列表======//
					//cdoParent.setCDOListValue(key, cdoList);	
				}				
				CDO subCDO = new CDO();
				cdoList.add(subCDO);
				setCDOFromJson((JSONObject) obj, subCDO,cls,clsFieldsName,defaultType);
			} else if (obj instanceof JSONArray) {
				throw new JSONException("unsupport json Array nesting, [[]] is unsupported");				
			} else {
				if(commonList==null)
					commonList=new ArrayList<String>();
				commonList.add(obj==null?"":obj.toString());
			}
		}
		if(cdoList!=null){
			cdoParent.setCDOArrayValue(key, cdoList.toArray(new CDO[cdoList.size()]));	
		}
		//设置普通类型数组
		if(commonList!=null){
			setCommonArray(cdoParent, key,commonList,cls,clsFieldsName,defaultType);
		}

	}
	
	private static void setCommonField(CDO cdoRequest,String key,Object values,Class<?>  cls,Set<String> clsFieldsName,String defaultType) throws JSONException{
		try{
			String type=getClassType(cls, key,clsFieldsName, defaultType);												
				if(type.equals(Class_Byte)){
					 cdoRequest.setByteValue(key,Utility.parseByteValue(values));
				}else if(type.equals(Class_Short)){
					 cdoRequest.setShortValue(key, Utility.parseShortValue(values));
				}else if(type.equals(Class_Int)){
					  cdoRequest.setIntegerValue(key, Utility.parseIntegerValue(values));
				}else if(type.equals(Class_Integer)){				
					  cdoRequest.setIntegerValue(key, Utility.parseIntegerValue(values));
				}else if(type.equals(Class_Long)){				
					 cdoRequest.setLongValue(key, Utility.parseLongValue(values));
				}else if(type.equals(Class_Float)){				
					cdoRequest.setFloatValue(key,  Utility.parseFloatValue(values));
				}else if(type.equals(Class_Double)){				
					cdoRequest.setDoubleValue(key, Utility.parseDoubleValue(values));
				}else if(type.equals(Class_Boolean)){				
					cdoRequest.setBooleanValue(key,Utility.parseBooleanValue(values));
				}else if(type.equals(Class_String)){				
					cdoRequest.setStringValue(key,Utility.parseStingValue(values));
				}else if(type.equals(Class_Time)){				
					cdoRequest.setTimeValue(key, Utility.parseTimeValue(values));
				}else if(type.equals(Class_Date)){				
					cdoRequest.setDateValue(key, Utility.parseDateValue(values));
				}else if(type.equals(Class_DateTime)){				
					cdoRequest.setDateTimeValue(key,Utility.parseDateTimeValue(values));
				}else if(type.equals(Class_File)){				
					cdoRequest.setFileValue(key, new File(Utility.parseStingValue(values)));
				}else {				
					throw new JSONException("unsupported json to cdo type,Json Key=["+key+"] Json value["+values+"] cast to "+type);
				}
		}catch(Exception ex){
			throw new JSONException(ex);
		}

	}
	private static void setEmptyArray(CDO cdoRequest,String key,Class<?>  cls,Set<String> clsFieldsName,String defaultType) throws JSONException{
		try{
			String type=getClassType(cls, key,clsFieldsName,defaultType);					
				if(type.equals(Class_Byte)){				
					 cdoRequest.setByteArrayValue(key, new byte[0]);
				}else if(type.equals(Class_Short)){				
					 cdoRequest.setShortArrayValue(key, new short[0]);
				}else if(type.equals(Class_Int)){				
					  cdoRequest.setIntegerArrayValue(key, new int[0]);
				}else if(type.equals(Class_Integer)){				
					  cdoRequest.setIntegerArrayValue(key, new int[0]);
				}else if(type.equals(Class_Long)){				
					 cdoRequest.setLongArrayValue(key, new long[0]);
				}else if(type.equals(Class_Float)){				
					cdoRequest.setFloatArrayValue(key, new float[0]);
				}else if(type.equals(Class_Double)){				
					cdoRequest.setDoubleArrayValue(key, new double[0]);
				}else if(type.equals(Class_Boolean)){			
					cdoRequest.setBooleanArrayValue(key, new boolean[0]);
				}else if(type.equals(Class_String)){				
					cdoRequest.setStringArrayValue(key, new String[0]);
				}else if(type.equals(Class_Time)){				
					cdoRequest.setTimeArrayValue(key, new String[0]);
				}else if(type.equals(Class_Date)){				
					cdoRequest.setDateArrayValue(key, new String[0]);
				}else if(type.equals(Class_DateTime)){				
					cdoRequest.setDateTimeArrayValue(key, new String[0]);
				}else if(type.equals(Class_CDO)){				
					cdoRequest.setCDOArrayValue(key, new CDO[0]);
				}else {				
					throw new JSONException("unsupported json to cdo type="+cls.getDeclaredField(key).getType().getName());
				}			
		}catch(Exception ex){
			throw new JSONException(ex);
		}

	}
	/**
	 * 设置常规类型数组
	 * @param cdoRequest
	 * @param key
	 * @param values
	 * @param json2CDOType
	 * @param cls
	 * @throws JSONException
	 */
	private static void setCommonArray(CDO cdoRequest,String key,List<String> commonList,Class<?>  cls,Set<String> clsFieldsName,String defaultType) throws JSONException{
		String[] values=commonList.toArray(new String[commonList.size()]);
		try{
			String type=getClassType(cls, key,clsFieldsName,defaultType);
				if(type.equals(Class_Byte)){					
					 cdoRequest.setByteArrayValue(key,Utility.parseByteArrayValue(values));
				}else if(type.equals(Class_Short)){						
					 cdoRequest.setShortArrayValue(key, Utility.parseShortArrayValue(values));
				}else if(type.equals(Class_Int) || type.equals(Class_Integer)){										
					  cdoRequest.setIntegerArrayValue(key, Utility.parseIntegerArrayValue(values));
				}else if(type.equals(Class_Long)){					
					 cdoRequest.setLongArrayValue(key, Utility.parseLongArrayValue(values));
				}else if(type.equals(Class_Float)){					
					cdoRequest.setFloatArrayValue(key,  Utility.parseFloatArrayValue(values));
				}else if(type.equals(Class_Double)){					
					cdoRequest.setDoubleArrayValue(key, Utility.parseDoubleArrayValue(values));
				}else if(type.equals(Class_Boolean)){					
					cdoRequest.setBooleanArrayValue(key,Utility.parseBooleanArrayValue(values));
				}else if(type.equals(Class_String)){					
					cdoRequest.setStringArrayValue(key, Utility.parseStringArrayValue(values));
				}else if(type.equals(Class_Time)){					
					cdoRequest.setTimeArrayValue(key, values);
				}else if(type.equals(Class_Date)){					
					cdoRequest.setDateArrayValue(key, values);
				}else if(type.equals(Class_DateTime)){					
					cdoRequest.setDateTimeArrayValue(key,values);
				}else {					
					throw new JSONException("unsupported json to cdo type,Json Key=["+key+"] Json value["+commonList+"] cast to "+cls.getDeclaredField(key).getType().getName());
				}			
		}catch(Exception ex){
			throw new JSONException(ex);
		}
	}
	
	private static String getClassType(Class<?> cls,String key,Set<String> clsFieldsName,String defaultType) throws NoSuchFieldException, SecurityException {
		if(defaultType!=null){
			if(!clsFieldsName.contains(key)){
				return defaultType; 
			}
		}
		String type=cls.getDeclaredField(key).getType().getSimpleName().toUpperCase();			
		type=type.indexOf("[")>0?type.substring(0,type.indexOf("[")):type;
		return type;
	}
}
