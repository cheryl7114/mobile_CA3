package com.example.h2now

import com.google.gson.annotations.SerializedName

data class HydrationTipsResponse(
    @SerializedName("tips") val tips: List<Tip>
)

data class Tip(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("imageUrl") val imageUrl: String
)
