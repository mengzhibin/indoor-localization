package com.Localization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import org.json.simple.JSONValue;

// See http://www.androidsnippets.com/scan-for-wireless-networks

public class StartLocating extends Activity {

	WifiManager mainWifi;

	Handler dataPushHandler;
	boolean dataPushHandlerActive;
	Button btnStartPushing;
	Button btnStopPushing;

	Runnable statusChecker = new Runnable() {
		@Override 
		public void run() {
			List locationData = getAvailableData();
			String jsonStringified = JSONValue.toJSONString(locationData);

			Log.d(C.TAG, "JSON encoded data: " + jsonStringified.substring(0, 100) + (jsonStringified.length() > 100 ? "..." : ""));
			Networking.postData(C.SERVER + "push", jsonStringified);
			if (dataPushHandlerActive)
				dataPushHandler.postDelayed(this, C.pushIntervalMillis);
		}
	};
	
	private void startPushing() {
		dataPushHandlerActive = true;
		statusChecker.run();
		btnStopPushing.setEnabled(true);
		btnStartPushing.setEnabled(false);
	}
	
	private void stopPushing() {
		dataPushHandlerActive = false;
		dataPushHandler.removeCallbacks(statusChecker);
		btnStopPushing.setEnabled(false);
		btnStartPushing.setEnabled(true);
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_start_locating);
		mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		dataPushHandler = new Handler();

		dataPushHandlerActive = false;
		ErrorReporting.initialize(this);

		btnStartPushing = (Button)findViewById(R.id.start_push);
		btnStopPushing = (Button)findViewById(R.id.stop_pushing);
		btnStopPushing.setEnabled(false);
		btnStartPushing.setEnabled(true);
		
		btnStartPushing.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startPushing();
			}
		});
		
		btnStopPushing.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				stopPushing();
			}
		});

	}
	// Adds data to target if data is not null and denotes it as name
	private void addData(List target, String name, Object data) {
		if (data != null) {
			Map dataContainer = new HashMap();
			dataContainer.put("name", name);
			dataContainer.put("data", data);
			target.add(dataContainer);
		}
	}

	protected List getAvailableData() {
		Long start = System.currentTimeMillis();

		List result = new LinkedList();

		// Wifi
		addData(result, "wifi", getWifi());

		Log.d(C.TAG, "Data extraction complete in " + (System.currentTimeMillis() - start) + " ms");
		return result;
	}

	protected List getWifi() {
		mainWifi.startScan();
		List jsonScanResults = new LinkedList();

		for(ScanResult scanResult : mainWifi.getScanResults()) {
			HashMap jsonScanResult = new HashMap();
			jsonScanResult.put("bssid", scanResult.BSSID);
			jsonScanResult.put("level", scanResult.level);
			jsonScanResult.put("frequency", scanResult.frequency);
			jsonScanResults.add(jsonScanResult);
		}

		return jsonScanResults;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.start_locating, menu);
		return true;
	}

}