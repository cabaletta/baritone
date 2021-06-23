/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.launch.mixins;

import baritone.api.BaritoneAPI;
import baritone.api.event.events.TabCompleteEvent;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;

/**
 * @author Brady
 * @since 10/9/2019
 */
@Mixin(CommandSuggestions.class)
public class MixinCommandSuggestionHelper {

    @Shadow
    @Final
    EditBox input;

    @Shadow
    @Final
    private List<String> commandUsage;

    @Shadow
    private CompletableFuture<Suggestions> pendingSuggestions;

    @Inject(
            method = "updateCommandInfo",
            at = @At("HEAD"),
            cancellable = true
    )
    private void preUpdateSuggestion(CallbackInfo ci) {
        // Anything that is present in the input text before the cursor position
        String prefix = this.input.getValue().substring(0, Math.min(this.input.getValue().length(), this.input.getCursorPosition()));

        TabCompleteEvent event = new TabCompleteEvent(prefix);
        BaritoneAPI.getProvider().getPrimaryBaritone().getGameEventHandler().onPreTabComplete(event);

        if (event.isCancelled()) {
            ci.cancel();
            return;
        }

        if (event.completions != null) {
            ci.cancel();

            // TODO: Support populating the command usage
            this.commandUsage.clear();

            if (event.completions.length == 0) {
                this.pendingSuggestions = Suggestions.empty();
            } else {
                int offset = this.input.getValue().endsWith(" ")
                        ? this.input.getCursorPosition()
                        : this.input.getValue().lastIndexOf(" ") + 1; // If there is no space this is still 0 haha yes

                List<Suggestion> suggestionList = Stream.of(event.completions)
                        .map(s -> new Suggestion(StringRange.between(offset, offset + s.length()), s))
                        .collect(Collectors.toList());

                Suggestions suggestions = new Suggestions(
                        StringRange.between(offset, offset + suggestionList.stream().mapToInt(s -> s.getText().length()).max().orElse(0)),
                        suggestionList);

                this.pendingSuggestions = new CompletableFuture<>();
                this.pendingSuggestions.complete(suggestions);
            }
        }
    }
}
