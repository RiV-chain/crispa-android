package org.mesh.app.crispa

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager

class VpnActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vpn)
        setSupportActionBar(findViewById(R.id.toolbar))
        val preferences =
            PreferenceManager.getDefaultSharedPreferences(this.baseContext)
        //val ipv6Address = intent.extras!!.getString(MainActivity.IPv6, "")
        //val publicKey = preferences.getString(MainActivity.publicKey, "")
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(false)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}