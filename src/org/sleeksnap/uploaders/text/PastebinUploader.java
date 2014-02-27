/**
 * Sleeksnap, the open source cross-platform screenshot uploader
 * Copyright (C) 2014 Nikki <nikki@nikkii.us>
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
package org.sleeksnap.uploaders.text;

import java.io.IOException;

import org.sleeksnap.http.HttpUtil;
import org.sleeksnap.http.RequestData;
import org.sleeksnap.upload.TextUpload;
import org.sleeksnap.uploaders.UploadException;
import org.sleeksnap.uploaders.Uploader;
import org.sleeksnap.uploaders.UploaderConfigurationException;
import org.sleeksnap.uploaders.settings.Setting;
import org.sleeksnap.uploaders.settings.SettingsClass;
import org.sleeksnap.uploaders.settings.types.ComboBoxSettingType;
import org.sleeksnap.uploaders.settings.types.PasswordSettingType;

/**
 * A text uploader for Pastebin.com (New API)
 * 
 * @author Nikki
 * 
 */
@SettingsClass(PastebinUploader.PastebinSettings.class)
public class PastebinUploader extends Uploader<TextUpload> {

	/**
	 * Sleeksnap's API key
	 */
	private static final String API_KEY = "c61444e938ad8215390a77e2d64adcfe";

	/**
	 * API Options
	 */
	private static final String API_OPTION_PASTE = "paste";

	/**
	 * The URL of the API page
	 */
	private static final String API_URL = "http://pastebin.com/api/api_post.php";
	
	/**
	 * The URL of the API Auth page
	 */
	private static final String API_AUTH_URL = "http://pastebin.com/api/api_login.php";

	/**
	 * The settings object used for this uploader
	 */
	private PastebinSettings settings;
	
	/**
	 * Construct this uploader with the loaded settings
	 * @param settings
	 * 			The settings object
	 */
	public PastebinUploader(PastebinSettings settings) {
		this.settings = settings;
	}

	@Override
	public String getName() {
		return "Pastebin.com";
	}

	@Override
	public String upload(TextUpload contents) throws Exception {
		RequestData data = new RequestData();
		
		data.put("api_dev_key", API_KEY)
			.put("api_option", API_OPTION_PASTE)
			.put("api_paste_code", contents.getText());
		
		// User signed in through API
		if (settings.apikey != null && !settings.apikey.isEmpty()) {
			data.put("api_user_key", settings.apikey);
		}
		
		// Paste exposure is set.
		if (settings.paste_exposure != null) {
			if (settings.paste_exposure == PastebinExposure.Private
					&& (settings.apikey == null || settings.apikey.isEmpty())) {
				throw new UploaderConfigurationException(
						"Pastebin.com only supports private pastes while logged in!");
			} else {
				data.put("api_paste_private", settings.paste_exposure.ordinal());
			}
		}
		
		// Expiration, this'll probably be set.
		if (settings.expiration != null) {
			String exp = settings.expiration;
			
			if(exp.indexOf(' ') != -1) {
				exp = exp.substring(0, exp.indexOf(' ')) + Character.toUpperCase(exp.charAt(exp.indexOf(' ')+1));
			}
			
			if (!exp.equals("Never")) {
				data.put("api_paste_expire_date", exp);
			}
		}
		
		// Execute it and let the user know if something is wrong with it.
		String resp = HttpUtil.executePost(API_URL, data);
		
		if (resp.startsWith("Bad")) {
			throw new UploadException(resp.substring(resp.indexOf(',') + 2));
		}
		
		return resp;
	}

	@Override
	public boolean validateSettings() throws UploaderConfigurationException {
		if (settings.username != null && settings.password != null && !settings.username.isEmpty() && !settings.password.isEmpty()) {
			// Validate the username and password, then get us a key.
			RequestData data = new RequestData();
			data.put("api_dev_key", API_KEY)
				.put("api_user_name", settings.username)
				.put("api_user_password", settings.password);
			try {
				String resp = HttpUtil.executePost(API_AUTH_URL, data);
				if (resp.startsWith("Bad")) {
					throw new UploaderConfigurationException(
							resp.substring(resp.indexOf(',') + 2));
				} else {
					settings.apikey = resp;
				}
			} catch (IOException e) {
				throw new UploaderConfigurationException(
						"Unable to verify username and password");
			}
		}
		// We don't need to store the user's password since we get an application key
		settings.password = null;
		return true;
	}
	
	public static class PastebinSettings {
		// "username", "password|password", "paste_exposure|combobox[Public,Unlisted,Private]", "expiration|combobox[Never,10 minutes,1 hour,1 day,1 week,2 weeks,1 month]"
		@Setting(name = "Username", description = "Pastebin.com Account Username", optional = true)
		public String username;
		
		@Setting(name = "Password", description = "Pastebin.com Account Password", type = PasswordSettingType.class, optional = true)
		public String password;
		
		@Setting(name = "Paste Exposure", description = "Pastebin Paste Exposure")
		public PastebinExposure paste_exposure = PastebinExposure.Public;
		
		@Setting(name = "Expiration", description = "Paste Expiration Time", type = ComboBoxSettingType.class, defaults = { "Never", "10 minutes", "1 hour", "1 day", "1 week", "2 weeks", "1 month" })
		public String expiration;
		
		// The user API Key, this is automatically generated so it doesn't need a Setting annotation.
		public String apikey;
	}

	/**
	 * Pastebin exposure settings.
	 * 
	 * Documented as: We have 3 valid values available which you can use with
	 * the 'api_paste_private' parameter: 0 = Public 1 = Unlisted 2 = Private
	 * (only allowed in combination with api_user_key, as you have to be logged
	 * into your account to access the paste)
	 * 
	 * @author Nikki
	 * 
	 */
	private enum PastebinExposure {
		Public, Unlisted, Private
	}
}
