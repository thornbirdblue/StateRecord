package com.thornbird.staterecord;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;

public class StateRecordActivity extends ListActivity {
	private final String TAG="BbkStateRecord";
	private static final boolean EnableLog=true;
	
	private final static String[] attr = {"Battery FG","Battery log","Android Process","kernel process"};
	
	private boolean bat_fg_enable = false;
	private boolean bat_log_enable = false;
	private boolean android_process_enable = false;
	private boolean kernel_process_enable = false;
	
	private static final int EVENT_UPDATE = 1;
	    
    private Button mLogRecord;

    private File mLogFile;
    private File batterylog = null;
    
    
    Calendar rightNow;
    SimpleDateFormat fmt;
    String sysDateTime;
 
    private boolean mIsRecording = false;
    private boolean mRun = false;

    private String cmdString = null;
    private String PowerCmdString = null;
    
    private Thread bbkrun;
    private int mUpdateInterval = 10000; // 10 sec
    
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

        cmdString = "cat /sys/devices/platform/mt6577-battery/";
        PowerCmdString = "cat /sys/class/power_supply/battery/";   
       
       if(EnableLog)
        	Log.d(TAG,"onCreate!\n");
    }
  
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
                String BatteryInfoLable = "BBK phone state log:\n";
                FileWriter fileWriter = new FileWriter(mLogFile);
                fileWriter.write(BatteryInfoLable);
                fileWriter.flush();
                fileWriter.close();
                
                if(EnableLog)
                	Log.d(TAG," create file:/sdcard/bbkstatelog/"+fileName+"\n");
     
    	 	} catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
    	 	}
    	 		
    	 		mRun = true;
    	        bbkrun = new runThread();
    	 		bbkrun.start();
    	 	
    	 		mIsRecording = true;
            
   	 	 		mLogRecord.setText("stop");
             }
             else
             {
            	     mRun = false;
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
    
        private void WriteCurrentBatteryInfo() {
                String LogContent = "\n";
                rightNow = Calendar.getInstance();
                fmt = new SimpleDateFormat("yyyyMMddhhmmss");
                sysDateTime = fmt.format(rightNow.getTime());
                LogContent = LogContent + "[" + sysDateTime + "]:"+ "\n" + mInfo;

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

  		};
    
  		
  		@Override
  		protected void onListItemClick(ListView l, View v, int position, long id)
  		{
  			super.onListItemClick(l, v, position, id);   
  			boolean isClicked = l.isItemChecked(position); 
  			switch(position)
  			{
  				case 0:
  					bat_fg_enable = isClicked;
  					if(EnableLog)
  		             	Log.d(TAG,"bat_fg_enable\n");
  					break;
  				case 1:
  					bat_log_enable = isClicked;
  					if(EnableLog)
  		             	Log.d(TAG,"bat_log_enable\n");
  					break;
  					
  				case 2:
  					android_process_enable = isClicked;
  					if(EnableLog)
  		             	Log.d(TAG,"android_process_enable\n");
  					break;
  					
  				case 3:
  					kernel_process_enable = isClicked;
  					if(EnableLog)
  		             	Log.d(TAG,"kernel_process_enable\n");
  					break;		
  			
  			} 			
  			 if(EnableLog)
             	Log.d(TAG," list view pos "+ position +" is clicked "+ isClicked+ "!!\n");
  		}

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

        public Handler mUpdateHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                case EVENT_UPDATE:
                    Bundle b = msg.getData();
                    mInfo = new String(b.getString("INFO"));
                    WriteCurrentBatteryInfo();
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
                    
                    if(bat_fg_enable)
                    {		
                    	text.append("-----------------------------------------\n");
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
                    }
                    
                    if(bat_log_enable)
                    {
                    	text.append("-----------------------------------------\n");
                    	for (int i = 0; i < bat_files.length; i++) {
                    		cmd = PowerCmdString + bat_files[i][0];
                        
                    		text.append(String.format("%1$-28s: [ %2$-6s ]%3$s\n",
                        		bat_files[i][0], getInfo(cmd), bat_files[i][1]));
                    	}
                    }

                    ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                                                      
                    if(android_process_enable)                    	
                    {
                    	text.append("-----------------------------------------\n");
                    	List<RunningAppProcessInfo> apps = am.getRunningAppProcesses();
                    	text.append( "Android Process name:\n");
                    	for(RunningAppProcessInfo app:apps){
                         text.append( app.processName + "\n");
                    	}
                    }
                    
                    if(kernel_process_enable)
                    {
                    	text.append("-----------------------------------------\n");
                    	cmd = "ps";
                		text.append(getInfo(cmd));
                		text.append("\n");
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
}