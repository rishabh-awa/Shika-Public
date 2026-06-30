package com.example.applicaion

data class QueueItem (
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val streamUrl: String = "",
    val addedBy: String = ""
    ) {
    constructor(id: String, map: Map<*, *>): this(
        id= id,
        title = map["title"] as? String?:"",
        artist = map["artist"] as? String?:"",
        streamUrl = map["streamUrl"] as? String?:""
    )
    fun toMap(): Map<String, Any>{
        return mapOf(
            "title" to title,
            "artist" to artist,
            "streamUrl" to streamUrl
        )
    }


}