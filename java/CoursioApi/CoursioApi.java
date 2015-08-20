package CoursioApi;

import java.net.*;
import java.io.*;
import java.sql.Timestamp;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoursioApi
{
	protected HttpURLConnection connection;

	protected String salt;
	protected String publicKey;
	protected String privateKey;
	protected String sessionId = null;
	protected String baseUrl = "https://t-api.s.coursio.com/api/";

    public CoursioApi(String publicKey, String privateKey, String salt) throws Exception
    {
		if (publicKey == null || privateKey == null || salt == null)
		{
			throw new Exception ("Both keys and salt are required.");
		}

		this.salt = salt;
		this.publicKey = publicKey;
		this.privateKey = privateKey;
    }
    
    public void Auth() throws Exception
    {
        URL obj = new URL(baseUrl + "auth");
        connection = (HttpURLConnection) obj.openConnection();
		connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        
   		// compute HMAC

        // get an hmac_sha1 key from the raw key bytes
        SecretKeySpec signingKey = new SecretKeySpec(privateKey.getBytes(), "HmacSHA1");

        // get an hmac_sha1 Mac instance and initialize with the signing key
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(signingKey);

        // generate timestamp
        java.util.Date date = new java.util.Date();
        Timestamp timestamp = new Timestamp(date.getTime());

        String signature = publicKey + timestamp.toString() + salt;

        // compute the hmac on input data bytes
        byte[] bytes = mac.doFinal(signature.getBytes());
        StringBuilder hash = new StringBuilder();

        for (byte b : bytes)
        {
        	hash.append(String.format("%02x", b));
        }

        connection.setRequestProperty ("X-Coursio-apikey", publicKey);
        connection.setRequestProperty ("X-Coursio-time", timestamp.toString());
        connection.setRequestProperty ("X-Coursio-random", salt);
        connection.setRequestProperty ("X-Coursio-hmac", hash.toString());
        
        OutputStreamWriter osw = new OutputStreamWriter(connection.getOutputStream());
        osw.write("{\"method\":\"loginHmac\"}");
        osw.flush();
        osw.close();

        // Get the response
		String line, result = "";
		InputStream stream = connection.getResponseCode() == 200 ? connection.getInputStream() : connection.getErrorStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(stream));

        while ((line = rd.readLine()) != null) result += line;
        
        rd.close();

        if (sessionId == null)
        {
            Pattern pattern = Pattern.compile("\"sessionId\":\"(.*?)\"");
            Matcher matcher = pattern.matcher(result);
            if (matcher.find()) sessionId = matcher.group(1);
        }
        
        connection.disconnect();
    }

	public String Exec(String endpoint, String method, String jsonString) throws Exception
	{
		if (endpoint == null || endpoint.length() == 0)
		{
			throw new Exception ("No endpoint specified");
		}
		
		if (method == null || method.length() == 0)
		{
			throw new Exception ("No method specified");
		}
		
        if (sessionId == null) Auth();
        
		// setup connection to endpoint
        URL obj = new URL(baseUrl + endpoint);
        connection = (HttpURLConnection) obj.openConnection();
		connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        connection.setRequestProperty ("Token", sessionId);

        OutputStreamWriter osw = new OutputStreamWriter(connection.getOutputStream());
        osw.write("{\"method\":\"" + method + "\",\"data\":" + jsonString + "}");
        osw.flush();
        osw.close();

        // Get the response
		String line, result = "";
		InputStream stream = connection.getResponseCode() == 200 ? connection.getInputStream() : connection.getErrorStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(stream));

        while ((line = rd.readLine()) != null) result += line;

        rd.close();
        
        return result;
	}
}