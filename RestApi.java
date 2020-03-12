package com.decision.v2x.era.util.http;


import java.util.HashMap;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.decision.v2x.era.util.convert.RestApiParameter;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RestApi extends iRestApi
{
	protected HashMap<String, String> m_dlCertIdHeader = new HashMap<String, String>() ;

	public RestApi(String id, String pwd)
	{
		super(id, pwd);
		m_dlCertIdHeader.put("Accept", "multipart/form-data");
		m_dlCertIdHeader.put("Accept-Encoding", "gzip, deflate, br");
	}
	
	/**
	 * 요청조회
	 * @param reqest_hash - 요청 hash
	 * @return JSONObject - 응답 response
	 * @throws ParseException 
	 * @exception Exception
	 */
	public JSONObject reqList() throws Exception
	{		
		String id = this.m_ID;
		
		final String URL = "/api/requestlist";
		HashMap<String, String> header = new HashMap<String, String>();
		JSONParser parser = new JSONParser();
		JSONObject result = null;
		
		//header.put("Authorization", AUTHORIZATION_PREFIX + strJwtAccessToken);
		header.put("Content-Type", HEADERJSON);
		header.put("Accept", "*/*");
	
		String strResult = requestHttpGET(URL + "?id=" + id, header);
		return (JSONObject) parser.parse(strResult);
	}

	/**
	 * CSR 요청정보
	 * @param reqest_hash - 요청 hash
	 * @return JSONObject - 응답 response
	 * @exception Exception
	 */
	public JSONObject reqCSR(String request_hash) throws Exception
	{
		final String URL = "/api/csr/" + request_hash;
		HashMap<String, String> header = new HashMap<String, String>();
		
		header.put("Content-Type", HEADERJSON);
		header.put("Accept", "*/*");
		String strResult = requestHttpGET(URL, header);

		JSONParser jp = new JSONParser();
		
		//System.out.println(strResult);
		return (JSONObject) jp.parse(strResult);
	}

	/**
	 * CSR 요청정보 - 상세보기
	 * @param reqest_hash - 요청 hash
	 * @return JSONObject - 응답 response
	 * @exception Exception
	 */
	public JSONObject reqCSRDetail(String request_hash) throws Exception
	{
		final String URL = "/api/csr/decode/" + request_hash;
		HashMap<String, String> header = new HashMap<String, String>();
		
		header.put("Content-Type", HEADERJSON);
		header.put("Accept", "*/*");
		String strResult = requestHttpGET(URL, header);

		JSONParser jp = new JSONParser();
		
		return (JSONObject) jp.parse(strResult);
	}

	/**
	 * Bootstrap 정보
	 * @param reqest_hash - 요청 hash
	 * @return JSONObject - 응답 response
	 * @exception Exception
	 */
	public JSONObject reqBootstrap(String request_hash) throws Exception
	{
		final String URL = "/api/bootstrap/" + request_hash;
		HashMap<String, String> header = new HashMap<String, String>();
		
		header.put("Accpet", "*/*");
		//header.put("Authorization", AUTHORIZATION_PREFIX + strJwtAccessToken);
		
		
		String strResult = requestHttpGET(URL, header);
		
		JSONParser jp = new JSONParser();

		return (JSONObject) jp.parse(strResult);
	}

	/**
	 * Bootstrap Data 다운로드
	 * @param reqest_hash - 요청 hash
	 * @return JSONObject - 응답 response
	 * @exception Exception
	 */
	public JSONObject downloadBootstrap(String request_hash) throws Exception
	{
		final String URL = "/api/download/bootstrap/" + request_hash;
		HashMap<String, String> header = new HashMap<String, String>();
		
		header.put("Accpet", "*/*");
		return this.requestHttpGetFile(URL, header);
	}

	/**
	 * 등록인증서 내역조회
	 * @param public_key - 공용키
	 * @return JSONObject - 응답 response
	 * @exception Exception
	 */
	public JSONObject enrolList(String public_key, String strJwtAccessToken) throws Exception
	{
		final String URL = "/api/enrol/";
		String strResult = requestHttpGET(URL, public_key, null);
		
		JSONParser jp = new JSONParser();
		
		return (JSONObject) jp.parse(strResult);
	}

	public JSONObject downloadEnrol(String public_key) throws Exception
	{
		final String URL = "/api/download/enrol/" + public_key;
		
		
		return this.requestHttpGetFile(URL, null);
	}

	public JSONObject downloadEnrolSeedKey(String public_key, String strJwtAccessToken) throws Exception
	{
		final String URL = "/api/download/enrol/seedkey/";
		String strResult = requestHttpGET(URL, public_key, HEADERFORM);
		
		JSONParser jp = new JSONParser();
		
		return (JSONObject) jp.parse(strResult);
	}

	public JSONObject downloadEnrolContribution(String public_key, String strJwtAccessToken) throws Exception
	{
		final String URL = "/api/download/enrol/contribution/";
		String strResult = requestHttpGET(URL, public_key, HEADERFORM);
		
		JSONParser jp = new JSONParser();
		
		return (JSONObject) jp.parse(strResult);
	}

	public JSONObject certList(String public_key) throws Exception
	{
		final String URL = "/api/certs/" + public_key;
		HashMap<String, String> header = new HashMap<String, String>();
		JSONParser jp = new JSONParser();
		JSONObject retval = new JSONObject();
		
		header.put("Content-Type", HEADERJSON);
		header.put("Accept", HEADER_ACCEPT_JSON);
		
		try
		{
			String strResult = this.requestHttpGET(URL, header);
			JSONObject result = (JSONObject) jp.parse(strResult);
			
			retval = (JSONObject) jp.parse(strResult);
			retval.put("success", "Y");
			retval.put("msg", "");
		}
		catch(Exception e)
		{
			retval.put("success", "N");
			retval.put("msg", e.getMessage());
		}
		
		return retval;
	}

	public JSONObject downloadCertExpansion(String public_key, String strJwtAccessToken) throws Exception
	{
		final String URL = "/api/download/expansion/";
		String strResult = requestHttpGET(URL, public_key, HEADERFORM);
		
		JSONParser jp = new JSONParser();
		
		return (JSONObject) jp.parse(strResult);

	}

	public JSONObject downloadCertSeedKey(String public_key, String strJwtAccessToken) throws Exception
	{
		final String URL = "/api/download/signingkey/";
		String strResult = requestHttpGET(URL, public_key, HEADERFORM);
		
		JSONParser jp = new JSONParser();

		return (JSONObject) jp.parse(strResult);
	}

	public JSONObject downloadCertBundle(String public_key)
	{
		final String URL = "/api/download/certs/" + public_key;
		HashMap<String, String> header = new HashMap<String, String>();
		
		header.put("Accept", "multipart/form-data");
		header.put("Accept-Encoding", "gzip, deflate, br");
		
		return this.requestHttpGetFile(URL, header);
	}

	public JSONObject reqCertApplication(String public_key) throws Exception
	{
		final String URL = "/api/request/cert/application/" + public_key;
		HashMap<String, String> header = new HashMap<String, String>();
		
		//header.put("Authorization", AUTHORIZATION_PREFIX + strJwtAccessToken);
		
		String strResult = requestHttpGET(URL, header);
		
		JSONParser jp = new JSONParser();
		
		return (JSONObject) jp.parse(strResult);
	}

	public JSONObject reqCertPseudonym(String public_key) throws Exception
	{
		final String URL = "/api/request/cert/pseudonym/" + public_key;
		HashMap<String, String> header = new HashMap<String, String>();
		//header.put("Authorization", AUTHORIZATION_PREFIX + strJwtAccessToken);
		String strResult = requestHttpGET(URL, header);
		
		JSONParser jp = new JSONParser();

		return (JSONObject) jp.parse(strResult);
	}
	
	public JSONObject reqCertIdentification(String public_key) throws Exception
	{
		final String URL = "/api/request/cert/identification/" + public_key;
		HashMap<String, String> header = new HashMap<String, String>();
		//header.put("Authorization", AUTHORIZATION_PREFIX + strJwtAccessToken);
		
		
		String strResult = requestHttpGET(URL, header);
		
		JSONParser jp = new JSONParser();
		return (JSONObject) jp.parse(strResult);
	}

	public JSONObject downloadCertApplication(String cert_hash, String strJwtAccessToken) throws Exception
	{
		final String URL = "/api/download/cert/application/";
		String strResult = requestHttpGET(URL, cert_hash, HEADERFORM);
		
		JSONParser jp = new JSONParser();
		
		JSONObject result = null;
		return (JSONObject) jp.parse(strResult);
	}

	
	public JSONObject downloadCertPseudonym(String cert_hash, String strJwtAccessToken) throws Exception
	{
		final String URL = "/api/download/cert/pseudonym/";
		String strResult = requestHttpGET(URL, cert_hash, HEADERFORM);
		
		JSONParser jp = new JSONParser();
		
		JSONObject result = null;
		result = (JSONObject) jp.parse(strResult);

		
		return result;
	}

	
	public JSONObject downloadCertIdentification(String cert_hash, String strJwtAccessToken) throws Exception
	{
		/*
		final String URL = "/api/download/cert/identification/";
		String strResult = requestHttpGET(URL, cert_hash, HEADERFORM);		
		JSONParser jp = new JSONParser();		
		JSONObject result = null;
		
		try {
			result = (JSONObject) jp.parse(strResult);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return result;
		//*/
		
		final String URL = this.getIP() + "/api/download/cert/identification/" + cert_hash;
		
		return this.requestHttpFile(URL, true, this.m_dlCertIdHeader);		
	}

	
	public JSONObject downloadCertApplicationContribution(String cert_hash, String strJwtAccessToken) throws Exception
	{
		final String URL = "/api/download/svalue/application/";
		String strResult = requestHttpGET(URL, cert_hash, HEADERFORM);
		
		JSONParser jp = new JSONParser();
		
		JSONObject result = null;
		result = (JSONObject) jp.parse(strResult);

		return result;
	}

	
	public JSONObject downloadCertPseudonymContribution(String cert_hash, String strJwtAccessToken) throws Exception
	{
		final String URL = "/api/download/svalue/pseudonym/";
		String strResult = requestHttpGET(URL, cert_hash, HEADERFORM);
		
		JSONParser jp = new JSONParser();
		
		JSONObject result = null;
		result = (JSONObject) jp.parse(strResult);

		return result;
	}

	
	public JSONObject downloadCertIdentificationContribution(String cert_hash, String strJwtAccessToken) throws Exception
	{
		final String URL = "/api/download/svalue/identification/";
		String strResult = requestHttpGET(URL, cert_hash, HEADERFORM);
		
		JSONParser jp = new JSONParser();
		
		JSONObject result = null;
		result = (JSONObject) jp.parse(strResult);

		return result;
	}

	/*
	public JSONObject makeCSR(CertificationVO vo, String strJwtAccessToken) throws Exception
	{
		final String URL = "/api/request/csr";
		
		JSONObject result = null;
		//String strResult = requestHttpPOST(URL, request_hash, null);
		return result;
	}
*/
	public JSONObject makeCSR(JSONObject param) throws Exception
	{
		final String URL = "/api/request/csr";
		JSONParser jp = new JSONParser();
		JSONObject retval = new JSONObject();
		
		try
		{
			HashMap<String, String> header = new HashMap<String, String>();
			
			header.put("Accept", HEADER_ACCEPT_JSON);
			//header.put("Authorization", AUTHORIZATION_PREFIX + strJwtAccessToken);
			header.put("Content-Type", HEADERJSON);

			String strResult = requestHttpPOST(URL, param, header);
			
			//System.out.println(strResult);
			retval = (JSONObject) jp.parse(strResult);
			
			retval.put("success", "Y");
			retval.put("msg", "");
			

		}
		catch(Exception e)
		{
			retval.put("success", "Y");
			retval.put("msg", e.getMessage());
		}
		
		return retval;
	}

	
	public JSONObject CSRFileCheck(String request_hash, String strJwtAccessToken) throws Exception
	{
		final String URL = "/api/upload/csr";
		
		JSONObject result = null;
		//String strResult = requestHttpPOST(URL, request_hash, null);
		return result;
	}

	/* */
	public JSONObject CSRUploadBootstrap(byte[] fileData, String fileName, String strTitle) throws Exception
	{
		final String URL = "/api/upload/bootstrap";
		return this.uploadFile(this.getIP() + URL, fileData, fileName, strTitle);
	}

	/**
	 * 
	 * @return 반환된느 값에는 request_hash, public_key, enrol_hash, enrol_validity_start, enrol_validity_end, success, msg가 있다.
	 * */
	public JSONObject uploadCsr(byte[] fileData, String fileName, String strTitle) throws Exception
	{
		JSONObject result = new JSONObject();
		JSONObject retval = new JSONObject();
		final String URL = "/api/upload/bootstrap";
		result =  this.uploadFile(this.getIP() + URL, fileData, fileName, strTitle);
		
		//*
		String requestHash = "";
		String publicKey = "";
		String enrolValidityStart = "";
		String enrolValidityEnd = "";
		//*/
		
		JSONArray arr = null;
		
		if(result.get("success").equals("Y"))
		{
			//JSONArray arrRequestHash = new JSONArray();
			
			result = this.reqList();
			arr = (JSONArray)result.get("users");
			for(int i = 0; i<arr.size(); i++)
			{
				JSONObject item = (JSONObject)arr.get(i);
				String outTitle = item.get("title").toString();
				if(strTitle.equals(outTitle))
				{
					//arrRequestHash.add(item.get("request_hash").toString());
					requestHash = item.get("request_hash").toString();
					break;
				}
			}
			
			//if(arrRequestHash.size() == 0)
			if(requestHash.isEmpty())
			{
				retval.put("success", "N");
				retval.put("msg", strTitle + "이 CSR 목록에 없습니다.");
			}
			else
			{
				/*
				JSONArray arrCertResult = new JSONArray();
				for(int q = 0; q<arrRequestHash.size(); q++)
				{
					JSONObject certResult = new JSONObject();
					String requestHash = arrRequestHash.get(q).toString();
					result = this.reqBootstrap(requestHash);
					arr = (JSONArray)result.get("enrols");
					//for(int i = 0; i<arr.size(); i++)
					//{
					//	JSONObject item = (JSONObject)arr.get(i);
					//	String publicKey = item.get("public_key").toString();
					//	String enrolHash = item.get("enrol_hash").toString();
					//	String enrolValidityStart = item.get("enrol_validity_start").toString();
					//	String enrolValidityEnd = item.get("enrol_validity_end").toString();
					//	
					//}
					certResult.put("request_hash", requestHash);
					certResult.put("enrols", arr);
					arrCertResult.add(certResult);
				}
				retval.put("results", arrCertResult);
				retval.put("success", "Y");
				retval.put("msg", "");
				//*/
				
				//*
				result = this.reqBootstrap(requestHash);
				arr = (JSONArray)result.get("enrols");
				
				for(int i = 0; i<arr.size(); i++)
				{
					JSONObject item = (JSONObject)arr.get(i);
					
					publicKey = item.get("public_key").toString();
					enrolValidityStart = item.get("enrol_validity_start").toString();
					enrolValidityEnd = item.get("enrol_validity_end").toString();
					break;
				}
				
				if(publicKey.isEmpty() && 
						enrolValidityEnd.isEmpty() && enrolValidityEnd.isEmpty())
				{
					retval.put("success", "N");
					retval.put("msg", strTitle + "(" + requestHash + ")가 부트스트랩 정보에 없습니다.");
				}
				else
				{
					retval.put("success", "Y");
					retval.put("request_hash", requestHash);
					retval.put("public_key", publicKey);
					retval.put("enrol_validity_start", enrolValidityStart);
					retval.put("enrol_validity_end", enrolValidityEnd);
					retval.put("msg", "");
				}
				//*/
			}
		}
		else
		{
			return result;
		}
		
		return retval;
	}
	
	public JSONObject uploadInfraCsr(byte[] fileData, String fileName, String docNo, String role) throws Exception
	{
		final String URL = "/api/webui/cert/self/csr/upload";
		
		return this.uploadInfra(this.getIP() + URL, fileData, fileName, docNo, role);
	}
	public JSONObject uploadInfraCsrEx(byte[] fileData, String fileName, String docNo, String role)
	{
		final String URL = "/api/webui/cert/self/csr/upload";
		JSONObject retval = new JSONObject();
		String extName = "octet-stream";
		
		
		try
		{
			extName = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
		}
		catch(Exception e)
		{
			
		}
		
		
		
		try
		{
			OkHttpClient client = new OkHttpClient().newBuilder()
				  .build();
				MediaType mediaType = MediaType.parse("multipart/form-data");
				RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
				  .addFormDataPart("file",fileName,
				    RequestBody.create(MediaType.parse("application/octet-stream"),
				    		fileData))
				  .addFormDataPart("docNo", docNo)
				  .addFormDataPart("role", role.toUpperCase())
				  .build();
				
			Request request = new Request.Builder()
				  .url(this.getIP() + URL)
				  .method("POST", body)
				  .addHeader("Content-Type", "multipart/form-data")
				  .addHeader("Authorization", "Bearer " + this.jwtToken)
				  .build();
			
			Response response = client.newCall(request).execute();
				
			retval.put("success", "Y");
			retval.put("msg", "");
		}
		catch(Exception e)
		{
			retval.put("success", "Y");
			retval.put("msg", e.getMessage());
		}
		
		return retval;
	}
	
	public JSONObject decodeCertification(String octet_encode_cert_binary_hex_str, String strJwtAccessToken) throws Exception
	{
		final String URL = "/api/decode/cert/";
		
		JSONObject result = null;
		//String strResult = requestHttpGET(URL, request_hash, null);
		return result;
	}

	
	public JSONObject infraCertCount(JSONObject data) throws Exception
	{
		JSONParser parser = new JSONParser();
		final String URL = "/api/webui/cert/self/csr/cnt";
		JSONObject retval = new JSONObject();
		HashMap<String, String> header = new HashMap<String, String>();
		
		header.put("Accept", HEADER_ACCEPT_JSON);
		header.put("Content-Type", HEADERJSON);
		
		try
		{
			String strResult = requestHttpPOST(URL, data, header);
			
			retval.put("success", "Y");
			retval.put("msg", strResult);
			retval.put("result", parser.parse(strResult));
			
		}
		catch(Exception e)
		{
			retval.put("success", "N");
			retval.put("msg", e.getMessage());
		}
		
		return retval;
	}

	
	public JSONObject infraCertList(JSONObject data)
	{
		final String URL = "/api/webui/cert/self/csr/list";
		JSONParser parser = new JSONParser();
		JSONObject retval = new JSONObject();
		HashMap<String, String> header = new HashMap<String, String>();
		
		header.put("Accept", HEADER_ACCEPT_JSON);
		header.put("Content-Type", HEADERJSON);
		
		try
		{
			String strResult = requestHttpPOST(URL, data, header);
			
			retval.put("success", "Y");
			retval.put("msg", "");
			retval.put("result", parser.parse(strResult));
		}
		catch(Exception e)
		{
			retval.put("success", "N");
			retval.put("msg", e.getMessage());
		}
		
		return retval;
	}

	
	public JSONObject infraCertDetail(JSONObject data)
	{
		final String URL = "/api/webui/cert/self/csr/detail";
		JSONParser parser = new JSONParser();
		JSONObject retval = new JSONObject();
		HashMap<String, String> header = new HashMap<String, String>();
		
		header.put("Accept", HEADER_ACCEPT_JSON);
		header.put("Content-Type", HEADERJSON);
		
		try
		{
			String strResult = requestHttpPOST(URL, data, header);
			
			retval.put("success", "Y");
			retval.put("msg", strResult);
			retval.put("result", parser.parse(strResult));
		}
		catch(Exception e)
		{
			retval.put("success", "N");
			retval.put("msg", e.getMessage());
		}
		
		return retval;
	}

	
	public JSONObject infraCertCreate(JSONObject data)
	{
		JSONObject retval = new JSONObject();
		final String URL = "/api/webui/cert/self/csr/create";
		HashMap<String, String> header = new HashMap<String, String>();
		
		header.put("Accept", "*/*");
		header.put("Content-Type", HEADERJSON);
		
		try
		{
			String strResult = requestHttpPOST(URL, data, header);
			
			retval.put("success", "Y");
			retval.put("msg", strResult);
		}
		catch(Exception e)
		{
			retval.put("success", "N");
			retval.put("msg", e.getMessage());
		}
		
		return retval;
	}

	public JSONObject infraCertCreate(String data)
	{
		JSONObject retval = new JSONObject();
		final String URL = "/api/webui/cert/self/csr/create";
		HashMap<String, String> header = new HashMap<String, String>();
		
		header.put("Accept", "*/*");
		header.put("Content-Type", HEADERJSON);
		
		try
		{
			String strResult = requestHttpPOST(URL, data, header);
			
			retval.put("success", "Y");
			retval.put("msg", strResult);
		}
		catch(Exception e)
		{
			retval.put("success", "N");
			retval.put("msg", e.getMessage());
		}
		
		return retval;
	}

	public JSONObject downloadCertInfra(JSONObject data) throws Exception
	{
		final String URL = "/api/webui/cert/self/csr/download";
		HashMap<String, String> header = new HashMap<String, String>();
		
		//header.put("Accept", "*/*");
		header.put("Content-Type", HEADERJSON);
		return requestHttpPostFile(URL, data, header);
	}

	
	

	
	public JSONObject applyCertInfra(String docNo)
	{
		JSONObject retval = new JSONObject();
		RestApiParameter param = new RestApiParameter();
		final String URL = "/api/webui/cert/self/csr/apply";
		HashMap<String, String> header = new HashMap<String, String>();
		
		header.put("Content-Type", HEADERJSON);
		//header.put("Accept", HEADER_ACCEPT_JSON);
		
		try
		{
			JSONObject infraParam = param.infraCertApply(docNo);		
			String strResult = requestHttpPOST(URL, infraParam ,header);
			
			retval.put("success", "Y");
			retval.put("msg", strResult);
		}
		catch(Exception e)
		{
			retval.put("success", "N");
			retval.put("msg", e.getMessage());
		}
		
		return retval;
	}

	


	
	/**
	 * 보안인증서를 생성할 수 있는 함수
	 * @param jwt JWT 토큰 값
	 * @param id 인증서를 요청한 계정
	 * @param strType IDE, PSE, APP 중에 하나
	 * @param strTitle 인증서 이름 
	 * @param nCount 생성할 인증서의 개수
	 * @param arrPsid PSID로 int[]로 줘야한다.
	 * @param arrContryCode 국가코드로 410을 주면된다.
	 * @param arrCircularRegin arrContryCode가 값이 있을 경우는 null이다.
	 * @param strDurationUint HOURS 또는 YEARS로 주면된다.
	 * @param nDurationValue 4320 또는 3로 주되 strDurationUint에 맞게 줘야한다.
	 * @return JSON으로 파싱되었다. 반환되는 값에는 
	 *  download_url, wait_time(대기시간), request_date(요청날짜), download_time, title, 
	 *	public_key, request_hash, enrol_validity_start(등록인증서 시작일), enrol_validity_end(등록인증서 만료일)가 있다.
	 * 
	 * */
	private JSONObject createCertification(String id, String strType, String strTitle, int nCount, int[] arrPsid, int[] arrContryCode, int[] arrCircularRegin, String strDurationUint, int nDurationValue) throws Exception
	{
		RestApiParameter rap = new RestApiParameter();
		JSONObject retval = new JSONObject();		
		String requestHash = "";
		JSONObject makeCsrParam = rap.makeCSR(strTitle, strType, nCount, arrPsid, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue);	
		JSONObject result = this.makeCSR(makeCsrParam);
		String publicKey = "";
		String enrolValidityStart = "";
		String enrolValidityEnd = "";
		
		if(result.get("success").equals("N")) 
		{	
			return result;
		}
		
		result = this.reqList(strTitle);
		JSONArray arr = (JSONArray)result.get("users");
		
		for(int i = 0; i<arr.size(); i++)
		{
			JSONObject item = (JSONObject)arr.get(i);
			String outTitle = item.get("title").toString();
			if(strTitle.equals(outTitle))
			{
				requestHash = item.get("request_hash").toString();
				break;
			}
		}
		
		if(requestHash.isEmpty())
			throw new Exception("요청 목록을 가져오지 못했습니다.");
		
		result = this.reqBootstrap(requestHash);
		result = (JSONObject)((JSONArray)result.get("enrols")).get(0);
		publicKey = (String)result.get("public_key");
		enrolValidityStart = (String)result.get("enrol_validity_start");
		enrolValidityEnd = (String)result.get("enrol_validity_end");
		
		if(publicKey == null || enrolValidityEnd == null || enrolValidityStart == null)
			throw new Exception("부트스트랩 요청 실패");
		
		switch(strType)
		{
		case "IDE":
			result = this.reqCertIdentification(publicKey);
			break;
			
		case "APP":
			result = this.reqCertApplication(publicKey);
			break;
			
		case "PSE":
			result = this.reqCertPseudonym(publicKey);
			break;
			
		default:
			throw new Exception(strType + " => 인증서 타입이 존재하지 않습니다.");
		}
		
		
		
		
		
		JSONObject resultReq = (JSONObject)result.get("certs");
		retval.putAll(resultReq);
		//retval.put("download_url", resultReq.get("download_url"));
		//retval.put("wait_time", resultReq.get("wait_time"));
		//retval.put("request_date", resultReq.get("request_date"));		
		//retval.put("download_time", resultReq.get("download_time"));
		
		retval.put("success", "Y");
		retval.put("title", strTitle);
		retval.put("public_key", publicKey);
		retval.put("request_hash", requestHash);
		retval.put("enrol_validity_start", enrolValidityStart);
		retval.put("enrol_validity_end", enrolValidityEnd);
		
		return retval;
		
	}
	
	public JSONObject createCertApp(String strTitle, int nCount, int[] arrPsid, int[] arrContryCode, int[] arrCircularRegin, String strDurationUint, int nDurationValue) throws Exception
	{
		return this.createCertification(this.m_ID, "APP", strTitle, nCount, arrPsid, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue);
	}
	
	public JSONObject createCertIde(String strTitle, int nCount, int[] arrPsid, int[] arrContryCode, int[] arrCircularRegin, String strDurationUint, int nDurationValue) throws Exception
	{
		return this.createCertification(this.m_ID, "IDE", strTitle, nCount, arrPsid, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue);
	}
	
	public JSONObject createCertPse(String strTitle, int nCount, int[] arrPsid, int[] arrContryCode, int[] arrCircularRegin, String strDurationUint, int nDurationValue) throws Exception
	{
		return this.createCertification(this.m_ID, "PSE", strTitle, nCount, arrPsid, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue);
	}
	
	
	public JSONObject insertUser(String id, String pw)
	{
		JSONObject retval = new JSONObject();
		HashMap<String, String> header = new HashMap<String, String>();
		
		header.put("Accept", HEADER_ACCEPT_JSON);
		header.put("Content-Type", HEADERJSON);
		
		//UTF-8
		try
		{
			RestApiParameter apiParam = new RestApiParameter();
			final String URL = "/api/webui/mng/user/insert";
			JSONObject param = apiParam.insertUser(id, pw);

			this.requestHttpPOST(URL, param, header);
			retval.put("success", "Y");
			retval.put("msg", "");
		}
		catch(Exception e)
		{
			retval.put("success", "N");
			retval.put("msg", e);
		}
		
		return retval;
		
	}
	public JSONObject deleteUser(String id)
	{
		JSONObject retval = new JSONObject();
		HashMap<String, String> header = new HashMap<String, String>();
		
		header.put("Accept", HEADER_ACCEPT_JSON);
		header.put("Content-Type", HEADERJSON);
		
		try
		{	
			final String URL = "/api/webui/mng/user/delete";
			RestApiParameter apiParam = new RestApiParameter();
			JSONObject param = apiParam.deleteUser(id);

			this.requestHttpPOST(URL, param, header);
			retval.put("success", "Y");
			retval.put("msg", "");
		}
		catch(Exception e)
		{
			retval.put("success", "N");
			retval.put("msg", e);
		}
		

		
		return retval;
		
	}
	public JSONObject insertAuth(String id)
	{
		JSONObject retval = new JSONObject();
		HashMap<String, String> header = new HashMap<String, String>();
		
		header.put("Accept", HEADER_ACCEPT_JSON);
		header.put("Content-Type", HEADERJSON);
		
		try
		{
			final String URL = "/api/webui/mng/authuser/insert";
			RestApiParameter apiParam = new RestApiParameter();
			JSONObject param = apiParam.insertAuth(id);

			this.requestHttpPOST(URL, param, header);
			retval.put("success", "Y");
			retval.put("msg", "");
		}
		catch(Exception e)
		{
			retval.put("success", "N");
			retval.put("msg", e);
		}

		
		return retval;
		
	}
	public JSONObject deleteAuth(String id)
	{
		JSONObject retval = new JSONObject();
		HashMap<String, String> header = new HashMap<String, String>();
		
		header.put("Accept", HEADER_ACCEPT_JSON);
		header.put("Content-Type", HEADERJSON);
		
		try
		{
			final String URL = "/api/webui/mng/authuser/delete";
			RestApiParameter apiParam = new RestApiParameter();
			JSONObject param = apiParam.deleteAuth(id);

			this.requestHttpPOST(URL, param, header);
			retval.put("success", "Y");
			retval.put("msg", "");
		}
		catch(Exception e)
		{
			retval.put("success", "N");
			retval.put("msg", e);
		}

		
		return retval;
	}

	public JSONObject changeUser(String id, String pwd, String nm, String useYN)
	{
		JSONObject retval = new JSONObject();
		final String URL = "/api/webui/mng/user/update";
		HashMap<String, String> header = new HashMap<String, String>();
		
		header.put("Accept", HEADER_ACCEPT_JSON);
		header.put("Content-Type", HEADERJSON);
		
		try
		{
			RestApiParameter apiParam = new RestApiParameter();
			JSONObject param = apiParam.changeUser(id, nm, pwd, useYN);

			//System.out.println(param);
			
			this.requestHttpPOST(URL, param, header);
			retval.put("success", "Y");
			retval.put("msg", "");
		}
		catch(Exception e)
		{
			retval.put("success", "N");
			retval.put("msg", e.getMessage());
		}

		
		return retval;
	}
	public JSONObject changeUser(JSONObject param)
	{
		JSONObject retval = new JSONObject();
		final String URL = "/api/webui/mng/user/update";
		HashMap<String, String> header = new HashMap<String, String>();
		
		header.put("Accept", HEADER_ACCEPT_JSON);
		header.put("Content-Type", HEADERJSON);
		
		try
		{
			this.requestHttpPOST(URL, param, header);
			retval.put("success", "Y");
			retval.put("msg", "");
		}
		catch(Exception e)
		{
			retval.put("success", "N");
			retval.put("msg", e.getMessage());
		}

		
		return retval;
	}
	/*
	 * blacklist에 등록인증서를 등록한다.
	 * */
	public JSONObject certBlacklist(String publicKey)
	{
		JSONObject retval = new JSONObject();
		final String URL = "/api/webui/cert/enr/revoke/" + publicKey;
		HashMap<String, String> header = new HashMap<String, String>();
		
		header.put("Accept", HEADER_ACCEPT_JSON);
		header.put("Content-Type", HEADERJSON);
		
		try
		{
			String result = this.requestHttpPOST(URL, (JSONObject)null, header);
			retval.put("success", "Y");
			retval.put("msg", result);
		}
		catch(Exception e)
		{
			retval.put("success", "N");
			retval.put("msg", e.getMessage());
		}

		return retval;
	}
	
	/*
	 * Backlist에 등록된 인증서를 제거한다.
	 * */
	public JSONObject certBlacklistRemove(String publicKey)
	{
		JSONObject retval = new JSONObject();
		final String URL = "/api/webui/cert/enr/revoke/insert/" + publicKey;
		HashMap<String, String> header = new HashMap<String, String>();
		
		header.put("Accept", HEADER_ACCEPT_JSON);
		header.put("Content-Type", HEADERJSON);
		
		try
		{
			String result = this.requestHttpPOST(URL, (JSONObject)null, header);
			retval.put("success", "Y");
			retval.put("msg", result);
		}
		catch(Exception e)
		{
			retval.put("success", "N");
			retval.put("msg", e.getMessage());
		}

		return retval;
	}
	
	/*
	 * 등록 인증서를 위한 CSR를 생성하는 API
	 * */
	public JSONObject makeOER(JSONObject param) 
	{
		final String URL = "/api/request/enr/csr";
		HashMap<String, String> header = new HashMap<String, String>();
		
		//header.put("Accept", HEADER_ACCEPT_JSON);
		header.put("Content-Type", HEADERJSON);
		
		
		return requestHttpPostFile(URL, param, header);
		
		/*
		final String URL = "/api/request/enr/csr";
		JSONParser jp = new JSONParser();
		JSONObject retval = new JSONObject();
		
		try
		{
			HashMap<String, String> header = new HashMap<String, String>();
			
			header.put("Accept", HEADER_ACCEPT_JSON);
			header.put("Content-Type", HEADERJSON);

			String strResult = requestHttpPOST(URL, param, header);
			
			//System.out.println(strResult);
			retval = (JSONObject) jp.parse(strResult);
			
			retval.put("success", "Y");
			retval.put("msg", "");
			

		}
		catch(Exception e)
		{
			retval.put("success", "N");
			retval.put("msg", e.getMessage());
		}
		
		return retval;
		*/
	}
	
	public JSONObject privateKeyDownload(String title) throws Exception
	{
		final String oerPrefix = "OER";
		JSONObject resultReqList = this.reqList(title + oerPrefix);
		JSONArray arr = (JSONArray)resultReqList.get("users"); 
		Object requestHash = null;
		
		for(int i = 0; i<arr.size(); i++)
		{
			JSONObject item = (JSONObject)arr.get(i);	
			requestHash = item.get("request_hash");
			break;
		}
		
		if(requestHash != null)
		{
			final String URL = "/api/download/csr/seedkey/" + requestHash.toString();
			HashMap<String, String> header = new HashMap<String, String>();
	
			header.put("Accept", "*/*");
			//header.put("Content-Type", HEADERJSON);
			return requestHttpPostFile(URL, null, header);
		}
		else
		{
			JSONObject err = new JSONObject();
			
			err.put("success", "N");
			err.put("msg", "잘못된 request hash입니다.");
			
			return err;
			
		}
	}

	
	
	public JSONObject reqList(String title) throws Exception
	{		
		final String URL = "/api/requestlist/" + title;
		HashMap<String, String> header = new HashMap<String, String>();
		JSONParser parser = new JSONParser();
		JSONObject result = null;
		
		//header.put("Authorization", AUTHORIZATION_PREFIX + strJwtAccessToken);
		header.put("Content-Type", HEADERJSON);
		header.put("Accept", "*/*");
	
		String strResult = requestHttpPOST(URL, (JSONObject)null, header);
		return (JSONObject) parser.parse(strResult);
	}
	
	public JSONObject getKeys(String title)
	{
		JSONObject retval = new JSONObject();
	
		try
		{
			JSONObject resultReqList = this.reqList(title);
			JSONArray arr = (JSONArray)resultReqList.get("users"); 
		
			for(int i = 0; i<arr.size(); i++)
			{
				JSONObject item = (JSONObject)arr.get(i);	
				String requestHash = item.get("request_hash").toString();
				retval.put("request_hash", requestHash);
				
				JSONObject resultBpList = this.reqBootstrap(requestHash);
				JSONArray arrEnrol = (JSONArray)resultBpList.get("enrols");
				for(int z = 0; z<arrEnrol.size(); z++)
				{
					JSONObject enrolItem = (JSONObject)arrEnrol.get(z);
					retval.put("public_key", enrolItem.get("public_key").toString());
					retval.put("enrol_validity_start", enrolItem.get("enrol_validity_start").toString());
					retval.put("enrol_validity_end", enrolItem.get("enrol_validity_end").toString());
				}
			}			
			
			retval.put("success", "Y");
			retval.put("msg", "");
		}
		catch(Exception e)
		{
			retval.put("success", "N");
			retval.put("msg", e.toString());
		}
		
		
		
		
		return retval;
	}
	
	
	
	public JSONObject makeEnrol(String strTitle, int nCount, int[] arrPsid, int[] arrContryCode, int[] arrCircularRegin, String strDurationUint, int nDurationValue)
	{
		String oerPrefix = "OER";
		RestApiParameter paramHelper = new RestApiParameter();
		JSONObject param = paramHelper.makeOER(strTitle + oerPrefix, nCount, arrPsid, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue);
		JSONObject oerResult = this.makeOER(param);
		
		try
		{
			if(oerResult.get("success").equals("Y"))
			{
				String fileName = (String)oerResult.get("fileName");
				byte[] fileData = (byte[])oerResult.get("fileData");
			
				JSONObject retval = this.uploadCsr(fileData, fileName, strTitle);
				if(retval.get("success").equals("Y"))
				{
					retval.put("oer_title", strTitle + oerPrefix);
					
					return retval;	
				}
				else
				{
					JSONObject error = new JSONObject();
					
					error.put("success", "N");
					error.put("msg", retval.get("msg").toString());
					
					
					return error;
				}
			}
			else
			{
				JSONObject retval = new JSONObject();
				
				retval.put("success", "N");
				retval.put("msg", oerResult.get("msg").toString());
				
				
				return retval;
			}
		}
		catch(Exception e)
		{
			JSONObject retval = new JSONObject();
			
			retval.put("success", "N");
			retval.put("msg", e.toString());
			
			
			return retval;
		}
		
	}
	
}

