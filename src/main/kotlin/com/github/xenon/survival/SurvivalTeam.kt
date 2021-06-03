package com.github.xenon.survival

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Team

class SurvivalTeam(val team: Team) {
    var inventory = SurvivalInventory()
    var alive = true
    var offlinePlayers = team.players
    var currentPlayer: Player? = null
    fun swap() {
        currentPlayer?.gameMode = GameMode.SPECTATOR
        var i = 0
        offlinePlayers.forEachIndexed { index, p ->
            if(p.player == currentPlayer) {
                i = index
            }
        }
        if(offlinePlayers.toList()[i + 1] == null) {
            currentPlayer = offlinePlayers.toList().first().player
        } else {
            currentPlayer = offlinePlayers.toList()[i + 1].player
        }
    }
    fun onUpdate() {
        currentPlayer?.gameMode = GameMode.SURVIVAL
        offlinePlayers.toList().filter { it != currentPlayer }.forEach {
            it.player?.gameMode = GameMode.SPECTATOR
        }
        if(offlinePlayers.count() == 0) {
            alive = false
        }
        offlinePlayers.forEach {
            val distance = currentPlayer?.location?.distance(it.player!!.location)
            if(distance!! >= 256) {
                it.player?.teleport(currentPlayer!!.location)
            }
        }
    }
    fun removePlayer(player: Player) {
        val p = Bukkit.getOfflinePlayer(player.uniqueId)
        offlinePlayers.remove(p)
    }
    fun onInitialize() {
        team.players.forEach { p ->
            inventory.patch(p.player!!)
        }
    }
}