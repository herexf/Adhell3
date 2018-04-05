package com.fusionjack.adhell3.viewmodel;


import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

import com.fusionjack.adhell3.App;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.entity.FirewallWhitelistedPackage;
import com.fusionjack.adhell3.utils.AdhellAppIntegrity;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.Maybe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class AdhellWhitelistAppsViewModel extends AndroidViewModel {

    @Inject
    AppDatabase appDatabase;

    private LiveData<List<AppInfo>> appInfos;

    public AdhellWhitelistAppsViewModel(Application application) {
        super(application);
        App.get().getAppComponent().inject(this);
    }

    public LiveData<List<AppInfo>> getSortedAppInfo() {
        if (appInfos == null) {
            appInfos = new MutableLiveData<>();
            loadAppInfos();
        }
        return appInfos;
    }

    private void loadAppInfos() {
        appInfos = appDatabase.applicationInfoDao().getAllSortedByWhitelist();
    }

    public void toggleApp(AppInfo appInfo) {
        Maybe.fromCallable(() -> {
            appInfo.adhellWhitelisted = !appInfo.adhellWhitelisted;
            appDatabase.applicationInfoDao().update(appInfo);

            if (appInfo.adhellWhitelisted) {
                FirewallWhitelistedPackage whitelistedPackage = new FirewallWhitelistedPackage();
                whitelistedPackage.packageName = appInfo.packageName;
                whitelistedPackage.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
                appDatabase.firewallWhitelistedPackageDao().insert(whitelistedPackage);
            } else {
                appDatabase.firewallWhitelistedPackageDao().deleteByPackageName(appInfo.packageName);
            }

            return null;
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
    }
}
