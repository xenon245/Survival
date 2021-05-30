package com.github.xenon.survival

import com.github.monun.kommand.argument.integer
import com.github.monun.kommand.kommand
import com.github.monun.kommand.sendFeedback
import net.kyori.adventure.text.Component.text
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class TeamSurvivalPlugin : JavaPlugin() {
    private lateinit var processFile: File
    private var process: SurvivalProcess? = null
    private lateinit var teamFile: File
    override fun onEnable() {
        dataFolder.mkdirs()
        processFile = File(File(dataFolder, "A"), "process.yml").also { it.mkdirs() }
        teamFile = File(File(dataFolder, "A"), "team.yml").also { it.mkdirs() }
        setupKommands()
    }

    override fun onDisable() {
        process?.save()
    }
    private fun setupKommands() = kommand {
        register("svl") {
            then("start") {
                then("A") {
                    then("count" to integer(1, 100)) {
                        executes {
                            require(process == null) { "Process is already running" }
                            val count = it.parseArgument<Int>("count")
                            it.sender.sendFeedback { text("게임 시작!") }
                            process = SurvivalProcess(this@TeamSurvivalPlugin, processFile, teamFile, count)
                        }
                    }
                }
            }
            then("stop") {
                then("A") {
                    executes {
                        process?.stop()
                        process = null
                        processFile.delete()
                        teamFile.delete()
                        it.sender.sendFeedback { text("전투 종료") }
                    }
                }
            }
            then("load") {
                then("A") {
                    executes {
                        require(process == null) { "Process is already running" }
                        require(processFile.exists()) { "Process file not exists" }
                        process = SurvivalProcess(this@TeamSurvivalPlugin, processFile, teamFile, 1, true)
                    }
                }
            }
        }
    }
}