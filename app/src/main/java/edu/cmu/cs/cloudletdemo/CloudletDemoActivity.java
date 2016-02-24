package edu.cmu.cs.cloudletdemo;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.gabriel.Const;
import edu.cmu.cs.gabriel.GabrielClientActivity;
import edu.cmu.cs.gabriel.GabrielConfigurationAsyncTask;
import edu.cmu.cs.gabriel.R;

public class CloudletDemoActivity extends AppCompatActivity implements
        GabrielConfigurationAsyncTask.AsyncResponse {
    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;

    private static final String TAG = "cloudletDemoActivity";
    private static final int DLG_EXAMPLE1 = 0;
    private static final int TEXT_ID = 999;
    public String inputDialogResult;
    private CloudletFragment childFragment;
    private EditText dialogInputTextEdit;
    int curModId = -1;

    protected static final String
            IPV4Pattern = "(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])";
    protected static final String IPV6Pattern = "([0-9a-f]{1,4}:){7}([0-9a-f]){1,4}";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isOnline()){
            notifyError(Const.CONNECTIVITY_NOT_AVAILABLE, true);
        } else {
            setContentView(R.layout.activity_cloudlet_demo);

            toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            viewPager = (ViewPager) findViewById(R.id.viewpager);
            setupViewPager(viewPager);

            tabLayout = (TabLayout) findViewById(R.id.tabs);
            tabLayout.setupWithViewPager(viewPager);
            curModId=R.id.setting_cloudlet_ip;
            inputDialogResult = Const.CLOUDLET_GABRIEL_IP;
            sendOpenFaceResetRequest(Const.CLOUDLET_GABRIEL_IP);
        }

    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        childFragment = new CloudletFragment();
        adapter.addFragment(childFragment, "Face Swap Demo");
//        adapter.addFragment(new CloudFragment(), "cloud-EC2");
        viewPager.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_cloudlet_demo, menu);
        return true;
    }

    @Override
    public void processFinish(boolean output) {
        String serverLocation ="";
        if (curModId == R.id.setting_cloudlet_ip){
            serverLocation="Cloudlet";
        } else if (curModId == R.id.setting_cloud_ip){
            serverLocation="Cloud";
        }

        if (!output){
            createExampleDialog("Settings", "No Gabriel Server Found. \n" +
                    "Please enter a Gabriel Server IP (" + serverLocation + "): ");
        } else {
            if (curModId == R.id.setting_cloudlet_ip){
                Const.CLOUDLET_GABRIEL_IP = inputDialogResult;
                Log.i(TAG, "cloudlet ip changed to : " + Const.CLOUDLET_GABRIEL_IP);
            } else if (curModId == R.id.setting_cloud_ip){
                Const.CLOUD_GABRIEL_IP = inputDialogResult;
                Log.i(TAG, "cloudlet ip changed to : " + Const.CLOUD_GABRIEL_IP);
            }
            curModId = -1;
        }
    }

    public void sendOpenFaceResetRequest(String remoteIP){
        boolean online = isOnline();
        if (online){
            GabrielConfigurationAsyncTask task =
                    new GabrielConfigurationAsyncTask(this,
                            remoteIP,
                            GabrielClientActivity.VIDEO_STREAM_PORT,
                            GabrielClientActivity.RESULT_RECEIVING_PORT,
                            this);
            task.execute(Const.GABRIEL_CONFIGURATION_RESET_STATE);
            Log.d(TAG, "send reset openface server request");
            childFragment.clearTrainedPeople();
        } else {
            notifyError(Const.CONNECTIVITY_NOT_AVAILABLE, false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.setting_cloudlet_ip) {
            createExampleDialog("Settings", "Enter Cloudlet IP:");
            curModId = R.id.setting_cloudlet_ip;
            return true;
        }
        if (id == R.id.setting_cloud_ip) {
            createExampleDialog("Settings", "Enter Cloud IP:");
//            settingDialog.show();
            curModId = R.id.setting_cloud_ip;
            return true;
        }

        if (id == R.id.setting_reset_openface_server) {
            //check wifi state
            sendOpenFaceResetRequest(Const.CLOUDLET_GABRIEL_IP);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void notifyError(String msg, final boolean terminate){
        DialogInterface.OnClickListener error_listener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (terminate){
                            finish();
                        }
                    }
                };
        new AlertDialog.Builder(this)
                .setTitle("Error").setMessage(msg)
                .setNegativeButton("close", error_listener).show();
    }

//    private void notifyError(String msg, final boolean terminate){
//        DialogInterface.OnClickListener error_listener =
//                new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        if (terminate){
//                            finish();
//                        }
//                    }
//                };
//        new AlertDialog.Builder(this)
//                .setTitle("Error").setMessage(msg)
//                .setNegativeButton("close", error_listener).show();
//    }

    public boolean isOnline() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }


    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }

    /**
     * If a dialog has already been created,a
     * this is called to reset the dialog
     * before showing it a 2nd time. Optional.
     */
//    @Override
//    protected void onPrepareDialog(int id, Dialog dialog) {
//        switch (id) {
//            case DLG_EXAMPLE1:
//                // Clear the input box.
//                EditText text = (EditText) dialog.findViewById(TEXT_ID);
//                text.setText("");
//                break;
//        }
//    }

    public boolean isIpAddress(String ipAddress) {
        if (ipAddress.matches(IPV4Pattern) || ipAddress.matches(IPV6Pattern)) {
            return true;
        }
        return false;
    }

    /**
     * Create and return an example alert dialog with an edit text box.
     */
    public Dialog createExampleDialog(String title, String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(msg);

        // Use an EditText view to get user input.
        final EditText input = new EditText(this);
        input.setText("");
        dialogInputTextEdit = input;
        inputDialogResult=null;
        builder.setView(input);

        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                return;
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
        Button customOkButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        customOkButton.setOnClickListener(new CustomListener(alertDialog));
        return alertDialog;
    }

    class CustomListener implements View.OnClickListener {
        private final Dialog dialog;
        public CustomListener(Dialog dialog) {
            this.dialog = dialog;
        }
        @Override
        public void onClick(View v) {
            String value = dialogInputTextEdit.getText().toString();
            Log.d(TAG, "user input: " + value);
            if (!isIpAddress(value)) {
                Toast.makeText(getApplicationContext(),
                        "Invalid IP address", Toast.LENGTH_SHORT).show();
            } else {
                sendOpenFaceResetRequest(value);
                inputDialogResult = value;
                dialog.dismiss();
            }
            return;
        }
    }

}
