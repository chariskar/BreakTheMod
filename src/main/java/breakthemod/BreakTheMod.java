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
import breakthemod.commands.economy.calculateGold;
import breakthemod.commands.economy.calculateStacks;
import breakthemod.utils.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BreakTheMod implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("breakthemod");

    @Override
    public void onInitializeClient() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null)  LOGGER.error("Minecraft client instance is null, cannot initialize commands.");
        // Register commands
        registerCommands(
                new coords(),
                new friends(),
                new GoTo(),
                new nationLocation(),
                new nationpop(),
                new staff(),
                new whereIs(),
                new townless(),
                new discord(),
                new lastSeen(),
                new help(),
                new nearby(),
                new locate(),
                new calculateGold(),
                new calculateStacks()
        );
        render Render = new render();
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            Render.renderOverlay(drawContext, MinecraftClient.getInstance());
        });

    }

    public void registerCommands(Command ...commands){
        for (Command command:commands){
            command.register();
        }
    }
}
