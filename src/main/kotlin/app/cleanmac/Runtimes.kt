package app.cleanmac

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.util.concurrent.TimeUnit

data class SimRuntime(val id: String, val name: String, val build: String, val size: Long)

/** Simulator runtimes live in system-managed disk images, so they are removed with `xcrun simctl runtime delete`, never by file deletion. */
interface RuntimeManager {
    fun list(): List<SimRuntime>

    /** Deletes the runtime and returns the bytes it occupied. */
    fun delete(runtime: SimRuntime): Long
}

object SimctlRuntimes : RuntimeManager {
    override fun list(): List<SimRuntime> = try {
        parseRuntimes(run("list", "-j"))
    } catch (_: Exception) {
        emptyList()
    }

    override fun delete(runtime: SimRuntime): Long {
        run("delete", runtime.id)
        return runtime.size
    }

    private fun run(vararg args: String): String {
        val p = ProcessBuilder(listOf("xcrun", "simctl", "runtime") + args).redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText()
        if (!p.waitFor(10, TimeUnit.MINUTES)) { p.destroyForcibly(); error("simctl timed out") }
        if (p.exitValue() != 0) error(out.trim().ifEmpty { "simctl exited with ${p.exitValue()}" })
        return out
    }
}

/** Parses `simctl runtime list -j`, keeping only deletable runtimes. */
fun parseRuntimes(json: String): List<SimRuntime> =
    Json.parseToJsonElement(json).jsonObject.values.mapNotNull { e ->
        val o = e as? JsonObject ?: return@mapNotNull null
        if (o["deletable"]?.jsonPrimitive?.boolean != true) return@mapNotNull null
        val platform = o["runtimeIdentifier"]?.jsonPrimitive?.content?.substringAfter("SimRuntime.")?.substringBefore("-")
            ?: return@mapNotNull null
        SimRuntime(
            id = o["identifier"]!!.jsonPrimitive.content,
            name = "$platform ${o["version"]?.jsonPrimitive?.content ?: "?"}",
            build = o["build"]?.jsonPrimitive?.content ?: "",
            size = o["sizeBytes"]?.jsonPrimitive?.long ?: 0,
        )
    }.sortedByDescending { it.size }
