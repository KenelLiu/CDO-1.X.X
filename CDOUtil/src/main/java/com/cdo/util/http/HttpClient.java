package com.cdo.util.http;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.ByteArrayBuffer;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import com.cdo.util.bean.CDOHTTPResponse;
import com.cdo.util.bean.OutStreamCDO;
import com.cdo.util.constants.Constants;
import com.cdo.util.exception.HttpException;
import com.cdo.util.resource.GlobalResource;
import com.cdoframework.cdolib.data.cdo.CDO;

/**
 * 
 * @author KenelLiu
 *
 */
public class HttpClient {
	private Logger log = Logger.getLogger(HttpClient.class);
    public static final ContentType TEXT_PLAIN_UTF8 =ContentType.create("text/plain", Charset.forName(Constants.Encoding.CHARSET_UTF8));
	//----------????????????---------//
	public static final int TRANSMODE_BODY = 1;//????????????  ??????SOAP xml?????? ?????????body?????????
	public static final int TRANSMODE_FORM = 2;//??????form??????  ??????????????????
	//--------??????-------------//
	public static final String METHOD_POST = "POST";
	public static final String METHOD_PUT = "PUT";
	public static final String METHOD_GET = "GET";
	public static final String METHOD_DELETE = "DELETE";
	
	private org.apache.http.client.HttpClient httpClient;
	private HttpRequestBase httpRequest;
	private Map<String, String> headers;
	private Map<String, File>  uploadFiles;
	private int transMode;
	private String url;
	private String method;


	private List<NameValuePair> paramsList = new ArrayList<NameValuePair>();

	
	private String body = null;

	private HttpHost httpHost = null;
	
	
	public HttpClient() {
		this(null,METHOD_POST);
	}

	public HttpClient(String url) {
		this(url,METHOD_POST);
	}
	
	public HttpClient(String url,String method) {
		this(url,method,TRANSMODE_FORM);
	}
	
	public HttpClient(String url, String method,int transMode) {
		this(url,method,transMode,null);	
	}

	public HttpClient(String url, String method,int transMode,
			Map<String, String> headers) {
		this.url = url;
		this.transMode = transMode;
		this.method = method;
		this.headers = headers;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setTransMode(int transMode) {
		this.transMode = transMode;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

	public void setUploadFiles(Map<String, File> uploadFiles) {	
		this.uploadFiles = uploadFiles;
	}	
	
	public void setNameValuePair(Map<String, String> param) {		
		if (param == null)
			return;
		for (Map.Entry<String,String> entry : param.entrySet()) {			
			if ((entry.getKey()  == null) || (entry.getKey().trim().equals("")))
				continue;
			this.paramsList.add(new BasicNameValuePair(entry.getKey().trim(), entry.getValue()));
		}
	}

	public CDOHTTPResponse execute(){
		return execute(null);
	}
	
	public CDOHTTPResponse execute(CDO cdoResponse){	
		HTTPResponse handler=new HTTPResponse();
		if(cdoResponse!=null && cdoResponse instanceof OutStreamCDO){
			OutStreamCDO streamCDO=(OutStreamCDO)cdoResponse;
			if(streamCDO.getOutputStream()!=null){
				handler.setOutputStream(streamCDO.getOutputStream());
				handler.setAutoCloseStream(streamCDO.isAutoCloseStream());
			}			
		}		
		return handleResponse(handler);
	}
	/**
	 * ??????Http Request,???????????????
	 * @param handler
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private  <T>T handleResponse(ResponseHandler<T> handler) {
		Object response = null;
		try {
			setRequestParam();		
			this.httpClient = HttpUtil.getHttpClient();	
			if (this.httpHost != null)
				response = this.httpClient.execute(this.httpHost,this.httpRequest, handler);
			else 
				response = this.httpClient.execute(this.httpRequest, handler);
			
			return (T)response;
		} catch (Exception e) {
			if(httpRequest!=null){try{httpRequest.abort();}catch(Exception em){}}
			log.error(e.getMessage(), e);
			throw new HttpException("httpClient send request error:"+ e.getMessage());
		}finally{
			if(httpRequest!=null){try{httpRequest.releaseConnection();}catch(Exception ex){}};		
		}
	}
	/**
	 * ??????????????????
	 */
	private void setRequestParam() {
		HttpEntity  entity = null;		
		try {
			if (this.url == null || this.url.trim().length()==0) {
				throw new HttpException("http url is null");
			}	
			
			if (this.transMode ==TRANSMODE_BODY) {
				//??????body??????
				if (this.body!= null) {
					entity = new StringEntity(this.body,Constants.Encoding.CHARSET_UTF8);
				}
			} else {
				//????????????form??????,????????????,?????????MultipartEntityBuilder
				if(this.uploadFiles!=null && this.uploadFiles.size()>0){
					MultipartEntityBuilder reqEntity=MultipartEntityBuilder.create();
					reqEntity.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);					
					reqEntity.setCharset(Charset.forName(Constants.Encoding.CHARSET_UTF8));
					//????????????
					for (Map.Entry<String,File> entry : this.uploadFiles.entrySet()) {			
						if ((entry.getKey()  == null) || (entry.getKey().trim().equals("")))
							continue;
						reqEntity.addBinaryBody(entry.getKey(),  entry.getValue());						
					}
					//????????????
					for(NameValuePair pair:this.paramsList){
						reqEntity.addTextBody(pair.getName(),pair.getValue(),TEXT_PLAIN_UTF8);	
					}
					entity=reqEntity.build();
				}else{
					//??????????????????
					entity = new UrlEncodedFormEntity(this.paramsList, Constants.Encoding.CHARSET_UTF8);
				}				
			}
				
			if (this.method.equals(METHOD_PUT)) {
				this.httpRequest = new HttpPut(this.url);
				((HttpEntityEnclosingRequestBase) this.httpRequest).setEntity(entity);
			} else if (this.method.equals(METHOD_GET)) {
				this.httpRequest = new HttpGet(this.url);
			} else if (this.method.equals(METHOD_DELETE)) {
				this.httpRequest = new HttpDelete(this.url);
			} else {
				this.httpRequest = new HttpPost(this.url);
				((HttpPost)this.httpRequest).setEntity(entity);
				
			}
			if (this.headers != null) {
				for (Map.Entry<String,String> entry : this.headers.entrySet()) {			
					if ((entry.getKey()  == null) || (entry.getKey().trim().equals("")))
						continue;
					this.httpRequest.setHeader(entry.getKey().trim(), entry.getValue());
				}			
			}
			this.httpRequest.setHeader("Connection", "close");
			
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new HttpException("http ??????????????????:"+e.getMessage(),e);
		}
		
	}


	public void setProxy(String url, int port) {
		this.httpHost = new HttpHost(url, port);
	}
		


	
	class HTTPResponse implements ResponseHandler<CDOHTTPResponse>{
		private OutputStream outStream;
		private boolean autoCloseStream=true;
		public HTTPResponse(){
			
		}
		public void setOutputStream(OutputStream outStream){
			this.outStream=outStream;
		}
		public void setAutoCloseStream(boolean autoCloseStream){
			this.autoCloseStream=autoCloseStream;
		}
		
		public CDOHTTPResponse handleResponse(HttpResponse response)
				throws ClientProtocolException, IOException {
			CDOHTTPResponse cdoHttpResponse=new CDOHTTPResponse();
			cdoHttpResponse.setAllHeaders(response.getAllHeaders());
			cdoHttpResponse.setStatusCode(response.getStatusLine().getStatusCode());			
			//----????????????????????????	 ????????????????????????  ??????cdo??????---//
			LinkedHashMap<String, FileInfo> linkMap=readHeader(response.getAllHeaders());
			try{					
				if(linkMap.size()==1){//?????? cdo??????				 
					cdoHttpResponse.setResponseBytes(response.getEntity()==null?null:EntityUtils.toByteArray(response.getEntity()));					
				}else if(linkMap.size()>1){//--cdo  ?????????   ????????????,??????????????????cdo---//	
					outMixData(response, linkMap, cdoHttpResponse);
				}else{
					 //??????http ??????  ?????????????????????????????????				
		    		 Header header = response.getFirstHeader("Content-Disposition"); 					
		    		 if(header==null){
		    			 //????????????????????????,???????????????
		    			 cdoHttpResponse.setResponseBytes(response.getEntity()==null?null:EntityUtils.toByteArray(response.getEntity()));
		    		 }else{
		    			 //????????????		    			
		    			 HttpEntity resEntity = response.getEntity();		    			 	
	    				 if(resEntity==null){//body ??? empty	    					
	    					 log.warn("download http entity is empty ....");
	    					 return cdoHttpResponse;
	    				 }	
	    				 download(header, response, cdoHttpResponse);
		    		 }					
				}
			  return cdoHttpResponse;
			}catch(Exception ex){
		    		 throw new IOException(ex.getMessage(),ex);
		   }	
	 }
		//---------------------?????? ????????????,?????????cdo  header------------------//	
		private LinkedHashMap<String, FileInfo> readHeader(Header[] header){
				LinkedHashMap<String, FileInfo> linkMap=new	LinkedHashMap<String, FileInfo>(); 		
				for(int i=0;i<header.length;i++){
					if(header[i].getName().startsWith(Constants.CDO.HTTP_CDO_RESPONSE)){
						String[] filesField=header[i].getName().split(",");//??????cdo file key??????						
						String[] fileLen=header[i].getValue().split(";")[0].split(",");//??????  ????????????
						String[] filesName=header[i].getValue().split(";")[1].split(",");//?????? ?????????
						for(int j=0;j<filesField.length;j++){							
							linkMap.put(filesField[j],new FileInfo(filesName[j], Long.parseLong(fileLen[j])));	
						}						
					}
				}
				return linkMap;
		}
	 //-----------------------------????????????cdo ???????????????-----------------------//	
	 private void outMixData(HttpResponse response,LinkedHashMap<String, FileInfo> linkMap,CDOHTTPResponse cdoHttpResponse) throws IOException{					 
			 InputStream inStream=null;
			 try{
				//??????CDO	
				 inStream = response.getEntity().getContent();	
	    		 Iterator<String> it=linkMap.keySet().iterator();
	    		 String strFieldName=it.next();
	    		 FileInfo fileInfo=linkMap.get(strFieldName);
	    		 cdoHttpResponse.setResponseBytes(outCDO(inStream,(int)fileInfo.getLength()));
	    		 linkMap.remove(strFieldName);	    		 
	   			 if(this.outStream==null){
	   			     //????????????????????????????????? ???????????????????????? cdoHttpResponse
	   				 outFile(inStream, linkMap,cdoHttpResponse);	 
	   			 }else{
	   			     //????????????????????? ?????????????????????,??????????????????????????????
	   				 Iterator<Map.Entry<String,FileInfo>> iterator=linkMap.entrySet().iterator(); 
	   				 Entry<String,FileInfo> entry=iterator.next();
	   				 try{
	   					 outFile(inStream,entry.getValue().getLength(),this.outStream);
	   				 }finally{
	   					if(this.autoCloseStream){
	   						try{if(outStream!=null)outStream.close();}catch(Exception ex){};
	   					}
	   				 }
	   			 }	
			 }catch(Exception ex){
				 log.error(ex.getMessage(),ex);
				 throw new IOException(ex.getMessage(),ex);
			 }finally{
				 if(inStream!=null)try{inStream.close();}catch(Exception e){}
			 }
		}
		//-----------------???????????? CDO ??????---------------------//
		private byte[] outCDO(InputStream instream,int xmlLen) throws IOException{
            final ByteArrayBuffer buffer = new ByteArrayBuffer(xmlLen);           
            byte[] tmp =null;
			int len=xmlLen;		
			int maxSize=Constants.Byte.maxSize;		
			int l;
			while(len>0){					
			    int length=len>maxSize?maxSize:len;
			    tmp= new byte[length];
			    l=instream.read(tmp);
			    buffer.append(tmp, 0, l);
				len=len-maxSize;			
			}          
            return buffer.toByteArray();
		}
		//--------------???????????????????????????????????????-------------------//
		private String getTmpPath(){
	   		 String tmpDirPath =GlobalResource.cdoConfig==null?null:GlobalResource.cdoConfig.getString(Constants.CDO.TEMP_FILE_PATH);
		     if(tmpDirPath==null || tmpDirPath.trim().equals(""))
		            tmpDirPath=System.getProperty("java.io.tmpdir");
		     File tmpDir= new File(tmpDirPath); 
		     if(!tmpDir.exists() || !tmpDir.isDirectory())  
		           tmpDir.mkdirs();  
		     return tmpDirPath;
		}
		
		public void download( Header header,HttpResponse response,CDOHTTPResponse cdoHttpResponse) throws IOException{
			 InputStream inStream=null;
			 try{						 
				 inStream = response.getEntity().getContent();	
   			 HeaderElement[] values = header.getElements();  
   			 for(int i=0;i<values.length;i++){			    		
   				 NameValuePair param = values[i].getParameterByName("filename");
   				 if(param!=null){
	    	               try {   
	   	                        String fileName = param.getValue();   	                       
	   	                        if(this.outStream==null){
	   	                        	//?????????????????????
	   	                        	outFile(inStream, fileName,cdoHttpResponse);
	   	                        }else{
	   	                        	try{
	   	                        		//??????????????????
	   	                        		outStream(inStream, this.outStream);
	   	                        	}finally{
	   	     	   						if(this.autoCloseStream){
	   	     	   							try{if(outStream!=null)outStream.close();}catch(Exception ex){};
	   	     	   						}
	   	                        	}
	   	                        }
   	                        break;
   	                   } catch (Exception e) {  
   	                    	log.error(e.getMessage(), e);
   	                 }   
   				 }
   			 }	
			 }catch(Exception ex){
				 log.error(ex.getMessage(),ex);
				 throw new IOException(ex.getMessage(),ex);
			 }finally{
				 if(inStream!=null)try{inStream.close();}catch(Exception e){}
			 }			
		}
		//--------------------???????????????????????????????????????????????????cdoHTTPResponse-------------//
		private void outFile(InputStream inStream,String fileName,CDOHTTPResponse cdoHTTPResponse) throws IOException{
			  String tmpDirPath=getTmpPath();
			  FileOutputStream fileout=null;
			  try{	   				
	   			  String suffix=fileName.substring(fileName.lastIndexOf("."),fileName.length());
	   			  String date=new SimpleDateFormat("yyyyMMddHHmmsss").format(new Date());
	   		      String tempPath =tmpDirPath+"/"+fileName.substring(0,fileName.lastIndexOf("."))+"_"+date+ suffix;				    				 
	   			  File tmpFile=new File(tempPath);
	   			  if(!tmpFile.exists())
	   			    	tmpFile.createNewFile();
	   			    
	   			 fileout= new FileOutputStream(tmpFile);  
	   			 outStream(inStream, fileout);
	   			 cdoHTTPResponse.addFile(fileName, tmpFile);
	         } catch (Exception e) {  
	        	throw new IOException(e.getMessage(),e);
	         }finally{
	        	 fileout.close();	        	 
	        } 	  
		}
		//-------------------???????????????-----------------------//
		private void outStream(InputStream inStream,OutputStream outputStream) throws IOException{
			  try{	   				 
	             byte[] buffer=new byte[Constants.Byte.maxSize*2];  
	             int ch = 0;  
	             while ((ch = inStream.read(buffer)) != -1) {  
	            	 outputStream.write(buffer,0,ch);  
	             }  	            
	             outputStream.flush(); 		         
	         } catch (Exception e) {  
	        	throw new IOException(e.getMessage(),e);
	         }	  
		}	
		//------------------????????????????????? ?????????????????????------------------//
		private void outFile(InputStream inStream,LinkedHashMap<String, FileInfo> linkMap,CDOHTTPResponse cdoResponse) throws IOException{
			  String tmpDirPath=getTmpPath();			
		     //?????????        
	   		 for(Iterator<Map.Entry<String,FileInfo>> it=linkMap.entrySet().iterator();it.hasNext();){
	   			 	Entry<String,FileInfo> entry=it.next();
	   			 	//????????????
	   				String fileName=entry.getValue().getFileName();
	   			    String suffix=fileName.substring(fileName.lastIndexOf("."),fileName.length());
	   			    String date=new SimpleDateFormat("yyyyMMddHHmmsss").format(new Date());
	   				String tempPath =tmpDirPath+"/"+fileName.substring(0,fileName.lastIndexOf("."))+"_"+date+suffix;				    				 
	   			    File tmpFile=new File(tempPath);
	   			    if(!tmpFile.exists())
	   			    	tmpFile.createNewFile();
	   			    FileOutputStream fileOutputStream=null;
	   			    try{
	   			    	fileOutputStream=new FileOutputStream(tmpFile);
	   				    outFile(inStream,entry.getValue().getLength(),fileOutputStream);
	   			      }finally{
	   			    	  try{if(fileOutputStream!=null)fileOutputStream.close();}catch(Exception ex){};
	   				 }	   				 
	   				 cdoResponse.addFile(entry.getKey(), tmpFile);
	   		 }		
		}
		
		//----------------------??????????????????????????? ----------------------//
		private void outFile(InputStream inStream,long fileLen,OutputStream outputStream) throws IOException{            
            try{		
				int maxSize=Constants.Byte.maxSize*2;		
				int l;
				long total=0;
				byte[] tmp =null;
				while(fileLen>total){	
					long remainLen=fileLen-total;
				    int length=remainLen>maxSize?maxSize:(int)remainLen;
				    tmp= new byte[length];
			        if((l = inStream.read(tmp)) != -1) {			        	
			        	outputStream.write(tmp,0, l);			            
			        }
			        total=total+l;		
				}  
				outputStream.flush();		       
            }catch(Exception ex){            	
            	throw new IOException(ex.getMessage(),ex);
            }
		}

		
		class FileInfo {
			String fileName;
			long  length;
			public FileInfo(String fileName,long length){
				this.fileName=fileName;
				this.length=length;
			}
			public String getFileName() {
				return fileName;
			}
			public void setFileName(String fileName) {
				this.fileName = fileName;
			}
			public long getLength() {
				return length;
			}
			public void setLength(long length) {
				this.length = length;
			}
			
		}	
	}
}