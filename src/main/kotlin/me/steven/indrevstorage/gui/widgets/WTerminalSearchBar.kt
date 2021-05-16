package me.steven.indrevstorage.gui.widgets

import io.github.cottonmc.cotton.gui.widget.WTextField
import net.minecraft.text.LiteralText

class WTerminalSearchBar: WTextField(LiteralText("Search...")) {
    override fun setSize(x: Int, y: Int) {
        this.width = x
        this.height = y
    }
}