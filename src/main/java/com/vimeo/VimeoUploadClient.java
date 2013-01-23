package com.vimeo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.activation.MimetypesFileTypeMap;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.net.io.CopyStreamEvent;
import org.apache.commons.net.io.CopyStreamListener;
import org.apache.commons.net.io.Util;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.VimeoApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/* 

 VideoUploadClient

 Copyright (c) 2012 Enrique Esteban at Medialab Prado

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.

 */

public class VimeoUploadClient implements Runnable {

	final static String TOKEN_FILE = ".vimeo-token";
	final static String CONFIG_FILE = ".vimeo-uploader";
	final static String ENDPOINT = "http://vimeo.com/api/rest/v2";
	static boolean saveToken = true;
	static boolean verbose = true;
	static boolean checkStatusOnly = false;

	File videoFile;
	String videoTitle;
	String description;
	String keywords;

	/**
	 * vimeo service - main object
	 */
	private OAuthService service;
	private Token accessToken;

	String apiKey;
	String secret;

	public long currentBytesTransferred;
	public long currentStreamSize;
	private Thread vimeoThread;
	private boolean uploading = false;

	public VimeoUploadClient(String apiKey, String secret) throws IOException,
			APIException {

		saveToken = false; // no save token
		verbose = true; // be verbose
		checkStatusOnly = false; // we want upload

		this.apiKey = apiKey;
		this.secret = secret;

		/**
		 * read .vimeo-uploader
		 */

		FileInputStream in1 = null;
		Properties properties = null;
		try {
			File f = new File(System.getProperty("user.home"),
					VimeoUploadClient.CONFIG_FILE);
			properties = new Properties();
			if (f.createNewFile()) {
				properties.put("apiKey", apiKey);
				properties.put("secret", secret);
				properties.store(new FileOutputStream(f),
						"vimeo-uploader configuration file");
			}
			in1 = new FileInputStream(f);
			properties.load(in1);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			in1.close();
		}

		try {
			service = new ServiceBuilder().provider(VimeoApi.class)
					.apiKey(properties.getProperty("apiKey"))
					.apiSecret(properties.getProperty("secret")).build();
		} catch (Exception e) {
			System.out.println(e.getMessage()
					+ "\nCheck Your configuration file (" + CONFIG_FILE + ")");
			return;
		}

		/**
		 * try read saved token
		 */
		accessToken = readToken();
		if (accessToken == null) {

			if (verbose)
				System.out.println("Fetching the Request Token...");
			Token token = service.getRequestToken();
			System.out.println("Got the Request Token!");
			System.out.println();

			if (verbose)
				System.out.println("Fetching the Authorization URL...");
			String authURL = service.getAuthorizationUrl(token);
			if (verbose)
				System.out.println("Got the Authorization URL!");
			System.out.println("Now go and authorize Scribe here:");
			System.out.println(authURL + "&permission=write");
			System.out.println("And paste the authorization code here");
			System.out.print(">>");

			// TODO: SCRIBE AUTHORIZATION GETTED THE FIRST TIME
			Verifier verifier = new Verifier("land-5zbo4");
			System.out.println();
			// Trade the Request Token and Verfier for the Access Token
			if (verbose)
				System.out
						.println("Trading the Request Token for an Access Token...");
			accessToken = service.getAccessToken(token, verifier);
			if (verbose)
				System.out.println("Got the Access Token!");
			if (saveToken) {
				saveToken(accessToken);
			}
			if (verbose) {
				System.out
						.println("(if your curious it looks like this [token, secret]: "
								+ accessToken + " )");
				System.out.println();
			}
		}

		// get logged username
		ResponseWrapper response = call("vimeo.test.login", null);
		String username = (((Node) path(response.getResponse().getBody(),
				"//username", XPathConstants.NODE)).getTextContent());
		System.out.printf("%-30S: %s\n", "Logged", username);

		// TODO: Get User name from Vimeo: xstain1981 -> user14301672

		// get free storage space in bytes
		response = call("vimeo.videos.upload.getQuota", null);
		String free = ((Node) path(response.getResponse().getBody(),
				"//upload_space", XPathConstants.NODE)).getAttributes()
				.getNamedItem("free").getNodeValue();
		System.out.printf("%-30S: %s MB\n", "Free Storage Space",
				Double.parseDouble(free) / 1024 / 1024);

		// TODO: Get free storage (new Vimeo Account has 500.MB (January 2013))

	}

	// Video upload

	public void uploadVideo(File videoFile, String videoTitle,
			String description, String keywords) {

		// Initialize video variables
		this.videoFile = videoFile;
		this.description = description;
		this.keywords = keywords;
		this.videoTitle = videoTitle;

		// Initialize process variables
		this.currentBytesTransferred = 0;
		this.currentStreamSize = 0;

		uploading = true;

		vimeoThread = new Thread(this, "VimeoClientThread");
		vimeoThread.start();
	}

	@SuppressWarnings("serial")
	public void run() {
		// get a upload ticket

		try {
			ResponseWrapper response = call("vimeo.videos.upload.getTicket",
					new HashMap<String, String>() {
						{
							put("upload_method", "streaming");
						}
					});
			NamedNodeMap nodeMap = ((Node) path(response.getResponse()
					.getBody(), "//ticket", XPathConstants.NODE))
					.getAttributes();
			final String ticketId = nodeMap.getNamedItem("id").getNodeValue();
			String endpoint = nodeMap.getNamedItem("endpoint").getNodeValue();
			System.out.printf("%-30S: %s\n", "Ticket ID", ticketId);
			System.out.printf("%-30S: %s\n", "Endpoint", endpoint);

			// check ticket
			response = call("vimeo.videos.upload.checkTicket",
					new HashMap<String, String>() {
						{
							put("ticket_id", ticketId);
						}
					});
			NamedNodeMap nodeMap1 = ((Node) path(response.getResponse()
					.getBody(), "//ticket", XPathConstants.NODE))
					.getAttributes();
			System.out.printf("%-30S: %s\n", "Ticket VALID", nodeMap1
					.getNamedItem("valid").getNodeValue());

			// TODO: send video
			doPut(endpoint);

			HashMap<String, String> hm = new HashMap<String, String>();
			hm.put("filename", videoFile.getName());
			hm.put("ticket_id", ticketId);

			// Complete the upload process.
			response = call("vimeo.videos.upload.complete", hm);

			NamedNodeMap nodeMap2 = ((Node) path(response.getResponse()
					.getBody(), "//ticket", XPathConstants.NODE))
					.getAttributes();
			final String videoId = nodeMap2.getNamedItem("video_id")
					.getNodeValue();
			System.out.printf("%-30S: %s\n", "Video ID", videoId);

			// TODO: set title

			hm = new HashMap<String, String>();
			hm.put("video_id", videoId);
			hm.put("title", videoTitle);

			response = call("vimeo.videos.setTitle", hm);
			NamedNodeMap nodeMap3 = ((Node) path(response.getResponse()
					.getBody(), "//rsp", XPathConstants.NODE)).getAttributes();
			String stat = nodeMap3.getNamedItem("stat").getNodeValue();
			System.out.printf("%-30S: %s\n", "set Title status", stat);

			// TODO: set description

			hm = new HashMap<String, String>();
			hm.put("video_id", videoId);
			hm.put("description", description);

			response = call("vimeo.videos.setDescription", hm);
			NamedNodeMap nodeMap4 = ((Node) path(response.getResponse()
					.getBody(), "//rsp", XPathConstants.NODE)).getAttributes();
			String stat2 = nodeMap4.getNamedItem("stat").getNodeValue();
			System.out.printf("%-30S: %s\n", "set Description status", stat2);

			// TODO: set description

			hm = new HashMap<String, String>();
			hm.put("video_id", videoId);
			hm.put("tags", keywords);

			response = call("vimeo.videos.addTags", hm);
			NamedNodeMap nodeMap5 = ((Node) path(response.getResponse()
					.getBody(), "//rsp", XPathConstants.NODE)).getAttributes();
			String stat3 = nodeMap5.getNamedItem("stat").getNodeValue();
			System.out.printf("%-30S: %s\n", "set Tags status", stat3);

			// TODO: set

			System.out.printf("DONE.\n\n");
		} catch (APIException e) {
			System.out.println("Fail uploading " + videoTitle);
			e.setStackTrace(null);
		}

		uploading = false;
	}

	/**
	 * call method
	 * 
	 * @param method
	 * @param params
	 * @return
	 */
	private ResponseWrapper call(String method, Map<String, String> params)
			throws APIException {
		if (verbose)
			System.out.println("Calling method: \"" + method + "\"");
		OAuthRequest orequest = new OAuthRequest(Verb.GET,
				VimeoUploadClient.ENDPOINT);
		orequest.addQuerystringParameter("method", method);
		if (params != null) {
			for (Map.Entry<String, String> p : params.entrySet()) {
				orequest.addQuerystringParameter(p.getKey(), p.getValue());
			}
		}
		service.signRequest(accessToken, orequest);
		Response response = orequest.send();
		if (verbose)
			System.out.println(response.getBody());
		/*
		 * NodeList nodes = (NodeList) path(response.getBody(), "//username",
		 * XPathConstants.NODESET); for (int i = 0; i < nodes.getLength(); i++)
		 * { System.out.println(nodes.item(i).getTextContent()); }
		 */

		NamedNodeMap nodeMap3 = ((Node) path(response.getBody(), "//rsp",
				XPathConstants.NODE)).getAttributes();
		String stat = nodeMap3.getNamedItem("stat").getNodeValue();

		if (stat.equals("fail")) {
			System.out.println(" response " + response);
			throw new APIException(method, response);
		}

		return new ResponseWrapper(response, stat);
	}

	/**
	 * parse xml response
	 * 
	 * @param content
	 * @param path
	 * @param returnType
	 * @return
	 */
	private Object path(String content, String path, QName returnType) {
		try {
			DocumentBuilderFactory domFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder builder = domFactory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new StringReader(
					content)));
			XPath xpath = XPathFactory.newInstance().newXPath();
			XPathExpression expr = xpath.compile(path);
			return expr.evaluate(doc, returnType);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * upload file (streaming)
	 * 
	 * @param endpoint
	 */
	private void doPut(String endpoint) {
		URL endpointUrl;
		HttpURLConnection connection;
		try {
			endpointUrl = new URL(endpoint);
			connection = (HttpURLConnection) endpointUrl.openConnection();
			connection.setRequestMethod("PUT");
			connection.setRequestProperty("Content-Length", videoFile.length()
					+ "");
			connection.setRequestProperty("Content-Type",
					new MimetypesFileTypeMap().getContentType(videoFile));
			connection.setFixedLengthStreamingMode((int) videoFile.length());
			connection.setDoOutput(true);

			CopyStreamListener listener = new CopyStreamListener() {
				public void bytesTransferred(long totalBytesTransferred,
						int bytesTransferred, long streamSize) {
					// TODO: get the values to visualize it: currentStreamSize,
					// currentBytesTransferred
					currentStreamSize = streamSize;
					currentBytesTransferred = totalBytesTransferred;

					/*
					 * System.out.printf("\r%-30S: %d / %d (%d %%)", "Sent",
					 * totalBytesTransferred, streamSize, totalBytesTransferred
					 * * 100 / streamSize);
					 */
				}

				public void bytesTransferred(CopyStreamEvent event) {
				}
			};
			InputStream in = new FileInputStream(videoFile);
			OutputStream out = connection.getOutputStream();
			System.out.println("Uploading \"" + videoFile.getAbsolutePath()
					+ "\"... ");
			long c = Util.copyStream(in, out, Util.DEFAULT_COPY_BUFFER_SIZE,
					videoFile.length(), listener);
			System.out.printf("\n%-30S: %d\n", "Bytes sent", c);
			in.close();
			out.close();

			// return code
			System.out.printf("\n%-30S: %d\n", "Response code",
					connection.getResponseCode());

			// TODO: Response code, if everything OK the code is 200

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * save token in file
	 * 
	 * @param token
	 * @return
	 */
	private boolean saveToken(Token token) {
		File f = new File(System.getProperty("user.home"),
				VimeoUploadClient.TOKEN_FILE);
		try {
			f.createNewFile();
			if (f.exists()) {
				if (verbose)
					System.out.print("Saving token \"" + token + "\" to \""
							+ f.getAbsolutePath() + "\" ... ");
				FileWriter w = new FileWriter(f);
				BufferedWriter writer = new BufferedWriter(w);
				writer.write(token.getToken() + ";" + token.getSecret());
				writer.close();
				if (verbose)
					System.out.println("DONE");
			}
			return true;
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return false;
	}

	/**
	 * read token from file
	 * 
	 * @return access token
	 */
	private Token readToken() {
		File f = new File(System.getProperty("user.home"),
				VimeoUploadClient.TOKEN_FILE);
		if (f.exists()) {
			if (verbose)
				System.out.println("Reading token from \""
						+ f.getAbsolutePath() + "\"");
			try {
				FileReader r = new FileReader(f);
				BufferedReader br = new BufferedReader(r);
				String token1 = br.readLine();
				Token token = new Token(token1.split(";")[0],
						token1.split(";")[1]);
				br.close();
				return token;
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
		return null;
	}

	public boolean isUploading() {
		return uploading;
	}

	public double getProgress() {
		return (double) currentBytesTransferred / (double) currentStreamSize;
	}

	public class ResponseWrapper {
		Response response;
		String status;

		public ResponseWrapper(Response response, String status) {
			this.response = response;
			this.status = status;
		}

		public String getStatus() {
			return status;
		}

		public Response getResponse() {
			return response;
		}
	}

	@SuppressWarnings("serial")
	public class APIException extends Exception {
		Response response;
		String method;

		public APIException(String method, Response response) {
			this.response = response;
			this.method = method;
		}

		@Override
		public String getMessage() {
			return method + " FAIL\nRESPONSE BODY:\n" + response.getBody();
		}
	}

}
