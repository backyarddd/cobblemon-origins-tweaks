package dev.cobblemonorigins.tweaks.flight

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.api.riding.behaviour.RidingBehaviours
import com.cobblemon.mod.common.util.adapters.RidingBehaviourSettingsAdapter
import net.fabricmc.api.ModInitializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object ClassicFlight : ModInitializer {
    const val MOD_ID = "cobblemon_origins_tweaks"

    private val LOGGER: Logger = LoggerFactory.getLogger("Cobblemon Origins Tweaks")

    override fun onInitialize() {
        RidingBehaviours.register(ClassicFlightBehaviour.KEY, ClassicFlightBehaviour)
        // Only our own key: synced riding data is decoded by key, while bird/jet keys from a server without this mod
        // must keep decoding as Cobblemon's own settings so a modded client can still join it.
        RidingBehaviourSettingsAdapter.types[ClassicFlightBehaviour.KEY] = ClassicFlightSettings::class.java
        LOGGER.info("Classic flight: replaced bird/jet flight")

        // Cobblemon emits this only on the logical server, once per server start (species are not reloadable, so
        // /reload keeps the converted data): server data reload -> SpeciesAdditions -> species initialised -> this,
        // on the loading thread before the server is ready, i.e. before species are synced to joining players.
        // A client that receives species from a server's sync packet only calls PokemonSpecies.reload, which never
        // emits it, so client-side sync never converts anything.
        PokemonSpecies.observable.subscribe {
            // Never let a failure escape into Cobblemon's emitter and skip its other subscribers.
            runCatching { SpeciesConversion.convertAll(LOGGER) }
                .onSuccess { LOGGER.info("Classic flight: {} species/forms now use classic flight", it) }
                .onFailure { LOGGER.error("Classic flight: could not convert bird/jet flight", it) }
        }
    }
}
