package com.troy.sansina

import android.content.Context
import android.media.MediaPlayer
import android.os.SystemClock

/**
 * Robot voice: pre-baked ElevenLabs lines bundled in assets/voice/.
 * invite.mp3 plays on the idle screen (with a cooldown so it doesn't nag),
 * win.mp3 plays when the card is revealed. Missing assets fail silently.
 */
class Voice(private val ctx: Context) {
    private var player: MediaPlayer? = null
    private var lastInviteAt = 0L

    /** Don't repeat the invite more often than this while idling. */
    private val inviteCooldownMs = 30_000L

    private fun play(file: String) {
        runCatching {
            player?.release()
            val p = MediaPlayer()
            player = p
            ctx.assets.openFd("voice/$file").use { fd ->
                p.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            }
            p.setOnCompletionListener { it.release(); if (player == it) player = null }
            p.prepare()
            p.start()
        }
    }

    fun invite() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastInviteAt < inviteCooldownMs) return
        lastInviteAt = now
        play("invite.mp3")
    }

    fun win() = play("win.mp3")

    fun stop() {
        runCatching { player?.release() }
        player = null
    }
}
