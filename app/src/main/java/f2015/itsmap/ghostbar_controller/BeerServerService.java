package f2015.itsmap.ghostbar_controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class BeerServerService extends Service {
	public static final String RESULT_BEER_SERVICE_NEW_ORDER = "RESULT_BEER_SERVICE_NEW_ORDER";
	public static final String RESULT_BEER_SERVICE_SET_ORDER_SUCCESS = "RESULT_BEER_SERVICE_SET_ORDER_SUCCESS";
	public static final String ERROR_CALL_SERVICE = "Error_Call_Service";
	private final IBinder binder = new ItogBinder();
	private Thread servicecallthread = null;
	private boolean CallError = false;

	private int BeerSetOrderResult;
	private String BeerSetOrderDate;
	public int GetBeerSetOrderResult(){
		return BeerSetOrderResult;
	}
	public String GetBeerSetOrderDate(){
		return BeerSetOrderDate;
	}

	private int BeerOrderId;
	private double BeerTransactionId;
	private double BeerPrice;
	private double BeerAmount;
	private String BeerOrderCreated;

	public int GetBeerOrderId(){
		return BeerOrderId;
	}
	public double GetBeerTransactionId(){
		return BeerTransactionId;
	}
	public double GetBeerPrice(){
		return BeerPrice;
	}
	public double GetBeerAmount(){
		return BeerAmount;
	}
	public String GetBeerOrderCreated(){
		return BeerOrderCreated;
	}

	/**
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	public class ItogBinder extends Binder {
		BeerServerService getService() {
			return BeerServerService.this;
		}
	}

	/**
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate() {
		// TODO Put your code here
	}

	/**
	 * @see android.app.Service#onStart(android.content.Intent,int)
	 */
	@Override
	public void onStart(Intent intent, int startId) {
		// TODO Put your code here
	}

	public void setOrderComplete(final int orderId) {
		CallError = false;
		servicecallthread = new Thread() {
			public void run() {
				setOrderDone(orderId);
			}
		};
		this.servicecallthread.start();
	}
	private void setOrderDone(int orderId) {

		// For using Internet methods, AndroidManifest.xml must have the
		// following permission:
		// <uses-permission android:name="android.permission.INTERNET"/>
		URI myURI = null;
		try {
			myURI = new URI("http://2party.dk/itsmap/beerbot_upd.php");
		} catch (URISyntaxException e) {
			// Deal with it
			broadCastError(e);
			return;
		}
		HttpClient httpClient = new DefaultHttpClient();
		HttpPost postMethod = new HttpPost(myURI);

		String request = "{\"id\":"+orderId+"}";
		StringEntity se = null;
		try {
			se = new StringEntity(request);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		postMethod.setEntity(se);


		HttpResponse webServerResponse = null;
		try {
			webServerResponse = httpClient.execute(postMethod);
		} catch (ClientProtocolException e) {
			broadCastError(e);
			return;
		} catch (IOException e) {
			broadCastError(e);
			return;
		}

		HttpEntity httpEntity = webServerResponse.getEntity();

		if (httpEntity != null) {

			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(
						httpEntity.getContent()));

				StringBuilder SB = new StringBuilder();
				String line;
				//Remove newlines
				while ((line = in.readLine()) != null) {
					SB.append(line);
				}

				JSONObject jo = new JSONObject(SB.toString());

				if(jo.length() >0 ) {

					BeerSetOrderResult = jo.getInt("result");
					BeerSetOrderDate = jo.getString("date");

					Intent retint = new Intent(RESULT_BEER_SERVICE_SET_ORDER_SUCCESS);
					sendBroadcast(retint);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.w("myApp", e.getMessage());
				broadCastError(e);
				return;
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				Log.w("myApp", e.getMessage());
				broadCastError(e);
				return;
			}

		}
	}

	public void checkForNewOrder() {
		CallError = false;
		//A simple Thread requires special attention if UI Thread is called
		// In this a Broadcast will call back to the Activity and from here get access
		// to UI Thread when calling Activty BroadCastReceiver
		// ie the Activity.BroadcastReceiver.onReceive method is run on the UI Thread!!
		servicecallthread = new Thread() {
			public void run() {
				getNewOrder();
			}
		};
		this.servicecallthread.start();
	}

	private void getNewOrder() {
		// For using Internet methods, AndroidManifest.xml must have the
		// following permission:
		// <uses-permission android:name="android.permission.INTERNET"/>
		URI myURI = null;
		try {
			myURI = new URI("http://2party.dk/itsmap/beerbot_get.php");
		} catch (URISyntaxException e) {
			// Deal with it
			broadCastError(e);
			return;
		}
		HttpClient httpClient = new DefaultHttpClient();
		HttpGet getMethod = new HttpGet(myURI);
		HttpResponse webServerResponse = null;
		try {
			webServerResponse = httpClient.execute(getMethod);
		} catch (ClientProtocolException e) {
			broadCastError(e);
			return;
		} catch (IOException e) {
			broadCastError(e);
			return;
		}

        BeerOrderId = 0;

		HttpEntity httpEntity = webServerResponse.getEntity();

		if (httpEntity != null) {
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(
						httpEntity.getContent()));

				StringBuilder SB = new StringBuilder();
				String line;
                //Remove newlines
				while ((line = in.readLine()) != null) {
					SB.append(line);
				}

				Intent retint = new Intent(RESULT_BEER_SERVICE_NEW_ORDER);

                if (SB.toString().equals("0")) {
                    sendBroadcast(retint);
                    return;
                }

				JSONArray ja = new JSONArray(SB.toString());

				if(ja.length() >0 ) {
					JSONObject jo = (JSONObject) ja.get(0);

					BeerOrderId = jo.getInt("id");
					BeerTransactionId = jo.getDouble("transactionId");
					BeerPrice = jo.getDouble("price");
					BeerAmount = jo.getDouble("amount");
					BeerOrderCreated = jo.getString("created");

					sendBroadcast(retint);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				broadCastError(e);
				return;
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				//broadCastError(e);
                BeerOrderId = 0;
				return;
			} catch (ClassCastException e){
                broadCastError(e);
            }

		}
	}

	private void broadCastError(Exception e) {
		CallError = true;
		e.printStackTrace();
		Intent ie = new Intent(ERROR_CALL_SERVICE);
		sendBroadcast(ie);
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	@Override
	public void onRebind(Intent intent) {
		// TODO Auto-generated method stub
		super.onRebind(intent);
	}

	@Override
	public boolean onUnbind(Intent intent) {
		// TODO Auto-generated method stub
		return super.onUnbind(intent);
	}

}
