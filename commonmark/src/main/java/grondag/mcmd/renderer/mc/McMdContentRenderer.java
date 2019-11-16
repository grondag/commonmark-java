package grondag.mcmd.renderer.mc;

import java.util.ArrayList;
import java.util.List;

import grondag.mcmd.Extension;
import grondag.mcmd.internal.renderer.NodeRendererMap;
import grondag.mcmd.node.Node;
import grondag.mcmd.renderer.NodeRenderer;
import grondag.mcmd.renderer.Renderer;

public class McMdContentRenderer implements Renderer {

    private final List<McMdContentNodeRendererFactory> nodeRendererFactories;

    private McMdContentRenderer(Builder builder) {
        nodeRendererFactories = new ArrayList<>(builder.nodeRendererFactories.size() + 1);
        nodeRendererFactories.addAll(builder.nodeRendererFactories);
        // Add as last. This means clients can override the rendering of core nodes if they want.
        nodeRendererFactories.add(new McMdContentNodeRendererFactory() {
            @Override
            public NodeRenderer create(McMdNodeRendererContext context) {
                return new CoreMcMdNodeRenderer(context);
            }
        });
    }

    /**
     * Create a new builder for configuring a {@link McMdContentRenderer}.
     *
     * @return a builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void render(Node node, Appendable output) {
        final RendererContext context = new RendererContext(new McMdContentWriter(output));
        context.render(node);
    }

    @Override
    public String render(Node node) {
        final StringBuilder sb = new StringBuilder();
        render(node, sb);
        return sb.toString();
    }

    /**
     * Builder for configuring an {@link McMdContentRenderer}. See methods for default configuration.
     */
    public static class Builder {

        private final List<McMdContentNodeRendererFactory> nodeRendererFactories = new ArrayList<>();

        /**
         * @return the configured {@link McMdContentRenderer}
         */
        public McMdContentRenderer build() {
            return new McMdContentRenderer(this);
        }

        /**
         * Add a factory for instantiating a node renderer (done when rendering). This allows to override the rendering
         * of node types or define rendering for custom node types.
         * <p>
         * If multiple node renderers for the same node type are created, the one from the factory that was added first
         * "wins". (This is how the rendering for core node types can be overridden; the default rendering comes last.)
         *
         * @param nodeRendererFactory the factory for creating a node renderer
         * @return {@code this}
         */
        public Builder nodeRendererFactory(McMdContentNodeRendererFactory nodeRendererFactory) {
            nodeRendererFactories.add(nodeRendererFactory);
            return this;
        }

        /**
         * @param extensions extensions to use on this text content renderer
         * @return {@code this}
         */
        public Builder extensions(Iterable<? extends Extension> extensions) {
            for (final Extension extension : extensions) {
                if (extension instanceof McMdContentRenderer.MarkdownContentRendererExtension) {
                    final McMdContentRenderer.MarkdownContentRendererExtension htmlRendererExtension =
                            (McMdContentRenderer.MarkdownContentRendererExtension) extension;
                    htmlRendererExtension.extend(this);
                }
            }
            return this;
        }
    }

    /**
     * Extension for {@link McMdContentRenderer}.
     */
    public interface MarkdownContentRendererExtension extends Extension {
        void extend(McMdContentRenderer.Builder rendererBuilder);
    }

    private class RendererContext implements McMdNodeRendererContext {
        private final McMdContentWriter markdownContentWriter;
        private final NodeRendererMap nodeRendererMap = new NodeRendererMap();

        private RendererContext(McMdContentWriter textContentWriter) {
            markdownContentWriter = textContentWriter;

            // The first node renderer for a node type "wins".
            for (int i = nodeRendererFactories.size() - 1; i >= 0; i--) {
                final McMdContentNodeRendererFactory nodeRendererFactory = nodeRendererFactories.get(i);
                final NodeRenderer nodeRenderer = nodeRendererFactory.create(this);
                nodeRendererMap.add(nodeRenderer);
            }
        }

        @Override
        public McMdContentWriter getWriter() {
            return markdownContentWriter;
        }

        @Override
        public void render(Node node) {
            nodeRendererMap.render(node);
        }
    }
}
