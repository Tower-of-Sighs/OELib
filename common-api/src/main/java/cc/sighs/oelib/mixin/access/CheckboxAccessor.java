package cc.sighs.oelib.mixin.access;

import net.minecraft.client.gui.components.Checkbox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Checkbox.class)
public interface CheckboxAccessor {
    @Accessor("selected")
    boolean getSelected();

    @Accessor("selected")
    void setSelected(boolean selected);
}