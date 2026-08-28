package io.github.favasur.smoothterrain.util;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.Util;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.stream.Collectors;

public interface BlockStateSerializer {
    static BlockState fromId(int id) {
        BlockState state = Block.BLOCK_STATE_REGISTRY.byId(id);
        if (state == null) {
            throw new IllegalStateException("Unknown blockstate id " + id);
        }
        return state;
    }

    static int toId(BlockState state) {
        int id = Block.BLOCK_STATE_REGISTRY.getId(state);
        if (id < 0) {
            throw new IllegalStateException("Unknown blockstate " + state);
        }
        return id;
    }

    static BlockState fromStringOrNull(String value) {
        try {
            return BlockStateParser.parseForBlock(
                    BuiltInRegistries.BLOCK, value, true).blockState();
        } catch (CommandSyntaxException exception) {
            return null;
        }
    }

    static String toString(BlockState state) {
        String block = ModUtil.platform.getRegistryName(state.getBlock()).toString();
        if (state.isSingletonState()) {
            return block;
        }
        return state.getValues()
                .map(value -> value.property().getName() + "=" + value.valueName())
                .collect(Collectors.joining(",", block + "[", "]"));
    }

    static void writeBlockStatesTo(FriendlyByteBuf buffer, BlockState[] states) {
        buffer.writeVarIntArray(Arrays.stream(states).mapToInt(BlockStateSerializer::toId).toArray());
    }

    static BlockState[] readBlockStatesFrom(FriendlyByteBuf buffer) {
        return Arrays.stream(buffer.readVarIntArray()).mapToObj(BlockStateSerializer::fromId).toArray(BlockState[]::new);
    }
}
