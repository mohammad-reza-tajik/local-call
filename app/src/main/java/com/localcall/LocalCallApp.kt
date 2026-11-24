package com.localcall

import android.app.Application
import com.localcall.call.CallEngine
import com.localcall.data.IdentityRepository

/** Holds the process-wide singletons so a call survives the activity being recreated. */
class LocalCallApp : Application() {
    val identityRepository: IdentityRepository by lazy { IdentityRepository(this) }
    val engine: CallEngine by lazy { CallEngine(this, identityRepository) }
}
