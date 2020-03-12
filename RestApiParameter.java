package com.decision.v2x.era.util.convert;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.json.simple.*;

public class RestApiParameter 
{
	
	
	
	public static String localtimeToUTC(String inputdatetime, TimeZone tz) throws Exception
	{
		String utcTime = null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		if(tz == null)
			tz = TimeZone.getDefault();
  
		
		
		try 
		{
		    Date parseDate = sdf.parse(inputdatetime);
		    long milliseconds = parseDate.getTime();
		    int offset = tz.getOffset(milliseconds);
		    utcTime = sdf.format(milliseconds - offset);
		    utcTime = utcTime.replace("+0000", "");
		} 
		catch (Exception e) 
		{
			return "";
		}
	      
	    return utcTime;
	}

	
	public static String dateToString(Date item)
	{
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return format.format(item);
	}
	
	// docmlogin(jwt token 얻기)
	public static JSONObject dcmLogin(String id, String pw)
	{
		JSONObject retval = new JSONObject();
		
		retval.put("user_id", id);
		retval.put("user_pw", pw);		
		
		return retval;		
	}
		
	public static JSONArray toArray(int[] arr)
	{
		if(arr == null)
			return null;
		
		JSONArray retval = new JSONArray();

		for(int i = 0; i<arr.length; i++)
		{
			retval.add(arr[i]);
		}
	
		return retval;		
	}
	
	
	public static JSONObject makeCSR(String strTitle, String strType, int nCount, int[] arrPsid, int[] arrContryCode, int[] arrCircularRegin, String strDurationUint, int nDurationValue)
	{
		JSONObject retval = new JSONObject();
		JSONObject validityPeriod = new JSONObject();
		JSONObject duration = new JSONObject();
		
		retval.put("title", strTitle);
		retval.put("type", strType);
		retval.put("count", nCount);
		retval.put("psids", toArray(arrPsid));
		retval.put("country_code", toArray(arrContryCode));
		retval.put("circular_region", toArray(arrCircularRegin));
				
		duration.put("unit", strDurationUint);
		duration.put("value", nDurationValue);
		validityPeriod.put("duration", duration);		
		retval.put("validity_period", validityPeriod);
		
		
		return retval;		
	}
	
	/**
	 * @param startDate yyyy-MM-dd HH:mm:ss
	 * 
	 * */
	public static JSONObject infraCertCreate(String hostname, String startDate, String durationUnit, String durationUnitValue, String keyId)
	{
		//depth: 1
		JSONObject retval = new JSONObject();
		
		//depth: 2
		JSONObject data = new JSONObject();
		JSONObject verificationKey = new JSONObject();
		
		//depth: 3
		JSONObject certId = new JSONObject();
		JSONObject validityPeriod = new JSONObject();
		JSONObject region = new JSONObject();
		JSONObject permissions = new JSONObject();
		
		//depth: 4
		JSONObject vpDutaion = new JSONObject();
		
		JSONArray regionIr = new JSONArray();
		JSONObject regionIrItem = new JSONObject();
	
		JSONArray arrApp = new JSONArray();
		JSONArray arrReq = new JSONArray();
		JSONArray arrIssue = new JSONArray();
		JSONObject arrAppItem = new JSONObject();
		JSONObject arrReqItem = new JSONObject();
		JSONObject arrReqItemSp = new JSONObject();
		JSONArray arrReqItemEetype = new JSONArray();
		
		certId.put("type", "hostname");
		certId.put("value", hostname);
		data.put("cert_id", certId);
		
		vpDutaion.put("unit", durationUnit);
		vpDutaion.put("value", durationUnitValue);
		validityPeriod.put("begin", startDate);
		validityPeriod.put("duration", vpDutaion);
		data.put("validity_period", validityPeriod);
		
		regionIrItem.put("region_type", "COUNTRYONLY");
		regionIrItem.put("country_only", 410);
		regionIr.add(regionIrItem);
		region.put("identified_regions", regionIr);
		region.put("region_type", "IDENTIFIEDREGIONS");
		data.put("region", region);
		
		arrAppItem.put("seq", 1);
		arrAppItem.put("psid", 35);
		arrAppItem.put("psid_user", "");
		arrAppItem.put("ssp", "870001");
		arrAppItem.put("ssp_user", "");
		arrApp.add(arrAppItem);
		
		
		arrReqItemEetype.add("enrol");
		arrReqItemSp.put("range", "all");
		arrReqItem.put("seq", 1);
		arrReqItem.put("min_chain_depth", "0");
		arrReqItem.put("chain_depth_range", "0");
		arrReqItem.put("subject_permissions", arrReqItemSp);
		arrReqItem.put("eetype", arrReqItemEetype);
		arrReq.add(arrReqItem);
		
		permissions.put("app", arrApp);
		permissions.put("req", arrReq);
		permissions.put("issue", arrIssue);
		data.put("permissions", permissions);
		
		
		data.put("doc_no", "");
		data.put("crl_series", 2);

		verificationKey.put("key_id", keyId);
		data.put("verification_key", verificationKey);
		retval.put("data", data);
		
		
		return retval;		
	}
	
	public static JSONObject infraCertCreate(String hostname, String startDate, String durationUnit, String durationUnitValue, String keyId,
			JSONArray app, JSONArray req, JSONArray issue)
	{
		//depth: 1
		JSONObject retval = new JSONObject();
		
		//depth: 2
		JSONObject data = new JSONObject();
		JSONObject verificationKey = new JSONObject();
		
		//depth: 3
		JSONObject certId = new JSONObject();
		JSONObject validityPeriod = new JSONObject();
		JSONObject region = new JSONObject();
		JSONObject permissions = new JSONObject();
		
		//depth: 4
		JSONObject vpDutaion = new JSONObject();
		
		JSONArray regionIr = new JSONArray();
		JSONObject regionIrItem = new JSONObject();
	
		JSONObject arrAppItem = new JSONObject();
		JSONObject arrReqItem = new JSONObject();
		JSONObject arrReqItemSp = new JSONObject();
		JSONArray arrReqItemEetype = new JSONArray();
		
		certId.put("type", "hostname");
		certId.put("value", hostname);
		data.put("cert_id", certId);
		
		vpDutaion.put("unit", durationUnit);
		vpDutaion.put("value", durationUnitValue);
		validityPeriod.put("begin", startDate);
		validityPeriod.put("duration", vpDutaion);
		data.put("validity_period", validityPeriod);
		
		regionIrItem.put("region_type", "COUNTRYONLY");
		regionIrItem.put("country_only", 410);
		regionIr.add(regionIrItem);
		region.put("identified_regions", regionIr);
		region.put("region_type", "IDENTIFIEDREGIONS");
		data.put("region", region);
		
		permissions.put("app", app);
		permissions.put("req", req);
		permissions.put("issue", issue);
		data.put("permissions", permissions);
		
		
		data.put("doc_no", "");
		data.put("crl_series", 2);

		verificationKey.put("key_id", keyId);
		data.put("verification_key", verificationKey);
		retval.put("data", data);
		
		
		return retval;		
	}
	
	public static JSONObject infraCertList(String startTime, String endTime, String docNo, String hostName, String pagePerData, String page)
	{
		JSONObject retval = new JSONObject();
		JSONObject data = new JSONObject();
		
		data.put("start_time", startTime);
		data.put("end_time", endTime);
		data.put("doc_no", docNo);
		data.put("hostname", hostName);
		data.put("page_per_data", Integer.parseInt(pagePerData));
		data.put("page", Integer.parseInt(page));
		
		retval.put("data", data);
		
		return retval;
	}
	public static JSONObject infraCertList(String startTime, String endTime, String docNo, String hostName, int pagePerData, int page)
	{
		JSONObject retval = new JSONObject();
		JSONObject data = new JSONObject();
		
		data.put("start_time", startTime);
		data.put("end_time", endTime);
		data.put("doc_no", docNo);
		data.put("hostname", hostName);
		data.put("page_per_data", pagePerData);
		data.put("page", page);
		
		retval.put("data", data);
		
		return retval;
	}
	
	public static JSONObject infraCertCnt(String startTime, String endTime, String docNo, String hostName)
	{
		JSONObject retval = new JSONObject();
		JSONObject data = new JSONObject();
		
		data.put("start_time", startTime);
		data.put("end_time", endTime);
		data.put("doc_no", docNo);
		data.put("hostname", hostName);

		retval.put("data", data);
		
		return retval;
	}
	public static JSONObject infraCertApply(String docNo)
	{
		JSONObject retval = new JSONObject();
		JSONObject data = new JSONObject();
		
		data.put("doc_no", docNo);
		retval.put("data", data);
		
		return retval;		
	}
	public static JSONObject infraCertDetail(String docNo, String hostname, String startTime, String endTime)
	{
		JSONObject retval = new JSONObject();
		JSONObject data = new JSONObject();

	    data.put("doc_no", docNo);
		data.put("hostname", hostname);
		data.put("start_time", startTime);
		data.put("end_time", endTime);
		retval.put("data", data);
		
		return retval;		
	}
	
	public static JSONObject insertUser(String id, String pw)
	{
		JSONObject retval = new JSONObject();
		JSONObject data = new JSONObject();

		data.put("user_id", id);
		data.put("user_nm", "EraAdmin");
		data.put("user_pw", pw);
		data.put("use_yn", "Y");
		retval.put("data", data);
		
		return retval;
	}
	
	public static JSONObject deleteUser(String id)
	{
		JSONObject retval = new JSONObject();
		JSONObject data = new JSONObject();

		data.put("user_id", id);
		retval.put("data", data);
		
		return retval;
	}
	public static JSONObject insertAuth(String id)
	{
		JSONObject retval = new JSONObject();
		JSONObject data = new JSONObject();

		data.put("user_id", id);
		data.put("auth_id", "sys_admin");
		retval.put("data", data);
		
		return retval;
		
	}
	public static JSONObject deleteAuth(String id)
	{
		JSONObject retval = new JSONObject();
		JSONObject data = new JSONObject();

		data.put("user_id", id);
		data.put("auth_id", "sys_admin");
		retval.put("data", data);
		
		return retval;
		
	}
	
	public static JSONObject changeUser(String id, String nm, String pwd, String useYN)
	{
		JSONObject retval = new JSONObject();
		JSONObject data = new JSONObject();

		data.put("user_id", id);
		data.put("user_nm", nm);
		data.put("user_pw", pwd);
		data.put("use_yn", useYN);
		retval.put("data", data);
		
		return retval;
	}
	public static JSONObject changeUser(String id, String pwd)
	{
		JSONObject retval = new JSONObject();
		JSONObject data = new JSONObject();

		data.put("user_id", id);
		data.put("user_nm", "EraAdmin");
		data.put("user_pw", pwd);
		data.put("use_yn", "Y");
		retval.put("data", data);
		
		return retval;
	}
	
	public static JSONObject downloadCertInfra(String docNo)
	{
		JSONObject retval = new JSONObject();
		JSONObject data = new JSONObject();

		data.put("doc_no", docNo);
		retval.put("data", data);
		
		return retval;
	}
	public static JSONObject makeOER(String strTitle, int nCount, int[] arrPsid, int[] arrContryCode, int[] arrCircularRegin, String strDurationUint, int nDurationValue)
	{
		JSONObject retval = new JSONObject();
		JSONObject validityPeriod = new JSONObject();
		JSONObject duration = new JSONObject();
		
		retval.put("title", strTitle);
		retval.put("count", nCount);
		retval.put("psids", toArray(arrPsid));
		retval.put("country_code", toArray(arrContryCode));
		retval.put("circular_region", toArray(arrCircularRegin));
				
		duration.put("unit", strDurationUint);
		duration.put("value", nDurationValue);
		validityPeriod.put("duration", duration);		
		retval.put("validity_period", validityPeriod);
		
		
		return retval;		
	}
	
	public static int[] toArray(JSONArray arr)
	{
		if(arr == null)
			return null;
		
		int[] retval = new int[arr.size()];
		
		for(int i = 0; i<arr.size(); i++)
		{
			retval[i] = Integer.parseInt(arr.get(i).toString());
		}
		
		return retval;
		
	}
}
