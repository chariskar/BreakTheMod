/*
 * This file is part of BreakTheMod.
 *
 * BreakTheMod is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BreakTheMod is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with <Your Project Name>. If not, see <https://www.gnu.org/licenses/>.
 */
package breakthemod;

import breakthemod.commands.*;
import breakthemod.utils.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// TODO: Retire HudRenderCallback for the newer version of the api (i forgot the name)
public class BreakTheMod implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("breakthemod");

    @Override
    public void onInitializeClient() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            // Register commands
            nearby.register();
            lastSeen.register();
            locate.register();
            coords.register();
            discord.register();
            staff.register();
            townless.register();
            whereIs.register();
            GoTo.register();
            help.register();
            friends.register();

            render Render = new render();
            // Register a client tick event to call the update method regularly
            if (client.getGameVersion().equals("1.21.4")){
                HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
                    Render.renderOverlay(drawContext, MinecraftClient.getInstance());
                });
            }
        } else {
            LOGGER.error("Minecraft client instance is null, cannot initialize commands.");
        }
    }

}
