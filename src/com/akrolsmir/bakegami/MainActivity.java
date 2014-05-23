package com.akrolsmir.bakegami;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.akrolsmir.bakegami.WallpaperControlWidgetProvider.FavoriteWallpaperService;
import com.akrolsmir.bakegami.WallpaperControlWidgetProvider.NextWallpaperService;
import com.squareup.picasso.Picasso;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		onNextBG();
		
		findViewById(R.id.favButton).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, FavoriteWallpaperService.class);
				PendingIntent pendingIntent = PendingIntent.getService(MainActivity.this, 0, intent, 0);
				try {
					pendingIntent.send();
				} catch (CanceledException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		
		final EditText subredditText = new EditText(this);
		subredditText.setText("awwnime");
		final EditText refreshTimeText = new EditText(this);
		refreshTimeText.setText("200");

		ImageButton nextButton = (ImageButton) findViewById(R.id.nextButton);
		nextButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(MainActivity.this, NextWallpaperService.class);
				PendingIntent pendingIntent = PendingIntent.getService(MainActivity.this, 0, intent, 0);
				try {
					pendingIntent.send();
				} catch (CanceledException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

//				MainActivity.this.startService(new Intent(MainActivity.this,
//						WallpaperControlWidgetProvider.RefreshService.class).setAction("start"));
//
//				getSharedPreferences("com.akrolsmir.bakegami", 0).edit().putString("subreddit",
//						subredditText.getText().toString()).commit();
//
//				getSharedPreferences("com.akrolsmir.bakegami", 0).edit().putLong("refreshTime",
//						Long.parseLong(refreshTimeText.getText().toString())).commit();
//
//				Toast.makeText(MainActivity.this, "Cycling through r/" + subredditText.getText()
//						+ " every " + refreshTimeText.getText() + " sec", Toast.LENGTH_LONG).show();
			}
		});

		ImageButton clearButton = (ImageButton) findViewById(R.id.pausePlayButton);
		clearButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				//stops refreshing
				Intent intent = new Intent(MainActivity.this,
						WallpaperControlWidgetProvider.RefreshService.class);
				MainActivity.this.startService(intent.setAction("stop"));//TODO unhardcode

				//clears history
				getSharedPreferences("com.akrolsmir.bakegami", 0).edit().clear().commit();
				
				// loads from current stuff
				getSharedPreferences("com.akrolsmir.bakegami", 0).edit().putString("subreddit",
						subredditText.getText().toString()).commit();

				getSharedPreferences("com.akrolsmir.bakegami", 0).edit().putLong("refreshTime",
						Long.parseLong(refreshTimeText.getText().toString())).commit();
				
				// Loads the first 5:
				new WallpaperManager(MainActivity.this);
				
				Toast.makeText(MainActivity.this, "Stopped cycling", Toast.LENGTH_SHORT).show();
			}
		});
		
//		clearButton.setOnLongClickListener(new OnLongClickListener() {
//			@Override
//			public boolean onLongClick(View arg0) {
//				// clears cache
//				for(File file : getExternalCacheDir().listFiles())
//					file.delete();
//				
//				Toast.makeText(MainActivity.this, "Cleared cache", Toast.LENGTH_SHORT).show();
//				return false;
//			}
//		});
		
		
		findViewById(R.id.currentBG).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_VIEW);
				intent.setDataAndType(
						Uri.fromFile(new WallpaperManager(MainActivity.this).getCurrentWallpaper().getCacheFile()),
						"image/*");
				startActivity(intent);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	/* Listen for changes made by services/the widget */
	
	public final static String NEXT = "com.akrolsmir.bakegami.NEXT";
	public final static String FAVORITE = "com.akrolsmir.bakegami.FAVORITE";
	
	@Override
	protected void onResume() {
		LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
		manager.registerReceiver(updateReceiver, new IntentFilter(NEXT));
		manager.registerReceiver(updateReceiver, new IntentFilter(FAVORITE));
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
		manager.unregisterReceiver(updateReceiver);
		super.onPause();
	}
	
	private BroadcastReceiver updateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(NEXT)) {
				onNextBG();
			} else if (intent.getAction().equals(FAVORITE)) {
				onFavorite();
			}
		}
	};
	
	private void onNextBG() {
		try { //TODO more robust/better handling
			WallpaperManager wp = new WallpaperManager(this);
			ImageView currentBG = (ImageView) findViewById(R.id.currentBG);
			Picasso.with(this).load(wp.getCurrentWallpaper().getCacheFile())
					.fit().centerInside().into(currentBG);
		} catch (Exception e) {
			e.printStackTrace();
		}
		onFavorite();
	}
	
	private void onFavorite() {
		try { //TODO more robust/better handling
			FavoritesView fv = (FavoritesView) findViewById(R.id.favorites);
			fv.onFavorite();
			
			WallpaperManager wp = new WallpaperManager(this);
			
			ImageButton favButton = (ImageButton) findViewById(R.id.favButton);
			if(wp.getCurrentWallpaper().imageInFavorites()) {
				favButton.setImageResource(android.R.drawable.star_big_on);
			} else {
				favButton.setImageResource(android.R.drawable.star_big_off);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
//	public void updateConnectedFlags() {
//        ConnectivityManager connMgr = (ConnectivityManager) 
//                getSystemService(Context.CONNECTIVITY_SERVICE);
//        
//        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
//        if (activeInfo != null && activeInfo.isConnected()) {
//            wifiConnected = activeInfo.getType() == ConnectivityManager.TYPE_WIFI;
//            mobileConnected = activeInfo.getType() == ConnectivityManager.TYPE_MOBILE;
//        } else {
//            wifiConnected = false;
//            mobileConnected = false;
//        }  
//    }

}
