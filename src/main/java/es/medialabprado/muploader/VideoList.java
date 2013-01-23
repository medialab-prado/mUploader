package es.medialabprado.muploader;

/* 
 VideoList

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

import processing.core.*;
import controlP5.*;
import java.util.ArrayList;

public class VideoList {

	VListBox list;

	ControlP5 cp5;
	PApplet papplet;

	ArrayList<VBoxItemIcon> icons;

	int platformLogoWidth = 15;
	int platformUploadOffset = 10;

	private PImage youtubeLogo;

	private PImage vimeoLogo;

	private PImage archiveLogo;

	int listX;
	int listY;
	int listWidth = 400;
	int listHeight = 500;
	String listName;
	ArrayList<VideoInfo> videoInfos;

	public VideoList(PApplet papplet, ControlP5 cp5, String listName,
			int listX, int listY, int listWidth, int listHeight,
			ArrayList<VideoInfo> videoInfos) {

		this.papplet = papplet;
		this.cp5 = cp5;
		this.listX = listX;
		this.listY = listY;
		this.listWidth = listWidth;
		this.listHeight = listHeight;
		this.videoInfos = videoInfos;
		this.listName = listName;

		list = new VListBox(cp5, listName);
		PFont p5Font = cp5.getFont().getFont();

		list.setPosition(listX, listY).setSize(listWidth, listHeight)
				.setItemHeight(25);
		icons = new ArrayList<VBoxItemIcon>();

		// for each ListBoxItem added to our custom MyListBox, an icon is added
		// to the ArrayList icons, these icons will then be used to change the
		// View of the ListBoxItems visible.

		PImage[] logos = new PImage[3];

		youtubeLogo = papplet.loadImage("YoutubeLogo.jpg");
		youtubeLogo.resize(platformLogoWidth, platformLogoWidth);
		logos[0] = youtubeLogo;

		vimeoLogo = papplet.loadImage("VimeoLogo.png");
		vimeoLogo.resize(platformLogoWidth, platformLogoWidth);
		logos[1] = vimeoLogo;

		archiveLogo = papplet.loadImage("ArchiveLogo.jpg");
		archiveLogo.resize(platformLogoWidth, platformLogoWidth);
		logos[2] = archiveLogo;

		for (int i = 0; i < videoInfos.size(); i++) {
			VideoInfo videoInfo = videoInfos.get(i);
			int id = icons.size();
			list.addCustomItem("item " + id, id, new VListBoxButton());
			icons.add(i, new VBoxItemIcon(videoInfo, listWidth, listHeight,
					logos, p5Font));
		}
	}

	class VListBox extends ListBox {

		VListBox(ControlP5 cp5, String theName) {
			super(cp5, cp5.getTab("default"), theName, 0, 0, 100, 10);
		}

		// now we have to add a custom function to add Items to the ListBox so
		// that
		// we can set our custom view of each item.
		public void addCustomItem(String theName, int theValue,
				ControllerView<Button> theView) {
			addItem(theName, theValue);
			// when overriding a view, the properties of the previous view are
			// lost,
			// here the label will get lost in favor of the image, but you can
			// manually add
			// a label to your custom view of course (custom view class see
			// below)
			buttons.get(buttons.size() - 1).setView(theView);
		}

	}

	public class VListBoxButton implements ControllerView<Button> {

		public void display(PApplet theApplet, Button theButton) {
			theApplet.fill(!theButton.isPressed() ? 200 : 0xFFFF0000);
			theApplet.rect(0, 0, theButton.getWidth(), theButton.getHeight());
			int id = (int) theButton.getValue();

			icons.get(id).display(theApplet, theButton.isMousePressed(),
					theButton.isMouseOver());

		}
	}

	public class VBoxItemIcon {

		PGraphics img;
		String videoTitle;
		int listWidth;
		int listHeight;
		boolean medialabLoaded;
		boolean youtubeLoaded;
		boolean vimeoLoaded;
		boolean archiveLoaded;
		PImage[] logos;
		static final int YOUTUBE_LOGO_ID = 0;
		static final int VIMEO_LOGO_ID = 1;
		static final int ARCHIVE_LOGO_ID = 2;
		PFont font;
		int logoGap = 5;

		VBoxItemIcon(VideoInfo videoInfo, int listWidth, int listHeight,
				PImage[] logos, PFont font) {

			this.listWidth = listWidth;
			this.listHeight = listHeight;
			this.medialabLoaded = videoInfo.medialabLoaded;
			this.youtubeLoaded = videoInfo.youtubeLoaded;
			this.vimeoLoaded = videoInfo.vimeoLoaded;
			this.archiveLoaded = videoInfo.archiveLoaded;
			this.logos = logos;

			int maxLogosWidth = (int) (5.5 * (logos[0].width + logoGap));

			int origSize = videoInfo.title.length();
			this.videoTitle = new String(videoInfo.title);
			while (papplet.textWidth(this.videoTitle) > listWidth
					- maxLogosWidth) {
				this.videoTitle = this.videoTitle.substring(0,
						this.videoTitle.length() - 1);

			}
			this.videoTitle = this.videoTitle.toUpperCase();
			if (origSize > this.videoTitle.length())
				this.videoTitle += "...";
			this.font = font;

			img = papplet.createGraphics(listWidth - 5, 25);

		}

		public void display(PApplet theApplet, boolean isPressed, boolean isOver) {

			int logoOffsetX = listWidth;

			img.beginDraw();
			img.background(0);

			img.noStroke();
			img.textFont(font);
			if (isPressed) {
				img.fill(0x00, 0xB3, 0xFF);
			} else if (isOver) {
				img.fill(150);
			} else {
				img.fill(255);
			}
			img.text(videoTitle, 5, 17);

			if (youtubeLoaded) {
				logoOffsetX -= logos[YOUTUBE_LOGO_ID].width + logoGap;
				img.image(logos[YOUTUBE_LOGO_ID], logoOffsetX, 5);

			}
			if (vimeoLoaded) {
				logoOffsetX -= logos[VIMEO_LOGO_ID].width + logoGap;
				img.image(logos[VIMEO_LOGO_ID], logoOffsetX, 5);

			}
			if (archiveLoaded) {
				logoOffsetX -= logos[ARCHIVE_LOGO_ID].width + logoGap;
				img.image(logos[ARCHIVE_LOGO_ID], logoOffsetX, 5);
			}
			img.endDraw();

			theApplet.image(img, 0, 0);

		}
	}

}
