package com.example.data

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class OnlineRoomData(
    val roomCode: String = "",
    val status: String = "WAITING", // WAITING, PLAYING, FINISHED
    val playerHostId: String = "",
    val playerGuestId: String? = null,
    val hostName: String = "Player 1",
    val guestName: String = "Player 2",
    val activePlayer: String = "O", // "O" or "X"
    val gridSize: Int = 3,
    val board: List<String> = List(9) { "" },
    val winner: String? = null,
    val isDraw: Boolean = false,
    val hostMutedMic: Boolean = false,
    val guestMutedMic: Boolean = false,
    val sdpOffer: String? = null,
    val sdpAnswer: String? = null,
    val chatMessages: Map<String, Map<String, String>> = emptyMap()
)
