package grondag.mcmd.internal.renderer.mc;

import grondag.mcmd.node.OrderedList;

public class OrderedListHolder extends ListHolder {
    private final char delimiter;
    private int counter;

    public OrderedListHolder(ListHolder parent, OrderedList list) {
        super(parent);
        delimiter = list.getDelimiter();
        counter = list.getStartNumber();
    }

    public char getDelimiter() {
        return delimiter;
    }

    public int getCounter() {
        return counter;
    }

    public void increaseCounter() {
        counter++;
    }
}
