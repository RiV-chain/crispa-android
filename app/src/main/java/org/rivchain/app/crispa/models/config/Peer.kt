package org.mesh.app.crispa.models.config

import com.google.gson.annotations.SerializedName

data class Peer (
    //Example [{"Key":"JQZIX3KIamcp/6S9rycKiAGyg9MK7U6h8UUY5ej36fY=","Root":"AAABERGfllXfKNJshDs/8uzKEIFkFEccE16dmZV/cAo=","Coords":[2,4],"Port":1,"Remote":"tcp://[fe80::5207:4518:4378:7f1%wlan0]:57541","IP":"202:d7cd:bd04:6bbc:acc6:b002:da12:86c7"},{"Key":"DCNBiKAV1xr72JAFUgNrOYfY6Qm/f0Nq6ESZTSLn1eo=","Root":"AAABERGfllXfKNJshDs/8uzKEIFkFEccE16dmZV/cAo=","Coords":[2,4,1],"Port":2,"Remote":"tcp://[fe80::1c39:839:90a5:6ef%wlan0]:1108","IP":"204:7b97:ceeb:fd45:1ca0:84ed:ff55:bf92"}]
    @SerializedName("key") var key : String,
    @SerializedName("priority") var priority : String,
    @SerializedName("coords") var coords : Array<Int>,
    @SerializedName("port") var port : Int,
    @SerializedName("remote") var remote : String,
    @SerializedName("address") var address : String,
    @SerializedName("bytes_recvd") var bytes_recvd : Long,
    @SerializedName("bytes_sent") var bytes_sent : Long,
    @SerializedName("uptime") var uptime : Float

)