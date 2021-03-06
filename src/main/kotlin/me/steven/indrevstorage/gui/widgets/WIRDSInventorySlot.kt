package me.steven.indrevstorage.gui.widgets

import io.github.cottonmc.cotton.gui.client.ScreenDrawing
import io.github.cottonmc.cotton.gui.widget.WWidget
import me.steven.indrevstorage.gui.AbstractTerminalScreenHandler
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawableHelper
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.util.math.MatrixStack

class WIRDSInventorySlot(private val handler: AbstractTerminalScreenHandler, val index: Int) : WWidget() {

    override fun paint(matrices: MatrixStack, x: Int, y: Int, mouseX: Int, mouseY: Int) {
        ScreenDrawing.drawBeveledPanel(matrices, x, y, height, width, -1207959552, 1275068416, -1191182337)
        renderItem(matrices, x, y)
        if (mouseX >= 0 && mouseY >= 0 && mouseX < width && mouseY < height)
            DrawableHelper.fill(matrices, x + 1, y + 1, x + 17, y + 17, -2130706433)
    }

    private fun renderItem(matrices: MatrixStack, x: Int, y: Int) {
        val (type, count) = if (handler.connection.clientCache.size <= index) return else handler.connection.clientCache[index]
        val client = MinecraftClient.getInstance()
        client.itemRenderer.renderGuiItemIcon(type.toItemStack(), x + 1, y + 1)
        val renderer = client.textRenderer
        client.itemRenderer.renderGuiItemOverlay(renderer, type.toItemStack(), x + 1, y + 1)

        if (count > 1) {
            matrices.push()

            matrices.translate(0.0, 0.0, client.itemRenderer.zOffset + 200.0)
            matrices.scale(0.5f, 0.5f, 0.5f)
            val immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().buffer)
            renderer.draw(count.toString(), (x * 2 + 16) + 18f - renderer.getWidth(count.toString()), (y * 2 + 16) + 10f , 16777215, true, matrices.peek().model, immediate, false, 0, 15728880)
            immediate.draw()
            matrices.pop()
        }

    }

    override fun onClick(x: Int, y: Int, button: Int) {
        handler.terminalSlotClickAction(index, button)
    }

}