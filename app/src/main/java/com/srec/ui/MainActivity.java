package com.srec.ui;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.srec.AppController;
import com.srec.R;
import com.srec.adapter.ItemlistAdapter;
import com.srec.listener.OnItemClickListener;
import com.srec.wrapper.AppInfo;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallState;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.OnSuccessListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnItemClickListener {

    private static final int RC_APP_UPDATE = 201;

    private TextView app_found_count;
    private ImageView doneIcon;
    private ImageView dragonIcon;
    private RelativeLayout listLayout;
    private RelativeLayout noappsfoundLayout;
    private ProgressBar progressBar;
    private RecyclerView recycler_view;
    private Button rescan_now;
    private Button scan_now;
    private RelativeLayout scan_ui;


    private List<AppInfo> appInfos = new ArrayList<>();

    InstallStateUpdatedListener installStateUpdatedListener = new InstallStateUpdatedListener() {
        public void onStateUpdate(InstallState installState) {
            try {
                if (installState.installStatus() == InstallStatus.DOWNLOADED) {
                    popupSnackbarForCompleteUpdate();
                } else if (installState.installStatus() != InstallStatus.INSTALLED) {
                    Log.i("MainActivity", "InstallStateUpdatedListener: state: " + installState.installStatus());
                } else if (mAppUpdateManager != null) {
                    mAppUpdateManager.unregisterListener(installStateUpdatedListener);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private ItemlistAdapter itemlistAdapter;
    AppUpdateManager mAppUpdateManager;

    private boolean isDeleteClick;


    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_main);

        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setHomeAsUpIndicator(R.mipmap.ic_launcher);
        }

        progressBar = findViewById(R.id.progressBar);
//        dragonIcon = findViewById(R.id.dragonIcon);
//        doneIcon = findViewById(R.id.doneIcon);

        scan_now = findViewById(R.id.scan_now);
        recycler_view = findViewById(R.id.recycler_view);
        listLayout = findViewById(R.id.listLayout);
        scan_ui = findViewById(R.id.scan_ui);
        noappsfoundLayout = findViewById(R.id.noappsfoundLayout);
        app_found_count = findViewById(R.id.app_found_count);
        rescan_now = findViewById(R.id.rescan_now);

        itemlistAdapter = new ItemlistAdapter(this, appInfos, this);
        recycler_view.setLayoutManager(new LinearLayoutManager(this));
        recycler_view.setItemAnimator(new DefaultItemAnimator());
        recycler_view.setAdapter(itemlistAdapter);

        ((TextView) findViewById(R.id.scan_now_txt)).setText(Html.fromHtml(getResources().getString(R.string.clickscantext)));

        scan_now.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                getApps();
            }
        });
        rescan_now.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                getApps();
            }
        });
        checkAppUpdate();
    }

    private void checkAppUpdate() {
        try {
            mAppUpdateManager = AppUpdateManagerFactory.create(this);
            mAppUpdateManager.registerListener(installStateUpdatedListener);
            mAppUpdateManager.getAppUpdateInfo().addOnSuccessListener(new OnSuccessListener<AppUpdateInfo>() {
                public void onSuccess(AppUpdateInfo appUpdateInfo) {
                    if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                        try {
                            mAppUpdateManager.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.FLEXIBLE, MainActivity.this, RC_APP_UPDATE);
                        } catch (IntentSender.SendIntentException e) {
                            e.printStackTrace();
                        }
                    } else if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                        popupSnackbarForCompleteUpdate();
                    } else {
                        Log.e("MainActivity", "checkForAppUpdateAvailability: something else");
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_APP_UPDATE && resultCode != RESULT_OK
        ) {
            Log.e("onActivityResult", "onActivityResult: app download failed");
        }
    }

    private void popupSnackbarForCompleteUpdate() {
        try {
            Snackbar make = Snackbar.make(findViewById(R.id.scan_ui), "An update has just been downloaded.", Snackbar.LENGTH_INDEFINITE);
            make.setAction("RESTART", new View.OnClickListener() {
                public void onClick(View view) {
                    if (mAppUpdateManager != null) {
                        mAppUpdateManager.completeUpdate();
                    }
                }
            });
            make.setActionTextColor(getResources().getColor(R.color.green));
            make.show();
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
        }
    }


    private void getApps() {
        new GetAppsAsync().execute();
    }


    protected void onResume() {
        super.onResume();
        if (isDeleteClick) {
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    getApps();
                }
            }, 1500);
        }
        isDeleteClick = false;
    }

    public void onItemClick(int i) {
        isDeleteClick = true;
    }

    class AppUninstalledreceiver extends BroadcastReceiver {
        AppUninstalledreceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            getApps();
        }
    }

    class GetAppsAsync extends AsyncTask<Void, Void, List<AppInfo>> {

        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            scan_ui.setVisibility(View.GONE);
        }

        protected List<AppInfo> doInBackground(Void... voidArr) {
            return AppController.getDbHelper().isExist(getInstalledApps(), appInfos);
        }

        protected void onPostExecute(List<AppInfo> list) {
            super.onPostExecute(list);
            progressBar.setVisibility(View.GONE);
            scan_ui.setVisibility(View.GONE);
            if (list.size() > 0) {
                listLayout.setVisibility(View.VISIBLE);
                noappsfoundLayout.setVisibility(View.GONE);
                app_found_count.setText(Html.fromHtml(getResources().getString(R.string.app_found_count, list.size())));
            } else {
                listLayout.setVisibility(View.GONE);
                noappsfoundLayout.setVisibility(View.VISIBLE);
            }
//            dragonIcon.setVisibility(View.GONE);
//            doneIcon.setVisibility(View.VISIBLE);
            itemlistAdapter.notifyDataSetChanged();
            if (list.size() == 0) {
                Collections.sort(appInfos, new Comparator<AppInfo>() {
                    public int compare(AppInfo appInfo, AppInfo appInfo2) {
                        return appInfo.appName.compareToIgnoreCase(appInfo2.appName);
                    }
                });
            }
        }
    }

//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.main, menu);
//        return super.onCreateOptionsMenu(menu);
//    }
//
//    public boolean onOptionsItemSelected(MenuItem menuItem) {
//        int itemId = menuItem.getItemId();
//        if (itemId == R.id.contact_us) {
//            openUrl(getResources().getString(R.string.contact_us_url));
//            return true;
//        } else if (itemId == R.id.privacy_policy) {
//            openUrl(getResources().getString(R.string.privacy_policy_url));
//            return true;
//        } else if (itemId != R.id.rate_us) {
//            return super.onOptionsItemSelected(menuItem);
//        } else {
//            rateus();
//            return true;
//        }
//    }

    private void openUrl(String str) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(str)));
    }

    private void rateus() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName()));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException unused) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + getPackageName())));
        }
    }

    public void shareApp() {
        try {
            Intent intent = new Intent("android.intent.action.SEND");
            intent.setType("text/plain");
            intent.putExtra("android.intent.extra.SUBJECT", getResources().getString(R.string.app_name));
            intent.putExtra("android.intent.extra.TEXT", "\nHey, I am using Remove China Apps to get rid of Chinese apps. If you want the same try using the app by clicking\n\n" + "https://play.google.com/store/apps/details?id=" + getPackageName() + "\n\n");
            startActivity(Intent.createChooser(intent, "choose one"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isSystemPackage(PackageInfo packageInfo) {
        return (packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }

    public List<AppInfo> getInstalledApps() {
        ArrayList<AppInfo> arrayList = new ArrayList<>();
        List<PackageInfo> installedPackages = getPackageManager().getInstalledPackages(0);
        for (int i = 0; i < installedPackages.size(); i++) {
            PackageInfo packageInfo = installedPackages.get(i);
            if (!isSystemPackage(packageInfo)) {
                AppInfo appInfo = new AppInfo();
                appInfo.appName = packageInfo.applicationInfo.loadLabel(getPackageManager()).toString();
                appInfo.packageName = packageInfo.packageName;
                appInfo.versionName = packageInfo.versionName;
                appInfo.versionCode = packageInfo.versionCode;
                appInfo.icon = packageInfo.applicationInfo.loadIcon(getPackageManager());
                appInfo.size = (new File(packageInfo.applicationInfo.publicSourceDir).length() / 1024 * 1024) + " MB";
                arrayList.add(appInfo);
            }
        }
        return arrayList;
    }
}
