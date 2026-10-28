package dev.cobblemonorigins.tweaks.mixin.cosmetics;

import com.cobblemon.mod.common.client.battle.ClientBallDisplay;
import com.cobblemon.mod.common.client.entity.PokemonClientDelegate;
import com.cobblemon.mod.common.client.render.player.RecallArmAnimationTracker;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokeball.PokeBall;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.cobblemonorigins.tweaks.cosmetics.ClientState;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// The ball in the owner's hand during a recall: RecallArmAnimationTracker.start builds its own
// ClientBallDisplay(ball, emptySet()) that lives for the whole arm animation and is drawn for first person, F5 and other
// viewers. Nothing else ever sets its aspects, so the skin aspect goes in once, right after start, while the Pokemon
// entity (whose network id keys the ball skin) is still loaded. No skin known (singleplayer, no server payload, hidden
// cosmetics): withBallSkin returns the aspects unchanged, so the default ball draws as before.
// onSyncedDataUpdated and the INVOKE target are Cobblemon names with Cobblemon and Java types only, hence remap = false.
@Mixin(value = PokemonClientDelegate.class, remap = false)
public abstract class PokemonClientDelegateMixin {
    @WrapOperation(
        method = "onSyncedDataUpdated",
        at = @At(
            value = "INVOKE",
            target = "Lcom/cobblemon/mod/common/client/render/player/RecallArmAnimationTracker;start(Ljava/util/UUID;Lcom/cobblemon/mod/common/pokeball/PokeBall;)V"
        ),
        require = 1
    )
    private void origins_tweaks$skinRecallBall(RecallArmAnimationTracker tracker, UUID owner, PokeBall ball, Operation<Void> original) {
        original.call(tracker, owner, ball);
        RecallArmAnimationTracker.RecallAnim anim = tracker.get(owner);
        PokemonEntity entity = ((PokemonClientDelegate) (Object) this).getCurrentEntity();
        if (anim == null || entity == null) return;
        ClientBallDisplay display = anim.getBallState();
        display.setCurrentAspects(ClientState.withBallSkin(entity.getId(), display.getCurrentAspects()));
    }
}
