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
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.gui.CommandSuggestionHelper;
import net.minecraft.client.gui.widget.TextFieldWidget;
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

/**
 * @author Brady
 * @since 10/9/2019
 */
@Mixin(CommandSuggestionHelper.class)
public class MixinCommandSuggestionHelper {

    @Shadow
    @Final
    private TextFieldWidget inputField;

    @Shadow
    @Final
    private List<String> exceptionList;

    @Shadow
    private ParseResults parseResults;

    @Shadow
    private CompletableFuture<Suggestions> suggestionsFuture;

    @Shadow
    private CommandSuggestionHelper.Suggestions suggestions;

    @Shadow
    boolean isApplyingSuggestion;

    @Inject(
            method = "init",
            at = @At("HEAD"),
            cancellable = true
    )
    private void preUpdateSuggestion(CallbackInfo ci) {
        // Anything that is present in the input text before the cursor position
        String prefix = this.inputField.getText().substring(0, Math.min(this.inputField.getText().length(), this.inputField.getCursorPosition()));

        TabCompleteEvent event = new TabCompleteEvent(prefix);
        BaritoneAPI.getProvider().getPrimaryBaritone().getGameEventHandler().onPreTabComplete(event);

        if (event.isCancelled()) {
            ci.cancel();
            return;
        }

        if (event.completions != null) {
            ci.cancel();

            this.parseResults = null; // stop coloring

            if (this.isApplyingSuggestion) { // Supress suggestions update when cycling suggestions.
                return;
            }

            this.inputField.setSuggestion(null); // clear old suggestions
            this.suggestions = null;
            // TODO: Support populating the command usage
            this.exceptionList.clear();

            if (event.completions.length == 0) {
                this.suggestionsFuture = Suggestions.empty();
            } else {
                StringRange range = StringRange.between(prefix.lastIndexOf(" ") + 1, prefix.length()); // if there is no space this starts at 0

                List<Suggestion> suggestionList = Stream.of(event.completions)
                        .map(s -> new Suggestion(range, s))
                        .collect(Collectors.toList());

                Suggestions suggestions = new Suggestions(range, suggestionList);

                this.suggestionsFuture = new CompletableFuture<>();
                this.suggestionsFuture.complete(suggestions);
            }
            ((CommandSuggestionHelper) (Object) this).updateSuggestions(true); // actually populate the suggestions list from the suggestions future
        }
    }
}
