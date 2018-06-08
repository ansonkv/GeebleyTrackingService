package net.fan360.geebleytrackingservice.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Messagemodel {

    @SerializedName("username")
    @Expose
    var username: String? = null
    @SerializedName("latitude")
    @Expose
    var latitude: String? = null
    @SerializedName("longitude")
    @Expose
    var longitude: String? = null

}