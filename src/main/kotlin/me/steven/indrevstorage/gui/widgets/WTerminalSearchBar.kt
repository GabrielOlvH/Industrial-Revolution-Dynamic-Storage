package me.steven.indrevstorage.gui.widgets

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import io.github.cottonmc.cotton.gui.client.Scissors
import io.github.cottonmc.cotton.gui.client.ScreenDrawing
import io.github.cottonmc.cotton.gui.widget.WTextField
import me.steven.indrevstorage.utils.SearchTerm
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.LiteralText
import org.lwjgl.opengl.GL11

class WTerminalSearchBar(val countProvider: () -> Int) : WTextField(LiteralText("Search...")) {
    override fun setSize(x: Int, y: Int) {
        this.width = x
        this.height = y
    }

    override fun renderTextField(matrices: MatrixStack?, x: Int, y: Int) {
        val renderer = MinecraftClient.getInstance().textRenderer

        ScreenDrawing.drawBeveledPanel(matrices, x - 1, y - 1, width + 2, height + 2, 0xFF373737.toInt(), -1, 0xFFffffff.toInt())
        ScreenDrawing.drawBeveledPanel(matrices, x, y, width, height, 0xFFe0ca9f.toInt(), 0xFFa09172.toInt(), 0xFF544c3b.toInt())

        val textColor = if (editable) 0xFFFFFFFF.toInt() else uneditableColor

        val trimText = renderer.trimToWidth(text, width - OFFSET_X_TEXT)

        val selection = select != -1
        val focused = this.isFocused

        val textX = x + OFFSET_X_TEXT

        val textY = y + (height - 8) / 2

        var adjustedCursor = cursor
        if (adjustedCursor > trimText.length) {
            adjustedCursor = trimText.length
        }

        var preCursorAdvance = textX
        if (trimText.isNotEmpty()) {
            val width = renderer.getWidth(trimText.substring(0, adjustedCursor))
            preCursorAdvance += width
            Scissors.push(textX, textY, width, height)
            val string2 = SearchTerm.applyFormatting(trimText)
            renderer.drawWithShadow(matrices, string2, textX.toFloat(), textY.toFloat(), textColor)
            Scissors.pop()
        }

        if (adjustedCursor < trimText.length) {
            val string2 = SearchTerm.applyFormatting(trimText)
            renderer.drawWithShadow(matrices, string2, textX.toFloat(), textY.toFloat(), textColor)
        }


        if (text.isEmpty() && suggestion != null) {
            renderer.drawWithShadow(matrices, suggestion, textX.toFloat(), textY.toFloat(), 0xFFAAAAAA.toInt())
        }
        if (focused && !selection) {
            if (adjustedCursor < trimText.length) {
                ScreenDrawing.coloredRect(matrices, preCursorAdvance - 1, textY - 2, 1, 12, -0x2f2f30)
            } else {
                renderer.drawWithShadow(matrices, "_", preCursorAdvance.toFloat(), textY.toFloat(), textColor)
            }
        }

        if (selection) {
            var a = getCaretOffset(text, cursor)
            var b = getCaretOffset(text, select)
            if (b < a) {
                val tmp = b
                b = a
                a = tmp
            }
            invertedRect(textX + a - 1, textY - 1, (b - a).coerceAtMost(width - OFFSET_X_TEXT), 12)
        }

        val countString = countProvider().toString()
        renderer.drawWithShadow(matrices, countString, x + width.toFloat() - renderer.getWidth(countString) - OFFSET_X_TEXT , y + (height - 8f) / 2, 0xCCCCCC)
    }

    private fun invertedRect(x: Int, y: Int, width: Int, height: Int) {
        val tessellator = Tessellator.getInstance()
        val bufferBuilder = tessellator.buffer
        RenderSystem.color4f(0.0f, 0.0f, 255.0f, 255.0f)
        RenderSystem.disableTexture()
        RenderSystem.enableColorLogicOp()
        RenderSystem.logicOp(GlStateManager.LogicOp.OR_REVERSE)
        bufferBuilder.begin(GL11.GL_QUADS, VertexFormats.POSITION)
        bufferBuilder.vertex(x.toDouble(), (y + height).toDouble(), 0.0).next()
        bufferBuilder.vertex((x + width).toDouble(), (y + height).toDouble(), 0.0).next()
        bufferBuilder.vertex((x + width).toDouble(), y.toDouble(), 0.0).next()
        bufferBuilder.vertex(x.toDouble(), y.toDouble(), 0.0).next()
        tessellator.draw()
        RenderSystem.disableColorLogicOp()
        RenderSystem.enableTexture()
    }
}