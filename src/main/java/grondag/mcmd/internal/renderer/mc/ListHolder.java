package grondag.mcmd.internal.renderer.mc;

import grondag.mcmd.McMdRenderer;

public abstract class ListHolder {
    private final ListHolder parent;
    public final String indentOn;
    public final String indentOff;

    ListHolder(ListHolder parent) {
        this.parent = parent;

        if (parent != null) {
            indentOn = parent.indentOn + McMdRenderer.ESC_INDENT_PLUS;
            indentOff = parent.indentOff + McMdRenderer.ESC_INDENT_MINUS;
        } else {
            indentOn = "";
            indentOff = "";
        }
    }

    public ListHolder getParent() {
        return parent;
    }

}
