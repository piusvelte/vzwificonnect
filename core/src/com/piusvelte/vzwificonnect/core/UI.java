/* VzWiFiConnect - Android WiFi connection tool
 * Copyright (C) 2009 Bryan Emmanuel
 * 
 * This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *  Bryan Emmanuel piusvelte@gmail.com
 */
package com.piusvelte.vzwificonnect.core;

import java.util.List;

import com.google.ads.*;

import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.wifi.WifiConfiguration.GroupCipher;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class UI extends ListActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, DialogInterface.OnClickListener {
	private static final int CONNECT_ID = Menu.FIRST;
	private Button btn_generator;
	private EditText fld_ssid, fld_wep, fld_wep_alternate;
	private CheckBox btn_wifi;
	private WifiManager mWifiManager;
	private Context mContext;
	private String[] mSsid = new String[0], mBssid = new String[0];
	private boolean wifiEnabled;
	private static final String TAG = "vzwificonnect";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		registerForContextMenu(getListView());
		if (!getPackageName().toLowerCase().contains("pro")) {
			AdView adView = new AdView(this, AdSize.BANNER, "a14c72b7fa7aace");
			((LinearLayout) findViewById(R.id.ad)).addView(adView);
			adView.loadAd(new AdRequest());
		}
		mContext = this;
		mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		fld_ssid = (EditText) findViewById(R.id.fld_ssid);
		fld_wep = (EditText) findViewById(R.id.fld_wep);
		fld_wep_alternate = (EditText) findViewById(R.id.fld_wep_alternate);
		btn_generator = (Button) findViewById(R.id.btn_generator);
		btn_wifi = (CheckBox) findViewById(R.id.btn_wifi);
		btn_generator.setOnClickListener(this);
		btn_wifi.setOnCheckedChangeListener(this);
		wifiStateChanged(mWifiManager.getWifiState());
		btnWifiSetText(R.string.scanning);
		mWifiManager.startScan();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_ui, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.menu_ui_rescan) {
			btnWifiSetText(R.string.scanning);
			mWifiManager.startScan();
			Toast.makeText(mContext, "Scanning for valid networks", Toast.LENGTH_SHORT).show();
		} else if (itemId == R.id.menu_ui_wifi_settings) {
			startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
		} else if (itemId == R.id.menu_ui_about) {
			AlertDialog.Builder dialog = new AlertDialog.Builder(this);
			dialog.setMessage(R.string.usage);
			dialog.setNegativeButton(R.string.close, this);
			dialog.show();
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);
		menu.add(0, CONNECT_ID, 0, R.string.connect);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (item.getItemId() == CONNECT_ID) {
			int ap = ((AdapterContextMenuInfo) item.getMenuInfo()).position;
			if ((mSsid.length > ap) && (mBssid.length > ap)) {
				fld_ssid.setText(mSsid[ap]);
				int networkId = -1, priority = 0, max_priority = 99999;
				final List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();
				WifiConfiguration config;
				if (configs != null) {
					// get the highest priority
					for (int i = configs.size() - 1; i >= 0; i--) {
						if (configs.get(i).priority > priority) {
							priority = configs.get(i).priority;
						}
					}
					priority = priority < max_priority ? priority + 1 : max_priority;
					for (int i = configs.size() - 1; i >= 0; i--) {
						config = configs.get(i);
						// compare ssid & bssid
						if ((config.SSID != null) && mSsid[ap].equals(config.SSID) && ((config.BSSID == null) || mBssid[ap].equals(config.BSSID))) {
							networkId = config.networkId;
							config.allowedAuthAlgorithms.clear();
							config.allowedGroupCiphers.clear();
							config.allowedKeyManagement.clear();
							config.allowedPairwiseCiphers.clear();
							config.allowedProtocols.clear();
							config.wepKeys[0] = generator(mSsid[ap].toCharArray(), mBssid[ap]);
							if (config.priority < priority) {
								config.priority = priority;
							}
							config.hiddenSSID = false;
							config.wepTxKeyIndex = 0;
							config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
							config.allowedAuthAlgorithms.set(AuthAlgorithm.SHARED);
							config.allowedKeyManagement.set(KeyMgmt.NONE);
							config.allowedGroupCiphers.set(GroupCipher.WEP40);
							config.allowedGroupCiphers.set(GroupCipher.WEP104);
							mWifiManager.updateNetwork(config);
						}
					}
					if (networkId == -1) {
						config = new WifiConfiguration();
						config.allowedAuthAlgorithms.clear();
						config.allowedGroupCiphers.clear();
						config.allowedKeyManagement.clear();
						config.allowedPairwiseCiphers.clear();
						config.allowedProtocols.clear();
						config.SSID = '"' + mSsid[ap] + '"';
						config.wepKeys[0] = generator(mSsid[ap].toCharArray(), mBssid[ap]);
						config.priority = priority;
						config.hiddenSSID = false;
						config.wepTxKeyIndex = 0;
						config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
						config.allowedAuthAlgorithms.set(AuthAlgorithm.SHARED);
						config.allowedKeyManagement.set(KeyMgmt.NONE);
						config.allowedGroupCiphers.set(GroupCipher.WEP40);
						config.allowedGroupCiphers.set(GroupCipher.WEP104);
						networkId = mWifiManager.addNetwork(config);
					}
					// disable others to force connection to this network
					if (networkId != -1) {
						mWifiManager.enableNetwork(networkId, true);
					}
				} else {
					Toast.makeText(mContext, "Unable to get WiFi Configuration, please try again", Toast.LENGTH_SHORT).show();
				}
			}
			return true;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	protected void onListItemClick(ListView list, View view, int position, long id) {
		super.onListItemClick(list, view, position, id);
		fld_ssid.setText(mSsid[position]);
		fld_wep.setText(generator(mSsid[position].toCharArray(), mBssid[position]));
		fld_wep_alternate.setText("");
	}

	private String generator(char[] ssid, String bssid) {
		int dec = 0;
		for (int i = 0, i2 = ssid.length; i < i2; i++) {
			try {
				dec += Integer.parseInt(Character.toString(ssid[i])) * Math.pow(36, i);
			} catch (NumberFormatException nfe) {
				dec += (((int) ssid[i]) - 55) * Math.pow(36, i);				
			}
		}
		String wep = Integer.toHexString(dec).toUpperCase();
		// need to pad the wep out to 6 characters
		while (wep.length() < 6) {
			wep = "0" + wep;
		}
		if (bssid.length() > 4) {
			return (bssid.substring(3,5) + bssid.substring(6,8)).toUpperCase() + wep;
		} else {
			return bssid.toUpperCase() + wep;
		}
	}

	private void btnWifiSetText(int res) {
		btn_wifi.setText(getString(R.string.wifi) + " " + getString(res));		
	}

	private void wifiStateChanged(int state) {
		switch (state) {
		case WifiManager.WIFI_STATE_ENABLING:
			btnWifiSetText(R.string.enabling);
			return;
		case WifiManager.WIFI_STATE_ENABLED:
			btnWifiSetText(R.string.enabled);
			wifiEnabled = true;
			btn_wifi.setChecked(true);
			return;
		case WifiManager.WIFI_STATE_DISABLING:
			btnWifiSetText(R.string.disabling);
			wifiEnabled = false;
			btn_wifi.setChecked(false);
			return;
		case WifiManager.WIFI_STATE_DISABLED:
			btnWifiSetText(R.string.disabled);
			return;
		}
	}

	private void networkStateChanged(NetworkInfo.DetailedState state) {

		switch (state) {
		case AUTHENTICATING:
			btnWifiSetText(R.string.authenticating);
			return;
		case CONNECTED:
			btnWifiSetText(R.string.connected);
			return;
		case CONNECTING:
			btnWifiSetText(R.string.connecting);
			return;
		case DISCONNECTED:
			btnWifiSetText(R.string.disconnected);
			return;
		case DISCONNECTING:
			btnWifiSetText(R.string.disconnecting);
			return;
		case FAILED:
			btnWifiSetText(R.string.failed);
			return;
		case IDLE:
			return;
		case OBTAINING_IPADDR:
			btnWifiSetText(R.string.ipaddr);
			return;
		case SCANNING:
			btnWifiSetText(R.string.scanning);
			return;
		case SUSPENDED:
			return;
		}
	}

	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
				wifiStateChanged(mWifiManager.getWifiState());
				List<ScanResult> lsr = mWifiManager.getScanResults();
				if (lsr != null) {
					mSsid = new String[0];
					mBssid = new String[0];
					for (ScanResult sr : lsr) {
						String bssid = (sr.BSSID.substring(3,5) + sr.BSSID.substring(6,8)).toUpperCase();
						if ((sr.SSID.length() == 5) && isValidNetwork(sr.SSID) && (bssid.equals("1801") || bssid.equals("1F90"))) {
							String[] ssid_cp = new String[mSsid.length];
							String[] bssid_cp = new String[mSsid.length];
							for (int i = 0, i2 = mSsid.length; i < i2; i++) {
								ssid_cp[i] = mSsid[i];
								bssid_cp[i] = mBssid[i];
							}
							mSsid = new String[ssid_cp.length + 1];
							mBssid = new String[ssid_cp.length + 1];
							for (int i = 0, i2 = mSsid.length; i < i2; i++) {
								if (i == (mSsid.length - 1)) {
									mSsid[i] = sr.SSID;
									mBssid[i] = sr.BSSID;
								} else {
									mSsid[i] = ssid_cp[i];
									mBssid[i] = bssid_cp[i];
								}
							}
						}
					}
					setListAdapter(new ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_1, mSsid));
					if (mSsid.length > 0) {
						(new AlertDialog.Builder(mContext))
						.setMessage(R.string.ap_info)
						.setNegativeButton(android.R.string.ok, UI.this)
						.show();
					} else {
						(new AlertDialog.Builder(mContext))
						.setMessage(R.string.no_aps)
						.setNegativeButton(android.R.string.ok, UI.this)
						.show();
					}
				} else {
					Toast.makeText(mContext, R.string.no_aps, Toast.LENGTH_SHORT);
				}
			} else if(intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
				networkStateChanged(((NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO)).getDetailedState());
			} else if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
				wifiStateChanged(intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 4));
			}
		}
	};

	@Override
	public void onResume() {
		super.onResume();
		IntentFilter f = new IntentFilter();
		f.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		f.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		f.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		f.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
		f.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
		registerReceiver(mReceiver, f);
		setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mSsid));
	}

	@Override
	public void onPause() {
		super.onDestroy();
		if (mReceiver != null) {
			unregisterReceiver(mReceiver);
			mReceiver = null;
		}
	}

	public void onClick(DialogInterface dialog, int which) {
		dialog.cancel();
	}

	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if (isChecked != wifiEnabled) {
			mWifiManager.setWifiEnabled(isChecked);
		}
	}

	public void onClick(View v) {
		String ssid = fld_ssid.getText().toString().toUpperCase();
		if (ssid.length() != 5) {
			Toast.makeText(this, "The network name must be 5 characters to generate the key.", Toast.LENGTH_LONG).show();
		} else if (isValidNetwork(ssid)) {
			fld_ssid.setText(ssid);
			fld_wep.setText(generator(ssid.toCharArray(), "1801"));
			fld_wep_alternate.setText(generator(ssid.toCharArray(), "1F90"));
		} else {
			Toast.makeText(this, "The network name must only contain 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, A, B, C, D, E, or F to generate the key.", Toast.LENGTH_LONG).show();
		}
	}

	private boolean isValidNetwork(String network) {
		return network.matches("[0-9A-Za-z]+");
	}

}