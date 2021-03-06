package com.cdoframework.cdolib.database;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;


import org.apache.commons.dbcp.BasicDataSource;
import org.apache.log4j.Logger;

import com.cdoframework.cdolib.base.Date;
import com.cdoframework.cdolib.base.DateTime;
import com.cdoframework.cdolib.base.ObjectExt;
import com.cdoframework.cdolib.base.Return;
import com.cdoframework.cdolib.base.Time;
import com.cdoframework.cdolib.base.Utility;
import com.cdoframework.cdolib.data.cdo.CDO;
import com.cdoframework.cdolib.data.cdo.CDOArrayField;
import com.cdoframework.cdolib.database.dataservice.BlockType;
import com.cdoframework.cdolib.database.dataservice.BlockTypeItem;
import com.cdoframework.cdolib.database.dataservice.Delete;
import com.cdoframework.cdolib.database.dataservice.Else;
import com.cdoframework.cdolib.database.dataservice.For;
import com.cdoframework.cdolib.database.dataservice.If;
import com.cdoframework.cdolib.database.dataservice.Insert;
import com.cdoframework.cdolib.database.dataservice.OnError;
import com.cdoframework.cdolib.database.dataservice.OnException;
import com.cdoframework.cdolib.database.dataservice.SQLBlockType;
import com.cdoframework.cdolib.database.dataservice.SQLBlockTypeItem;
import com.cdoframework.cdolib.database.dataservice.SQLElse;
import com.cdoframework.cdolib.database.dataservice.SQLFor;
import com.cdoframework.cdolib.database.dataservice.SQLIf;
import com.cdoframework.cdolib.database.dataservice.SQLThen;
import com.cdoframework.cdolib.database.dataservice.SQLTrans;
import com.cdoframework.cdolib.database.dataservice.SQLTransChoiceItem;
import com.cdoframework.cdolib.database.dataservice.SelectField;
import com.cdoframework.cdolib.database.dataservice.SelectRecord;
import com.cdoframework.cdolib.database.dataservice.SelectRecordSet;
import com.cdoframework.cdolib.database.dataservice.SetVar;
import com.cdoframework.cdolib.database.dataservice.Then;
import com.cdoframework.cdolib.database.dataservice.Update;
import com.cdoframework.cdolib.database.dataservice.types.SQLIfTypeType;
import com.cdoframework.cdolib.database.dataservice.types.SetVarTypeType;

/**
 * @author Frank
 */
public class DataEngine implements IDataEngine
{
	private static final Logger log = Logger.getLogger(DataEngine.class);
	class AnalyzedSQL
	{
		public String strSQL;
		public ArrayList<String> alParaName;
	}

	// ????????????,??????static????????????????????????------------------------------------------------------------------------
	public final int RETURN_SYSTEMERROR=-1;

	// ?????????????????????????????????
	// protected static final String
	// DRIVER_SQLSERVER="net.sourceforge.jtds.jdbc.Driver";
	// protected static final String DRIVER_ORACLE
	// ="oracle.jdbc.driver.OracleDriver";
	// protected static final String DRIVER_DB2
	// ="com.ibm.db2.jdbc.app.DB2Driver";
	// protected static final String DRIVER_MYSQL ="com.mysql.jdbc.Driver";

	// ?????????????????????
	public static final String SQLSERVER="SQLServer";
	public static final String ORACLE="Oracle";
	public static final String DB2="DB2";
	public static final String MYSQL="MySQL";

	// ????????????,??????????????????????????????????????????????????????--------------------------------------------------------------
	protected BasicDataSource ds;   
	protected String strSystemCharset;
	private HashMap<String,ArrayList<PreparedStatement>> hmStatement;
	private HashMap<String,AnalyzedSQL> hmAnalyzedSQL;

	// ????????????,??????????????????????????????????????????????????????????????????????????????get/set??????-----------------------------------
	protected String strDriver;

	public void setDriver(String strDriver)
	{
		this.strDriver=strDriver;
	}

	public String getDriver()
	{
		return this.strDriver;
	}

	protected String strURI;

	public void setURI(String strURI)
	{
		this.strURI=strURI;
	}

	public String getURI()
	{
		return this.strURI;
	}

	protected String strCharset;

	public String getCharset()
	{
		return strCharset;
	}

	public void setCharset(String strCharset)
	{
		this.strCharset=strCharset;
	}

	protected Properties properties;

	public Properties getProperties()
	{
		return properties;
	}

	public void setProperties(Properties properties)
	{
		this.properties=properties;
	}

	protected String strUserName;

	public String getUserName()
	{
		return strUserName;
	}

	public void setUserName(String strUserName)
	{
		this.strUserName=strUserName;
	}

	protected String strPassword;

	public String getPassword()
	{
		return strPassword;
	}

	public void setPassword(String strPassword)
	{
		this.strPassword=strPassword;
	}

	protected int nMinConnectionCount;

	public void setMinConnectionCount(int nMinConnectionCount)
	{
		this.nMinConnectionCount=nMinConnectionCount;
	}

	public int getMinConnectionCount()
	{
		return this.nMinConnectionCount;
	}

	protected int nMaxConnectionCount;

	public void setMaxConnectionCount(int nMaxConnectionCount)
	{
		this.nMaxConnectionCount=nMaxConnectionCount;
	}

	public int getMaxConnectionCount()
	{
		return this.nMaxConnectionCount;
	}

	protected long lTimeout;

	public void setTimeout(long lTimeout)
	{
		this.lTimeout=lTimeout;
	}

	public long getTimeout()
	{
		return this.lTimeout;
	}

	public boolean isOpened()
	{
		if(ds==null)
		{
			return false;
		}
		else
		{
			return true;
		}
	}
	
	protected int nLoadLevel;
	public void setLoadLevel(int nLoadlevel)
	{
		this.nLoadLevel=nLoadlevel;
	}
	public int getLoadLevel()
	{
		return this.nLoadLevel;
	}

	// ????????????,??????????????????????????????????????????????????????????????????set??????-----------------------------------------------

	// ????????????,???????????????????????????????????????????????????????????????protected??????-------------------------------------------
	protected void callOnException(String strText,Exception e)
	{
		try
		{
			onException(strText,e);
		}
		catch(Exception ex)
		{
		}
	}

	/**
	 * ??????SQL?????? {}???????????????????????????????????????? {{??????{?????? }}??????}??????
	 */
	protected AnalyzedSQL analyzeSourceSQL(String strSourceSQL)
	{
		AnalyzedSQL anaSQL=null;
		synchronized(hmAnalyzedSQL)
		{
			anaSQL=hmAnalyzedSQL.get(strSourceSQL);
		}
		if(anaSQL!=null)
		{
			return anaSQL;
		}

		ArrayList<String> alParaName=new ArrayList<String>();

		StringBuilder strbSQL=new StringBuilder();

		int nState=0;// 0 : {} ???????????????, 1: {}????????????.
		int nLength=strSourceSQL.length();

		StringBuilder strbParaName=new StringBuilder(nLength);
		int i=0;
		while(i<nLength)
		{
			char ch=strSourceSQL.charAt(i);
			switch(ch)
			{
				case '{':
					if(nState==0)
					{// ???{}??????
						if(i+1<nLength&&strSourceSQL.charAt(i+1)=='{')
						{// ???????????????
							strbSQL.append("{");
							i+=2;
						}
						else
						{// ????????????
							nState=1;
							i++;
						}
					}
					else
					{// ???{}?????????????????????
						callOnException("analyzeSQL error",new Exception("SQL syntax Error: "+strSourceSQL));
						return null;
					}
					break;
				case '}':
					if(nState==0)
					{// ???{}??????
						if(i+1<nLength&&strSourceSQL.charAt(i+1)=='}')
						{// ???????????????
							strbSQL.append("}");
							i++;
						}
						else
						{// ????????????
							callOnException("analyzeSQL error",new Exception("SQL syntax Error: "+strSourceSQL));
							return null;
						}
					}
					else
					{// ???{}?????????????????????
						if(strbParaName.length()==0)
						{
							callOnException("analyzeSQL error",new Exception("SQL syntax Error: "+strSourceSQL));
							return null;
						}
						nState=0;
						strbSQL.append("?");
						alParaName.add(strbParaName.toString());
						strbParaName.setLength(0);
					}
					i++;
					break;
				default:
					if(nState==0)
					{// ???{}??????
						strbSQL.append(ch);
					}
					else
					{
						strbParaName.append(ch);
					}
					i++;
					break;
			}
		}

		if(nState==1)
		{
			callOnException("analyzeSQL error",new Exception("SQL syntax Error: "+strSourceSQL));
			return null;
		}

		anaSQL=new AnalyzedSQL();
		anaSQL.strSQL=strbSQL.toString();
		anaSQL.alParaName=alParaName;

		synchronized(hmAnalyzedSQL)
		{
			hmAnalyzedSQL.put(strSourceSQL,anaSQL);
		}

		return anaSQL;
	}

	public PreparedStatement prepareStatement(Connection conn,String strSourceSQL,CDO cdoRequest) throws SQLException
	{
		onSQLStatement(strSourceSQL);

		PreparedStatement ps=null;

		// ????????????SQL??????????????????????????????
		AnalyzedSQL anaSQL=analyzeSourceSQL(strSourceSQL);
		if(anaSQL==null)
		{
			throw new SQLException("Analyze source SQL exception: "+strSourceSQL);
		}

		// ??????JDBC??????
		try
		{
			if(ps==null)
			{
				ps=conn.prepareStatement(anaSQL.strSQL);
			}

			int nParaCount=anaSQL.alParaName.size();
			for(int i=0;i<nParaCount;i++)
			{
				String strParaName=anaSQL.alParaName.get(i);
				ObjectExt object=cdoRequest.getObject(strParaName);
				Object objValue=object.getValue();
				if(objValue==null){
					ps.setNull(i+1,java.sql.Types.NULL);
					continue;
				}
				int nType=object.getType();
				switch(nType)
				{
					case ObjectExt.BYTE_TYPE:
					case ObjectExt.SHORT_TYPE:
					case ObjectExt.INTEGER_TYPE:
					case ObjectExt.LONG_TYPE:
					case ObjectExt.FLOAT_TYPE:
					case ObjectExt.DOUBLE_TYPE:						
					{
						
						ps.setObject(i+1,objValue);
						break;
					}
					case ObjectExt.STRING_TYPE:
					{
						String strValue=(String)objValue;
						strValue=Utility.encodingText(strValue,strSystemCharset,strCharset);
						ps.setString(i+1,strValue);
						break;
					}
					case ObjectExt.DATE_TYPE:
					{
						String strValue=(String)objValue;
						if(strValue.length()==0)
						{
							ps.setNull(i+1,java.sql.Types.TIMESTAMP);
						}
						else
						{
							Date date=new Date((String)objValue);
							ps.setTimestamp(i+1,date.getTimestamp());
						}
						break;
					}
					case ObjectExt.TIME_TYPE:
					{
						String strValue=(String)objValue;
						if(strValue.length()==0)
						{
							ps.setNull(i+1,java.sql.Types.TIMESTAMP);
						}
						else
						{
							Time time=new Time((String)objValue);
							ps.setTimestamp(i+1,time.getTimestamp());
						}
						break;
					}
					case ObjectExt.DATETIME_TYPE:
					{
						String strValue=(String)objValue;
						if(strValue.length()==0)
						{
							ps.setNull(i+1,java.sql.Types.TIMESTAMP);
						}
						else
						{
							DateTime datetime=new DateTime((String)objValue);
							ps.setTimestamp(i+1,datetime.getTimestamp());
						}
						break;
					}
					case ObjectExt.BOOLEAN_TYPE:
					{
						ps.setBoolean(i+1,(Boolean)objValue);
						break;
					}

					case ObjectExt.BYTE_ARRAY_TYPE:
					{
						ps.setBytes(i+1,(byte[])objValue);
						break;
					}
					default:
					{
						throw new SQLException("Unsupported type's object "+objValue);
					}
				}
			}
			
			onExecuteSQL(anaSQL.strSQL, anaSQL.alParaName, cdoRequest);
		}
		catch(SQLException e)
		{
			closeStatement(strSourceSQL,ps);
			throw e;
		}

		return ps;
	}
	
	public void onExecuteSQL(String strSQL,ArrayList<String> alParaName,CDO cdoRequest){
		if (log.isDebugEnabled()) {
			StringBuilder sb = new StringBuilder("\n{");
			for (int i = 0; i <alParaName.size(); i++) {
				ObjectExt object = cdoRequest.getObject(alParaName
						.get(i));
				Object objValue = object.getValue();
				int nType = object.getType();
				sb.append(nType==ObjectExt.BYTE_ARRAY_TYPE?new String((byte[]) objValue):objValue);
				sb.append(',');
			}
			sb.append('}');
			log.debug(strSQL + sb.toString());
		}
	}

	
	public CDO readRecord(ResultSet rs,String[] strsFieldName,int[] naFieldType,int[] nsPrecision,int[] nsScale) throws SQLException,IOException
	{
		CDO cdoRecord=new CDO();

		if(readRecord(rs,strsFieldName,naFieldType,nsPrecision,nsScale,cdoRecord)==0)
		{
			return null;
		}
		
		return cdoRecord;
	}

	/**
	 * ???????????????????????????
	 * 
	 * @param rs
	 */
	public int readRecord(ResultSet rs,String[] strsFieldName,int[] naFieldType,int[] nsPrecision,int[] nsScale,CDO cdoRecord) throws SQLException,IOException
	{
		if(rs.next()==false)
		{
			return 0;
		}
		
		for(int i=0;i<strsFieldName.length;i++)
		{
			String strFieldName=strsFieldName[i];
			
			try
			{
				if(rs.getObject(i+1)==null)
				{
					continue;
				}
			}catch(Exception e)
			{//?????????????????????,getObject????????????????????????,??????????????????,?????????driver???????????????????????????,????????????????????????,?????????????????????
				continue;				
			}

			int nFieldType=naFieldType[i];
			switch(nFieldType)
			{
				case Types.BIT:
				{
					byte byValue=rs.getByte(i+1);
					if(byValue==0)
					{
						cdoRecord.setBooleanValue(strFieldName,false);
					}
					else
					{
						cdoRecord.setBooleanValue(strFieldName,true);
					}
					break;
				}
				case Types.TINYINT:
				{
					cdoRecord.setByteValue(strFieldName,rs.getByte(i+1));
					break;
				}
				case Types.SMALLINT:
				{
					cdoRecord.setShortValue(strFieldName,rs.getShort(i+1));
					break;
				}
				case Types.INTEGER:
				{
					cdoRecord.setIntegerValue(strFieldName,rs.getInt(i+1));
					break;
				}
				case Types.BIGINT:
				{
					cdoRecord.setLongValue(strFieldName,rs.getLong(i+1));
					break;
				}
				case Types.REAL:
				{
					cdoRecord.setFloatValue(strFieldName,rs.getFloat(i+1));
					break;
				}
				case Types.DOUBLE:
				case Types.FLOAT:
				{
					cdoRecord.setDoubleValue(strFieldName,rs.getDouble(i+1));
					break;
				}
				case Types.DECIMAL:
				case Types.NUMERIC:
				{
					if(nsScale[i]==0)
					{// ??????
						if(nsPrecision[i]<3)
						{
							cdoRecord.setByteValue(strFieldName,rs.getByte(i+1));
						}
						else if(nsPrecision[i]<5)
						{
							cdoRecord.setShortValue(strFieldName,rs.getShort(i+1));
						}
						else if(nsPrecision[i]<10)
						{
							cdoRecord.setIntegerValue(strFieldName,rs.getInt(i+1));
						}
						else if(nsPrecision[i]<20)
						{
							cdoRecord.setLongValue(strFieldName,rs.getLong(i+1));
						}
						else
						{
							cdoRecord.setDoubleValue(strFieldName,rs.getDouble(i+1));
						}
					}
					else
					{// ??????
						cdoRecord.setDoubleValue(strFieldName,rs.getDouble(i+1));
					}
					break;
				}
				case Types.VARCHAR:
				case Types.LONGVARCHAR:
				case Types.CHAR:
				{
					String strValue=rs.getString(i+1);
					strValue=Utility.encodingText(strValue,strCharset,strSystemCharset);

					cdoRecord.setStringValue(strFieldName,strValue);
					break;
				}
				case Types.CLOB:
				{
					String strValue="";
					Clob clobValue=rs.getClob(i+1);
					char[] chsValue=new char[(int)clobValue.length()];
					clobValue.getCharacterStream().read(chsValue);
					strValue=new String(chsValue);
					strValue=Utility.encodingText(strValue,strCharset,strSystemCharset);
					cdoRecord.setStringValue(strFieldName,strValue.trim());
					break;
				}
				case Types.DATE:
				case Types.TIME:
				case Types.TIMESTAMP:
				{
					try
					{
						String strValue="";
						DateTime dtValue=new DateTime(rs.getTimestamp(i+1));
						strValue=dtValue.toString("yyyy-MM-dd HH:mm:ss");
						cdoRecord.setDateTimeValue(strFieldName,strValue);						
					}
					catch(Exception e)
					{						
					}
					
					break;
				}
				case Types.BINARY:
				case Types.VARBINARY:	
				case Types.LONGVARBINARY:
				{
					byte[] bysValue=null;
					InputStream stream=null;
					try
					{
						stream=rs.getBinaryStream(i+1);
						bysValue=new byte[stream.available()];
						stream.read(bysValue);
					}
					finally
					{
						Utility.closeStream(stream);
					}

					cdoRecord.setByteArrayValue(strFieldName,bysValue);
					break;
				}
			
				case Types.BLOB:
				{
					byte[] bysValue=null;
					Blob blobValue=rs.getBlob(i+1);
					bysValue=blobValue.getBytes(1,(int)blobValue.length());
					cdoRecord.setByteArrayValue(strFieldName,bysValue);
					break;
				}
				default:
					throw new SQLException("Unsupported sql data type "+nFieldType+" on field "+strFieldName);
			}
		}
		return 1;
	}

//	public Object[] readRecord(ResultSet rs,int[] naFieldType,int[] nsPrecision,int[] nsScale) throws SQLException,IOException
//	{
//		Object[] objsRecord=new Object[naFieldType.length];
//
//		if(readRecord(rs,naFieldType,nsPrecision,nsScale,objsRecord)==0)
//		{
//			return null;
//		}
//		
//		return objsRecord;
//	}

//	/**
//	 * ???????????????????????????
//	 * 
//	 * @param rs
//	 */
//	public int readRecord(ResultSet rs,int[] naFieldType,int[] nsPrecision,int[] nsScale,Object[] objsRecord) throws SQLException,IOException
//	{
//		if(rs.next()==false)
//		{
//			return 0;
//		}
//		
//		for(int i=0;i<naFieldType.length;i++)
//		{
//			Object obj=rs.getObject(i);
//			if(obj==null)
//			{
//				continue;
//			}
//
//			int nFieldType=naFieldType[i];
//			switch(nFieldType)
//			{
//				case Types.BIT:
//				{
//					byte byValue=rs.getByte(i);
//					if(byValue==0)
//					{
//						obj=new Boolean(false);
//					}
//					else
//					{
//						obj=new Boolean(true);
//					}
//					objsRecord[i]=obj;
//					
//					break;
//				}
//				case Types.TINYINT:
//				case Types.SMALLINT:
//				case Types.INTEGER:
//				case Types.BIGINT:
//				{
//					objsRecord[i]=obj;
//					break;
//				}
//				case Types.REAL:
//				{
//					objsRecord[i]=rs.getFloat(i);
//					break;
//				}
//				case Types.DOUBLE:
//				case Types.FLOAT:
//				{
//					objsRecord[i]=rs.getDouble(i);
//					break;
//				}
//				case Types.DECIMAL:
//				case Types.NUMERIC:
//				{
//					if(nsScale[i]==0)
//					{// ??????
//						if(nsPrecision[i]<3)
//						{
//							objsRecord[i]=rs.getByte(i);
//						}
//						else if(nsPrecision[i]<5)
//						{
//							objsRecord[i]=rs.getShort(i);
//						}
//						else if(nsPrecision[i]<10)
//						{
//							objsRecord[i]=rs.getInt(i);
//						}
//						else if(nsPrecision[i]<20)
//						{
//							objsRecord[i]=rs.getLong(i);
//						}
//						else
//						{
//							objsRecord[i]=rs.getDouble(i);
//						}
//					}
//					else
//					{// ??????
//						objsRecord[i]=rs.getDouble(i);
//					}
//					break;
//				}
//				case Types.VARCHAR:
//				case Types.LONGVARCHAR:
//				case Types.CHAR:
//				{
//					String strValue=rs.getString(i);
//					strValue=Utility.encodingText(strValue,strCharset,strSystemCharset);
//
//					objsRecord[i]=strValue;
//					break;
//				}
//				case Types.CLOB:
//				{
//					String strValue="";
//					Clob clobValue=rs.getClob(i);
//					char[] chsValue=new char[(int)clobValue.length()];
//					clobValue.getCharacterStream().read(chsValue);
//					strValue=new String(chsValue);
//					strValue=Utility.encodingText(strValue,strCharset,strSystemCharset);
//					objsRecord[i]=strValue.trim();
//					break;
//				}
//				case Types.DATE:
//				case Types.TIME:
//				case Types.TIMESTAMP:
//				{
//					String strValue="";
//					DateTime dtValue=new DateTime(rs.getTimestamp(i));
//					try
//					{
//						strValue=dtValue.toString("yyyy-MM-dd HH:mm:ss");
//					}
//					catch(Exception e)
//					{
//					}
//					objsRecord[i]=strValue;
//					break;
//				}
//				case Types.BINARY:
//				case Types.LONGVARBINARY:
//				{
//					byte[] bysValue=null;
//					InputStream stream=null;
//					try
//					{
//						stream=rs.getBinaryStream(i);
//						bysValue=new byte[stream.available()];
//						stream.read(bysValue);
//					}
//					finally
//					{
//						Utility.closeStream(stream);
//					}
//
//					objsRecord[i]=bysValue;
//					break;
//				}
//				case Types.BLOB:
//				{
//					byte[] bysValue=null;
//					Blob blobValue=rs.getBlob(i);
//					bysValue=blobValue.getBytes(1,(int)blobValue.length());
//					objsRecord[i]=bysValue;
//					break;
//				}
//				default:
//					throw new SQLException("Unsupported sql data type "+nFieldType);
//			}
//		}
//		return 1;
//	}

	// ??????{???}?????????????????????FieldId
	protected boolean handleFieldIdText(String strFieldIdText,StringBuilder strbOutput)
	{
		// modified at 2006-12-21
		if(strFieldIdText==null||strFieldIdText.length()==0)
		{
			return false;
		}
		strbOutput.setLength(0);

		char chFirst=strFieldIdText.charAt(0);
		int nIndex=0;
		int nLength=strFieldIdText.length();
		while(true)
		{
			if(nIndex>=nLength)
			{
				break;
			}

			char ch=strFieldIdText.charAt(nIndex);
			if(ch=='{'||ch=='}')
			{
				if(nIndex==nLength-1)
				{
					break;
				}
				if(strFieldIdText.charAt(nIndex+1)==ch)
				{
					strbOutput.append(ch);
				}
			}
			else
			{
				strbOutput.append(ch);
			}
			nIndex++;
		}

		if(chFirst=='{'&&strbOutput.charAt(0)!='{')
		{// ???FieldId
			return true;
		}
		else
		{// ???????????????
			return false;
		}
	}

	/**
	 * ??????FieldIdText?????????
	 * 
	 * @param strFieldIdText
	 * @param cdoRequest
	 * @return
	 * @throws Exception
	 */
	protected byte getByteValue(String strFieldIdText,CDO cdoRequest)
	{
		// ??????strFieldIdText,??????????????????FieldId
		StringBuilder strbFieldIdText=new StringBuilder();
		boolean bIsFieldId=handleFieldIdText(strFieldIdText,strbFieldIdText);

		if(bIsFieldId==false)
		{
			return Byte.parseByte(strbFieldIdText.toString());
		}
		else
		{
			return cdoRequest.getByteValue(strbFieldIdText.toString());
		}
	}

	/**
	 * ??????FieldIdText?????????
	 * 
	 * @param strFieldIdText
	 * @param cdoRequest
	 * @return
	 * @throws Exception
	 */
	protected short getShortValue(String strFieldIdText,CDO cdoRequest)
	{
		// ??????strFieldIdText,??????????????????FieldId
		StringBuilder strbFieldIdText=new StringBuilder();
		boolean bIsFieldId=handleFieldIdText(strFieldIdText,strbFieldIdText);

		if(bIsFieldId==false)
		{
			return Short.parseShort(strbFieldIdText.toString());
		}
		else
		{
			return cdoRequest.getShortValue(strbFieldIdText.toString());
		}
	}

	/**
	 * ??????FieldIdText?????????
	 * 
	 * @param strFieldIdText
	 * @param cdoRequest
	 * @return
	 * @throws Exception
	 */
	protected int getIntegerValue(String strFieldIdText,CDO cdoRequest)
	{
		// ??????strFieldIdText,??????????????????FieldId
		StringBuilder strbFieldIdText=new StringBuilder();
		boolean bIsFieldId=handleFieldIdText(strFieldIdText,strbFieldIdText);

		if(bIsFieldId==false)
		{
			return Integer.parseInt(strbFieldIdText.toString());
		}
		else
		{
			return cdoRequest.getIntegerValue(strbFieldIdText.toString());
		}
	}

	/**
	 * ??????FieldIdText?????????
	 * 
	 * @param strFieldIdText
	 * @param cdoRequest
	 * @return
	 * @throws Exception
	 */
	protected float getFloatValue(String strFieldIdText,CDO cdoRequest)
	{
		// ??????strFieldIdText,??????????????????FieldId
		StringBuilder strbFieldIdText=new StringBuilder();
		boolean bIsFieldId=handleFieldIdText(strFieldIdText,strbFieldIdText);

		if(bIsFieldId==false)
		{
			return Float.parseFloat(strbFieldIdText.toString());
		}
		else
		{
			return cdoRequest.getFloatValue(strbFieldIdText.toString());
		}
	}

	/**
	 * ??????FieldIdText?????????
	 * 
	 * @param strFieldIdText
	 * @param cdoRequest
	 * @return
	 * @throws Exception
	 */
	protected double getDoubleValue(String strFieldIdText,CDO cdoRequest)
	{
		// ??????strFieldIdText,??????????????????FieldId
		StringBuilder strbFieldIdText=new StringBuilder();
		boolean bIsFieldId=handleFieldIdText(strFieldIdText,strbFieldIdText);

		if(bIsFieldId==false)
		{
			return Double.parseDouble(strbFieldIdText.toString());
		}
		else
		{
			return cdoRequest.getDoubleValue(strbFieldIdText.toString());
		}
	}
	
	/**
	 * ??????FieldIdText?????????
	 * 
	 * @param strFieldIdText
	 * @param cdoRequest
	 * @return
	 * @throws Exception
	 */
	protected boolean getBooleanValue(String strFieldIdText,CDO cdoRequest)
	{
		// ??????strFieldIdText,??????????????????FieldId
		StringBuilder strbFieldIdText=new StringBuilder();
		boolean bIsFieldId=handleFieldIdText(strFieldIdText,strbFieldIdText);

		if(bIsFieldId==false)
		{
			return Boolean.parseBoolean(strbFieldIdText.toString());
		}
		else
		{
			return cdoRequest.getBooleanValue(strbFieldIdText.toString());
		}
	}

	/**
	 * ??????FieldIdText?????????
	 * 
	 * @param strFieldIdText
	 * @param cdoRequest
	 * @return
	 * @throws Exception
	 */
	protected long getLongValue(String strFieldIdText,CDO cdoRequest)
	{
		// ??????strFieldIdText,??????????????????FieldId
		StringBuilder strbFieldIdText=new StringBuilder();
		boolean bIsFieldId=handleFieldIdText(strFieldIdText,strbFieldIdText);

		if(bIsFieldId==false)
		{
			return Long.parseLong(strbFieldIdText.toString());
		}
		else
		{
			return cdoRequest.getLongValue(strbFieldIdText.toString());
		}
	}

	/**
	 * ??????FieldIdText?????????
	 * 
	 * @param strFieldIdText
	 * @param cdoRequest
	 * @return
	 * @throws Exception
	 */
	protected String getStringValue(String strFieldIdText,CDO cdoRequest)
	{
		// ??????strFieldIdText,??????????????????FieldId
		StringBuilder strbFieldIdText=new StringBuilder();
		boolean bIsFieldId=handleFieldIdText(strFieldIdText,strbFieldIdText);

		if(bIsFieldId==false)
		{
			return strbFieldIdText.toString();
		}
		else
		{
			return cdoRequest.getStringValue(strbFieldIdText.toString());
		}
	}

	/**
	 * ??????FieldIdText?????????
	 * 
	 * @param strFieldIdText
	 * @param cdoRequest
	 * @return
	 * @throws Exception
	 */
	protected String getDateValue(String strFieldIdText,CDO cdoRequest)
	{
		// ??????strFieldIdText,??????????????????FieldId
		StringBuilder strbFieldIdText=new StringBuilder();
		boolean bIsFieldId=handleFieldIdText(strFieldIdText,strbFieldIdText);

		if(bIsFieldId==false)
		{
			return strbFieldIdText.toString();
		}
		else
		{
			return cdoRequest.getDateValue(strbFieldIdText.toString());
		}
	}

	/**
	 * ??????FieldIdText?????????
	 * 
	 * @param strFieldIdText
	 * @param cdoRequest
	 * @return
	 * @throws Exception
	 */
	protected String getTimeValue(String strFieldIdText,CDO cdoRequest)
	{
		// ??????strFieldIdText,??????????????????FieldId
		StringBuilder strbFieldIdText=new StringBuilder();
		boolean bIsFieldId=handleFieldIdText(strFieldIdText,strbFieldIdText);

		if(bIsFieldId==false)
		{
			return strbFieldIdText.toString();
		}
		else
		{
			return cdoRequest.getTimeValue(strbFieldIdText.toString());
		}
	}

	/**
	 * ??????FieldIdText?????????
	 * 
	 * @param strFieldIdText
	 * @param cdoRequest
	 * @return
	 * @throws Exception
	 */
	protected String getDateTimeValue(String strFieldIdText,CDO cdoRequest)
	{
		// ??????strFieldIdText,??????????????????FieldId
		StringBuilder strbFieldIdText=new StringBuilder();
		boolean bIsFieldId=handleFieldIdText(strFieldIdText,strbFieldIdText);

		if(bIsFieldId==false)
		{
			return strbFieldIdText.toString();
		}
		else
		{
			return cdoRequest.getDateTimeValue(strbFieldIdText.toString());
		}
	}

	protected Object getFieldValue(String strFieldId,CDO cdoRequest)
	{
		if(strFieldId.indexOf('{')>=0)
		{
			strFieldId=getFieldId(strFieldId,cdoRequest);
		}
		return cdoRequest.getObjectValue(strFieldId);
	}

	/**
	 * ??????FieldId text??????FieldId???????????????
	 * 
	 * @param strFieldIdText
	 * @return
	 */
	protected String getFieldId(String strFieldIdText,CDO cdoRequest)
	{
		int nStartIndex=strFieldIdText.indexOf('{');
		if(nStartIndex<0)
		{
			return strFieldIdText;
		}

		// ??????{}
		int nEndIndex=Utility.findMatchedChar(nStartIndex,strFieldIdText);
		String strSubFieldId=strFieldIdText.substring(nStartIndex+1,nEndIndex);
		String strSubFieldValue=getFieldValue(strSubFieldId,cdoRequest).toString();

		return getFieldId(strFieldIdText.substring(0,nStartIndex)+strSubFieldValue
						+strFieldIdText.substring(nEndIndex+1),cdoRequest);
	}

	protected void setVar(SetVar sv,CDO cdoRequest)
	{
		String strVarId=sv.getVarId();
		String strFieldId=strVarId.substring(1,strVarId.length()-1);
		switch(sv.getType().getType())
		{
			case SetVarTypeType.BYTE_TYPE:
				cdoRequest.setByteValue(strFieldId,Byte.parseByte(sv.getValue()));
				break;
			case SetVarTypeType.SHORT_TYPE:
				cdoRequest.setShortValue(strFieldId,Short.parseShort(sv.getValue()));
				break;
			case SetVarTypeType.INTEGER_TYPE:
				cdoRequest.setIntegerValue(strFieldId,Integer.parseInt(sv.getValue()));
				break;
			case SetVarTypeType.LONG_TYPE:
				cdoRequest.setLongValue(strFieldId,Long.parseLong(sv.getValue()));
				break;
			case SetVarTypeType.FLOAT_TYPE:
				cdoRequest.setFloatValue(strFieldId,Float.parseFloat(sv.getValue()));
				break;
			case SetVarTypeType.DOUBLE_TYPE:
				cdoRequest.setDoubleValue(strFieldId,Double.parseDouble(sv.getValue()));
				break;
			case SetVarTypeType.STRING_TYPE:
				cdoRequest.setStringValue(strFieldId,sv.getValue());
				break;
			case SetVarTypeType.DATE_TYPE:
				cdoRequest.setDateValue(strFieldId,sv.getValue());
				break;
			case SetVarTypeType.TIME_TYPE:
				cdoRequest.setTimeValue(strFieldId,sv.getValue());
				break;
			case SetVarTypeType.DATETIME_TYPE:
				cdoRequest.setDateTimeValue(strFieldId,sv.getValue());
				break;
			default:
				throw new RuntimeException("Invalid type "+sv.getType().toString());
		}
	}

	/**
	 * ??????If?????????
	 * 
	 * @param strValue1
	 * @param strOperator
	 * @param strValue2
	 * @param strType
	 * @param cdoRequest
	 * @return
	 * @throws Exception
	 */
	protected boolean checkCondition(String strValue1,String strOperator,String strValue2,int nType,String strType,
					CDO cdoRequest)
	{
		// IS
		if(strOperator.equalsIgnoreCase("IS")==true)
		{
			// ??????FieldId
			StringBuilder strbText1=new StringBuilder();
			StringBuilder strbText2=new StringBuilder();
			boolean bIsFieldId1=handleFieldIdText(strValue1,strbText1);
			boolean bIsFieldId2=handleFieldIdText(strValue2,strbText2);

			if(bIsFieldId1&&!bIsFieldId2)
			{// Value1???FieldId
				if(cdoRequest.exists(strbText1.toString()))
				{
					return false;
				}
				else
				{
					return true;
				}
			}
			else if(bIsFieldId2&&!bIsFieldId1)
			{// Value2???FieldId
				if(cdoRequest.exists(strbText2.toString()))
				{
					return false;
				}
				else
				{
					return true;
				}
			}
			
			throw new RuntimeException("Invalid IF condition");
		}

		// ISNOT
		if(strOperator.equalsIgnoreCase("ISNOT")==true)
		{
			// ??????FieldId
			StringBuilder strbText1=new StringBuilder();
			StringBuilder strbText2=new StringBuilder();
			boolean bIsFieldId1=handleFieldIdText(strValue1,strbText1);
			boolean bIsFieldId2=handleFieldIdText(strValue2,strbText2);

			if(bIsFieldId1&&!bIsFieldId2)
			{// Value1???FieldId
				if(cdoRequest.exists(strbText1.toString()))
				{
					return true;
				}
				else
				{// Value2???FieldId
					return false;
				}
			}
			else if(bIsFieldId2&&!bIsFieldId1)
			{
				if(cdoRequest.exists(strbText2.toString()))
				{
					return true;
				}
				else
				{
					return false;
				}
			}

			throw new RuntimeException("Invalid IF condition");
		}

		// =
		if(strOperator.equalsIgnoreCase("="))
		{
			switch(nType)
			{
				case SQLIfTypeType.INTEGER_TYPE:
				{
					int value1=getIntegerValue(strValue1,cdoRequest);
					int value2=getIntegerValue(strValue2,cdoRequest);
					return value1==value2;
				}
				case SQLIfTypeType.STRING_TYPE:
				{
					String value1=getStringValue(strValue1,cdoRequest);
					String value2=getStringValue(strValue2,cdoRequest);
					return value1.equals(value2);
				}
				case SQLIfTypeType.LONG_TYPE:
				{
					long value1=getLongValue(strValue1,cdoRequest);
					long value2=getLongValue(strValue2,cdoRequest);
					return value1==value2;
				}
				case SQLIfTypeType.BYTE_TYPE:
				{
					byte value1=getByteValue(strValue1,cdoRequest);
					byte value2=getByteValue(strValue2,cdoRequest);
					return value1==value2;
				}
				case SQLIfTypeType.SHORT_TYPE:
				{
					short value1=getShortValue(strValue1,cdoRequest);
					short value2=getShortValue(strValue2,cdoRequest);
					return value1==value2;
				}
				case SQLIfTypeType.DATE_TYPE:
				{
					String value1=getDateValue(strValue1,cdoRequest);
					String value2=getDateValue(strValue2,cdoRequest);
					return value1.equals(value2);
				}
				case SQLIfTypeType.TIME_TYPE:
				{
					String value1=getTimeValue(strValue1,cdoRequest);
					String value2=getTimeValue(strValue2,cdoRequest);
					return value1.equals(value2);
				}
				case SQLIfTypeType.DATETIME_TYPE:
				{
					String value1=getDateTimeValue(strValue1,cdoRequest);
					String value2=getDateTimeValue(strValue2,cdoRequest);
					return value1.equals(value2);
				}
				case SQLIfTypeType.FLOAT_TYPE:
				{
					float value1=getFloatValue(strValue1,cdoRequest);
					float value2=getFloatValue(strValue2,cdoRequest);
					return value1==value2;
				}
				case SQLIfTypeType.DOUBLE_TYPE:
				{
					double value1=getDoubleValue(strValue1,cdoRequest);
					double value2=getDoubleValue(strValue2,cdoRequest);
					return value1==value2;
				}
				case SQLIfTypeType.BOOLEAN_TYPE:
				{
					boolean value1=getBooleanValue(strValue1,cdoRequest);
					boolean value2=getBooleanValue(strValue2,cdoRequest);
					return value1==value2;
				}
				default:
				{
					throw new RuntimeException("Invalid type"+strType);
				}
			}
		}
		else if(strOperator.equalsIgnoreCase("!="))
		{
			switch(nType)
			{
				case SQLIfTypeType.INTEGER_TYPE:
				{
					int value1=getIntegerValue(strValue1,cdoRequest);
					int value2=getIntegerValue(strValue2,cdoRequest);
					return value1!=value2;
				}
				case SQLIfTypeType.STRING_TYPE:
				{
					String value1=getStringValue(strValue1,cdoRequest);
					String value2=getStringValue(strValue2,cdoRequest);
					return !value1.equals(value2);
				}
				case SQLIfTypeType.LONG_TYPE:
				{
					long value1=getLongValue(strValue1,cdoRequest);
					long value2=getLongValue(strValue2,cdoRequest);
					return value1!=value2;
				}
				case SQLIfTypeType.BYTE_TYPE:
				{
					byte value1=getByteValue(strValue1,cdoRequest);
					byte value2=getByteValue(strValue2,cdoRequest);
					return value1!=value2;
				}
				case SQLIfTypeType.SHORT_TYPE:
				{
					short value1=getShortValue(strValue1,cdoRequest);
					short value2=getShortValue(strValue2,cdoRequest);
					return value1!=value2;
				}
				case SQLIfTypeType.DATE_TYPE:
				{
					String value1=getDateValue(strValue1,cdoRequest);
					String value2=getDateValue(strValue2,cdoRequest);
					return !value1.equals(value2);
				}
				case SQLIfTypeType.TIME_TYPE:
				{
					String value1=getTimeValue(strValue1,cdoRequest);
					String value2=getTimeValue(strValue2,cdoRequest);
					return !value1.equals(value2);
				}
				case SQLIfTypeType.DATETIME_TYPE:
				{
					String value1=getDateTimeValue(strValue1,cdoRequest);
					String value2=getDateTimeValue(strValue2,cdoRequest);
					return !value1.equals(value2);
				}
				case SQLIfTypeType.FLOAT_TYPE:
				{
					float value1=getFloatValue(strValue1,cdoRequest);
					float value2=getFloatValue(strValue2,cdoRequest);
					return value1!=value2;
				}
				case SQLIfTypeType.DOUBLE_TYPE:
				{
					double value1=getDoubleValue(strValue1,cdoRequest);
					double value2=getDoubleValue(strValue2,cdoRequest);
					return value1!=value2;
				}
				case SQLIfTypeType.BOOLEAN_TYPE:
				{
					boolean value1=getBooleanValue(strValue1,cdoRequest);
					boolean value2=getBooleanValue(strValue2,cdoRequest);
					return value1!=value2;
				}				
				default:
				{
					throw new RuntimeException("Invalid type "+strType);
				}
			}
		}
		else if(strOperator.equalsIgnoreCase(">"))
		{
			switch(nType)
			{
				case SQLIfTypeType.INTEGER_TYPE:
				{
					int value1=getIntegerValue(strValue1,cdoRequest);
					int value2=getIntegerValue(strValue2,cdoRequest);
					return value1>value2;
				}
				case SQLIfTypeType.STRING_TYPE:
				{
					String value1=getStringValue(strValue1,cdoRequest);
					String value2=getStringValue(strValue2,cdoRequest);
					return value1.compareTo(value2)>0;
				}
				case SQLIfTypeType.LONG_TYPE:
				{
					long value1=getLongValue(strValue1,cdoRequest);
					long value2=getLongValue(strValue2,cdoRequest);
					return value1>value2;
				}
				case SQLIfTypeType.BYTE_TYPE:
				{
					byte value1=getByteValue(strValue1,cdoRequest);
					byte value2=getByteValue(strValue2,cdoRequest);
					return value1>value2;
				}
				case SQLIfTypeType.SHORT_TYPE:
				{
					short value1=getShortValue(strValue1,cdoRequest);
					short value2=getShortValue(strValue2,cdoRequest);
					return value1>value2;
				}
				case SQLIfTypeType.DATE_TYPE:
				{
					String value1=getDateValue(strValue1,cdoRequest);
					String value2=getDateValue(strValue2,cdoRequest);
					return value1.compareTo(value2)>0;
				}
				case SQLIfTypeType.TIME_TYPE:
				{
					String value1=getTimeValue(strValue1,cdoRequest);
					String value2=getTimeValue(strValue2,cdoRequest);
					return value1.compareTo(value2)>0;
				}
				case SQLIfTypeType.DATETIME_TYPE:
				{
					String value1=getDateTimeValue(strValue1,cdoRequest);
					String value2=getDateTimeValue(strValue2,cdoRequest);
					return value1.compareTo(value2)>0;
				}
				case SQLIfTypeType.FLOAT_TYPE:
				{
					float value1=getFloatValue(strValue1,cdoRequest);
					float value2=getFloatValue(strValue2,cdoRequest);
					return value1>value2;
				}
				case SQLIfTypeType.DOUBLE_TYPE:
				{
					double value1=getDoubleValue(strValue1,cdoRequest);
					double value2=getDoubleValue(strValue2,cdoRequest);
					return value1>value2;
				}
				default:
				{
					throw new RuntimeException("Invalid type "+strType);
				}
			}
		}
		else if(strOperator.equalsIgnoreCase("<"))
		{
			switch(nType)
			{
				case SQLIfTypeType.INTEGER_TYPE:
				{
					int value1=getIntegerValue(strValue1,cdoRequest);
					int value2=getIntegerValue(strValue2,cdoRequest);
					return value1<value2;
				}
				case SQLIfTypeType.STRING_TYPE:
				{
					String value1=getStringValue(strValue1,cdoRequest);
					String value2=getStringValue(strValue2,cdoRequest);
					return value1.compareTo(value2)<0;
				}
				case SQLIfTypeType.LONG_TYPE:
				{
					long value1=getLongValue(strValue1,cdoRequest);
					long value2=getLongValue(strValue2,cdoRequest);
					return value1<value2;
				}
				case SQLIfTypeType.BYTE_TYPE:
				{
					byte value1=getByteValue(strValue1,cdoRequest);
					byte value2=getByteValue(strValue2,cdoRequest);
					return value1<value2;
				}
				case SQLIfTypeType.SHORT_TYPE:
				{
					short value1=getShortValue(strValue1,cdoRequest);
					short value2=getShortValue(strValue2,cdoRequest);
					return value1<value2;
				}
				case SQLIfTypeType.DATE_TYPE:
				{
					String value1=getDateValue(strValue1,cdoRequest);
					String value2=getDateValue(strValue2,cdoRequest);
					return value1.compareTo(value2)<0;
				}
				case SQLIfTypeType.TIME_TYPE:
				{
					String value1=getTimeValue(strValue1,cdoRequest);
					String value2=getTimeValue(strValue2,cdoRequest);
					return value1.compareTo(value2)<0;
				}
				case SQLIfTypeType.DATETIME_TYPE:
				{
					String value1=getDateTimeValue(strValue1,cdoRequest);
					String value2=getDateTimeValue(strValue2,cdoRequest);
					return value1.compareTo(value2)<0;
				}
				case SQLIfTypeType.FLOAT_TYPE:
				{
					float value1=getFloatValue(strValue1,cdoRequest);
					float value2=getFloatValue(strValue2,cdoRequest);
					return value1<value2;
				}
				case SQLIfTypeType.DOUBLE_TYPE:
				{
					double value1=getDoubleValue(strValue1,cdoRequest);
					double value2=getDoubleValue(strValue2,cdoRequest);
					return value1<value2;
				}
				default:
				{
					throw new RuntimeException("Invalid type "+strType);
				}
			}
		}
		else if(strOperator.equalsIgnoreCase(">="))
		{
			switch(nType)
			{
				case SQLIfTypeType.INTEGER_TYPE:
				{
					int value1=getIntegerValue(strValue1,cdoRequest);
					int value2=getIntegerValue(strValue2,cdoRequest);
					return value1>=value2;
				}
				case SQLIfTypeType.STRING_TYPE:
				{
					String value1=getStringValue(strValue1,cdoRequest);
					String value2=getStringValue(strValue2,cdoRequest);
					return value1.compareTo(value2)>=0;
				}
				case SQLIfTypeType.LONG_TYPE:
				{
					long value1=getLongValue(strValue1,cdoRequest);
					long value2=getLongValue(strValue2,cdoRequest);
					return value1>=value2;
				}
				case SQLIfTypeType.BYTE_TYPE:
				{
					byte value1=getByteValue(strValue1,cdoRequest);
					byte value2=getByteValue(strValue2,cdoRequest);
					return value1>=value2;
				}
				case SQLIfTypeType.SHORT_TYPE:
				{
					short value1=getShortValue(strValue1,cdoRequest);
					short value2=getShortValue(strValue2,cdoRequest);
					return value1>=value2;
				}
				case SQLIfTypeType.DATE_TYPE:
				{
					String value1=getDateValue(strValue1,cdoRequest);
					String value2=getDateValue(strValue2,cdoRequest);
					return value1.compareTo(value2)>=0;
				}
				case SQLIfTypeType.TIME_TYPE:
				{
					String value1=getTimeValue(strValue1,cdoRequest);
					String value2=getTimeValue(strValue2,cdoRequest);
					return value1.compareTo(value2)>=0;
				}
				case SQLIfTypeType.DATETIME_TYPE:
				{
					String value1=getDateTimeValue(strValue1,cdoRequest);
					String value2=getDateTimeValue(strValue2,cdoRequest);
					return value1.compareTo(value2)>=0;
				}
				case SQLIfTypeType.FLOAT_TYPE:
				{
					float value1=getFloatValue(strValue1,cdoRequest);
					float value2=getFloatValue(strValue2,cdoRequest);
					return value1>=value2;
				}
				case SQLIfTypeType.DOUBLE_TYPE:
				{
					double value1=getDoubleValue(strValue1,cdoRequest);
					double value2=getDoubleValue(strValue2,cdoRequest);
					return value1>=value2;
				}
				default:
				{
					throw new RuntimeException("Invalid type "+strType);
				}
			}
		}
		else if(strOperator.equalsIgnoreCase("<="))
		{
			switch(nType)
			{
				case SQLIfTypeType.INTEGER_TYPE:
				{
					int value1=getIntegerValue(strValue1,cdoRequest);
					int value2=getIntegerValue(strValue2,cdoRequest);
					return value1<value2;
				}
				case SQLIfTypeType.STRING_TYPE:
				{
					String value1=getStringValue(strValue1,cdoRequest);
					String value2=getStringValue(strValue2,cdoRequest);
					return value1.compareTo(value2)<=0;
				}
				case SQLIfTypeType.LONG_TYPE:
				{
					long value1=getLongValue(strValue1,cdoRequest);
					long value2=getLongValue(strValue2,cdoRequest);
					return value1<=value2;
				}
				case SQLIfTypeType.BYTE_TYPE:
				{
					byte value1=getByteValue(strValue1,cdoRequest);
					byte value2=getByteValue(strValue2,cdoRequest);
					return value1<=value2;
				}
				case SQLIfTypeType.SHORT_TYPE:
				{
					short value1=getShortValue(strValue1,cdoRequest);
					short value2=getShortValue(strValue2,cdoRequest);
					return value1<=value2;
				}
				case SQLIfTypeType.DATE_TYPE:
				{
					String value1=getDateValue(strValue1,cdoRequest);
					String value2=getDateValue(strValue2,cdoRequest);
					return value1.compareTo(value2)<=0;
				}
				case SQLIfTypeType.TIME_TYPE:
				{
					String value1=getTimeValue(strValue1,cdoRequest);
					String value2=getTimeValue(strValue2,cdoRequest);
					return value1.compareTo(value2)<=0;
				}
				case SQLIfTypeType.DATETIME_TYPE:
				{
					String value1=getDateTimeValue(strValue1,cdoRequest);
					String value2=getDateTimeValue(strValue2,cdoRequest);
					return value1.compareTo(value2)<=0;
				}
				case SQLIfTypeType.FLOAT_TYPE:
				{
					float value1=getFloatValue(strValue1,cdoRequest);
					float value2=getFloatValue(strValue2,cdoRequest);
					return value1<=value2;
				}
				case SQLIfTypeType.DOUBLE_TYPE:
				{
					double value1=getDoubleValue(strValue1,cdoRequest);
					double value2=getDoubleValue(strValue2,cdoRequest);
					return value1<=value2;
				}
				default:
				{
					throw new RuntimeException("Invalid type "+strType);
				}
			}
		}
		else if(strOperator.equalsIgnoreCase("MATCH"))
		{
			switch(nType)
			{
				case SQLIfTypeType.STRING_TYPE:
				{
					String value1=getTimeValue(strValue1,cdoRequest);
					String value2=getTimeValue(strValue2,cdoRequest);
					return value1.matches(value2);
				}
				default:
				{
					throw new RuntimeException("Invalid type "+strType);
				}
			}
		}
		else if(strOperator.equalsIgnoreCase("NOTMATCH"))
		{
			switch(nType)
			{
				case SQLIfTypeType.STRING_TYPE:
				{
					String value1=getTimeValue(strValue1,cdoRequest);
					String value2=getTimeValue(strValue2,cdoRequest);
					return !value1.matches(value2);
				}
				default:
				{
					throw new RuntimeException("Invalid type "+strType);
				}
			}
		}
		else
		{
			throw new RuntimeException("Invalid operator "+strOperator);
		}
	}

	/**
	 * ??????SQL????????????If??????
	 * 
	 * @param sqlIf
	 * @param cdoRequest
	 * @return 0-?????????????????????1-??????Break?????????2-??????Return??????
	 * @throws Exception
	 */
	protected int handleSQLIf(SQLIf sqlIf,CDO cdoRequest,StringBuilder strbSQL)
	{
		// ??????????????????
		boolean bCondition=checkCondition(sqlIf.getValue1(),sqlIf.getOperator().toString(),sqlIf.getValue2(),sqlIf
						.getType().getType(),sqlIf.getType().toString(),cdoRequest);
		if(bCondition==true)
		{// Handle Then
			SQLThen sqlThen=sqlIf.getSQLThen();
			return handleSQLBlock(sqlThen,cdoRequest,strbSQL);
		}
		else
		{// handle Else
			SQLElse sqlElse=sqlIf.getSQLElse();
			if(sqlElse==null)
			{// ????????????
				return 0;
			}
			return handleSQLBlock(sqlElse,cdoRequest,strbSQL);
		}
	}

	/**
	 * ??????SQL????????????For??????
	 * 
	 * @param sqlFor
	 * @param cdoRequest
	 * @param strbSQL
	 * @return 0-?????????????????????1-??????Break?????????2-??????Return??????
	 * @throws Exception
	 */
	protected int handleSQLFor(SQLFor sqlFor,CDO cdoRequest,StringBuilder strbSQL)
	{
		// ??????????????????
		int nFromIndex=this.getIntegerValue(sqlFor.getFromIndex(),cdoRequest);
		int nCount=this.getIntegerValue(sqlFor.getCount(),cdoRequest);
		String strIndexId=sqlFor.getIndexId();
		strIndexId=strIndexId.substring(1,strIndexId.length()-1);

		// ????????????
		for(int i=nFromIndex;i<nFromIndex+nCount;i++)
		{
			// ??????IndexId
			cdoRequest.setIntegerValue(strIndexId,i);

			// ??????Block
			int nResult=handleSQLBlock(sqlFor,cdoRequest,strbSQL);
			if(nResult==0)
			{// ??????????????????
				continue;
			}
			else if(nResult==1)
			{// ??????Break
				break;
			}
			else
			{// ??????Return
				return nResult;
			}
		}

		return 0;
	}

	/**
	 * ??????SQLBlock????????????????????????SQL??????
	 * 
	 * @param sqlBlock
	 * @return 0-?????????????????????1-??????Break?????????2-??????Return??????
	 */
	protected int handleSQLBlock(SQLBlockType sqlBlock,CDO cdoRequest,StringBuilder strbSQL)
	{
		// ??????????????????Item
		int nItemCount=sqlBlock.getSQLBlockTypeItemCount();
		for(int i=0;i<nItemCount;i++)
		{
			// ?????????Item??????????????????
//			if(i>0)
//			{
//				strbSQL.append(' ');
//			}

			// ???????????????Item
			SQLBlockTypeItem item=sqlBlock.getSQLBlockTypeItem(i);
			if(item.getOutputSQL()!=null)
			{// OutputSQL,?????????????????????
				strbSQL.append(item.getOutputSQL());
			}
			else if(item.getOutputField()!=null)
			{// OutputField?????????????????????????????????
				String strOutputFieldId=item.getOutputField();
				strOutputFieldId=strOutputFieldId.substring(1,strOutputFieldId.length()-1);
				strbSQL.append(cdoRequest.getStringValue(strOutputFieldId));
			}
			else if(item.getSQLIf()!=null)
			{// SQLIf
				int nResult=handleSQLIf(item.getSQLIf(),cdoRequest,strbSQL);
				if(nResult==0)
				{// ??????????????????
					continue;
				}
				else
				{// ??????Break???Return
					return nResult;
				}
			}
			else if(item.getSQLFor()!=null)
			{// SQLFor
				int nResult=handleSQLFor(item.getSQLFor(),cdoRequest,strbSQL);
				if(nResult==0)
				{// ??????????????????
					continue;
				}
				else
				{// ??????Break???Return
					return nResult;
				}
			}
			else
			{
				continue;
			}
		}

		// ??????????????????
		return 0;
	}

	/**
	 * ??????If??????
	 * 
	 * @param ifItem
	 * @param cdoRequest
	 * @return 0-?????????????????????1-??????Break?????????2-??????Return??????
	 * @throws Exception
	 */
	protected int handleIf(Connection conn,HashMap<String,SQLTrans> hmTrans,If ifItem,CDO cdoRequest,
					boolean bUseTransFlag,CDO cdoResponse,Return ret) throws SQLException,IOException
	{
		// ??????????????????
		boolean bCondition=checkCondition(ifItem.getValue1(),ifItem.getOperator().toString(),ifItem.getValue2(),ifItem
						.getType().getType(),ifItem.getType().toString(),cdoRequest);
		if(bCondition==true)
		{// Handle Then
			Then thenItem=ifItem.getThen();
			return handleBlock(conn,hmTrans,thenItem,cdoRequest,bUseTransFlag,cdoResponse,ret);
		}
		else
		{// handle Else
			Else elseItem=ifItem.getElse();
			if(elseItem==null)
			{// ??????else??????????????????????????????????????????
				return 0;
			}
			return handleBlock(conn,hmTrans,elseItem,cdoRequest,bUseTransFlag,cdoResponse,ret);
		}
	}

	/**
	 * ??????For??????
	 * 
	 * @param sqlFor
	 * @param cdoRequest
	 * @param strbSQL
	 * @return 0-?????????????????????1-??????Break?????????2-??????Return??????
	 * @throws Exception
	 */
	protected int handleFor(Connection conn,HashMap<String,SQLTrans> hmTrans,For forItem,CDO cdoRequest,
					boolean bUseTransFlag,CDO cdoResponse,Return ret) throws SQLException,IOException
	{
		// ??????????????????
		int nFromIndex=this.getIntegerValue(forItem.getFromIndex(),cdoRequest);
		int nCount=this.getIntegerValue(forItem.getCount(),cdoRequest);
		String strIndexId=forItem.getIndexId();
		strIndexId=strIndexId.substring(1,strIndexId.length()-1);

		// ????????????
		for(int i=nFromIndex;i<nFromIndex+nCount;i++)
		{
			// ??????IndexId
			cdoRequest.setIntegerValue(strIndexId,i);

			// ??????Block
			int nResult=handleBlock(conn,hmTrans,forItem,cdoRequest,bUseTransFlag,cdoResponse,ret);
			if(nResult==0)
			{// ??????????????????
				continue;
			}
			else if(nResult==1)
			{// ??????Break
				break;
			}
			else
			{// ??????Return
				return nResult;
			}
		}

		return 0;
	}

	protected void handleReturn(com.cdoframework.cdolib.database.dataservice.Return returnObject,CDO cdoRequest,CDO cdoResponse,Return ret) throws SQLException
	{
		int nReturnItemCount=returnObject.getReturnItemCount();
		for(int j=0;j<nReturnItemCount;j++)
		{
			String strFieldId=returnObject.getReturnItem(j).getFieldId();
			String strValueId=returnObject.getReturnItem(j).getValueId();
			strFieldId=strFieldId.substring(1,strFieldId.length()-1);
			strValueId=strValueId.substring(1,strValueId.length()-1);
			ObjectExt object=null;
			try
			{
				object=cdoRequest.getObject(strValueId);
				cdoResponse.setObjectExt(strFieldId,object);
			}
			catch(Exception e)
			{
				continue;
			}
		}
		ret.setCode(returnObject.getCode());
		ret.setInfo(returnObject.getInfo());
		ret.setText(returnObject.getText());
	}
	
	/*
	 * ????????????Block
	 * 
	 * @return 0-?????????????????????1-??????Break?????????2-??????Return??????
	 */
	protected int handleBlock(Connection conn,HashMap<String,SQLTrans> hmTrans,BlockType block,CDO cdoRequest,
					boolean bUseTransFlag,CDO cdoResponse,Return ret) throws SQLException,IOException
	{
		int nItemCount=block.getBlockTypeItemCount();
		for(int i=0;i<nItemCount;i++)
		{
			BlockTypeItem blockItem=block.getBlockTypeItem(i);
			if(blockItem.getInsert()!=null)
			{// Insert
				// ?????????????????????SQL
				Insert insert=(Insert)blockItem.getInsert();
				StringBuilder strbSQL=new StringBuilder();
				handleSQLBlock(insert,cdoRequest,strbSQL);
				String strSQL=strbSQL.toString();
				// ??????SQL
				this.executeUpdate(conn,strSQL,cdoRequest);
			}
			else if(blockItem.getUpdate()!=null)
			{// Update
				// ?????????????????????SQL
				Update update=(Update)blockItem.getUpdate();
				StringBuilder strbSQL=new StringBuilder();
				handleSQLBlock(update,cdoRequest,strbSQL);
				String strSQL=strbSQL.toString();

				// ??????SQL
				int nRecordCount=this.executeUpdate(conn,strSQL,cdoRequest);
				String strRecordCountId=update.getRecordCountId();
				if(strRecordCountId.length()>0)
				{// ???????????????????????????
					strRecordCountId=strRecordCountId.substring(1,strRecordCountId.length()-1);
					cdoRequest.setIntegerValue(strRecordCountId,nRecordCount);
				}
			}
			else if(blockItem.getDelete()!=null)
			{// Delete
				// ?????????????????????SQL
				Delete delete=(Delete)blockItem.getDelete();
				StringBuilder strbSQL=new StringBuilder();
				handleSQLBlock(delete,cdoRequest,strbSQL);
				String strSQL=strbSQL.toString();

				// ??????SQL
				int nRecordCount=this.executeUpdate(conn,strSQL,cdoRequest);
				String strRecordCountId=delete.getRecordCountId();
				if(strRecordCountId.length()>0)
				{// ???????????????????????????
					strRecordCountId=strRecordCountId.substring(1,strRecordCountId.length()-1);
					cdoRequest.setIntegerValue(strRecordCountId,nRecordCount);
				}
			}
			else if(blockItem.getSelectRecordSet()!=null)
			{// SelectRecordSet
				// ?????????????????????SQL
				SelectRecordSet selectRecordSet=(SelectRecordSet)blockItem.getSelectRecordSet();
				StringBuilder strbSQL=new StringBuilder();
				handleSQLBlock(selectRecordSet,cdoRequest,strbSQL);
				String strSQL=strbSQL.toString();

				// ??????SQL
				CDOArrayField cdoArrayField=new CDOArrayField("");
				String strRecordCountId=selectRecordSet.getRecordCountId();
				if(strRecordCountId.length()>0)
				{//????????? ??????SQL???????????????????????????					
					cdoRequest.setBooleanValue("$$nRecordCountId$$", true);
				}
				int nRecordCount=this.executeQueryRecordSet(conn,strSQL,cdoRequest,cdoArrayField);				
				if(strRecordCountId.length()>0)
				{// ???????????????????????????
					strRecordCountId=strRecordCountId.substring(1,strRecordCountId.length()-1);
					cdoRequest.setIntegerValue(strRecordCountId,nRecordCount);
				}

				String strOutputId=selectRecordSet.getOutputId();
				strOutputId=strOutputId.substring(1,strOutputId.length()-1);
				String strKeyFieldName=selectRecordSet.getKeyFieldName();
				if(strKeyFieldName.length()==0)
				{// RecordSet???????????????
					cdoRequest.setCDOArrayValue(strOutputId,cdoArrayField.getValue());
				}
				else
				{// RecordSet?????????HashMap
					CDO[] cdosRecordSet=cdoArrayField.getValue();
					CDO cdoRecordSet=new CDO();
					for(int j=0;j<cdosRecordSet.length;j++)
					{
						cdoRecordSet.setCDOValue(cdosRecordSet[j].getObjectValue(strKeyFieldName).toString(),
										cdosRecordSet[j]);
					}
					cdoRequest.setCDOValue(strOutputId,cdoRecordSet);
				}
			}
			else if(blockItem.getSelectRecord()!=null)
			{
				// ?????????????????????SQL
				SelectRecord selectRecord=(SelectRecord)blockItem.getSelectRecord();
				StringBuilder strbSQL=new StringBuilder();
				handleSQLBlock(selectRecord,cdoRequest,strbSQL);
				String strSQL=strbSQL.toString();

				// ??????SQL
				CDO cdo=new CDO();
				String strRecordCountId=selectRecord.getRecordCountId();
				if(strRecordCountId.length()>0)
				{//????????? ??????SQL???????????????????????????					
					cdoRequest.setBooleanValue("$$nRecordCountId$$", true);
				}
				int nRecordCount=this.executeQueryRecord(conn,strSQL,cdoRequest,cdo);				
				if(strRecordCountId.length()>0)
				{// ???????????????????????????
					strRecordCountId=strRecordCountId.substring(1,strRecordCountId.length()-1);
					cdoRequest.setIntegerValue(strRecordCountId,nRecordCount);
				}

				String strOutputId=selectRecord.getOutputId();
				strOutputId=strOutputId.substring(1,strOutputId.length()-1);
				cdoRequest.setCDOValue(strOutputId,cdo);
			}
			else if(blockItem.getSelectField()!=null)
			{
				// ?????????????????????SQL
				SelectField selectField=(SelectField)blockItem.getSelectField();
				StringBuilder strbSQL=new StringBuilder();
				handleSQLBlock(selectField,cdoRequest,strbSQL);
				String strSQL=strbSQL.toString();

				// ??????SQL
				ObjectExt objFieldValue=this.executeQueryFieldExt(conn,strSQL,cdoRequest);
				if(objFieldValue==null)
				{
					continue;
				}
				int nType=objFieldValue.getType();
				Object objValue=objFieldValue.getValue();

				String strOutputId=selectField.getOutputId();
				strOutputId=strOutputId.substring(1,strOutputId.length()-1);
				switch(nType)
				{
					case ObjectExt.BYTE_TYPE:
					{
						cdoRequest.setByteValue(selectField.getOutputId(),((Byte)objValue).byteValue());
						break;
					}
					case ObjectExt.SHORT_TYPE:
					{
						cdoRequest.setShortValue(strOutputId,((Short)objValue).shortValue());
						break;
					}
					case ObjectExt.INTEGER_TYPE:
					{
						cdoRequest.setIntegerValue(strOutputId,((Integer)objValue).intValue());
						break;
					}
					case ObjectExt.LONG_TYPE:
					{
						cdoRequest.setLongValue(strOutputId,((Long)objValue).longValue());
						break;
					}
					case ObjectExt.FLOAT_TYPE:
					{
						cdoRequest.setFloatValue(strOutputId,((Float)objValue).floatValue());
						break;
					}
					case ObjectExt.DOUBLE_TYPE:
					{
						cdoRequest.setDoubleValue(strOutputId,((Double)objValue).doubleValue());
						break;
					}
					case ObjectExt.STRING_TYPE:
					{
						cdoRequest.setStringValue(strOutputId,((String)objValue));
						break;
					}
					case ObjectExt.DATE_TYPE:
					{
						cdoRequest.setDateValue(strOutputId,((String)objValue));
						break;
					}
					case ObjectExt.TIME_TYPE:
					{
						cdoRequest.setTimeValue(strOutputId,((String)objValue));
						break;
					}
					case ObjectExt.DATETIME_TYPE:
					{
						cdoRequest.setDateTimeValue(strOutputId,((String)objValue));
						break;
					}
					case ObjectExt.BYTE_ARRAY_TYPE:
					{
						cdoRequest.setByteArrayValue(strOutputId,((byte[])objValue));
						break;
					}
					default:
					{
						throw new SQLException("Unsupported type "+nType);
					}
				}
			}
			else if(blockItem.getSetVar()!=null)
			{
				SetVar sv=blockItem.getSetVar();
				setVar(sv,cdoRequest);
			}
			else if(blockItem.getIf()!=null)
			{
				int nResult=handleIf(conn,hmTrans,(If)blockItem.getIf(),cdoRequest,bUseTransFlag,cdoResponse,ret);
				if(nResult==0)
				{// ??????????????????
					continue;
				}
				else
				{// ??????Break???Return
					return nResult;
				}
			}
			else if(blockItem.getFor()!=null)
			{
				int nResult=handleFor(conn,hmTrans,(For)blockItem.getFor(),cdoRequest,bUseTransFlag,cdoResponse,ret);
				if(nResult==0)
				{// ??????????????????
					continue;
				}
				else
				{// ??????Break???Return
					return nResult;
				}
			}
			else if(blockItem.getReturn()!=null)
			{
				com.cdoframework.cdolib.database.dataservice.Return returnObject=(com.cdoframework.cdolib.database.dataservice.Return)blockItem.getReturn();
				this.handleReturn(returnObject,cdoRequest,cdoResponse,ret);

				return 2;
			}
			else if(blockItem.getBreak()!=null)
			{// Break??????
				return 1;
			}
			else if(blockItem.getCommit()!=null)
			{
				conn.commit();
			}
			else if(blockItem.getRollback()!=null)
			{
				conn.rollback();
			}
		}

		return 0;
	}

	// ????????????Trans
	protected Return handleTrans(Connection conn,HashMap<String,SQLTrans> hmTrans,SQLTrans trans,CDO cdoRequest,
					CDO cdoResponse,boolean bUseTransFlag) throws SQLException,IOException
	{
		Return ret=new Return();

		// ??????????????????
		int nTransFlag=trans.getTransFlag().getType();

		try
		{
			if(bUseTransFlag==true)
			{// ????????????
				conn.setAutoCommit(false);
			}

			// ??????Block??????
			BlockType block=new BlockType();
			int nTransItemCount=trans.getSQLTransChoice(0).getSQLTransChoiceItemCount();
			for(int i=0;i<nTransItemCount;i++)
			{
				SQLTransChoiceItem transItem=trans.getSQLTransChoice(0).getSQLTransChoiceItem(i);
				BlockTypeItem blockItem=null;
				if(transItem.getInsert()!=null)
				{
					blockItem=new BlockTypeItem();
					blockItem.setInsert(transItem.getInsert());
				}
				else if(transItem.getUpdate()!=null)
				{
					blockItem=new BlockTypeItem();
					blockItem.setUpdate(transItem.getUpdate());
				}
				else if(transItem.getDelete()!=null)
				{
					blockItem=new BlockTypeItem();
					blockItem.setDelete(transItem.getDelete());
				}
				else if(transItem.getSelectRecordSet()!=null)
				{
					blockItem=new BlockTypeItem();
					blockItem.setSelectRecordSet(transItem.getSelectRecordSet());
				}
				else if(transItem.getSelectRecord()!=null)
				{
					blockItem=new BlockTypeItem();
					blockItem.setSelectRecord(transItem.getSelectRecord());
				}
				else if(transItem.getSelectField()!=null)
				{
					blockItem=new BlockTypeItem();
					blockItem.setSelectField(transItem.getSelectField());
				}
				else if(transItem.getIf()!=null)
				{
					blockItem=new BlockTypeItem();
					blockItem.setIf(transItem.getIf());
				}
				else if(transItem.getFor()!=null)
				{
					blockItem=new BlockTypeItem();
					blockItem.setFor(transItem.getFor());
				}
				else if(transItem.getDelete()!=null)
				{
					blockItem=new BlockTypeItem();
					blockItem.setDelete(transItem.getDelete());
				}

				if(blockItem!=null)
				{
					block.addBlockTypeItem(blockItem);
				}
			}

			// ????????????
			int nResult=handleBlock(conn,hmTrans,block,cdoRequest,bUseTransFlag,cdoResponse,ret);
			if(nResult!=2)
			{// Break???????????????????????????
				com.cdoframework.cdolib.database.dataservice.Return returnObject=trans.getReturn();
				this.handleReturn(returnObject,cdoRequest,cdoResponse,ret);
			}

			return ret;
		}
		finally
		{
			if(ret.getCode()==0)
			{//????????????
				if((nTransFlag&0x1)==0x1)
				{//????????????
					conn.commit();
				}
			}
			else
			{//????????????
				if((nTransFlag&0x1)==0x1)
				{//????????????
					conn.rollback();
					conn.setAutoCommit(true);
				}
			}
		}
	}

	// ????????????,???????????????????????????????????????????????????public??????------------------------------------------------------
	/**
	 * ???????????????
	 */
	public synchronized Return open()
	{
		if(ds!=null)
		{
			return Return.OK;
		}

		ds=new BasicDataSource();   
        ds.setDriverClassName(strDriver);   
        ds.setUsername(strUserName);   
        ds.setPassword(strPassword);   
        ds.setUrl(strURI);   
        ds.setInitialSize(nMinConnectionCount);
        ds.setMaxActive(nMaxConnectionCount);   
        ds.setMaxIdle(10);
        ds.setMinIdle(nMinConnectionCount);
        ds.setMaxWait(10000);//?????????????????????????????????
        ds.setTestWhileIdle(true);
        ds.setTestOnBorrow(true);
        ds.setValidationQuery("select 1");
        ds.setPoolPreparedStatements(true);
        ds.setRemoveAbandoned(true);
        ds.setRemoveAbandonedTimeout(60);
        ds.setLogAbandoned(true);
        
		// ????????????
		try
		{
			Connection conn=ds.getConnection();
			closeConnection(conn);
		}
		catch(Exception e)
		{
			callOnException("Open database error: "+e.getMessage(),e);

			Return ret=Return.valueOf(-1,e.getMessage(),"System.Error");
			ret.setThrowable(e);
			return ret;

		}

		return Return.OK;
	}

	/**
	 * ???????????????
	 * 
	 */
	public synchronized void close()
	{
		if(ds!=null)
		{
			try
			{
				ds.close();
			}
			catch(Exception e)
			{
			}
			ds=null;
		}
		
		synchronized(this.hmAnalyzedSQL)
		{
			this.hmAnalyzedSQL.clear();
		}
	}

	/**
	 * ???????????????????????????
	 * 
	 * @return
	 */
	public Connection getConnection() throws SQLException
	{
		Return ret=this.open();
		if(ret.getCode()!=0)
		{
			return null;
		}

		return ds.getConnection();
	}

	public void commit(Connection conn) throws SQLException
	{
		conn.commit();
	}

	public void rollback(Connection conn)
	{
		try
		{
			if(conn.getAutoCommit()==false)
			{
				conn.rollback();
			}
		}
		catch(Exception e)
		{
		}
	}

	/**
	 * ???????????????
	 * 
	 * @param rs
	 */
	public void closeResultSet(ResultSet rs)
	{
		if(rs==null)
		{
			return;
		}

		try
		{
			rs.close();
		}
		catch(Exception e)
		{
		}
	}

	/**
	 * ??????Statement
	 * 
	 * @param stat
	 */
	public void closeStatement(String strSQL,PreparedStatement stat)
	{
		if(stat==null)
		{
			return;
		}

		try
		{
			stat.close();
		}
		catch(Exception e)
		{
		}
	}

	/**
	 * ??????Statement
	 * 
	 * @param stat
	 */
	public void closeStatement(Statement stat)
	{
		if(stat==null)
		{
			return;
		}

		try
		{
			stat.close();
		}
		catch(Exception e)
		{
		}
	}

	/**
	 * ??????Connection
	 * 
	 * @param conn
	 */
	public void closeConnection(Connection conn)
	{
		if(conn==null)
		{
			return;
		}
		try
		{
			if(conn.getAutoCommit()==false)
			{
				conn.setAutoCommit(true);
			}
		}
		catch(SQLException e1)
		{
		}

		try
		{
			conn.close();
		}
		catch(Exception e)
		{
		}
	}

	/**
	 * ??????Connection
	 * 
	 * @param conn
	 */
	public void closeConnection(Connection conn,boolean bAutoCommit)
	{
		if(conn==null)
		{
			return;
		}

		try
		{
			if(conn.getAutoCommit()!=bAutoCommit)
			{
				conn.setAutoCommit(bAutoCommit);
			}
		}
		catch(Exception e)
		{
		}
		try
		{
			conn.close();
		}
		catch(Exception e)
		{
		}
	}

	/**
	 * ????????????????????????????????????????????????????????????????????????????????????
	 * 
	 * @param conn
	 * @param strSQL
	 * @param cdoRequest
	 * @param cdoResponse
	 * @return
	 * @throws Exception
	 */
	public Object executeQueryField(Connection conn,String strSQL,CDO cdoRequest) throws SQLException,IOException
	{
		// ??????JDBC??????
		PreparedStatement ps=prepareStatement(conn,strSQL,cdoRequest);

		// ??????????????????
		ResultSet rs=null;
		try
		{
			// ????????????
			rs=ps.executeQuery();

			// ??????????????????
			ResultSetMetaData meta=rs.getMetaData();
			String[] strsFieldName=new String[1];
			int[] nsFieldType=new int[1];
			int[] nsPrecision=new int[1];
			int[] nsScale=new int[1];
			for(int i=0;i<strsFieldName.length;i++)
			{
				strsFieldName[i]=meta.getColumnName(i+1);
				nsFieldType[i]=meta.getColumnType(i+1);
				nsPrecision[i]=meta.getPrecision(i+1);
				nsScale[i]=meta.getScale(i+1);
			}

			CDO cdoRecord=readRecord(rs,strsFieldName,nsFieldType,nsPrecision,nsScale);
			if(cdoRecord==null)
			{
				return null;
			}

			// ??????
			if(cdoRecord.exists(strsFieldName[0]))
			{
				return cdoRecord.getObject(strsFieldName[0]);
			}
			else
			{
				return null;
			}
		}
		catch(SQLException e)
		{
			callOnException("executeQueryField Exception: "+strSQL,e);
			throw e;
		}
		finally
		{
			this.closeResultSet(rs);
			this.closeStatement(strSQL,ps);
		}
	}

	/**
	 * ????????????????????????????????????????????????????????????????????????????????????(?????????)
	 * 
	 * @param conn
	 * @param strSQL
	 * @param cdoRequest
	 * @param cdoResponse
	 * @return
	 * @throws Exception
	 */
	public ObjectExt executeQueryFieldExt(Connection conn,String strSQL,CDO cdoRequest) throws SQLException,IOException
	{
		// ??????JDBC??????
		PreparedStatement ps=prepareStatement(conn,strSQL,cdoRequest);

		// ??????????????????
		ResultSet rs=null;
		try
		{
////System.out.println("Query "+conn.toString());
			// ????????????
			rs=ps.executeQuery();
			ResultSetMetaData meta=rs.getMetaData();
			String[] strsFieldName=new String[1];
			int[] nsFieldType=new int[1];
			int[] nsPrecision=new int[1];
			int[] nsScale=new int[1];
			for(int i=0;i<strsFieldName.length;i++)
			{
				strsFieldName[i]=meta.getColumnName(i+1);
				nsFieldType[i]=meta.getColumnType(i+1);
				nsPrecision[i]=meta.getPrecision(i+1);
				nsScale[i]=meta.getScale(i+1);
			}

			// ??????????????????
			CDO cdoRecord=readRecord(rs,strsFieldName,nsFieldType,nsPrecision,nsScale);
			if(cdoRecord==null)
			{
				return null;
			}

			// ??????
			if(cdoRecord.exists(strsFieldName[0]))
			{
				return cdoRecord.getObject(strsFieldName[0]);
			}
			else
			{
				return null;
			}
		}
		catch(SQLException e)
		{
			callOnException("executeQueryField Exception: "+strSQL,e);
			throw e;
		}
		finally
		{
			this.closeResultSet(rs);
			this.closeStatement(strSQL,ps);
		}
	}

	/**
	 * ??????????????????????????????????????????????????????????????????
	 * 
	 * @param conn
	 * @param strSQL
	 * @param cdoRequest
	 * @param cdoResponse
	 * @return
	 * @throws Exception
	 */
	public int executeQueryRecord(Connection conn,String strSQL,CDO cdoRequest,CDO cdoResponse) throws SQLException,
					IOException
	{
		// ??????JDBC?????? ??????sql????????????
		PreparedStatement ps=prepareStatement(conn,strSQL,cdoRequest);

		// ??????????????????
		ResultSet rs=null;
		try
		{
			// ????????????
			rs=ps.executeQuery();
			ResultSetMetaData meta=rs.getMetaData();
			String[] strsFieldName=new String[meta.getColumnCount()];
			int[] nsFieldType=new int[strsFieldName.length];
			int[] nsPrecision=new int[strsFieldName.length];
			int[] nsScale=new int[strsFieldName.length];
			for(int i=0;i<strsFieldName.length;i++)
			{
				/**??????JDBC4**/
				strsFieldName[i]=meta.getColumnLabel(i+1);
				/**??????JDBC3**/
				/**strsFieldName[i]=meta.getColumnName(i+1);**/
				nsFieldType[i]=meta.getColumnType(i+1);
				nsPrecision[i]=meta.getPrecision(i+1);
				nsScale[i]=meta.getScale(i+1);
			}
			// ??????????????????
			int nRecordCount=readRecord(rs,strsFieldName,nsFieldType,nsPrecision,nsScale,cdoResponse);
			//????????????
			int nCount=executeCount(conn, strSQL, cdoRequest);
			if(nCount==0)
				nCount=nRecordCount;
			return nCount;
			
		}
		catch(SQLException e)
		{
			callOnException("executeQueryRecord Exception: "+strSQL,e);
			throw e;
		}
		finally
		{
			this.closeResultSet(rs);
			this.closeStatement(strSQL,ps);
		}
	}

	private int executeCount(Connection conn,String strSQL,CDO cdoRequest){
		if(!cdoRequest.exists("$$nRecordCountId$$") || !cdoRequest.getBooleanValue("$$nRecordCountId$$"))
			return 0;
		if(!getDriver().trim().equals("com.mysql.jdbc.Driver"))
			return 0;

		String strTempSQL=strSQL.toUpperCase();
		//???????????????
		int index=-1;
		boolean isGroup=false;
		if(strTempSQL.contains("GROUP")){
			index=strTempSQL.lastIndexOf("GROUP");
			index=index+5;
			if(strTempSQL.charAt(index)==' ' || strTempSQL.charAt(index)!='\t'
					|| strTempSQL.charAt(index)=='\n'){	
				index++;
				while(true){
					try{
						if(strTempSQL.charAt(index)==' ' || strTempSQL.charAt(index)=='\t'
								|| strTempSQL.charAt(index)=='\n'){
							System.out.println(strTempSQL.charAt(index));
							index++;
							continue;
						}						
						if(strTempSQL.charAt(index)=='B' && strTempSQL.charAt(index+1)=='Y'
								&& (strTempSQL.charAt(index+2)==' ' || strTempSQL.charAt(index+2)!='\t'
										|| strTempSQL.charAt(index+2)!='\n') ){
							isGroup=true;
							break;
						}else{
							break;
						}
					}catch(Exception ex){
						isGroup=false;
					}
				}				
			}
		}
		
		//????????????Group by  
		if(!isGroup){
			//?????? ??????SQL From?????? ??????			
			while(true){
				strTempSQL=strSQL.toUpperCase();
				index=strTempSQL.lastIndexOf("FROM");			
				if(index==-1 || strTempSQL.length()<(index+4))
					break;
				if(strSQL.charAt(index-1)=='{' && strSQL.charAt(index+4)=='}'){
					strSQL=" "+strSQL.substring(index+4);
					continue;
				}
				//???????????????????????? ,?????? ??? From????????????????????????
				if((strTempSQL.charAt(index-1)==' ' ||strTempSQL.charAt(index-1)=='\t' || strTempSQL.charAt(index-1)=='\n')
						&& (strTempSQL.charAt(index+4)==' ' ||strTempSQL.charAt(index+4)=='\t' || strTempSQL.charAt(index+4)=='\n')){
					strSQL=strSQL.substring(index);
					break;
				}else{
					for(int i=(index+4);i<strSQL.length();i++){
						if(strSQL.charAt(i)==' '||strSQL.charAt(i)=='\t'||strSQL.charAt(i)=='\n'){
							strSQL=strSQL.substring(i);
							break;
						}
					}
				}
			}
		}
		/** strSQL?????? ???from ??? ?????????????????? 
		 * ?????? limit??????
		 */
		//???????????????????????????
		StringBuilder sb=new StringBuilder(30);
		sb.append("SELECT count(*) as nCount ");
		if(isGroup)
			sb.append(" FROM (");
		delOrderLimit(sb, strSQL, "LIMIT");		
		/**??????Order  By**/
		strSQL=sb.toString();
		sb=new StringBuilder(30);
		delOrderLimit(sb, strSQL, "ORDER");
		if(isGroup)
			sb.append(" )T");		
		// ??????JDBC?????? ??????sql????????????
		PreparedStatement ps=null;			
		// ??????????????????
		ResultSet rs=null;
		int nCount=0;
		try
		{
			ps=prepareStatement(conn,sb.toString(),cdoRequest);
			rs=ps.executeQuery();	
			while(rs.next()){
				nCount=rs.getInt("nCount");
			}
		}catch(Exception e){		
			nCount=0;
			callOnException("executeQueryRecord Count Exception: "+strSQL,e);			
		}finally{
			this.closeResultSet(rs);
			this.closeStatement(strSQL,ps);
		}
		return nCount;
		
	}
	/**??????strSQL ?????? Limit,ORDER BY ?????????????????????
	 * delKeys="LIMIT","ORDER"
	**/
	private void delOrderLimit(StringBuilder sb,String strSQL,String delKeys){
		String strTempSQL=null;
		int index=-1;
		int delKeyLength=delKeys.length();
		boolean isbreak=false;
		while(true){
			strTempSQL=strSQL.toUpperCase();
			index=strTempSQL.lastIndexOf(delKeys);			
			if(index==-1 || strTempSQL.length()<(index+delKeyLength)){
				if(delKeys.equals("ORDER"))
					sb.append(strSQL);
				break;
			}				
			if(strSQL.charAt(index-1)=='{' && strSQL.charAt(index+delKeyLength)=='}'){
				sb.append(strSQL.substring(0,index+delKeyLength));
				strSQL=" "+strSQL.substring(index+delKeyLength);
				continue;
			}
			if((strTempSQL.charAt(index-1)==' ' ||strTempSQL.charAt(index-1)=='\t' || strTempSQL.charAt(index-1)=='\n')
				&& (strTempSQL.charAt(index+delKeyLength)==' '||strTempSQL.charAt(index+delKeyLength)=='\t'||strTempSQL.charAt(index+delKeyLength)=='\n')){
				sb.append(strSQL.substring(0,index));
				break;
			}else{
				isbreak=false;
				for(int i=(index+delKeyLength);i<strSQL.length();i++){
					if(strSQL.charAt(i)==' ' || strSQL.charAt(i)=='\t' || strSQL.charAt(index+delKeyLength)=='\n'){
						sb.append(strSQL.substring(0,i));
						strSQL=strSQL.substring(i);
						isbreak=true;
						break;
					}
				}
				if(!isbreak)
					break;
			}	
		}	
	}
	
	/**
	 * ???????????????????????????????????????????????????????????????
	 * 
	 * @param conn
	 * @param strSQL
	 * @param cdoRequest
	 * @param cafRecordSet
	 * @return
	 * @throws Exception
	 */
	public int executeQueryRecordSet(Connection conn,String strSQL,CDO cdoRequest,CDOArrayField cafRecordSet)
					throws SQLException,IOException
	{
		// ??????JDBC?????? ??????????????????
		PreparedStatement ps=prepareStatement(conn,strSQL,cdoRequest);		
		// ??????????????????
		ResultSet rs=null;
		try
		{
			// ????????????
			rs=ps.executeQuery();
			// ??????Meta??????
			ResultSetMetaData meta=rs.getMetaData();
			String[] strsFieldName=new String[meta.getColumnCount()];
			int[] nsFieldType=new int[strsFieldName.length];
			int[] nsPrecision=new int[strsFieldName.length];
			int[] nsScale=new int[strsFieldName.length];
		
			for(int i=0;i<strsFieldName.length;i++)
			{
				/**??????JDBC4**/
				strsFieldName[i]=meta.getColumnLabel(i+1);
				/**??????JDBC3**/
				/**strsFieldName[i]=meta.getColumnName(i+1);**/
				nsFieldType[i]=meta.getColumnType(i+1);
				nsPrecision[i]=meta.getPrecision(i+1);
				nsScale[i]=meta.getScale(i+1);
			}

			// ????????????
			ArrayList<CDO> alRecord=new ArrayList<CDO>();
			while(true)
			{
				// ??????????????????
				CDO cdoRecord=readRecord(rs,strsFieldName,nsFieldType,nsPrecision,nsScale);
				if(cdoRecord==null)
				{
					break;
				}
				alRecord.add(cdoRecord);
			}

			// ????????????
			CDO[] cdosRecord=new CDO[alRecord.size()];
			for(int i=0;i<cdosRecord.length;i++)
			{
				cdosRecord[i]=(CDO)alRecord.get(i);
			}
			cafRecordSet.setValue(cdosRecord);
			
			//?????????????????????
			int nCount=executeCount(conn, strSQL, cdoRequest);
			if(nCount==0)
				nCount=cdosRecord.length;
			
			return nCount;
		}
		catch(SQLException e)
		{
			callOnException("executeQueryRecordSet Exception: "+strSQL,e);
			throw e;
		}
		finally
		{
			this.closeResultSet(rs);
			this.closeStatement(strSQL,ps);
		}
	}

	/**
	 * ???????????????????????????
	 * 
	 * @param conn
	 * @param strSQL
	 * @param cdoRequest
	 * @return
	 * @throws Exception
	 */
	public int executeUpdate(Connection conn,String strSQL,CDO cdoRequest) throws SQLException
	{
		// ??????JDBC??????
		PreparedStatement ps=prepareStatement(conn,strSQL,cdoRequest);

		// ??????????????????
		try
		{
			// ????????????
////System.out.println("Update "+conn.toString());
			int nCount=ps.executeUpdate();

			return nCount;
		}
		catch(SQLException e)
		{
			callOnException("executeUpdate Exception: "+strSQL,e);
			throw e;
		}
		finally
		{
			this.closeStatement(strSQL,ps);
		}
	}

	/**
	 * ???????????????????????????
	 * 
	 * @param conn
	 * @param strSQL
	 * @param cdoRequest
	 * @return
	 * @throws Exception
	 */
	public void executeSQL(Connection conn,String strSQL,CDO cdoRequest) throws SQLException
	{
		// ??????JDBC??????
		PreparedStatement ps=prepareStatement(conn,strSQL,cdoRequest);

		// ??????????????????
		try
		{
			// ????????????
			ps.execute();
		}
		catch(SQLException e)
		{
			callOnException("executeUpdate Exception: "+strSQL,e);
			throw e;
		}
		finally
		{
			this.closeStatement(strSQL,ps);
		}
	}

	/**
	 * ??????????????????????????????????????????
	 * 
	 * @param conn
	 * @param cdoRequest
	 * @return
	 */
	public Return executeTrans(Connection conn,HashMap<String,SQLTrans> hmTrans,CDO cdoRequest,CDO cdoResponse,boolean bUseTransFlag)
	{
		Return ret=null;

		// ??????TransName
		String strTransName="";
		try
		{
			strTransName=cdoRequest.getStringValue("strTransName");
		}
		catch(Exception e)
		{
			return null;
		}

		// ??????Trans??????
		SQLTrans trans=(SQLTrans)hmTrans.get(strTransName);
		if(trans==null)
		{// ?????????
			return null;
		}

		// Trans???????????????????????????
		try
		{
			return handleTrans(conn,hmTrans,trans,cdoRequest,cdoResponse,bUseTransFlag);
		}
		catch(SQLException e)
		{
			callOnException("executeTrans Exception: "+strTransName,e);

			ret=null;

			OnException onException=trans.getOnException();
			int nErrorCount=onException.getOnErrorCount();
			for(int i=0;i<nErrorCount;i++)
			{
				OnError onError=onException.getOnError(i);
				if(onError.getCode()==e.getErrorCode())
				{
					ret=Return.valueOf(onError.getReturn().getCode(),onError.getReturn().getText(),onError.getReturn()
									.getInfo());
					break;
				}
			}
			if(ret==null)
			{// ????????????OnError
				ret=Return.valueOf(onException.getReturn().getCode(),onException.getReturn().getText(),onException
								.getReturn().getInfo());
			}

			return ret;
		}
		catch(IOException e)
		{
			callOnException("executeTrans Exception: "+strTransName,e);

			OnException onException=trans.getOnException();
			ret=Return.valueOf(onException.getReturn().getCode(),onException.getReturn().getText(),onException
							.getReturn().getInfo());

			return ret;
		}
		catch(Exception e)
		{
			callOnException("executeTrans Exception: "+strTransName,e);

			OnException onException=trans.getOnException();
			ret=Return.valueOf(onException.getReturn().getCode(),onException.getReturn().getText(),onException
							.getReturn().getInfo());

			return ret;
		}
	}

	/**
	 * ???????????????????????????????????????????????????TransFlag
	 * 
	 * @param conn
	 * @param cdoRequest
	 * @return
	 */
	public Return executeTrans(Connection conn,HashMap<String,SQLTrans> hmTrans,CDO cdoRequest,CDO cdoResponse)
	{
		return executeTrans(conn,hmTrans,cdoRequest,cdoResponse,true);
	}

	/**
	 * ?????????????????????
	 * 
	 * @param cdoRequest
	 * @param cdoResponse
	 * @return
	 */
	public Return executeTrans(HashMap<String,SQLTrans> hmTrans,CDO cdoRequest,CDO cdoResponse)
	{
		Return ret=null;

		// ?????????????????????
		Connection conn=null;
		try
		{
			conn=this.getConnection();
		}
		catch(SQLException e)
		{
			ret=Return.valueOf(RETURN_SYSTEMERROR,"Cannot obtain database connection","System.Error");
			return ret;
		}

		// ?????????????????????
		try
		{
			ret=executeTrans(conn,hmTrans,cdoRequest,cdoResponse);
		}
		finally
		{
			closeConnection(conn);
		}

		return ret;
	}


	// ????????????,?????????????????????????????????????????????--------------------------------------------------------------------

	// ????????????,???????????????????????????????????????(?????????on...ed)????????????-------------------------------------------------

	// ????????????,?????????????????????????????????????????????????????????????????????????????????(?????????on...ed)????????????---------------------
	public void onException(String strText,Exception e)
	{
	}

	public void onSQLStatement(String strSQL)
	{
	}

	// ????????????,??????????????????????????????------------------------------------------------------------------------------

	public DataEngine()
	{
		// ??????????????????????????????,???????????????????????????????????????????????????,????????????????????????null??????????????????????????????????????????????????????
		strDriver="";
		strURI="";
		strCharset="GBK";
		properties=null;

		ds=null;

		strUserName="";
		strPassword="";
		strSystemCharset=System.getProperty("sun.jnu.encoding");

		nMinConnectionCount=10;
		nMaxConnectionCount=100;
		lTimeout=60000L;

		hmStatement		=new HashMap<String,ArrayList<PreparedStatement>>(100);
		hmAnalyzedSQL	=new HashMap<String,AnalyzedSQL>(100);
	}

//	public static void main(String[] args)
//	{
//		CDO cdoRequest=new CDO();
////		cdoRequest.setStringValue("strName","Frank");
//		
//		DataEngine dataEngine=new DataEngine();
//		dataEngine.checkCondition("{strName}","IS","NULL",0,"string",cdoRequest);
//	}
	
}
