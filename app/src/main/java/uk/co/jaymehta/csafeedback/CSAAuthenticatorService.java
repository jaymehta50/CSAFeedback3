package uk.co.jaymehta.csafeedback;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by jkm50 on 02/07/2015.
 */
public class CSAAuthenticatorService extends Service {
    @Override
    public IBinder onBind(Intent intent) {

        CSAAuthenticator authenticator = new CSAAuthenticator(this);
        return authenticator.getIBinder();
    }
}