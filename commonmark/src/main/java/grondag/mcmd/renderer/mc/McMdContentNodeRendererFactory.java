package grondag.mcmd.renderer.mc;

import grondag.mcmd.renderer.NodeRenderer;

/**
 * Factory for instantiating new node renderers when rendering is done.
 */
public interface McMdContentNodeRendererFactory {

    /**
     * Create a new node renderer for the specified rendering context.
     *
     * @param context the context for rendering (normally passed on to the node renderer)
     * @return a node renderer
     */
    NodeRenderer create(McMdNodeRendererContext context);
}
