package es.medialabprado.muploader;

/* 
 VideoInfo

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

public class VideoInfo {
	int arrID;
	public String videoID;
	public String uri;
	public String duration;
	public String title;
	public String date;
	public String description;
	public String place;
	public String data_1;
	public String data_2;
	public String data_3;
	public String type;
	public String tags;
	public String authors;

	public boolean medialabLoaded;
	public boolean youtubeLoaded;
	public boolean vimeoLoaded;
	public boolean archiveLoaded;

	public VideoInfo() {
		float auxF = (float) Math.random();

		if (auxF > 0.5) {
			medialabLoaded = true;
		} else {
			medialabLoaded = false;
		}

		auxF = (float) Math.random();
		if (auxF > 0.5) {
			youtubeLoaded = true;
		} else {
			youtubeLoaded = false;
		}

		auxF = (float) Math.random();
		if (auxF > 0.5) {
			vimeoLoaded = true;
		} else {
			vimeoLoaded = false;
		}

		auxF = (float) Math.random();
		if (auxF > 0.5) {
			archiveLoaded = true;
		} else {
			archiveLoaded = false;
		}
	}

	void printData() {
		String message = "[" + this + "] " + videoID + ":" + uri + ":"
				+ duration + ":" + title + ":" + date + ":" + description + ":"
				+ place + ":" + data_1 + ":" + data_2 + ":" + data_3 + ":"
				+ type + ":" + tags + ":" + authors;
		System.out.println(message);
	}

}
