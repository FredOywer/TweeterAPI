package me.tweet;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by Fredrick on 08/12/2015.
 */
public class CheckConnection {
    private Context c;

    public CheckConnection(Context context){
        this.c = context;
    }

    public Boolean isConnecting(){
        ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null){
            NetworkInfo info = cm.getActiveNetworkInfo();
            if (info != null)
                    if (info.getState() == NetworkInfo.State.CONNECTED)
                    {
                        return true;
                    }
        }
        return false;
    }
}
