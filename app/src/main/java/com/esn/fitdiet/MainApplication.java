package com.esn.fitdiet;

import android.app.Application;
import com.esn.fitdiet.data.local.AppDatabase;
import com.esn.fitdiet.data.repository.AppContainer;
import com.esn.fitdiet.data.repository.AppContainerImpl;
import com.esn.fitdiet.util.AlarmScheduler;

/**
 * Application entry point. Owns the Room database, repositories and schedules the
 * daily summary alarm on cold start.
 */
public class MainApplication extends Application {

    private AppContainer container;

    @Override
    public void onCreate() {
        super.onCreate();
        AppDatabase db = AppDatabase.getInstance(this);
        container = new AppContainerImpl(db);
        // Ensure the daily 20:00 summary alarm is registered.
        AlarmScheduler.schedule(this);
    }

    public AppContainer getContainer() {
        return container;
    }
}
