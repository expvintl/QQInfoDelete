public class Tools
{
	//计算ptqrtoken
	public int ptqr(String qrsig) {
		int hash=0;
		for (int i = 0; i < qrsig.length(); ++i)
		hash += (hash << 5) + qrsig.charAt(i);
		return 2147483647 & hash;
	}
}
