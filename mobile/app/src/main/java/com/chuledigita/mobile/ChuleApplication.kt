package com.chuledigita.mobile

import android.app.Application
import com.chuledigita.mobile.data.mock.MockDataProvider

/**
 * Application class initializing global application state and [AppContainer].
 */
class ChuleApplication : Application() {

    /**
     * AppContainer instance used by ViewModels to access repositories.
     */
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        
        // Initialize and seed MockDataProvider outside of any Compose snapshots.
        // This prevents IllegalStateException: "Cannot modify a state object in a read-only snapshot"
        // which occurs if the data lists are first populated inside a snapshotFlow or composition.
        MockDataProvider.seedMockData()

        container = DefaultAppContainer(this)
    }
}
