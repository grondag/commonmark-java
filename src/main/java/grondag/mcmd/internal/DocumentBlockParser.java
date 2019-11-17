package grondag.mcmd.internal;

import grondag.mcmd.node.Block;
import grondag.mcmd.node.Document;
import grondag.mcmd.parser.block.AbstractBlockParser;
import grondag.mcmd.parser.block.BlockContinue;
import grondag.mcmd.parser.block.ParserState;

public class DocumentBlockParser extends AbstractBlockParser {

    private final Document document = new Document();

    @Override
    public boolean isContainer() {
        return true;
    }

    @Override
    public boolean canContain(Block block) {
        return true;
    }

    @Override
    public Document getBlock() {
        return document;
    }

    @Override
    public BlockContinue tryContinue(ParserState state) {
        return BlockContinue.atIndex(state.getIndex());
    }

    @Override
    public void addLine(CharSequence line) {
    }

}
