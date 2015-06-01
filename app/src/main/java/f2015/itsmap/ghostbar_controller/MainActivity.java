package f2015.itsmap.ghostbar_controller;

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
        startLockTask();

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
                toggleTransmit();
            }
        });

    }

    private void toggleTransmit() {
        if(transmitting) {
            beaconTransmitter.stopAdvertising();
            transmitting = false;
            Toast.makeText(getApplicationContext(), "Beacon transmission stopped", Toast.LENGTH_SHORT).show();
        } else {
            // TODO: check if bluetooth is enabled
            beaconTransmitter.startAdvertising(beacon);
            transmitting = true;
            Toast.makeText(getApplicationContext(), "Beacon transmission started", Toast.LENGTH_SHORT).show();
        }
    }

    private void findUSB () throws IOException {
        // Find all available drivers from attached devices.
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            return;
        }

        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            // You probably need to call UsbManager.requestPermission(driver.getDevice(), ..)
            return;
        }

        /*// Read some data! Most have just one port (port 0).
        UsbSerialPort port = driver.getPort(0);
        port.open(connection);
        try {
            port.setBaudRate(115200);
            byte buffer[] = new byte[16];
            int numBytesRead = port.read(buffer, 1000);
            Log.d(TAG, "Read " + numBytesRead + " bytes.");
        } catch (IOException e) {
            // Deal with error.
        } finally {
            port.close();
        }*/
    }
}
