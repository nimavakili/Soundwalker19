package com.minimv.soundwalker;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import com.bugsense.trace.BugSenseHandler;

//import com.minimv.soundwalker.R;
import android.content.Context;
import android.content.SharedPreferences;
//import android.content.res.AssetFileDescriptor;
import android.location.Location;
import android.media.AudioManager;
import android.media.MediaPlayer;
//import android.util.Log;
import android.widget.Toast;

public class NodeManager {

	private MediaPlayer mPlayer;
	public static Context mContext;
	private String path = "";
	private double lat = 0;
	private double lon = 0;
	private double radO = 0;
	private double radI = 0;
	public static SharedPreferences lastPositions;
	public static SharedPreferences.Editor positionEditor;
	public boolean invalid = false;
    private Timer timer;
    private TimerTask timerTask;
    private int vol = 0, curVol = 0;
	private final int maxVolume = 100;
	private final int fadeFactor = 50;
	
	public NodeManager(String p) {
// TODO: Error handling
		try {
			//mContext = context;
			path = GPSService.sdFolder.getAbsolutePath() + "/" + p;
			//path = "nodes/" + p;
			String[] split = p.replace(".mp3", "").replace("._", "").split(",");
			//String[] split = p.replace(".mp3", "").split(",");
			lat = Double.parseDouble(split[0].trim());
			lon = Double.parseDouble(split[1].trim());
			if (split.length < 4) {
				radO = Double.parseDouble(split[2].trim());
				radI = 0;
			}
			else {
				radI = Math.min(Math.abs(Double.parseDouble(split[2].trim())), Math.abs(Double.parseDouble(split[3].trim())));
				radO = Math.max(Math.abs(Double.parseDouble(split[2].trim())), Math.abs(Double.parseDouble(split[3].trim())));
			}
			//Log.v("FILES", "Lat: " + lat + ", Lon: " + lon + ", RadO: " + radO + ", RadI: " + radI);
			//lastPositions = mContext.getSharedPreferences("LAST_POSITIONS", 0);
			//positionEditor = lastPositions.edit();
		}
		catch (Exception e) {
			invalid = true;
			Toast.makeText(mContext.getApplicationContext(), "Invalid MP3 name format!\n" + p + mContext.getResources().getString(R.string.invalid_format), Toast.LENGTH_LONG).show();
			e.printStackTrace();
	        BugSenseHandler.sendException(e);
		}
	}
		
	public double getLat() {
		return lat;
	}

	public double getLon() {
		return lon;
	}

	public double getRadI() {
		return radI;
	}

	public double getRadO() {
		return radO;
	}
	
	public double distanceTo(double _lat, double _lon) {
		//double latDiff = Math.abs(_lat - lat);
		//double lonDiff = Math.abs(_lon - lon);
		//double dist = Math.sqrt(Math.pow(latDiff, 2) + Math.pow(lonDiff, 2));
		float[] results = new float[3];
		Location.distanceBetween(lat, lon, _lat, _lon, results);
		return results[0];
	}
	
	public boolean isInside(double _lat, double _lon) {
		double dist = distanceTo(_lat, _lon);
		if (dist < radO) {
			return true;
		}
		return false;
	}

	private void preparePlayer() {
		//String idStr = "audio_";
		//if (audioId < 10) idStr = idStr + "0";
		//idStr = idStr + audioId;
		//Uri uri = Uri.parse("android.resource://com.minimv.soundwalker/raw/" + idStr);
		mPlayer = new MediaPlayer();
		try {
			//Log.v("MP3", path);
			//AssetFileDescriptor afd = mContext.getAssets().openFd(path);
			//mPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
			mPlayer.setDataSource(path);
		} catch (IllegalArgumentException e1) {
			e1.printStackTrace();
		} catch (SecurityException e1) {
			e1.printStackTrace();
		} catch (IllegalStateException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
	        public void onPrepared(MediaPlayer mp) {
	        	mPlayer.setLooping(true);
	        	int curPos = lastPositions.getInt(path, 0);
	        	mPlayer.seekTo(curPos);
	        	mPlayer.start();
	        }
	    });
		mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
	        public void onCompletion(MediaPlayer mp) {
	        }
	    });
	}
	
	public void play() {
		curVol = 0;
		preparePlayer();
		try {
			mPlayer.prepareAsync();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
	}

	public void stop() {
		try {
			int curPos = mPlayer.getCurrentPosition();
			positionEditor.putInt(path, curPos);
			mPlayer.stop();
			mPlayer.release();
			mPlayer = null;
			positionEditor.commit();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
		if (timer != null) {
			timer.cancel();
			timer.purge();
		}
		curVol = 0;
	}

	public boolean isPlaying() {
		if (mPlayer != null) {
			return mPlayer.isPlaying();
		}
		return false;
	}
	
	public float setVolume(double lat, double lon) {
        double dist = distanceTo(lat, lon);
		if (radO == radI)
			vol = maxVolume;
		else {
			vol = (int) Math.max(((dist - radI)/(radO - radI))*maxVolume, 0);
			vol = maxVolume - vol;
		}
		
		//Log.v("Volume", "" + vol);
		setVolume(vol);
		
		return vol;
	}
	
	public void setVolume(int volume) {
		vol = volume;
		if (timer != null) {
			timer.cancel();
			timer.purge();
		}
		
		if (vol != curVol) {
			timer = new Timer();
			timerTask = new TimerTask() {
		        @Override
		        public void run() {
		        	if (vol > curVol)
		        		curVol++;
		        	else if (vol < curVol)
		        		curVol--;
		        	else if (vol == curVol) {
		                timer.cancel();
		                timer.purge();
		            }
		            updateVolume();
		            if (curVol == 0) {
		            	stop();
		            }
		        }
		    };
		    timer.schedule(timerTask, 0, fadeFactor);
		}
	}
	
	private void updateVolume() {
		float logVol = 1 - (float)(Math.log(maxVolume - curVol)/Math.log(maxVolume));
		try {
			mPlayer.setVolume(logVol, logVol);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void reset() {
		if (positionEditor != null) {
			positionEditor.clear();
			positionEditor.commit();
		}
	}
}