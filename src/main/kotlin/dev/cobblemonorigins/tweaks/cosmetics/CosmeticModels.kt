package dev.cobblemonorigins.tweaks.cosmetics

import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor

/**
 * Hat and back item models: every `assets/origins/models/cosmetic/<id>.json` of the loaded resource packs, baked as an
 * extra model `origins:cosmetic/<id>`. Rescanned on every resource reload, because the server pack that ships them is
 * applied after the client has joined.
 */
object CosmeticModels {
    private const val NAMESPACE = "origins"
    private const val DIRECTORY = "models/cosmetic"

    private val LOGGER: Logger = LoggerFactory.getLogger("Cobblemon Origins Tweaks")

    /** Ids already reported missing since the last reload. Written by the render thread, reset by the model loader. */
    private val reportedMissing: MutableSet<String> = ConcurrentHashMap.newKeySet()

    fun register() {
        PreparableModelLoadingPlugin.register(::scan) { modelIds, context ->
            reportedMissing.clear()
            context.addModels(modelIds)
        }
    }

    private fun scan(resources: ResourceManager, executor: Executor): CompletableFuture<List<ResourceLocation>> =
        CompletableFuture.supplyAsync({
            resources.listResources(DIRECTORY) { it.namespace == NAMESPACE && it.path.endsWith(".json") }.keys.map {
                // models/cosmetic/<id>.json -> origins:cosmetic/<id>, the id the model loader resolves back to the file
                ResourceLocation.fromNamespaceAndPath(NAMESPACE, it.path.removePrefix("models/").removeSuffix(".json"))
            }
        }, executor)

    /**
     * Baked model of cosmetic [id], or null (logged once per id per reload) if no loaded pack has a usable model for it.
     * Only valid on the render thread after the model manager has finished reloading, i.e. while rendering.
     */
    fun get(id: String): BakedModel? {
        val manager = Minecraft.getInstance().modelManager
        // Cosmetic ids come from the server; one that is not a valid resource path can have no model.
        val model = ResourceLocation.tryBuild(NAMESPACE, "cosmetic/$id")?.let { manager.getModel(it) }
        // A file that exists but failed to parse is baked as the missing model; drawing that would show a purple cube.
        if (model != null && model !== manager.missingModel) return model
        if (reportedMissing.add(id)) {
            LOGGER.warn("Cosmetics: no usable model {}:cosmetic/{} in the loaded resource packs, not drawing it", NAMESPACE, id)
        }
        return null
    }
}
