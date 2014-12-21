package com.github.chemikadze.pumploader;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class PumpLoaderAuthenticatorService extends Service {

    private PumpLoaderAccountAuthenticator mAuthenticator;

    @Override
    public void onCreate() {
        super.onCreate();
        mAuthenticator = new PumpLoaderAccountAuthenticator(getApplicationContext());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }

}
