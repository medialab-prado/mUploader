package es.medialabprado.muploader;

import processing.core.*;
import processing.data.*;

import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.io.FileNotFoundException;
import java.io.IOException;
import controlP5.Bang;
import controlP5.ControlEvent;
import controlP5.ControlP5;
import controlP5.Textarea;
import controlP5.Textfield;
import controlP5.Textlabel;
import java.io.File;
import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;
import com.vimeo.VimeoUploadClient;
import com.vimeo.VimeoUploadClient.APIException;
import com.youtube.YouTubeUploadClient;

/*
 VideoUploaderP5

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

public class VideoUploaderP5 extends PApplet {

	private static final long serialVersionUID = 1L;

	String VIMEO_API_KEY = "YOUR VIMEO API KEY";
	String VIMEO_API_SECRET = "YOUR VIMEO API SECRET";
	String YOUTUBE_USER_NAME = "YOUR YOUTUBE USER ACCOUNT NAME";
	String YOUTUBE_PASSWORD = "YOUR YOUTUBE USER ACCOUNT PASSWORD";
	String YOUTUBE_DEVELOPER_KEY = "YOUR YOUTUBE DEVELOPER KEY";

	int numOfSites = 3;

	final static int YOUTUBE_SITE_ID = 0;
	final static int VIMEO_SITE_ID = 1;
	final static int ARCHIVE_SITE_ID = 2;

	final static int SITE_SELECTED = 1;
	final static int SITE_NOT_SELECTED = 0;

	int[] siteSelection = new int[numOfSites];

	ControlP5 cp5;
	Textarea myTextarea;
	VideoList videoList;
	Bang uploadBarLeftBang;
	Bang uploadBarRightBang;
	Bang youtubeBang;
	Bang vimeoBang;
	Bang archiveBang;
	Textlabel uploadBarLeftLabel;
	Textlabel uploadBarRightLabel;
	Textlabel uploadBarMainLabel;

	Textfield videoIdTextField;
	Textfield uriTextField;
	Textfield durationTextField;
	Textfield titleTextField;
	Textfield dateTextField;
	Textfield descriptionTextField;
	Textfield creatorTextField;
	Textfield data_1TextField;
	Textfield data_2TextField;
	Textfield data_3TextField;
	Textfield typeTextField;
	Textfield tagsTextField;
	Textfield authorsTextField;

	int platformsButtonOffset = 230;

	int textAreaHeight = 150;
	int buttonsWidth = 20;
	int buttonsHeight = 20;
	int processButtonHeight = 100;
	int videoListX = 5;
	int videoListY = 20;
	int videoListWidth = 240;
	int videoInfoXOffset = 20;
	int videoInfoYOffset = 25;
	int videoInfoTextHeight = 20;
	int videoInfoTextWidth = 600;
	int uploaderBarHeight = 52;
	int uploaderBarY;

	ArrayList<VideoInfo> videoInfoArr;

	YouTubeUploadClient youtubeClient;
	File youtubeVideoFolder = new File("videos");
	Hashtable<File, VideoInfo> youtubeVideos;

	private VimeoUploadClient vimeoClient;
	File vimeoVideoFolder = new File("videos");
	Hashtable<File, VideoInfo> vimeoVideos;

	File archiveVideoFolder = new File("videos");
	Hashtable<File, VideoInfo> archiveVideos;

	// Upload mode
	final int UPLOAD_ALL = 0;
	final int UPLOAD_SELECTED = 1;

	// Upload state
	final int NOT_UPLOADING = 0;
	final int UPLOADING = 1;
	final int PAUSING_UPLOAD = 2;
	final int PAUSED = 3;
	final int STOPPING_UPLOAD = 4;

	int uploadMode = UPLOAD_ALL;
	int uploadState = NOT_UPLOADING;

	private int currentUploadVideoIndex;
	int currentUploadPlatform = YOUTUBE_SITE_ID;

	PImage[] uploadAllImages;
	PImage[] uploadImages;
	PImage[] pauseImages;
	PImage[] resumeImages;
	PImage[] stopImages;
	PImage[] platformUploadImages;
	PImage[] platformUndoImages;
	PImage platformUploadedImage;
	PImage stopDisabledImage;
	PImage medialabLogo;
	PImage youtubeLogo;
	PImage vimeoLogo;
	PImage archiveLogo;
	int platformLogoWidth = 30;
	int platformUploadOffset = 20;
	int platformButtonsOffsetY = 10;
	int platformButtonsHeight;

	String uploadAllLabel = "ALL";
	String uploadLabel = "SELECTED";
	String pauseLabel = "PAUSE";
	String resumeLabel = "RESUME";
	String stopLabel = "STOP";
	String noUploadLabel = "CLICK TO START UPLOADING";
	String pausedUploadLabel_0 = "UPLOAD WILL PAUSE WHEN FINISHING THE CURRENT UPLOAD";
	String pausedUploadLabel = "UPLOAD PAUSED";
	String stopedUploadLabel_0 = "UPLOAD WILL STOP WHEN FINISHING THE CURRENT UPLOAD";
	String currentUploadLabel = "UPLOADING FILE TO ";
	String csvFile = "movie.csv";
	String csvFile_out = "movie_out.csv";

	int backgroundUploadColor = 0xFFA1A1A1;

	private float mainLabelX;

	private int backgroundProgressBarColor = 0xFFFFAB08;

	public void setup() {
		size(870, 700);

		initDDBB();

		videoInfoArr = readMovieCVS();
		initGui(videoInfoArr);
		VideoInfo videoInfo = videoInfoArr.get(0);
		setVideoInfo(videoInfo);

		initializeYoutube();
		initializeVimeo();
		initializeArchive();

	}

	private void initDDBB() {
		// Open Uploads database
		/*
		 * try { Connection c = DriverManager.getConnection(
		 * "jdbc:hsqldb:file:db/testdb", "SA", ""); } catch (SQLException e) {
		 * // TODO Auto-generated catch block e.printStackTrace(); }
		 */
	}

	public void draw() {
		background(0);
		backgroundUploadColor = 0xFFD0D0D0;
		fill(backgroundUploadColor);
		rect(0, height - uploaderBarHeight - textAreaHeight, width,
				uploaderBarHeight);

		switch (uploadState) {
		case UPLOADING: {
			if (currentUploadPlatform == YOUTUBE_SITE_ID) {
				drawProgressBar(youtubeClient.getProgress(), 0xFFFF0000);
				if (!youtubeClient.isUploading()) {
					currentUploadPlatform = VIMEO_SITE_ID;
					uploadVimeoVideo(currentUploadVideoIndex);
					updateBarGUI(uploadState, currentUploadPlatform);
				}
			} else if (currentUploadPlatform == VIMEO_SITE_ID) {
				drawProgressBar(vimeoClient.getProgress(), 0xFF0099FF);
				if (!vimeoClient.isUploading()) {
					currentUploadPlatform = YOUTUBE_SITE_ID;
					// TODO: UPDATE NEXT VIDEO, WE HAVE TO MANAGE DIFERENTS
					// VIDEOS UPLOADING TO SEVERAL PLATFORMS
					// NOW WE UPLOAD AUTOMATICLY VIDEOS FROM A SINGLE FOLDER
					// ('videos') TO ALL PLATFORMS (youtube & vimeo)
					currentUploadVideoIndex++;
					if (currentUploadVideoIndex < vimeoVideoFolder.listFiles().length) {
						uploadYoutubeVideo(currentUploadVideoIndex);
						updateBarGUI(uploadState, currentUploadPlatform);

					} else { // Subidon ending
						currentUploadVideoIndex = 0;
						uploadState = NOT_UPLOADING;
					}
				}
			}
			// println("uploadState: UPLOADING");
			break;
		}
		case PAUSING_UPLOAD: {

			if (currentUploadPlatform == YOUTUBE_SITE_ID) {
				drawProgressBar(youtubeClient.getProgress(), 0xFFFF0000);
			} else if (currentUploadPlatform == VIMEO_SITE_ID) {
				drawProgressBar(vimeoClient.getProgress(), 0xFF0099FF);
			}
			// println("uploadState: PAUSING_UPLOAD");
			break;
		}
		case PAUSED: {
			// println("uploadState: PAUSED");
			break;
		}
		case STOPPING_UPLOAD: {
			if (currentUploadPlatform == YOUTUBE_SITE_ID) {
				drawProgressBar(youtubeClient.getProgress(), 0xFFFF0000);
			} else if (currentUploadPlatform == VIMEO_SITE_ID) {
				drawProgressBar(vimeoClient.getProgress(), 0xFF0099FF);
			}
			// println("uploadState: STOPPING_UPLOAD");
			break;
		}
		case NOT_UPLOADING: {
			// println("uploadState: NOT_UPLOADING");
			break;
		}
		}

		image(youtubeLogo, width - platformsButtonOffset + youtubeLogo.width,
				platformButtonsHeight);
		image(vimeoLogo, width - platformsButtonOffset + 3 * youtubeLogo.width
				+ platformUploadOffset, platformButtonsHeight);
		image(archiveLogo, width - platformsButtonOffset + 5
				* youtubeLogo.width + 2 * platformUploadOffset,
				platformButtonsHeight);
		cp5.draw();
	}

	void drawProgressBar(double progressValue, int progressColor) {
		noStroke();
		fill(backgroundProgressBarColor);
		rect(mainLabelX, uploaderBarY + 30, width - mainLabelX - 10, 15);

		fill(progressColor);
		float uploadProgressBarWidth = map((float) progressValue, 0, 1, 0,
				width - mainLabelX - 10);
		rect(mainLabelX, uploaderBarY + 30, uploadProgressBarWidth, 15);

	}

	@SuppressWarnings("deprecation")
	public void controlEvent(ControlEvent theEvent) {

		if (theEvent.isGroup() && theEvent.name().equals("videoList")) {
			int arrId = (int) theEvent.group().value();
			for (int i = 0; i < videoInfoArr.size(); i++) {
				if (arrId == videoInfoArr.get(i).arrID) {
					VideoInfo videoInfo = videoInfoArr.get(i);
					setVideoInfo(videoInfo);
					break;
				}
			}
		} else {
			if (theEvent.name().equals("uploadBarLeftBang")) {
				switch (uploadState) {
				case NOT_UPLOADING: {
					uploadMode = UPLOAD_ALL;
					uploadState = UPLOADING;
					currentUploadVideoIndex = 0;
					currentUploadPlatform = YOUTUBE_SITE_ID;
					uploadYoutubeVideo(currentUploadVideoIndex);
					break;
				}
				case UPLOADING: {
					uploadState = PAUSING_UPLOAD;
					break;
				}
				case PAUSING_UPLOAD: {
					uploadState = UPLOADING;
					break;
				}
				case PAUSED: {
					uploadState = UPLOADING;
					break;
				}
				case STOPPING_UPLOAD: {
					uploadState = UPLOADING;
					break;
				}
				}
				updateBarGUI(uploadState, currentUploadPlatform);

			} else if (theEvent.name().equals("uploadBarRightBang")) {
				switch (uploadState) {
				case NOT_UPLOADING: {
					uploadMode = UPLOAD_SELECTED;
					uploadState = UPLOADING;
					currentUploadVideoIndex = 0;
					currentUploadPlatform = YOUTUBE_SITE_ID;
					uploadYoutubeVideo(currentUploadVideoIndex);
					break;
				}
				case UPLOADING: {
					uploadState = STOPPING_UPLOAD;
					break;

				}
				case PAUSING_UPLOAD: {
					uploadState = STOPPING_UPLOAD;
					break;

				}
				case PAUSED: {
					uploadState = NOT_UPLOADING;
					break;
				}
				case STOPPING_UPLOAD: {
					uploadState = STOPPING_UPLOAD;
					break;
				}
				}
				updateBarGUI(uploadState, currentUploadPlatform);
			}
		}
	}

	void updateBarGUI(int uploadState, int platform) {

		switch (uploadState) {
		case NOT_UPLOADING: {
			uploadBarLeftBang.setImages(uploadAllImages[0], uploadAllImages[1],
					uploadAllImages[2]);
			uploadBarLeftLabel.setText(uploadAllLabel);
			uploadBarRightBang.setImages(uploadImages[0], uploadImages[1],
					uploadImages[2]);
			uploadBarRightLabel.setText(uploadLabel);
			uploadBarMainLabel.setText(noUploadLabel);
			break;
		}
		case UPLOADING: {
			uploadBarLeftBang.setImages(pauseImages[0], pauseImages[1],
					pauseImages[2]);
			uploadBarLeftLabel.setText(pauseLabel);
			uploadBarRightBang.setImages(stopImages[0], stopImages[1],
					stopImages[2]);
			uploadBarRightLabel.setText(stopLabel);
			String mainLabelText = currentUploadLabel;
			if (platform == YOUTUBE_SITE_ID) {
				mainLabelText += "YOUTUBE";
			} else if (platform == VIMEO_SITE_ID) {
				mainLabelText += "VIMEO";
			} else if (platform == ARCHIVE_SITE_ID) {
				mainLabelText += "ARCHIVE.ORG";
			}
			uploadBarMainLabel.setText(mainLabelText);
			break;
		}
		case PAUSING_UPLOAD: {
			uploadBarLeftBang.setImages(resumeImages[0], resumeImages[1],
					resumeImages[2]);
			uploadBarLeftLabel.setText(resumeLabel);
			uploadBarRightBang.setImages(stopImages[0], stopImages[1],
					stopImages[2]);
			uploadBarRightLabel.setText(stopLabel);
			uploadBarMainLabel.setText(pausedUploadLabel_0);
			break;
		}
		case PAUSED: {
			uploadBarLeftBang.setImages(resumeImages[0], resumeImages[1],
					resumeImages[2]);
			uploadBarLeftLabel.setText(resumeLabel);
			uploadBarRightBang.setImages(stopImages[0], stopImages[1],
					stopImages[2]);
			uploadBarRightLabel.setText(stopLabel);
			uploadBarMainLabel.setText(pausedUploadLabel);
			break;
		}
		case STOPPING_UPLOAD: {
			uploadBarLeftBang.setImages(resumeImages[0], resumeImages[1],
					resumeImages[2]);
			uploadBarLeftLabel.setText(resumeLabel);
			uploadBarRightBang.setImages(stopDisabledImage, stopDisabledImage,
					stopDisabledImage);
			uploadBarRightLabel.setText(stopLabel);
			uploadBarMainLabel.setText(stopedUploadLabel_0);
			break;
		}
		}
	}

	private void setVideoInfo(VideoInfo videoInfo) {
		videoIdTextField.setText(videoInfo.videoID);
		uriTextField.setText(videoInfo.uri);
		durationTextField.setText(videoInfo.duration);
		titleTextField.setText(videoInfo.title);
		dateTextField.setText(videoInfo.date);
		descriptionTextField.setText(videoInfo.description);
		creatorTextField.setText(videoInfo.place);
		data_1TextField.setText(videoInfo.data_1);
		data_2TextField.setText(videoInfo.data_2);
		data_3TextField.setText(videoInfo.data_3);
		typeTextField.setText(videoInfo.type);
		tagsTextField.setText(videoInfo.tags);
		authorsTextField.setText(videoInfo.authors);
	}

	private void initializeArchive() {

	}

	private void completeArchiveUpload() {

	}

	private boolean uploadVimeoVideo(int videoIndex) {

		if (vimeoClient != null) {

			File[] videoFiles = vimeoVideoFolder.listFiles();
			File videoFile = videoFiles[videoIndex];
			VideoInfo videoInfo = vimeoVideos.get(videoFile);
			setVideoInfo(videoInfo);
			String keywords = videoInfo.tags.replace(";", ", ");

			String descripcion = videoInfo.description + "\n" + "Autor/es: "
					+ videoInfo.authors + "\nFecha: " + videoInfo.date;

			descripcion = parseUploadString(descripcion);

			println(descripcion);

			println("uploading " + videoInfo.title + " to youtube");

			vimeoClient.uploadVideo(videoFile, videoInfo.title, descripcion,
					keywords);

			return true;
		} else
			return false;

	}

	private void initializeVimeo() {

		// TODO Filter .mp4 or .mov files
		File[] videoFiles = vimeoVideoFolder.listFiles();
		vimeoVideos = new Hashtable<File, VideoInfo>();

		for (int i = 0; i < videoFiles.length; i++) {
			int selectedInfoIndex = -1;
			for (int j = 0; j < videoInfoArr.size(); j++) {
				String videoNameMov = videoInfoArr.get(j).videoID + ".mov";
				String videoNameMp4 = videoInfoArr.get(j).videoID + ".mp4";
				if (videoNameMov.equals(videoFiles[i].getName())
						|| videoNameMp4.equals(videoFiles[i].getName())) {
					selectedInfoIndex = j;
					break;
				}
			}
			if (selectedInfoIndex >= 0) {
				vimeoVideos.put(videoFiles[i],
						videoInfoArr.get(selectedInfoIndex));
			}
		}

		vimeoClient = null;
		try {
			vimeoClient = new VimeoUploadClient(VIMEO_API_KEY, VIMEO_API_SECRET);

		} catch (IOException e) {
			System.out.println("Fail accessing IO with vimeo ");
			e.setStackTrace(null);
		} catch (APIException e) {
			System.out.println("Fail accessing vimeo ");
			e.setStackTrace(null);
		}

	}

	private void completeVimeoUpload() {

		File[] videoFiles = vimeoVideoFolder.listFiles();

		if (vimeoClient != null) {
			for (int i = 0; i < videoFiles.length; i++) {
				File videoFile = videoFiles[i];
				VideoInfo videoInfo = vimeoVideos.get(videoFile);
				String keywords = videoInfo.tags.replace(";", ", ");

				String descripcion = videoInfo.description + "\n"
						+ "Autor/es: " + videoInfo.authors + "\nFecha: "
						+ videoInfo.date;

				descripcion = parseUploadString(descripcion);

				println(descripcion);

				println("uploading " + videoInfo.title + " to youtube");

				vimeoClient.uploadVideo(videoFile, videoInfo.title,
						descripcion, keywords);
			}
		}
	}

	private boolean uploadYoutubeVideo(int videoIndex) {

		if (youtubeClient != null) {

			File[] videoFiles = vimeoVideoFolder.listFiles();
			File videoFile = videoFiles[videoIndex];
			VideoInfo videoInfo = vimeoVideos.get(videoFile);
			setVideoInfo(videoInfo);
			String keywords = videoInfo.tags.replace(";", ", ");

			String descripcion = videoInfo.description + "\n" + "Autor/es: "
					+ videoInfo.authors + "\nFecha: " + videoInfo.date;

			descripcion = parseUploadString(descripcion);

			println(descripcion);

			println("uploading " + videoInfo.title + " to youtube");

			youtubeClient.uploadVideo(videoFile, videoInfo.title, descripcion,
					keywords);

			return true;
		} else
			return false;
	}

	private void initializeYoutube() {

		// TODO Filter .mp4 or .mov files
		File[] videoFiles = youtubeVideoFolder.listFiles();
		youtubeVideos = new Hashtable<File, VideoInfo>();

		for (int i = 0; i < videoFiles.length; i++) {
			int selectedInfoIndex = -1;
			for (int j = 0; j < videoInfoArr.size(); j++) {
				String videoNameMov = videoInfoArr.get(j).videoID + ".mov";
				String videoNameMp4 = videoInfoArr.get(j).videoID + ".mp4";
				if (videoNameMov.equals(videoFiles[i].getName())
						|| videoNameMp4.equals(videoFiles[i].getName())) {
					selectedInfoIndex = j;
					break;
				}
			}
			if (selectedInfoIndex >= 0) {
				youtubeVideos.put(videoFiles[i],
						videoInfoArr.get(selectedInfoIndex));
			}
		}

		youtubeClient = new YouTubeUploadClient(YOUTUBE_USER_NAME,
				YOUTUBE_PASSWORD, YOUTUBE_DEVELOPER_KEY);

	}

	private void completeYoutubeUpload() {

		File[] videoFiles = youtubeVideoFolder.listFiles();

		for (int i = 0; i < videoFiles.length; i++) {
			File videoFile = videoFiles[i];
			VideoInfo videoInfo = youtubeVideos.get(videoFile);
			String keywords = videoInfo.tags.replace(";", ", ");

			String descripcion = videoInfo.description; // + "\n" + "Autor/es: "
														// + videoInfo.authors +
														// "\nFecha: " +
														// videoInfo.date;
			// descripcion = descripcion.substring(0,descripcion.length()-1);

			descripcion = parseUploadString(descripcion);

			println("uploading " + videoInfo.title + " to youtube");

			youtubeClient.uploadVideo(videoFile, videoInfo.title, descripcion,
					keywords);

		}
	}

	String parseUploadString(String input) {

		int startIndex = input.indexOf('<');
		while (startIndex > -1) {
			int endIndex = input.indexOf('>', startIndex);
			if (endIndex >= 0) {
				input = input.substring(0, startIndex)
						+ input.substring(endIndex + 1, input.length());
				startIndex = input.indexOf('<');
			} else
				startIndex = -1;
		}
		return input;
	}

	private ArrayList<VideoInfo> readMovieCVS() {

		ArrayList<VideoInfo> videoArr = new ArrayList<VideoInfo>();

		try {

			CsvReader videoInfoReader= new CsvReader(csvFile,',',Charset.forName("UTF-8"));
			CsvWriter videoInfoWriter =  new CsvWriter(csvFile_out,',',Charset.forName("UTF-8"));
			
			// header:
			// id,uri,duration,title,date,description,creator,data_1,data_2,data_3,type,tags,authors
			videoInfoReader.readHeaders();
			String[] readHeaders = videoInfoReader.getHeaders();
			int aditionalHeaders = 2; // local_video_title, upload_log_file
			String[] writeHeaders = new String[readHeaders.length + aditionalHeaders];
			for(int i = 0; i < readHeaders.length; i++) {
				writeHeaders[i] = readHeaders[i];
			}
			writeHeaders[readHeaders.length] = "local_video_title";
			writeHeaders[readHeaders.length+1] = "upload_log_file";
			String headers = "";
			for(int i = 0; i < writeHeaders.length-1; i++) {
				headers += writeHeaders[i] + ",";
			}
			headers += writeHeaders[writeHeaders.length-1];
			videoInfoWriter.write(headers); 
			videoInfoWriter.close();
		
			
			int counter = 0;
			while (videoInfoReader.readRecord()) {
				VideoInfo videoInfo = new VideoInfo();
				videoInfo.arrID = counter;
				counter++;
				videoInfo.videoID = videoInfoReader.get("id");
				videoInfo.uri = videoInfoReader.get("uri");
				videoInfo.duration = videoInfoReader.get("duration");
				videoInfo.title = parseUploadString(videoInfoReader.get("title"));
				videoInfo.date = parseUploadString(videoInfoReader.get("date"));
				videoInfo.description = parseUploadString(videoInfoReader
						.get("description"));
				videoInfo.place = parseUploadString(videoInfoReader.get("creator"));
				videoInfo.data_1 = videoInfoReader.get("data_1");
				videoInfo.data_2 = videoInfoReader.get("data_2");
				videoInfo.data_3 = videoInfoReader.get("data_3");
				videoInfo.type = parseUploadString(videoInfoReader.get("type"));
				videoInfo.tags = parseUploadString(videoInfoReader.get("tags"));
				videoInfo.authors = parseUploadString(videoInfoReader.get("authors"));

				videoInfo.printData();
				print(counter + " : ");
				videoArr.add(videoInfo);
			}

			videoInfoReader.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return videoArr;
	}

	@SuppressWarnings("deprecation")
	private void initGui(ArrayList<VideoInfo> videoInfoArr) {

		cp5 = new ControlP5(this);
		cp5.setAutoDraw(false);
		PFont p5Font = cp5.getFont().getFont();
		textFont(p5Font);

		myTextarea = cp5.addTextarea("txt")
				.setPosition(0, height - textAreaHeight).setSize(width, 200)
				.setFont(createFont("arial", 12)).setLineHeight(14)
				// .setLabel("Log")
				.setColor(color(250)).setColorBackground(color(255, 150))
				.setColorForeground(color(255, 250));
		;
		myTextarea.setText("Consola de logs");

		medialabLogo = loadImage("MedialabLogo.jpg");
		youtubeLogo = loadImage("YoutubeLogo.jpg");
		youtubeLogo.resize(platformLogoWidth, platformLogoWidth);
		vimeoLogo = loadImage("VimeoLogo.png");
		vimeoLogo.resize(platformLogoWidth, platformLogoWidth);
		archiveLogo = loadImage("ArchiveLogo.png");
		archiveLogo.resize(platformLogoWidth, platformLogoWidth);

		uploaderBarY = height - textAreaHeight - uploaderBarHeight;
		uploadAllImages = new PImage[3];
		uploadAllImages[0] = loadImage("Update.png");
		uploadAllImages[1] = loadImage("green_Update.png");
		uploadAllImages[2] = loadImage("hover_Update.png");
		uploadImages = new PImage[3];
		uploadImages[0] = loadImage("Up.png");
		uploadImages[1] = loadImage("green_Up.png");
		uploadImages[2] = loadImage("hover_Up.png");
		pauseImages = new PImage[3];
		pauseImages[0] = loadImage("Pause.png");
		pauseImages[1] = loadImage("green_Pause.png");
		pauseImages[2] = loadImage("hover_Pause.png");
		resumeImages = new PImage[3];
		resumeImages[0] = loadImage("Play.png");
		resumeImages[1] = loadImage("green_Play.png");
		resumeImages[2] = loadImage("hover_Play.png");
		stopImages = new PImage[3];
		stopImages[0] = loadImage("Stop.png");
		stopImages[1] = loadImage("green_Stop.png");
		stopImages[2] = loadImage("hover_Stop.png");

		stopDisabledImage = createImage(stopImages[0].width,
				stopImages[0].height, PApplet.ARGB);
		stopDisabledImage.copy(stopImages[0], 0, 0, stopImages[0].width,
				stopImages[0].height, 0, 0, stopDisabledImage.width,
				stopDisabledImage.height);
		stopDisabledImage.filter(PApplet.GRAY);

		platformUploadImages = new PImage[3];
		platformUploadImages[0] = loadImage("Upload.png");
		platformUploadImages[1] = loadImage("green_Upload.png");
		platformUploadImages[2] = loadImage("hover_Upload.png");
		platformUndoImages = new PImage[3];
		platformUndoImages[0] = loadImage("Undo.png");
		platformUndoImages[1] = loadImage("green_Undo.png");
		platformUndoImages[2] = loadImage("hover_Undo.png");
		platformUploadedImage = loadImage("Yes.png");

		textAlign(PApplet.CENTER);

		uploadBarLeftBang = cp5
				.addBang("uploadBarLeftBang")
				.setPosition(5, uploaderBarY + 5)
				.setImages(uploadAllImages[0], uploadAllImages[1],
						uploadAllImages[2]).setSize(uploadAllImages[0]);

		uploadBarLeftLabel = cp5
				.addTextlabel("uploadBarLeftLabel")
				.setText(uploadAllLabel)
				.setPosition(
						uploadBarLeftBang.getPosition().x,
						uploadBarLeftBang.getPosition().y
								+ uploadBarLeftBang.getHeight() + 3)
				.setColorValue(0xff11AAff);

		uploadBarRightBang = cp5
				.addBang("uploadBarRightBang")
				.setPosition(5 + (int) (uploadAllImages[0].width * 2),
						uploaderBarY + 5)
				.setImages(uploadImages[0], uploadImages[1], uploadImages[2])
				.setSize(uploadImages[0]);

		uploadBarRightLabel = cp5
				.addTextlabel("uploadBarRightLabel")
				.setText(uploadLabel)
				.setPosition(
						uploadBarRightBang.getPosition().x,
						uploadBarRightBang.getPosition().y
								+ uploadBarLeftBang.getHeight() + 3)
				.setColorValue(0xff11AAff);

		platformButtonsHeight = uploaderBarY + platformButtonsOffsetY;

		youtubeBang = cp5
				.addBang("youtubeBang")
				.setPosition(width - platformsButtonOffset,
						platformButtonsHeight)
				.setImages(platformUploadImages[0], platformUploadImages[1],
						platformUploadImages[2])
				.setSize(platformUploadImages[0]);

		vimeoBang = cp5
				.addBang("vimeoBang")
				.setPosition(
						width - platformsButtonOffset + 2 * youtubeLogo.width
								+ platformUploadOffset, platformButtonsHeight)
				.setImages(platformUploadImages[0], platformUploadImages[1],
						platformUploadImages[2])
				.setSize(platformUploadImages[0]);

		archiveBang = cp5
				.addBang("archiveBang")
				.setPosition(
						width - platformsButtonOffset + 4 * youtubeLogo.width
								+ 2 * platformUploadOffset,
						platformButtonsHeight)
				.setImages(platformUploadImages[0], platformUploadImages[1],
						platformUploadImages[2])
				.setSize(platformUploadImages[0]);

		mainLabelX = uploadBarRightBang.getPosition().x + 2
				* uploadBarRightBang.getWidth() + 5;
		uploadBarMainLabel = cp5.addTextlabel("noUploadLabel")
				.setText(noUploadLabel)
				.setPosition(mainLabelX, uploaderBarY + 13)
				.setColorValue(0xff11AAff);

		textAlign(PApplet.LEFT);

		videoList = new VideoList(this, cp5, "videoList", videoListX,
				videoListY, videoListWidth, height - textAreaHeight
						- uploaderBarHeight - videoListY, videoInfoArr);

		int yOffset = videoInfoYOffset / 2;

		videoIdTextField = cp5.addTextfield("videoID")
				.setPosition(videoListWidth + videoInfoXOffset, yOffset)
				.setSize(videoInfoTextWidth, videoInfoTextHeight)
				.setAutoClear(false).setLock(true);

		yOffset += videoInfoYOffset + videoInfoTextHeight;

		uriTextField = cp5.addTextfield("uri")
				.setPosition(videoListWidth + videoInfoXOffset, yOffset)
				.setSize(videoInfoTextWidth, videoInfoTextHeight)
				.setAutoClear(false).setLock(true);

		yOffset += videoInfoYOffset + videoInfoTextHeight;

		durationTextField = cp5.addTextfield("duration")
				.setPosition(videoListWidth + videoInfoXOffset, yOffset)
				.setSize(videoInfoTextWidth, videoInfoTextHeight)
				.setAutoClear(false).setLock(true);

		yOffset += videoInfoYOffset + videoInfoTextHeight;

		titleTextField = cp5.addTextfield("title")
				.setPosition(videoListWidth + videoInfoXOffset, yOffset)
				.setSize(videoInfoTextWidth, videoInfoTextHeight)
				.setAutoClear(false).setLock(true);

		yOffset += videoInfoYOffset + videoInfoTextHeight;

		dateTextField = cp5.addTextfield("date")
				.setPosition(videoListWidth + videoInfoXOffset, yOffset)
				.setSize(videoInfoTextWidth, videoInfoTextHeight)
				.setAutoClear(false).setLock(true);

		yOffset += videoInfoYOffset + videoInfoTextHeight;

		descriptionTextField = cp5.addTextfield("description")
				.setPosition(videoListWidth + videoInfoXOffset, yOffset)
				.setSize(videoInfoTextWidth, videoInfoTextHeight)
				.setAutoClear(false).setLock(true);

		yOffset += videoInfoYOffset + videoInfoTextHeight;

		creatorTextField = cp5.addTextfield("creator")
				.setPosition(videoListWidth + videoInfoXOffset, yOffset)
				.setSize(videoInfoTextWidth, videoInfoTextHeight)
				.setAutoClear(false).setLock(true);

		yOffset += videoInfoYOffset + videoInfoTextHeight;

		data_1TextField = cp5.addTextfield("data_1")
				.setPosition(videoListWidth + videoInfoXOffset, yOffset)
				.setSize(100, videoInfoTextHeight).setAutoClear(false)
				.setLock(true);

		data_2TextField = cp5.addTextfield("data_2")
				.setPosition(videoListWidth + videoInfoXOffset + 120, yOffset)
				.setSize(100, videoInfoTextHeight).setAutoClear(false)
				.setLock(true);

		data_3TextField = cp5.addTextfield("data_3")
				.setPosition(videoListWidth + videoInfoXOffset + 240, yOffset)
				.setSize(100, videoInfoTextHeight).setAutoClear(false)
				.setLock(true);

		yOffset += videoInfoYOffset + videoInfoTextHeight;

		typeTextField = cp5.addTextfield("type")
				.setPosition(videoListWidth + videoInfoXOffset, yOffset)
				.setSize(videoInfoTextWidth, videoInfoTextHeight)
				.setAutoClear(false).setLock(true);

		yOffset += videoInfoYOffset + videoInfoTextHeight;

		tagsTextField = cp5.addTextfield("tags")
				.setPosition(videoListWidth + videoInfoXOffset, yOffset)
				.setSize(videoInfoTextWidth, videoInfoTextHeight)
				.setAutoClear(false).setLock(true);

		yOffset += videoInfoYOffset + videoInfoTextHeight;

		authorsTextField = cp5.addTextfield("authors")
				.setPosition(videoListWidth + videoInfoXOffset, yOffset)
				.setSize(videoInfoTextWidth, videoInfoTextHeight)
				.setAutoClear(false).setLock(true);

	}

	static public void main(String[] passedArgs) {
		String[] appletArgs = new String[] { "es.medialabprado.muploader.VideoUploaderP5" };
		if (passedArgs != null) {
			PApplet.main(concat(appletArgs, passedArgs));
		} else {
			PApplet.main(appletArgs);
		}
	}
}
