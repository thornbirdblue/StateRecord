package com.thornbird.staterecord;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class StateRecordActivity extends ListActivity {
	private final String TAG="StateRecord";
	private static final boolean EnableLog=true;
	
	private final static String[] attr = {"Battery log","Task","Process"};
	
	private static final int EVENT_TICK = 1;
	private static final int EVENT_LOG_RECORD = 2;
	
	private static final int EVENT_UPDATE = 1;
	
    private String mStatus;
    private int mLevel;
    private int mScale;
    private String mHealth;
    private int mVoltage;
    private int mTemperature;
    private String mTechnology;
    private long mUptime;
    
    private Button mLogRecord;

    private File mLogFile;
    private File batterylog = null;
    
    
    Calendar rightNow;
    SimpleDateFormat fmt;
    String sysDateTime;
 
    private int interval = 10000;
    
    private boolean mIsRecording = false;
    private boolean mRun = false;

    private String cmdString = null;
    private String PowerCmdString = null;
    
    private Thread bbkrun;
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
      
        setContentView(R.layout.main);  
        
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
        		android.R.layout.simple_list_item_multiple_choice,attr);
        setListAdapter(adapter);
        
        final ListView listView = getListView();
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        
       
     
        
        mLogRecord = (Button) findViewById(R.id.start);
        if(mLogRecord == null)
        {
                Log.e(TAG, "clocwork worked...");
                //not return and let exception happened.
        }
        
        File sdcard = null;
        if(!Environment.getExternalStorageState().equals(Environment.MEDIA_REMOVED))
        {
                        sdcard =  Environment.getExternalStorageDirectory();
                        batterylog = new File(sdcard.getParent()+"/" + sdcard.getName() + "/bbkstatelog/");
                        Log.d(TAG, sdcard.getParent() +"/"+ sdcard.getName() + "/bbkstatelog/");
                        if(!batterylog.isDirectory())
                        {
                                batterylog.mkdirs();
                        }

        }
        
        if (ChipSupport.GetChip() == ChipSupport.MTK_6573_SUPPORT) {
            cmdString = "cat /sys/devices/platform/mt6573-battery/";
        } else if (ChipSupport.GetChip() == ChipSupport.MTK_6575_SUPPORT) {
            cmdString = "cat /sys/devices/platform/mt6575-battery/";
        } else if (ChipSupport.GetChip() == ChipSupport.MTK_6577_SUPPORT) {
            // 6577 branch
            cmdString = "cat /sys/devices/platform/mt6577-battery/";
        } else {
            cmdString = "";
        }

        PowerCmdString = "cat /sys/class/power_supply/battery/";
        
       if(EnableLog)
        	Log.d(TAG,"onCreate!\n");
    }
   
    @Override
    public void onResume()
    {
    	super.onResume();    	 
    	
 
             
    	if(EnableLog)
        	Log.d(TAG,"onResume!\n");
    }

    @Override
    public void onPause() {
        super.onPause();
   
   
    }

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
    	@Override
    	 public void onReceive(Context arg0, Intent arg1) {
                // TODO Auto-generated method stub
                String action = arg1.getAction();
                if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                	int plugType = arg1.getIntExtra("plugged", 0);

                    mLevel = arg1.getIntExtra("level", 0);
                    mScale = arg1.getIntExtra("scale", 0);
                    mVoltage = arg1.getIntExtra("voltage", 0);
                    mTemperature = arg1.getIntExtra("temperature", 0)/10;
                    mTechnology = arg1.getStringExtra("technology");

                    int status = arg1.getIntExtra("status", BatteryManager.BATTERY_STATUS_UNKNOWN);
                    String statusString;
                    if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
                        statusString = getString(R.string.battery_info_status_charging);
                        if (plugType > 0) {
                            statusString = statusString + " " + getString(
                                    (plugType == BatteryManager.BATTERY_PLUGGED_AC)
                                            ? R.string.battery_info_status_charging_ac
                                            : R.string.battery_info_status_charging_usb);
                        }
                    } else if (status == BatteryManager.BATTERY_STATUS_DISCHARGING) {
                        statusString = getString(R.string.battery_info_status_discharging);
                    } else if (status == BatteryManager.BATTERY_STATUS_NOT_CHARGING) {
                        statusString = getString(R.string.battery_info_status_not_charging);
                    } else if (status == BatteryManager.BATTERY_STATUS_FULL) {
                        statusString = getString(R.string.battery_info_status_full);
                    } else {
                        statusString = getString(R.string.battery_info_status_unknown);
                    }
                    mStatus = new String(statusString);

                    int health = arg1.getIntExtra("health", BatteryManager.BATTERY_HEALTH_UNKNOWN);
                    String healthString;
                    if (health == BatteryManager.BATTERY_HEALTH_GOOD) {
                        healthString = getString(R.string.battery_info_health_good);
                    } else if (health == BatteryManager.BATTERY_HEALTH_OVERHEAT) {
                        healthString = getString(R.string.battery_info_health_overheat);
                    } else if (health == BatteryManager.BATTERY_HEALTH_DEAD) {
                        healthString = getString(R.string.battery_info_health_dead);
                    } else if (health == BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE) {
                        healthString = getString(R.string.battery_info_health_over_voltage);
                    } else if (health == BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE) {
                        healthString = getString(R.string.battery_info_health_unspecified_failure);
                    } else {
                    	  healthString = getString(R.string.battery_info_health_unknown);
                    }
                    mHealth = new String(healthString);

                }
        }		
    };

    
 
    public void onButtonClick(View arg0) {
    	 if(arg0.getId() == mLogRecord.getId())
         {
			if(false == mIsRecording)
             {
    	
    	 	if(Environment.getExternalStorageState().equals(Environment.MEDIA_REMOVED)
    	 				||  Environment.getExternalStorageState().equals(Environment.MEDIA_BAD_REMOVAL)
    	 				||  Environment.getExternalStorageState().equals(Environment.MEDIA_UNMOUNTED))
            {
                         AlertDialog.Builder builder = new AlertDialog.Builder(this);
                         builder.setTitle("SD Card not available");
                         builder.setMessage("Please insert an SD Card.");
                         builder.setPositiveButton("OK" , null);
                         builder.create().show();
                         return;
            }

    	 	String state = Environment.getExternalStorageState();
    	 	Log.i(TAG, "Environment.getExternalStorageState() is : " + state);

    	 	if(Environment.getExternalStorageState().equals(Environment.MEDIA_SHARED))
    	 	{
               AlertDialog.Builder builder = new AlertDialog.Builder(this);
               builder.setTitle("sdcard is busy");
               builder.setMessage("Sorry, your SD card is busy.");
               builder.setPositiveButton("OK" , null);
               builder.create().show();
               return;
    	 	}    	
    	
    	 	 mLogRecord.setText("stop");

    	 	//Create a new file under the "/sdcard/batterylog" path
    	 	rightNow = Calendar.getInstance();
    	 	fmt = new SimpleDateFormat("yyyyMMddhhmmss");
    	 	sysDateTime = fmt.format(rightNow.getTime());
    	 	String fileName = "";
    	 	fileName = fileName + sysDateTime;
    	 	fileName = fileName + ".txt";
    	 	Log.i(TAG, fileName);
        
    	 	if(EnableLog)
    	 		Log.d(TAG,"onButtonClick!\n");
        
    	 	mLogFile = new File("/sdcard/bbkstatelog/" + fileName);
    	 	try {
                mLogFile.createNewFile();
                String BatteryInfoLable = "SystemDate,     Battery status, level, scale, health, voltage, temperature, technology, time since boot:\n";
                FileWriter fileWriter = new FileWriter(mLogFile);
                fileWriter.write(BatteryInfoLable);
                fileWriter.flush();
                fileWriter.close();
                
                if(EnableLog)
                	Log.d(TAG," create file:/sdcard/bbkstatelog/!!\n");
     
    	 	} catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
    	 	}
    	 	mRun = true;
    	 	bbkrun = new runThread();
    	 	bbkrun.start();
    	 	
            mIsRecording = true;
             }
             else
             {
            	 	 bbkrun.stop();
                     mLogRecord.setText("start");
                     mIsRecording = false;
                     AlertDialog.Builder builder = new AlertDialog.Builder(this);
                     builder.setTitle("bbkstatelog Saved");
                     builder.setMessage("BatteryLog has been saved under" + " /sdcard/bbkstatelog.");
                     builder.setPositiveButton("OK" , null);
                     builder.create().show();
             }
	
         }	


    }
    
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_TICK:
                    updateBatteryStats();
                    sendEmptyMessageDelayed(EVENT_TICK, interval);
                    break;
            }
        }

                private void updateBatteryStats() {
                        // TODO Auto-generated method stub
                	mUptime = SystemClock.elapsedRealtime();
                }
    };

    public Handler mLogHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_LOG_RECORD:
                        Log.i(TAG, "Record one time");
                        WriteCurrentBatteryInfo();
                        sendEmptyMessageDelayed(EVENT_LOG_RECORD, interval);
                  break;
            }
        }

        private void WriteCurrentBatteryInfo() {
                String LogContent = "";
                rightNow = Calendar.getInstance();
                fmt = new SimpleDateFormat("yyyyMMddhhmmss");
                sysDateTime = fmt.format(rightNow.getTime());
                LogContent = LogContent + "[" + sysDateTime + "]" + " " + mStatus + ", " + mLevel + ", " + mScale
                        + ", " + mHealth + ", " + mVoltage + ", " + mTemperature
                        + ", " + mTechnology + ", " + mUptime + "\n";


                try {
                        FileWriter fileWriter = new FileWriter(mLogFile, true);
                        fileWriter.write(LogContent);
                        fileWriter.flush();
                        fileWriter.close();
                        
                        if(EnableLog)
                        	Log.d(TAG,"Write file!\n");
                        
                        } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                        }

        		}

        };

        private String getInfo(String cmd) {
            String result = null;
            try {
                String[] cmdx = { "/system/bin/sh", "-c", cmd }; // file must
                // exist// or
                // wait()
                // return2
                int ret = ShellExe.execCommand(cmdx);
                if (0 == ret) {
                    result = ShellExe.getOutput();
                } else {
                    // result = "ERROR";
                    result = ShellExe.getOutput();
                }

            } catch (IOException e) {
                result = "ERR.JE";
            }
            return result;
        }

        private String mInfo;
        private int mUpdateInterval = 1500; // 1.5 sec
        public Handler mUpdateHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                case EVENT_UPDATE:
                    Bundle b = msg.getData();
                    mInfo = new String(b.getString("INFO"));
                    break;
                }
            }
        };

        private String[][] files = {       	  
        	            { "FG_Battery_CurrentConsumption", "mA" } };

        private String[][] bat_files = {  
        		{ "BatteryAverageCurrent", "" }, { "BatterySenseVoltage", "mV" },
        		{ "ChargerVoltage", "mV" }, { "ISenseVoltage", "mV" },
        		{ "TempBattVoltage", "" }, { "batt_vol", "mV" },
        		{ "capacity", "" }, { "status", "" }};
        
        class runThread extends Thread {
            public void run() {
                while (mRun) {
                    StringBuilder text = new StringBuilder("");
                    String cmd = "";
                    for (int i = 0; i < files.length; i++) {
                      
                        cmd = cmdString + files[i][0];
                        if (files[i][1].equalsIgnoreCase("mA")) {
                            double f = 0.0f;
                            try {
                                f = Float.valueOf(getInfo(cmd)) / 10.0f;
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "read file error " + files[i][0]);
                            }
                            text.append(String.format("%1$-28s:[ %2$-6s ]%3$s\n",
                                    files[i][0], f, files[i][1]));
                        } else {
                            text.append(String.format("%1$-28s: [ %2$-6s ]%3$s\n",
                                    files[i][0], getInfo(cmd), files[i][1]));
                        }
                    }
                    
                    for (int i = 0; i < files.length; i++) {
                        cmd = PowerCmdString + files[i][0];
                        
                        text.append(String.format("%1$-28s: [ %2$-6s ]%3$s\n",
                                    files[i][0], getInfo(cmd), files[i][1]));

                    }

                    Bundle b = new Bundle();
                    b.putString("INFO", text.toString());

                    Message msg = new Message();
                    msg.what = EVENT_UPDATE;
                    msg.setData(b);

                    mUpdateHandler.sendMessage(msg);
                    try {
                        sleep(mUpdateInterval);
                    } catch (InterruptedException e) {

                    }
                }
            }
        }

        static class ShellExe {

            /*
             * how to use try { execCommand("./data/kenshin/x.sh"); }
             * catch (IOException e) { e.printStackTrace(); } }
             */
            public static String ERROR = "ERROR";
            private static StringBuilder sb = new StringBuilder("");
            public static String getOutput()
            {
                    return sb.toString();
            }
            public static int execCommand(String[] command) throws IOException {

                    // start the ls command running
                    // String[] args = new String[]{"sh", "-c", command};
                    Runtime runtime = Runtime.getRuntime();
                    Process proc = runtime.exec(command);
                    InputStream inputstream = proc.getInputStream();
                    InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
                    BufferedReader bufferedreader = new BufferedReader(inputstreamreader);

                    // read the ls output
                    //sb = new StringBuilder("");
                    sb.delete(0, sb.length());
                    try {
                            if (proc.waitFor() != 0) {
                                    Log.i("MTK","exit value = " + proc.exitValue());
                                    sb.append(ERROR);
                                    return -1;
                            }
                            else
                            {
                                    String line;
                                    //one line has not CR, or  "line1 CR line2 CR line3..."
                                    line = bufferedreader.readLine();
                                    if(line != null)
                                    {
                                            sb.append(line);
                                    }
                                    else
                                    {
                                            return 0;
                                    }
                                    while(true)
                                    {
                                            line = bufferedreader.readLine();
                                            if(line == null)
                                            {
                                                    break;
                                            }
                                            else
                                            {
                                                    sb.append('\n');
                                                    sb.append(line);
                                            }
                                    }
                                    return 0;
                            }
                    } catch (InterruptedException e) {
                            Log.i("MTK","exe fail " + e.toString());
                            sb.append(ERROR);
                            return -1;
                    }
            }
    }

        static class ChipSupport {
            // constants below.
            public final static int MTK_UNKNOWN_SUPPORT = 0;
            public final static int MTK_6573_SUPPORT = 1;
            public final static int MTK_6516_SUPPORT = 2;
            public final static int MTK_6575_SUPPORT = 4;
            public final static int MTK_6577_SUPPORT = 8;

            public static native int GetChip();

            public static String GetChipString() {
                if (GetChip() == MTK_6573_SUPPORT) {
                    return "Chip 6573 ";
                } else if (GetChip() == MTK_6516_SUPPORT) {
                    return "Chip 6516 ";
                } else if (GetChip() == MTK_6575_SUPPORT) {
                    return "Chip 6575 ";
                } else if (GetChip() == MTK_6577_SUPPORT) {
                    return "Chip 6577 ";
                } else {
                    return "Chip unknown ";
                }
            }

            public final static int MTK_FM_SUPPORT = 0;
            public final static int MTK_FM_TX_SUPPORT = 1;
            public final static int MTK_RADIO_SUPPORT = 2;
            public final static int MTK_AGPS_APP = 3;
            public final static int MTK_GPS_SUPPORT = 4;
            public final static int HAVE_MATV_FEATURE = 5;
            public final static int MTK_BT_SUPPORT = 6;
            public final static int MTK_WLAN_SUPPORT = 7;
            public final static int MTK_TTY_SUPPORT = 8;

            // FEATURE SUPPORTED
            public static native boolean IsFeatureSupported(int feature_id);

            static {
                System.loadLibrary("em_chip_support_jni");

            }
        }


}