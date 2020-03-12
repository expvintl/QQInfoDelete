import java.util.*;
import javax.net.ssl.*;
import java.net.*;
import java.io.*;

public class Main
{
	public static void main(String[] args)
	{
		try
		{
			//拿到签名
			String pt_login_sig=getPtLoginSig();
			//拿到二维码签名并保存二维码到文件
			String qrsig = getQRCode("/sdcard/Ubuntu/TEN.png");
			//根据qrsig计算ptqrtoken
			int ptqrtoken = new Tools().ptqr(qrsig);
			//等待登录
			String login_url=waitQRCode(qrsig,ptqrtoken,pt_login_sig);
			//拿到Cookie和uin
			List<Object> objs=getAllCookie(login_url);
			//Cookie
			String cookie=(String)objs.get(0);
			//QQ号
			String uin=(String)objs.get(1);
			System.out.println("您登录的账号为:"+uin);
			//这东西在修改信息时必要
			String lbw=getLDW(cookie,uin);
			//清空信息
			setNull(cookie,lbw);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	//清空个人资料
	private static void setNull(String allCookie,String ldw) throws IOException{
		HttpsURLConnection http=(HttpsURLConnection)new URL(URLS.USER_INFO_MOD).openConnection();
		http.setDoInput(true);
		http.setDoOutput(true);
		//没有这个会403
		http.addRequestProperty("Referer", "https://id.qq.com/myself/myself.html?ver=10045&");
		http.addRequestProperty("Cookie", allCookie+ldw);
		OutputStreamWriter writer=new OutputStreamWriter(http.getOutputStream());
		/*
		n 昵称
		ln 个性签名
		g 性别
		xz 星座
		xx 血型
		sx 生肖
		gx_ 故乡地址
		pos_ 现居地址
		*/
		writer.write("&n="+(char)8290+"&ln=&g=0&xx=0&realname=&english_name=&tel=&mail=&work=&jing=0&wei=0&pos_u=&pos_p=&pos_c=&pos_d=&gx_u=&gx_p=&gx_c=&gx_d=&schl=&hmpg=&commt=&detail_addr=&sx=12&bir_new_y=1901&bir_new_m=1&bir_new_d=1&bir_new_lunar=0&bir_new_second=0&"+ldw);
		writer.flush();
		String str="";
		InputStream input=http.getInputStream();
		int c;
		while((c=input.read())!=-1){
			str+=(char)c;
		}
		if(str.contains("\"ec\":0")){
			System.out.println("资料已清空! 返回信息:"+str);
		}
	}
	//步骤5
	//拿到ldw我也不知道这是干什么的但这也是必要的
	private static String getLDW(String allCookie,String uin) throws IOException{
		HttpsURLConnection http=(HttpsURLConnection)new URL(URLS.GET_INFO.replace("[uin]",uin)).openConnection();
		http.addRequestProperty("Referer", "https://id.qq.com/index.html");
		http.addRequestProperty("Cookie", allCookie);
		//这里面读取的全部是用户信息
		/*InputStream is=http.getInputStream();
		int c=0;
		while((c=is.read())!=-1){
			System.out.print((char)c);
		}*/
		//返回ldw
		return http.getHeaderField("Set-Cookie").substring(0,http.getHeaderField("Set-Cookie").indexOf(";"));
	}
	//步骤4
	//拿到所有登录后的Cookie
	private static List<Object> getAllCookie(String login_url) throws IOException{
	//这么写是为了使重定向起效果
	HttpsURLConnection http=null;
	//对象列表
	List<Object> objList=new ArrayList<Object>();
	String cookie="",temp="",uin="";
	//关闭重定向
	http.setFollowRedirects(false);
	//发起连接
	http=(HttpsURLConnection)new URL(login_url).openConnection();
	//遍历Set-Cookie
	for(int i=3;i<http.getHeaderFields().get("Set-Cookie").size();i++){
	temp=http.getHeaderFields().get("Set-Cookie").get(i);
	//这里把QQ取下来
	if(temp.startsWith("uin")) uin=temp.substring(5,temp.indexOf(";"));
	//拿到cookie主要内容，不需要作用域
		cookie+=temp.substring(0,temp.indexOf(";")+1);
		}
		//Java没法一次性返回2个参数，所以用object数组实现一个
		objList.add(cookie);
		objList.add(uin);
		//返回所有cookie
		return objList;
	}
	//步骤3
	//等待二维码扫描
	private static String waitQRCode(String qrsig,int ptqrtoken,String pt_login_sig) throws InterruptedException, IOException{
		String url="";
		//循环遍历等待扫描
		while(true){
			//每2秒一次
			Thread.sleep(2000);
			HttpsURLConnection http=(HttpsURLConnection)new URL(URLS.PTQR_LOGIN.replace("[token]", "" + ptqrtoken).replace("[sig]", pt_login_sig)).openConnection();
			//传入qrsig
			http.addRequestProperty("Cookie", "qrsig=" + qrsig);
			//读取返回内容
			InputStreamReader is=new InputStreamReader(http.getInputStream(), "UTF-8");
			int c;
			String temp="";
			while((c=is.read())!=-1){
				temp+=(char)c;
			}
			//关键字判断
			if(temp.contains("二维码未失效"))
				System.out.println("等待扫描二维码...");
			else if(temp.contains("https://")){
				System.out.println("已被扫描!");
				url=temp.substring(temp.indexOf("https://"),temp.indexOf("'",temp.indexOf("https://")));
				break;
			}else if(temp.contains("已失效")){
				System.out.println("二维码已失效!");
				break;
			}
			is.close();
		}
		return url;
		}
		//步骤2
	//获取登录二维码
	private static String getQRCode(String filepath) throws IOException{
		HttpsURLConnection http=(HttpsURLConnection)new URL(URLS.GET_QRCODE).openConnection();
		String qrsig=http.getHeaderField("Set-Cookie");
		//截取qrsig值
		qrsig = qrsig.substring(qrsig.indexOf("=") + 1, qrsig.indexOf(";"));
		InputStream input=http.getInputStream();
		//将二维码写入文件
		FileOutputStream file_writer=new FileOutputStream(filepath);
		int c;
		while((c=input.read())!=-1){
			file_writer.write(c);
		}
		input.close();
		file_writer.close();
		return qrsig;
		
	}
	//步骤1
	//获取登录签名
	private static String getPtLoginSig() throws IOException{
		String pt_login_sig="";
		HttpsURLConnection http=(HttpsURLConnection)new URL(URLS.X_LOGIN).openConnection();
		//遍历动态获取效果更好
		for(int i=0;i<http.getHeaderFields().get("Set-Cookie").size();i++){
			//拿到所有Set-Cookie头
			pt_login_sig=http.getHeaderFields().get("Set-Cookie").get(i);
			//只拿pt_login_sig
			if(pt_login_sig.contains("pt_login_sig")){
				//截取并返回
				return pt_login_sig.substring(pt_login_sig.indexOf("=")+1,pt_login_sig.indexOf(";"));
				}
				}
					
		return null;
	}
}
