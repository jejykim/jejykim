package com.decision.v2x.dcm.util.http;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;

import javax.net.ssl.*;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.decision.v2x.dcm.util.Exception.HttpUnauthorizedException;
import com.decision.v2x.dcm.util.convert.RestApiParameter;
import com.sun.corba.se.impl.encoding.CodeSetConversion.BTCConverter;

import okhttp3.*;


public class iRestApi 
{
	static protected final int MAX_TOKEN_MINUTE = 14;
	
	//protected final String IP = "https://dcm.pilot.c-its.pentasecurity.com:8980";
	//static protected final String IP = "https://192.168.3.175:8980";
	static private String IP;

	protected String m_ID;
	protected String m_Pwd;
	
	static private RestApiParameter m_Param = new RestApiParameter();
	static protected final String HEADER_ACCEPT_JSON = "application/json";
	static protected final String HEADERJSON = "application/json; charset=utf-8";
	static protected final String HEADERFORM = "multipart/form-data";
	static protected final String USER_AGENT = "Mozilla/5.0";
	static protected final String AUTHORIZATION_PREFIX = "Bearer ";
	protected String jwtToken = "";
	protected String jwtReToken = "";
	protected Date jwtTokenDate = null;

	public iRestApi(String id, String pwd)
	{
		this.m_ID = id;
		this.m_Pwd = pwd;
	}
	
	static public void init(String ip) throws Exception
	{
		IP = ip;
		disableSslVerification();
	}
	static public String getIP()
	{
		return IP;
	}
	
	public void setJwt(String jwt)
	{
		this.jwtToken = jwt;
	}
	public boolean isToken()
	{
		if(this.jwtToken.isEmpty())
			return false;
		
		return true;
	}
	public boolean isReToken()
	{
		if(this.jwtReToken.isEmpty())
			return false;
		
		return true;
	}
	
	public JSONObject ERALogin() throws Exception
	{
		final String URL = "/api/webui/login";
		HashMap<String, String> header = new HashMap<String, String>();
		JSONObject data = this.m_Param.eraLogin(this.m_ID, this.m_Pwd);

		//header.put("Accept", HEADER_ACCEPT_JSON);
		header.put("Content-Type", HEADERJSON);
		String strResult = requestHttpPOST(URL, data, header);
		
		JSONParser jp = new JSONParser();
		JSONObject retval = (JSONObject) jp.parse(strResult);
	
		this.jwtToken = retval.get("access_token").toString();
		this.jwtReToken = retval.get("refresh_token").toString();
		this.jwtTokenDate = new Date();
		
		return retval;
	}
	
	// Refresh 토큰으로 access 토큰 요청
	//api/webui/refresh
	public JSONObject getAccessToken() throws Exception
	{
		final String URL = "/api/webui/refresh";
		HashMap<String, String> header = new HashMap<String, String>();
		JSONObject data = null;
		
		header.put("Content-Type", HEADERJSON);
		header.put("Authorization", AUTHORIZATION_PREFIX + jwtToken);
		String strResult = requestHttpPOST(URL, data, header);
		
		JSONParser jp = new JSONParser();
		JSONObject retval = (JSONObject) jp.parse(strResult);
		
		this.jwtToken = retval.get("access_token").toString();
		
		return retval;
	}
	
	/**
	 * 신뢰되지 않은 SSL인증서를 무조건 신뢰하도록 변경하는 함수
	 * */
	//@PostConstruct -> spring의 어노테이션, init과 같은 기능을 하며 was를 실행하면 1번만 호출이 된다.
	@SuppressWarnings("unused")
	public static void disableSslVerification() throws Exception
	{
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() 
        {
    		@Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() 
            {
                return null;
            }
            
			@Override
			public void checkClientTrusted(java.security.cert.X509Certificate[] arg0, String arg1)
					throws CertificateException 
			{
				// TODO Auto-generated method stub
				
			}
			@Override
			public void checkServerTrusted(java.security.cert.X509Certificate[] arg0, String arg1)
					throws CertificateException 
			{
				// TODO Auto-generated method stub
					
			}
        }};
 
            // Install the all-trusting trust manager
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() 
            {
                public boolean verify(String hostname, SSLSession session) 
                {
                    return true;
                }
            };
 
            // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    }
	JSONObject requestHttpFile(String strUrl, boolean bGet, HashMap<String, String> headers ) throws Exception
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		JSONObject retval = new JSONObject();
	
		HttpsURLConnection con = getHttpsConnection(strUrl, true);		
		/*
		if(!(jwtToken.isEmpty() || jwtToken == null)) 
		{
			con.setRequestProperty("Authorization", "bearer " + "TESTTESTTEST");
		}
		
		if(!(headerAccept.isEmpty() || headerAccept == null)) 
		{
			con.setRequestProperty("Accept", headerAccept);
			if(!headerAccept.equals(HEADERFORM)) 
			{
				con.setRequestProperty("content-type", headerAccept);
			}
		}
		//*/
		if(headers != null && headers.size() > 0)
		{
			Set<String> keys = headers.keySet();
			for(String key : keys)
			{
				String value = headers.get(key);
				con.setRequestProperty(key, value);
			}
		}				
					
		int responseCode = con.getResponseCode();
		retval.put("Copntent-Type", con.getHeaderField("Content-Type"));
		retval.put("Content-Disposition", con.getHeaderField("Content-Disposition"));
					
		InputStream stream = con.getInputStream();
		BufferedInputStream in = new BufferedInputStream(stream);
		byte[] buffer = new byte[1024];
		int i = 0;
		while((i = in.read(buffer)) != -1)
		{
			out.write(buffer);
		}
		
		in.close();
		
		retval.put("bytes", out.toByteArray());
		return retval;
	}
	
	/**
	 * 
	 * @param strUrl url
	 * @param bGet true일 경우 get이고 false일 경우에는 post
	 * @return 
	 * @throws Exception
	 */
	protected HttpsURLConnection getHttpsConnection(String strUrl, boolean bGet) throws Exception
	{
		URL url = new URL(strUrl);
		
		HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
		con.setConnectTimeout(300000);
		
		/*
		con.setHostnameVerifier(new HostnameVerifier() {
			
			@Override
			public boolean verify(String hostname, SSLSession session) 
			{
				// TODO Auto-generated method stub
				return true;
			}
		});
		//*/
		//con.setRequestProperty("User-Agent", "Mozilla/5.0");
		
		if(!this.jwtToken.isEmpty())
		{
			
			if(this.jwtTokenDate != null)
			{
				Date now = new Date();
				
				long result = now.getTime() - this.jwtTokenDate.getTime();
				result = result / (60 * 1000);
				
				if(result >= MAX_TOKEN_MINUTE)
				{
					this.ERALogin();
				}
			}
			
			con.setRequestProperty("Authorization", AUTHORIZATION_PREFIX + this.jwtToken);
		}

		if(bGet)
		{
			con.setRequestMethod("GET");
		}
		else
		{
			con.setRequestMethod("POST");
			con.setDoOutput(true);
		}

		con.setDoInput(true);
		
		return con;
	}
	
	
	
	
	/**
	 * http 통신 (GET)
	 * @param URL - GW URL
	 * @param param - 요청 파라미터
	 * @return String - 응답 response
	 * @exception Exception
	 */
	public String requestHttpGET(String strUrl, String param, String headerAccept) 
	{
		StringBuffer response = new StringBuffer();
		HashMap<Integer, Integer> error = initErrorData();
		try 
		{
			HttpsURLConnection con = this.getHttpsConnection(IP + strUrl, true);
						
			if(!(jwtToken.isEmpty() || jwtToken == null)) 
			{
				con.setRequestProperty("Authorization", AUTHORIZATION_PREFIX + jwtToken);
			}
			
			if(!(headerAccept.isEmpty() || headerAccept == null)) 
			{
				con.setRequestProperty("Accept", headerAccept);
				if(!headerAccept.equals(HEADERFORM)) 
				{
					con.setRequestProperty("content-type", headerAccept);
				}
			}
						
			int responseCode = con.getResponseCode();
			
			/*=============================================*/
			// api 재로그인 소스 (추가자 : 김진열)
			if(responseCode == 401) {
				this.Error(responseCode, error);
				
				this.getAccessToken();
				
				con = this.getHttpsConnection(IP + strUrl, true);
				
				if(!(jwtToken.isEmpty() || jwtToken == null)) 
				{
					con.setRequestProperty("Authorization", AUTHORIZATION_PREFIX + jwtToken);
				}
				
				if(!(headerAccept.isEmpty() || headerAccept == null)) 
				{
					con.setRequestProperty("Accept", headerAccept);
					if(!headerAccept.equals(HEADERFORM)) 
					{
						con.setRequestProperty("content-type", headerAccept);
					}
				}
				
				responseCode = con.getResponseCode();
			}
			/*=============================================*/
			
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			
			String inputLine;
			
			while ((inputLine = in.readLine()) != null) 
			{
				response.append(inputLine);
			}
			
			in.close();
			
			//System.out.println("응답 코드 : " + responseCode);
			//System.out.println("응답 response : " + response.toString());
			
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		return response.toString();
	}

	
	
	
	
	
	/**
	 * http 통신 (GET)
	 * @param URL - GW URL
	 * @param param - 요청 파라미터
	 * @return String - 응답 response
	 * @exception Exception
	 */
	public String requestHttpGET(String strUrl, HashMap<String, String> headers)
			throws Exception, HttpUnauthorizedException
	{
		int responseCode = -1;
		StringBuffer response = new StringBuffer();
		HashMap<Integer, Integer> error = initErrorData();
		
		while(true)
		{
			try 
			{
				HttpsURLConnection con = this.getHttpsConnection(IP + strUrl, true);
				
				if(headers != null)
				{
					Set<String> keys = headers.keySet(); 
					for(String key : keys)
					{
						String item = headers.get(key);
						con.setRequestProperty(key, item);
					}
				}
				
				
				responseCode = con.getResponseCode();
				
				/*=============================================*/
				// api 재로그인 소스 (추가자 : 김진열)
				if(responseCode == 401) {
					this.Error(responseCode, error);
					
					this.getAccessToken();
					
					con = this.getHttpsConnection(IP + strUrl, true);
					if(headers != null)
					{
						Set<String> keys = headers.keySet(); 
						for(String key : keys)
						{
							String item = headers.get(key);
							con.setRequestProperty(key, item);
						}
					}
					
					responseCode = con.getResponseCode();
				}
				/*=============================================*/
				
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				
				String inputLine;
				
				while ((inputLine = in.readLine()) != null) 
				{
					response.append(inputLine);
				}
				
				in.close();

				return response.toString();
			}
			catch (Exception e) 
			{
				if(!this.Error(responseCode, error))
				{	
					throw e;
				}
			}
		}
	}

	/**
	 * http 통신 (POST)
	 * @param URL - GW URL
	 * @param param - 요청 파라미터
	 * @return String - 응답 response
	 * @exception Exception
	 */
	public String requestHttpPOST(String strUrl, JSONObject param, HashMap<String, String> headers) 
			throws Exception, HttpUnauthorizedException
	{
		String errMsg = "";
		StringBuffer response = new StringBuffer();
		HttpsURLConnection con = null;
		URL url =  null;
		int responseCode = -1;
		HashMap<Integer, Integer> error = initErrorData();
		
		while(true)
		{
			try 
			{	
				con = this.getHttpsConnection(getIP() + strUrl, false);
				
				if(headers != null)
				{
					Set<String> keys = headers.keySet(); 
					for(String key : keys)
					{
						String item = headers.get(key);
						con.setRequestProperty(key, item);
					}
				}
				
				if(param != null)
				{
					DataOutputStream wr = new DataOutputStream(con.getOutputStream());
					wr.writeBytes(param.toString());
					wr.flush();
					wr.close();
				}
				
				responseCode = con.getResponseCode();
				
				/*=============================================*/
				// api 재로그인 소스 (추가자 : 김진열)
				if(responseCode == 401) {
					this.Error(responseCode, error);
					
					this.getAccessToken();
					
					con = this.getHttpsConnection(getIP() + strUrl, false);
					if(headers != null)
					{
						Set<String> keys = headers.keySet(); 
						for(String key : keys)
						{
							String item = headers.get(key);
							con.setRequestProperty(key, item);
						}
					}
					
					if(param != null)
					{
						DataOutputStream wr = new DataOutputStream(con.getOutputStream());
						wr.writeBytes(param.toString());
						wr.flush();
						wr.close();
					}
					
					responseCode = con.getResponseCode();
				}
				/*=============================================*/
				
				InputStream is = con.getInputStream();
				
				if(is != null)
				{
					BufferedReader in = new BufferedReader(new InputStreamReader(is));
				
					String inputLine;
					
					while ((inputLine = in.readLine()) != null) 
					{
						response.append(inputLine);
					}
					
					in.close();	
					is.close();
				}
				else
				{
					response.append("null");				
				}
				
				return response.toString();
			} 
			catch (Exception e) 
			{
				if(!this.Error(responseCode, error))
				{
					throw e;
				}
			}
		}
	}
	
	
	public String requestHttpPOST(String strUrl, String param, HashMap<String, String> headers) 
			throws Exception, HttpUnauthorizedException
	{
		String errMsg = "";
		StringBuffer response = new StringBuffer();
		HttpsURLConnection con = null;
		URL url =  null;
		int responseCode = -1;
		HashMap<Integer, Integer> error = initErrorData();
		
		while(true)
		{
			try 
			{	
				con = this.getHttpsConnection(getIP() + strUrl, false);
				
				if(headers != null)
				{
					Set<String> keys = headers.keySet(); 
					for(String key : keys)
					{
						String item = headers.get(key);
						con.setRequestProperty(key, item);
					}
				}
				
				if(param != null)
				{
					DataOutputStream wr = new DataOutputStream(con.getOutputStream());
					wr.writeBytes(param);
					wr.flush();
					wr.close();
				}
				
				responseCode = con.getResponseCode();

				/*=============================================*/
				// api 재로그인 소스 (추가자 : 김진열)
				
				if(responseCode == 401) {
					this.Error(responseCode, error);
					
					this.getAccessToken();
					
					con = this.getHttpsConnection(getIP() + strUrl, false);
					if(headers != null)
					{
						Set<String> keys = headers.keySet(); 
						for(String key : keys)
						{
							String item = headers.get(key);
							con.setRequestProperty(key, item);
						}
					}
					
					if(param != null)
					{
						DataOutputStream wr = new DataOutputStream(con.getOutputStream());
						wr.writeBytes(param);
						wr.flush();
						wr.close();
					}
					
					responseCode = con.getResponseCode();
				}
				/*=============================================*/
				
				InputStream is = con.getInputStream();
				
				if(is != null)
				{
					BufferedReader in = new BufferedReader(new InputStreamReader(is));
				
					String inputLine;
					
					while ((inputLine = in.readLine()) != null) 
					{
						response.append(inputLine);
					}
					
					in.close();	
					is.close();
				}
				else
				{
					response.append("null");				
				}
				
				return response.toString();
			} 
			catch (Exception e) 
			{
				if(!this.Error(responseCode, error))
				{
					throw e;
				}
			}
		}
	}
	
	public HashMap<Integer, Integer> initErrorData()
	{
		HashMap<Integer, Integer> retval = new HashMap<Integer, Integer>();
		
		//Client
		retval.put(400, 0);
		retval.put(401, 0);
		retval.put(402, 0);
		retval.put(403, 0);
		retval.put(404, 0);
		retval.put(405, 0);
		retval.put(406, 0);
		retval.put(407, 0); 
		retval.put(408, 0);
		retval.put(409, 0);
		retval.put(410, 0);
		retval.put(411, 0);
		retval.put(412, 0);
		retval.put(413, 0);
		retval.put(414, 0);
		retval.put(415, 0);
		retval.put(416, 0);
		retval.put(417, 0);
		retval.put(418, 0);
		retval.put(421, 0);
		retval.put(422, 0);
		retval.put(423, 0);
		retval.put(424, 0);
		retval.put(426, 0);
		retval.put(428, 0);
		retval.put(429, 0);
		retval.put(431, 0);
		retval.put(451, 0);

		//Server
		retval.put(500, 0);
		retval.put(501, 0);
		retval.put(502, 0);
		retval.put(503, 0); 
		retval.put(504, 0); 
		retval.put(505, 0); 
		retval.put(506, 0);
		retval.put(507, 0); 
		retval.put(508, 0); 
		retval.put(510, 0); 
		retval.put(511, 0);

		return retval;
	}
	public boolean Error(int code, HashMap<Integer, Integer> error) throws Exception
	{
		int cnt = -1;
		
		switch(code)
		{
		case 401:
			cnt = error.get(code);
			if(cnt == 5)
				throw new HttpUnauthorizedException();
			
			if(cnt == 0)
			{
				if(this.jwtToken != this.jwtReToken)
				{
					this.jwtToken = this.jwtReToken;
				}
				else
				{
					this.ERALogin();
				}
			}
			else
			{
				this.ERALogin();				
			}
			
			error.put(code, ++cnt);
			return true;

		}
		
		
		return false;
	}
	public JSONObject uploadFile(String strUrl, byte[] fileData, String fileName, String strTitle)
	{
		JSONObject retval = new JSONObject();
        String CRLF = "\r\n";
        //String TWO_HYPHENS = "";
        String TWO_HYPHENS = "--";
        String BOUNDARY = "---------------------------012345678901234567890123456";
        HttpsURLConnection conn = null;
        DataOutputStream dos = null;

        // Request
        try 
        {
            //URL url = new URL(strUrl);
            //conn = (HttpsURLConnection) url.openConnection();
            conn = getHttpsConnection(strUrl, false);
            
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" +
                                      BOUNDARY);
            //conn.setRequestProperty("Authorization", "Bearer " + ACCESS_TOKEN);
            conn.setRequestProperty("Cache-Control", "no-cache");

            dos = new DataOutputStream(conn.getOutputStream());

            //파일 추가
            dos.writeBytes(TWO_HYPHENS + BOUNDARY + CRLF);
            dos.writeBytes("Content-Disposition: form-data; name=\"file\";" +
                            " filename=\"" + fileName + "\"" + CRLF);
            dos.writeBytes(CRLF);
            dos.write(fileData);
            dos.writeBytes(CRLF);
            
            //title 추가
            dos.writeBytes(TWO_HYPHENS + BOUNDARY + CRLF);
            dos.writeBytes("Content-Disposition: form-data; name=\"title\";" + CRLF);
            dos.writeBytes(CRLF);
            dos.writeBytes(strTitle);
            dos.writeBytes(CRLF);

            // finish delimiter
            dos.writeBytes(TWO_HYPHENS + BOUNDARY + TWO_HYPHENS + CRLF);

            //fis.close();
            dos.flush();
            dos.close();

        }
        catch (Exception e) 
        {
        	try
        	{
	        	if (dos != null)
	        	{ 
	        		dos.close(); 
	        	}
        	}
        	catch(Exception ex) {}
        	
        	retval.put("success", "N");
        	retval.put("msg", e.getMessage());
        	
        	return retval;
        }

        // Response
        InputStream inputStream = null;
        BufferedReader reader = null;
        try 
        {
            inputStream = new BufferedInputStream(conn.getInputStream());
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            StringBuilder builder = new StringBuilder();
            while ((line = reader.readLine()) != null) 
            {
                builder.append(line);
            }
            
            reader.close();
            inputStream.close();            
            conn.disconnect();
            
            retval.put("success", "Y");
            retval.put("code", conn.getResponseCode());
            retval.put("msg", builder);
            
            return retval;
        } 
        catch (Exception e) 
        {
        	try
        	{
	        	if(reader != null)
	        		reader.close();
	        	
	        	if(inputStream != null)
	        		inputStream.close();            
	            
	        	if(conn != null)
	        		conn.disconnect();
        	}
        	catch(Exception ex)
        	{}
        	
        	retval.put("success", "N");
        	retval.put("msg", e.getMessage());
        	
           return retval;
        }
    }
	
	
	public JSONObject requestHttpPostFile(String strUrl, JSONObject param, HashMap<String, String> headers) 
	{
		StringBuffer response = new StringBuffer();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		JSONObject retval = new JSONObject();
		HashMap<Integer, Integer> error = initErrorData();
		
		int responseCode = 0;
		try 
		{
			HttpsURLConnection con = this.getHttpsConnection(this.getIP() + strUrl, false);	
			if(headers != null)
			{
				Set<String> keys = headers.keySet(); 
				for(String key : keys)
				{
					String item = headers.get(key);
					con.setRequestProperty(key, item);
				}
			}
			
			if(param != null)
			{
				OutputStream os = con.getOutputStream();
				os.write(param.toString().getBytes());
				
				os.flush();
				os.close();
			}
			
			responseCode = con.getResponseCode();
			
			/*=============================================*/
			// api 재로그인 소스 (추가자 : 김진열)
			if(responseCode == 401) {
				this.Error(responseCode, error);
				
				this.getAccessToken();
				
				con = this.getHttpsConnection(this.getIP() + strUrl, false);	
				if(headers != null)
				{
					Set<String> keys = headers.keySet(); 
					for(String key : keys)
					{
						String item = headers.get(key);
						con.setRequestProperty(key, item);
					}
				}
				
				if(param != null)
				{
					OutputStream os = con.getOutputStream();
					os.write(param.toString().getBytes());
					
					os.flush();
					os.close();
				}
				
				responseCode = con.getResponseCode();
			}
			/*=============================================*/
			
			InputStream is = con.getInputStream();
			
			byte[] buffer = new byte[1024];
			int readBytes;
			while ((readBytes = is.read(buffer)) != -1) 
			{
				baos.write(buffer, 0, readBytes);
			}
			
			is.close();
			
			retval.put("success", "Y");
			retval.put("fileName", con.getHeaderField("Content-Disposition"));
			retval.put("fileData", baos.toByteArray());
			baos.close();
		}
		catch (Exception e) 
		{
			e.printStackTrace();
			retval.put("success", "N");
			retval.put("response", responseCode);
			retval.put("msg", e.getMessage());
		}
		
		
		return retval;
	}
	public JSONObject uploadInfra(String strUrl, byte[] fileData, String fileName, String docNo, String role)
	{
		JSONObject retval = new JSONObject();
        String CRLF = "\r\n";
        //String TWO_HYPHENS = "--";
        String TWO_HYPHENS = "";
        String BOUNDARY = "---------------------------012345678901234567890123456";
        HttpsURLConnection conn = null;
        DataOutputStream dos = null;
        int responseCode = 0;
        
        // Request
        try 
        {
            URL url = new URL(strUrl);
            conn = getHttpsConnection(strUrl, false);
            
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" +
                                      BOUNDARY);
            conn.setRequestProperty("Cache-Control", "no-cache");

            dos = new DataOutputStream(conn.getOutputStream());

            //파일 추가
            dos.writeBytes(TWO_HYPHENS + BOUNDARY + CRLF);
            dos.writeBytes("Content-Disposition: form-data; name=\"file\";" +
                            " filename=\"" + fileName + "\"" + CRLF);
            dos.writeBytes(CRLF);
            dos.writeBytes(CRLF);
            dos.write(fileData);
            dos.writeBytes(CRLF);

            dos.writeBytes(TWO_HYPHENS + BOUNDARY + CRLF);
            dos.writeBytes("Content-Disposition: form-data; name=\"docNo\";" + CRLF);
            dos.writeBytes(CRLF);
            dos.writeBytes(docNo);
            
            dos.writeBytes(TWO_HYPHENS + BOUNDARY + CRLF);
            dos.writeBytes("Content-Disposition: form-data; name=\"role\";" + CRLF);
            dos.writeBytes(CRLF);
            dos.writeBytes(role);
            dos.writeBytes(CRLF);
            
            

            // finish delimiter
            dos.writeBytes(TWO_HYPHENS + BOUNDARY + TWO_HYPHENS + CRLF);

            //fis.close();
            dos.flush();
            dos.close();

        }
        catch (Exception e) 
        {
        	try
        	{
	        	if (dos != null)
	        	{ 
	        		dos.close(); 
	        	}
        	}
        	catch(Exception ex) {}
        	
        	retval.put("success", "N");
        	retval.put("msg", e.getMessage());
        	
        	return retval;
        }

        // Response
        InputStream inputStream = null;
        BufferedReader reader = null;
        try 
        {
        	InputStream is = conn.getInputStream();
           
            if(is != null)
            { 
            	inputStream = new BufferedInputStream(is);
	            reader = new BufferedReader(new InputStreamReader(inputStream));
	            String line;
	            StringBuilder builder = new StringBuilder();
	            while ((line = reader.readLine()) != null) 
	            {
	                builder.append(line);
	            }
	            
	            reader.close();
	            inputStream.close();            
	            conn.disconnect();
	            
	            retval.put("success", "Y");
	            retval.put("code", conn.getResponseCode());
	            retval.put("msg", builder.toString());
            }
            else
            {
            	retval.put("success", "Y");
	            retval.put("code", conn.getResponseCode());
	            retval.put("msg", "");
            }
            
            
            return retval;
        } 
        catch (Exception e) 
        {
        	try
        	{
        		retval.put("code", conn.getResponseCode());
        		
	        	if(reader != null)
	        		reader.close();
	        	
	        	if(inputStream != null)
	        		inputStream.close();            
	            
	        	if(conn != null)
	        		conn.disconnect();
        	}
        	catch(Exception ex)
        	{}
        	
        	retval.put("success", "N");
        	retval.put("msg", e.getMessage());
        	
           return retval;
        }
    }
	
	public static void setHeaderAccept(HttpsURLConnection con, String value)
	{
		con.setRequestProperty("Accept", value);
	}
	public static void setHeaderContentType(HttpsURLConnection con, String value)
	{
		con.setRequestProperty("Content-Type", value);
	}
	
	public JSONObject requestHttpGetFile(String strUrl, HashMap<String, String> headers) 
	{
		StringBuffer response = new StringBuffer();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		JSONObject retval = new JSONObject();
		HashMap<Integer, Integer> error = initErrorData();
		
		int responseCode = 0;
		try 
		{
			HttpsURLConnection con = this.getHttpsConnection(IP + strUrl, true);
			if(headers != null)
			{
				Set<String> keys = headers.keySet(); 
				for(String key : keys)
				{
					String item = headers.get(key);
					con.setRequestProperty(key, item);
				}
			}
			
			responseCode = con.getResponseCode();
			
			/*=============================================*/
			// api 재로그인 소스 (추가자 : 김진열)
			if(responseCode == 401) {
				this.Error(responseCode, error);
				
				this.getAccessToken();
				
				con = this.getHttpsConnection(IP + strUrl, true);
				if(headers != null)
				{
					Set<String> keys = headers.keySet(); 
					for(String key : keys)
					{
						String item = headers.get(key);
						con.setRequestProperty(key, item);
					}
				}
				
				responseCode = con.getResponseCode();
			}
			/*=============================================*/
			
			InputStream is = con.getInputStream();
			
			byte[] buffer = new byte[1024];
			int readBytes;
			while ((readBytes = is.read(buffer)) != -1) 
			{
				baos.write(buffer, 0, readBytes);
			}
			
			is.close();
			
			retval.put("success", "Y");
			retval.put("fileName", con.getHeaderField("Content-Disposition"));
			retval.put("fileData", baos.toByteArray());
			baos.close();
		}
		catch (Exception e) 
		{
			retval.put("success", "N");
			retval.put("response", responseCode);
			retval.put("msg", e.getMessage());
		}
		
		
		return retval;
	}
	
	///*
	public static OkHttpClient getUnsafeOkHttpClient() 
	{
		  try 
		  {
		    // Create a trust manager that does not validate certificate chains
		    final TrustManager[] trustAllCerts = new TrustManager[] {
		        new X509TrustManager() {
		          @Override
		          public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
		          }

		          @Override
		          public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
		          }

		          @Override
		          public java.security.cert.X509Certificate[] getAcceptedIssuers() {
		            return new java.security.cert.X509Certificate[]{};
		          }
		        }
		    };

		    // Install the all-trusting trust manager
		    final SSLContext sslContext = SSLContext.getInstance("SSL");
		    sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
		    
		    // Create an ssl socket factory with our all-trusting manager
		    final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

		    OkHttpClient.Builder builder = new OkHttpClient.Builder();
		    builder.sslSocketFactory(sslSocketFactory, (X509TrustManager)trustAllCerts[0]);
		    builder.hostnameVerifier(new HostnameVerifier() 
		    {
		      @Override
		      public boolean verify(String hostname, SSLSession session) 
		      {
		        return true;
		      }
		    });

		    OkHttpClient okHttpClient = builder.build();
		    return okHttpClient;
		  } 
		  catch (Exception e) 
		  {
		    throw new RuntimeException(e);
		  }
		}
		//*/
}
