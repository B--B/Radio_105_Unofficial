package com.bb.radio105;

import org.adblockplus.AdblockEngine;
import org.adblockplus.libadblockplus.android.AdblockEngineProvider;
import org.adblockplus.libadblockplus.android.settings.AdblockHelper;

import timber.log.Timber;

public class Application extends android.app.Application
{
    private final AdblockEngineProvider.EngineCreatedListener engineCreatedListener =
            adblockEngine -> {
                // put your AdblockEngine initialization here
            };

    private final AdblockEngineProvider.EngineDisposedListener engineDisposedListener =
            () -> {
                // put your AdblockEngine de-initialization here
            };

    @Override
    public void onCreate()
    {
        super.onCreate();

        if (BuildConfig.DEBUG)
        {
            Timber.plant(new Timber.DebugTree());
        }


            final AdblockHelper helper = AdblockHelper.get();
            helper
                    .init(this, null /*use default value*/, AdblockHelper.PREFERENCE_NAME)
                    .preloadSubscriptions(
                            R.raw.easylist_minified,
                            R.raw.exceptionrules_minimal)
                    .addEngineCreatedListener(engineCreatedListener)
                    .addEngineDisposedListener(engineDisposedListener);

            helper.getSiteKeysConfiguration().setForceChecks(true);
    }
}
