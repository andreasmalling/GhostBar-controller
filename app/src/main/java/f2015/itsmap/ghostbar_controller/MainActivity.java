package f2015.itsmap.ghostbar_controller;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends Activity {

    private static final String TAG = "Controller MainActivity";
    private Beacon beacon;
    private BeaconTransmitter beaconTransmitter;

    private int MAX_WEIGHT = 999;
    private int MIN_WEIGHT = 100;


    private boolean transmitting = false;
    private boolean askForLock;

    private boolean mBound = false;
    private BeerServerService beerServerServiceRef;
    private BeerBroadcastReceiver beerBroadcastReceiver;

    private Thread server;

    /** Defines callbacks for service binding, passed to bindService() */
    /**  */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get
            // LocalService instance
            beerServerServiceRef = ((BeerServerService.ItogBinder) service)
                    .getService();
            mBound = true;
            //Make first update
            beerServerServiceRef.checkForNewOrder();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            beerServerServiceRef = null;
            mBound = false;
        }

    };
    private Button confirmButton;
    private int currentID;
    private boolean setOrder;
    private String currentBeerID;
    private boolean servingBeer = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Full screen
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        // Beacon
        int beaconSupport = BeaconTransmitter.checkTransmissionSupported(getApplicationContext());

        if(beaconSupport == BeaconTransmitter.SUPPORTED) {
            beacon = new Beacon.Builder()
                    .setId1(getResources().getString(R.string.beacon_id1))
                    .setId2(getResources().getString(R.string.beacon_id2))
                    .setId3(getResources().getString(R.string.beacon_id3))
                    .setManufacturer(0x0118) // Radius Networks.  Change this for other beacon layouts
                    .setTxPower(-59)
                    .setDataFields(Arrays.asList(new Long[]{0l})) // Remove this for beacon layouts without d: fields
                    .build();

            BeaconParser beaconParser = new BeaconParser()
                    .setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25");

            beaconTransmitter = new BeaconTransmitter(getApplicationContext(), beaconParser);
        }

        confirmButton = (Button) findViewById(R.id.button_confirm);
        confirmButton.setEnabled(false);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                servingBeer = true;
                confirmButton.setEnabled(false);
                int amount = (int) beerServerServiceRef.GetBeerAmount();
                Log.d(TAG, "Amount: " + amount + " mg.");
                pourBeer(amount*10);
            }
        });

    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");

        super.onResume();
        // Ensure re-activation when leaving app
        //startLockTask(); TODO

        setBeaconTransmission(true);

        IntentFilter filter;
        filter = new IntentFilter(BeerServerService.RESULT_BEER_SERVICE_NEW_ORDER);
        filter.addAction(BeerServerService.RESULT_BEER_SERVICE_SET_ORDER_SUCCESS);
        filter.addAction(BeerServerService.ERROR_CALL_SERVICE);
        beerBroadcastReceiver = new BeerBroadcastReceiver();
        registerReceiver(beerBroadcastReceiver, filter);

        if(server == null) {
            /*server = new ServerCommunication();
            server.execute();*/
            serverCommunication();
        }
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();

        setBeaconTransmission(false);

        unregisterReceiver(beerBroadcastReceiver);
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();

        // Bind to LocalService
        Intent intent = new Intent(this, BeerServerService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();

        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    private boolean setBeaconTransmission(boolean broadcast) {
        if(!broadcast) {
            beaconTransmitter.stopAdvertising();
            String message = "Beacon transmission stopped";
            Log.d(TAG, message);
            return broadcast;
        } else {

            // check if bluetooth is enabled, and turn on if not
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter.isEnabled()){
                beaconTransmitter.startAdvertising(beacon);

                String message = "Beacon transmission started";
                Log.d(TAG, message);
                return broadcast;
            } else {
                String message = "Please turn on bluetooth";
                Log.d(TAG, message);
                return false;
            }
        }
    }

    public int pourBeer(int mg) {
        new serialCommunication().execute(mg);
        return mg;  //FIXME: Hardcoded return, due to noise on response from Arduino.
    }

    public void setBeerID(String id) {
        TextView beerID = (TextView) findViewById(R.id.beer_id);

        if(id.equals("0") ){
            confirmButton.setEnabled(false);
            beerID.setText(getResources().getText(R.string.beer_id));
        } else if (id.equals(currentBeerID) && servingBeer ) {
            confirmButton.setEnabled(false);
        } else {
            currentID = Integer.parseInt(id);
            confirmButton.setEnabled(true);
            beerID.setText(id);
        }
        currentBeerID = id;
    }

    private class serialCommunication extends AsyncTask<Integer, Void, Integer> {
        @Override
        protected Integer doInBackground(Integer... params) {
            int responseCode = 0;
            int mg = params[0];

            Log.d(TAG, "Tag 1");
            // Check for valid weight request
            if(mg < MIN_WEIGHT || mg > MAX_WEIGHT) {
                setOrder = true;
                return responseCode;
            }

            Log.d(TAG, "Tag 2");
            // Find all available drivers from attached devices.
            UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
            List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
            if (availableDrivers.isEmpty())
                return responseCode;

            Log.d(TAG, "Tag 3");
            // Open a connection to the first available driver.
            UsbSerialDriver driver = availableDrivers.get(0);
            UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
            if (connection == null)
                return responseCode;

            Log.d(TAG, "Tag 4");

            // Open connection
            UsbSerialPort port = driver.getPorts().get(0);
            try {
                port.open(connection);
                port.setParameters(57600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

                // Send request
                String request = "D"+mg+"\r\n";

                // Serial write
                port.write(request.getBytes(),6);
                byte buffer[] = new byte[32];

                int numBytesRead = 0;
                String message = "";

                // Serial read
                while( true ) {
                    numBytesRead += port.read(buffer, 1000);

                    for(int i = 0; i< 30;i++){
                        if(buffer[i] == 0) break;
                        message += String.format("%c",buffer[i]);
                    }

                    if(message.contains("?"))
                        break;

                    // Clear buffer
                    Arrays.fill(buffer, (byte) 0);
                }

                Log.d(TAG, "Read " + numBytesRead + " bytes.");
                Log.d(TAG, "Message:" + message);

                // Extract response
                if(message.contains("No flow")) {
                    servingBeer = false;
                    return responseCode = -1;
                }

                int responseBegin = message.indexOf("d");
                String response = message.substring(responseBegin,responseBegin+4);
                Log.d(TAG, "Response:" + response);

                responseCode = Integer.parseInt(response.substring(1));
                Log.d(TAG, "Responsecode:" + responseCode);

                setOrder = true;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    port.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return responseCode;
        }
    }

    //- Called when new order details are ready
    private void newBeerOrderReady() {
        if (this.beerServerServiceRef != null) {
            setBeerID(String.valueOf(beerServerServiceRef.GetBeerOrderId()));

            String result = "BeerPrice: "+String.valueOf(beerServerServiceRef.GetBeerPrice())
                    + "\r\nBeerAmount: " + String.valueOf(beerServerServiceRef.GetBeerAmount()) +
                    "\nTransactionId: " + String.valueOf(beerServerServiceRef.GetBeerTransactionId()) +
                    "\nOrderId: " + String.valueOf(beerServerServiceRef.GetBeerOrderId())+
                    "\nOrdered time: " + String.valueOf(beerServerServiceRef.GetBeerOrderCreated());

            Log.d(TAG, "newBeerOrderReady: " + result);

            Toast toast = Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT);
            //toast.show();
        }
    }

    private void beerOrderUpdatedResult(){

        if (this.beerServerServiceRef != null) {

            String result = "Result: "+String.valueOf(beerServerServiceRef.GetBeerSetOrderResult())
                    + "\r\nTime: " + String.valueOf(beerServerServiceRef.GetBeerSetOrderDate());

            Log.d(TAG, "beerOrderUpdatedResult: " + result);
            Toast toast = Toast.makeText(getApplicationContext(),result, Toast.LENGTH_SHORT);
            //toast.show();

            servingBeer = false;
        }
    }

    private class BeerBroadcastReceiver extends BroadcastReceiver {
        /* the onReceive method will be run in the UI thread
        * it is put into the event queue like a user event
        * */
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().compareTo(
                    BeerServerService.RESULT_BEER_SERVICE_NEW_ORDER) == 0) {

                //- Ready for new pour
                newBeerOrderReady();

            }
            else if (intent.getAction().compareTo(
                    BeerServerService.RESULT_BEER_SERVICE_SET_ORDER_SUCCESS) == 0) {

                //- Result of updating pour
                beerOrderUpdatedResult();
            }

            else {
                Log.d(TAG, "Host unavailable");
                CharSequence text = "Host ikke tilgængelig";
                int duration = Toast.LENGTH_LONG;
                Toast.makeText(context, text, duration).show();
            }
        }
    };

    public void serverCommunication(){
        server = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){

                    if (mBound) {// Service may still not be bound
                        if(setOrder) {
                            beerServerServiceRef.setOrderComplete(currentID);
                            setOrder = false;
                        }
                        beerServerServiceRef.checkForNewOrder();
                    }
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        server.start();
    }
}
