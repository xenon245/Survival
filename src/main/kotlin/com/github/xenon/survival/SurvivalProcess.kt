package com.github.xenon.survival

import com.google.common.collect.ImmutableMap
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.GameMode
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.permissions.Permission
import org.bukkit.scheduler.BukkitTask
import org.bukkit.scoreboard.Team
import java.io.File
import java.lang.Double.toHexString
import java.util.*

class SurvivalProcess(
    plugin: TeamSurvivalPlugin,
    val processFile: File,
    val teamFile: File,
    val count: Int? = null,
    load: Boolean = false
) {
    val plugin = plugin

    var onlineTeams = arrayListOf<SurvivalTeam>()

    val surviveTeams
        get() = onlineTeams.filter { it.alive }
    val knockoutTeams
        get() = onlineTeams.filter { !it.alive }

    val players: Map<UUID, SurvivalPlayer>

    val player = arrayListOf<SurvivalPlayer>()

    val onlinePlayers
        get() = Bukkit.getOnlinePlayers().mapNotNull { players[it.uniqueId] }

    private val survivePlayers
        get() = players.values.filter { it.rank == -1 }

    private val knockoutPlayers
        get() = players.values.filter { it.rank >= 0 }

    private val task: BukkitTask

    init {
        if(!load) {
            players = ImmutableMap.copyOf(
                Bukkit.getOnlinePlayers().asSequence().filter {
                    it.gameMode.isDamageable
                }.associate { p ->
                    p.uniqueId to SurvivalPlayer(this, p.uniqueId, p.name).apply {
                        player = p
                    }
                }
            )
            players.values.forEach {
                player.add(it)
            }
            if(count != null) {
                val teamcount = onlinePlayers.count() / count
                for(x in 1..teamcount) {
                    val team =
                        if(Bukkit.getScoreboardManager().mainScoreboard.getTeam(x.toString()) != null) Bukkit.getScoreboardManager().mainScoreboard.getTeam(x.toString())
                        else Bukkit.getScoreboardManager().mainScoreboard.registerNewTeam(x.toString())
                    team?.players?.forEach(team::removePlayer)
                    team?.color = ChatColor.values()[x - 1]
                    for(i in 1..count) {
                        val player1 = player.shuffled()[0]
                        team?.addPlayer(player1.offlinePlayer)
                        player.remove(player1)
                    }
                    team?.players?.forEach {
                        it.player?.gameMode = GameMode.SPECTATOR
                    }
                    team?.players?.first()?.player?.gameMode = GameMode.SURVIVAL
                    val onlineTeam = SurvivalTeam(team!!).apply {
                        currentPlayer = offlinePlayers.first().player
                    }
                    onlineTeams.add(onlineTeam)
                }
            }
        } else {
            val yaml = YamlConfiguration.loadConfiguration(processFile)
            val players = HashMap<UUID, SurvivalPlayer>()
            for((name, value) in yaml.getValues(false).filter { it.value is ConfigurationSection }) {
                val section = value as ConfigurationSection
                val survivalPlayer = SurvivalPlayer(this, UUID.fromString(name), section.getString("name")!!).apply {
                    player = Bukkit.getPlayer(uniqueId)
                    rank = section.getInt("rank")
                }
                players[survivalPlayer.uniqueId] = survivalPlayer
            }
            this.players = ImmutableMap.copyOf(players)

            val teamyaml = YamlConfiguration.loadConfiguration(teamFile)
            val teams = arrayListOf<SurvivalTeam>()
            for((_, value) in teamyaml.getValues(false).filter { it.value is ConfigurationSection }) {
                val section = value as ConfigurationSection
                val team = SurvivalTeam(Bukkit.getScoreboardManager().mainScoreboard.getTeam(section.getString("name")!!)!!).apply {
                    alive = section.getBoolean("alive")
                    currentPlayer = Bukkit.getPlayer(section.getString("currentPlayer")!!)
                    offlinePlayers = team.players.toMutableSet()
                }
                teams += team
            }
            onlineTeams = teams
        }
        Bukkit.getOnlinePlayers().forEach {
            if (!it.isOp && it.uniqueId !in players) {
                it.kick(text("게임 참가자가 아닙니다."))
            }
        }
        plugin.server.run {
            pluginManager.registerEvents(SurvivalListener(this@SurvivalProcess), plugin)
            task = scheduler.runTaskTimer(plugin, this@SurvivalProcess::onUpdate, 0L, 1L)
        }
        onlineTeams.forEach { it.onInitialize() }
    }
    fun stop() {
        HandlerList.unregisterAll(plugin)
        task.cancel()
    }
    fun onUpdate() {
        onlineTeams.forEach {
            it.onUpdate()
            onlineTeams.removeIf { !it.alive }
        }
        if(surviveTeams.count() == 1) {
            stop()
            Bukkit.getOnlinePlayers().forEach {
                it.sendTitle("${surviveTeams.first().team.color}${surviveTeams.first().team.name.toString()}", "")
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "bf cancelall")
        }
    }
    fun team(player: Player): SurvivalTeam {
        var t : SurvivalTeam? = null
        onlineTeams.forEach {
            if(it.team.players.contains(Bukkit.getOfflinePlayer(player.uniqueId))) {
                t = it
            }
        }
        return t!!
    }

    fun player(uuid: UUID) = players[uuid]

    fun player(player: Player) = player(player.uniqueId)

    fun save() {
        val yaml = YamlConfiguration()
        for ((uuid, player) in players) {
            yaml.createSection(uuid.toString()).let { section ->
                section["name"] = player.name
                section["rank"] = player.rank
            }
        }
        yaml.save(processFile.also { it.parentFile.mkdirs() })
        val teamyaml = YamlConfiguration()
        for(team in onlineTeams) {
            teamyaml.createSection(team.team.name).let { section ->
                section["alive"] = team.alive
                section["currentPlayer"] = team.currentPlayer?.name
                section["team"] = team.team
                section["name"] = team.team.name
            }
        }
        teamyaml.save(teamFile.also { it.parentFile.mkdirs() })
    }
}
val GameMode.isDamageable
    get() = this == GameMode.SURVIVAL || this == GameMode.ADVENTURE