/**
 * Sleeksnap, the open source cross-platform screenshot uploader
 * Copyright (C) 2012 Nikki <nikki@nikkii.us>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.sleeksnap.http;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.sleeksnap.util.StreamUtils;
import org.sleeksnap.util.Util;


/**
 * A simple HTTP Utility which assists with POST/GET methods
 * 
 * @author Nikki
 * 
 */
public class HttpUtil {

	/**
	 * Attempt to encode the string silenty
	 * 
	 * @param string
	 *            The string
	 * @return The encoded string
	 */
	public static String encode(String string) {
		try {
			return URLEncoder.encode(string, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// Ignored
		}
		return string;
	}

	/**
	 * Alias for <code>executeGet(URL url)</code>
	 * 
	 * @param url
	 *            The URL
	 * @return The response
	 * @throws IOException
	 *             If an error occurred
	 */
	public static String executeGet(String url) throws IOException {
		return executeGet(new URL(url));
	}

	/**
	 * Execute a GET request
	 * 
	 * @param url
	 *            The URL
	 * @return The response
	 * @throws IOException
	 *             If an error occurred
	 */
	public static String executeGet(URL url) throws IOException {
		return StreamUtils.readContents(url.openStream());
	}

	/**
	 * Executes a GET request with the specified query
	 * 
	 * @param url
	 *            The URL
	 * @param data
	 * 			  The GET query
	 * 
	 * @return The response
	 * @throws IOException
	 *             If an error occurred
	 */
	public static String executeGet(String url, RequestData data) throws IOException {
		return executeGet(url + '?' + data.toURLEncodedString());
	}

	/**
	 * Executes a GET request with the specified query
	 * Wrapper for <code>executeGet(String, RequestData)</code>
	 * 
	 * @param url
	 *            The URL
	 * @param data
	 * 			  The GET query
	 * @return The response
	 * @throws IOException
	 *             If an error occurred
	 */
	public static String executeGet(URL url, RequestData data) throws IOException {
		return executeGet(url.toString(), data);
	}

	/**
	 * Alias for <code>executePost(URL url, String data)</code>, constructs the
	 * url
	 * 
	 * @param url
	 *            The URL
	 * @param data
	 *            The data
	 * @return The response
	 * @throws IOException
	 *             If an error occurred
	 */
	public static String executePost(String url, String data) throws IOException {
		return executePost(new URL(url), data, ResponseType.CONTENTS);
	}
	
	/**
	 * POST to the specified URL with the specified map of values.
	 * 
	 * @param url
	 *            The URL
	 * @param data
	 *            The form data
	 * @return The HTTP response
	 * @throws IOException
	 *             If an error occurred while connecting/receiving the data
	 */
	public static String executePost(String url, RequestData data)
			throws IOException {
		return executePost(url, data.toURLEncodedString());
	}
	
	/**
	 * Alias for <code>executePost(URL url, String data)</code>, constructs the
	 * url
	 * 
	 * @param url
	 *            The URL
	 * @param data
	 *            The data
	 * @return The response
	 * @throws IOException
	 *             If an error occurred
	 */
	public static String executePost(String url, String data, ResponseType responseType) throws IOException {
		return executePost(new URL(url), data, responseType);
	}
	
	/**
	 * Alias for <code>executePost(URL url, String data)</code>, constructs the
	 * url
	 * 
	 * @param url
	 *            The URL
	 * @param data
	 *            The data
	 * @return The response
	 * @throws IOException
	 *             If an error occurred
	 */
	public static String executePost(String url, RequestData data, ResponseType responseType) throws IOException {
		return executePost(new URL(url), data.toURLEncodedString(), responseType);
	}
	
	/**
	 * POST to the specified URL with the specified map of values.
	 * 
	 * @param url
	 *            The URL
	 * @param data
	 *            The form data
	 * @return The HTTP response
	 * @throws IOException
	 *             If an error occurred while connecting/receiving the data
	 */
	public static String executePost(URL url, RequestData data) throws IOException {
		return executePost(url, data.toURLEncodedString(), ResponseType.CONTENTS);
	}
	
	/**
	 * Execute a POST request
	 * 
	 * @param url
	 *            The URL
	 * @param data
	 *            The data
	 * @return The response
	 * @throws IOException
	 *             If an error occurred
	 */
	public static String executePost(URL url, String data, ResponseType responseType) throws IOException {
		// Set redirect following to false if we want the redirect url
		if (responseType == ResponseType.REDIRECT_URL) {
			HttpURLConnection.setFollowRedirects(false);
		}
		// Execute the request
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestProperty("User-Agent", Util.getHttpUserAgent());
		connection.setDoOutput(true);
		try {
			OutputStreamWriter writer = new OutputStreamWriter(
					connection.getOutputStream());
			writer.write(data);
			writer.flush();
			writer.close();

			switch(responseType) {
			case REDIRECT_URL:
				String location = connection.getHeaderField("Location");
				if (location == null) {
					throw new IOException("No location header found, body: " + StreamUtils.readContents(connection.getInputStream()));
				}
				return location;
			default:
				return StreamUtils.readContents(connection.getInputStream());
			}
		} finally {
			connection.disconnect();
			
			// Reset redirect following
			HttpURLConnection.setFollowRedirects(true);
		}
	}
	
	/**
	 * Implode a map of key -> value pairs to a URL safe string
	 * 
	 * @param values
	 *            The values to implode
	 * @return The imploded string
	 * @throws IOException
	 *             If an error occurred while encoding any values. 
	 */
	public static String implode(Map<String, Object> values) throws IOException {
		StringBuilder builder = new StringBuilder();
		Iterator<Entry<String, Object>> iterator = values.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, Object> entry = iterator.next();
			builder.append(entry.getKey());

			if (entry.getValue() != null) {
				builder.append("=").append(URLEncoder.encode(entry.getValue().toString(), "UTF-8"));
			}
			if (iterator.hasNext())
				builder.append("&");
		}
		return builder.toString();
	}

	/**
	 * Parse an http query string
	 * @param string
	 * 			The string to parse
	 * @return
	 * 			The parsed string in a map.
	 */
	public static Map<String, String> parseQueryString(String string) {
		Map<String, String> values = new HashMap<String, String>();
		String[] split = string.split("&");

		for(String s : split) {
			if(s.indexOf('=') != -1) {
				values.put(s.substring(0, s.indexOf('=')), s.substring(s.indexOf('=')+1));
			} else {
				values.put(s, null);
			}
		}

		return values;
	}
}
