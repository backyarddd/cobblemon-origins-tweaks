package dev.cobblemonorigins.tweaks.flight

import com.cobblemon.mod.common.api.riding.behaviour.RidingBehaviourSettings
import com.cobblemon.mod.common.api.riding.sound.RideSoundSettingsList
import com.cobblemon.mod.common.api.riding.stats.RidingStat
import com.cobblemon.mod.common.util.readRidingStats
import com.cobblemon.mod.common.util.writeRidingStats
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.resources.ResourceLocation

/**
 * Per-species settings for classic flight: the bird/jet ride stats and ride sounds, copied over by
 * [SpeciesConversion] on the server, then synced to clients under our key.
 *
 * Needs a public no-arg constructor: Cobblemon's network decoder (and Gson, for datapacks that use our key directly)
 * instantiate it reflectively.
 */
class ClassicFlightSettings : RidingBehaviourSettings {
    // Deliberately no backing field, so a JSON "key" entry can never override it.
    override val key: ResourceLocation
        get() = ClassicFlightBehaviour.KEY

    override val stats: MutableMap<RidingStat, IntRange> = mutableMapOf()

    var rideSounds: RideSoundSettingsList = RideSoundSettingsList()
        private set

    override fun encode(buffer: RegistryFriendlyByteBuf) {
        buffer.writeRidingStats(stats)
        rideSounds.encode(buffer)
    }

    override fun decode(buffer: RegistryFriendlyByteBuf) {
        stats.putAll(buffer.readRidingStats())
        rideSounds = RideSoundSettingsList.decode(buffer)
    }

    companion object {
        fun copyOf(stats: Map<RidingStat, IntRange>, rideSounds: RideSoundSettingsList): ClassicFlightSettings =
            ClassicFlightSettings().also {
                it.stats.putAll(stats)
                it.rideSounds = rideSounds
            }
    }
}
