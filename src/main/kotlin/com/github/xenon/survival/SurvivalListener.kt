package com.github.xenon.survival

import net.kyori.adventure.text.Component.text
import org.bukkit.GameMode
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent

class SurvivalListener(val process: SurvivalProcess) : Listener {
    @EventHandler
    fun onPlayerPreLogin(event: PlayerLoginEvent) {
        @EventHandler
        fun onPlayerLogin(event: PlayerLoginEvent) {
            val player = event.player; if (player.isOp) return
            val battle = process.player(player)
            if (battle == null) {
                event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, text("게임 참가자가 아닙니다."))
            }
        }
    }
    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        process.team(event.entity).team.players.forEach {
            it.player?.sendMessage("팀원이 탈락했습니다.")
        }
        process.team(event.entity).swap()
        process.team(event.entity).removePlayer(event.entity)
        if(event.entity.isOp) {
            event.entity.gameMode = GameMode.SPECTATOR
        } else {
            event.entity.banPlayer("탈락하셨습니다.")
        }
    }
    @EventHandler
    fun onPlayerSwapItem(event: PlayerSwapHandItemsEvent) {
        event.isCancelled = true
        if(event.player.gameMode.isDamageable) {
            process.team(event.player).swap()
        }
    }
}