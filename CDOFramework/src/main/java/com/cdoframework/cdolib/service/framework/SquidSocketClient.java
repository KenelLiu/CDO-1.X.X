package com.cdoframework.cdolib.service.framework;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.cdoframework.cdolib.base.Return;
import com.cdoframework.cdolib.data.cdo.CDO;
import com.cdoframework.cdolib.service.framework.schema.Parameter;
import com.cdoframework.cdolib.service.framework.schema.URLCacheServer;
import com.cdoframework.cdolib.service.framework.transfilter.schema.CacheURL;
import com.cdoframework.cdolib.service.framework.transfilter.schema.RemoveURLCache;

public class SquidSocketClient implements IURLCacheServerClient
{
	private static Logger logger = Logger.getLogger(SquidSocketClient.class);
	private String strId;
	private String domain;
	private int nPort = 80;

	private HashMap<String,String> hmParameter;
	
	public SquidSocketClient()
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
		String strPort = this.hmParameter.get("Port");
		if(strPort!=null)
		{
			this.nPort = Integer.parseInt(strPort);
		}		
		this.strId = urlCacheServer.getId();
		this.domain = this.hmParameter.get("domain");
		return Return.OK;
	}


	public boolean removeCacheURL(RemoveURLCache removeURLCache,CDO cdoRequest,CDO cdoResponse)
	{
		if(removeURLCache==null)
		{//不需要清除
			return true;
		}
		boolean bOK = true;
		CacheURL[] cacheURLs = removeURLCache.getCacheURL();
		for(CacheURL cacheURL: cacheURLs)
		{
			try
			{
				String strURL = FrameworkUtil.getString(cacheURL.getType().getType(),cacheURL.getContent(),cdoRequest,cdoResponse);
				String strHost = cacheURL.getHost();
				if(strHost==null)
				{
					strHost = this.domain;
				}
				this.removeURLCache(strURL,strHost);					
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
		{//不需要清除
			return true;
		}
		boolean bOK = true;
		CacheURL[] cacheURLs = removeURLCache.getCacheURL();
		for(CacheURL cacheURL: cacheURLs)
		{
			try
			{
				String strURL = FrameworkUtil.getString(fromIndex,strIndexId,cacheURL.getType().getType(),cacheURL.getContent(),cdoRequest,cdoResponse);
				String strHost = cacheURL.getHost();
				if(strHost==null)
				{
					strHost = this.domain;
				}
				this.removeURLCache(strURL,strHost);
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
			logger.error("getCDOJsonURL:"+e.getMessage(),e);
		}
		return _strURL;
	}
	public static boolean purge(String strDomain,int nPort,String url,String urlHost) throws UnknownHostException, IOException
	{
		StringBuilder sb = new StringBuilder();
		sb.append("PURGE ").append(url).append("HTTP/1.1HOST:").append(urlHost);
		
		Socket socket = new Socket(strDomain,nPort);
		OutputStream outputStream = socket.getOutputStream();
		outputStream.write(sb.toString().getBytes());
		outputStream.flush();
		outputStream.close();
		return true;
	}

	public void removeURLCache(String url,String webServerHost) throws UnknownHostException, IOException
	{
		if(webServerHost==null)
		{
			webServerHost = this.domain;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("PURGE ").append(url).append("HTTP/1.1HOST:").append(webServerHost);
		
		Socket socket = new Socket(this.domain,this.nPort);
		OutputStream outputStream = socket.getOutputStream();
		outputStream.write(sb.toString().getBytes());
		outputStream.flush();
		outputStream.close();			
	}
	
	public String getId()
	{
		return this.strId;
	}

	public boolean testConnection()
	{
		try
		{
			Socket socket = new Socket(this.domain,nPort);
			socket.getOutputStream();
		}
		catch(Exception e)
		{
			logger.error("testConnection:"+e.getMessage(),e);
			return false;
		}
		return true;
	}
	
	public static void main(String[] args)
	{
		String strDomain = "mall.business.my";
		int nPort = 80;
		CDO cdo = new CDO();
		cdo.setStringValue("strServiceName","SellerGoodsService");
		cdo.setStringValue("strTransName","getSellerSubGoods");
		cdo.setLongValue("lSellerGoodsId",100000L);
		cdo.setLongValue("lSubGoodsId",1000L);
		cdo.setLongValue("lSellerId",2);
		StringBuilder sb = new StringBuilder();
		sb.append("http:/mall.business.my/handleTrans.cdo?strTransName=").append("getSellerSubGoods&&$$CDOREQUEST=$$");

		String strRequest = null;;
		try
		{
			strRequest=java.net.URLEncoder.encode(cdo.toXML(),"UTF-8");
			sb.append(strRequest);
			String url = sb.toString();
			if(logger.isInfoEnabled()){
				logger.info("to remove url="+url);
			}
			String urlHost = "business.mall.my";
			SquidSocketClient.purge(strDomain,nPort,url,urlHost);
		}
		catch(Exception e)
		{
			logger.error("main:"+e.getMessage(),e);
		}
		if(logger.isInfoEnabled()){
			logger.info("complete");
		}
	}

}
