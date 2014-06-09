package spider.nitt.motiongaming;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
// import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This is the main Activity that displays the current chat session.
 */
public class motiongaming extends Activity implements SensorEventListener {
    // Debugging
	float SensorX = 0;
	float SensorY = 0;
    private static final String TAG = "motiongaming";
    private static final boolean D = true;
    SensorManager sm;
    TextView disp,disp2;
    private boolean paused = false;
    private boolean race = false;
    private boolean nitrous_on = false;
    private boolean on_click = false;
    Button power,resume,nitrous,gearup,geardown;
    // Message types sent from the motiongamingservice Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the motiongamingservice Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
   // private TextView disp;

    // Layout Views
 //   private TextView mTitle;
   // private ListView mConversationView;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
 //   private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private motiongamingservice mChatService = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");
        paused = false;
        race = false;
        
        // Set up the window layout
       // requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.main);
       // getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

        // Set up the custom title
       // mTitle = (TextView) findViewById(R.id.title_left_text);
       // mTitle.setText(R.string.app_name);
       // mTitle = (TextView) findViewById(R.id.title_right_text);
        disp = (TextView) findViewById(R.id.dis);
        disp2 = (TextView) findViewById(R.id.dis2);
        Typeface tf = Typeface.createFromAsset(getAssets(),"font3.TTF"); 
        disp.setTypeface(tf);
        disp2.setTypeface(tf);
        power = (Button) findViewById(R.id.p);
        resume = (Button) findViewById(R.id.res);
        nitrous = (Button) findViewById(R.id.n);
        gearup = (Button) findViewById(R.id.gu);
        geardown = (Button) findViewById(R.id.gd);
        
        
        disp.setText("Go to MENU >>> CONNECT \n (make sure that your device has already been paired to PC)");
        
        
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        
        
        
    }

    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
            if (mChatService == null) setupChat();
        }
        
        
        
    }

    @Override
    public synchronized void onResume() 
    {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == motiongamingservice.STATE_NONE) {
              // Start the Bluetooth chat services
              mChatService.start();
            }
        }
    }

    private void setupChat() {
        Log.d(TAG, "setupChat()");


        // Initialize the motiongamingservice to perform bluetooth connections
        mChatService = new motiongamingservice(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    @Override
    public synchronized void onPause() {
    	super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
    	try{
    		sm.unregisterListener(this);
    	}catch(Exception e){
    		// do nothing
    	}
    	super.onStop();
    	try{
    		 mBluetoothAdapter.disable();
    	}catch(Exception e4){
    		//do nothing
    	}
    	if (mChatService != null) mChatService.stop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
    	try{
    		sm.unregisterListener(this);
    	}catch(Exception e){
    		// do nothing
    	}
    	try{
   		 mBluetoothAdapter.disable();
   	}catch(Exception e4){
   		//do nothing
   	}
    	if (mChatService != null) mChatService.stop();
        finish();
    	super.onDestroy();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }

    private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != motiongamingservice.STATE_CONNECTED) {
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the motiongamingservice to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
        }
    }

    // The Handler that gets information back from the motiongamingservice
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case motiongamingservice.STATE_CONNECTED:
        //            mTitle.setText(R.string.title_connected_to);
          //          mTitle.append(mConnectedDeviceName);
    //                mConversationArrayAdapter.clear();
                    break;
                case motiongamingservice.STATE_CONNECTING:
            //        mTitle.setText(R.string.title_connecting);
                    break;
                case motiongamingservice.STATE_LISTEN:
                case motiongamingservice.STATE_NONE:
              //      mTitle.setText(R.string.title_not_connected);
                    break;
                }
                break;
            case MESSAGE_WRITE:
                break;
            case MESSAGE_READ:
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                mChatService.connect(device);
                if (mChatService.getState() != motiongamingservice.STATE_CONNECTED) {
                sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
                if(sm.getSensorList(Sensor.TYPE_ACCELEROMETER).size() != 0){
                	Sensor s = sm.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
                	sm.registerListener(this, s, SensorManager.SENSOR_DELAY_UI);
                }
                disp.setText("CONNECTION ESTABLISHED. \n PRESS POWER BUTTON DURING START OF RACE ONLY!");
                }
                
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupChat();
                
                
            } else {
                // User did not enable Bluetooth or an error occured
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.scan:
            // Launch the DeviceListActivity to see devices and do scan
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            return true;
        case R.id.discoverable:
            // Ensure this device is discoverable by others
            ensureDiscoverable();
            return true;
        }
        return false;
    }

	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	public void onSensorChanged(SensorEvent e) {
		// TODO Auto-generated method stub
		if(race == true && paused == false && on_click == false){
			try{
		/*	try {
				Thread.sleep(16);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} */
			SensorX = e.values[0];
			SensorY = e.values[1];
			if(SensorX < 6.0){
				sendMessage("F");
				disp2.setText("Accelerating...");
			}
	if(SensorX > 8.0){
		sendMessage("B");
	disp2.setText("Back...");
		}
	if(SensorY < -2.0){
		sendMessage("L");
		disp2.setText("\nLeft");
		}
	if(SensorY > 2.0){
		sendMessage("R");
		disp2.setText("\nRight");
		}
		}catch(Exception e3){
			 Toast.makeText(this, "CRASHED!", Toast.LENGTH_LONG).show();
	           }
	}}

	public void buttons(View v){
		on_click = true;
		switch(v.getId()){
		case R.id.gu:
			on_click = false;
			break;
		case R.id.gd:
			on_click = false;
			break;
		case R.id.n:
			if(race == true && paused == false){
				if(nitrous_on == false){
					nitrous_on = true;
					nitrous.setBackgroundResource(R.drawable.nitro_o);
				}else{
					nitrous_on = false;
					nitrous.setBackgroundResource(R.drawable.nitro_n);
					}
			}
			on_click = false;
			break;
		case R.id.p:
			if(race == true){
				race = false;
				power.setBackgroundResource(R.drawable.start);
				paused = false;
				resume.setBackgroundResource(R.drawable.play);
				nitrous_on = false;
					nitrous.setBackgroundResource(R.drawable.nitro_n);
				}else{
				race = true;
				power.setBackgroundResource(R.drawable.stop);
				paused = false;
				resume.setBackgroundResource(R.drawable.pause);
			}
			on_click = false;
			break;
		case R.id.res:
			if(race == true){
				if(paused == false){
					paused = true;
					resume.setBackgroundResource(R.drawable.play);
					if(nitrous_on == true){
						nitrous_on = false;
						nitrous.setBackgroundResource(R.drawable.nitro_n);
					}
				}else{
					paused = false;
					resume.setBackgroundResource(R.drawable.pause);
					}
			}
			on_click = false;
					break;
		}
	}
}				