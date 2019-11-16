package grondag.mcmd.renderer.mc;

import grondag.mcmd.node.Node;

public interface McMdNodeRendererContext {

    /**
     * @return the writer to use
     */
    McMdContentWriter getWriter();

    /**
     * Render the specified node and its children using the configured renderers. This should be used to render child
     * nodes; be careful not to pass the node that is being rendered, that would result in an endless loop.
     *
     * @param node the node to render
     */
    void render(Node node);
}
