/*
 * License (MIT)
 *
 * Copyright (c) 2014 Granite Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the
 * Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.granitepowered.granite;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import org.granitepowered.granite.impl.block.GraniteBlockType;
import org.granitepowered.granite.impl.item.GraniteItemBlock;
import org.granitepowered.granite.impl.item.GraniteItemType;
import org.granitepowered.granite.mappings.Mappings;
import org.granitepowered.granite.utils.ReflectionUtils;
import org.spongepowered.api.GameRegistry;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.item.ItemBlock;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

public class GraniteGameRegistry implements GameRegistry {

    static Map<String, BlockType> blockTypes = Maps.newHashMap();
    static Map<String, ItemType> itemTypes = Maps.newHashMap();
    static Map<String, ItemType> itemBlocks = Maps.newHashMap();

    public static void register() {
        registerBlocks();
        registerItems();
    }

    private static void registerBlocks() {
        Granite.instance.getLogger().info("Registering Blocks");
        Class minecraftClazz = Mappings.getClass("Blocks");

        for (Field field : BlockTypes.class.getDeclaredFields()) {
            field.setAccessible(true);

            try {
                Field modifiers = Field.class.getDeclaredField("modifiers");
                modifiers.setAccessible(true);
                modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }

            String name = field.getName().toLowerCase();
            BlockType block = null;
            try {
                block = new GraniteBlockType(Mappings.getField(minecraftClazz, name).getDeclaringClass());
                field.set(null, block);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            blockTypes.put(name, block);
        }
    }

    private static void registerItems() {
        Granite.instance.getLogger().info("Registering Items");
        Class minecraftClazz = ReflectionUtils.getClassByName("Items");

        for (Field field : ItemTypes.class.getDeclaredFields()) {
            field.setAccessible(true);

            try {
                Field modifiers = Field.class.getDeclaredField("modifiers");
                modifiers.setAccessible(true);
                modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }

            String name = field.getName().toLowerCase();
            if (field.getType().equals(ItemType.class)) {
                ItemType item = null;
                try {
                    item = new GraniteItemType(Mappings.getField(minecraftClazz, name).getDeclaringClass());
                    field.set(null, item);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                itemTypes.put(name, item);
            }
        }
    }

    private static void registerItemBlocks() {
        Granite.instance.getLogger().info("Registering ItemBlocks");
        Class minecraftClazz = ReflectionUtils.getClassByName("Blocks");

        for (Field field : ItemTypes.class.getDeclaredFields()) {
            field.setAccessible(true);

            try {
                Field modifiers = Field.class.getDeclaredField("modifiers");
                modifiers.setAccessible(true);
                modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }

            String name = field.getName().toLowerCase();
            if (field.getType().equals(ItemBlock.class)) {
                ItemBlock item = null;
                try {
                    item = new GraniteItemBlock(Mappings.getField(minecraftClazz, name).getDeclaringClass());
                    field.set(null, item);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                itemBlocks.put(name, item);
            }
        }
    }

    @Override
    public Optional<BlockType> getBlock(String s) {
        return Optional.fromNullable(blockTypes.get(s));
    }

    @Override
    public List<BlockType> getBlocks() {
        return (List<BlockType>) blockTypes.values();
    }

    @Override
    public Optional<ItemType> getItem(String s) {
        return Optional.fromNullable(itemTypes.get(s));
    }

    @Override
    public List<ItemType> getItems() {
        return (List<ItemType>) itemTypes.values();
    }
}
