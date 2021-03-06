package com.cdoframework.cdolib.service.framework;


import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.log4j.Logger;

import com.cdoframework.cdolib.base.Return;
import com.cdoframework.cdolib.data.cdo.CDO;
import com.cdoframework.cdolib.service.framework.schema.Parameter;
import com.cdoframework.cdolib.service.framework.schema.URLCacheServer;
import com.cdoframework.cdolib.service.framework.transfilter.schema.CacheURL;
import com.cdoframework.cdolib.service.framework.transfilter.schema.RemoveURLCache;

public class SquidHttpClient implements IURLCacheServerClient
{
	private static Logger logger = Logger.getLogger(SquidHttpClient.class);
	private String KEY;
	private String strId;
	private String URL;
	private String DDA="MD5";
	private HashMap<String,String> hmParameter;
	public SquidHttpClient()
	{
		this.hmParameter = new HashMap<String,String>(4);
	}
	public Return init(URLCacheServer urlCacheServer)
	{
		Parameter[] paras = urlCacheServer.getParameter();
		if(paras!=null && paras.length>0)
		{
			for(Parameter para:paras)
			{
				this.hmParameter.put(para.getName(),para.getValue());
			}
		}
		String strDDA = this.hmParameter.get("DDA");
		if(strDDA!=null)
		{
			this.DDA = strDDA;
		}
		
		this.strId = urlCacheServer.getId();
		this.URL = this.hmParameter.get("URL");
		this.KEY = this.hmParameter.get("KEY");
		return Return.OK;
	}
	public  boolean removeCacheURL(String url)
	{
		if(url==null || url.length()>1024)
		{
			return false;
		}
		try
		{
			String urlTemp = URL.replace("{URL}",url);
			String strKey = DDA(url);
			urlTemp = urlTemp.replace("{KEY}",strKey);
			return doGet(urlTemp);
		}
		catch(Exception e)
		{
			logger.error("removeCacheURL:"+e.getMessage(),e);
			return false;
		}
	}
	public boolean doGet(String url)
	{
		HttpGet getMethod = null;
		HttpClient httpClient = new DefaultHttpClient();
		getMethod = new HttpGet(url);

		// ??????GetMethod
		int statusCode = 0;
		try {
			
			HttpConnectionParams.setConnectionTimeout(httpClient.getParams(),
					6000);
			HttpConnectionParams.setSoTimeout(httpClient.getParams(),
					6000);
			HttpResponse response=httpClient.execute(getMethod);
			statusCode=response.getStatusLine().getStatusCode();
			if(statusCode!=HttpStatus.SC_OK){
				logger.error("failed to connect to  server");
				return false;
			}

			if(response.getEntity()!=null)
				return true;
			logger.error("response.getEntity() is null");
			return false;	
		} catch (Exception e) {
			logger.error("doGet:"+e.getMessage(),e);
			return false;
		}
		finally
		{
			//TODO ???????????????
		}
	
	}
	private String DDA(String strURL)
	{
        StringBuilder sb = new StringBuilder();
        MessageDigest messagedigest = null;
		try
		{
			messagedigest = MessageDigest.getInstance(DDA);
		}
		catch(NoSuchAlgorithmException e)
		{
			logger.error("error>>cannot get MD5>>",e);				
			return "";
		}
		
		messagedigest.update((strURL + KEY).getBytes());
        byte abyte[] = messagedigest.digest();
        for(int i = 0; i < abyte.length; i++) {
            String _str = Integer.toHexString(0xff & (abyte[i]));
            if (_str != null && _str.length() == 1) {
                _str = "0" + _str;
            }
            sb.append(_str);
        }	        
        return sb.toString();
	}
	public boolean removeCacheURL(RemoveURLCache removeURLCache,CDO cdoRequest,CDO cdoResponse)
	{
		if(removeURLCache==null)
		{//???????????????
			return true;
		}
		boolean bOK = true;
		CacheURL[] cacheURLs = removeURLCache.getCacheURL();
		for(CacheURL cacheURL: cacheURLs)
		{
			try
			{
				String strURL = FrameworkUtil.getString(cacheURL.getType().getType(),cacheURL.getContent(),cdoRequest,cdoResponse);
				boolean bOKTemp = removeCacheURL(strURL);	
				if(bOKTemp == false)
				{
					bOK = false;
				}
			}
			catch(Exception e)
			{
				bOK = false;
				logger.error("removeCacheURL:"+e.getMessage(),e);
			}
		}
		return bOK;
	}
	
	public boolean removeCacheURL(int fromIndex,String strIndexId,RemoveURLCache removeURLCache,CDO cdoRequest,CDO cdoResponse)
	{
		if(removeURLCache==null)
		{//???????????????
			return true;
		}
		boolean bOK = true;
		CacheURL[] cacheURLs = removeURLCache.getCacheURL();
		for(CacheURL cacheURL: cacheURLs)
		{
			try
			{
				String strURL = FrameworkUtil.getString(fromIndex,strIndexId,cacheURL.getType().getType(),cacheURL.getContent(),cdoRequest,cdoResponse);
				removeCacheURL(strURL);	
			}
			catch(Exception e)
			{
				bOK = false;
				logger.error("removeCacheURL:"+e.getMessage(),e);
			}
		}
		return bOK;
	}
	
	public static String getCDOXMLURL(String URLPre,String strTransName,CDO cdoRequest)
	{
		String _strURL = null;
		try
		{
			 _strURL = URLPre + "?strTransName=" + strTransName + "&$$CDORequest$$=" + java.net.URLEncoder.encode(cdoRequest.toXML(),"UTF-8");
		}
		catch(UnsupportedEncodingException e)
		{
			logger.error("getCDOXMLURL:"+e.getMessage(),e);
		}
		return _strURL;
	}
	public static String getCDOJsonURL(String URLPre,String strTransName,CDO cdoRequest)
	{
		String _strURL = null;
		try
		{
			 _strURL = URLPre + "?strTransName=" + strTransName + "&$$CDORequest$$=" + java.net.URLEncoder.encode(cdoRequest.toJSON(),"UTF-8");
		}
		catch(UnsupportedEncodingException e)
		{
			logger.error("getCDOXMLURL:"+e.getMessage(),e);
		}
		return _strURL;
	}

	public String getId()
	{
		return this.strId;
	}

	public void removeURLCache(String url,String webServerHost) throws Exception
	{
		this.removeCacheURL(url);
		
	}

	public boolean testConnection()
	{
		return true;
	}
}
