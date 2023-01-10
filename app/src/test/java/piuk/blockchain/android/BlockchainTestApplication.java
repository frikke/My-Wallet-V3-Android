package piuk.blockchain.android;

import android.annotation.SuppressLint;
import com.google.firebase.FirebaseApp;

@SuppressLint("Registered")
public class BlockchainTestApplication extends BlockchainApplication {

    @Override
    public void onCreate() {
        FirebaseApp.initializeApp(this);
        super.onCreate();
    }

    @Override
    protected void checkSecurityProviderAndPatchIfNeeded() {
        // No-op
    }
}