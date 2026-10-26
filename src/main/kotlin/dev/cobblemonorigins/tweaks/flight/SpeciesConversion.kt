package dev.cobblemonorigins.tweaks.flight

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.api.riding.RidingProperties
import com.cobblemon.mod.common.api.riding.RidingStyle
import com.cobblemon.mod.common.api.riding.behaviour.RidingBehaviourSettings
import com.cobblemon.mod.common.api.riding.behaviour.types.air.BirdBehaviour
import com.cobblemon.mod.common.api.riding.behaviour.types.air.BirdSettings
import com.cobblemon.mod.common.api.riding.behaviour.types.air.JetBehaviour
import com.cobblemon.mod.common.api.riding.behaviour.types.air.JetSettings
import com.cobblemon.mod.common.api.riding.behaviour.types.composite.CompositeSettings
import com.cobblemon.mod.common.api.riding.sound.RideSoundSettingsList
import org.slf4j.Logger
import java.util.Collections
import java.util.IdentityHashMap

/**
 * Swaps Cobblemon's bird and jet riding settings for classic flight in the loaded species data, in place, so riding
 * controllers that already hold a species' behaviour map see the change. Runs on the logical server only (see
 * [ClassicFlight.onInitialize]); clients get the converted data through Cobblemon's species sync.
 */
internal object SpeciesConversion {
    private val REPLACED = setOf(BirdBehaviour.KEY, JetBehaviour.KEY)

    // CompositeSettings keeps its two child behaviours in private fields without setters.
    private val COMPOSITE_CHILDREN = listOf("defaultBehaviour", "alternateBehaviour").map { name ->
        CompositeSettings::class.java.getDeclaredField(name).apply { isAccessible = true }
    }

    /** Converts every species and form, then returns how many species/forms use classic flight. */
    fun convertAll(log: Logger): Int {
        val visited = Collections.newSetFromMap(IdentityHashMap<RidingProperties, Boolean>())
        for (species in PokemonSpecies.species) {
            // Forms without their own riding block share the species' properties; convert each object once.
            val ridings = listOf(species.riding) + (species.forms + species.standardForm).map { it.riding }
            for (riding in ridings) {
                if (visited.add(riding)) convert(riding, species.resourceIdentifier.toString(), log)
            }
        }
        return PokemonSpecies.species.sumOf { species ->
            (species.forms + species.standardForm).distinct().count { form ->
                form.riding.behaviours.orEmpty().values.any { it.usesClassicFlight() }
            }
        }
    }

    private fun convert(riding: RidingProperties, speciesId: String, log: Logger) {
        val behaviours = riding.behaviours ?: return
        if (behaviours.values.none { it.needsConversion() }) return
        // Cobblemon exposes the behaviour map as read-only, but builds it mutable (Gson and its network decoder).
        @Suppress("UNCHECKED_CAST")
        val mutable = behaviours as? MutableMap<RidingStyle, RidingBehaviourSettings>
        if (mutable == null) {
            log.warn("Classic flight: riding data of {} is read-only; it keeps Cobblemon's flight", speciesId)
            return
        }
        try {
            mutable.replaceAll { _, settings -> settings.converted(speciesId, log) }
        } catch (e: UnsupportedOperationException) {
            log.warn("Classic flight: riding data of {} is read-only; it keeps Cobblemon's flight", speciesId, e)
        }
    }

    private fun RidingBehaviourSettings.needsConversion(): Boolean = when (this) {
        is CompositeSettings -> COMPOSITE_CHILDREN.any { (it.get(this) as? RidingBehaviourSettings)?.needsConversion() == true }
        else -> key in REPLACED
    }

    private fun RidingBehaviourSettings.converted(speciesId: String, log: Logger): RidingBehaviourSettings = when {
        this is CompositeSettings -> also { composite ->
            for (field in COMPOSITE_CHILDREN) {
                val child = field.get(composite) as? RidingBehaviourSettings ?: continue
                try {
                    field.set(composite, child.converted(speciesId, log))
                } catch (e: ReflectiveOperationException) {
                    log.warn("Classic flight: could not convert a composite ride of {}", speciesId, e)
                }
            }
        }
        key in REPLACED -> ClassicFlightSettings.copyOf(stats, rideSounds())
        else -> this
    }

    private fun RidingBehaviourSettings.rideSounds(): RideSoundSettingsList = when (this) {
        is BirdSettings -> rideSounds
        is JetSettings -> rideSounds
        else -> RideSoundSettingsList()
    }

    private fun RidingBehaviourSettings.usesClassicFlight(): Boolean = when (this) {
        is ClassicFlightSettings -> true
        is CompositeSettings -> COMPOSITE_CHILDREN.any { (it.get(this) as? RidingBehaviourSettings)?.usesClassicFlight() == true }
        else -> false
    }
}
