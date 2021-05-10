package me.steven.indrevstorage.gui.widgets

import io.github.cottonmc.cotton.gui.client.ScreenDrawing
import io.github.cottonmc.cotton.gui.widget.WWidget
import me.steven.indrevstorage.PacketHelper
import me.steven.indrevstorage.api.MappedItemType
import me.steven.indrevstorage.gui.HardDriveRackScreenHandler
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawableHelper
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.util.math.MatrixStack

class WIRDSInventorySlot(private val handler: HardDriveRackScreenHandler, val index: Int) : WWidget() {

    init {
        handler.children.add(this)
    }

    override fun paint(matrices: MatrixStack, x: Int, y: Int, mouseX: Int, mouseY: Int) {
        ScreenDrawing.drawBeveledPanel(matrices, x, y, height, width, -1207959552, 1275068416, -1191182337)
        val (type, invs) = if (handler.mappedTypes.size <= index) MappedItemType.EMPTY else handler.mappedTypes[index]
        val count = invs.sumBy { it[type] }
        val client = MinecraftClient.getInstance()
        client.itemRenderer.renderGuiItemIcon(type.toItemStack(), x + 1, y + 1)
        client.itemRenderer.renderGuiItemOverlay(client.textRenderer, type.toItemStack(), x + 1, y + 1, count.toString())
        if (mouseX >= 0 && mouseY >= 0 && mouseX < width && mouseY < height)
            DrawableHelper.fill(matrices, x + 1, y + 1, x + 17, y + 17, -2130706433)
    }

    override fun onClick(x: Int, y: Int, button: Int) {
        val buf = PacketByteBufs.create()
        buf.writeByte(index)
        buf.writeBoolean(Screen.hasShiftDown())
        ClientPlayNetworking.send(PacketHelper.CLICK_IRDSINV_SLOT, buf)
    }

}