package grondag.mcmd.node;

public class Strikethrough extends Node implements Delimited {

    private static final String DELIMITER = "~~";

    @Override
    public String getOpeningDelimiter() {
        return DELIMITER;
    }

    @Override
    public String getClosingDelimiter() {
        return DELIMITER;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
