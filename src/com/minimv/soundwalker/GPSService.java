package com.minimv.soundwalker;

import java.io.File;
//import java.io.IOException;
//import java.text.SimpleDateFormat;
//import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import com.minimv.soundwalker.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
//import android.content.res.AssetFileDescriptor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
//import android.view.Gravity;
import android.widget.Toast;

public class GPSService extends Service implements LocationListener {

	private final String TAG = "GPSService";
	private final IBinder mBinder = new LocationServiceBinder();
	private NotificationManager mNotificationManager;
	private NotificationCompat.Builder mBuilder;
	private LocationManager locationManager;
	private boolean GPSDisabled = false;
	private boolean isTracking = false;
	private boolean gotLock = false;
	//private Location curLocation = null;
	//private boolean bound = false;
	//private BufferedWriter out;
	//private SimpleDateFormat date = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
	//private String filename;
	//private TimerTask gpsLockCheck;
	//private BroadcastManager broadcaster;
	private long lastLocationMillis;
	//private float scale;
	//private int curMapI = -1;
	//private int curMapJ = -1;
	//private String lastSession = "NoSession";
	//private String lastLocation = "NoLocation";
	//private Handler handler;
	//private Runnable updateMapOC;
	//public static final double leftLon = -79.05f; //buffalo
	//public static final double widthLon = 0.4f;
	//public static final double leftLon = 3.759888d;
	//public static final double widthLon = 0.21992d;
	//public static final double topLat = 43.1f; //buffalo
	//public static final double heightLat = 0.3f;
	//public static final double topLat = 43.659722d;
	//public static final double heightLat = 0.159391d;
	//private int linesCt = 0;
	//private MediaPlayer mPlayer;
	private final Timer timer = new Timer();
	private final LockCheck lockCheck = new LockCheck();
	public final static String messageAction = "com.minimv.soundwalker.GPSService.Broadcast";
	public final static String messageLock = "com.minimv.soundwalker.GPSService.Lock";
	public final static String messageLoc = "com.minimv.soundwalker.GPSService.Location";
	//final static public String messageAcc = "com.minimv.soundwalker.GPSService.Acc";
	//final static public String messageLat = "com.minimv.soundwalker.GPSService.Lat";
	//final static public String messageLon = "com.minimv.soundwalker.GPSService.Lon";
	public final static String messageAct = "com.minimv.soundwalker.GPSService.Act";
	public final static String messageAll = "com.minimv.soundwalker.GPSService.All";
	public final static String messageIntro = "com.minimv.soundwalker.GPSService.Intro";
    private AudioManager audioManager;
    private AudioManager.OnAudioFocusChangeListener onAudioFocusChange;
    public int nodeCount = 0;
    public NodeManager[] node;
    private boolean hasAudioFocus = false;
    public static File sdFolder;
    public boolean noKill = false;
    private double[] lats = {0, 0, 0, 0, 0, 0};
    private double[] lons = {0, 0, 0, 0, 0, 0};
    private double[] accs = {-1, -1, -1, -1, -1, -1};
    private int curIndex = 0;
	private MediaPlayer mPlayer;
	//private boolean introPlaying = true;
    //private double preLat = 0, preLon = 0, preAcc = -1;
    
	@Override
	public void onCreate() {
		super.onCreate();
		Log.v(TAG, "OnCreate");

		//broadcaster = BroadcastManager.getInstance(this);
		
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
		
		lastLocationMillis = System.currentTimeMillis();
		timer.schedule(lockCheck, 5000, 5000);

		Intent intent = new Intent(this, MainActivity.class);
	    PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, Notification.FLAG_ONGOING_EVENT);
	    mBuilder = new NotificationCompat.Builder(this)
		        .setSmallIcon(R.drawable.ic_notif)
		        .setContentTitle(getResources().getString(R.string.is_tracking))
		        .setContentText(getResources().getString(R.string.searching))
	    		.setContentIntent(pIntent)
	    		.setOngoing(true)
	    		.setTicker(getResources().getString(R.string.is_tracking));
	    mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		//scale = getResources().getDisplayMetrics().widthPixels/320.0f;

		//handler = new Handler();

		/*updateMapOC = new Runnable() {
			@Override
			public void run() {
				try {
					GPSActivity.mapOver.setVisibility(View.VISIBLE);
					GPSActivity.center.setVisibility(View.INVISIBLE);
					if (!GPSDisabled) {
						GPSActivity.searching.setVisibility(View.VISIBLE);
					}
					else {
						GPSActivity.searching.setVisibility(View.INVISIBLE);
					}
					if (!GPSActivity.GPSAlert.isShowing()) {
						//Toast.makeText(getApplicationContext(), getResources().getString(R.string.no_lock), Toast.LENGTH_LONG).show();
						Toast lockToast = Toast.makeText(getApplicationContext(), getResources().getString(R.string.no_lock), Toast.LENGTH_LONG);
						lockToast.setGravity(Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL, 0, 0);
						lockToast.show();
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		};*/

		/*gpsLockCheck = new Runnable() {
			@Override
			public void run() {
				if (System.currentTimeMillis() - lastLocationMillis > 15000) {
					if (gotLock) {
						Log.v(TAG, "OnStatusChanged NoLock");
						gotLock = false;
						if (isTracking) {
							mBuilder.setContentText("No GPS lock");
							mNotificationManager.notify(1366, mBuilder.build());
							//startForeground(0, mBuilder.build());
						}
					}
				}
			}
		};*/

		//preparePlayer();
		
		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		
		onAudioFocusChange = new AudioManager.OnAudioFocusChangeListener() {
			@Override
			public void onAudioFocusChange(int focusChange) {
				if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
					hasAudioFocus = true;
				}
				else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
					hasAudioFocus = false;
					stopAllNodes();
					playIntro(false);
				}
				//else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
					// TODO lowering volume
				//}
			}
		};
		
		/*for (int i = 0; i < nodeCount; i++) {
			node[i] = new NodeManager(this, audioFiles[i]);
		}*/
		
		//loadMp3();
		NodeManager.mContext = this;
		NodeManager.lastPositions = getSharedPreferences("LAST_POSITIONS", 0);
		NodeManager.positionEditor = NodeManager.lastPositions.edit();
		NodeManager.reset();

		//playIntro(true);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.v(TAG, "OnStartCommand");
		//locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 10, this);
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.v(TAG, "OnBind");
		onRebind(intent);
		return mBinder;
	}

	@Override
	public void onRebind(Intent intent) {
		Log.v(TAG, "OnRebind");
		//bound = true;
		
		//if (!gotLock && !GPSDisabled)
			//GPSActivity.searching.setVisibility(View.VISIBLE);

		if (!isTracking() && node == null) {
			loadMp3();
		}
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		Log.v(TAG, "OnUnbind");
		//bound = false;
		if (!isTracking && !noKill) {
			Log.v(TAG, "OnUnbind StopSelf");
			locationManager.removeUpdates(this);
			/*try {
				timer.cancel();
				timer = null;
			}
			catch (Exception e) {
				e.printStackTrace();
			}*/
			stopSelf();
		}
		noKill = false;
		return true;
	}

	public class LocationServiceBinder extends Binder {
		GPSService getService() {
			return GPSService.this;
		}
	}
	
	public void onProviderDisabled(String provider) {
		GPSDisabled = true;
		//GPSActivity.searching.setVisibility(View.INVISIBLE);
		noLock();
	}

	public void onProviderEnabled(String provider) {
		GPSDisabled = false;
		//if (!gotLock && GPSActivity.gb.getVisibility() != View.VISIBLE && !GPSActivity.recordingMode && !GPSActivity.listeningMode)
		//	GPSActivity.searching.setVisibility(View.VISIBLE);
		noLock();
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
	}
	
	public void startTracking() {
		Log.v(TAG, "startTracking");

		if (node == null) {
			loadMp3();
		}
		startForeground(1366, mBuilder.build());
		isTracking = true;
		//linesCt = 0;

		//filename = date.format(new Date());
		//File outputFile = new File(Environment.getExternalStorageDirectory().getPath() + "/Grimpant", filename + ".csv");
		//lastSession = filename;
		//FileWriter gpxwriter = null;
		/*if (!outputFile.exists()) {
			try {
				//outputFile.mkdirs();
				outputFile.createNewFile();
				gpxwriter = new FileWriter(outputFile, true);
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (gpxwriter != null) {
				out = new BufferedWriter(gpxwriter);
			}
		}*/
		
		/*if (!gotLock && !GPSDisabled) {
			//Toast.makeText(getApplicationContext(), getResources().getString(R.string.no_lock), Toast.LENGTH_LONG).show();
			Toast lockToast = Toast.makeText(getApplicationContext(), getResources().getString(R.string.no_lock), Toast.LENGTH_LONG);
			lockToast.setGravity(Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL, 0, 0);
			//GPSActivity.searching.setVisibility(View.INVISIBLE);
			lockToast.show();
		}*/

		//if (gotLock) {
		//	startPlayers();
		//}
		
		//updateMap(43.610873d, 3.876306d);
		//updateMap(43.659722d, 3.759888d);
		//updateMap(43.659722d - 0.159391d, 3.759888d + 0.21992d);
		//updateMap(43.611962825625106d,3.876388898489914d);
		//GPSActivity.mapOver.setVisibility(View.INVISIBLE);
	}

	private void loadMp3() {
		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			Toast.makeText(getApplicationContext(), getResources().getString(R.string.no_sd), Toast.LENGTH_LONG).show();
			return;
		}

		sdFolder = new File(Environment.getExternalStorageDirectory().getPath() + "/Soundwalker");
		if (!sdFolder.exists()) {
			sdFolder = new File(Environment.getExternalStorageDirectory().getPath() + "/soundwalker");
		}
		if (!sdFolder.exists()) {
			File mtFolder = new File("/mnt");
			String[] mtList = mtFolder.list();
			for (int i = 0; i < mtList.length; i++) {
				if (mtList[i].toLowerCase(Locale.ENGLISH).contains("sd")) {
					sdFolder = new File("/mnt/" + mtList[i] + "/Soundwalker");
					if (!sdFolder.exists()) {
						sdFolder = new File("/mnt/" + mtList[i] + "/soundwalker");
					}
					if (sdFolder.exists())
						break;
				}
			}
		}
		if (!sdFolder.exists()) {
			Toast.makeText(getApplicationContext(), getResources().getString(R.string.no_folder), Toast.LENGTH_LONG).show();
			return;
		}
		String[] allFiles = sdFolder.list();
		//String[] audioFiles = sdFolder.list(filter)
		//Log.v("FILES", "Audio files count: " + audioFiles.length);
		int fileCount = allFiles.length;
		node = new NodeManager[fileCount];
		//NodeManager.mContext = this;
		//NodeManager.lastPositions = getSharedPreferences("LAST_POSITIONS", 0);
		//NodeManager.positionEditor = NodeManager.lastPositions.edit();
		nodeCount = 0;
		int error = 0;
		for (int i = 0; i < fileCount; i++) {
			if (allFiles[i].endsWith(".mp3")) {
				node[nodeCount] = new NodeManager(allFiles[i]);
				if (node[nodeCount].invalid)
					error++;
				nodeCount++;
			}
		}
		sendMessage(messageAll, nodeCount - error);
		if (nodeCount == 0) {
			Toast.makeText(getApplicationContext(), getResources().getString(R.string.no_mp3), Toast.LENGTH_LONG).show();
		}
		else if (error > 0) {
			//Toast.makeText(getApplicationContext(), getResources().getString(R.string.invalid_format), Toast.LENGTH_LONG).show();
		}
		
		/*String[] allFiles = new String[0];
		try {
			allFiles = getAssets().list("nodes");
			//for(String name : allFiles) {
			    //Log.v("NodePath", "nodes/" + name);
			//}
			int fileCount = allFiles.length;
			node = new NodeManager[fileCount];
			NodeManager.mContext = this;
			NodeManager.lastPositions = getSharedPreferences("LAST_POSITIONS", 0);
			NodeManager.positionEditor = NodeManager.lastPositions.edit();
			nodeCount = 0;
			int error = 0;
			for (int i = 0; i < fileCount; i++) {
				if (allFiles[i].endsWith(".mp3")) {
					node[nodeCount] = new NodeManager(allFiles[i]);
					if (node[nodeCount].invalid)
						error++;
					nodeCount++;
				}
			}
			sendMessage(messageAll, nodeCount - error);
		}
		catch (IOException e) {
			e.printStackTrace();
		}*/
	}
	
	public void onLocationChanged(Location location) {
		/*if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			return;
		}*/
		//curLocation = location;
		lastLocationMillis = System.currentTimeMillis();
		if (!gotLock) {
			Log.v(TAG, "OnLockChanged Lock");
			gotLock = true;
			//Toast lockToast = Toast.makeText(getApplicationContext(), getResources().getString(R.string.lock), Toast.LENGTH_LONG);
			//lockToast.setGravity(Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL, 0, 0);
			//GPSActivity.searching.setVisibility(View.INVISIBLE);
		    //lockToast.show();
			mBuilder.setContentText(getResources().getString(R.string.lock));
			if (isTracking) {
				mNotificationManager.notify(1366, mBuilder.build());
			}
		}
		
		//String stamp = date.format(new Date());
		//String comm = ",";

		//double weight = 1/Math.max(acc, 1);
		//double preWeight = 1/Math.max(preAcc, 1);
		//if (preAcc == -1);
			//preWeight = 0;
		
		double acc = location.getAccuracy();
		double lat = location.getLatitude();
		double lon = location.getLongitude();
		
		if (curIndex > lats.length - 1) {
			curIndex = 0;
		}
		lats[curIndex] = lat;
		lons[curIndex] = lon;
		accs[curIndex] = acc;
		curIndex++;
		
		// TODO
		//lat += 42.295670 - 50.976125;
		//lon += -71.112375 - 11.339915;

		double latsTotal = 0;
		double lonsTotal = 0;
		double weightsTotal = 0;
		for (int i = 0; i < lats.length; i++) {
			double weight = 1/Math.max(accs[i], 1);
			if (accs[i] == -1) {
				weight = 0;
			}
			weightsTotal += weight;
			latsTotal += lats[i]*weight;
			lonsTotal += lons[i]*weight;
			//double preWeight = 1/Math.max(preAcc, 1);
		}
		//lat = (lat*weight + preLat*preWeight)/(weight + preWeight);
		//lon = (lon*weight + preLon*preWeight)/(weight + preWeight);

		float[] results = new float[3];
		Location.distanceBetween(lat, lon, latsTotal/weightsTotal, lonsTotal/weightsTotal, results);
		//Log.v(TAG, "Smoothing Delta: " + results[0]);

		lat = latsTotal/weightsTotal;
		lon = lonsTotal/weightsTotal;
		acc += results[0];
		
		//preAcc = acc;
		//preLat = lat;
		//preLon = lon;
		//double acc = location.getAccuracy();
		//double lat = (location.getLatitude()*acc + preLat*preAcc)/(acc + preAcc);
		//double lon = location.getLongitude();
		//String line = lat + comm	+ lon + comm + stamp;
		//sendMessage(messageAcc, location.getAccuracy());
		//sendDouble(messageAcc, location.getSpeed());
		//sendMessage(messageLat, lat);
		//sendMessage(messageLon, lon);
		double[] loc = {lat, lon, acc};
		sendMessage(messageLoc, loc);
		//sendDouble(messageAlt, location.getAltitude());

		sendMessage(messageLock, "Yes");
		//lastLocation = line;
		//Log.v("Location", line);

		//double dist = node[3].distanceFromTo(lat, lon);
		//sendDouble(messageAlt, dist);

		int activeRegions = 0;
		int error = 0;
		if (isTracking) {
			for (int i = 0; i < nodeCount; i++) {
				if (!node[i].invalid) {
					if (node[i].isInside(lat, lon)) {
						activeRegions++;
						if (hasAudioFocus) {
							if (!node[i].isPlaying()) {
								node[i].play();
							}
							node[i].setVolume(lat, lon);
						}
					}
					else {
						if (node[i].isPlaying())
							//node[i].stop();
							node[i].setVolume(0);
					}
				}
				else
					error++;
			}
			if (activeRegions > 0 && !hasAudioFocus) {
				int result = audioManager.requestAudioFocus(onAudioFocusChange, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
				if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
					hasAudioFocus = true;
				}
				else if (result == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
					hasAudioFocus = false;
				}
				if (isIntroPlaying()) {
					playIntro(false);
				}
			}
			else {
				audioManager.abandonAudioFocus(onAudioFocusChange);
				hasAudioFocus = false;
			}
		}
		sendMessage(messageAct, activeRegions);
		sendMessage(messageAll, nodeCount - error);

		/*if (isTracking) {
			String path = lastLocation;
			String name = path.split("/")[path.split("/").length - 1];
			double lat = Double.parseDouble(name.split(",")[0]);
			double lon = Double.parseDouble(name.split(",")[1]);
			if ((lat < topLat - heightLat || lat > topLat || lon < leftLon || lon > leftLon + widthLon) && linesCt < 10) {
				Toast.makeText(getApplicationContext(), getResources().getString(R.string.outide_map), Toast.LENGTH_LONG).show();
				//Log.v(TAG, "Track Size: " + linesCt);
				File trackFile = new File(Environment.getExternalStorageDirectory().getPath() + "/Grimpant", lastSession + ".csv");
				GPSActivity.tb.setChecked(false);
				stopTracking();
				trackFile.delete();
			}
			try {
				out.write(line + "\n");
				linesCt++;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}*/
		
		//if (bound) {
			//updateMap(location.getLatitude(), location.getLongitude());
		//}
	}
	
	private class LockCheck extends TimerTask {
		public void run() {
			//sendString(messageLock, "Yes");
			if (System.currentTimeMillis() - lastLocationMillis > 30000) {
				if (gotLock) {
					noLock();
				}
			}
		}
	}
	public void stopTracking() {
		/*if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			return;
		}*/
		Log.v(TAG, "stopTracking");
		stopForeground(true);
		isTracking = false;

		stopAllNodes();
		
		//locationManager.removeUpdates(this);

		/*try {
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}*/
	}
	
	public boolean isTracking() {
		return isTracking;
	}

	public boolean isIntroPlaying() {
		if (mPlayer != null)
			return mPlayer.isPlaying();
		return false;
	}

	public boolean gotLock() {
		return gotLock;
	}

	public boolean GPSDisabled() {
		return GPSDisabled;
	}

	/*public Location curLocation() {
		return curLocation;
	}*/

	/*public String lastSession() {
		return lastSession;
	}*/
	
	/*public String lastLocation() {
		return lastLocation;
	}*/
	
	/*public void updateMap(double lat, double lon) {
		if (GPSActivity.gb.getVisibility() != View.VISIBLE) {// && !GPSActivity.listeningMode) {
			GPSActivity.mapOver.setVisibility(View.INVISIBLE);
			GPSActivity.searching.setVisibility(View.INVISIBLE);
			GPSActivity.center.setVisibility(View.VISIBLE);
		}
		
		//float leftLon = -79.05f; //buffalo
		//float widthLon = 0.4f;
		//float leftLon = -74.0f; //new york
		//float widthLon = 0.4f;
		//double leftLon = 3.759888d; //montpellier
		//double widthLon = 0.21992d;
		//float topLat = 43.1f; //buffalo
		//float heightLat = 0.3f;
		//float topLat = 40.7f; //new york
		//float heightLat = 0.3f;
		//double topLat = 43.659722d; // montpellier
		//double heightLat = 0.159391d;
		int widthDP = ((int)(409.6*scale))*20;
		int heightDP = ((int)(409.6*scale))*20;
		//double x = ViewHelper.getTranslationX(GPSActivity.mapAll) - ((int)409.6*scale)/2.0;
		//double y = ViewHelper.getTranslationY(GPSActivity.mapAll) - ((int)409.6*scale)/2.0;
		//if (location != null) {
		double x = (widthDP*(leftLon - lon)/widthLon) + 160*scale;
		double y = (heightDP*(lat - topLat)/heightLat) + 160*scale;
		//}
		//LayoutParams layoutParams = new LayoutParams((int) (widthDP*scale), (int) (heightDP*scale));
	    //layoutParams.setMargins((int) x, (int) y, 0, 0);
		if (Math.abs((int) x - ViewHelper.getTranslationX(GPSActivity.mapAll)) > 0 || Math.abs((int) y - ViewHelper.getTranslationY(GPSActivity.mapAll)) > 0) {
			ObjectAnimator animationX = ObjectAnimator.ofFloat(GPSActivity.mapAll, "translationX", ViewHelper.getTranslationX(GPSActivity.mapAll), (int) x);
			animationX.setDuration(750);
			animationX.start();
			ObjectAnimator animationY = ObjectAnimator.ofFloat(GPSActivity.mapAll, "translationY", ViewHelper.getTranslationY(GPSActivity.mapAll), (int) y);
			animationY.setDuration(750);
			animationY.start();

			ObjectAnimator animationX2 = ObjectAnimator.ofFloat(GPSActivity.mapPins, "translationX", ViewHelper.getTranslationX(GPSActivity.mapPins), (int) x);
			animationX2.setDuration(750);
			animationX2.start();
			ObjectAnimator animationY2 = ObjectAnimator.ofFloat(GPSActivity.mapPins, "translationY", ViewHelper.getTranslationY(GPSActivity.mapPins), (int) y);
			animationY2.setDuration(750);
			animationY2.start();
	    	//Log.v("Location", ViewHelper.getTranslationX(GPSActivity.mapPins) + ", " + ViewHelper.getTranslationY(GPSActivity.mapPins) + "; " + x + ", " + y);
	    	Log.v("Test", x + ", " + y);
    	}
		
		int i = (int)(-x/(int)(409.6*scale));
		int j = (int)(-y/(int)(409.6*scale));

		if (curMapI != i || curMapJ != j) {
			for (int k = 0; k < 20; k++) {
				for (int l = 0; l < 20; l++) {
					GPSActivity.map[k][l].setImageBitmap(null);
				}
			}
			for (int k = -1; k < 2; k++) {
				if(i + k >= 0 && i + k < 20) {
					for (int l = -1; l < 2; l++) {
						if(j + l >= 0 && j + l < 20) {
							int id = (i + k) + 1 + (j + l)*20;
							int resID = getResources().getIdentifier("map_" + id, "drawable", getPackageName());
							Bitmap bMap = BitmapFactory.decodeResource(getResources(), resID);
							GPSActivity.map[i + k][j + l].setImageBitmap(bMap);
							bMap = null;
							Log.v("Position", (i + k) + "," + (j + l) + "," + "map_" + id);
						}
					}
				}
			}
			curMapI = i;
			curMapJ = j;
		}		
	}*/
	
	private void noLock() {
		/*if (GPSActivity.gb.getVisibility() != View.VISIBLE) {
			//GPSActivity.mapOver.setVisibility(View.VISIBLE);
			//GPSActivity.center.setVisibility(View.INVISIBLE);
			handler.postDelayed(updateMapOC, 100);
		}*/
		//GPSActivity.searching.setVisibility(View.VISIBLE);
		sendMessage(messageLock, "No");
		//Log.v(TAG, "OnLockChanged NoLock");
		gotLock = false;
		if (GPSDisabled) {
			mBuilder.setContentText(getResources().getString(R.string.gps_disabled));
		}
		else {
			mBuilder.setContentText(getResources().getString(R.string.searching));
		}
		if (isTracking) {
			mNotificationManager.notify(1366, mBuilder.build());
		}
		stopAllNodes();
	}
/*
	private void preparePlayer() {
		int resID = getResources().getIdentifier("audio_00", "raw", getPackageName());
		//Uri uri = Uri.parse(path);
		//mPlayer = new MediaPlayer();
		mPlayer = MediaPlayer.create(this, resID);
		mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		//mPlayer.setDataSource(this, uri);
		mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer mp) {
            	mPlayer.setLooping(true);
            	//mPlayer.start();
            }
        });
		mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
            }
        });
		try {
			mPlayer.prepare();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
*/	
	@Override
	public void onDestroy() {
		Log.v(TAG, "OnDestroy");
		playIntro(false);
		//stopForeground(true);
		//locationManager.removeUpdates(this);
	}

	private void sendMessage(String name, String message) {
        Intent intent = new Intent(messageAction);
        intent.putExtra(name, message);
        sendBroadcast(intent);
        //Log.v("Send", name + ": " + message);
    }
	private void sendMessage(String name, double[] message) {
        Intent intent = new Intent(messageAction);
        intent.putExtra(name, message);
        sendBroadcast(intent);
        //Log.v("Send", name + ": " + message);
    }
	private void sendMessage(String name, int message) {
        Intent intent = new Intent(messageAction);
        intent.putExtra(name, message);
        sendBroadcast(intent);
        //Log.v("Send", name + ": " + message);
    }
/*
	private void startPlayers() {
		audioManager.requestAudioFocus(onAudioFocusChange, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
		node[3].play();
	}
*/
	public void stopAllNodes() {
		audioManager.abandonAudioFocus(onAudioFocusChange);
		hasAudioFocus = false;
		for (int i = 0; i < nodeCount; i++) {
			if (!node[i].invalid) {
				if (node[i].isPlaying()) {
					node[i].stop();
					//node[i].setVolume(0);
				}
			}
		}
	}

    public void playIntro(boolean play) {
		// TODO VolumePrompt
    	//AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    	//int currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
    	if (play) {
        	if (mPlayer == null) {
        		mPlayer = MediaPlayer.create(this, R.raw.intro);
        		mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        		mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
        	        public void onPrepared(MediaPlayer mp) {
        				int result = audioManager.requestAudioFocus(onAudioFocusChange, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        				if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
        					hasAudioFocus = true;
            	        	mPlayer.start();
            	        	//introPlaying = true;
            	    		sendMessage(messageIntro, "Yes");
        				}
        				else if (result == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
        					hasAudioFocus = false;
        				}
        	        }
        	    });
        		mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
        	        public void onCompletion(MediaPlayer mp) {
        				audioManager.abandonAudioFocus(onAudioFocusChange);
    					hasAudioFocus = false;
        	    		sendMessage(messageIntro, "No");
        	    		mPlayer.release();
        	    		mPlayer = null;
        	        }
        		});
        	}
    	}
    	else {
    		if (mPlayer != null) {
                //prefs.edit().putBoolean("firstRun", false).commit();
    			if (mPlayer.isPlaying()) {
    				mPlayer.stop();
    				audioManager.abandonAudioFocus(onAudioFocusChange);
					hasAudioFocus = false;
    	    		sendMessage(messageIntro, "No");
    			}
    			mPlayer.release();
    			mPlayer = null;
	        	//introPlaying = false;
    		}
    	}
    }
}