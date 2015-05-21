package f2015.itsmap.ghostbar_controller;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;

import java.util.Arrays;


public class MainActivity extends Activity {

    private Beacon beacon;
    private BeaconTransmitter beaconTransmitter;

    private boolean transmitting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            toggleTransmit();
            return true;
        }

        return super.onOptionsItemSelected(item);
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
}
