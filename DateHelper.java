package com.decision.v2x.era.util.convert;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DateHelper
{
	
	public String getUTC(Date date, String format)
	{
		if(format == null)
			format = "yyyy-MM-dd HH:mm:ss";
		
		SimpleDateFormat dateFormatGmt = new SimpleDateFormat(format);
        dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		
		return dateFormatGmt.format(date);
	}
	public String getUTC(String date, String format) throws ParseException
	{
		if(format == null)
			format = "yyyy-MM-dd HH:mm:ss";
		
		SimpleDateFormat normalFormat = new SimpleDateFormat(format);
		SimpleDateFormat dateFormatGmt = new SimpleDateFormat(format);
        dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		
        Date result = normalFormat.parse(date);
        
		
		return dateFormatGmt.format(result);
	}
	public Date getDate(String strDate, String format) throws ParseException
	{
		if(format == null)
			format = "yyyy-MM-dd HH:mm:ss";
		
		SimpleDateFormat normalFormat = new SimpleDateFormat(format);
		return normalFormat.parse(strDate);
	}
	public long getMinute(String strDate, String format, boolean bUtc) throws ParseException
	{
		Date now = new Date();
		if(format == null)
			format = "yyyy-MM-dd HH:mm:ss";
		
		if(bUtc)
		{
			SimpleDateFormat normalFormat = new SimpleDateFormat(format);
			SimpleDateFormat dateFormatGmt = new SimpleDateFormat(format);
	        dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
			
	        Date date = normalFormat.parse(strDate);	
	        return date.getTime() - now.getTime();
		}
		else
		{
			SimpleDateFormat normalFormat = new SimpleDateFormat(format);
			Date date = normalFormat.parse(strDate);
			
			
			return date.getTime() - now.getTime();
		}
	}
	public String dateToString(Date item)
	{
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return format.format(item);
	}
}
