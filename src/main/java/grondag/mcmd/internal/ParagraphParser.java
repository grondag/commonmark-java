package grondag.mcmd.internal;

import java.util.List;

import grondag.mcmd.node.Block;
import grondag.mcmd.node.LinkReferenceDefinition;
import grondag.mcmd.node.Paragraph;
import grondag.mcmd.parser.InlineParser;
import grondag.mcmd.parser.block.AbstractBlockParser;
import grondag.mcmd.parser.block.BlockContinue;
import grondag.mcmd.parser.block.ParserState;

public class ParagraphParser extends AbstractBlockParser {

    private final Paragraph block = new Paragraph();
    private LinkReferenceDefinitionParser linkReferenceDefinitionParser = new LinkReferenceDefinitionParser();

    @Override
    public boolean canHaveLazyContinuationLines() {
        return true;
    }

    @Override
    public Block getBlock() {
        return block;
    }

    @Override
    public BlockContinue tryContinue(ParserState state) {
        if (!state.isBlank()) {
            return BlockContinue.atIndex(state.getIndex());
        } else {
            return BlockContinue.none();
        }
    }

    @Override
    public void addLine(CharSequence line) {
        linkReferenceDefinitionParser.parse(line);
    }

    @Override
    public void closeBlock() {
        if (linkReferenceDefinitionParser.getParagraphContent().length() == 0) {
            block.unlink();
        }
    }

    @Override
    public void parseInlines(InlineParser inlineParser) {
        CharSequence content = linkReferenceDefinitionParser.getParagraphContent();
        if (content.length() > 0) {
            inlineParser.parse(content.toString(), block);
        }
    }

    public CharSequence getContentString() {
        return linkReferenceDefinitionParser.getParagraphContent();
    }

    public List<LinkReferenceDefinition> getDefinitions() {
        return linkReferenceDefinitionParser.getDefinitions();
    }
}
