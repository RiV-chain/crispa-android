package org.rivchain.app.crispa

import android.os.Bundle
import android.widget.EditText
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager

class VpnActivity: AppCompatActivity() {

    private lateinit var enable: Switch
    private lateinit var ipv4_remote_subnet: EditText
    private lateinit var ipv6_remote_subnet: EditText
    private lateinit var ipv4_public_key: EditText
    private lateinit var ipv6_public_key: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vpn)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        enable = findViewById(R.id.enable_switch)
        ipv4_remote_subnet = findViewById(R.id.ipv4_edittext)
        ipv6_remote_subnet = findViewById(R.id.ipv6_edittext)
        ipv4_public_key = findViewById(R.id.ipv4_publickey_edittext)
        ipv6_public_key = findViewById(R.id.ipv6_publickey_edittext)
        val preferences =
            PreferenceManager.getDefaultSharedPreferences(this.baseContext)
        val tunnelRoutingEnable = preferences.getBoolean(MainActivity.TUNNEL_ROUTING_ENABLE, false)
        enable.isChecked = tunnelRoutingEnable
        val iPv4RemoteSubnet = preferences.getString(MainActivity.IPV4_REMOTE_SUBNET, null)
        if(iPv4RemoteSubnet!=null) {
            val iPv4PublicKey = preferences.getString(MainActivity.IPV4_PUBLIC_KEY, "")
            ipv4_remote_subnet.setText(iPv4RemoteSubnet)
            ipv4_public_key.setText(iPv4PublicKey)
        }
        val iPv6RemoteSubnet = preferences.getString(MainActivity.IPV6_REMOTE_SUBNET, null)
        if(iPv6RemoteSubnet!=null) {
            val iPv6PublicKey = preferences.getString(MainActivity.IPV6_PUBLIC_KEY, "")
            ipv6_remote_subnet.setText(iPv6RemoteSubnet)
            ipv6_public_key.setText(iPv6PublicKey)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        //save config
        val preferences =
            PreferenceManager.getDefaultSharedPreferences(this.baseContext)
        preferences.edit()
            .putBoolean(MainActivity.TUNNEL_ROUTING_ENABLE, enable.isChecked)
            .putString(MainActivity.IPV4_REMOTE_SUBNET, ipv4_remote_subnet.text.toString())
            .putString(MainActivity.IPV4_PUBLIC_KEY, ipv4_public_key.text.toString())
            .putString(MainActivity.IPV6_REMOTE_SUBNET, ipv6_remote_subnet.text.toString())
            .putString(MainActivity.IPV6_PUBLIC_KEY, ipv6_public_key.text.toString())
            .apply()
    }
}