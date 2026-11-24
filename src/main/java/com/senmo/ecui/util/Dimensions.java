package com.senmo.ecui.util;

import com.senmo.ecui.Lang;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class Dimensions {
    private Dimensions() {}

    public static String of(ResourceKey<Level> dim) {
        boolean zh = Lang.current().chinese();
        if (dim.equals(Level.OVERWORLD)) {
            return zh ? "主世界" : "Overworld";
        }
        if (dim.equals(Level.NETHER)) {
            return zh ? "下界" : "Nether";
        }
        if (dim.equals(Level.END)) {
            return zh ? "末地" : "The End";
        }
        Identifier id = dim.identifier();
        return id.getNamespace().equals("minecraft") ? id.getPath() : id.toString();
    }
}
