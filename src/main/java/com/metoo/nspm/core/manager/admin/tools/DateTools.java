package com.metoo.nspm.core.manager.admin.tools;

import org.junit.Test;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;

/**
 *   String.format("%tY", new Date())    //2011
 *   String.format("%tm", new Date())   //03
 *   String.format("%tF", new Date())    //2011-03-04
 *   String.format("%tR", new Date())   //15:49
 *   String.format("%tT", new Date())   //15:49:34
 *   String.format("%tc", new Date())   //星期五 三月 04 15:49:34 CST 2011
 *   String.format("%tD", new Date())  //03/04/11
 *   String.format("%td", new Date())   //04
 */
@Component
public class DateTools {

    public static String FORMAT_yyyyMMdd = "yyyyMMdd";
    public static String FORMAT_STANDARD = "yyyy-MM-dd HH:mm:ss";
    public static String FORMAT_yyyyMMddHHmm= "yyyy-MM-dd HH:mm";
    public static String FORMAT_yyyyMMddHHmmss = "yyyyMMddHHmmss";
    public static String FORMAT_yyyyMMddHHmmss_CH = "yyyy 年 MM 月 dd 日 HH 时 mm 分 ss 秒";
    public static String TIME_000000 = "000000";
    public static String TIME_000 = "000";
    public static String TIME_235959 = "235959";
    public static long ONEDAY_TIME = 86400000L;

    // Duration.between 时间差计算工具类

    public static void main(String[] args) {
        System.out.println(1670317656L * 1000L);
        System.out.println(1670317656 * 1000L);
        System.out.println((1 * 60000));
        System.out.println(longToStr((1670317656 * 1000L) + (1 * 60000L), "yyyy-MM-dd HH:mm"));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime time = now.withSecond(0).withNano(0);
        System.out.println(time);

    }


    SimpleDateFormat sdf2 = new SimpleDateFormat();

    /**
     * @param date 当前时间
     * @return
     */
    public static String getCurrentDate(Date date){
        if(date == null){
            date = new Date();
        }
        SimpleDateFormat sdf = new SimpleDateFormat(FORMAT_yyyyMMddHHmmss);
        return sdf.format(date);
    }

    public static String getCurrentDateByCh(long timeStamp){
        try {
            return dateToStr(new Date(timeStamp), FORMAT_yyyyMMddHHmmss_CH);
        } catch (Exception var4) {
            return null;
        }
    }

    public static String longToStr(long date, String format) {
        try {
            return dateToStr(new Date(date), format);
        } catch (Exception var4) {
            return null;
        }
    }
    public static Date getCurrentTimeNoSecond(Date date){
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.clear(Calendar.SECOND);
        return cal.getTime();
    }

    /**
     *
     * @param timestamp 时间戳
     * @param format 时间格式
     * @return
     */


   // 字符串转时间戳
    public static long strToLong(String data, String format){
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        try {
            return sdf.parse(data).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
            return -1L;
        }
    }

    public static String dateToStr(Date date, String format) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            return sdf.format(date);
        } catch (Exception var3) {
            return null;
        }
    }

    public static Date parseDate(String date, String format) {
        if(date != null && !date.equals("")){
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                return sdf.parse(date);
            } catch (Exception var3) {
                return null;
            }
        }
        return null;
    }

    // 时间转时间戳
    public static Long dateToLong(Date date){
        try {
            return date.getTime() / 1000;
        } catch (Exception var3) {
            return null;
        }
    }

    /**
     * 时间戳转日期
     * @param timestamp 时间戳
     * @param format 时间格式
     * @return
     */
    public static String longToDate(Long timestamp, String format){
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            Date date = new Date(timestamp);
            return sdf.format(date);
        } catch (Exception var3) {
            return null;
        }
    }



    // 转换 10位时间戳
    public static long getTimesTamp10(){
        Date date = new Date();
        return date.getTime() / 1000;
    }

    public static long getTimesTamp10(Date date){
        if(date == null){
            date = new Date();
        }
        return date.getTime() / 1000;
    }

    public static long currentTimeMillis(){
        Long currencTimeMillis = System.currentTimeMillis();
        return currencTimeMillis;
    }

    public static long currentTimeSecond(){
        Long currencTimeMillis = System.currentTimeMillis();
        return currencTimeMillis / 1000;
    }

    public static int compare(Date date1, Date date2){
        int day = (int) ((date1.getTime() - date2.getTime()) / ONEDAY_TIME);
        return day;
    }

    public static int compare(Long time1, Long time2){
        int day = (int) ((time1 - time2) / ONEDAY_TIME);
        return day;
    }

    public static long millisecondInterval(Long time1, Long time2){
        long day = time1 - time2;
        long second = day / 1000;
        return second;
    }

    public static long secondInterval(Long time1, Long time2){
        long second = time1 - time2;
        return second;
    }

    // 计算时间差
    @Test
    public void calculatingTimeDifference() throws InterruptedException {
        Calendar cal = Calendar.getInstance();
        Long start_1 = cal.getTime().getTime();

        Thread.sleep(5000);

        cal.add(Calendar.MINUTE, 1);
        cal.add(Calendar.SECOND, 1);
        Long start_2 = cal.getTime().getTime();
        Long diff = start_2 - start_1;



        long diffSeconds = diff / 1000 % 60;

        long diffMinutes = diff / (60 * 1000) % 60;

        long diffHours = diff / (60 * 60 * 1000) % 24;

        long diffDays = diff / (24 * 60 * 60 * 1000);

        System.out.println(diffSeconds);
        System.out.println(diffMinutes);
        System.out.println(diffHours);
        System.out.println(diffDays);
    }

    @Test
    public void diff(){
        long time = 1677059659000L;
        long time2 = 1677059898000L;
        long diff = time2 - time;

        long diffSeconds = diff / 1000 % 60;

        long diffMinutes = diff / (60 * 1000) % 60;

        long diffHours = diff / (60 * 60 * 1000) % 24;

        long diffDays = diff / (24 * 60 * 60 * 1000);

        System.out.println(diffSeconds);
        System.out.println(diffMinutes);
        System.out.println(diffHours);
        System.out.println(diffDays);

    }

    @Test
    public void testGetMinTime(){
        Long time = this.getMinTime(-1);
        System.out.println(time / 1000);
    }
    // 获取前N分钟时间
    public static Long getMinTime(int min){
        Calendar cal = Calendar.getInstance();
//        cal.add(Calendar.MINUTE, Math.negateExact(min));
        cal.add(Calendar.MINUTE, min);
        return cal.getTime().getTime() / 1000;
    }

}
