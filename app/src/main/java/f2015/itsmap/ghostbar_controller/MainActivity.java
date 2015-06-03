package f2015.itsmap.ghostbar_controller;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
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

    private static final String TAG = "MainActivity";
    private Beacon beacon;
    private BeaconTransmitter beaconTransmitter;

    private int MAX_WEIGHT = 999;
    private int MIN_WEIGHT = 100;


    private boolean transmitting = false;

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
                        | View.SYSTEM_UI_FLAG_IMMERSIVE
        );

        // Pin screen
        // TODO: startLockTask();

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

        Switch toggle = (Switch) findViewById(R.id.switch_beacon);
        toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((Switch) v).setChecked(toggleTransmit());
            }
        });

        Button readUSB = (Button) findViewById(R.id.read_usb);
        readUSB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    pourBeer(100);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private boolean toggleTransmit() {
        if(transmitting) {
            beaconTransmitter.stopAdvertising();
            transmitting = false;
            Toast.makeText(getApplicationContext(), "Beacon transmission stopped", Toast.LENGTH_SHORT).show();
            return transmitting;
        } else {

            // check if bluetooth is enabled, and turn on if not
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter.isEnabled()){
                beaconTransmitter.startAdvertising(beacon);
                transmitting = true;
                Toast.makeText(getApplicationContext(), "Beacon transmission started", Toast.LENGTH_SHORT).show();
            } else
                Toast.makeText(getApplicationContext(), "Please turn on bluetooth", Toast.LENGTH_LONG).show();
            return transmitting;
        }
    }

    private int pourBeer(int mg) throws IOException {

        int responseCode = 0;

        // Check for valid weight request
        if(mg < MIN_WEIGHT || mg > MAX_WEIGHT)
            return responseCode;

        // Find all available drivers from attached devices.
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty())
            return responseCode;

        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
        if (connection == null)
            return responseCode;

        // Open connection
        UsbSerialPort port = driver.getPorts().get(0);
        port.open(connection);
        port.setParameters(57600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

        // Send request
        try {
            String request = "D"+mg+"\r\n";

            port.write(request.getBytes(),6);
            byte buffer[] = new byte[32];

            int numBytesRead = 0;
            String message = "";
            while( numBytesRead < 15 ) {
                numBytesRead += port.read(buffer, 1000);
                message += new String(buffer);

                // Clear buffer
               Arrays.fill( buffer, (byte) 0 );
            }
            Log.d(TAG, "Read " + numBytesRead + " bytes.");
            Log.d(TAG, "Message:" + message);

/* FIXME: Doesn't work, due to noise on response
            // Extract response
            if(message.contains("No flow"))
                return responseCode = -1;

            int responseBegin = message.indexOf("d");
            String response = message.substring(responseBegin,responseBegin+4);
            Log.d(TAG, "Response:" + response);

            responseCode = Integer.parseInt(response.substring(1));
            Log.d(TAG, "Responsecode:" + responseCode);
*/
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            port.close();
        }
        return responseCode;
    }
}
