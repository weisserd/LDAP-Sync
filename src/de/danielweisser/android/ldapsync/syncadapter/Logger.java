package de.danielweisser.android.ldapsync.syncadapter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.os.Environment;
import android.util.Log;
import de.danielweisser.android.ldapsync.Constants;

public class Logger {

	private BufferedWriter f;
	public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
	public static final String DATE_FORMAT_NOW_FILE = "yyyy-MM-dd-HH-mm-ss";

	public static String now() {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
		return sdf.format(cal.getTime());
	}

	public static String nowFile() {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW_FILE);
		return sdf.format(cal.getTime());
	}

	public void d(String message) {
		try {
			if (f != null) {
				f.write(now() + ": " + message + "\n");
				f.flush();
			}
		} catch (IOException e) {
			Log.e("TAG", e.getMessage(), e);
		}
	}

	public void startLogging() {
		File sdCard = Environment.getExternalStorageDirectory();
		File dir = new File(sdCard.getAbsolutePath() + Constants.SDCARD_FOLDER);
		dir.mkdirs();
		File file = new File(dir, nowFile() + "_sync.log");

		try {
			f = new BufferedWriter(new FileWriter(file));
		} catch (FileNotFoundException e) {
			Log.e("TAG", e.getMessage(), e);
		} catch (IOException e) {
			Log.e("TAG", e.getMessage(), e);
		}
	}

	public void stopLogging() {
		try {
			if (f != null) {
				f.close();
			}
		} catch (IOException e) {
			Log.e("TAG", e.getMessage(), e);
		}
	}

}
