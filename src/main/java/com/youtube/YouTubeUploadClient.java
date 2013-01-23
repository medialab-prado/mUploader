/* 
  YoutubeUploadClient

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

package com.youtube;

import com.google.gdata.client.media.ResumableGDataFileUploader;
import com.google.gdata.client.youtube.YouTubeService;
import com.google.gdata.data.media.MediaFileSource;
import com.google.gdata.data.media.mediarss.MediaCategory;
import com.google.gdata.data.media.mediarss.MediaDescription;
import com.google.gdata.data.media.mediarss.MediaKeywords;
import com.google.gdata.data.media.mediarss.MediaTitle;
import com.google.gdata.data.youtube.VideoEntry;
import com.google.gdata.data.youtube.YouTubeMediaGroup;
import com.google.gdata.data.youtube.YouTubeNamespace;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;
import com.google.gdata.client.uploader.ProgressListener;
import com.google.gdata.client.uploader.ResumableHttpFileUploader;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * YouTube video uploader. Upload local videos, with description metadata, to a
 * youtube account
 * 
 * 
 */

public class YouTubeUploadClient {

	String username;
	String password;
	String developerKey;

	public long currentBytesTransferred;
	public long currentStreamSize;

	YouTubeService service;

	/**
	 * The URL used to resumable upload
	 */
	public static final String RESUMABLE_UPLOAD_URL = "http://uploads.gdata.youtube.com/resumable/feeds/api/users/default/uploads";

	/** Time interval at which upload task will notify about the progress */
	private static final int PROGRESS_UPDATE_INTERVAL = 1000;

	/** Max size for each upload chunk */
	private static final int DEFAULT_CHUNK_SIZE = 10000000;

	private ResumableGDataFileUploader uploader;

	/**
	 * A {@link ProgressListener} implementation to track upload progress. The
	 * listener can track multiple uploads at the same time.
	 */
	private class FileUploadProgressListener implements ProgressListener {
		public synchronized void progressChanged(
				ResumableHttpFileUploader uploader) {
			switch (uploader.getUploadState()) {
			case COMPLETE:
				// TODO: Implement verbose process
				// output.println("Upload Completed");
				break;
			case CLIENT_ERROR:
				// output.println("Upload Failed");
				break;
			case IN_PROGRESS:
				// output.println(String.format("%3.0f",
				// uploader.getProgress() * 100) + "%");
				break;
			case NOT_STARTED:
				// output.println("Upload Not Started");
				break;
			case PAUSED:
				// output.println("Upload Not Started");
				break;
			}
		}
	}

	public YouTubeUploadClient(String username, String password,
			String developerKey) {

		this.username = username;
		this.password = password;
		this.developerKey = developerKey;

		this.service = new YouTubeService("gdataSample-YouTubeAuth-1",
				developerKey);

		try {
			service.setUserCredentials(username, password);
		} catch (AuthenticationException e) {
			System.out.println("Invalid login credentials.");
			System.exit(1);
		}
	}

	/**
	 * Uploads a new video to YouTube.
	 * 
	 * @param service
	 *            An authenticated YouTubeService object.
	 * @throws IOException
	 *             Problems reading user input.
	 */
	public void uploadVideo(File videoFile, String videoTitle,
			String description, String keywords) {

		if (!videoFile.exists()) {
			// TODO: change output
			// output.println("Sorry, that video doesn't exist.");
			return;
		}

		// TODO: Detect de file type and put the correct MediaFileSource
		// we allways upload .mp4 / .mov files
		MediaFileSource ms = new MediaFileSource(videoFile, "video/quicktime");

		VideoEntry newEntry = new VideoEntry();
		YouTubeMediaGroup mg = newEntry.getOrCreateMediaGroup();
		mg.addCategory(new MediaCategory(YouTubeNamespace.CATEGORY_SCHEME,
				"Tech"));
		mg.setTitle(new MediaTitle());
		mg.getTitle().setPlainTextContent(videoTitle);
		mg.setKeywords(new MediaKeywords());
		mg.getKeywords().addKeyword(keywords);
		mg.setDescription(new MediaDescription());
		mg.getDescription().setPlainTextContent(description);

		FileUploadProgressListener listener = new FileUploadProgressListener();

		try {
			uploader = new ResumableGDataFileUploader.Builder(service, new URL(
					RESUMABLE_UPLOAD_URL), ms, newEntry).title(videoTitle)
					.trackProgress(listener, PROGRESS_UPDATE_INTERVAL)
					.chunkSize(DEFAULT_CHUNK_SIZE).build();
			uploader.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public boolean isUploading() {
		if (uploader != null)
			return !uploader.isDone();
		else
			return false;
	}

	public double getProgress() {
		if (uploader != null)
			return uploader.getProgress();
		else
			return 0.0;
	}

}
