using System;
using System.Net;
using System.Text;
using System.IO;
using System.Security.Cryptography;
using System.Text.RegularExpressions;

namespace Coursio
{
	public class CoursioApi
	{
		protected WebRequest request;

		protected string salt;
		protected string publicKey;
		protected string privateKey;
		protected string sessionId = null;
		protected string baseUrl = "https://t-api.s.coursio.com/api/";

		public CoursioApi (string publicKey, string privateKey, string salt = "coursio-salt")
		{
			if (publicKey == null || privateKey == null)
			{
				throw new System.Exception ("Both public and private key are required.");
			}

			this.salt = salt;
			this.publicKey = publicKey;
			this.privateKey = privateKey;
		}

		public void Auth()
		{
			// setup connection to endpoint
			request = WebRequest.Create(baseUrl + "auth");

			// compute HMAC
			var enc = Encoding.ASCII;
			HMACSHA1 hmac = new HMACSHA1(enc.GetBytes(privateKey));
			hmac.Initialize();

			var timestamp = DateTime.Now.ToString(@"MM\/dd\/yyyy hh\:mm");
			byte[] buffer = enc.GetBytes(publicKey + timestamp + salt);
			var hash = BitConverter.ToString(hmac.ComputeHash(buffer)).Replace("-", "").ToLower();

			request.Headers ["X-Coursio-apikey"] = publicKey;
			request.Headers ["X-Coursio-time"] = timestamp;
			request.Headers ["X-Coursio-random"] = salt;
			request.Headers ["X-Coursio-hmac"] = hash;
			request.Method = "POST";

			byte[] byteArray = Encoding.UTF8.GetBytes ("{\"method\":\"loginHmac\"}");

			// Set the ContentLength property of the WebRequest.
			request.ContentLength = byteArray.Length;

			// Write data to the Stream
			Stream dataStream = request.GetRequestStream ();
			dataStream.Write (byteArray, 0, byteArray.Length);
			dataStream.Close ();

			// Get the response.
			WebResponse response = request.GetResponse ();

			// Get the stream content and read it
			Stream dataStream2 = response.GetResponseStream ();
			StreamReader reader = new StreamReader (dataStream2);

			// Read the content.
			string responseFromServer = reader.ReadToEnd ();

			// Clean up
			reader.Close ();
			dataStream2.Close ();
			response.Close ();

			Regex regex = new Regex(@"""sessionId"":""(.*?)""");
			Match match = regex.Match(responseFromServer);
			if (match.Success)
			{
			    sessionId = match.Groups[1].Value;
			}
			else
			{
				throw new System.Exception ("Login failed");
			}
		}

		public string Exec(string endpoint, string method, string jsonString = "{}")
		{
			if (endpoint == null || endpoint.Length == 0)
			{
				throw new System.Exception ("No endpoint specified");
			}

			if (sessionId == null) Auth();

			request = WebRequest.Create(baseUrl + endpoint);
			request.Method = "POST";
  		request.Headers ["Token"] = sessionId;

			byte[] byteArray = Encoding.UTF8.GetBytes ("{\"method\":\"" + method + "\",\"data\":" + jsonString + "}");

			// Set the ContentLength property of the WebRequest.
			request.ContentLength = byteArray.Length;

			// Write data to the Stream
			Stream dataStream = request.GetRequestStream ();
			dataStream.Write (byteArray, 0, byteArray.Length);
			dataStream.Close ();

			try
			{
				// Get the response.
				WebResponse response = request.GetResponse ();

				// Get the stream content and read it
				dataStream = response.GetResponseStream ();
				StreamReader reader = new StreamReader (dataStream);

				// Read the content.
				string responseFromServer = reader.ReadToEnd ();

				// Clean up
				reader.Close ();
				dataStream.Close ();
				response.Close ();

				return responseFromServer;
      }
      catch (WebException webExcp)
      {
      	HttpWebResponse httpResponse = (HttpWebResponse)webExcp.Response;
        return (int)httpResponse.StatusCode + " - " + httpResponse.StatusCode;
      }
		}
	}
}