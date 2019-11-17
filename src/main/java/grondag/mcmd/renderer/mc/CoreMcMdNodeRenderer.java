package grondag.mcmd.renderer.mc;

import static grondag.mcmd.McMdRenderer.ESC_BOLD;
import static grondag.mcmd.McMdRenderer.ESC_BOLD_OFF;
import static grondag.mcmd.McMdRenderer.ESC_HALF_NEWLINE;
import static grondag.mcmd.McMdRenderer.ESC_ITALIC;
import static grondag.mcmd.McMdRenderer.ESC_ITALIC_OFF;
import static grondag.mcmd.McMdRenderer.ESC_OBFUSCATE;
import static grondag.mcmd.McMdRenderer.ESC_OBFUSCATE_OFF;
import static grondag.mcmd.McMdRenderer.ESC_STRIKETHROUGH;
import static grondag.mcmd.McMdRenderer.ESC_STRIKETHROUGH_OFF;
import static grondag.mcmd.McMdRenderer.ESC_UNDERLINE;
import static grondag.mcmd.McMdRenderer.ESC_UNDERLINE_OFF;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import grondag.mcmd.McMdRenderer;
import grondag.mcmd.internal.renderer.mc.BulletListHolder;
import grondag.mcmd.internal.renderer.mc.ListHolder;
import grondag.mcmd.internal.renderer.mc.OrderedListHolder;
import grondag.mcmd.node.AbstractVisitor;
import grondag.mcmd.node.BlockQuote;
import grondag.mcmd.node.BulletList;
import grondag.mcmd.node.Code;
import grondag.mcmd.node.Document;
import grondag.mcmd.node.Emphasis;
import grondag.mcmd.node.FencedCodeBlock;
import grondag.mcmd.node.HardLineBreak;
import grondag.mcmd.node.Heading;
import grondag.mcmd.node.HtmlBlock;
import grondag.mcmd.node.HtmlInline;
import grondag.mcmd.node.Image;
import grondag.mcmd.node.IndentedCodeBlock;
import grondag.mcmd.node.Link;
import grondag.mcmd.node.ListItem;
import grondag.mcmd.node.Node;
import grondag.mcmd.node.Obfuscate;
import grondag.mcmd.node.OrderedList;
import grondag.mcmd.node.Paragraph;
import grondag.mcmd.node.SoftLineBreak;
import grondag.mcmd.node.Strikethrough;
import grondag.mcmd.node.StrongEmphasis;
import grondag.mcmd.node.Text;
import grondag.mcmd.node.ThematicBreak;
import grondag.mcmd.node.Underline;
import grondag.mcmd.renderer.NodeRenderer;

/**
 * The node renderer that renders all the core nodes (comes last in the order of node renderers).
 */
public class CoreMcMdNodeRenderer extends AbstractVisitor implements NodeRenderer {

    protected final McMdNodeRendererContext context;
    private final McMdContentWriter textContent;

    private ListHolder listHolder;

    public CoreMcMdNodeRenderer(McMdNodeRendererContext context) {
        this.context = context;
        textContent = context.getWriter();
    }

    @Override
    public Set<Class<? extends Node>> getNodeTypes() {
        return new HashSet<>(Arrays.asList(
                Document.class,
                Heading.class,
                Paragraph.class,
                BlockQuote.class,
                BulletList.class,
                FencedCodeBlock.class,
                HtmlBlock.class,
                ThematicBreak.class,
                IndentedCodeBlock.class,
                Link.class,
                ListItem.class,
                OrderedList.class,
                Image.class,
                Emphasis.class,
                Strikethrough.class,
                Underline.class,
                Obfuscate.class,
                StrongEmphasis.class,
                Text.class,
                Code.class,
                HtmlInline.class,
                SoftLineBreak.class,
                HardLineBreak.class
                ));
    }

    @Override
    public void render(Node node) {
        node.accept(this);
    }

    @Override
    public void visit(Document document) {
        // No rendering itself
        visitChildren(document);
    }

    @Override
    public void visit(BlockQuote blockQuote) {
        textContent.write('«');
        visitChildren(blockQuote);
        textContent.write('»');
        textContent.write(ESC_HALF_NEWLINE);
    }

    @Override
    public void visit(BulletList bulletList) {
        //        if (listHolder != null) {
        //            writeEndOfLine();
        //        }
        listHolder = new BulletListHolder(listHolder, bulletList);
        visitChildren(bulletList);
        textContent.write(ESC_HALF_NEWLINE);
        if (listHolder.getParent() != null) {
            listHolder = listHolder.getParent();
        } else {
            listHolder = null;
        }
    }

    @Override
    public void visit(Code code) {
        textContent.write('\"');
        textContent.write(code.getLiteral());
        textContent.write('\"');
    }

    @Override
    public void visit(FencedCodeBlock fencedCodeBlock) {
        textContent.write(fencedCodeBlock.getLiteral());
    }

    @Override
    public void visit(HardLineBreak hardLineBreak) {
        textContent.write(ESC_HALF_NEWLINE);
    }

    @Override
    public void visit(Heading heading) {
        visitChildren(heading);
        textContent.write(ESC_HALF_NEWLINE);
    }

    @Override
    public void visit(ThematicBreak thematicBreak) {
        textContent.write("***");
        textContent.write(ESC_HALF_NEWLINE);
    }

    @Override
    public void visit(HtmlInline htmlInline) {
        textContent.write(htmlInline.getLiteral());
    }

    @Override
    public void visit(HtmlBlock htmlBlock) {
        textContent.write(htmlBlock.getLiteral());
    }

    @Override
    public void visit(Image image) {
        writeLink(image, image.getTitle(), image.getDestination());
    }

    @Override
    public void visit(IndentedCodeBlock indentedCodeBlock) {
        textContent.write(indentedCodeBlock.getLiteral());
    }

    @Override
    public void visit(Link link) {
        writeLink(link, link.getTitle(), link.getDestination());
    }

    @Override
    public void visit(ListItem listItem) {
        if (listHolder != null && listHolder instanceof OrderedListHolder) {
            final OrderedListHolder orderedListHolder = (OrderedListHolder) listHolder;
            textContent.write(orderedListHolder.indentOn + orderedListHolder.getCounter() + orderedListHolder.getDelimiter() + McMdRenderer.ESC_INDENT_PLUS + McMdRenderer.ESC_ALIGN_TO_INDENT);
            visitChildren(listItem);
            textContent.write(orderedListHolder.indentOff + McMdRenderer.ESC_INDENT_MINUS);
            orderedListHolder.increaseCounter();
        } else if (listHolder != null && listHolder instanceof BulletListHolder) {
            final BulletListHolder bulletListHolder = (BulletListHolder) listHolder;
            textContent.write(bulletListHolder.indentOn + bulletListHolder.getMarker() + McMdRenderer.ESC_INDENT_PLUS + McMdRenderer.ESC_ALIGN_TO_INDENT);
            visitChildren(listItem);
            textContent.write(bulletListHolder.indentOff + McMdRenderer.ESC_INDENT_MINUS);
        }

        if (listItem.getNext() != null) {
            textContent.write(ESC_HALF_NEWLINE);
        }
    }

    @Override
    public void visit(OrderedList orderedList) {
        //        if (listHolder != null) {
        //            writeEndOfLine();
        //        }

        listHolder = new OrderedListHolder(listHolder, orderedList);
        visitChildren(orderedList);
        textContent.write(ESC_HALF_NEWLINE);
        if (listHolder.getParent() != null) {
            listHolder = listHolder.getParent();
        } else {
            listHolder = null;
        }
    }

    @Override
    public void visit(Paragraph paragraph) {
        visitChildren(paragraph);
        // Add "end of line" only if its "root paragraph.
        if (paragraph.getParent() == null || paragraph.getParent() instanceof Document) {
            textContent.write(ESC_HALF_NEWLINE);
        }
    }

    @Override
    public void visit(SoftLineBreak softLineBreak) {
        textContent.whitespace();
    }

    @Override
    public void visit(Text text) {
        textContent.writeStripped(text.getLiteral());
    }

    @Override
    public void visit(Emphasis emphasis) {
        textContent.write(ESC_ITALIC);
        visitChildren(emphasis);
        textContent.write(ESC_ITALIC_OFF);
    }

    @Override
    public void visit(StrongEmphasis strongEmphasis) {
        textContent.write(ESC_BOLD);
        visitChildren(strongEmphasis);
        textContent.write(ESC_BOLD_OFF);
    }

    @Override
    public void visit(Strikethrough strikethrough) {
        textContent.write(ESC_STRIKETHROUGH);
        visitChildren(strikethrough);
        textContent.write(ESC_STRIKETHROUGH_OFF);
    }

    @Override
    public void visit(Underline underline) {
        textContent.write(ESC_UNDERLINE);
        visitChildren(underline);
        textContent.write(ESC_UNDERLINE_OFF);
    }

    @Override
    public void visit(Obfuscate obfuscate) {
        textContent.write(ESC_OBFUSCATE);
        visitChildren(obfuscate);
        textContent.write(ESC_OBFUSCATE_OFF);
    }

    @Override
    protected void visitChildren(Node parent) {
        Node node = parent.getFirstChild();
        while (node != null) {
            final Node next = node.getNext();
            context.render(node);
            node = next;
        }
    }

    private void writeLink(Node node, String title, String destination) {
        final boolean hasChild = node.getFirstChild() != null;
        final boolean hasTitle = title != null && !title.equals(destination);
        final boolean hasDestination = destination != null && !destination.equals("");

        if (hasChild) {
            textContent.write('"');
            visitChildren(node);
            textContent.write('"');
            if (hasTitle || hasDestination) {
                textContent.whitespace();
                textContent.write('(');
            }
        }

        if (hasTitle) {
            textContent.write(title);
            if (hasDestination) {
                textContent.colon();
                textContent.whitespace();
            }
        }

        if (hasDestination) {
            textContent.write(destination);
        }

        if (hasChild && (hasTitle || hasDestination)) {
            textContent.write(')');
        }
    }
}
